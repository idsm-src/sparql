package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlVariable extends SqlExpressionIntercode
{
    private final Variable variable;


    private SqlVariable(Variable variable, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, true);

        this.variable = variable;
        this.referencedVariables.add(variable);
    }


    public static SqlExpressionIntercode create(VariableBinding binding)
    {
        return create(binding, Restriction.ALL);
    }


    private static SqlExpressionIntercode create(VariableBinding binding, Restriction restriction)
    {
        if(binding == null)
            return SqlNull.get();

        return new SqlVariable(binding.getVariable(), restriction.restrict(binding.getMappings()), binding.canBeNull());
    }


    @Override
    public Restrictions getRequirements()
    {
        Restrictions restrictions = new Restrictions();

        for(Entry<ResourceClass, List<Column>> e : variableBinding.getMappings().entrySet())
            if(e.getValue() != null)
                restrictions.add(variable, e.getKey());

        return restrictions;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        VariableBinding binding = bindings.get(variable);

        if(binding == null)
            return SqlNull.get();


        if(variableBinding.equals(binding) && restriction.isOptimized(variableBinding))
            return this;

        return create(binding, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(variable);
    }


    public Variable getVariable()
    {
        return variable;
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

        if(!variable.equals(imcode.variable))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(variable);
    }
}
