package cz.iocb.chemweb.server.sparql.parser.model.triple;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.BasicPattern;



/**
 * Triple without any syntax sugar, just a single subject ({@link #getSubject}), a single predicate
 * ({@link #getPredicate}) and a single object ( {@link #getObject}).
 */
public class Triple extends BaseElement implements BasicPattern
{
    private Node subject;
    private Verb predicate;
    private Node object;

    public Triple()
    {
    }

    public Triple(Node subject, Verb predicate, Node object)
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public Node getSubject()
    {
        return subject;
    }

    public void setSubject(Node subject)
    {
        this.subject = subject;
    }

    public Verb getPredicate()
    {
        return predicate;
    }

    public void setPredicate(Verb predicate)
    {
        this.predicate = predicate;
    }

    public Node getObject()
    {
        return object;
    }

    public void setObject(Node object)
    {
        this.object = object;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
