package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LITERAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.TYPE;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class UnsupportedLiteralClass extends LiteralClass
{
    UnsupportedLiteralClass()
    {
        super("literal", Arrays.asList("varchar", "varchar"), Arrays.asList(LITERAL, TYPE), null);
    }


    @Override
    public String getLiteralPatternCode(Literal literal, int part)
    {
        if(part == 0)
            return "'" + literal.getStringValue().replaceAll("'", "''") + "'::varchar";
        else
            return "'" + literal.getTypeIri().getUri().toString() + "'::varchar";
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_typed_literal_" + (part == 0 ? "literal" : "type") + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(Literal literal)
    {
        String value = literal.getStringValue().replaceAll("'", "''");

        return "sparql.cast_as_rdfbox_from_typed_literal('" + value + "', '" + literal.getTypeIri().getUri().toString()
                + "')";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return "sparql.cast_as_rdfbox_from_typed_literal(" + variableAccessor.getSqlVariableAccess(variable, this, 0)
                + ", " + variableAccessor.getSqlVariableAccess(variable, this, 1) + ")";
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }
}
