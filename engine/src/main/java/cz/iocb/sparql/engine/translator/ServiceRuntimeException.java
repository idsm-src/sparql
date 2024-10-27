package cz.iocb.sparql.engine.translator;



public class ServiceRuntimeException extends RuntimeException
{
    private static final long serialVersionUID = 1L;


    public ServiceRuntimeException(ServiceException exception)
    {
        super(exception);
    }
}
