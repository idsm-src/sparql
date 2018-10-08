package cz.iocb.chemweb.server.sparql.translator;

import java.util.Collection;
import java.util.LinkedHashMap;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



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


    public boolean contains(String name, ResourceClass resClass)
    {
        UsedVariable variable = usedVariables.get(name);

        if(variable == null)
            return false;

        return variable.getClasses().contains(resClass);
    }
}
