package cz.iocb.sparql.engine.model.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseElement;



/**
 * Base class for call expressions.
 *
 * <p>
 * Each call expression represents the function to call in a different way, but the list of arguments
 * ({@link #getArguments}) and {@code DISTINCT} ( {@link #isDistinct}) are handled the same way.
 */
abstract class CallExpression extends BaseElement implements Expression
{
    /**
     * True if the call has the DISTINCT modifier (aggregates only).
     */
    private boolean isDistinct;

    /**
     * Arguments in order.
     */
    private List<Expression> arguments;


    /**
     * Creates a call without arguments.
     */
    protected CallExpression()
    {
        this.arguments = new ArrayList<>();
    }


    /**
     * Creates a call with the given arguments.
     *
     * @param arguments the arguments
     */
    protected CallExpression(Collection<Expression> arguments)
    {
        this(false, arguments);
    }


    /**
     * Creates a call with the given arguments and DISTINCT flag.
     *
     * @param isDistinct whether DISTINCT is specified
     * @param arguments the arguments
     */
    protected CallExpression(boolean isDistinct, Collection<Expression> arguments)
    {
        this.isDistinct = isDistinct;
        this.arguments = new ArrayList<>(arguments);
    }


    /**
     * Arguments in order (modifiable).
     *
     * @return arguments in order (modifiable)
     */
    public List<Expression> getArguments()
    {
        return arguments;
    }


    /**
     * True if the call has the DISTINCT modifier.
     *
     * @return true if the call has the DISTINCT modifier, false otherwise
     */
    public boolean isDistinct()
    {
        return isDistinct;
    }


    /**
     * Sets the DISTINCT modifier.
     *
     * @param isDistinct whether DISTINCT is specified
     */
    public void setDistinct(boolean isDistinct)
    {
        this.isDistinct = isDistinct;
    }
}
