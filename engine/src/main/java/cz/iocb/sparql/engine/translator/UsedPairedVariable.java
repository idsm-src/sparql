package cz.iocb.sparql.engine.translator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



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

        if(leftVariable == null)
        {
            for(ResourceClass resClass : rightVariable.getClasses())
                addClasses(null, resClass);
        }
        else if(rightVariable == null)
        {
            for(ResourceClass resClass : leftVariable.getClasses())
                addClasses(resClass, null);
        }
        else
        {
            Set<ResourceClass> leftOthers = new HashSet<ResourceClass>(leftVariable.getClasses());
            Set<ResourceClass> rightOthers = new HashSet<ResourceClass>(rightVariable.getClasses());

            for(ResourceClass leftClass : leftVariable.getClasses())
            {
                for(ResourceClass rightClass : rightVariable.getClasses())
                {
                    if(!ResourceClass.areDisjunct(leftClass, rightClass))
                    {
                        addClasses(leftClass, rightClass);
                        leftOthers.remove(leftClass);
                        rightOthers.remove(rightClass);
                    }
                }
            }

            for(ResourceClass leftClass : leftOthers)
                addClasses(leftClass, null);

            for(ResourceClass rightClass : rightOthers)
                addClasses(null, rightClass);
        }
    }


    public UsedPairedVariable(UsedVariable leftVariable, UsedVariable rightVariable)
    {
        this(null, leftVariable, rightVariable);
    }


    public void addClasses(ResourceClass l, ResourceClass r)
    {
        classes.add(new PairedClass(l, r));
    }


    public static ArrayList<UsedPairedVariable> getPairs(UsedVariables left, UsedVariables right)
    {
        HashSet<String> varNames = new HashSet<String>(left.getNames());
        varNames.retainAll(right.getNames());

        ArrayList<UsedPairedVariable> pairs = new ArrayList<UsedPairedVariable>(varNames.size());

        for(String varName : varNames)
            pairs.add(new UsedPairedVariable(varName, left.get(varName), right.get(varName)));

        return pairs;
    }


    public boolean isJoinable()
    {
        if(leftVariable == null || rightVariable == null)
            return true;

        if(leftVariable.canBeNull() || rightVariable.canBeNull())
            return true;

        for(PairedClass pairedClass : classes)
        {
            if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
            {
                if(pairedClass.getLeftClass().equals(pairedClass.getRightClass()))
                {
                    List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightVariable.getMapping(pairedClass.getLeftClass());

                    /* NOTE: Consider the situation "(X join Y) left join Z". In both X and Z, the variable V may have
                     * resource class R. In Y, however, the variable V cannot have resource class R, and is therefore
                     * eliminated from both X and Z. During optimization, however, isJoinable may be called with
                     * parameters X and Z.
                     */
                    if(leftCols == null || rightCols == null)
                        continue;

                    if(!isJoinable(leftCols, rightCols))
                        continue;
                }

                return true;
            }
        }

        return false;
    }


    private static boolean isJoinable(List<Column> leftCols, List<Column> rightCols)
    {
        for(int i = 0; i < leftCols.size(); i++)
            if(leftCols.get(i) instanceof ConstantColumn && rightCols.get(i) instanceof ConstantColumn
                    && !leftCols.get(i).equals(rightCols.get(i)))
                return false;

        return true;
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
