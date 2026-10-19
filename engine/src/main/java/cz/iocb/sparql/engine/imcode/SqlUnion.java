package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Union (keeping duplicates) of children over the union of their variables; a variable missing in a branch is unbound
 * there, and the columns of the branches are aligned by resource class. Two branches accessing the same table whose
 * conditions differ only in the null test of a column are written as a single scan of the table (see
 * {@link SqlTableAccess#translateUnion}).
 */
public final class SqlUnion extends SqlIntercode
{
    /**
     * The branches.
     */
    private final List<SqlIntercode> childs;

    /**
     * Per branch, the branch column each union column is taken from.
     */
    private final List<Map<Column, Column>> columnMappings;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param childs the child nodes
     * @param columnMappings per branch, the branch column each output column is taken from
     */
    protected SqlUnion(VariableBindings bindings, List<SqlIntercode> childs, List<Map<Column, Column>> columnMappings)
    {
        super(bindings, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMappings = columnMappings;
    }


    /**
     * Union of the branches; branches without solutions are dropped and a single remaining branch is returned as is.
     *
     * @param request the current request
     * @param branches the branches
     * @return union of the branches; branches without solutions are dropped and a single remaining branch is returned
     *         as is
     */
    public static SqlIntercode union(Request request, List<SqlIntercode> branches)
    {
        return union(request, branches, null);
    }


    /**
     * Union of the branches (see {@link #union(Request, List)}) presenting the classes of a variable that a join above
     * the union compares with one of its join classes as a single class, so that the join compares a single pair of
     * classes (see {@link Restrictions#getJoinClasses}).
     *
     * @param request the current request
     * @param branches the branches
     * @param restrictions what the parent needs of the variables, or null
     * @return union of the branches presenting the classes a join above compares as a single class
     */
    protected static SqlIntercode union(Request request, List<SqlIntercode> branches, Restrictions restrictions)
    {
        ClassRelations relations = request.getConfiguration();

        /* special cases */

        branches = branches.stream().filter(i -> !i.equals(SqlNoSolution.get())).toList();

        if(branches.isEmpty())
            return SqlNoSolution.get();

        if(branches.size() == 1)
            return branches.get(0);


        /* standard union */

        Map<Variable, Set<ResourceClass>> classes = getUnionClasses(relations, branches, restrictions);


        List<SqlIntercode> childs = new ArrayList<>();

        for(SqlIntercode branch : branches)
        {
            if(branch instanceof SqlUnion union)
                childs.addAll(union.childs);
            else
                childs.add(branch);
        }


        Map<List<Column>, Column> unionColumns = new HashMap<>();
        List<Map<Column, Column>> columnMappings = new ArrayList<>(childs.size());

        for(int i = 0; i < childs.size(); i++)
            columnMappings.add(new HashMap<>());

        VariableBindings bindings = new VariableBindings();

        for(Entry<Variable, Set<ResourceClass>> entry : classes.entrySet())
        {
            Variable variable = entry.getKey();
            List<VariableBinding> uvars = childs.stream().map(c -> c.getVariable(variable)).toList();

            boolean canBeNull = uvars.stream().anyMatch(v -> v == null || v.canBeNull());
            VariableBinding variableBinding = new VariableBinding(variable, canBeNull);

            for(ResourceClass resourceClass : entry.getValue())
            {
                if(uvars.stream().filter(Objects::nonNull).flatMap(v -> v.getMappings().entrySet().stream())
                        .anyMatch(r -> isNull(r.getValue()) && !areDisjunct(relations, r.getKey(), resourceClass)))
                {
                    variableBinding.addMapping(resourceClass, null);
                    continue;
                }


                List<List<Column>> cols = new ArrayList<>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                    cols.add(new ArrayList<>(childs.size()));


                for(VariableBinding binding : uvars)
                {
                    if(binding == null)
                    {
                        for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            cols.get(i).add(new NullColumn(resourceClass.getSqlTypes().get(i)));
                    }
                    else
                    {
                        List<Column> c = binding.deriveMapping(relations, resourceClass);

                        for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            cols.get(i).add(c.get(i));
                    }
                }


                List<Column> columns = resourceClass.createColumns(request.getColumnMap(),
                        variableBinding.getVariable());
                List<Column> mapping = new ArrayList<>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                {
                    List<Column> c = cols.get(i);

                    if(c.get(0) instanceof ConstantColumn c0 && c.stream().allMatch(d -> d.equals(c0)))
                    {
                        mapping.add(c.get(0));
                    }
                    else if(unionColumns.containsKey(c))
                    {
                        mapping.add(unionColumns.get(c));
                    }
                    else
                    {
                        // the union column is not null when the columns of all the branches are not null
                        Column col = new TableColumn(columns.get(i).getName(), columns.get(i).getType(),
                                c.stream().anyMatch(Column::canBeNull));
                        unionColumns.put(c, col);
                        mapping.add(col);

                        for(int j = 0; j < childs.size(); j++)
                            columnMappings.get(j).put(col, c.get(j));
                    }
                }

                variableBinding.addMapping(resourceClass, mapping);
            }

            bindings.add(variableBinding);
        }

        return new SqlUnion(bindings, childs, columnMappings);
    }


    /**
     * Classes per variable of a union of the branches: the classes of the branches merged by
     * {@link #unionResourceClasses(ClassRelations, List)}, and further by the join classes of the restrictions when
     * given (see {@link #unionResourceClasses(ClassRelations, List, Set)}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param branches the branches
     * @param restrictions what the parent needs of the variables, or null
     * @return classes per variable of a union of the branches
     */
    private static Map<Variable, Set<ResourceClass>> getUnionClasses(ClassRelations relations,
            List<SqlIntercode> branches, Restrictions restrictions)
    {
        Set<Variable> varNames = branches.stream().flatMap(i -> i.bindings.getVariables().stream()).collect(toSet());
        Map<Variable, Set<ResourceClass>> classes = new HashMap<>();

        for(Variable var : varNames)
        {
            List<VariableBinding> defs = branches.stream().map(b -> b.getVariableBindings().get(var))
                    .filter(Objects::nonNull).toList();

            classes.put(var, restrictions == null ? unionResourceClasses(relations, defs) :
                    unionResourceClasses(relations, defs, restrictions.getJoinClasses(var)));
        }

        return classes;
    }


    /**
     * The branches.
     *
     * @return the branches
     */
    public final List<SqlIntercode> getChilds()
    {
        return childs;
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        List<SqlIntercode> optChilds = childs.stream()
                .map(c -> c.optimize(request, restrictions, reduced, evalServices))
                .filter(i -> !i.equals(SqlNoSolution.get())).toList();


        if(optChilds.isEmpty())
            return SqlNoSolution.get();

        if(optChilds.size() == 1)
            return optChilds.get(0);


        if(optChilds.equals(childs) && hasClasses(getUnionClasses(relations, childs, restrictions)))
            return this;

        return union(request, optChilds, restrictions);
    }


    /**
     * True if the variables take exactly the given classes.
     *
     * @param classes classes per variable
     * @return true if the variables take exactly the given classes, false otherwise
     */
    private boolean hasClasses(Map<Variable, Set<ResourceClass>> classes)
    {
        return bindings.getValues().stream().allMatch(b -> b.getClasses().equals(classes.get(b.getVariable())));
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        Set<Column> columns = bindings.getNonConstantColumns();

        // the branches already written together with an earlier branch as a single scan of their table
        boolean[] merged = new boolean[childs.size()];
        boolean hasBranch = false;

        for(int i = 0; i < childs.size(); i++)
        {
            if(merged[i])
                continue;

            if(hasBranch)
                builder.append(" UNION ALL ");

            hasBranch = true;

            SqlIntercode child = childs.get(i);
            Map<Column, Column> columnMapping = columnMappings.get(i);

            if(child instanceof SqlTableAccess access)
            {
                int partner = findUnionPartner(access, i, merged);

                if(partner >= 0)
                {
                    merged[partner] = true;
                    builder.append(SqlTableAccess.translateUnion(access, (SqlTableAccess) childs.get(partner),
                            columnMapping, columnMappings.get(partner), columns));
                    continue;
                }
            }

            builder.append("SELECT ");
            boolean hasSelect = false;

            for(Column column : columns)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                Column col = columnMapping.get(column);

                if(col == null)
                    builder.append("NULL AS ").append(column);
                else if(column.equals(col))
                    builder.append(column);
                else
                    builder.append(col).append(" AS ").append(column);
            }

            if(!hasSelect)
                builder.append("1");

            builder.append(" FROM (");
            builder.append(child.translate(request));
            builder.append(") AS tab");
            builder.append(i);
        }

        return builder.toString();
    }


    /**
     * Index of the first later branch that can be written together with the given branch as a single scan of their
     * table (see {@link SqlTableAccess#canBeUnionized}), or -1 when there is none.
     *
     * @param access the branch
     * @param index the index of the branch
     * @param merged the branches already written together with an earlier branch
     * @return index of the first later branch that can be written together with the branch, or -1
     */
    private int findUnionPartner(SqlTableAccess access, int index, boolean[] merged)
    {
        for(int j = index + 1; j < childs.size(); j++)
            if(!merged[j] && childs.get(j) instanceof SqlTableAccess other
                    && SqlTableAccess.canBeUnionized(access, other))
                return j;

        return -1;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return childs.stream().anyMatch(c -> c.hasServiceSubpattern());
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return getVirtualTables(childs);
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, childs, variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("union");

        for(int i = 0; i < childs.size(); i++)
        {
            indentChild(builder, indent, i == childs.size() - 1);
            childs.get(i).generateExplanation(builder, getIndent(indent, i == childs.size() - 1));
        }
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlUnion imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(new Multiset<>(childs), new Multiset<>(imcode.childs)))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(new Multiset<>(childs));
    }
}
