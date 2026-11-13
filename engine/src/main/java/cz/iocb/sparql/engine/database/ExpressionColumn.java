package cz.iocb.sparql.engine.database;

import java.util.Objects;



/**
 * Arbitrary SQL expression of a known type. The expression is SQL code (see {@link SqlCode}), so the column keeps the
 * declared tables its expression reads, like the pattern of an existence test, and code written with the column reads
 * them too (see {@link SqlBuilder#append(Object)}). An expression is therefore made of the columns and the code it is
 * composed of, never of their texts.
 */
public final class ExpressionColumn extends Column
{
    /**
     * SQL code of the expression.
     */
    private final SqlCode code;

    /**
     * Whether the expression may evaluate to NULL.
     */
    private final boolean canBeNull;


    /**
     * Creates the expression column with the given nullability. The type is not part of the identity of the column: two
     * columns of the same expression are equal regardless of it.
     *
     * @param code SQL code of the expression
     * @param type SQL type of the expression
     * @param canBeNull whether the value may be null
     */
    public ExpressionColumn(SqlCode code, SqlType type, boolean canBeNull)
    {
        //TODO: check whether the parameter is a valid SQL expression
        super(code.getText(), Objects.requireNonNull(type, "type of expression " + code.getText()));
        this.code = code;
        this.canBeNull = canBeNull;
    }


    /**
     * Creates a possibly-null expression column.
     *
     * @param code SQL code of the expression
     * @param type SQL type of the expression
     */
    public ExpressionColumn(SqlCode code, SqlType type)
    {
        this(code, type, true);
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public SqlCode getCode()
    {
        return code;
    }


    @Override
    public Column fromTable(Table table)
    {
        if(table == null)
            return this;

        throw new UnsupportedOperationException();
    }


    /**
     * True if the expression may evaluate to NULL.
     *
     * @return true if the expression may evaluate to NULL, false otherwise
     */
    @Override
    public boolean canBeNull()
    {
        return canBeNull;
    }
}
