package cz.iocb.chemweb.server.db.postgresql;

import java.sql.SQLException;
import java.sql.Statement;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class PostgreHandler
{
    Statement stmt;


    public PostgreHandler(Statement stmt)
    {
        this.stmt = stmt;
    }


    public void cancel() throws DatabaseException
    {
        try
        {
            stmt.cancel();
        }
        catch (SQLException e)
        {
            throw new DatabaseException(e);
        }
    }
}
