package cz.iocb.chemweb.server.sparql.parser.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Virtuoso-specific declaration, used for configuring the query.
 */
public class Define extends BaseElement
{
    /**
     * Interface for values that a {@link Define} can contain.
     */
    public interface DefineValue extends Element
    {
    }

    /**
     * String value of a define.
     *
     * <p>
     * Exists only because {@link String} can't implement {@link DefineValue} directly.
     */
    public static class StringValue extends BaseElement implements DefineValue
    {
        private String value;

        public StringValue(String value)
        {
            setValue(value);
        }

        /**
         * The string value, including quotation marks.
         */
        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            if(value == null)
                throw new IllegalArgumentException();

            this.value = value;
        }

        @Override
        public <T> T accept(ElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }

    /**
     * Integer value of a define.
     *
     * <p>
     * Exists only because {@link BigInteger} can't implement {@link DefineValue} directly.
     */
    public static class IntegerValue extends BaseElement implements DefineValue
    {
        private BigInteger value;

        public IntegerValue(String value)
        {
            setValue(new BigInteger(value));
        }

        public IntegerValue(BigInteger value)
        {
            setValue(value);
        }

        public BigInteger getValue()
        {
            return value;
        }

        public void setValue(BigInteger value)
        {
            if(value == null)
                throw new IllegalArgumentException();

            this.value = value;
        }

        @Override
        public <T> T accept(ElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }

    private PrefixedName key;
    private List<DefineValue> values;

    public Define()
    {
    }

    public Define(PrefixedName key, Collection<DefineValue> values)
    {
        this.key = key;
        this.values = new ArrayList<>(values);
    }

    public PrefixedName getKey()
    {
        return key;
    }

    public void setKey(PrefixedName key)
    {
        this.key = key;
    }

    public List<DefineValue> getValues()
    {
        return values;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
