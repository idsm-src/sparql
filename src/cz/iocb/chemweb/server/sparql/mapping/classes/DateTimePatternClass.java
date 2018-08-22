package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.DATETIME;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class DateTimePatternClass extends PatternLiteralBaseClass implements DateTimeClass
{
    DateTimePatternClass()
    {
        super("datetime", Arrays.asList("timestamptz", "int4"), Arrays.asList(DATETIME), xsdDateTimeIri);
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        // xsdDateTime is returned as zoneddatetime because there are many discrepancies in timestamp interpretations
        return "sparql.zoneddatetime_create(" + getSqlColumn(variable, 0) + ", " + getSqlColumn(variable, 1) + ")";
    }


    @Override
    public String getSqlPatternLiteralValue(Literal literal, int part)
    {
        if(part == 0)
            return "sparql.zoneddatetime_datetime('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
        else
            return "sparql.zoneddatetime_zone('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return BuiltinClasses.xsdDateTimeExpr;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return (rdfbox ? "sparql.cast_as_rdfbox_from_datetime" : "sparql.zoneddatetime_create") + "("
                + variableAccessor.variableAccess(variable, this, 0) + ", "
                + variableAccessor.variableAccess(variable, this, 1) + ")";
    }
}
