package cz.iocb.chemweb.server.services.check;

import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.shared.services.check.CheckerWarning;



public class CheckerWarningFactory
{
    public static CheckerWarning create(Range range, String type, String message)
    {
        if(range == null)
        {
            System.err.println("error: warning without range: " + type + ": " + message);
            return null;
        }

        return new CheckerWarning(range.getStart().getLineNumber() - 1, range.getStart().getPositionInLine(),
                range.getEnd().getLineNumber() - 1, range.getEnd().getPositionInLine() + 1, type, message);
    }
}
