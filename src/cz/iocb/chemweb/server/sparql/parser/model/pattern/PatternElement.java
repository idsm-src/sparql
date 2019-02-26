package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import java.util.LinkedHashSet;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;



public abstract class PatternElement extends BaseElement
{
    protected final LinkedHashSet<String> variablesInScope = new LinkedHashSet<String>();


    public LinkedHashSet<String> getVariablesInScope()
    {
        return variablesInScope;
    }
}
