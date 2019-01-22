package cz.iocb.chemweb.server.sparql.parser.model.triple;

import java.util.concurrent.atomic.AtomicInteger;
import cz.iocb.chemweb.server.sparql.parser.BaseComplexNode;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;



/**
 * Named ({@link #getName}) blank node (for example {@code _:node}).
 *
 * <p>
 * Corresponds to the rule [142] BLANK_NODE_LABEL in the SPARQL grammar.
 */
public final class BlankNode extends BaseComplexNode implements Node, VariableOrBlankNode
{
    private static final String prefix = "@bn";
    private static final AtomicInteger nodeId = new AtomicInteger();
    private String name;

    public BlankNode(String name)
    {
        setName(name);
    }

    public static BlankNode getNewBlankNode()
    {
        int id = nodeId.incrementAndGet();
        return new BlankNode(prefix + id);
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if(name == null)
            throw new IllegalArgumentException();

        // NOTE: changed by galgonek
        // if (name.startsWith("_:"))
        // name = name.substring(2);

        this.name = name;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;

        BlankNode blankNode = (BlankNode) o;

        return name.equals(blankNode.name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
