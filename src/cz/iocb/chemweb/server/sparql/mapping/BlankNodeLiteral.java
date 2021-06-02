package cz.iocb.chemweb.server.sparql.mapping;

import java.nio.charset.StandardCharsets;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class BlankNodeLiteral implements Node
{
    public static enum Category
    {
        STR, INT
    }


    private final Category category;
    private final String label;
    private final int segment;


    public BlankNodeLiteral(Category category, int segment, String label)
    {
        this.category = category;
        this.segment = segment;
        this.label = label;
    }


    public BlankNodeLiteral(String id)
    {
        if(id.startsWith("S"))
        {
            String value = decodeBlankNodeValue(id);

            this.category = Category.STR;
            this.segment = Integer.parseInt(value.substring(0, 8));
            this.label = value.substring(8);
        }
        else if(id.startsWith("I"))
        {
            long value = Long.parseLong(id.substring(1));

            this.category = Category.INT;
            this.segment = (int) (value >> 32);
            this.label = Integer.toString((int) (value & 0xFFFFFFFF));
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }


    public Category getCategory()
    {
        return category;
    }


    public int getSegment()
    {
        return segment;
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

        if(category != literal.category)
            return false;

        if(segment != literal.segment)
            return false;

        if(!label.equals(literal.label))
            return false;

        return true;
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


    private static String decodeBlankNodeValue(String value)
    {
        byte[] buffer = new byte[value.length()];
        int length = 0;

        for(int j = 1; j < value.length(); j++)
        {
            if(value.charAt(j) == '_')
            {
                buffer[length++] = Byte.parseByte(value.substring(j + 1, j + 3), 16);
                j += 2;
            }
            else
            {
                buffer[length++] = (byte) (value.charAt(j));
            }
        }

        return new String(buffer, 0, length, StandardCharsets.UTF_8);
    }
}
