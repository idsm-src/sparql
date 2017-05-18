package cz.iocb.chemweb.server.sparql.translator.sql;



public class NoSolutionSegment extends TranslatedSegment
{
    public NoSolutionSegment(UsedVariables variables)
    {
        super("SELECT 1 WHERE false", variables);
    }


    public NoSolutionSegment()
    {
        this(new UsedVariables());
    }
}
