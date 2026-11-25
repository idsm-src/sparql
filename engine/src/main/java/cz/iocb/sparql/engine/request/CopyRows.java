package cz.iocb.sparql.engine.request;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.iocb.sparql.engine.database.SqlType;



/**
 * Rows of the result of a statement that the database copies to the client ({@code COPY (statement) TO STDOUT}), read
 * through the copy API of the PostgreSQL driver. The database evaluates a copied statement whole, so possibly by
 * parallel workers, and sends its rows as it makes them, as far ahead of the reading as the buffers of the connection
 * take them, waiting for the reading otherwise; so the rows are read in parts, whereas the database evaluates a
 * statement whose rows a result set reads in parts, by fetches, without workers.
 * <p>
 * A row is a line of fields separated by tabs, the field of a NULL being {@code \N}; a backslash and the control
 * characters with names in C that a value holds are written by a backslash and a character (a tab by {@code \t}, a line
 * feed by {@code \n}), so no field holds a tab or a line feed. The field of a value is its text, which is read as the
 * driver reads the text of a value of a result set. The connection runs nothing else while it copies: rows that are
 * closed before their end has been read cancel the copy and read and drop the rows the database has sent before it took
 * the cancellation, and the error that ends the copy, so that the connection can run the next statement, and roll the
 * transaction, which the cancellation aborts, back to the savepoint made before the copy, so that the request can go on
 * as it can after a result set closed before its end. The request cancels the copy, which no statement of the driver
 * runs, through the connection of the driver, and only until the end of the copy has been read (see
 * {@link Request#cancel}), so that a cancellation reaches no later statement.
 */
final class CopyRows implements ResultRows
{
    /**
     * Logger of the copies.
     */
    private static final Logger logger = LoggerFactory.getLogger(CopyRows.class);

    /**
     * The request whose statement is copied.
     */
    private final Request request;

    /**
     * The connection of the request, which keeps the warnings the database raises while it copies the rows.
     */
    private final Connection connection;

    /**
     * The connection of the driver.
     */
    private final PGConnection driver;

    /**
     * The savepoint made before the copy.
     */
    private final Savepoint savepoint;

    /**
     * The copy, null until the database has started it.
     */
    private volatile CopyOut copy;

    /**
     * Data of the current row, null before the first row and after the end.
     */
    private byte[] row;

    /**
     * Positions of the fields of the current row in its data, the beginning and the end of every field.
     */
    private int[] bounds = new int[32];

    /**
     * Number of the fields of the current row.
     */
    private int fields;

    /**
     * Whether the end of the copy has been read.
     */
    private boolean ended;


    /**
     * Creates the rows of a copy over the connection of the request, which has to give the connection of the driver
     * (see {@link #isSupported}), and makes the savepoint the transaction is rolled back to when the rows are closed
     * before their end.
     *
     * @param request the request
     * @param connection the connection of the request
     * @throws SQLException when the connection does not give the connection of the driver, or when the savepoint cannot
     *             be made
     */
    CopyRows(Request request, Connection connection) throws SQLException
    {
        this.request = request;
        this.connection = connection;
        this.driver = connection.unwrap(PGConnection.class);
        this.savepoint = connection.setSavepoint();
    }


    /**
     * True if the connection gives the connection of the PostgreSQL driver, whose copy API the rows are read through; a
     * connection pool may not give it.
     *
     * @param connection the connection
     * @return true if the connection gives the connection of the driver, false otherwise
     * @throws SQLException on database errors
     */
    static boolean isSupported(Connection connection) throws SQLException
    {
        return connection.isWrapperFor(PGConnection.class);
    }


    /**
     * Starts the copy of the rows of the statement. The driver gives the copy when the database has sent its start,
     * which it sends together with the first rows, as it sends nothing before its buffer is full or the copy has ended.
     *
     * @param code SQL code of the statement
     * @throws SQLException when the copy cannot be started
     */
    void start(String code) throws SQLException
    {
        // the warnings of the statement are those the driver adds to the connection while the database copies the rows
        connection.clearWarnings();

        copy = driver.getCopyAPI().copyOut("COPY (" + code + ") TO STDOUT");
    }


    /**
     * Cancels the copy through the connection of the driver; called only until the end of the copy has been read.
     *
     * @throws SQLException when the cancellation cannot be sent
     */
    void cancel() throws SQLException
    {
        driver.cancelQuery();
    }


    /**
     * True if the database has started the copy, so that the statement of the copy has reached it.
     *
     * @return true if the database has started the copy, false otherwise
     */
    boolean isStarted()
    {
        return copy != null;
    }


    @Override
    public boolean next() throws SQLException
    {
        if(ended)
            return false;

        // a cancelled request ends at once, while the database may still be sending rows, which closing drops
        request.checkCanceled();

        byte[] data;

        try
        {
            data = copy.readFromCopy();
        }
        catch(SQLException e)
        {
            end();
            throw e;
        }

        if(data == null)
        {
            end();
            return false;
        }

        split(data);
        return true;
    }


    /**
     * Makes the data the current row and finds its fields: the line is ended by a line feed and its fields are
     * separated by tabs, which the database writes into no field unescaped.
     *
     * @param data data of the row
     */
    private void split(byte[] data)
    {
        int end = data.length > 0 && data[data.length - 1] == '\n' ? data.length - 1 : data.length;
        int start = 0;

        row = data;
        fields = 0;

        for(int i = 0; i <= end; i++)
        {
            if(i == end || data[i] == '\t')
            {
                if(2 * fields + 2 > bounds.length)
                    bounds = Arrays.copyOf(bounds, 2 * bounds.length);

                bounds[2 * fields] = start;
                bounds[2 * fields + 1] = i;
                fields++;
                start = i + 1;
            }
        }
    }


    @Override
    public String getString(int index) throws SQLException
    {
        if(row == null || index < 1 || index > fields)
            throw new SQLException("no column " + index + " in the current row");

        int start = bounds[2 * index - 2];
        int end = bounds[2 * index - 1];

        if(end - start == 2 && row[start] == '\\' && row[start + 1] == 'N')
            return null;

        int escape = start;

        while(escape < end && row[escape] != '\\')
            escape++;

        if(escape == end)
            return new String(row, start, end - start, StandardCharsets.UTF_8);

        // the escapes, made of ASCII characters, are replaced byte by byte, as no byte of the UTF-8 sequence of another
        // character is one
        byte[] text = Arrays.copyOfRange(row, start, end);
        int length = escape - start;

        for(int i = escape; i < end; i++)
            text[length++] = row[i] == '\\' && i + 1 < end ? unescape(row[++i]) : row[i];

        return new String(text, 0, length, StandardCharsets.UTF_8);
    }


    /**
     * The character a backslash and the given character stand for: a control character that has a name in C (a tab for
     * {@code t}), any other character itself (a backslash for a backslash).
     *
     * @param character the character after the backslash
     * @return the character they stand for
     */
    private static byte unescape(byte character)
    {
        return switch(character)
        {
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'v' -> 0x0B;
            default -> character;
        };
    }


    @Override
    public int getInt(int index) throws SQLException
    {
        Object value = getValue(index, SqlType.INT4);

        return value != null ? (Integer) value : 0;
    }


    /**
     * {@inheritDoc} The text of the value is read as the driver reads it: a boolean is {@code t} or {@code f}, a
     * floating-point number may be {@code NaN}, {@code Infinity} or {@code -Infinity}, and a date and a timestamp are
     * written in the date style ISO, which the driver requires.
     */
    @Override
    public Object getValue(int index, SqlType type) throws SQLException
    {
        String text = getString(index);
        Class<?> javaClass = type.getJavaClass();

        if(text == null)
            return null;

        try
        {
            if(javaClass == String.class)
                return text;
            else if(javaClass == Boolean.class)
                return toBoolean(text);
            else if(javaClass == Short.class)
                return Short.valueOf(text);
            else if(javaClass == Integer.class)
                return Integer.valueOf(text);
            else if(javaClass == Long.class)
                return Long.valueOf(text);
            else if(javaClass == BigDecimal.class)
                return new BigDecimal(text);
            else if(javaClass == Float.class)
                return Float.valueOf(text);
            else if(javaClass == Double.class)
                return Double.valueOf(text);
            else if(javaClass == LocalDate.class)
                return toDate(text);
            else if(javaClass == OffsetDateTime.class)
                return toTimestamp(text);
        }
        catch(IllegalArgumentException | DateTimeException e)
        {
            throw new SQLException("bad value of type " + type.getName() + ": " + text, e);
        }

        throw new SQLException("values of type " + type.getName() + " are not read");
    }


    @Override
    public List<String> getWarnings() throws SQLException
    {
        List<String> warnings = new ArrayList<>();

        for(SQLWarning warning = connection.getWarnings(); warning != null; warning = warning.getNextWarning())
            warnings.add(warning.getMessage());

        return warnings;
    }


    /**
     * {@inheritDoc} A copy whose end has not been read is cancelled, the rows the database has sent before it took the
     * cancellation are read and dropped, as is the error ending the copy, and the transaction is rolled back to the
     * savepoint made before the copy.
     */
    @Override
    public void close() throws SQLException
    {
        if(ended || copy == null)
            return;

        SQLException failure = null;
        long dropped = 0;

        try
        {
            cancel();
        }
        catch(SQLException e)
        {
            // the copy is read to its end then
            failure = e;
        }

        try
        {
            while(copy.readFromCopy() != null)
                dropped++;
        }
        catch(SQLException e)
        {
            // the error ending the cancelled copy
        }
        finally
        {
            end();
        }

        logger.trace("copy cancelled, {} rows dropped", dropped);

        connection.rollback(savepoint);

        if(failure != null)
            throw failure;
    }


    /**
     * Records that the end of the copy has been read.
     */
    private void end()
    {
        ended = true;
        row = null;
        request.endCopy();
    }


    /**
     * The boolean of the text form of a value of the type {@code bool}.
     *
     * @param text the text, {@code t} or {@code f}
     * @return the boolean
     * @throws IllegalArgumentException when the text is neither
     */
    private static Boolean toBoolean(String text)
    {
        return switch(text)
        {
            case "t" -> Boolean.TRUE;
            case "f" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException();
        };
    }


    /**
     * The date of the text form of a value of the type {@code date} in the date style ISO: the year of at least four
     * digits, the month and the day, followed by {@code BC} for a year before Christ, which is the year 1 - year of the
     * proleptic calendar; or {@code infinity} and {@code -infinity}, which are {@link LocalDate#MAX} and
     * {@link LocalDate#MIN} as the driver reads them.
     *
     * @param text the text
     * @return the date
     * @throws DateTimeException when the text is no such form
     */
    static LocalDate toDate(String text)
    {
        if(text.equals("infinity"))
            return LocalDate.MAX;
        else if(text.equals("-infinity"))
            return LocalDate.MIN;

        boolean bc = text.endsWith(" BC");
        int end = bc ? text.length() - 3 : text.length();
        int dash = text.indexOf('-');

        expect(isAt(text, dash + 3, '-') && end == dash + 6, text);

        int year = number(text, 0, dash);

        return LocalDate.of(bc ? 1 - year : year, number(text, dash + 1, dash + 3), number(text, dash + 4, dash + 6));
    }


    /**
     * The instant of the text form of a value of the type {@code timestamptz} in the date style ISO, as a date-time of
     * the offset UTC, as the driver reads it: the date as {@link #toDate} reads it, with the time and the offset of the
     * time zone of the session before {@code BC}; the seconds with a fraction of up to nine digits, and the offset in
     * hours with the minutes and the seconds that are not zero; or {@code infinity} and {@code -infinity}, which are
     * {@link OffsetDateTime#MAX} and {@link OffsetDateTime#MIN}.
     *
     * @param text the text
     * @return the instant
     * @throws DateTimeException when the text is no such form
     */
    static OffsetDateTime toTimestamp(String text)
    {
        if(text.equals("infinity"))
            return OffsetDateTime.MAX;
        else if(text.equals("-infinity"))
            return OffsetDateTime.MIN;

        boolean bc = text.endsWith(" BC");
        int end = bc ? text.length() - 3 : text.length();
        int dash = text.indexOf('-');

        expect(isAt(text, dash + 3, '-') && isAt(text, dash + 6, ' ') && isAt(text, dash + 9, ':')
                && isAt(text, dash + 12, ':'), text);

        int year = number(text, 0, dash);
        int position = dash + 15;
        int nanos = 0;

        if(isAt(text, position, '.'))
        {
            int start = ++position;

            while(position < end && isDigit(text.charAt(position)))
                nanos = 10 * nanos + text.charAt(position++) - '0';

            expect(position > start && position - start <= 9, text);

            for(int i = position - start; i < 9; i++)
                nanos *= 10;
        }

        expect(isAt(text, position, '+') || isAt(text, position, '-'), text);

        int sign = text.charAt(position) == '-' ? -1 : 1;
        int[] offset = new int[3];

        for(int i = 0; i < offset.length && position < end; i++, position += 3)
        {
            expect(i == 0 || isAt(text, position, ':'), text);
            offset[i] = sign * number(text, position + 1, position + 3);
        }

        expect(position == end, text);

        LocalDateTime local = LocalDateTime.of(bc ? 1 - year : year, number(text, dash + 1, dash + 3),
                number(text, dash + 4, dash + 6), number(text, dash + 7, dash + 9), number(text, dash + 10, dash + 12),
                number(text, dash + 13, dash + 15), nanos);

        return OffsetDateTime.of(local, ZoneOffset.ofHoursMinutesSeconds(offset[0], offset[1], offset[2]))
                .withOffsetSameInstant(ZoneOffset.UTC);
    }


    /**
     * The number written by the decimal digits between the positions of the text.
     *
     * @param text the text
     * @param begin position of the first digit
     * @param end position after the last digit
     * @return the number
     * @throws DateTimeException when there is no digit or something else than digits between the positions
     */
    private static int number(String text, int begin, int end)
    {
        expect(begin >= 0 && begin < end && end <= text.length(), text);

        int value = 0;

        for(int i = begin; i < end; i++)
        {
            expect(isDigit(text.charAt(i)), text);
            value = 10 * value + text.charAt(i) - '0';
        }

        return value;
    }


    /**
     * True if the character is a decimal digit of ASCII.
     *
     * @param character the character
     * @return true if the character is a decimal digit of ASCII, false otherwise
     */
    private static boolean isDigit(char character)
    {
        return character >= '0' && character <= '9';
    }


    /**
     * True if the text holds the character at the position.
     *
     * @param text the text
     * @param position the position, which may lie outside the text
     * @param character the character
     * @return true if the text holds the character at the position, false otherwise
     */
    private static boolean isAt(String text, int position, char character)
    {
        return position >= 0 && position < text.length() && text.charAt(position) == character;
    }


    /**
     * Checks a condition of the text form of a value.
     *
     * @param condition the condition
     * @param text the text form
     * @throws DateTimeException when the condition does not hold
     */
    private static void expect(boolean condition, String text)
    {
        if(!condition)
            throw new DateTimeException("unexpected text form: " + text);
    }
}
