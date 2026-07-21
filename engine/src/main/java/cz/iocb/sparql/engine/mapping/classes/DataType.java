package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import java.util.Objects;
import java.util.function.Function;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public class DataType
{
    protected final LiteralClass literalClass;
    protected final Function<String, ?> parser;


    public DataType(LiteralClass literalClass, Function<String, ?> parser)
    {
        this.literalClass = literalClass;
        this.parser = parser;
    }


    public IRI getTypeIri()
    {
        return literalClass.getTypeIri();
    }


    public LiteralClass getGeneralLiteralClass()
    {
        return literalClass;
    }


    public LiteralClass getResourceClass(Literal literal)
    {
        assert literalClass.getTypeIri().equals(literal.getTypeIri());

        if(literal.getValue() == null)
            return unsupportedLiteral;

        return literalClass;
    }


    public Object parse(String literalValue)
    {
        return parser.apply(literalValue);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(literalClass, parser);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        DataType other = (DataType) object;

        return Objects.equals(literalClass, other.literalClass) && Objects.equals(parser, other.parser);
    }
}
