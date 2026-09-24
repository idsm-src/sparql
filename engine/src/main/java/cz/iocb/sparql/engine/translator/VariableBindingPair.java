package cz.iocb.sparql.engine.translator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Bindings of one variable on the two sides of a join, with the pairs of overlapping classes; a class with no
 * overlapping counterpart is paired with null.
 */
public class VariableBindingPair
{
    /**
     * Pair of a left and a right class that may hold a common value (either may be null).
     */
    public static class ResourceClassPair
    {
        /**
         * Class on the left side, or null.
         */
        private final ResourceClass leftClass;

        /**
         * Class on the right side, or null.
         */
        private final ResourceClass rightClass;

        /**
         * Creates the pair.
         *
         * @param leftClass class on the left side, or null
         * @param rightClass class on the right side, or null
         */
        public ResourceClassPair(ResourceClass leftClass, ResourceClass rightClass)
        {
            this.leftClass = leftClass;
            this.rightClass = rightClass;
        }


        /**
         * Class on the left side, or null.
         *
         * @return class on the left side, or null
         */
        public final ResourceClass getLeftClass()
        {
            return leftClass;
        }


        /**
         * Class on the right side, or null.
         *
         * @return class on the right side, or null
         */
        public final ResourceClass getRightClass()
        {
            return rightClass;
        }
    }


    /**
     * The variable; may be null.
     */
    private final Variable variable;

    /**
     * Binding on the left side, or null.
     */
    private final VariableBinding leftVariableBinding;

    /**
     * Binding on the right side, or null.
     */
    private final VariableBinding rightVariableBinding;

    /**
     * Pairs of overlapping classes, unmatched classes paired with null.
     */
    private final List<ResourceClassPair> classes = new ArrayList<>();


    /**
     * Creates the pair, pairing every left class with every overlapping right class.
     *
     * @param variable the variable
     * @param leftVariableBinding binding on the left side, or null
     * @param rightVariableBinding binding on the right side, or null
     */
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


    /**
     * Creates the pair without naming the variable.
     *
     * @param leftVariableBinding binding on the left side, or null
     * @param rightVariableBinding binding on the right side, or null
     */
    public VariableBindingPair(VariableBinding leftVariableBinding, VariableBinding rightVariableBinding)
    {
        this(null, leftVariableBinding, rightVariableBinding);
    }


    /**
     * Adds a class pair.
     *
     * @param l class on the left side
     * @param r class on the right side
     */
    public void addClasses(ResourceClass l, ResourceClass r)
    {
        classes.add(new ResourceClassPair(l, r));
    }


    /**
     * Pairs for the variables bound on both sides.
     *
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @return pairs for the variables bound on both sides
     */
    public static List<VariableBindingPair> getPairs(VariableBindings left, VariableBindings right)
    {
        Set<Variable> varNames = new HashSet<>(left.getVariables());
        varNames.retainAll(right.getVariables());

        List<VariableBindingPair> pairs = new ArrayList<>(varNames.size());

        for(Variable var : varNames)
            pairs.add(new VariableBindingPair(var, left.get(var), right.get(var)));

        return pairs;
    }


    /**
     * False if the variable can never take equal values on both sides, so the join is empty: both sides are always
     * bound, and every pair of overlapping classes is contradicted by differing constants.
     *
     * @return false if the variable can never take equal values on both sides, so the join is empty, true otherwise
     */
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


    /**
     * False if the columns differ in a constant.
     *
     * @param leftCols columns on the left side
     * @param rightCols columns on the right side
     * @return false if the columns differ in a constant, true otherwise
     */
    private static boolean isJoinable(List<Column> leftCols, List<Column> rightCols)
    {
        for(int i = 0; i < leftCols.size(); i++)
            if(leftCols.get(i) instanceof ConstantColumn && rightCols.get(i) instanceof ConstantColumn
                    && !leftCols.get(i).equals(rightCols.get(i)))
                return false;

        return true;
    }


    /**
     * The variable; may be null.
     *
     * @return the variable; may be null
     */
    public final Variable getVariable()
    {
        return variable;
    }


    /**
     * Binding on the left side, or null.
     *
     * @return binding on the left side, or null
     */
    public final VariableBinding getLeftVariableBinding()
    {
        return leftVariableBinding;
    }


    /**
     * Binding on the right side, or null.
     *
     * @return binding on the right side, or null
     */
    public final VariableBinding getRightVariableBinding()
    {
        return rightVariableBinding;
    }


    /**
     * Pairs of overlapping classes, unmatched classes paired with null.
     *
     * @return pairs of overlapping classes, unmatched classes paired with null
     */
    public final List<ResourceClassPair> getClasses()
    {
        return classes;
    }
}
