package cz.iocb.chemweb.server.sparql.parser;

import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.error.ParseException;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;



public class ParseResult
{
    final SelectQuery result;
    final List<ParseException> exceptions;

    public ParseResult(SelectQuery result, List<ParseException> exceptions)
    {
        this.result = result;
        this.exceptions = exceptions;
    }

    public SelectQuery getResult()
    {
        return result;
    }

    public List<ParseException> getExceptions()
    {
        return exceptions;
    }
}
