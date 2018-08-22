package cz.iocb.chemweb.server.sparql.translator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResourceClass;



public class UsedVariable
{
    private final String name;
    private final boolean canBeNull;
    private final Set<PatternResourceClass> classes = new HashSet<PatternResourceClass>();


    public UsedVariable(String name, Set<PatternResourceClass> mapClasses, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
        this.classes.addAll(mapClasses);
    }


    public UsedVariable(String name, PatternResourceClass mapClass, boolean canBeNull)
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


    public void addClass(PatternResourceClass mapClass)
    {
        classes.add(mapClass);
    }


    public void addClasses(List<PatternResourceClass> mapClasses)
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


    public final Set<PatternResourceClass> getClasses()
    {
        return classes;
    }
}
