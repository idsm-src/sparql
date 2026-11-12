package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.Table.toTableColumns;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UnsupportedIriClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Inner join whose right side is evaluated once for every solution of the left side and may refer to that solution (SQL
 * CROSS JOIN LATERAL).
 *
 * The right side refers to the left one through a table alias obtained from Request.createLateralTable(). The alias has
 * to be unique, because the reference crosses the boundaries of nested subqueries and a nested lateral join would
 * otherwise shadow the alias of the outer one. The columns are given to the right side by getLateralVariableBindings()
 * (an expression of the right side refers to them through SqlLateralVariable) and the same alias and requirements have
 * to be passed to lateralJoin(). The alias is fixed when the join is created and is kept by all reconstructions of the
 * join during its optimisation. The bindings exposed this way are read by the right side in one of two ways.
 *
 * A live join lets its right side read them as the left side provides them at the moment: while the right side is
 * optimised, the request holds the current bindings of the left side (see Request.beginLateral()), from which the
 * references of the right side take their classes and columns again, so the right side is optimised for the classes and
 * the constants the left side has been optimised to. When the left side is a union, the join is made for every branch,
 * and each of them then optimises the right side for its own branch: an evaluation of a pattern per named graph keeps
 * in the branch of a graph (or of the graphs of a class) only the table accesses that can hold triples of that graph,
 * instead of all accesses of the pattern with the graphs compared when the statement runs. The join keeps the bindings
 * its right side has been optimised for.
 *
 * The other joins keep the bindings exposed when the join was created, which the copies of a SERVICE call in the
 * branches rely on: the context of the call is evaluated by a statement of its own for all solutions of the left side
 * at once, and its result is shared by the copies (see SqlServiceStub.Enclosure).
 *
 * In both cases, if the left side does not provide an exposed column the right side may refer to (the optimisation has
 * proven the variable unbound in that class or has turned the column into a constant, or the join has just been made
 * for a branch of the left side), the translation adds a projection that supplies the column again, so the right side
 * never refers to a missing column.
 *
 * Variables shared by both sides are joined in the usual way. Unlike in SqlLeftJoin, the right side never introduces
 * nulls, so the nullability of the result follows the same rules as in SqlJoin. Unlike in SqlJoin, the sides can
 * neither be reordered nor merged into a single table access, because the right side depends on the rows of the left
 * side. A lateral join whose right side refers to nothing is an ordinary join, unless the right side is
 * nondeterministic, because the lateral join evaluates it again for every solution of the left side.
 */
public final class SqlLateralJoin extends SqlIntercode
{
    /**
     * Alias of the right side.
     */
    private static final AliasTable rightTable = new AliasTable("tab1");

    /**
     * Alias of the projection supplying columns the optimised left side dropped.
     */
    private static final AliasTable innerTable = new AliasTable("tab");

    /**
     * Left side.
     */
    private final SqlIntercode left;

    /**
     * Right side, evaluated per solution of the left side.
     */
    private final SqlIntercode right;

    /**
     * Alias through which the right side refers to the left side.
     */
    private final AliasTable table;

    /**
     * Left-side bindings the right side may refer to: those the right side has been optimised for when the right side
     * reads them live, those exposed when the join was created otherwise.
     */
    private final VariableBindings lateral;

    /**
     * Whether the right side reads the exposed bindings as the left side provides them at the moment.
     */
    private final boolean live;

    /**
     * What the right side needs of the left side: the exposed variables whole when the right side reads them live,
     * because it follows whatever the left side binds them to; otherwise the variables and classes exposed through
     * non-constant columns.
     */
    private final Restrictions requirements;

    /**
     * For each output column, the side column it is taken from.
     */
    private final Map<Column, Column> columnMap;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param left the left side
     * @param right the right side
     * @param table alias through which the right side refers to the left side
     * @param lateral left-side bindings exposed to the right side
     * @param live whether the right side reads the exposed bindings as the left side provides them at the moment
     * @param columnMap the column map
     */
    protected SqlLateralJoin(VariableBindings bindings, SqlIntercode left, SqlIntercode right, AliasTable table,
            VariableBindings lateral, boolean live, Map<Column, Column> columnMap)
    {
        super(bindings, left.isDeterministic() && right.isDeterministic());

        this.left = left;
        this.right = right;
        this.table = table;
        this.lateral = lateral;
        this.live = live;
        this.requirements = live ? new Restrictions(lateral.getVariables()) : getLateralRequirements(lateral);
        this.columnMap = columnMap;
    }


    /**
     * Lateral join with a fresh alias; the right side may refer to all variables of the left side.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @return lateral join with a fresh alias; the right side may refer to all variables of the left side
     */
    public static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right)
    {
        return lateralJoin(request, left, right, request.createLateralTable(), null, false);
    }


    /**
     * Lateral join through the given alias; the right side may refer to all variables of the left side.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param table alias through which the right side refers to the left side
     * @return lateral join through the given alias; the right side may refer to all variables of the left side
     */
    public static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right, AliasTable table)
    {
        //NOTE: the right side may refer to all variables of the left side, so the left side cannot drop any of them
        return lateralJoin(request, left, right, table, null, false);
    }


    /**
     * Lateral join through the given alias; the right side may refer to the left-side bindings returned by
     * {@link #getLateralVariableBindings} for the same alias and requirements.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param table alias through which the right side refers to the left side
     * @param requirements variables and classes the right side refers to
     * @param live whether the right side reads the exposed bindings as the left side provides them at the moment,
     *            instead of as they are exposed now
     * @return lateral join through the given alias; the right side may refer to the left-side bindings returned by
     *         {@link #getLateralVariableBindings} for the same alias and requirements
     */
    public static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right, AliasTable table,
            Restrictions requirements, boolean live)
    {
        //NOTE: the right side may refer to the bindings returned by getLateralVariableBindings() for these arguments
        return lateralJoin(request, left, right, table,
                left.getVariableBindings().restrict(request.getConfiguration(), requirements), live, null);
    }


    /**
     * Lateral join exposing only what the parent needs; degrades to an ordinary join when the right side is
     * deterministic and refers to nothing.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param table alias through which the right side refers to the left side
     * @param lateral left-side bindings exposed to the right side
     * @param live whether the right side reads the exposed bindings as the left side provides them at the moment
     * @param restrictions what the parent needs of the variables
     * @return lateral join exposing only what the parent needs; degrades to an ordinary join when the right side is
     *         deterministic and refers to nothing
     */
    protected static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right, AliasTable table,
            VariableBindings lateral, boolean live, Restrictions restrictions)
    {
        /* NOTE: If the right side does not refer to the left side, all optimizations of the ordinary join apply. This
         * does not hold for a nondeterministic right side, which the ordinary join evaluates only once, whereas the
         * lateral join evaluates it again for every solution of the left side.
         */

        if(getLateralRequirements(lateral).getNames().isEmpty() && right.isDeterministic())
            return SqlJoin.join(request, List.of(left, right), restrictions);

        Map<Column, Column> map = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, left.getVariableBindings(),
                right.getVariableBindings(), table, rightTable, restrictions, map);

        return new SqlLateralJoin(bindings, left, right, table, lateral, live, map);
    }


    /**
     * Bindings of the left side as seen by the right side: its columns addressed through the lateral alias, restricted
     * to the requirements.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param table alias through which the right side refers to the left side
     * @param left the left side
     * @param requirements variables and classes the right side refers to
     * @return bindings of the left side as seen by the right side: its columns addressed through the lateral alias,
     *         restricted to the requirements
     */
    public static VariableBindings getLateralVariableBindings(ClassRelations relations, AliasTable table,
            SqlIntercode left, Restrictions requirements)
    {
        //NOTE: the returned bindings refer to the current solution of the left side through the given alias

        VariableBindings result = new VariableBindings();

        for(VariableBinding variableBinding : left.getVariableBindings().restrict(relations, requirements).getValues())
        {
            VariableBinding binding = new VariableBinding(variableBinding.getVariable(), variableBinding.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : variableBinding.getMappings().entrySet())
                binding.addMapping(entry.getKey(), toTableColumns(table, entry.getValue()));

            result.add(binding);
        }

        return result;
    }


    /**
     * Variables and classes referred to through non-constant columns of the exposed bindings, i.e. what the side
     * exposing them has to provide.
     *
     * @param lateral exposed bindings
     * @return variables and classes referred to through non-constant columns of the exposed bindings
     */
    public static Restrictions getLateralRequirements(VariableBindings lateral)
    {
        Restrictions requirements = new Restrictions();

        for(VariableBinding binding : lateral.getValues())
            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                if(entry.getValue() != null && entry.getValue().stream().anyMatch(c -> !(c instanceof ConstantColumn)))
                    requirements.add(binding.getVariable(), entry.getKey());

        return requirements;
    }


    /**
     * Value of every non-constant exposed column as the current bindings of the exposing side provide it: the column of
     * the same class converted from what the side provides now (a compatible class or a constant), or NULL when the
     * variable or the class is gone.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param lateral exposed bindings
     * @param current current bindings of the exposing side
     * @return value of every non-constant exposed column as the current bindings of the exposing side provide it
     */
    public static Map<Column, Column> getLateralValues(ClassRelations relations, VariableBindings lateral,
            VariableBindings current)
    {
        Map<Column, Column> values = new HashMap<>();

        for(VariableBinding binding : lateral.getValues())
        {
            VariableBinding provided = current.get(binding.getVariable());

            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
            {
                ResourceClass resClass = entry.getKey();
                List<Column> exposed = entry.getValue();

                if(exposed == null)
                    continue;

                List<Column> columns = provided == null ? null : deriveExposed(relations, provided, resClass);

                for(int i = 0; i < resClass.getColumnCount(); i++)
                {
                    Column column = exposed.get(i);

                    if(column instanceof ConstantColumn)
                        continue;

                    values.put(column,
                            columns != null ? columns.get(i) : new NullColumn(resClass.getSqlTypes().get(i)));
                }
            }
        }

        return values;
    }


    /**
     * Columns of the exposed class holding the value the binding provides (see {@link VariableBinding#deriveMapping}).
     * Whether an IRI of a more general class belongs to no user IRI class (see {@link UnsupportedIriClass}) cannot be
     * checked, so such an IRI is converted to that class unchecked: the exposed value is only compared with IRIs of the
     * class, which an IRI of a user class never equals, so an IRI of a user class converted unchecked matches nothing.
     * A value provided in several classes such an IRI could come from is left to {@link VariableBinding#deriveMapping},
     * which cannot convert it either.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param provided the binding providing the value
     * @param resClass the exposed class
     * @return columns of the exposed class holding the value, or null when the class providing it has no columns
     */
    private static List<Column> deriveExposed(ClassRelations relations, VariableBinding provided,
            ResourceClass resClass)
    {
        if(!(resClass instanceof UnsupportedIriClass) || provided.canDeriveMapping(relations, resClass))
            return provided.deriveMapping(relations, resClass);

        List<Column> result = null;

        for(Entry<ResourceClass, List<Column>> entry : provided.getMappings().entrySet())
        {
            ResourceClass sourceClass = entry.getKey();

            if(ResourceClass.areDisjunct(relations, resClass, sourceClass)
                    || !resClass.isSubclassOf(sourceClass.getEffectiveClass()))
                continue;

            if(entry.getValue() == null)
                return null;

            // a value provided in several classes would need a choice among them
            if(result != null)
                return provided.deriveMapping(relations, resClass);

            result = resClass.fromGeneralClass(sourceClass, entry.getValue(), true);
        }

        return result != null ? result : provided.deriveMapping(relations, resClass);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;

        Restrictions leftBaseRestrictions = new Restrictions(restrictions, requirements);

        boolean leftReduced = reduced && optRight.isDeterministic();
        Restrictions leftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                optRight.getVariableBindings(), leftBaseRestrictions, true);
        Restrictions rightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                optLeft.getVariableBindings(), restrictions, true);


        // the sides could swap their presentations of a shared variable in every round
        Settlement settlement = new Settlement();

        // the left-side bindings the right side refers to at the moment
        VariableBindings optLateral = lateral;

        while(true)
        {
            optLeft = optLeft.optimize(request, leftRestrictions, leftReduced, evalServices);

            // a right side reading the current solution of the left side would find every variable of it unbound
            if(live && optLeft.equals(SqlNoSolution.get()))
                return SqlNoSolution.get();

            /* NOTE: A right side reading the bindings live is not optimised for the whole union first: the join of
             * every branch optimises it for its own branch as it stands, before the optimisation rewrites it in ways
             * that cannot be taken back (an aggregation distributed over the members of a union that a branch would
             * have reduced to a single member).
             */
            if(live && optLeft instanceof SqlUnion union)
                return distribute(request, union, optRight, optLateral, restrictions, reduced, evalServices);

            VariableBindings enclosing = null;

            // the right side reads the current solution of the left side as the left side provides it now
            if(live)
                enclosing = request.beginLateral(table,
                        getLateralVariableBindings(relations, table, optLeft, requirements));

            try
            {
                optRight = optRight.optimize(request, rightRestrictions, reduced, evalServices);

                if(optRight instanceof SqlUnion union)
                {
                    List<SqlIntercode> unionList = new ArrayList<>();

                    for(SqlIntercode child : union.getChilds())
                        if(isJoinable(relations, optLeft, child))
                            unionList.add(child);

                    if(!unionList.equals(union.getChilds()))
                        optRight = SqlUnion.union(request, unionList).optimize(request, rightRestrictions, reduced,
                                evalServices);
                }
            }
            finally
            {
                if(live)
                    request.endLateral(table, enclosing);
            }

            if(live)
                optLateral = optLeft.getVariableBindings().restrict(relations, requirements);


            boolean newLeftReduced = reduced && optRight.isDeterministic();
            Restrictions newLeftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                    Set.of(optRight.getVariableBindings()), leftBaseRestrictions, true, settlement.getClasses());
            Restrictions newRightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                    Set.of(optLeft.getVariableBindings()), restrictions, true, settlement.getClasses());


            if(newLeftReduced == leftReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            if(!settlement.record(List.of(optLeft, optRight, newLeftReduced), List.of(optLeft, optRight)))
                break;

            leftReduced = newLeftReduced;
            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
        }


        if(optLeft.equals(SqlNoSolution.get()) || optRight.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(!isJoinable(relations, optLeft, optRight))
            return SqlNoSolution.get();

        if(optRight.equals(SqlEmptySolution.get()))
            return optLeft.optimize(request, restrictions, reduced, evalServices);

        if(optLeft instanceof SqlUnion union)
            return distribute(request, union, optRight, optLateral, restrictions, reduced, evalServices);


        if(optLeft == left && optRight == right && optLateral.equals(lateral)
                && restrictions.isOptimized(relations, bindings))
            return this;

        return lateralJoin(request, optLeft, optRight, table, optLateral, live, restrictions);
    }


    /**
     * The join made for every branch of a left side that is a union, optimised. The branches share the right side and
     * therefore also the alias; they are not nested, so no shadowing. A right side reading the bindings live refers to
     * the given bindings until the join of a branch optimises it for the branch.
     *
     * @param request the current request
     * @param union the left side
     * @param optRight the right side
     * @param optLateral left-side bindings the right side refers to
     * @param restrictions what the parent needs of the variables
     * @param reduced whether duplicate solutions may be dropped
     * @param evalServices whether SERVICE stubs are evaluated
     * @return union of the joins of the branches of the left side with the right side, optimised
     */
    private SqlIntercode distribute(Request request, SqlUnion union, SqlIntercode optRight, VariableBindings optLateral,
            Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        List<SqlIntercode> unionList = new ArrayList<>();

        for(SqlIntercode child : union.getChilds())
            unionList.add(lateralJoin(request, child, optRight, table, optLateral, live, restrictions));

        return SqlUnion.union(request, unionList).optimize(request, restrictions, reduced, evalServices);
    }


    @Override
    public String translate(Request request)
    {
        ClassRelations relations = request.getConfiguration();

        StringBuilder builder = new StringBuilder();

        Set<Column> columns = bindings.getNonConstantColumns();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        translateLeft(request, builder);
        builder.append(") AS ");
        builder.append(table);

        builder.append(" CROSS JOIN LATERAL (");
        builder.append(right.translate(request));
        builder.append(") AS ");
        builder.append(rightTable);

        String condition = generateJoinCondition(relations, left.getVariableBindings(), right.getVariableBindings(),
                table, rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toString();
    }


    /**
     * Appends the SQL of the left side, wrapped in a projection re-supplying the exposed columns the optimised left
     * side no longer provides (from a compatible class, a constant, or NULL).
     *
     * @param request the current request
     * @param builder the builder to append to
     */
    private void translateLeft(Request request, StringBuilder builder)
    {
        ClassRelations relations = request.getConfiguration();

        /* NOTE: If the optimized left side no longer provides some column that was exposed to the right side when the
         * join was created, it is wrapped into a projection that supplies the column from what the left side provides
         * now (a value of a compatible class, a constant, or null).
         */

        Map<Column, Column> supplements = new HashMap<>();

        for(Entry<Column, Column> entry : getLateralValues(relations, lateral, left.getVariableBindings()).entrySet())
            if(!entry.getValue().equals(entry.getKey()))
                supplements.put(entry.getKey(), entry.getValue());


        if(supplements.isEmpty())
        {
            builder.append(left.translate(request));
            return;
        }


        builder.append("SELECT ");

        builder.append(supplements.entrySet().stream().map(e -> e.getValue() + " AS " + e.getKey()).sorted()
                .collect(joining(", ")));

        Set<Column> columns = new HashSet<>(left.getVariableBindings().getNonConstantColumns());
        columns.removeAll(supplements.keySet());

        for(Column column : columns)
        {
            builder.append(", ");
            builder.append(column);
        }

        builder.append(" FROM (");
        builder.append(left.translate(request));
        builder.append(") AS ");
        builder.append(innerTable);
    }


    /**
     * Left side.
     *
     * @return left side
     */
    public final SqlIntercode getLeft()
    {
        return left;
    }


    /**
     * Right side, evaluated per solution of the left side.
     *
     * @return right side, evaluated per solution of the left side
     */
    public final SqlIntercode getRight()
    {
        return right;
    }


    /**
     * Alias through which the right side refers to the left side.
     *
     * @return alias through which the right side refers to the left side
     */
    public final AliasTable getLateralTable()
    {
        return table;
    }


    /**
     * Left-side bindings the right side may refer to.
     *
     * @return left-side bindings the right side may refer to
     */
    public final VariableBindings getLateralBindings()
    {
        return lateral;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return left.hasServiceSubpattern() || right.hasServiceSubpattern();
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return getVirtualTables(left, right);
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, List.of(left, right), variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("lateral join");

        if(!requirements.getNames().isEmpty())
        {
            indentInfo(builder, indent, true);
            builder.append(table);
            builder.append(" using ");
            builder.append(requirements.getNames().stream().map(Object::toString).sorted().collect(joining(", ")));
        }

        indentChild(builder, indent, false);
        left.generateExplanation(builder, getIndent(indent, false));

        indentChild(builder, indent, true);
        right.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlLateralJoin imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(table, imcode.table))
            return false;

        if(live != imcode.live)
            return false;

        if(!Objects.equals(lateral, imcode.lateral))
            return false;

        if(!Objects.equals(left, imcode.left))
            return false;

        if(!Objects.equals(right, imcode.right))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(table, lateral, live, left, right);
    }
}
