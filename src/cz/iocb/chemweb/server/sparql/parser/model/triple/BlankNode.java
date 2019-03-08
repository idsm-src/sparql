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


    protected BlankNode()
    {
        int id = nodeId.incrementAndGet();
        this.name = prefix + id;
    }


    public BlankNode(String name)
    {
        if(name.startsWith("_:"))
            name = '_' + name.substring(2);
        else if(!name.startsWith("_") && !name.startsWith("@"))
            name = '_' + name;

        this.name = name;
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
