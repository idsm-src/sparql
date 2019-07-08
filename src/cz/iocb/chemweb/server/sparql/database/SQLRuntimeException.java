package cz.iocb.chemweb.server.sparql.database;

import java.sql.SQLException;



public class SQLRuntimeException extends RuntimeException
{
    private static final long serialVersionUID = 1L;


    public SQLRuntimeException(SQLException exception)
    {
        super(exception);
    }
}
