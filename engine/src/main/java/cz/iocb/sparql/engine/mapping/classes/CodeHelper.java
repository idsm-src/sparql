package cz.iocb.sparql.engine.mapping.classes;

import java.util.Locale;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;



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
    public static Column constant(Object value, String type)
    {
        return new ConstantColumn(value.toString(), type);
    }


    /**
     * Expression column formatted with {@link String#format} (US locale).
     *
     * @param format the format string
     * @param args the format arguments
     * @return expression column formatted with {@link String#format} (US locale)
     */
    public static Column expression(String format, Object... args)
    {
        return new ExpressionColumn(String.format(Locale.US, format, args), true);
    }
}
