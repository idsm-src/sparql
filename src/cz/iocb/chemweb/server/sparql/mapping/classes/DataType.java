package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import java.util.function.Function;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



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
}
