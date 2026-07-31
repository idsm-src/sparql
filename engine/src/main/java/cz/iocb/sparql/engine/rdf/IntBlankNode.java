package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



public class IntBlankNode extends BlankNode
{
    private final int value;
    private final int segment;


    public IntBlankNode(int value, int segment)
    {
        this.value = value;
        this.segment = segment;
    }


    @Override
    public String getLabel()
    {
        return String.format("i%8s%8s", Integer.toHexString(segment), Integer.toHexString(value)).replace(' ', '0');
    }


    public int getValue()
    {
        return value;
    }


    public int getSegment()
    {
        return segment;
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(value, segment);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        IntBlankNode other = (IntBlankNode) object;

        return value == other.value && segment == other.segment;
    }
}
