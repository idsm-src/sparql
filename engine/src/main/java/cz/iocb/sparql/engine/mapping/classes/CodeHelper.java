package cz.iocb.sparql.engine.mapping.classes;

import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.ValueColumn;



/**
 * Helpers for assembling SQL fragments in resource classes.
 */
public class CodeHelper
{
    /**
     * Not instantiable.
     */
    private CodeHelper()
    {
    }


    /**
     * Single-quoted SQL string literal of the object's text.
     *
     * @param object the value to quote
     * @return single-quoted SQL string literal of the object's text
     */
    public static String string(Object object)
    {
        return "'" + object.toString().replaceAll("'", "''") + "'";
    }


    /**
     * Typed constant column {@code 'value'::type}.
     *
     * @param value the constant value
     * @param type the SQL type
     * @return typed constant column {@code 'value'::type}
     */
    public static Column constant(Object value, SqlType type)
    {
        return new ValueColumn(value.toString(), type);
    }


    /**
     * Expression column of the given type whose SQL code is made of the format string and the arguments (see
     * {@link SqlCode#format}): the columns and the code among the arguments are written by their SQL texts, and the
     * expression reads the declared tables they read.
     *
     * @param type SQL type of the expression
     * @param format the format string
     * @param args the format arguments, pieces of SQL
     * @return expression column of the given type
     */
    public static Column expression(SqlType type, String format, Object... args)
    {
        return new ExpressionColumn(SqlCode.format(format, args), type, true);
    }
}
