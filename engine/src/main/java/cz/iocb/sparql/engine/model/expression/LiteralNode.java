package cz.iocb.sparql.engine.model.expression;

import java.util.Objects;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents a literal: a lexical value ({@link #getValue}) with either a datatype IRI ({@link #getType}) or a language
 * tag ({@link #getTag}). Numeric and boolean shorthand forms get the corresponding xsd datatype.
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar:
 * <ul>
 * <li>[129] RDFLiteral
 * <li>[130] NumericLiteral
 * <li>[134] BooleanLiteral
 * </ul>
 */
public class LiteralNode extends BaseComplexNode implements Expression, Node
{
    /**
     * Lexical form.
     */
    private final String value;

    /**
     * Datatype IRI, null for language-tagged literals.
     */
    private final IriNode type;

    /**
     * Language tag, null for typed literals.
     */
    private final String tag;


    /**
     * Creates a language-tagged literal.
     *
     * @param value the lexical form
     * @param tag the language tag
     */
    public LiteralNode(String value, String tag)
    {
        this.value = value;
        this.type = null;
        this.tag = tag;
    }


    /**
     * Creates a typed literal.
     *
     * @param value the lexical form
     * @param type the datatype IRI
     */
    public LiteralNode(String value, IriNode type)
    {
        this.value = value;
        this.type = type;
        this.tag = null;
    }


    /**
     * Lexical form with quotes removed and escapes resolved.
     *
     * @return lexical form with quotes removed and escapes resolved
     */
    public String getValue()
    {
        return value;
    }


    /**
     * Datatype IRI; null for language-tagged literals.
     *
     * @return datatype IRI; null for language-tagged literals
     */
    public IriNode getType()
    {
        return type;
    }


    /**
     * Language tag without the leading {@code @}; null for typed literals.
     *
     * @return language tag without the leading {@code @}; null for typed literals
     */
    public String getTag()
    {
        return tag;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(value, type, tag);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        LiteralNode other = (LiteralNode) object;

        if(!Objects.equals(value, other.value))
            return false;

        if(!Objects.equals(type, other.type))
            return false;

        if(!Objects.equals(tag, other.tag))
            return false;

        return true;
    }
}
