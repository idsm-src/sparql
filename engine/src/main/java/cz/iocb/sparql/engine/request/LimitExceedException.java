package cz.iocb.sparql.engine.request;



/**
 * Thrown when a query exceeds a configured limit, e.g. the maximum size of the generated SQL.
 */
public class LimitExceedException extends Exception
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Creates the exception with the message.
     *
     * @param message the message
     */
    public LimitExceedException(String message)
    {
        super(message);
    }
}
