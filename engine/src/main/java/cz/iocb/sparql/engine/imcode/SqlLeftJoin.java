package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlLeftJoin extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final SqlIntercode left;
    private final SqlIntercode right;
    private final List<SqlExpressionIntercode> conditions;
    private final Map<Column, Column> columnMap;


    protected SqlLeftJoin(VariableBindings bindings, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions, Map<Column, Column> columnMap)
    {
        super(bindings, left.isDeterministic() && right.isDeterministic()
                && conditions.stream().allMatch(c -> c.isDeterministic()));

        this.left = left;
        this.right = right;
        this.conditions = conditions;
        this.columnMap = columnMap;
    }


    public static SqlIntercode leftJoin(Request request, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions)
    {
        return leftJoin(request, left, right, conditions, null);
    }


    protected static SqlIntercode leftJoin(Request request, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions, Restrictions restrictions)
    {
        boolean hasConstantColumn = false;

        for(VariableBinding binding : right.getVariableBindings().getValues())
            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                if(entry.getValue() != null)
                    for(Column column : entry.getValue())
                        if(column instanceof ConstantColumn)
                            hasConstantColumn = true;

        if(hasConstantColumn)
            right = SqlStripConstantColumns.strip(right);

        Map<Column, Column> map = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, left.getVariableBindings(),
                setCanBeNull(right.getVariableBindings()), leftTable, rightTable, restrictions, map);

        return new SqlLeftJoin(bindings, left, right, conditions, map);
    }


    private static boolean isJoinable(SqlIntercode left, SqlIntercode right, List<SqlExpressionIntercode> conditions)
    {
        if(conditions.stream().anyMatch(f -> f.equals(SqlNull.get()) || f.equals(falseValue)
                || f instanceof SqlBooleanExpression b && b.isFalseOrError()))
            return false;

        return isJoinable(left, right);
    }


    private static VariableBindings setCanBeNull(VariableBindings bindings)
    {
        VariableBindings result = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
            result.add(new VariableBinding(binding.getVariable(), binding.getMappings(), true));

        return result;
    }


    public static VariableBindings getExpressionVariableBindings(Request request, VariableBindings left,
            VariableBindings right)
    {
        Map<Column, Column> map = new HashMap<>();
        VariableBindings joinBindings = getJoinVariableBindings(request, left, right, leftTable, rightTable, null, map);

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding variableBinding : (joinBindings != null ? joinBindings : left).getValues())
        {
            VariableBinding binding = new VariableBinding(variableBinding.getVariable(), variableBinding.canBeNull());

            for(ResourceClass resClass : variableBinding.getClasses())
            {
                List<Column> cols = variableBinding.getMapping(resClass);

                if(cols == null)
                    binding.addMapping(resClass, null);
                else if(joinBindings != null)
                    binding.addMapping(resClass, cols.stream().map(c -> map.get(c)).toList());
                else
                    binding.addMapping(resClass, cols.stream().map(c -> c.fromTable(leftTable)).toList());
            }

            bindings.add(binding);
        }

        return bindings;
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode right = this.right;

        if(right instanceof SqlStripConstantColumns strip)
            right = strip.getChild();

        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;
        List<SqlExpressionIntercode> optConditions = conditions;
        boolean optReduced = reduced & optConditions.stream().allMatch(r -> r.isDeterministic());


        Restrictions cndRestrictions = new Restrictions(restrictions);

        for(SqlExpressionIntercode condition : optConditions)
            cndRestrictions.add(condition.getRequirements());

        Restrictions leftRestrictions = getJoinRestrictions(optLeft.getVariableBindings(),
                optRight.getVariableBindings(), cndRestrictions);
        Restrictions rightRestrictions = getJoinRestrictions(optRight.getVariableBindings(),
                optLeft.getVariableBindings(), cndRestrictions);


        while(true)
        {
            optLeft = optLeft.optimize(request, leftRestrictions, optReduced, evalServices);
            optRight = optRight.optimize(request, rightRestrictions, optReduced, evalServices);
            optConditions = optimize(request, optConditions, optLeft.getVariableBindings(),
                    optRight.getVariableBindings(), evalServices);

            if(optRight instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<>();

                for(SqlIntercode child : union.getChilds())
                    if(isJoinable(optLeft, child, optimize(request, optConditions, optLeft.getVariableBindings(),
                            child.getVariableBindings(), evalServices)))
                        unionList.add(child);

                if(!unionList.equals(union.getChilds()))
                {
                    optRight = SqlUnion.union(request, unionList).optimize(request, rightRestrictions, optReduced,
                            evalServices);

                    optConditions = optimize(request, optConditions, optLeft.getVariableBindings(),
                            optRight.getVariableBindings(), evalServices);
                }
            }


            boolean newOptReduced = optReduced & optConditions.stream().allMatch(r -> r.isDeterministic());

            Restrictions newCndRestrictions = new Restrictions(restrictions);

            for(SqlExpressionIntercode condition : optConditions)
                newCndRestrictions.add(condition.getRequirements());

            Restrictions newLeftRestrictions = getJoinRestrictions(optLeft.getVariableBindings(),
                    optRight.getVariableBindings(), newCndRestrictions);
            Restrictions newRightRestrictions = getJoinRestrictions(optRight.getVariableBindings(),
                    optLeft.getVariableBindings(), newCndRestrictions);


            if(newOptReduced == optReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            optReduced = newOptReduced;
            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
        }


        if(optLeft.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(optRight.equals(SqlNoSolution.get()) || optRight.equals(SqlEmptySolution.get())
                || !isJoinable(optLeft, optRight, optConditions))
            return optLeft.optimize(request, restrictions, optReduced, evalServices);

        //FIXME: valid only if it is ensured that optRight has at least one solution
        //if(isJoinConditionAlwaysTrue(optLeft.variables, optRight.getVariables()) && optConditions.isEmpty())
        //    return SqlJoin.join(request, optLeft, optRight).optimize(request, restrictions, optReduced, evalServices);

        if(optLeft instanceof SqlUnion union)
        {
            List<SqlIntercode> unionList = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                List<SqlExpressionIntercode> cnds = optimize(request, optConditions, child.getVariableBindings(),
                        optRight.getVariableBindings(), evalServices);

                unionList.add(leftJoin(request, child, optRight, cnds, restrictions));
            }

            return SqlUnion.union(request, unionList).optimize(request, restrictions, optReduced, evalServices);
        }

        if(optLeft instanceof SqlTableAccess l && optRight instanceof SqlTableAccess r && optConditions.isEmpty())
        {
            DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

            SqlIntercode merge = SqlTableAccess.tryReduceLeftJoin(schema, l, r, restrictions);

            if(merge != null)
                return merge;
        }


        if(optLeft == left && optRight == right && optConditions.equals(conditions)
                && restrictions.isOptimized(bindings))
            return this;

        return leftJoin(request, optLeft, optRight, optConditions, restrictions);
    }


    private static List<SqlExpressionIntercode> optimize(Request request, List<SqlExpressionIntercode> conditions,
            VariableBindings left, VariableBindings right, boolean evalServices)
    {
        VariableBindings bindings = getExpressionVariableBindings(request, left, right);

        List<SqlExpressionIntercode> result = new ArrayList<>(conditions.size());

        conditions.stream().map(f -> f.optimize(request, bindings, new Restriction(xsdBoolean), evalServices))
                .filter(f -> !f.equals(trueValue)).forEach(f -> result.add(f));

        return result;
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
        builder.append(left.translate(request));
        builder.append(" ) AS ");
        builder.append(leftTable);

        builder.append(" LEFT JOIN (");
        builder.append(right.translate(request));
        builder.append(" ) AS ");
        builder.append(rightTable);

        String condition = generateJoinCondition(left.bindings, right.getVariableBindings(), leftTable, rightTable);

        builder.append(" ON ");

        if(condition != null)
            builder.append(condition);

        if(condition != null && !conditions.isEmpty())
            builder.append(" AND ");

        builder.append(conditions.stream().map(c -> c.get(xsdBoolean).get(0).toString()).collect(joining(" AND ")));

        if(condition == null && conditions.isEmpty())
            builder.append("true");

        return builder.toString();
    }


    public final SqlIntercode getLeft()
    {
        return left;
    }


    public final SqlIntercode getRight()
    {
        return right;
    }


    public final List<SqlExpressionIntercode> getConditions()
    {
        return conditions;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return left.hasServiceSubpattern() || right.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("left join");

        for(SqlExpressionIntercode cnd : conditions)
        {
            indentInfo(builder, indent, true);
            cnd.generateExplanation(builder, indent);
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

        if(!(object instanceof SqlLeftJoin imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(conditions, imcode.conditions))
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
        return Objects.hash(conditions, left, right);
    }
}
