package cz.iocb.chemweb.server.sparql.parser.error;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class ParseExceptions extends Exception
{
    private static final long serialVersionUID = 1L;


    private final List<ParseException> exceptions;


    public ParseExceptions(Collection<ParseException> exceptions)
    {
        super(getMessage(exceptions));

        this.exceptions = new ArrayList<>(exceptions);
    }


    private static String getMessage(Collection<ParseException> exceptions)
    {
        Iterable<String> exceptionMessages = exceptions.stream().map(e -> e.getMessage())::iterator;

        return "Syntax error(s):" + System.lineSeparator() + String.join(System.lineSeparator(), exceptionMessages);
    }

    public List<ParseException> getExceptions()
    {
        return exceptions;
    }
}
