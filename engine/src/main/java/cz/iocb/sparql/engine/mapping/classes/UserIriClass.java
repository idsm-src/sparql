package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;



/**
 * IRI class defined by a deployment: IRIs of a recognisable shape whose identifying part is stored in native columns.
 */
public abstract class UserIriClass extends IriClass
{
    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected UserIriClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    /**
     * Relative cost of {@link #match}: 0 for a regular expression test only, 1 when a database lookup may be needed, 2
     * when it always is. The configuration tries cheaper classes first when detecting the class of an IRI.
     *
     * @return relative cost of {@link #match}: 0 for a regular expression test only, 1 when a database lookup may be
     *         needed, 2 when it always is
     */
    public abstract int getCheckCost();


    /**
     * SQL expression concatenating the (non-null) prefix, the value and the (non-null) suffix into a varchar.
     *
     * @param prefix the prefix
     * @param value the value column
     * @param suffix the suffix
     * @return SQL expression concatenating the (non-null) prefix, the value and the (non-null) suffix into a varchar
     */
    protected static Column addPrefixAndSuffix(String prefix, Column value, String suffix)
    {
        if(prefix != null && suffix != null)
            return expression("(%s || %s || %s)::varchar", string(prefix), value, string(suffix));
        else if(prefix != null)
            return expression("(%s || %s)::varchar", string(prefix), value);
        else if(suffix != null)
            return expression("(%s || %s)::varchar", value, string(suffix));
        else
            return value;
    }
}
