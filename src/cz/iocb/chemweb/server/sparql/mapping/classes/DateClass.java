package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATE;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class DateClass extends LiteralClass
{
    DateClass()
    {
        super("date", Arrays.asList("date", "int4"), Arrays.asList(DATE), xsdDateIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return xsdDate;
    }


    @Override
    public String getLiteralPatternCode(Literal literal, int part)
    {
        if(part == 0)
            return "sparql.zoneddate_date('" + (String) literal.getValue() + "'::sparql.zoneddate)";
        else
            return "sparql.zoneddate_zone('" + (String) literal.getValue() + "'::sparql.zoneddate)";
    }


    @Override
    public String getGeneralisedPatternCode(String table, String var, int part, boolean check)
    {
        return (table != null ? table + "." : "") + getSqlColumn(var, part);
    }


    @Override
    public String getSpecialisedPatternCode(String table, String var, int part)
    {
        return (table != null ? table + "." : "") + getSqlColumn(var, part);
    }


    @Override
    public String getPatternCode(String column, int part, boolean isBoxed)
    {
        if(isBoxed)
            return "sparql.rdfbox_extract_date_" + (part == 0 ? "date" : "zone") + "(" + column + ")";
        else
            return "sparql.zoneddate_" + (part == 0 ? "date" : "zone") + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(Literal literal)
    {
        return "'" + (String) literal.getValue() + "'::sparql.zoneddate";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return (rdfbox ? "sparql.cast_as_rdfbox_from_date" : "sparql.zoneddate_create") + "("
                + variableAccessor.getSqlVariableAccess(variable, this, 0) + ", "
                + variableAccessor.getSqlVariableAccess(variable, this, 1) + ")";
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        // xsdDate is returned as zoneddate because there are many discrepancies in date interpretations
        return "sparql.zoneddate_create(" + getSqlColumn(variable, 0) + ", " + getSqlColumn(variable, 1) + ")";
    }
}
