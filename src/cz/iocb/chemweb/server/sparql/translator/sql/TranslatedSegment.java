package cz.iocb.chemweb.server.sparql.translator.sql;



public class TranslatedSegment
{
    private final String code;
    private final UsedVariables variables;


    public TranslatedSegment(String code, UsedVariables variables)
    {
        this.code = code;
        this.variables = variables;
    }


    public final String getCode()
    {
        return code;
    }


    public final UsedVariables getVariables()
    {
        return variables;
    }
}
