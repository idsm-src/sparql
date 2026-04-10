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
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.iocb.sparql.engine.mapping.classes.BooleanClass;
import cz.iocb.sparql.engine.mapping.classes.CommonIriClass;
import cz.iocb.sparql.engine.mapping.classes.DateClass;
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleClass;
import cz.iocb.sparql.engine.mapping.classes.FloatClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IntClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringConstantTagClass;
import cz.iocb.sparql.engine.mapping.classes.LongClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ShortClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.StringClass;
import cz.iocb.sparql.engine.mapping.classes.UnsupportedLiteralClass;



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

    private static final Map<String, Class<?>> typeMap = Map.ofEntries(Map.entry("bool", Boolean.class),
            Map.entry("char", Character.class), Map.entry("int2", Short.class), Map.entry("int4", Integer.class),
            Map.entry("int8", Long.class), Map.entry("numeric", BigDecimal.class), Map.entry("float8", Double.class),
            Map.entry("float4", Float.class), Map.entry("varchar", String.class), Map.entry("date", LocalDate.class),
            Map.entry("timestamptz", LocalDateTime.class));

    protected final ResultType type;
    protected final Map<String, List<ResultResourceClass>> description;
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


    public Result(ResultType type, Map<String, List<ResultResourceClass>> description, ResultSet rs, long begin,
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

        for(Entry<String, List<ResultResourceClass>> entry : description.entrySet())
        {
            for(ResultResourceClass rc : entry.getValue())
            {
                if(typeMap.get(((ResourceClass) rc).getSqlTypes().get(0)) == null)
                    System.err.println(((ResourceClass) rc).getSqlTypes().get(0));

                Object value = rs.getObject(i++, typeMap.get(((ResourceClass) rc).getSqlTypes().get(0)));

                if(value == null)
                {
                    i += (((ResourceClass) rc).getColumnCount() - 1);
                    continue;
                }

                rowData[idx] = switch(rc)
                {
                    case IntBlankNodeClass c -> new BNode(encodeIBlankNodeLabel((Integer) value, rs.getInt(i++)));

                    case StrBlankNodeClass c -> new BNode(encodeSBlankNodeLabel((String) value, rs.getInt(i++)));

                    case CommonIriClass c -> new IriNode((String) value);

                    case BooleanClass c -> new TypedLiteral(value.toString(), xsdBooleanType.getTypeIri());

                    case ShortClass c -> new TypedLiteral(value.toString(), xsdShortType.getTypeIri());

                    case IntClass c -> new TypedLiteral(value.toString(), xsdIntType.getTypeIri());

                    case LongClass c -> new TypedLiteral(value.toString(), xsdLongType.getTypeIri());

                    case FloatClass c ->
                    {
                        Object data = Float.isFinite((float) value) ? new BigDecimal(value.toString()) : value;
                        yield new TypedLiteral(decimalFormat.format(data), xsdFloatType.getTypeIri());
                    }

                    case DoubleClass c -> new TypedLiteral(decimalFormat.format(value), xsdDoubleType.getTypeIri());

                    case IntegerClass c -> new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                            xsdIntegerType.getTypeIri());

                    case DecimalClass c -> new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                            xsdDecimalType.getTypeIri());

                    case DateTimeClass c -> new TypedLiteral(dateTimeToString((LocalDateTime) value, rs.getInt(i++)),
                            xsdDateTimeType.getTypeIri());

                    case DateTimeConstantZoneClass c -> new TypedLiteral(
                            dateTimeToString((LocalDateTime) value, c.getZone()), xsdDateTimeType.getTypeIri());

                    case DateClass c -> new TypedLiteral(dateToString((LocalDate) value, rs.getInt(i++)),
                            xsdDateType.getTypeIri());

                    case DateConstantZoneClass c -> new TypedLiteral(dateToString((LocalDate) value, c.getZone()),
                            xsdDateType.getTypeIri());

                    case DayTimeDurationClass c -> new TypedLiteral(durationToString((Long) value),
                            xsdDayTimeDurationType.getTypeIri());

                    case StringClass c -> new TypedLiteral(value.toString(), xsdStringType.getTypeIri());

                    case LangStringClass c -> new LanguageTaggedLiteral(value.toString(), rs.getString(i++));

                    case LangStringConstantTagClass c -> new LanguageTaggedLiteral(value.toString(), c.getTag());

                    case UnsupportedLiteralClass c -> new TypedLiteral(value.toString(), rs.getString(i++));

                    default ->
                    {
                        System.err.println(rc.getClass().getCanonicalName());
                        yield null;
                    }
                };
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


    private static String encodeIBlankNodeLabel(int value, int segment)
    {
        return String.format("i%8s%8s", Integer.toHexString(segment), Integer.toHexString(value)).replace(' ', '0');
    }


    private static String encodeSBlankNodeLabel(String value, int segment)
    {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("s%8s", Integer.toHexString(segment)).replace(' ', '0'));

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


    private String dateTimeToString(LocalDateTime value, int zone)
    {
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(zone != Integer.MIN_VALUE ? zone : 0);
        OffsetDateTime date = value.atOffset(offset);
        DateTimeFormatter format = zone != Integer.MIN_VALUE ? ISO_OFFSET_DATE_TIME : ISO_LOCAL_DATE_TIME;

        return date.format(format);
    }


    private static String dateToString(LocalDate value, int zone)
    {
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(zone != Integer.MIN_VALUE ? zone : 0);
        OffsetDateTime date = value.atStartOfDay().atOffset(offset);
        DateTimeFormatter format = zone != Integer.MIN_VALUE ? ISO_OFFSET_DATE : ISO_LOCAL_DATE;

        return date.format(format);
    }

}
