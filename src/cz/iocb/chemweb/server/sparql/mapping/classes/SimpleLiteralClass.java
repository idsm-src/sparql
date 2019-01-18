package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SimpleLiteralClass extends LiteralClass
{
    protected SimpleLiteralClass(ResultTag resultTag, String sqlType, IRI sparqlTypeIri)
    {
        super(resultTag.getTag(), Arrays.asList(sqlType), Arrays.asList(resultTag), sparqlTypeIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public String getLiteralPatternCode(Literal literal, int part)
    {
        Object value = literal.getValue();

        if(value instanceof String)
            return "'" + ((String) value).replace("'", "''") + "'::varchar";
        else
            return "'" + value.toString() + "'::" + sqlTypes.get(part);
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
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_" + getName() + "(" + column + ")";
    }


    @Override
    public String getExpressionCode(Literal literal)
    {
        return getPatternCode(literal, 0);
    }


    @Override
    public String getExpressionCode(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        if(!rdfbox)
            return variableAccessor.getSqlVariableAccess(variable, this, 0);

        return "sparql.cast_as_rdfbox_from_" + name + "(" + variableAccessor.getSqlVariableAccess(variable, this, 0)
                + ")";
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }
}
