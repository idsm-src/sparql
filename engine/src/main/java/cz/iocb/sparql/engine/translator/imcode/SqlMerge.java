package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlMerge extends SqlIntercode
{
    private final SqlIntercode child;
    private final String variable1;
    private final String variable2;
    private final Map<Column, Column> columnMap;


    protected SqlMerge(UsedVariables variables, String variable1, String variable2, SqlIntercode child,
            Map<Column, Column> columnMap)
    {
        super(variables, child.isDeterministic());

        this.variable1 = variable1;
        this.variable2 = variable2;
        this.child = child;
        this.columnMap = columnMap;
    }


    public static SqlIntercode create(Request request, String variable1, String variable2, SqlIntercode child)
    {
        return create(request, variable1, variable2, child, null);
    }


    protected static SqlIntercode create(Request request, String variable1, String variable2, SqlIntercode child,
            Restrictions restrictions)
    {
        UsedVariables usedVars1 = new UsedVariables();
        UsedVariables usedVars2 = new UsedVariables();

        for(UsedVariable variable : child.getVariables().getValues())
        {
            if(!variable.getName().equals(variable2))
                usedVars1.add(variable);
            else
                usedVars2.add(new UsedVariable(variable1, variable.getMappings(), variable.canBeNull()));
        }

        Map<Column, Column> map = new HashMap<Column, Column>();
        UsedVariables variables = getJoinUsedVariables(request, usedVars1, usedVars2, null, null, restrictions, map);

        return new SqlMerge(variables, variable1, variable2, child, map);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optChild = child;

        Restrictions childRestrictions = getRestrictions(optChild, variable1, variable2, restrictions);

        while(true)
        {
            optChild = child.optimize(request, childRestrictions, reduced, evalServices);

            Restrictions newChildRestrictions = getRestrictions(optChild, variable1, variable2, restrictions);

            if(newChildRestrictions.equals(childRestrictions))
                break;

            childRestrictions = newChildRestrictions;
        }

        if(optChild == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(!(new UsedPairedVariable(optChild.getVariable(variable1), optChild.getVariable(variable2))).isJoinable())
            return SqlNoSolution.get();

        if(optChild.getVariables().get(variable1) == null && !restrictions.containsVar(variable1))
            return optChild.optimize(request, restrictions, reduced, evalServices);

        if(optChild.getVariables().get(variable2) == null)
            return optChild.optimize(request, restrictions, reduced, evalServices);

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
                childs.add(create(request, variable1, variable2, child, restrictions));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(restrictions.isOptimized(variables) && optChild == child)
            return this;

        return create(request, variable1, variable2, optChild, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        UsedVariable usedVariable1 = child.getVariables().get(variable1);
        UsedVariable usedVariable2 = child.getVariables().get(variable2);

        Set<Column> columns = variables.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate(request));
        builder.append(" ) AS ");
        builder.append("tab");

        String condition = generateJoinCondition(usedVariable1, usedVariable2, null, null);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toString();
    }


    protected static Restrictions getRestrictions(SqlIntercode child, String variable1, String variable2,
            Restrictions restrictions)
    {
        UsedVariable var1 = child.getVariable(variable1);
        UsedVariable var2 = child.getVariable(variable2);

        Restrictions result = new Restrictions(restrictions);
        result.add(getJoinRestrictions(var1, var2));
        result.add(getJoinRestrictions(var2, var1));

        return result;
    }


    protected static Restrictions getJoinRestrictions(UsedVariable var, UsedVariable other)
    {
        Restrictions restrictions = new Restrictions();

        if(var == null || other == null)
            return restrictions;

        String name = var.getName();
        Set<ResourceClass> classes = var.getClasses();

        if(var.canBeNull())
            restrictions.set(name, var.getClasses());
        else
            restrictions.set(name, classes.stream().filter(c -> !areDisjunct(c, other.getClasses())).collect(toSet()));

        return restrictions;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("merge ");
        builder.append(variable1);
        builder.append(" and ");
        builder.append(variable2);

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlMerge imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(variable1, imcode.variable1))
            return false;

        if(!Objects.equals(variable2, imcode.variable2))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(variable1, variable2, child);
    }
}
