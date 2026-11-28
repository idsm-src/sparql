package cz.iocb.sparql.endpoint;

import static cz.iocb.sparql.engine.config.SettingValues.checkDuration;
import static cz.iocb.sparql.engine.config.SettingValues.checkRange;
import static cz.iocb.sparql.engine.config.SettingValues.parseDuration;
import static cz.iocb.sparql.engine.config.SettingValues.parseInt;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import cz.iocb.sparql.engine.config.SettingValues;



/**
 * Settings of the endpoint servlet (see {@link EndpointServlet}): how the rows of a result are read from the database,
 * the time limits of the requests, the size limit of their SQL, and how often the servlet asks whether the client of a
 * request still waits for its result. The settings are immutable: the {@code with} methods derive changed copies,
 * checking the values given. The default of a setting, which {@link #DEFAULT} holds, is the initial value of its field,
 * whose description tells why the value is what it is where there is a reason for it. Each setting has a name, the name
 * of its accessor written in lower case with its words joined by hyphens, like {@code fetch-size}, by which an initial
 * parameter of the servlet sets it to the value its text gives (see {@link #with(String, String)}).
 */
public final class ServletSettings implements Cloneable
{
    /**
     * The default settings.
     */
    public static final ServletSettings DEFAULT = new ServletSettings();

    /**
     * The names of the settings, in the order of their accessors.
     */
    public static final List<String> NAMES = List.of("fetch-size", "timeout", "max-timeout", "sql-query-size-limit",
            "client-watch-interval", "client-check-rows", "client-check-interval");

    /**
     * Number of the rows of a result read from the database at once (see
     * {@link cz.iocb.sparql.engine.request.Request#execute}): a positive number reads a result in parts, so that its
     * rows are written to the client as they come, by a copy of the result of its statement, which the database
     * evaluates whole, by parallel workers too, while it sends the rows, where the driver lets it, and by fetches of so
     * many rows otherwise; zero reads every result whole, as the engine reads the result of an ASK or a DESCRIBE query,
     * of a small limit and of aggregates without GROUP BY anyway.
     */
    private int fetchSize = 1000;

    /**
     * Time limit of a request that asks for no time limit of its own by the parameter {@code timeout}; zero for none.
     */
    private Duration timeout = Duration.ofSeconds(1000);

    /**
     * Longest time limit a request may ask for by the parameter {@code timeout}, which gives a number of seconds.
     */
    private Duration maxTimeout = Duration.ofSeconds(6000);

    /**
     * Largest number of the characters of the SQL code generated for the query of a request, whose translation is
     * refused beyond it; zero for none.
     */
    private int sqlQuerySizeLimit = 0;

    /**
     * Time between two reads of the socket of the connection of a request by the watch of the connection, which cancels
     * the request once the client has closed the connection (see {@link ClientWatch}): the database keeps evaluating
     * the statement of a request nobody waits for any more for half the time at most, while a read that does not block
     * costs little.
     */
    private Duration clientWatchInterval = Duration.ofMillis(500);

    /**
     * Largest number of the rows of a result written between two checks that the client still takes the response (see
     * {@link EndpointServlet.ClientCheck}), each of which flushes the writer of the response: the rows written after
     * the client has closed the connection are wasted, as are the flushes of the rows written before.
     */
    private int clientCheckRows = 1000;

    /**
     * Longest time between two checks that the client still takes the response (see
     * {@link EndpointServlet.ClientCheck}), for the rows that come slowly; zero checks it with every row.
     */
    private Duration clientCheckInterval = Duration.ofSeconds(1);


    /**
     * Creates the default settings.
     */
    private ServletSettings()
    {
    }


    /**
     * Number of the rows of a result read from the database at once, zero for the whole result (the setting
     * {@code fetch-size}).
     *
     * @return number of the rows of a result read at once
     */
    public int fetchSize()
    {
        return fetchSize;
    }


    /**
     * Copy with the given number of the rows of a result read from the database at once.
     *
     * @param fetchSize number of the rows of a result read at once, zero for the whole result
     * @return copy with the given number of rows
     * @throws IllegalArgumentException if the number is negative
     */
    public ServletSettings withFetchSize(int fetchSize)
    {
        ServletSettings copy = copy();
        copy.fetchSize = checkRange("fetch-size", fetchSize, 0, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Time limit of a request that asks for no time limit of its own, zero for none (the setting {@code timeout}).
     *
     * @return time limit of a request that asks for none
     */
    public Duration timeout()
    {
        return timeout;
    }


    /**
     * Copy with the given time limit of a request that asks for no time limit of its own.
     *
     * @param timeout time limit of a request that asks for none, zero for none
     * @return copy with the given time limit
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is negative or longer than a {@code long} number of nanoseconds
     */
    public ServletSettings withTimeout(Duration timeout)
    {
        ServletSettings copy = copy();
        copy.timeout = checkDuration("timeout", timeout, Duration.ZERO);
        return copy;
    }


    /**
     * Longest time limit a request may ask for (the setting {@code max-timeout}).
     *
     * @return longest time limit a request may ask for
     */
    public Duration maxTimeout()
    {
        return maxTimeout;
    }


    /**
     * Copy with the given longest time limit a request may ask for.
     *
     * @param maxTimeout longest time limit a request may ask for
     * @return copy with the given time limit
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is not positive or longer than a {@code long} number of nanoseconds
     */
    public ServletSettings withMaxTimeout(Duration maxTimeout)
    {
        ServletSettings copy = copy();
        copy.maxTimeout = checkDuration("max-timeout", maxTimeout, Duration.ofNanos(1));
        return copy;
    }


    /**
     * Largest number of the characters of the SQL code generated for the query of a request, zero for none (the setting
     * {@code sql-query-size-limit}).
     *
     * @return largest number of the characters of the SQL code of a request
     */
    public int sqlQuerySizeLimit()
    {
        return sqlQuerySizeLimit;
    }


    /**
     * Copy with the given largest number of the characters of the SQL code generated for the query of a request.
     *
     * @param sqlQuerySizeLimit largest number of the characters of the SQL code of a request, zero for none
     * @return copy with the given number of characters
     * @throws IllegalArgumentException if the number is negative
     */
    public ServletSettings withSqlQuerySizeLimit(int sqlQuerySizeLimit)
    {
        ServletSettings copy = copy();
        copy.sqlQuerySizeLimit = checkRange("sql-query-size-limit", sqlQuerySizeLimit, 0, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Time between two reads of the socket of the connection of a request by the watch of the connection (the setting
     * {@code client-watch-interval}).
     *
     * @return time between two reads of the socket of a connection
     */
    public Duration clientWatchInterval()
    {
        return clientWatchInterval;
    }


    /**
     * Copy with the given time between two reads of the socket of the connection of a request by the watch of the
     * connection.
     *
     * @param clientWatchInterval time between two reads of the socket of a connection
     * @return copy with the given time
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is less than a millisecond or longer than a {@code long} number of
     *             nanoseconds
     */
    public ServletSettings withClientWatchInterval(Duration clientWatchInterval)
    {
        ServletSettings copy = copy();
        copy.clientWatchInterval = checkDuration("client-watch-interval", clientWatchInterval, Duration.ofMillis(1));
        return copy;
    }


    /**
     * Largest number of the rows of a result written between two checks that the client still takes the response (the
     * setting {@code client-check-rows}).
     *
     * @return largest number of the rows written between two checks
     */
    public int clientCheckRows()
    {
        return clientCheckRows;
    }


    /**
     * Copy with the given largest number of the rows of a result written between two checks that the client still takes
     * the response.
     *
     * @param clientCheckRows largest number of the rows written between two checks
     * @return copy with the given number of rows
     * @throws IllegalArgumentException if the number is not positive
     */
    public ServletSettings withClientCheckRows(int clientCheckRows)
    {
        ServletSettings copy = copy();
        copy.clientCheckRows = checkRange("client-check-rows", clientCheckRows, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Longest time between two checks that the client still takes the response (the setting
     * {@code client-check-interval}).
     *
     * @return longest time between two checks
     */
    public Duration clientCheckInterval()
    {
        return clientCheckInterval;
    }


    /**
     * Copy with the given longest time between two checks that the client still takes the response.
     *
     * @param clientCheckInterval longest time between two checks, zero to check with every row
     * @return copy with the given time
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is negative or longer than a {@code long} number of nanoseconds
     */
    public ServletSettings withClientCheckInterval(Duration clientCheckInterval)
    {
        ServletSettings copy = copy();
        copy.clientCheckInterval = checkDuration("client-check-interval", clientCheckInterval, Duration.ZERO);
        return copy;
    }


    /**
     * Copy with the setting of the given name set to the value its text gives (see {@link SettingValues}), as an
     * initial parameter of the servlet sets it: the numbers of rows and characters are integers, and the times are
     * integers followed by their units, or without a unit in seconds for {@code timeout} and {@code max-timeout} and in
     * milliseconds for the others.
     *
     * @param name name of the setting (see {@link #NAMES})
     * @param value text of the value
     * @return copy with the setting set to the value
     * @throws IllegalArgumentException if no setting has the name, or the text gives no value of the setting
     */
    public ServletSettings with(String name, String value)
    {
        return switch(name)
        {
            case "fetch-size" -> withFetchSize(parseInt(name, value));
            case "timeout" -> withTimeout(parseDuration(name, value, ChronoUnit.SECONDS));
            case "max-timeout" -> withMaxTimeout(parseDuration(name, value, ChronoUnit.SECONDS));
            case "sql-query-size-limit" -> withSqlQuerySizeLimit(parseInt(name, value));
            case "client-watch-interval" -> withClientWatchInterval(parseDuration(name, value, ChronoUnit.MILLIS));
            case "client-check-rows" -> withClientCheckRows(parseInt(name, value));
            case "client-check-interval" -> withClientCheckInterval(parseDuration(name, value, ChronoUnit.MILLIS));
            default -> throw new IllegalArgumentException("unknown setting: " + name);
        };
    }


    /**
     * Copy of the settings, which a {@code with} method changes before it hands it out.
     *
     * @return copy of the settings
     */
    private ServletSettings copy()
    {
        try
        {
            return (ServletSettings) clone();
        }
        catch(CloneNotSupportedException e)
        {
            throw new AssertionError(e);
        }
    }
}
