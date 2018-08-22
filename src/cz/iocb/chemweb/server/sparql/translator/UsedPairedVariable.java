package cz.iocb.chemweb.server.sparql.translator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;



public class UsedPairedVariable
{
    public static class PairedClass
    {
        private final PatternResourceClass leftClass;
        private final PatternResourceClass rightClass;


        public PairedClass(PatternResourceClass leftClass, PatternResourceClass rightClass)
        {
            this.leftClass = leftClass;
            this.rightClass = rightClass;
        }


        public final PatternResourceClass getLeftClass()
        {
            return leftClass;
        }


        public final PatternResourceClass getRightClass()
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


    public void addClasses(PatternResourceClass l, PatternResourceClass r)
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
                for(PatternResourceClass resClass : rightVar.getClasses())
                    paired.addClasses(null, resClass);
            }
            else if(rightVar == null)
            {
                for(PatternResourceClass resClass : leftVar.getClasses())
                    paired.addClasses(resClass, null);
            }
            else
            {
                LinkedHashSet<PatternResourceClass> classes = new LinkedHashSet<PatternResourceClass>();

                for(PatternResourceClass resClass : leftVar.getClasses())
                    classes.add(resClass);

                for(PatternResourceClass resClass : rightVar.getClasses())
                    classes.add(resClass);

                for(PatternResourceClass resClass : classes)
                {
                    PatternResourceClass l = leftVar.containsClass(resClass) ? resClass : null;
                    PatternResourceClass r = rightVar.containsClass(resClass) ? resClass : null;

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
