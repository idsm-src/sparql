package cz.iocb.sparql.engine.request;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import java.math.BigDecimal;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.iocb.sparql.engine.mapping.classes.BooleanClass;
import cz.iocb.sparql.engine.mapping.classes.CommonIriClass;
import cz.iocb.sparql.engine.mapping.classes.DateCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleClass;
import cz.iocb.sparql.engine.mapping.classes.FloatClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IntClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringClass;
import cz.iocb.sparql.engine.mapping.classes.LongClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ShortClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.StringClass;
import cz.iocb.sparql.engine.mapping.classes.UnsupportedLiteralClass;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;



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

    private static final Map<String, Class<?>> typeMap = Map.ofEntries(Map.entry("bool", Boolean.class),
            Map.entry("char", Character.class), Map.entry("int2", Short.class), Map.entry("int4", Integer.class),
            Map.entry("int8", Long.class), Map.entry("numeric", BigDecimal.class), Map.entry("float8", Double.class),
            Map.entry("float4", Float.class), Map.entry("varchar", String.class), Map.entry("date", LocalDate.class),
            Map.entry("timestamptz", LocalDateTime.class));

    protected final ResultType type;
    protected final Map<Variable, List<ResultResourceClass>> description;
    protected final Map<Variable, Integer> varNames = new HashMap<>();
    protected final List<Variable> heads = new ArrayList<>();
    protected RdfTerm[] rowData;

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


    public Result(ResultType type, Map<Variable, List<ResultResourceClass>> description, ResultSet rs, long begin,
            long timeout) throws SQLException
    {
        this.rs = rs;
        this.description = description;
        this.rowData = new RdfTerm[description.size()];

        this.type = type;
        this.begin = begin;
        this.timeout = timeout;
        this.checkSize = Math.max(100, rs.getFetchSize());

        for(Variable var : description.keySet())
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

        Arrays.fill(rowData, null);

        int i = 1;
        int idx = 0;

        for(Entry<Variable, List<ResultResourceClass>> entry : description.entrySet())
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
                    case IntBlankNodeClass _ -> new IntBlankNode((Integer) value, rs.getInt(i++));

                    case StrBlankNodeClass _ -> new StrBlankNode((String) value, rs.getInt(i++));

                    case CommonIriClass _ -> new Iri((String) value);

                    case BooleanClass _ -> new TypedLiteral(value.toString(), xsdBooleanIri);

                    case ShortClass _ -> new TypedLiteral(value.toString(), xsdShortIri);

                    case IntClass _ -> new TypedLiteral(value.toString(), xsdIntIri);

                    case LongClass _ -> new TypedLiteral(value.toString(), xsdLongIri);

                    case FloatClass _ ->
                    {
                        Object data = Float.isFinite((float) value) ? new BigDecimal(value.toString()) : value;
                        yield new TypedLiteral(decimalFormat.format(data), xsdFloatType.getTypeIri());
                    }

                    case DoubleClass _ -> new TypedLiteral(decimalFormat.format(value), xsdDoubleType.getTypeIri());

                    case IntegerClass _ -> new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                            xsdIntegerType.getTypeIri());

                    case DecimalClass _ -> new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                            xsdDecimalType.getTypeIri());

                    case DateTimeCompositeClass _ -> new TypedLiteral(
                            dateTimeToString((LocalDateTime) value, rs.getInt(i++)), xsdDateTimeType.getTypeIri());

                    case DateCompositeClass _ -> new TypedLiteral(dateToString((LocalDate) value, rs.getInt(i++)),
                            xsdDateType.getTypeIri());

                    case DayTimeDurationClass _ -> new TypedLiteral(durationToString((Long) value),
                            xsdDayTimeDurationType.getTypeIri());

                    case StringClass _ -> new TypedLiteral(value.toString(), xsdStringType.getTypeIri());

                    case LangStringClass _ -> new LangStringLiteral(value.toString(), rs.getString(i++));

                    case UnsupportedLiteralClass _ -> new TypedLiteral(value.toString(), new Iri(rs.getString(i++)));

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
        LinkedList<String> warnings = new LinkedList<>();

        for(SQLWarning warning = rs.getStatement().getWarnings(); warning != null; warning = warning.getNextWarning())
            warnings.add(warning.getMessage());

        return warnings;
    }


    public List<Variable> getHeads()
    {
        return heads;
    }


    public Map<Variable, Integer> getVariableIndexes()
    {
        return varNames;
    }


    public RdfTerm get(int idx)
    {
        return rowData[idx];
    }


    public RdfTerm get(Variable var)
    {
        Integer idx = varNames.get(var);

        if(idx == null)
            return null;

        return rowData[idx];
    }


    public RdfTerm[] getRow()
    {
        return rowData.clone();
    }


    @Override
    public void close() throws SQLException
    {
        rs.close();
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
