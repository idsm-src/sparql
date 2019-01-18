package cz.iocb.chemweb.server.sparql.translator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class UsedVariable
{
    private final String name;
    private final boolean canBeNull;
    private final Set<ResourceClass> classes = new HashSet<ResourceClass>();


    public UsedVariable(String name, Set<ResourceClass> mapClasses, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
        this.classes.addAll(mapClasses);
    }


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


    public void addClasses(List<ResourceClass> mapClasses)
    {
        classes.addAll(mapClasses);
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


    public final Set<ResourceClass> getClasses()
    {
        return classes;
    }


    public final Set<ResourceClass> getCompatible(ResourceClass resClass)
    {
        return classes.stream().filter(r -> r == resClass || r.getGeneralClass() == resClass)
                .collect(Collectors.toSet());
    }
}
