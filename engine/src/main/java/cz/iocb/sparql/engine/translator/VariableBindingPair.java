package cz.iocb.sparql.engine.translator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;



public class VariableBindingPair
{
    public static class ResourceClassPair
    {
        private final ResourceClass leftClass;
        private final ResourceClass rightClass;

        public ResourceClassPair(ResourceClass leftClass, ResourceClass rightClass)
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


    private final Variable variable;
    private final VariableBinding leftVariableBinding;
    private final VariableBinding rightVariableBinding;
    private final List<ResourceClassPair> classes = new ArrayList<>();


    public VariableBindingPair(Variable variable, VariableBinding leftVariableBinding,
            VariableBinding rightVariableBinding)
    {
        this.variable = variable;
        this.leftVariableBinding = leftVariableBinding;
        this.rightVariableBinding = rightVariableBinding;

        if(leftVariableBinding == null)
        {
            for(ResourceClass resClass : rightVariableBinding.getClasses())
                addClasses(null, resClass);
        }
        else if(rightVariableBinding == null)
        {
            for(ResourceClass resClass : leftVariableBinding.getClasses())
                addClasses(resClass, null);
        }
        else
        {
            Set<ResourceClass> leftOthers = new HashSet<>(leftVariableBinding.getClasses());
            Set<ResourceClass> rightOthers = new HashSet<>(rightVariableBinding.getClasses());

            for(ResourceClass leftClass : leftVariableBinding.getClasses())
            {
                for(ResourceClass rightClass : rightVariableBinding.getClasses())
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


    public VariableBindingPair(VariableBinding leftVariableBinding, VariableBinding rightVariableBinding)
    {
        this(null, leftVariableBinding, rightVariableBinding);
    }


    public void addClasses(ResourceClass l, ResourceClass r)
    {
        classes.add(new ResourceClassPair(l, r));
    }


    public static List<VariableBindingPair> getPairs(VariableBindings left, VariableBindings right)
    {
        Set<Variable> varNames = new HashSet<>(left.getVariables());
        varNames.retainAll(right.getVariables());

        List<VariableBindingPair> pairs = new ArrayList<>(varNames.size());

        for(Variable var : varNames)
            pairs.add(new VariableBindingPair(var, left.get(var), right.get(var)));

        return pairs;
    }


    public boolean isJoinable()
    {
        if(leftVariableBinding == null || rightVariableBinding == null)
            return true;

        if(leftVariableBinding.canBeNull() || rightVariableBinding.canBeNull())
            return true;

        for(ResourceClassPair pairedClass : classes)
        {
            if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
            {
                if(pairedClass.getLeftClass().equals(pairedClass.getRightClass()))
                {
                    List<Column> leftCols = leftVariableBinding.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightVariableBinding.getMapping(pairedClass.getLeftClass());

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


    public final Variable getVariable()
    {
        return variable;
    }


    public final VariableBinding getLeftVariableBinding()
    {
        return leftVariableBinding;
    }


    public final VariableBinding getRightVariableBinding()
    {
        return rightVariableBinding;
    }


    public final List<ResourceClassPair> getClasses()
    {
        return classes;
    }
}
