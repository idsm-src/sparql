package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.ExpressionResourceBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class UnsupportedLiteralExpressionClass extends ExpressionResourceBaseClass
{
    protected UnsupportedLiteralExpressionClass()
    {
        super("literal");
    }


    @Override
    public String getSqlLiteralValue(Literal literal)
    {
        String value = literal.getStringValue().replaceAll("'", "''");

        return "sparql.cast_as_rdfbox_from_typed_literal('" + value + "', '" + literal.getTypeIri().getUri().toString()
                + "')";
    }


    @Override
    public PatternResourceClass getPatternResourceClass()
    {
        return unsupportedLiteral;
    }


    @Override
    public String getSqlPatternValue(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_typed_literal_" + (part == 0 ? "literal" : "type") + "(" + column + ")";
    }


    @Override
    public IRI getTypeIri()
    {
        return null;
    }
}
