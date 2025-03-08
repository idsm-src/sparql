package cz.iocb.sparql.engine.request;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdBooleanType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDateTimeType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDateType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDayTimeDurationType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDecimalType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDoubleType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdFloatType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntegerType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdLongType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdShortType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdStringType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.iocb.sparql.engine.mapping.classes.ResultTag;



public class Result implements AutoCloseable
{
    static public enum ResultType
    {
        SELECT, ASK, DESCRIBE, CONSTRUCT
    }


    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    private static final DecimalFormat decimalFormat;
    private static final long USECS_PER_DAY = 86400000000l;
    private static final long USECS_PER_HOUR = 3600000000l;
    private static final long USECS_PER_MINUTE = 60000000l;
    private static final long USECS_PER_SEC = 1000000l;
    private static final char[] encodeTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f' };

    protected final ResultType type;
    protected final Map<String, List<List<ResultTag>>> description;
    protected final HashMap<String, Integer> varNames = new HashMap<String, Integer>();
    protected final List<String> heads = new ArrayList<String>();
    protected RdfNode[] rowData;

    private final ResultSet rs;

    private final long begin;
    private final long timeout;
    private final int checkSize;
    private int count = 0;


    static
    {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setExponentSeparator("E");
        symbols.setInfinity("INF");
        symbols.setNaN("NaN");
        decimalFormat = new DecimalFormat("################0.0################", symbols);
    }


    public Result(ResultType type, Map<String, List<List<ResultTag>>> description, ResultSet rs, long begin,
            long timeout) throws SQLException
    {
        this.rs = rs;
        this.description = description;
        this.rowData = new RdfNode[description.size()];

        this.type = type;
        this.begin = begin;
        this.timeout = timeout;
        this.checkSize = Math.max(100, rs.getFetchSize());

        for(String var : description.keySet())
        {
            varNames.put(var, varNames.size());
            heads.add(var);
        }
    }


    public boolean next() throws SQLException
    {
        if(count++ % checkSize == 0 && timeout > 0 && timeout < System.nanoTime() - begin)
        {
            logger.warn("fetch timeout");
            throw new SQLException("fetch timeout", "57014", 0);
        }

        if(!rs.next())
            return false;

        for(int i = 0; i < rowData.length; i++)
            rowData[i] = null;

        int i = 1;
        int idx = 0;

        for(Entry<String, List<List<ResultTag>>> entry : description.entrySet())
        {
            for(ResultTag tag : entry.getValue().stream().flatMap(t -> t.stream()).toList())
            {
                String lexical = rs.getString(i);
                Object value = rs.getObject(i);
                i++;

                if(lexical != null)
                {
                    switch(tag)
                    {
                        case NULL:
                            rowData[idx] = null;
                            break;

                        case BLANKNODEINT:
                            rowData[idx] = new BNode(encodeIBlankNodeLabel((Long) value));
                            break;

                        case BLANKNODESTR:
                            rowData[idx] = new BNode(encodeSBlankNodeLabel((String) value));
                            break;

                        case IRI:
                            rowData[idx] = new IriNode((String) value);
                            break;

                        case BOOLEAN:
                            rowData[idx] = new TypedLiteral(value.toString(), xsdBooleanType.getTypeIri());
                            break;

                        case SHORT:
                            rowData[idx] = new TypedLiteral(value.toString(), xsdShortType.getTypeIri());
                            break;

                        case INT:
                            rowData[idx] = new TypedLiteral(value.toString(), xsdIntType.getTypeIri());
                            break;

                        case LONG:
                            rowData[idx] = new TypedLiteral(value.toString(), xsdLongType.getTypeIri());
                            break;

                        case FLOAT:
                            Object data = Float.isFinite((float) value) ? new BigDecimal(value.toString()) : value;
                            rowData[idx] = new TypedLiteral(decimalFormat.format(data), xsdFloatType.getTypeIri());
                            break;

                        case DOUBLE:
                            rowData[idx] = new TypedLiteral(decimalFormat.format(value), xsdDoubleType.getTypeIri());
                            break;

                        case INTEGER:
                            rowData[idx] = new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                    xsdIntegerType.getTypeIri());
                            break;

                        case DECIMAL:
                            rowData[idx] = new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                    xsdDecimalType.getTypeIri());
                            break;

                        case DATETIME:
                            rowData[idx] = new TypedLiteral(lexical, xsdDateTimeType.getTypeIri());
                            break;

                        case DATE:
                            rowData[idx] = new TypedLiteral(lexical, xsdDateType.getTypeIri());
                            break;

                        case DAYTIMEDURATION:
                            rowData[idx] = new TypedLiteral(durationToString((Long) value),
                                    xsdDayTimeDurationType.getTypeIri());
                            break;

                        case STRING:
                            rowData[idx] = new TypedLiteral(value.toString(), xsdStringType.getTypeIri());
                            break;

                        case LANGSTRING:
                            String lang = rs.getString(i);
                            rowData[idx] = new LanguageTaggedLiteral(value.toString(), lang);
                            break;

                        case LITERAL:
                            String type = rs.getString(i);
                            rowData[idx] = new TypedLiteral(value.toString(), type);

                        case LANG:
                        case TYPE:
                            // ignore supplementary literal tags
                            break;
                    }
                }
            }

            idx++;
        }

        return true;
    }


    public ResultType getResultType()
    {
        return type;
    }


    public List<String> getWarnings() throws SQLException
    {
        LinkedList<String> warnings = new LinkedList<String>();

        for(SQLWarning warning = rs.getStatement().getWarnings(); warning != null; warning = warning.getNextWarning())
            warnings.add(warning.getMessage());

        return warnings;
    }


    public List<String> getHeads()
    {
        return heads;
    }


    public HashMap<String, Integer> getVariableIndexes()
    {
        return varNames;
    }


    public RdfNode get(int idx)
    {
        return rowData[idx];
    }


    public RdfNode get(String name)
    {
        Integer idx = varNames.get(name);

        if(idx == null)
            return null;

        return rowData[idx];
    }


    public RdfNode[] getRow()
    {
        return rowData.clone();
    }


    @Override
    public void close() throws SQLException
    {
        rs.close();
    }


    private static String encodeIBlankNodeLabel(long value)
    {
        return String.format("i%8s", Long.toHexString(value)).replace(' ', '0');
    }


    private static String encodeSBlankNodeLabel(String value)
    {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);

        StringBuilder builder = new StringBuilder();
        builder.append('s');

        for(int j = 0; j < data.length; j++)
        {
            if((data[j] < '0' || data[j] > '9') && (data[j] < 'A' || data[j] > 'Z') && (data[j] < 'a' || data[j] > 'z'))
            {
                int val = data[j] < 0 ? data[j] + 256 : data[j];
                builder.append('-');
                builder.append(encodeTable[val / 16]);
                builder.append(encodeTable[val % 16]);
            }
            else
            {
                builder.append((char) data[j]);
            }
        }

        return builder.toString();
    }


    private static String durationToString(long value)
    {
        if(value == 0)
            return "PT0S";

        if(value == Long.MAX_VALUE)
            return "-P106751991DT4H54.775808S";

        StringBuilder builder = new StringBuilder();

        if(value < 0)
            builder.append('-');

        value = Math.abs(value);

        long days = value / USECS_PER_DAY;
        long hours = value % USECS_PER_DAY / USECS_PER_HOUR;
        long minutes = value % USECS_PER_HOUR / USECS_PER_MINUTE;
        long seconds = value % USECS_PER_MINUTE / USECS_PER_SEC;
        long useconds = value % USECS_PER_SEC;

        builder.append('P');

        if(days > 0)
        {
            builder.append(days);
            builder.append('D');
        }

        if(hours > 0 || minutes > 0 || seconds > 0 || useconds > 0)
            builder.append('T');

        if(hours > 0)
        {
            builder.append(hours);
            builder.append('H');
        }

        if(minutes > 0)
        {
            builder.append(minutes);
            builder.append('M');
        }

        if(seconds > 0)
        {
            builder.append(seconds);

            if(useconds == 0)
                builder.append('S');
        }

        if(useconds > 0)
        {
            if(seconds == 0)
                builder.append('0');

            String str = Long.toString(useconds);

            builder.append('.');
            builder.append("000000".substring(str.length(), 6) + str.replaceFirst("0+$", ""));
            builder.append('S');
        }

        return builder.toString();
    }
}
