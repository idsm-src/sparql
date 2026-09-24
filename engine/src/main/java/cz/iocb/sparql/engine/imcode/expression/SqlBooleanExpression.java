package cz.iocb.sparql.engine.imcode.expression;



/**
 * Boolean-valued expression that knows which non-error values it can take.
 */
public interface SqlBooleanExpression
{
    /**
     * Values an expression can take besides an error: never true, never false, or any.
     */
    static enum NonConstantBooleanValue
    {
        /**
         * Never true.
         */
        FALSE_OR_ERROR,

        /**
         * Never false.
         */
        TRUE_OR_ERROR,

        /**
         * Any value.
         */
        ANY
    }


    /**
     * Values the expression can take besides an error.
     *
     * @return values the expression can take besides an error
     */
    public NonConstantBooleanValue getBooleanValue();


    /**
     * True if the expression is never true.
     *
     * @return true if the expression is never true
     */
    public default boolean isFalseOrError()
    {
        return getBooleanValue() == NonConstantBooleanValue.FALSE_OR_ERROR;
    }


    /**
     * True if the expression is never false.
     *
     * @return true if the expression is never false
     */
    public default boolean isTrueOrError()
    {
        return getBooleanValue() == NonConstantBooleanValue.TRUE_OR_ERROR;
    }
}
