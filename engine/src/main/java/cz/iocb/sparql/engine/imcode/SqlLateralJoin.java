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
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/* Inner join whose right side is evaluated once for every solution of the left side and may refer to that solution
 * (SQL CROSS JOIN LATERAL).
 *
 * The right side refers to the left one through a table alias obtained from Request.createLateralTable(). The alias
 * has to be unique, because the reference crosses the boundaries of nested subqueries and a nested lateral join would
 * otherwise shadow the alias of the outer one. The columns are given to the right side by getLateralVariableBindings()
 * and the same alias and requirements have to be passed to lateralJoin(). The alias and the bindings exposed this way
 * are fixed when the join is created and are kept by all reconstructions of the join during its optimization; if the
 * optimization of the left side removes an exposed column (the variable is proven to be unbound in that class) or
 * turns it into a constant, the translation adds a projection that supplies the column again, so the right side never
 * refers to a missing column.
 *
 * Variables shared by both sides are joined in the usual way. Unlike in SqlLeftJoin, the right side never introduces
 * nulls, so the nullability of the result follows the same rules as in SqlJoin. Unlike in SqlJoin, the sides can
 * neither be reordered nor merged into a single table access, because the right side depends on the rows of the left
 * side. A lateral join whose right side refers to nothing is an ordinary join, unless the right side is
 * nondeterministic, because the lateral join evaluates it again for every solution of the left side.
 */
public final class SqlLateralJoin extends SqlIntercode
{
    private static final Table rightTable = new Table("tab1");
    private static final Table innerTable = new Table("tab");

    private final SqlIntercode left;
    private final SqlIntercode right;
    private final Table table;
    private final VariableBindings lateral;
    private final Restrictions requirements;
    private final Map<Column, Column> columnMap;


    protected SqlLateralJoin(VariableBindings bindings, SqlIntercode left, SqlIntercode right, Table table,
            VariableBindings lateral, Map<Column, Column> columnMap)
    {
        super(bindings, left.isDeterministic() && right.isDeterministic());

        this.left = left;
        this.right = right;
        this.table = table;
        this.lateral = lateral;
        this.requirements = getRequirements(lateral);
        this.columnMap = columnMap;
    }


    public static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right)
    {
        return lateralJoin(request, left, right, request.createLateralTable(), null);
    }


    public static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right, Table table)
    {
        //NOTE: the right side may refer to all variables of the left side, so the left side cannot drop any of them
        return lateralJoin(request, left, right, table, null);
    }


    public static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right, Table table,
            Restrictions requirements)
    {
        //NOTE: the right side may refer to the bindings returned by getLateralVariableBindings() for these arguments
        return lateralJoin(request, left, right, table, left.getVariableBindings().restrict(requirements), null);
    }


    protected static SqlIntercode lateralJoin(Request request, SqlIntercode left, SqlIntercode right, Table table,
            VariableBindings lateral, Restrictions restrictions)
    {
        /* NOTE: If the right side does not refer to the left side, all optimizations of the ordinary join apply. This
         * does not hold for a nondeterministic right side, which the ordinary join evaluates only once, whereas the
         * lateral join evaluates it again for every solution of the left side.
         */

        if(getRequirements(lateral).getNames().isEmpty() && right.isDeterministic())
            return SqlJoin.join(request, List.of(left, right), restrictions);

        Map<Column, Column> map = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, left.getVariableBindings(),
                right.getVariableBindings(), table, rightTable, restrictions, map);

        return new SqlLateralJoin(bindings, left, right, table, lateral, map);
    }


    public static VariableBindings getLateralVariableBindings(Table table, SqlIntercode left, Restrictions requirements)
    {
        //NOTE: the returned bindings refer to the current solution of the left side through the given alias

        VariableBindings result = new VariableBindings();

        for(VariableBinding variableBinding : left.getVariableBindings().restrict(requirements).getValues())
        {
            VariableBinding binding = new VariableBinding(variableBinding.getVariable(), variableBinding.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : variableBinding.getMappings().entrySet())
                binding.addMapping(entry.getKey(), toTableColumns(table, entry.getValue()));

            result.add(binding);
        }

        return result;
    }


    private static Restrictions getRequirements(VariableBindings lateral)
    {
        Restrictions requirements = new Restrictions();

        for(VariableBinding binding : lateral.getValues())
            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                if(entry.getValue() != null && entry.getValue().stream().anyMatch(c -> !(c instanceof ConstantColumn)))
                    requirements.add(binding.getVariable(), entry.getKey());

        return requirements;
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;

        Restrictions leftBaseRestrictions = new Restrictions(restrictions, requirements);

        boolean leftReduced = reduced && optRight.isDeterministic();
        Restrictions leftRestrictions = getJoinRestrictions(optLeft.getVariableBindings(),
                optRight.getVariableBindings(), leftBaseRestrictions);
        Restrictions rightRestrictions = getJoinRestrictions(optRight.getVariableBindings(),
                optLeft.getVariableBindings(), restrictions);


        while(true)
        {
            optLeft = optLeft.optimize(request, leftRestrictions, leftReduced, evalServices);
            optRight = optRight.optimize(request, rightRestrictions, reduced, evalServices);

            if(optRight instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<>();

                for(SqlIntercode child : union.getChilds())
                    if(isJoinable(optLeft, child))
                        unionList.add(child);

                if(!unionList.equals(union.getChilds()))
                    optRight = SqlUnion.union(request, unionList).optimize(request, rightRestrictions, reduced,
                            evalServices);
            }


            boolean newLeftReduced = reduced && optRight.isDeterministic();
            Restrictions newLeftRestrictions = getJoinRestrictions(optLeft.getVariableBindings(),
                    optRight.getVariableBindings(), leftBaseRestrictions);
            Restrictions newRightRestrictions = getJoinRestrictions(optRight.getVariableBindings(),
                    optLeft.getVariableBindings(), restrictions);


            if(newLeftReduced == leftReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            leftReduced = newLeftReduced;
            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
        }


        if(optLeft.equals(SqlNoSolution.get()) || optRight.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(!isJoinable(optLeft, optRight))
            return SqlNoSolution.get();

        if(optRight.equals(SqlEmptySolution.get()))
            return optLeft.optimize(request, restrictions, reduced, evalServices);

        if(optLeft instanceof SqlUnion union)
        {
            //NOTE: the branches share the right side and therefore also the alias; they are not nested, so no shadowing
            List<SqlIntercode> unionList = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                unionList.add(lateralJoin(request, child, optRight, table, lateral, restrictions));

            return SqlUnion.union(request, unionList).optimize(request, restrictions, reduced, evalServices);
        }


        if(optLeft == left && optRight == right && restrictions.isOptimized(bindings))
            return this;

        return lateralJoin(request, optLeft, optRight, table, lateral, restrictions);
    }


    @Override
    public String translate(Request request)
    {
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

        String condition = generateJoinCondition(left.getVariableBindings(), right.getVariableBindings(), table,
                rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toString();
    }


    private void translateLeft(Request request, StringBuilder builder)
    {
        /* NOTE: If the optimized left side no longer provides some column that was exposed to the right side when the
         * join was created, it is wrapped into a projection that supplies the column from what the left side provides
         * now (a value of a compatible class, a constant, or null).
         */

        Map<Column, Column> supplements = new HashMap<>();

        for(VariableBinding binding : lateral.getValues())
        {
            VariableBinding current = left.getVariableBindings().get(binding.getVariable());

            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
            {
                ResourceClass resClass = entry.getKey();
                List<Column> exposed = entry.getValue();

                if(exposed == null)
                    continue;

                List<Column> provided = current == null ? null : current.deriveMapping(resClass);

                for(int i = 0; i < resClass.getColumnCount(); i++)
                {
                    Column column = exposed.get(i);

                    if(column instanceof ConstantColumn)
                        continue;

                    Column value = provided != null ? provided.get(i) :
                            new ConstantColumn(null, resClass.getSqlTypes().get(i));

                    if(!value.equals(column))
                        supplements.put(column, value);
                }
            }
        }


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


    public final SqlIntercode getLeft()
    {
        return left;
    }


    public final SqlIntercode getRight()
    {
        return right;
    }


    public final Table getLateralTable()
    {
        return table;
    }


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
        return Objects.hash(table, lateral, left, right);
    }
}
