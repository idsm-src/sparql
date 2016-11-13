package cz.iocb.chemweb.server.sparql.parser.error;

import cz.iocb.chemweb.server.sparql.parser.Range;



/**
 * Represents error during parsing of a SPARQL query.
 *
 * <p>
 * Along with an error message, contains also the location of the error.
 *
 * <p>
 * A single ParseException can also represent multiple errors.
 */
public class ParseException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final Range range;

    public ParseException(ErrorType erType, Range range, Object... o)
    {
        super(String.format(erType.getText(), o));

        this.range = range;
    }

    public ParseException(UncheckedParseException ex)
    {
        super(ex.getMessage(), ex);

        this.range = ex.getRange();
    }

    public Range getRange()
    {
        return range;
    }
}
