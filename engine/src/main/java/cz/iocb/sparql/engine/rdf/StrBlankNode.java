package cz.iocb.sparql.engine.rdf;

import java.nio.charset.StandardCharsets;
import java.util.Objects;



/**
 * Blank node with a string value; its label is {@code s}, the segment as eight hexadecimal digits, and the UTF-8 bytes
 * of the value with every non-alphanumeric byte escaped as {@code -XX}.
 */
public class StrBlankNode extends BlankNode
{
    /**
     * Hexadecimal digits for escaping bytes of the label.
     */
    private static final char[] encodeTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f' };

    /**
     * String value within the segment.
     */
    private final String value;

    /**
     * Segment the node belongs to.
     */
    private final int segment;


    /**
     * Creates the node.
     *
     * @param value the value
     * @param segment the segment
     */
    public StrBlankNode(String value, int segment)
    {
        this.value = value;
        this.segment = segment;
    }


    @Override
    public String getLabel()
    {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("s%8s", Integer.toHexString(segment)).replace(' ', '0'));

        for(int j = 0; j < data.length; j++)
        {
            if((data[j] < '0' || data[j] > '9') && (data[j] < 'A' || data[j] > 'Z') && (data[j] < 'a' || data[j] > 'z'))
            {
                int val = data[j] < 0 ? data[j] + 256 : data[j];
                builder.append('-');
                builder.append(encodeTable[val / 16]);
                builder.append(encodeTable[val % 16]);
            }
            else
            {
                builder.append((char) data[j]);
            }
        }

        return builder.toString();
    }


    /**
     * String value within the segment.
     *
     * @return string value within the segment
     */
    public String getValue()
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

        StrBlankNode other = (StrBlankNode) object;

        return Objects.equals(value, other.value) && segment == other.segment;
    }
}
