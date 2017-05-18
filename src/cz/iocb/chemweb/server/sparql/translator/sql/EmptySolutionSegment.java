package cz.iocb.chemweb.server.sparql.translator.sql;



public class EmptySolutionSegment extends TranslatedSegment
{
    private static final EmptySolutionSegment singleton = new EmptySolutionSegment();


    private EmptySolutionSegment()
    {
        super("SELECT 1", new UsedVariables());
    }


    public static EmptySolutionSegment get()
    {
        return singleton;
    }
}
