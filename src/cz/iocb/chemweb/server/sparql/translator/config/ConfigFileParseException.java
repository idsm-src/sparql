package cz.iocb.chemweb.server.sparql.translator.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



/**
 * Exception class for ConfigFileParser
 * 
 */
public class ConfigFileParseException extends Exception
{
    private static final long serialVersionUID = 1L;

    private List<ConfigFileParseException> exceptions;
    private int lineNo;

    /**
     * Constructs an exception object.
     * 
     * @param message Error message.
     * @param lineNo Line number of the error.
     */
    public ConfigFileParseException(String message, int lineNo)
    {
        super(String.format("Line %d of Config File: %s", lineNo, message));

        this.lineNo = lineNo;
    }

    /**
     * Constructs exception object.
     * 
     * @param exceptions List of exceptions.
     */
    public ConfigFileParseException(Collection<ConfigFileParseException> exceptions)
    {
        super(getMessage(exceptions));

        this.exceptions = new ArrayList<>(exceptions);
    }

    /**
     * Gets a string of errors.
     * 
     * @param exceptions List of exceptions.
     */
    private static String getMessage(Collection<ConfigFileParseException> exceptions)
    {
        Iterable<String> exceptionMessages = exceptions.stream().map(e -> e.getMessage())::iterator;

        return "Syntax error(s):" + System.lineSeparator() + String.join(System.lineSeparator(), exceptionMessages);
    }

    /**
     * Gets a list of exceptions.
     * 
     * @return List of exceptions.
     */
    public List<ConfigFileParseException> getExceptions()
    {
        return exceptions;
    }

    /**
     * Get number of line of the error.
     * 
     * @return Line number.
     */
    public int getLineNo()
    {
        return lineNo;
    }
}
