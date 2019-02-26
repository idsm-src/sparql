package cz.iocb.chemweb.server.sparql.parser.model.triple;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.BasicPattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.PatternElement;



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


        if(subject instanceof VariableOrBlankNode)
            variablesInScope.add(((VariableOrBlankNode) subject).getName());

        if(predicate instanceof VariableOrBlankNode)
            variablesInScope.add(((VariableOrBlankNode) predicate).getName());

        if(object instanceof VariableOrBlankNode)
            variablesInScope.add(((VariableOrBlankNode) object).getName());
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
