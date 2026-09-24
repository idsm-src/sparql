package cz.iocb.sparql.engine.error;

import java.util.ArrayList;
import java.util.List;



/**
 * Thrown when parsing or translation of a query produced at least one error message. The exception message is the text
 * of the first one.
 */
public class TranslateExceptions extends Exception
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The error messages carried by the exception.
     */
    private final List<TranslateMessage> messages;


    /**
     * Creates the exception from the error messages; at least one is required.
     *
     * @param messages the error messages
     */
    public TranslateExceptions(List<TranslateMessage> messages)
    {
        super(getMessage(messages));

        this.messages = new ArrayList<>(messages);
    }


    /**
     * The error messages that caused the exception, in order of detection.
     *
     * @return the error messages that caused the exception, in order of detection
     */
    public List<TranslateMessage> getMessages()
    {
        return messages;
    }


    /**
     * Text of the first message, used as the exception message.
     *
     * @param messages the error messages
     * @return text of the first message, used as the exception message
     */
    private static String getMessage(List<TranslateMessage> messages)
    {
        return messages.get(0).getMessage();
    }
}
