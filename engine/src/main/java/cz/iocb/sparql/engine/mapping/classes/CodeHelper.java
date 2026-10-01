package cz.iocb.sparql.engine.mapping.classes;

import java.util.Locale;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
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
     * Expression column of the given type formatted with {@link String#format} (US locale).
     *
     * @param type SQL type of the expression
     * @param format the format string
     * @param args the format arguments
     * @return expression column of the given type formatted with {@link String#format} (US locale)
     */
    public static Column expression(SqlType type, String format, Object... args)
    {
        return new ExpressionColumn(String.format(Locale.US, format, args), type, true);
    }
}
