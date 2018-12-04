package cz.iocb.chemweb.server.db;

import java.sql.SQLException;



public class SQLRuntimeException extends RuntimeException
{
    private static final long serialVersionUID = 1L;


    public SQLRuntimeException(SQLException exception)
    {
        super(exception);
    }
}
