package cz.iocb.sparql.engine.translator;

import java.util.List;
import cz.iocb.sparql.engine.error.TranslateMessage;



/**
 * Outcome of a translation: the generated SQL and the errors and warnings collected on the way.
 */
public class TranslateResult
{
    /**
     * Generated SQL.
     */
    final String result;

    /**
     * Error messages.
     */
    final List<TranslateMessage> exceptions;

    /**
     * Warning messages.
     */
    final List<TranslateMessage> warnings;


    /**
     * Creates the result.
     *
     * @param result the generated SQL
     * @param exceptions the error messages
     * @param warnings the warning messages
     */
    public TranslateResult(String result, List<TranslateMessage> exceptions, List<TranslateMessage> warnings)
    {
        this.result = result;
        this.exceptions = exceptions;
        this.warnings = warnings;
    }


    /**
     * Generated SQL.
     *
     * @return generated SQL
     */
    public String getResult()
    {
        return result;
    }


    /**
     * Error messages.
     *
     * @return error messages
     */
    public List<TranslateMessage> getExceptions()
    {
        return exceptions;
    }


    /**
     * Warning messages.
     *
     * @return warning messages
     */
    public List<TranslateMessage> getWarnings()
    {
        return warnings;
    }
}
