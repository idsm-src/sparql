package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class BlankNodeLiteral implements Node
{
    private final String label;


    public BlankNodeLiteral(String label)
    {
        this.label = label;
    }


    public String getLabel()
    {
        return label;
    }


    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;

        if(o == null || getClass() != o.getClass())
            return false;

        BlankNodeLiteral literal = (BlankNodeLiteral) o;

        return label.equals(literal.label);
    }


    @Override
    public int hashCode()
    {
        return label.hashCode();
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return null;
    }


    @Override
    public Range getRange()
    {
        return null;
    }


    @Override
    public void setRange(Range range)
    {
    }
}
