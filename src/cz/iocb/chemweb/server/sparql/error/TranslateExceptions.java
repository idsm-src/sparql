package cz.iocb.chemweb.server.sparql.error;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class TranslateExceptions extends Exception
{
    private static final long serialVersionUID = 1L;

    private final List<TranslateMessage> exceptions;


    /**
     * Constructs exception object.
     *
     * @param exceptions List of exceptions.
     */
    public TranslateExceptions(Collection<TranslateMessage> exceptions)
    {
        super(getMessage(exceptions));

        this.exceptions = new ArrayList<>(exceptions);
    }


    /**
     * Gets a list of exceptions.
     *
     * @return List of exceptions.
     */
    public List<TranslateMessage> getExceptions()
    {
        return exceptions;
    }


    /**
     * Gets a string of errors.
     *
     * @param exceptions List of exceptions.
     */
    private static String getMessage(Collection<TranslateMessage> exceptions)
    {
        Iterable<String> exceptionMessages = exceptions.stream().map(e -> e.getMessage())::iterator;

        return "Syntax error(s):" + System.lineSeparator() + String.join(System.lineSeparator(), exceptionMessages);
    }
}
