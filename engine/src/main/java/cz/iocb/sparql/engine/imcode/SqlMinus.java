package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlMinus extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final SqlIntercode left;
    private final SqlIntercode right;


    protected SqlMinus(VariableBindings bindings, SqlIntercode left, SqlIntercode right)
    {
        super(bindings, left.isDeterministic() && right.isDeterministic());

        this.left = left;
        this.right = right;
    }


    public static SqlIntercode minus(Request request, SqlIntercode left, SqlIntercode right)
    {
        return minus(request, left, right, null);
    }


    protected static SqlIntercode minus(Request request, SqlIntercode left, SqlIntercode right,
            Restrictions restrictions)
    {
        return new SqlMinus(left.getVariableBindings().restrict(restrictions), left, right);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;

        boolean leftReduced = reduced & optRight.isDeterministic;
        Restrictions leftRestrictions = getJoinRestrictions(optLeft.getVariableBindings(),
                optRight.getVariableBindings(), restrictions);
        Restrictions rightRestrictions = getJoinRestrictions(optRight.getVariableBindings(),
                optLeft.getVariableBindings(), new Restrictions());

        while(true)
        {
            optLeft = optLeft.optimize(request, leftRestrictions, leftReduced, evalServices);
            optRight = optRight.optimize(request, rightRestrictions, true, evalServices);

            if(optRight instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<>();

                for(SqlIntercode child : union.getChilds())
                    if(isJoinable(optLeft, child))
                        unionList.add(child);

                if(!unionList.equals(union.getChilds()))
                    optRight = SqlUnion.union(request, unionList).optimize(request, restrictions, true, evalServices);
            }

            boolean newLeftReduced = reduced & optRight.isDeterministic;
            Restrictions newLeftRestrictions = getJoinRestrictions(optLeft.getVariableBindings(),
                    optRight.getVariableBindings(), restrictions);
            Restrictions newRightRestrictions = getJoinRestrictions(optRight.getVariableBindings(),
                    optLeft.getVariableBindings(), new Restrictions());



            if(newLeftReduced == leftReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
            leftReduced = newLeftReduced;
        }


        boolean shareVariables = false;

        for(VariableBindingPair pair : VariableBindingPair.getPairs(optLeft.getVariableBindings(),
                optRight.getVariableBindings()))
        {
            if(pair.getLeftVariableBinding() != null && pair.getRightVariableBinding() != null)
                shareVariables = true;

            if(!pair.isJoinable())
                return optLeft.optimize(request, restrictions, reduced, evalServices);
        }

        if(!shareVariables)
            return optLeft.optimize(request, restrictions, reduced, evalServices);

        if(optLeft instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                childs.add(minus(request, child, optRight, restrictions));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(restrictions.isOptimized(bindings) && optLeft == left && optRight == right)
            return this;

        return minus(request, optLeft, optRight, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(left.translate(request));
        builder.append(") AS ");
        builder.append(leftTable);

        builder.append(" WHERE NOT EXISTS (SELECT 1 FROM (");
        builder.append(right.translate(request));
        builder.append(") AS ");
        builder.append(rightTable);

        String condition = generateCondition(left.getVariableBindings(), right.getVariableBindings(), leftTable,
                rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(")");

        return builder.toString();
    }


    private String generateCondition(VariableBindings left, VariableBindings right, Table leftTable, Table rightTable)
    {
        String joinCondition = generateJoinCondition(left, right, leftTable, rightTable);

        List<VariableBindingPair> pairs = VariableBindingPair.getPairs(left, right);

        for(VariableBindingPair pair : pairs)
            if(pair.getLeftVariableBinding() != null && pair.getRightVariableBinding() != null)
                if(!pair.getLeftVariableBinding().canBeNull() && !pair.getRightVariableBinding().canBeNull())
                    return joinCondition;


        List<String> condition = new ArrayList<>();

        for(VariableBindingPair pair : pairs)
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            StringBuilder builder = new StringBuilder();

            if(leftBinding.canBeNull())
            {
                Set<Column> columns = leftBinding.getNonConstantColumns();
                assert !columns.isEmpty(); //NOTE: the variable can be null => no column can be constant

                builder.append("(");
                builder.append(columns.stream().map(c -> c.fromTable(leftTable) + " IS NOT NULL").sorted()
                        .collect(joining(" OR ")));
                builder.append(")");
            }

            if(leftBinding.canBeNull() && rightBinding.canBeNull())
                builder.append(" AND ");

            if(rightBinding.canBeNull())
            {
                Set<Column> columns = rightBinding.getNonConstantColumns();
                assert !columns.isEmpty(); //NOTE: the variable can be null => no column can be constant

                builder.append("(");
                builder.append(columns.stream().map(c -> c.fromTable(rightTable) + " IS NOT NULL").sorted()
                        .collect(joining(" OR ")));
                builder.append(")");
            }

            condition.add(builder.toString());
        }

        String domCondition = condition.stream().sorted().collect(joining(" OR "));

        if(domCondition.isEmpty())
            domCondition = "false";


        if(joinCondition == null)
            return domCondition;
        else
            return "(" + joinCondition + ") AND (" + domCondition + ")";
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return left.isDistinct(request, selected);
    }


    public final SqlIntercode getLeft()
    {
        return left;
    }


    public final SqlIntercode getRight()
    {
        return right;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return left.hasServiceSubpattern() || right.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("minus");

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

        if(!(object instanceof SqlMinus imcode))
            return false;

        if(!super.equals(imcode))
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
        return Objects.hash(left, right);
    }
}
