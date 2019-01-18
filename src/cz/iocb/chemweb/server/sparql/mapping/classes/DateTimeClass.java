package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.DATETIME;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class DateTimeClass extends LiteralClass
{
    DateTimeClass()
    {
        super("datetime", Arrays.asList("timestamptz", "int4"), Arrays.asList(DATETIME), xsdDateTimeIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return xsdDateTime;
    }


    @Override
    public String getLiteralPatternCode(Literal literal, int part)
    {
        if(part == 0)
            return "sparql.zoneddatetime_datetime('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
        else
            return "sparql.zoneddatetime_zone('" + literal.getStringValue().trim() + "'::sparql.zoneddatetime)";
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
            return "sparql.rdfbox_extract_datetime_" + (part == 0 ? "datetime" : "zone") + "(" + column + ")";
        else
            return "sparql.zoneddatetime_" + (part == 0 ? "datetime" : "zone") + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(Literal literal)
    {
        return "'" + literal.getStringValue().trim() + "'::sparql.zoneddatetime";
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        return (rdfbox ? "sparql.cast_as_rdfbox_from_datetime" : "sparql.zoneddatetime_create") + "("
                + variableAccessor.getSqlVariableAccess(variable, this, 0) + ", "
                + variableAccessor.getSqlVariableAccess(variable, this, 1) + ")";
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        // xsdDateTime is returned as zoneddatetime because there are many discrepancies in timestamp interpretations
        return "sparql.zoneddatetime_create(" + getSqlColumn(variable, 0) + ", " + getSqlColumn(variable, 1) + ")";
    }
}
