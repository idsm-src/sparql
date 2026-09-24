package cz.iocb.sparql.engine.translator;



/**
 * Unchecked wrapper of a {@link ServiceException} for the visitor code that cannot declare it; unwrapped by
 * {@link TranslateVisitor#translate}.
 */
public class ServiceRuntimeException extends RuntimeException
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Wraps the exception.
     *
     * @param exception the wrapped exception
     */
    public ServiceRuntimeException(ServiceException exception)
    {
        super(exception);
    }
}
