package cz.iocb.chemweb.server.db.postgresql;

import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDayTimeDurationIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDecimalIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdFloatIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntegerIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdLongIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdShortIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import cz.iocb.chemweb.server.db.BlankNode;
import cz.iocb.chemweb.server.db.IriNode;
import cz.iocb.chemweb.server.db.LanguageTaggedLiteral;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.TypedLiteral;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class PostgresDatabase
{
    private static final DecimalFormat decimalFormat;
    private static final String typedLiteralPattern = "^\"(.*)\"\\^\\^<([^<>]+)>$";
    private static final String taggedLiteralPattern = "^\"(.*)\"@([^@]+)$";
    private static final long USECS_PER_DAY = 86400000000l;
    private static final long USECS_PER_HOUR = 3600000000l;
    private static final long USECS_PER_MINUTE = 60000000l;
    private static final long USECS_PER_SEC = 1000000l;

    private static final char[] encodeTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F' };

    private final ConnectionPool connectionPool;


    static
    {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setExponentSeparator("E");
        symbols.setInfinity("INF");
        symbols.setNaN("NaN");
        decimalFormat = new DecimalFormat("0.0################################E0", symbols);
    }


    public PostgresDatabase(ConnectionPool connectionPool)
    {
        this.connectionPool = connectionPool;
    }


    public Result rawQuery(String query, PostgresHandler handler) throws DatabaseException
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

                if(!name.equals(lastName))
                {
                    lastName = name;

                    columnNames.put(name, heads.size());
                    heads.add(name);
                }

                varNames.put(var, heads.size() - 1);
            }


            final ArrayList<Row> rows = new ArrayList<Row>();

            while(rs.next())
            {
                RdfNode[] rowData = new RdfNode[heads.size()];

                for(int i = 0; i < metadata.getColumnCount(); i++)
                {
                    String name = metadata.getColumnName(i + 1);
                    String type = name.replaceAll(".*#", "");
                    String lexical = rs.getString(i + 1);
                    Object value = rs.getObject(i + 1);
                    int idx = varNames.get(name);

                    ResultTag tag = ResultTag.get(type);

                    if(lexical != null)
                    {
                        switch(tag)
                        {
                            case RDFTERM:
                                switch(lexical.charAt(0))
                                {
                                    case '<':
                                        rowData[idx] = new IriNode(lexical.substring(1, lexical.length() - 1));
                                        break;

                                    case '_':
                                        rowData[idx] = new BlankNode(lexical.substring(2));
                                        break;

                                    case '"':
                                        if(lexical.endsWith(">"))
                                            rowData[idx] = new TypedLiteral(
                                                    lexical.replaceFirst(typedLiteralPattern, "$1"),
                                                    lexical.replaceFirst(typedLiteralPattern, "$2"));
                                        else
                                            rowData[idx] = new LanguageTaggedLiteral(
                                                    lexical.replaceFirst(taggedLiteralPattern, "$1"),
                                                    lexical.replaceFirst(taggedLiteralPattern, "$2"));
                                        break;
                                }
                                break;

                            case BLANKNODEINT:
                                rowData[idx] = new BlankNode("I" + value);
                                break;

                            case BLANKNODESTR:
                                rowData[idx] = new BlankNode(encodeBlankNodeLabel((String) value));
                                break;

                            case IRI:
                                rowData[idx] = new IriNode((String) value);
                                break;

                            case BOOLEAN:
                                rowData[idx] = new TypedLiteral(value.toString(), xsdBooleanIri);
                                break;

                            case SHORT:
                                rowData[idx] = new TypedLiteral(value.toString(), xsdShortIri);
                                break;

                            case INT:
                                rowData[idx] = new TypedLiteral(value.toString(), xsdIntIri);
                                break;

                            case LONG:
                                rowData[idx] = new TypedLiteral(value.toString(), xsdLongIri);
                                break;

                            case FLOAT:
                                Object data = Float.isFinite((float) value) ? new BigDecimal(value.toString()) : value;
                                rowData[idx] = new TypedLiteral(decimalFormat.format(data), xsdFloatIri);
                                break;

                            case DOUBLE:
                                rowData[idx] = new TypedLiteral(decimalFormat.format(value), xsdDoubleIri);
                                break;

                            case INTEGER:
                                rowData[idx] = new TypedLiteral(value.toString(), xsdIntegerIri);
                                break;

                            case DECIMAL:
                                rowData[idx] = new TypedLiteral(value.toString(), xsdDecimalIri);
                                break;

                            case DATETIME:
                                rowData[idx] = new TypedLiteral(lexical, xsdDateTimeIri);
                                break;

                            case DATE:
                                rowData[idx] = new TypedLiteral(lexical, xsdDateIri);
                                break;

                            case DAYTIMEDURATION:
                                rowData[idx] = new TypedLiteral(durationToString((Long) value), xsdDayTimeDurationIri);
                                break;

                            case STRING:
                                rowData[idx] = new TypedLiteral(value.toString(), xsdStringIri);
                                break;

                            case LANGSTRING:
                                String lang = rs.getString(name.replaceAll("#.*", "") + "#" + ResultTag.LANG.getTag());
                                rowData[idx] = new LanguageTaggedLiteral(value.toString(), lang);
                                break;

                            // ignore supplementary literal tags
                            case LANG:
                                break;
                        }
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
        catch(Exception e)
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


    public PostgresHandler getHandler() throws DatabaseException
    {
        try
        {
            Statement stmt = connectionPool.getConnection().createStatement();
            return new PostgresHandler(stmt);
        }
        catch(Exception e)
        {
            throw new DatabaseException(e);
        }
    }


    private static String encodeBlankNodeLabel(String value)
    {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);

        StringBuffer buffer = new StringBuffer();
        buffer.append('S');

        for(int j = 0; j < data.length; j++)
        {
            if((data[j] < '0' || data[j] > '9') && (data[j] < 'A' || data[j] > 'Z') && (data[j] < 'a' || data[j] > 'z'))
            {
                int val = data[j] < 0 ? data[j] + 256 : data[j];
                buffer.append('_');
                buffer.append(encodeTable[val / 16]);
                buffer.append(encodeTable[val % 16]);
            }
            else
            {
                buffer.append((char) data[j]);
            }
        }

        return buffer.toString();
    }


    private static String durationToString(long value)
    {
        if(value == 0)
            return "PT0S";

        if(value == Long.MAX_VALUE)
            return "-P106751991DT4H54.775808S";

        StringBuffer buffer = new StringBuffer();

        if(value < 0)
            buffer.append('-');

        value = Math.abs(value);

        long days = value / USECS_PER_DAY;
        long hours = value % USECS_PER_DAY / USECS_PER_HOUR;
        long minutes = value % USECS_PER_HOUR / USECS_PER_MINUTE;
        long seconds = value % USECS_PER_MINUTE / USECS_PER_SEC;
        long useconds = value % USECS_PER_SEC;

        buffer.append('P');

        if(days > 0)
        {
            buffer.append(days);
            buffer.append('D');
        }

        if(hours > 0 || minutes > 0 || seconds > 0 || useconds > 0)
            buffer.append('T');

        if(hours > 0)
        {
            buffer.append(hours);
            buffer.append('H');
        }

        if(minutes > 0)
        {
            buffer.append(minutes);
            buffer.append('M');
        }

        if(seconds > 0)
        {
            buffer.append(seconds);

            if(useconds == 0)
                buffer.append('S');
        }

        if(useconds > 0)
        {
            if(seconds == 0)
                buffer.append('0');

            String str = Long.toString(useconds);

            buffer.append('.');
            buffer.append("000000".substring(str.length(), 6) + str.replaceFirst("0+$", ""));
            buffer.append('S');
        }

        return buffer.toString();
    }
}
