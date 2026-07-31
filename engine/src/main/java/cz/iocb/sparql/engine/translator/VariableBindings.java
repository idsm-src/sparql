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



public class VariableBindings
{
    private final LinkedHashMap<Variable, VariableBinding> usedVariables = new LinkedHashMap<>();


    public VariableBindings()
    {
    }


    public VariableBindings(VariableBindings bindings)
    {
        this.usedVariables.putAll(bindings.usedVariables);
    }


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


    public VariableBinding get(Variable variable)
    {
        return usedVariables.get(variable);
    }


    public void add(VariableBinding binding)
    {
        usedVariables.put(binding.getVariable(), binding);
    }


    public void remove(Variable variable)
    {
        usedVariables.remove(variable);
    }


    public Set<Variable> getVariables()
    {
        return usedVariables.keySet();
    }


    public Collection<VariableBinding> getValues()
    {
        return usedVariables.values();
    }


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
