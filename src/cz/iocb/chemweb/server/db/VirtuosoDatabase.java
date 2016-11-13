package cz.iocb.chemweb.server.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;
import cz.iocb.chemweb.shared.services.DatabaseException;
import virtuoso.jdbc4.VirtuosoDate;
import virtuoso.jdbc4.VirtuosoExtendedString;
import virtuoso.jdbc4.VirtuosoRdfBox;



public class VirtuosoDatabase
{
    private static final Logger logger = Logger.getLogger(VirtuosoDatabase.class);


    public Result query(List<String> queries, VirtuosoHandler handler) throws DatabaseException
    {
        Statement stmt = handler.stmt;


        try
        {
            stmt.getConnection().setAutoCommit(false);

            Result result = null;

            for(String query : queries)
            {
                result = rawQuery(query, handler);
            }

            return result;
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        finally
        {
            try
            {
                //stmt.close();
                stmt.getConnection().rollback();
                stmt.getConnection().close();
            }
            catch (SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }


    public Result query(String query) throws DatabaseException
    {
        VirtuosoHandler handler = getHandler();

        try
        {
            return rawQuery(query, handler);
        }
        finally
        {
            try
            {
                //handler.stmt.close();
                handler.stmt.getConnection().close();
            }
            catch (SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    public Result rawQuery(String query, VirtuosoHandler handler) throws DatabaseException
    {
        try
        {
            Statement stmt = handler.stmt;

            long time = System.currentTimeMillis();
            //Connection connection = stmt.getConnection();

            ResultSet rs = stmt.executeQuery(query);


            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();


            final Vector<String> heads = new Vector<String>();
            final HashMap<String, Integer> columnNames = new HashMap<String, Integer>();

            for(int i = 0; i < columnCount; i++)
            {
                columnNames.put(metadata.getColumnName(i + 1).toUpperCase(), i); //FIXME: toUpperCase?
                heads.add(metadata.getColumnName(i + 1));
            }


            final ArrayList<Row> rows = new ArrayList<Row>();


            while(rs.next())
            {
                RdfNode[] rowData = new RdfNode[columnCount];

                for(int i = 0; i < metadata.getColumnCount(); i++)
                {
                    Object o = rs.getObject(i + 1);

                    if(rs.wasNull())
                    {
                        rowData[i] = null;
                    }
                    else if(o instanceof VirtuosoExtendedString)
                    {
                        VirtuosoExtendedString vs = (VirtuosoExtendedString) o;

                        if(vs.iriType == VirtuosoExtendedString.IRI && (vs.strType & 0x01) == 0x01)
                            rowData[i] = new IriNode(vs.str);
                        else if(vs.iriType == VirtuosoExtendedString.BNODE)
                            rowData[i] = new BlankNode(vs.str);
                    }
                    else if(o instanceof VirtuosoRdfBox)
                    {
                        VirtuosoRdfBox rb = (VirtuosoRdfBox) o;

                        if(rb.getType() != null)
                            rowData[i] = new TypedLiteral(rb.rb_box.toString(), rb.getType());
                        else if(rb.getLang() != null)
                            rowData[i] = new LanguageTaggedLiteral(rb.rb_box.toString(), rb.getLang());
                        else
                            rowData[i] = new Literal(rb.rb_box.toString());
                    }
                    else if(o instanceof VirtuosoDate)
                    {
                        rowData[i] = new TypedLiteral(o.toString(), "http://www.w3.org/2001/XMLSchema#date");
                    }
                    else if(o instanceof String)
                    {
                        String value = (String) o;

                        if(value.startsWith("http://"))
                            rowData[i] = new IriNode(value); // workaround
                        else
                            rowData[i] = new Literal(value);
                    }
                    else
                    {
                        // FIXME: Handle as typed?
                        rowData[i] = new Literal(o.toString());
                    }
                }

                rows.add(new Row(columnNames, rowData));
            }


            rs.close();
            //stmt.close();
            //connection.close();


            time = System.currentTimeMillis() - time;


            if(time > 500)
            {
                System.err.println(query.replaceAll("\n", "\\\\n"));
                System.err.println("slow query (" + time / 1000.0 + "s)");
                logger.info("slow query (" + time / 1000.0 + "s): " + query.replaceAll("\n", "\\\\n"));
            }

            return new Result(heads, rows);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println(query);

            throw new DatabaseException(e);
        }
    }


    public Result query(List<String> query) throws DatabaseException
    {
        return query(query, getHandler());
    }


    public VirtuosoHandler getHandler() throws DatabaseException
    {
        try
        {
            Statement stmt = ConnectionPool.getConnection().createStatement();
            return new VirtuosoHandler(stmt);
        }
        catch (Exception e)
        {
            throw new DatabaseException(e);
        }
    }
}
