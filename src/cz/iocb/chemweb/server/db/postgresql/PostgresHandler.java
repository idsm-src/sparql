package cz.iocb.chemweb.server.db.postgresql;

import java.sql.SQLException;
import java.sql.Statement;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class PostgresHandler
{
    private Statement statement;


    PostgresHandler()
    {
    }


    void setStatement(Statement statement)
    {
        this.statement = statement;
    }


    public void cancel() throws DatabaseException
    {
        try
        {
            statement.cancel();
        }
        catch(SQLException e)
        {
            throw new DatabaseException(e);
        }
    }
}
