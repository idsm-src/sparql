package cz.iocb.sparql.engine.translator.imcode.expression;

public interface SqlBooleanExpression
{
    static enum NonConstantBooleanValue
    {
        FALSE_OR_ERROR, TRUE_OR_ERROR, ANY
    }


    public NonConstantBooleanValue getBooleanValue();


    public default boolean isFalseOrError()
    {
        return getBooleanValue() == NonConstantBooleanValue.FALSE_OR_ERROR;
    }


    public default boolean isTrueOrError()
    {
        return getBooleanValue() == NonConstantBooleanValue.TRUE_OR_ERROR;
    }
}
