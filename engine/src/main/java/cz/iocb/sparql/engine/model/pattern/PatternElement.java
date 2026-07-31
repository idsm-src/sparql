package cz.iocb.sparql.engine.model.pattern;

import java.util.LinkedHashSet;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.BaseElement;



public abstract class PatternElement extends BaseElement
{
    protected final LinkedHashSet<VariableNode> variablesInScope = new LinkedHashSet<>();


    public LinkedHashSet<VariableNode> getVariablesInScope()
    {
        return variablesInScope;
    }
}
