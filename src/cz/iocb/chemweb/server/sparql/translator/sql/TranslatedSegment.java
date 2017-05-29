package cz.iocb.chemweb.server.sparql.translator.sql;

import java.util.ArrayList;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlIntercode;



public class TranslatedSegment
{
    private final ArrayList<String> scope;
    private final SqlIntercode intercode;


    public TranslatedSegment(ArrayList<String> scope, SqlIntercode intercode)
    {
        this.scope = scope;
        this.intercode = intercode;
    }


    public static ArrayList<String> mergeVariableLists(ArrayList<String> left, ArrayList<String> right)
    {
        ArrayList<String> merged = new ArrayList<String>(left.size() + right.size());

        merged.addAll(left);

        for(String variable : right)
            if(!merged.contains(variable))
                merged.add(variable);

        return merged;
    }


    public final ArrayList<String> getVariablesInScope()
    {
        return scope;
    }


    public final SqlIntercode getIntercode()
    {
        return intercode;
    }
}
