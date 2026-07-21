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
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class UsedVariables
{
    private final LinkedHashMap<String, UsedVariable> usedVariables = new LinkedHashMap<String, UsedVariable>();


    public UsedVariables()
    {
    }


    public UsedVariables(UsedVariables variables)
    {
        this.usedVariables.putAll(variables.usedVariables);
    }


    public UsedVariables restrict(Restrictions restrictions)
    {
        UsedVariables result = new UsedVariables();

        for(Entry<String, UsedVariable> entry : usedVariables.entrySet())
        {
            if(restrictions == null)
            {
                result.add(entry.getValue());
            }
            else
            {
                Map<ResourceClass, List<Column>> map = new HashMap<ResourceClass, List<Column>>();

                if(!restrictions.containsVar(entry.getKey()))
                    continue;

                for(Entry<ResourceClass, List<Column>> e : entry.getValue().getMappings().entrySet())
                    if(restrictions.contains(entry.getKey(), e.getKey()))
                        map.put(e.getKey(), e.getValue());
                    else
                        map.put(e.getKey(), null);

                result.add(new UsedVariable(entry.getKey(), map, entry.getValue().canBeNull()));
            }
        }

        return result;
    }


    public UsedVariable get(String name)
    {
        return usedVariables.get(name);
    }


    public void add(UsedVariable usedVariable)
    {
        usedVariables.put(usedVariable.getName(), usedVariable);
    }


    public void remove(String name)
    {
        usedVariables.remove(name);
    }


    public Set<String> getNames()
    {
        return usedVariables.keySet();
    }


    public Collection<UsedVariable> getValues()
    {
        return usedVariables.values();
    }


    public Set<Column> getNonConstantColumns()
    {
        Set<Column> columns = new HashSet<Column>();

        for(UsedVariable variable : getValues())
            columns.addAll(variable.getNonConstantColumns());

        return columns;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        UsedVariables other = (UsedVariables) object;

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
