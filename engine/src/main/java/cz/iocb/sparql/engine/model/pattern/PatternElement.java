package cz.iocb.sparql.engine.model.pattern;

import java.util.LinkedHashSet;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.BaseElement;



/**
 * Base class for patterns; subclasses fill {@link #variablesInScope} in their constructors.
 */
public abstract class PatternElement extends BaseElement
{
    /**
     * Variables the pattern brings into scope, in order of first occurrence.
     */
    protected final LinkedHashSet<VariableNode> variablesInScope = new LinkedHashSet<>();


    /**
     * Creates the pattern element.
     */
    protected PatternElement()
    {
    }


    /**
     * Variables the pattern brings into scope, in order of first occurrence.
     *
     * @return variables the pattern brings into scope, in order of first occurrence
     */
    public LinkedHashSet<VariableNode> getVariablesInScope()
    {
        return variablesInScope;
    }
}
