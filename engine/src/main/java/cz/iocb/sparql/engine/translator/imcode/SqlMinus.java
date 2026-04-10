package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlMinus extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final SqlIntercode left;
    private final SqlIntercode right;


    protected SqlMinus(UsedVariables variables, SqlIntercode left, SqlIntercode right)
    {
        super(variables, left.isDeterministic() && right.isDeterministic());

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
        return new SqlMinus(left.getVariables().restrict(restrictions), left, right);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;

        boolean leftReduced = reduced & optRight.isDeterministic;
        Restrictions leftRestrictions = getJoinRestrictions(optLeft.getVariables(), optRight.getVariables(),
                restrictions);
        Restrictions rightRestrictions = getJoinRestrictions(optRight.getVariables(), optLeft.getVariables(),
                new Restrictions());

        while(true)
        {
            optLeft = optLeft.optimize(request, leftRestrictions, leftReduced, evalServices);
            optRight = optRight.optimize(request, rightRestrictions, true, evalServices);

            if(optRight instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

                for(SqlIntercode child : union.getChilds())
                    if(isJoinable(optLeft, child))
                        unionList.add(child);

                if(!unionList.equals(union.getChilds()))
                    optRight = SqlUnion.union(request, unionList).optimize(request, restrictions, true, evalServices);
            }

            boolean newLeftReduced = reduced & optRight.isDeterministic;
            Restrictions newLeftRestrictions = getJoinRestrictions(optLeft.getVariables(), optRight.getVariables(),
                    restrictions);
            Restrictions newRightRestrictions = getJoinRestrictions(optRight.getVariables(), optLeft.getVariables(),
                    new Restrictions());



            if(newLeftReduced == leftReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
            leftReduced = newLeftReduced;
        }


        boolean shareVariables = false;

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(optLeft.getVariables(), optRight.getVariables()))
        {
            if(pair.getLeftVariable() != null && pair.getRightVariable() != null)
                shareVariables = true;

            if(!pair.isJoinable())
                return optLeft.optimize(request, restrictions, reduced, evalServices);
        }

        if(shareVariables == false)
            return optLeft.optimize(request, restrictions, reduced, evalServices);

        if(optLeft instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
                childs.add(minus(request, child, optRight, restrictions));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(restrictions.isOptimized(variables) && optLeft == left && optRight == right)
            return this;

        return minus(request, optLeft, optRight, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

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

        String condition = generateCondition(left.getVariables(), right.getVariables(), leftTable, rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(")");

        return builder.toString();
    }


    private String generateCondition(UsedVariables left, UsedVariables right, Table leftTable, Table rightTable)
    {
        String joinCondition = generateJoinCondition(left, right, leftTable, rightTable);

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left, right);

        for(UsedPairedVariable pair : pairs)
            if(pair.getLeftVariable() != null && pair.getRightVariable() != null)
                if(!pair.getLeftVariable().canBeNull() && !pair.getRightVariable().canBeNull())
                    return joinCondition;


        List<String> condition = new ArrayList<String>();

        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            StringBuilder builder = new StringBuilder();

            if(leftVariable.canBeNull())
            {
                Set<Column> columns = leftVariable.getNonConstantColumns();
                assert !columns.isEmpty(); //NOTE: the variable can be null => no column can be constant

                builder.append("(");
                builder.append(columns.stream().map(c -> c.fromTable(leftTable) + " IS NOT NULL").sorted()
                        .collect(joining(" OR ")));
                builder.append(")");
            }

            if(leftVariable.canBeNull() && rightVariable.canBeNull())
                builder.append(" AND ");

            if(rightVariable.canBeNull())
            {
                Set<Column> columns = rightVariable.getNonConstantColumns();
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
    public boolean isDistinct(Request request, Collection<String> selected)
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
