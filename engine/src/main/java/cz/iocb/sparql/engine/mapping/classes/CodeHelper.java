package cz.iocb.sparql.engine.mapping.classes;

import java.util.Locale;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;



public class CodeHelper
{
    public static String string(Object object)
    {
        return "'" + object.toString().replaceAll("'", "''") + "'";
    }


    public static Column constant(Object value, String type)
    {
        return new ConstantColumn(value.toString(), type);
    }


    public static Column expression(String format, Object... args)
    {
        return new ExpressionColumn(String.format(Locale.US, format, args), true);
    }
}
