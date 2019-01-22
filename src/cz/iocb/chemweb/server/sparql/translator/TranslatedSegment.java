package cz.iocb.chemweb.server.sparql.translator;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;



public class TranslatedSegment
{
    private final List<String> scope;
    private final SqlIntercode intercode;


    public TranslatedSegment(List<String> scope, SqlIntercode intercode)
    {
        this.scope = scope;
        this.intercode = intercode;
    }


    public static List<String> mergeVariableLists(List<String> left, List<String> right)
    {
        ArrayList<String> merged = new ArrayList<String>(left.size() + right.size());

        merged.addAll(left);

        for(String variable : right)
            if(!merged.contains(variable))
                merged.add(variable);

        return merged;
    }


    public final List<String> getVariablesInScope()
    {
        return scope;
    }


    public final SqlIntercode getIntercode()
    {
        return intercode;
    }
}
