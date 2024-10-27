package cz.iocb.sparql.engine.parser.model.expression;


import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.rdfLangStringType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdStringType;
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.parser.BaseComplexNode;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.Node;



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
public class Literal extends BaseComplexNode implements Expression, Node
{
    private final String stringValue;
    private final Object value;
    private final String languageTag;
    private final IRI type;
    private final DataType datatype;
    private final boolean isSimple;


    public Literal(String value)
    {
        this.stringValue = value;
        this.value = xsdStringType.parse(value);
        this.languageTag = null;
        this.type = xsdStringType.getTypeIri();
        this.datatype = xsdStringType;
        this.isSimple = true;
    }


    public Literal(String value, String languageTag)
    {
        this.stringValue = value;
        this.value = rdfLangStringType.parse(value);
        this.languageTag = languageTag.toLowerCase();
        this.type = rdfLangStringType.getTypeIri();
        this.datatype = rdfLangStringType;
        this.isSimple = false;
    }


    public Literal(String value, DataType datatype, IRI typeIri)
    {
        this.stringValue = value;
        this.languageTag = null;
        this.type = typeIri;
        this.datatype = datatype;
        this.isSimple = false;

        this.value = datatype == null ? null : datatype.parse(value);
    }


    public Literal(String value, DataType datatype)
    {
        this.stringValue = value;
        this.languageTag = null;
        this.type = datatype.getTypeIri();
        this.datatype = datatype;
        this.isSimple = false;

        this.value = datatype.parse(value);
    }


    public String getStringValue()
    {
        return stringValue;
    }


    public Object getValue()
    {
        return value;
    }


    public String getLanguageTag()
    {
        return languageTag;
    }


    public IRI getTypeIri()
    {
        return type;
    }


    public DataType getDataType()
    {
        return datatype;
    }


    public boolean isTypeSupported()
    {
        return datatype != null;
    }


    public boolean isSimple()
    {
        return isSimple;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    @Override
    public int hashCode()
    {
        return value != null ? value.hashCode() : stringValue.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Literal literal = (Literal) object;

        if(!type.equals(literal.type))
            return false;

        if(value != null && !value.equals(literal.value) || value == null && literal.value != null)
            return false;

        if(value == null && !stringValue.equals(literal.stringValue))
            return false;

        if(languageTag != null && !languageTag.equals(literal.languageTag)
                || languageTag == null && literal.languageTag != null)
            return false;

        return true;
    }
}
