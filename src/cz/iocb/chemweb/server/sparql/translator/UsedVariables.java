package cz.iocb.chemweb.server.sparql.translator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class UsedVariables
{
    private final LinkedHashMap<String, UsedVariable> usedVariables = new LinkedHashMap<String, UsedVariable>();


    public UsedVariables restrict(HashSet<String> restrictions)
    {
        UsedVariables result = new UsedVariables();

        for(Entry<String, UsedVariable> entry : usedVariables.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey()))
                result.add(entry.getValue());

        return result;
    }


    public void add(UsedVariable usedVariable)
    {
        assert usedVariables.get(usedVariable.getName()) == null;
        usedVariables.put(usedVariable.getName(), usedVariable);
    }


    public UsedVariable get(String name)
    {
        return usedVariables.get(name);
    }


    public Set<String> getNames()
    {
        return usedVariables.keySet();
    }


    public Collection<UsedVariable> getValues()
    {
        return usedVariables.values();
    }


    public boolean contains(String name, ResourceClass resClass)
    {
        UsedVariable variable = usedVariables.get(name);

        if(variable == null)
            return false;

        return variable.getClasses().contains(resClass);
    }
}
