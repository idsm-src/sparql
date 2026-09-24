package cz.iocb.sparql.engine.request;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortIri;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
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
import cz.iocb.sparql.engine.mapping.classes.BooleanBaseClass;
import cz.iocb.sparql.engine.mapping.classes.BooleanClass;
import cz.iocb.sparql.engine.mapping.classes.ByteBaseClass;
import cz.iocb.sparql.engine.mapping.classes.ByteClass;
import cz.iocb.sparql.engine.mapping.classes.DateCompositeBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeCompositeBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DayTimeDurationClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DecimalClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DoubleClass;
import cz.iocb.sparql.engine.mapping.classes.FloatBaseClass;
import cz.iocb.sparql.engine.mapping.classes.FloatClass;
import cz.iocb.sparql.engine.mapping.classes.IntBaseClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IntClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerClass;
import cz.iocb.sparql.engine.mapping.classes.IriScalarClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringClass;
import cz.iocb.sparql.engine.mapping.classes.LongBaseClass;
import cz.iocb.sparql.engine.mapping.classes.LongClass;
import cz.iocb.sparql.engine.mapping.classes.NegativeIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.NegativeIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.NonNegativeIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.NonNegativeIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.NonPositiveIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.NonPositiveIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.PositiveIntegerBaseClass;
import cz.iocb.sparql.engine.mapping.classes.PositiveIntegerClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ShortBaseClass;
import cz.iocb.sparql.engine.mapping.classes.ShortClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.StringClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedByteBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedByteClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedIntBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedIntClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedLongBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedLongClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedShortBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UnsignedShortClass;
import cz.iocb.sparql.engine.mapping.classes.UnsupportedLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralCompositeBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralCompositeClass;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import info.adams.ryu.RyuDouble;
import info.adams.ryu.RyuFloat;



/**
 * Cursor over the rows of an executed query that turns the SQL columns back into RDF terms: for every projected
 * variable, the row holds one column group per result class of the variable, and the first non-null group gives the
 * value. Also enforces the fetch timeout.
 */
public class Result implements AutoCloseable
{
    /**
     * Form of the query the result comes from.
     */
    static public enum ResultType
    {
        /**
         * SELECT query: a table of variable bindings.
         */
        SELECT,

        /**
         * ASK query: a single boolean.
         */
        ASK,

        /**
         * DESCRIBE query: a graph.
         */
        DESCRIBE,

        /**
         * CONSTRUCT query: a graph.
         */
        CONSTRUCT
    }


    /**
     * Logger of result processing.
     */
    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    /**
     * Microseconds in a day.
     */
    private static final long USECS_PER_DAY = 86400000000l;

    /**
     * Microseconds in an hour.
     */
    private static final long USECS_PER_HOUR = 3600000000l;

    /**
     * Microseconds in a minute.
     */
    private static final long USECS_PER_MINUTE = 60000000l;

    /**
     * Microseconds in a second.
     */
    private static final long USECS_PER_SEC = 1000000l;

    /**
     * Form of the query.
     */
    protected final ResultType type;

    /**
     * Result classes of each projected variable, in column order.
     */
    protected final Map<Variable, List<ResultResourceClass>> description;

    /**
     * Position of each variable in a row.
     */
    protected final Map<Variable, Integer> varNames = new HashMap<>();

    /**
     * Projected variables in column order.
     */
    protected final List<Variable> heads = new ArrayList<>();

    /**
     * Terms of the current row; null entries are unbound.
     */
    protected RdfTerm[] rowData;

    /**
     * Underlying JDBC result set.
     */
    private final ResultSet rs;

    /**
     * Start of the execution in {@link System#nanoTime} units.
     */
    private final long begin;

    /**
     * Time limit of the execution in nanoseconds; 0 for none.
     */
    private final long timeout;

    /**
     * Number of rows between timeout checks.
     */
    private final int checkSize;

    /**
     * Number of rows fetched so far.
     */
    private int count = 0;


    /**
     * Creates the cursor over the result set described by the given result classes.
     *
     * @param type form of the query
     * @param description result classes of each projected variable
     * @param rs the JDBC result set
     * @param begin start of the execution in {@link System#nanoTime} units
     * @param timeout time limit in nanoseconds, 0 for none
     * @throws SQLException on database errors
     */
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


    /**
     * Advances to the next row, decoding its terms; false at the end.
     *
     * @return true if a row is available, false at the end
     * @throws SQLException on database errors or when the timeout elapsed (SQL state 57014)
     */
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
                Object value = rs.getObject(i++, ((ResourceClass) rc).getSqlTypes().get(0).getJavaClass());

                if(value == null)
                {
                    i += (((ResourceClass) rc).getColumnCount() - 1);
                    continue;
                }

                rowData[idx] = switch(rc)
                {
                    case IriScalarClass _ ->
                    {
                        yield new Iri((String) value);
                    }

                    case IntBlankNodeClass _ ->
                    {
                        int segment = rs.getInt(i++);
                        yield new IntBlankNode((Integer) value, segment);
                    }

                    case StrBlankNodeClass _ ->
                    {
                        int segment = rs.getInt(i++);
                        yield new StrBlankNode((String) value, segment);
                    }

                    case BooleanClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdBooleanIri);
                    }

                    case BooleanBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        yield new TypedLiteral(lexical.isEmpty() ? value.toString() : lexical, xsdBooleanIri);
                    }

                    case ByteClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdByteIri);
                    }

                    case ByteBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        yield new TypedLiteral(lexical.isEmpty() ? value.toString() : lexical, xsdByteIri);
                    }

                    case UnsignedByteClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdUnsignedByteIri);
                    }

                    case UnsignedByteBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        yield new TypedLiteral(lexical.isEmpty() ? value.toString() : lexical, xsdUnsignedByteIri);
                    }

                    case ShortClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdShortIri);
                    }

                    case ShortBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        yield new TypedLiteral(lexical.isEmpty() ? value.toString() : lexical, xsdShortIri);
                    }

                    case UnsignedShortClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdUnsignedShortIri);
                    }

                    case UnsignedShortBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        yield new TypedLiteral(lexical.isEmpty() ? value.toString() : lexical, xsdUnsignedShortIri);
                    }

                    case IntClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdIntIri);
                    }

                    case IntBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        yield new TypedLiteral(lexical.isEmpty() ? value.toString() : lexical, xsdIntIri);
                    }

                    case UnsignedIntClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdUnsignedIntIri);
                    }

                    case UnsignedIntBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        yield new TypedLiteral(lexical.isEmpty() ? value.toString() : lexical, xsdUnsignedIntIri);
                    }

                    case LongClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdLongIri);
                    }

                    case LongBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        String str = lexical.isEmpty() ? value.toString() : lexical;
                        yield new TypedLiteral(str, xsdLongIri);
                    }

                    case UnsignedLongClass _ ->
                    {
                        yield new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                xsdUnsignedLongIri);
                    }

                    case UnsignedLongBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        BigDecimal num = ((BigDecimal) value).stripTrailingZeros();
                        String str = lexical.isEmpty() ? num.toPlainString() : lexical;
                        yield new TypedLiteral(str, xsdUnsignedLongIri);
                    }

                    case IntegerClass _ ->
                    {
                        yield new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                xsdIntegerIri);
                    }

                    case IntegerBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        BigDecimal num = ((BigDecimal) value).stripTrailingZeros();
                        String str = lexical.isEmpty() ? num.toPlainString() : lexical;
                        yield new TypedLiteral(str, xsdIntegerIri);
                    }

                    case NonPositiveIntegerClass _ ->
                    {
                        yield new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                xsdNonPositiveIntegerIri);
                    }

                    case NonPositiveIntegerBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        BigDecimal num = ((BigDecimal) value).stripTrailingZeros();
                        String str = lexical.isEmpty() ? num.toPlainString() : lexical;
                        yield new TypedLiteral(str, xsdNonPositiveIntegerIri);
                    }

                    case NegativeIntegerClass _ ->
                    {
                        yield new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                xsdNegativeIntegerIri);
                    }

                    case NegativeIntegerBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        BigDecimal num = ((BigDecimal) value).stripTrailingZeros();
                        String str = lexical.isEmpty() ? num.toPlainString() : lexical;
                        yield new TypedLiteral(str, xsdNegativeIntegerIri);
                    }

                    case NonNegativeIntegerClass _ ->
                    {
                        yield new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                xsdNonNegativeIntegerIri);
                    }

                    case NonNegativeIntegerBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        BigDecimal num = ((BigDecimal) value).stripTrailingZeros();
                        String str = lexical.isEmpty() ? num.toPlainString() : lexical;
                        yield new TypedLiteral(str, xsdNonNegativeIntegerIri);
                    }

                    case PositiveIntegerClass _ ->
                    {
                        yield new TypedLiteral(((BigDecimal) value).stripTrailingZeros().toPlainString(),
                                xsdPositiveIntegerIri);
                    }

                    case PositiveIntegerBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        BigDecimal num = ((BigDecimal) value).stripTrailingZeros();
                        String str = lexical.isEmpty() ? num.toPlainString() : lexical;
                        yield new TypedLiteral(str, xsdPositiveIntegerIri);
                    }

                    case DecimalClass _ ->
                    {
                        BigDecimal bn = ((BigDecimal) value).stripTrailingZeros();
                        yield new TypedLiteral((bn.scale() < 1 ? bn.setScale(1) : bn).toPlainString(), xsdDecimalIri);
                    }

                    case DecimalBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        BigDecimal num = ((BigDecimal) value).stripTrailingZeros();
                        String str = lexical.isEmpty() ? num.toPlainString() : lexical;
                        yield new TypedLiteral(str, xsdDecimalIri);
                    }

                    case FloatClass _ ->
                    {
                        yield new TypedLiteral(RyuFloat.floatToString((float) value), xsdFloatIri);
                    }

                    case FloatBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        String str = lexical.isEmpty() ? RyuFloat.floatToString((float) value) : lexical;
                        yield new TypedLiteral(str, xsdFloatIri);
                    }

                    case DoubleClass _ ->
                    {
                        yield new TypedLiteral(RyuDouble.doubleToString((double) value), xsdDoubleIri);
                    }

                    case DoubleBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        String str = lexical.isEmpty() ? RyuDouble.doubleToString((double) value) : lexical;
                        yield new TypedLiteral(str, xsdDoubleIri);
                    }

                    case DateTimeCompositeClass _ ->
                    {
                        int zone = rs.getInt(i++);
                        yield new TypedLiteral(dateTimeToString((LocalDateTime) value, zone), xsdDateTimeIri);
                    }

                    case DateTimeCompositeBaseClass _ ->
                    {
                        int zone = rs.getInt(i++);
                        String lexical = rs.getString(i++);
                        String str = lexical.isEmpty() ? dateTimeToString((LocalDateTime) value, zone) : lexical;
                        yield new TypedLiteral(str, xsdDateTimeIri);
                    }

                    case DateCompositeClass _ ->
                    {
                        int zone = rs.getInt(i++);
                        yield new TypedLiteral(dateToString((LocalDate) value, zone), xsdDateIri);
                    }

                    case DateCompositeBaseClass _ ->
                    {
                        int zone = rs.getInt(i++);
                        String lexical = rs.getString(i++);
                        String str = lexical.isEmpty() ? dateToString((LocalDate) value, zone) : lexical;
                        yield new TypedLiteral(str, xsdDateIri);
                    }

                    case DayTimeDurationClass _ ->
                    {
                        yield new TypedLiteral(durationToString((Long) value), xsdDayTimeDurationIri);
                    }

                    case DayTimeDurationBaseClass _ ->
                    {
                        String lexical = rs.getString(i++);
                        String str = lexical.isEmpty() ? durationToString((Long) value) : lexical;
                        yield new TypedLiteral(str, xsdDayTimeDurationIri);
                    }

                    case StringClass _ ->
                    {
                        yield new TypedLiteral(value.toString(), xsdStringIri);
                    }

                    case LangStringClass _ ->
                    {
                        String lang = rs.getString(i++);
                        yield new LangStringLiteral(value.toString(), lang);
                    }

                    case UserLiteralCompositeClass _ ->
                    {
                        String type = rs.getString(i++);
                        yield new TypedLiteral(value.toString(), new Iri(type));
                    }

                    case UserLiteralCompositeBaseClass _ ->
                    {
                        String type = rs.getString(i++);
                        String lexical = rs.getString(i++);
                        String str = lexical.isEmpty() ? value.toString() : lexical;
                        yield new TypedLiteral(str, new Iri(type));
                    }

                    case UnsupportedLiteralClass _ ->
                    {
                        String type = rs.getString(i++);
                        yield new TypedLiteral(value.toString(), new Iri(type));
                    }

                    default ->
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            idx++;
        }

        return true;
    }


    /**
     * Form of the query.
     *
     * @return form of the query
     */
    public ResultType getResultType()
    {
        return type;
    }


    /**
     * Warnings raised by the database statement.
     *
     * @return warnings raised by the database statement
     * @throws SQLException on database errors
     */
    public List<String> getWarnings() throws SQLException
    {
        LinkedList<String> warnings = new LinkedList<>();

        for(SQLWarning warning = rs.getStatement().getWarnings(); warning != null; warning = warning.getNextWarning())
            warnings.add(warning.getMessage());

        return warnings;
    }


    /**
     * Projected variables in column order.
     *
     * @return projected variables in column order
     */
    public List<Variable> getHeads()
    {
        return heads;
    }


    /**
     * Position of each projected variable in a row.
     *
     * @return position of each projected variable in a row
     */
    public Map<Variable, Integer> getVariableIndexes()
    {
        return varNames;
    }


    /**
     * Term of the given column of the current row; null if unbound.
     *
     * @param idx the column index
     * @return term of the given column of the current row; null if unbound
     */
    public RdfTerm get(int idx)
    {
        return rowData[idx];
    }


    /**
     * Term of the variable in the current row; null if unbound or not projected.
     *
     * @param var the variable
     * @return term of the variable in the current row; null if unbound or not projected
     */
    public RdfTerm get(Variable var)
    {
        Integer idx = varNames.get(var);

        if(idx == null)
            return null;

        return rowData[idx];
    }


    /**
     * Copy of the terms of the current row.
     *
     * @return copy of the terms of the current row
     */
    public RdfTerm[] getRow()
    {
        return rowData.clone();
    }


    @Override
    public void close() throws SQLException
    {
        rs.close();
    }


    /**
     * Canonical xsd:dayTimeDuration lexical form of a duration in microseconds.
     *
     * @param value duration in microseconds
     * @return canonical xsd:dayTimeDuration lexical form of a duration in microseconds
     */
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


    /**
     * Lexical form of a date-time given in UTC with the zone offset in seconds; without an offset when the zone is
     * {@link Integer#MIN_VALUE}.
     *
     * @param value the date-time in UTC
     * @param zone the timezone offset in seconds
     * @return lexical form of a date-time given in UTC with the zone offset in seconds; without an offset when the zone
     *         is {@link Integer#MIN_VALUE}
     */
    private String dateTimeToString(LocalDateTime value, int zone)
    {
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(zone != Integer.MIN_VALUE ? zone : 0);
        OffsetDateTime date = value.atOffset(offset);
        DateTimeFormatter format = zone != Integer.MIN_VALUE ? ISO_OFFSET_DATE_TIME : ISO_LOCAL_DATE_TIME;

        return date.format(format);
    }


    /**
     * Lexical form of a date with the zone offset in seconds; without an offset when the zone is
     * {@link Integer#MIN_VALUE}.
     *
     * @param value the date
     * @param zone the timezone offset in seconds
     * @return lexical form of a date with the zone offset in seconds; without an offset when the zone is
     *         {@link Integer#MIN_VALUE}
     */
    private static String dateToString(LocalDate value, int zone)
    {
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(zone != Integer.MIN_VALUE ? zone : 0);
        OffsetDateTime date = value.atStartOfDay().atOffset(offset);
        DateTimeFormatter format = zone != Integer.MIN_VALUE ? ISO_OFFSET_DATE : ISO_LOCAL_DATE;

        return date.format(format);
    }

}
