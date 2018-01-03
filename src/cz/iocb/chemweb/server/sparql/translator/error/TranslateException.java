package cz.iocb.chemweb.server.sparql.translator.error;

import cz.iocb.chemweb.server.sparql.parser.Range;



/**
 * Exception class for the main translation process.
 *
 */
public class TranslateException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final Range range;
    private final String message;


    public TranslateException(ErrorType erType, Range range, Object... o)
    {
        this.range = range;
        this.message = String.format(erType.getText(), o);
    }



    /**
     * Constructs an exception object.
     *
     * @param message Error message.
     * @param range Error context.
     */
    /*
     * public TranslateException(String message, Range range) {
     * super(String.format("Line %d: %s", range.getStart().getLineNumber(),
     * message));
     *
     * this.range = range; this.message = message; }
     */



    /**
     * Get the context where the error occured.
     *
     * @return Error context.
     */
    public Range getRange()
    {
        return range;
    }

    /**
     * Get the error message.
     *
     * @return Error message.
     */
    public String getErrorMessage()
    {
        return message;
    }
}
