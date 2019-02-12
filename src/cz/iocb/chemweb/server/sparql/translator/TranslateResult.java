package cz.iocb.chemweb.server.sparql.translator;

import java.util.List;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;



public class TranslateResult
{
    final String result;
    final List<TranslateMessage> exceptions;
    final List<TranslateMessage> warnings;

    public TranslateResult(String result, List<TranslateMessage> exceptions, List<TranslateMessage> warnings)
    {
        this.result = result;
        this.exceptions = exceptions;
        this.warnings = warnings;
    }

    public String getResult()
    {
        return result;
    }

    public List<TranslateMessage> getExceptions()
    {
        return exceptions;
    }

    public List<TranslateMessage> getWarnings()
    {
        return warnings;
    }
}
