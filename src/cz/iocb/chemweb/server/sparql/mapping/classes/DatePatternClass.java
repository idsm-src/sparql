package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class DatePatternClass extends PatternLiteralBaseClass implements DateClass
{
    DatePatternClass()
    {
        super("date", Arrays.asList("date", "int4"), Arrays.asList(DATE), xsdDateIri);
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        // xsdDate is returned as zoneddate because there are many discrepancies in date interpretations
        return "sparql.zoneddate_create(" + getSqlColumn(variable, 0) + ", " + getSqlColumn(variable, 1) + ")";
    }


    @Override
    public String getSqlPatternLiteralValue(Literal literal, int part)
    {
        if(part == 0)
            return "sparql.zoneddate_date('" + (String) literal.getValue() + "'::sparql.zoneddate)";
        else
            return "sparql.zoneddate_zone('" + (String) literal.getValue() + "'::sparql.zoneddate)";
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return BuiltinClasses.xsdDateExpr;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return (rdfbox ? "sparql.cast_as_rdfbox_from_date" : "sparql.zoneddate_create") + "("
                + variableAccessor.variableAccess(variable, this, 0) + ", "
                + variableAccessor.variableAccess(variable, this, 1) + ")";
    }
}
