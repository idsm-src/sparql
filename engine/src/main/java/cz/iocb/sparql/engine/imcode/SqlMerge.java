package cz.iocb.sparql.engine.imcode;

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
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlMerge extends SqlIntercode
{
    private final SqlIntercode child;
    private final Variable variable1;
    private final Variable variable2;
    private final Map<Column, Column> columnMap;


    protected SqlMerge(VariableBindings bindings, Variable variable1, Variable variable2, SqlIntercode child,
            Map<Column, Column> columnMap)
    {
        super(bindings, child.isDeterministic());

        this.variable1 = variable1;
        this.variable2 = variable2;
        this.child = child;
        this.columnMap = columnMap;
    }


    public static SqlIntercode create(Request request, Variable variable1, Variable variable2, SqlIntercode child)
    {
        return create(request, variable1, variable2, child, null);
    }


    protected static SqlIntercode create(Request request, Variable variable1, Variable variable2, SqlIntercode child,
            Restrictions restrictions)
    {
        VariableBindings bindings1 = new VariableBindings();
        VariableBindings bindings2 = new VariableBindings();

        for(VariableBinding binding : child.getVariableBindings().getValues())
        {
            if(!binding.getVariable().equals(variable2))
                bindings1.add(binding);
            else
                bindings2.add(new VariableBinding(variable1, binding.getMappings(), binding.canBeNull()));
        }

        Map<Column, Column> map = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, bindings1, bindings2, null, null, restrictions,
                map);

        return new SqlMerge(bindings, variable1, variable2, child, map);
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

        if(optChild.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(!(new VariableBindingPair(optChild.getVariable(variable1), optChild.getVariable(variable2))).isJoinable())
            return SqlNoSolution.get();

        if(optChild.getVariableBindings().get(variable1) == null && !restrictions.containsVar(variable1))
            return optChild.optimize(request, restrictions, reduced, evalServices);

        if(optChild.getVariableBindings().get(variable2) == null)
            return optChild.optimize(request, restrictions, reduced, evalServices);

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                childs.add(create(request, variable1, variable2, child, restrictions));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(restrictions.isOptimized(bindings) && optChild == child)
            return this;

        return create(request, variable1, variable2, optChild, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        VariableBinding binding1 = child.getVariableBindings().get(variable1);
        VariableBinding binding2 = child.getVariableBindings().get(variable2);

        Set<Column> columns = bindings.getNonConstantColumns();

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

        String condition = generateJoinCondition(binding1, binding2, null, null);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toString();
    }


    protected static Restrictions getRestrictions(SqlIntercode child, Variable variable1, Variable variable2,
            Restrictions restrictions)
    {
        VariableBinding binding1 = child.getVariable(variable1);
        VariableBinding binding2 = child.getVariable(variable2);

        Restrictions result = new Restrictions(restrictions);
        result.add(getJoinRestrictions(binding1, binding2));
        result.add(getJoinRestrictions(binding2, binding1));

        return result;
    }


    protected static Restrictions getJoinRestrictions(VariableBinding binding, VariableBinding other)
    {
        Restrictions restrictions = new Restrictions();

        if(binding == null || other == null)
            return restrictions;

        Variable variable = binding.getVariable();
        Set<ResourceClass> classes = binding.getClasses();

        if(binding.canBeNull())
            restrictions.set(variable, binding.getClasses());
        else
            restrictions.set(variable,
                    classes.stream().filter(c -> !areDisjunct(c, other.getClasses())).collect(toSet()));

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
