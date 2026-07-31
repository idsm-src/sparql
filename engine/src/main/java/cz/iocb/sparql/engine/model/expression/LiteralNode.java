package cz.iocb.sparql.engine.model.expression;

import java.util.Objects;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents a literal value ({@link #getStringValue}) that has a type ({@link #getTypeIri}) and can have a language
 * tag ({@link #getLanguageTag}). This includes the shorthand forms for numeric and boolean literals.
 *
 * <p>
 * For supported literal types, a converted value ({@link #getValue}), along with its Java type ({@link #getJavaClass})
 * is also provided.
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
    private final String value;
    private final IriNode type;
    private final String tag;


    public LiteralNode(String value, String tag)
    {
        this.value = value;
        this.type = null;
        this.tag = tag;
    }


    public LiteralNode(String value, IriNode type)
    {
        this.value = value;
        this.type = type;
        this.tag = null;
    }


    public String getValue()
    {
        return value;
    }


    public IriNode getType()
    {
        return type;
    }


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
