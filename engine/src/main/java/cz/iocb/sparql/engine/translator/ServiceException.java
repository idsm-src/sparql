package cz.iocb.sparql.engine.translator;



/**
 * Failure of a federated SERVICE call (unreachable endpoint, malformed answer, exceeded limits).
 */
public class ServiceException extends Exception
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
    public ServiceException(String message)
    {
        super(message);
    }
}
