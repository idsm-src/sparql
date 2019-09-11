package cz.iocb.chemweb.server.sparql.parser.model.triple;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
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


        if(subject instanceof Variable)
            variablesInScope.add(((Variable) subject).getName());

        if(predicate instanceof Variable)
            variablesInScope.add(((Variable) predicate).getName());

        if(object instanceof Variable)
            variablesInScope.add(((Variable) object).getName());
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
