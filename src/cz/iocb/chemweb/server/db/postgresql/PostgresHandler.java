package cz.iocb.chemweb.server.db.postgresql;

import java.sql.SQLException;
import java.sql.Statement;



public class PostgresHandler
{
    private Statement statement = null;
    private boolean canceled = false;


    PostgresHandler()
    {
    }


    synchronized void setStatement(Statement statement) throws SQLException
    {
        if(canceled)
            throw new SQLException("query was canceled");

        this.statement = statement;
    }


    public synchronized void cancel() throws SQLException
    {
        canceled = true;

        if(statement != null)
        {
            statement.cancel();
            statement.close();
        }
    }
}
