package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteralExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.LITERAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.TYPE;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class UnsupportedLiteralClass extends PatternLiteralBaseClass
{
    UnsupportedLiteralClass()
    {
        super("literal", Arrays.asList("varchar", "varchar"), Arrays.asList(LITERAL, TYPE), null);
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return getSqlColumn(variable, 0);
    }


    @Override
    public String getSqlPatternLiteralValue(Literal literal, int part)
    {
        if(part == 0)
            return "'" + literal.getStringValue().replaceAll("'", "''") + "'::varchar";
        else
            return "'" + literal.getTypeIri().getUri().toString() + "'::varchar";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return unsupportedLiteralExpr;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return "sparql.cast_as_rdfbox_from_typed_literal(" + variableAccessor.variableAccess(variable, this, 0) + ", "
                + variableAccessor.variableAccess(variable, this, 1) + ")";
    }
}
