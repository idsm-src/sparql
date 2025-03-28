package cz.iocb.sparql.engine.request;



public class LimitExceedException extends Exception
{
    private static final long serialVersionUID = 1L;


    public LimitExceedException(String message)
    {
        super(message);
    }
}
