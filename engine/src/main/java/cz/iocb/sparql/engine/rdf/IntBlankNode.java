package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



/**
 * Blank node with an integer value; its label is {@code i} followed by the segment and the value, each as eight
 * hexadecimal digits.
 */
public class IntBlankNode extends BlankNode
{
    /**
     * Integer value within the segment.
     */
    private final int value;

    /**
     * Segment the node belongs to.
     */
    private final int segment;


    /**
     * Creates the node.
     *
     * @param value the integer value
     * @param segment the segment
     */
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


    /**
     * Integer value within the segment.
     *
     * @return integer value within the segment
     */
    public int getValue()
    {
        return value;
    }


    /**
     * Segment the node belongs to.
     *
     * @return segment the node belongs to
     */
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
