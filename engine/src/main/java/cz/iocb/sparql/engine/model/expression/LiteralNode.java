package cz.iocb.sparql.engine.model.expression;

import java.util.Objects;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents a literal: a lexical value ({@link #getValue}) with either a datatype IRI ({@link #getType}) or a language
 * tag ({@link #getTag}), the latter optionally with a base direction ({@link #getDirection}). Numeric and boolean
 * shorthand forms get the corresponding xsd datatype.
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar:
 * <ul>
 * <li>[149] RDFLiteral
 * <li>[150] NumericLiteral
 * <li>[154] BooleanLiteral
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
     * Base direction of a language-tagged literal, null when not specified.
     */
    private final String direction;


    /**
     * Creates a language-tagged literal.
     *
     * @param value the lexical form
     * @param tag the language tag
     */
    public LiteralNode(String value, String tag)
    {
        this(value, tag, null);
    }


    /**
     * Creates a language-tagged literal with a base direction.
     *
     * @param value the lexical form
     * @param tag the language tag
     * @param direction the base direction ({@code ltr} or {@code rtl}), or null
     */
    public LiteralNode(String value, String tag, String direction)
    {
        this.value = value;
        this.type = null;
        this.tag = tag;
        this.direction = direction;
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
        this.direction = null;
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


    /**
     * Base direction ({@code ltr} or {@code rtl}) of a language-tagged literal, without the leading {@code --}; null
     * when not specified or for typed literals.
     *
     * @return base direction ({@code ltr} or {@code rtl}) of a language-tagged literal, without the leading {@code --};
     *         null when not specified or for typed literals
     */
    public String getDirection()
    {
        return direction;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(value, type, tag, direction);
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

        if(!Objects.equals(direction, other.direction))
            return false;

        return true;
    }
}
