package cz.iocb.chemweb.server.db.postgresql;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import cz.iocb.chemweb.server.db.IriNode;
import cz.iocb.chemweb.server.db.Literal;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.TypedLiteral;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class PostgreDatabase
{
    public Result rawQuery(String query, PostgreHandler handler) throws DatabaseException
    {
        try
        {
            Statement stmt = handler.stmt;

            long time = System.currentTimeMillis();
            ResultSet rs = stmt.executeQuery(query);


            ResultSetMetaData metadata = rs.getMetaData();

            final Vector<String> heads = new Vector<String>();
            final HashMap<String, Integer> columnNames = new HashMap<String, Integer>();
            final HashMap<String, Integer> varNames = new HashMap<String, Integer>();

            String lastName = null;

            for(int i = 0; i < metadata.getColumnCount(); i++)
            {
                String var = metadata.getColumnName(i + 1);
                String name = var.replaceAll("#.*", "");

                varNames.put(var, heads.size());

                if(name.equals(lastName))
                    continue;
                else
                    lastName = name;

                columnNames.put(name, heads.size());
                heads.add(name);
            }


            final ArrayList<Row> rows = new ArrayList<Row>();

            while(rs.next())
            {
                RdfNode[] rowData = new RdfNode[heads.size()];

                for(int i = 0; i < metadata.getColumnCount(); i++)
                {
                    String name = metadata.getColumnName(i + 1);
                    String type = name.replaceAll(".*#", "");
                    String value = rs.getString(i + 1);

                    int idx = varNames.get(name);

                    if(value == null)
                        type = ResourceClass.nullTag;

                    switch(type)
                    {
                        case IriClass.iriTag:
                            rowData[idx] = new IriNode(value);
                            break;

                        case LiteralClass.booleanTag:
                            rowData[idx] = new TypedLiteral(value, xsdBoolean.getTypeIri());
                            break;

                        case LiteralClass.integerTag:
                            rowData[idx] = new TypedLiteral(value, xsdInteger.getTypeIri());
                            break;

                        case LiteralClass.decimalTag:
                            rowData[idx] = new TypedLiteral(value, xsdDecimal.getTypeIri());
                            break;

                        case LiteralClass.floatTag:
                            rowData[idx] = new TypedLiteral(value, xsdFloat.getTypeIri());
                            break;

                        case LiteralClass.doubleTag:
                            rowData[idx] = new TypedLiteral(value, xsdDouble.getTypeIri());
                            break;

                        case LiteralClass.dateTag:
                            rowData[idx] = new TypedLiteral(value, xsdDate.getTypeIri());
                            break;

                        case LiteralClass.dateTimeTag:
                            rowData[idx] = new TypedLiteral(value, xsdDateTime.getTypeIri());
                            break;

                        case LiteralClass.stringTag:
                            rowData[idx] = new Literal(value);
                            break;

                        case ResourceClass.nullTag:
                            rowData[idx] = null;
                            break;

                        default:
                            assert false;
                    }
                }

                rows.add(new Row(columnNames, rowData));
            }


            Connection conn = stmt.getConnection();

            rs.close();
            stmt.close();
            conn.close();

            time = System.currentTimeMillis() - time;

            if(time > 500)
            {
                //System.err.println(query.replaceAll("\n", "\\\\n"));
                System.err.println("slow query (" + time / 1000.0 + "s)");
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


    public Result query(String query) throws DatabaseException
    {
        return rawQuery(query, getHandler());
    }


    public PostgreHandler getHandler() throws DatabaseException
    {
        try
        {
            Statement stmt = ConnectionPool.getConnection().createStatement();
            return new PostgreHandler(stmt);
        }
        catch (Exception e)
        {
            throw new DatabaseException(e);
        }
    }
}
