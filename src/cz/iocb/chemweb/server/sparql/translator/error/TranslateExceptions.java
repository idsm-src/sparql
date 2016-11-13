package cz.iocb.chemweb.server.sparql.translator.error;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class TranslateExceptions extends Exception
{
    private static final long serialVersionUID = 1L;

    private final List<TranslateException> exceptions;


    /**
     * Constructs exception object.
     *
     * @param exceptions List of exceptions.
     */
    public TranslateExceptions(Collection<TranslateException> exceptions)
    {
        super(getMessage(exceptions));

        this.exceptions = new ArrayList<>(exceptions);
    }


    /**
     * Gets a list of exceptions.
     *
     * @return List of exceptions.
     */
    public List<TranslateException> getExceptions()
    {
        return exceptions;
    }


    /**
     * Gets a string of errors.
     *
     * @param exceptions List of exceptions.
     */
    private static String getMessage(Collection<TranslateException> exceptions)
    {
        Iterable<String> exceptionMessages = exceptions.stream().map(e -> e.getMessage())::iterator;

        return "Syntax error(s):" + System.lineSeparator() + String.join(System.lineSeparator(), exceptionMessages);
    }
}
