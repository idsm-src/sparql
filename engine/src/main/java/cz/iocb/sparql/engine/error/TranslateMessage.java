package cz.iocb.sparql.engine.error;

import java.io.Serializable;
import cz.iocb.sparql.engine.model.base.Range;



/**
 * Diagnostic message (error or warning) produced while parsing or translating a query, located by the source
 * {@link Range} it refers to.
 */
public class TranslateMessage implements Serializable
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Source range the message refers to.
     */
    private final Range range;

    /**
     * Severity of the message.
     */
    private final MessageCategory category;

    /**
     * Formatted message text.
     */
    private final String message;


    /**
     * Creates a message of the given type, formatting its template with the arguments {@code o}.
     *
     * @param erType type of the message
     * @param range the source range
     * @param o arguments of the message template
     */
    public TranslateMessage(MessageType erType, Range range, Object... o)
    {
        this.range = range;
        this.category = erType.getCategory();
        this.message = String.format(erType.getText(), o);
    }


    /**
     * Source range the message refers to; may be null.
     *
     * @return source range the message refers to; may be null
     */
    public Range getRange()
    {
        return range;
    }


    /**
     * Severity of the message.
     *
     * @return severity of the message
     */
    public MessageCategory getCategory()
    {
        return category;
    }


    /**
     * Formatted message text.
     *
     * @return formatted message text
     */
    public String getMessage()
    {
        return message;
    }
}
