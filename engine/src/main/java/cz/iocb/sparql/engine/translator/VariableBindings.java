package cz.iocb.sparql.engine.translator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Set of {@link VariableBinding}s of a piece of intermediate code, keyed by variable and kept in insertion order.
 */
public class VariableBindings
{
    /**
     * Bindings by variable, in insertion order.
     */
    private final LinkedHashMap<Variable, VariableBinding> usedVariables = new LinkedHashMap<>();


    /**
     * Creates empty bindings.
     */
    public VariableBindings()
    {
    }


    /**
     * Copy constructor.
     *
     * @param bindings the variable bindings
     */
    public VariableBindings(VariableBindings bindings)
    {
        this.usedVariables.putAll(bindings.usedVariables);
    }


    /**
     * Bindings narrowed to what the parent needs: variables outside the restrictions are dropped, and the columns of
     * classes the parent does not need are set to null (the class stays known).
     *
     * @param restrictions what the parent needs of the variables
     * @return bindings narrowed to what the parent needs: variables outside the restrictions are dropped, and the
     *         columns of classes the parent does not need are set to null (the class stays known)
     */
    public VariableBindings restrict(Restrictions restrictions)
    {
        VariableBindings result = new VariableBindings();

        for(Entry<Variable, VariableBinding> entry : usedVariables.entrySet())
        {
            if(restrictions == null)
            {
                result.add(entry.getValue());
            }
            else
            {
                Map<ResourceClass, List<Column>> map = new HashMap<>();

                if(!restrictions.containsVar(entry.getKey()))
                    continue;

                for(Entry<ResourceClass, List<Column>> e : entry.getValue().getMappings().entrySet())
                    if(restrictions.contains(entry.getKey(), e.getKey()))
                        map.put(e.getKey(), e.getValue());
                    else
                        map.put(e.getKey(), null);

                result.add(new VariableBinding(entry.getKey(), map, entry.getValue().canBeNull()));
            }
        }

        return result;
    }


    /**
     * Binding of the variable, or null.
     *
     * @param variable the variable
     * @return binding of the variable, or null
     */
    public VariableBinding get(Variable variable)
    {
        return usedVariables.get(variable);
    }


    /**
     * Adds or replaces the binding of its variable.
     *
     * @param binding the variable binding
     */
    public void add(VariableBinding binding)
    {
        usedVariables.put(binding.getVariable(), binding);
    }


    /**
     * Removes the binding of the variable.
     *
     * @param variable the variable
     */
    public void remove(Variable variable)
    {
        usedVariables.remove(variable);
    }


    /**
     * The bound variables.
     *
     * @return the bound variables
     */
    public Set<Variable> getVariables()
    {
        return usedVariables.keySet();
    }


    /**
     * The bindings.
     *
     * @return the bindings
     */
    public Collection<VariableBinding> getValues()
    {
        return usedVariables.values();
    }


    /**
     * Non-constant columns used by any binding.
     *
     * @return non-constant columns used by any binding
     */
    public Set<Column> getNonConstantColumns()
    {
        Set<Column> columns = new HashSet<>();

        for(VariableBinding binding : getValues())
            columns.addAll(binding.getNonConstantColumns());

        return columns;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        VariableBindings other = (VariableBindings) object;

        if(!usedVariables.equals(other.usedVariables))
            return false;

        return true;
    }


    @Override
    public int hashCode()
    {
        return usedVariables.hashCode();
    }
}
