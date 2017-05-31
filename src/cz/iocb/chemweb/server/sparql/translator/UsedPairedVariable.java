package cz.iocb.chemweb.server.sparql.translator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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


    public static ArrayList<UsedPairedVariable> getPairs(UsedVariables left, UsedVariables right)
    {
        LinkedHashSet<String> varNames = new LinkedHashSet<String>();

        for(UsedVariable variable : left.getValues())
            varNames.add(variable.getName());

        for(UsedVariable variable : right.getValues())
            varNames.add(variable.getName());


        ArrayList<UsedPairedVariable> pairs = new ArrayList<UsedPairedVariable>();

        for(String varName : varNames)
        {
            UsedVariable leftVar = left.get(varName);
            UsedVariable rightVar = right.get(varName);


            UsedPairedVariable paired = new UsedPairedVariable(varName, leftVar, rightVar);

            if(leftVar == null)
            {
                for(ResourceClass resClass : rightVar.getClasses())
                    paired.addClasses(null, resClass);
            }
            else if(rightVar == null)
            {
                for(ResourceClass resClass : leftVar.getClasses())
                    paired.addClasses(resClass, null);
            }
            else
            {
                LinkedHashSet<ResourceClass> classes = new LinkedHashSet<ResourceClass>();

                for(ResourceClass resClass : leftVar.getClasses())
                    classes.add(resClass);

                for(ResourceClass resClass : rightVar.getClasses())
                    classes.add(resClass);

                for(ResourceClass resClass : classes)
                {
                    ResourceClass l = leftVar.containsClass(resClass) ? resClass : null;
                    ResourceClass r = rightVar.containsClass(resClass) ? resClass : null;

                    paired.addClasses(l, r);
                }
            }

            pairs.add(paired);
        }

        return pairs;
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
