package cz.iocb.chemweb.server.sparql.translator.sql;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public class UsedPairedVariable
{
    public static class PairedClass
    {
        private final ResourceClass leftClass;
        private final ResourceClass rightClass;


        public PairedClass(ResourceClass leftClass, ResourceClass rightClass)
        {
            this.leftClass = leftClass;
            this.rightClass = rightClass;
        }


        public final ResourceClass getLeftClass()
        {
            return leftClass;
        }


        public final ResourceClass getRightClass()
        {
            return rightClass;
        }
    }


    private final String name;
    private final UsedVariable leftVariable;
    private final UsedVariable rightVariable;
    private final List<PairedClass> classes = new ArrayList<PairedClass>();


    public UsedPairedVariable(String name, UsedVariable leftVariable, UsedVariable rightVariable)
    {
        this.name = name;
        this.leftVariable = leftVariable;
        this.rightVariable = rightVariable;
    }


    public void addClasses(ResourceClass l, ResourceClass r)
    {
        classes.add(new PairedClass(l, r));
    }


    public final String getName()
    {
        return name;
    }


    public final UsedVariable getLeftVariable()
    {
        return leftVariable;
    }


    public final UsedVariable getRightVariable()
    {
        return rightVariable;
    }


    public final List<PairedClass> getClasses()
    {
        return classes;
    }
}
