package cz.iocb.chemweb.server.sparql.parser.error;

import cz.iocb.chemweb.server.sparql.parser.Range;



public class UncheckedParseException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final Range range;



    public UncheckedParseException(ErrorType erType, Range range, Object... o)
    {
        super(String.format(erType.getText(), o));

        this.range = range;
    }

    public UncheckedParseException(ErrorType erType, Throwable cause, Range range, Object... o)
    {
        super(String.format(erType.getText(), o), cause);

        this.range = range;
    }

    public Range getRange()
    {
        return range;
    }
}
