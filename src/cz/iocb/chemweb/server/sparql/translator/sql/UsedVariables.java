package cz.iocb.chemweb.server.sparql.translator.sql;

import java.util.Collection;
import java.util.LinkedHashMap;



public class UsedVariables
{
    private final LinkedHashMap<String, UsedVariable> usedVariables = new LinkedHashMap<String, UsedVariable>();


    public void add(UsedVariable usedVariable)
    {
        assert usedVariables.get(usedVariable.getName()) == null;
        usedVariables.put(usedVariable.getName(), usedVariable);
    }


    public UsedVariable get(String name)
    {
        return usedVariables.get(name);
    }


    public Collection<UsedVariable> getValues()
    {
        return usedVariables.values();
    }
}
