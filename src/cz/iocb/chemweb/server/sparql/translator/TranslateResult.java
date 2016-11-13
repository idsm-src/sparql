package cz.iocb.chemweb.server.sparql.translator;

import java.util.List;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;



public class TranslateResult
{
    final String result;
    final List<TranslateException> exceptions;
    final List<TranslateException> warnings;

    public TranslateResult(String result, List<TranslateException> exceptions, List<TranslateException> warnings)
    {
        this.result = result;
        this.exceptions = exceptions;
        this.warnings = warnings;
    }

    public String getResult()
    {
        return result;
    }

    public List<TranslateException> getExceptions()
    {
        return exceptions;
    }

    public List<TranslateException> getWarnings()
    {
        return warnings;
    }
}
