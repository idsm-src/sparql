package cz.iocb.sparql.engine.database;

import java.sql.SQLException;



/**
 * Unchecked wrapper of an {@link SQLException} for code paths that cannot declare it.
 */
public class SQLRuntimeException extends RuntimeException
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Wraps the exception.
     *
     * @param exception the wrapped exception
     */
    public SQLRuntimeException(SQLException exception)
    {
        super(exception);
    }
}
