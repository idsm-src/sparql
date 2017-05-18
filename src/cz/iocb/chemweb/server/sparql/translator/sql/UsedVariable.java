package cz.iocb.chemweb.server.sparql.translator.sql;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class UsedVariable
{
    private final String name;
    private final boolean canBeNull;
    private final List<ResourceClass> classes = new ArrayList<ResourceClass>();


    public UsedVariable(String name, ResourceClass mapClass, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
        this.classes.add(mapClass);
    }


    public UsedVariable(String name, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
    }


    public void addClass(ResourceClass mapClass)
    {
        classes.add(mapClass);
    }


    public boolean containsClass(ResourceClass mapClass)
    {
        for(ResourceClass map : classes)
            if(map == mapClass)
                return true;

        return false;
    }


    public final String getName()
    {
        return name;
    }


    public final boolean canBeNull()
    {
        return canBeNull;
    }


    public final List<ResourceClass> getClasses()
    {
        return classes;
    }
}
