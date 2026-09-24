package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Reference to a variable of the surrounding bindings; NULL when the variable is not bound there at all.
 */
public final class SqlVariable extends SqlExpressionIntercode
{
    /**
     * The variable.
     */
    private final Variable variable;


    /**
     * Creates the expression.
     *
     * @param variable the variable
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    private SqlVariable(Variable variable, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, true);

        this.variable = variable;
        this.referencedVariables.add(variable);
    }


    /**
     * Reference to the variable of the binding; NULL for a null binding.
     *
     * @param binding the variable binding
     * @return reference to the variable of the binding; NULL for a null binding
     */
    public static SqlExpressionIntercode create(VariableBinding binding)
    {
        return create(binding, Restriction.ALL);
    }


    /**
     * Reference materialising only the needed classes.
     *
     * @param binding the variable binding
     * @param restriction the result classes the parent needs
     * @return reference materialising only the needed classes
     */
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


    /**
     * The variable.
     *
     * @return the variable
     */
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
    public Set<VirtualTable> getVirtualTables()
    {
        return Set.of();
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(variable);
    }
}
