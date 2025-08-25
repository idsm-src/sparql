package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public final class SqlVariable extends SqlNodeValue
{
    private final UsedVariable variable;


    protected SqlVariable(UsedVariable variable, Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull);
        this.variable = variable;

        this.referencedVariables.add(variable.getName());
    }


    public static SqlExpressionIntercode create(String variable, UsedVariables variables)
    {
        UsedVariable usedVariable = variables.get(variable);

        if(usedVariable == null)
            return SqlNull.get();

        return new SqlVariable(usedVariable, usedVariable.getClasses(), usedVariable.canBeNull());
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        Restrictions restrictions = new Restrictions();

        if(expected == null)
            restrictions.add(variable.getName());
        else
            restrictions.add(variable.getName(), expected);

        return restrictions;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        if(variables.get(variable.getName()) == null)
            return SqlNull.get();

        if(variable.equals(variables.get(variable.getName())))
            return this;

        return create(variable.getName(), variables);
    }


    @Override
    public String translate(Request request)
    {
        if(isBoxed())
            return translateAsBoxedOperand(request, this, getResourceClasses());
        else
            return translateAsUnboxedOperand(request, this, getExpressionResourceClass());
    }


    Column getExpressionValue(ResourceClass resourceClass)
    {
        return resourceClass.toExpression(variable.getMapping(resourceClass));
    }


    @Override
    public List<Column> asResource(Request request, ResourceClass resourceClass)
    {
        return variable.toResource(resourceClass);
    }


    public String getName()
    {
        return variable.getName();
    }


    public UsedVariable getUsedVariable()
    {
        return variable;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(variable.getName());
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlVariable imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(variable, imcode.variable))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(variable);
    }
}
