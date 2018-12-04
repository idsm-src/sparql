package cz.iocb.chemweb.server.db.postgresql;

import java.sql.SQLException;
import java.sql.Statement;



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


    public void cancel() throws SQLException
    {
        statement.cancel();
    }
}
