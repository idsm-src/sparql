package cz.iocb.sparql.engine.database;



/**
 * SQL type that is not one of the built-in types known to the engine, typically a PostgreSQL user-defined type backing
 * a user datatype. The name is canonicalised the same way as for built-in types, so a user type named by an alias of a
 * built-in type is equal to that built-in type.
 */
public class UserType extends SqlType
{
    /**
     * Creates the type.
     *
     * @param name the type name
     */
    public UserType(String name)
    {
        super(canonicalName(name), builtinJavaClass(name));
    }
}
