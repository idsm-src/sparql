package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import java.util.LinkedHashSet;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;



public abstract class PatternElement extends BaseElement
{
    protected final LinkedHashSet<Variable> variablesInScope = new LinkedHashSet<Variable>();


    public LinkedHashSet<Variable> getVariablesInScope()
    {
        return variablesInScope;
    }
}
