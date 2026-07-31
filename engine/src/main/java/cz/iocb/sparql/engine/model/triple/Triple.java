package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.pattern.BasicPattern;
import cz.iocb.sparql.engine.model.pattern.PatternElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Triple without any syntax sugar, just a single subject ({@link #getSubject}), a single predicate
 * ({@link #getPredicate}) and a single object ( {@link #getObject}).
 */
public class Triple extends PatternElement implements BasicPattern
{
    private final Node subject;
    private final Verb predicate;
    private final Node object;


    public Triple(Node subject, Verb predicate, Node object)
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;


        if(subject instanceof VariableNode variable)
            variablesInScope.add(variable);

        if(predicate instanceof VariableNode variable)
            variablesInScope.add(variable);

        if(object instanceof VariableNode variable)
            variablesInScope.add(variable);
    }


    public Node getSubject()
    {
        return subject;
    }


    public Verb getPredicate()
    {
        return predicate;
    }


    public Node getObject()
    {
        return object;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
