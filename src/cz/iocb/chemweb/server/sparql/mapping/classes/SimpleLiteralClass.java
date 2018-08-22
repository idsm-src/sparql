package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternLiteralBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SimpleLiteralClass extends PatternLiteralBaseClass implements ExpressionLiteralClass
{
    protected SimpleLiteralClass(ResultTag resultTag, String sqlType, IRI sparqlTypeIri)
    {
        super(resultTag.getTag(), Arrays.asList(sqlType), Arrays.asList(resultTag), sparqlTypeIri);
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }


    @Override
    public String getSqlPatternLiteralValue(Literal node, int i)
    {
        Object value = node.getValue();

        if(value instanceof String)
            return "'" + ((String) value).replace("'", "''") + "'::varchar";
        else
            return "'" + value.toString() + "'::" + sqlTypes.get(i);
    }


    @Override
    public ExpressionResourceClass getExpressionResourceClass()
    {
        return this;
    }


    @Override
    public String getSqlExpressionValue(String variable, VariableAccessor variableAccessor, boolean rdfbox)
    {
        if(!rdfbox)
            return variableAccessor.variableAccess(variable, this, 0);

        return "sparql.cast_as_rdfbox_from_" + name + "(" + variableAccessor.variableAccess(variable, this, 0) + ")";
    }


    @Override
    public String getSqlLiteralValue(Literal literal)
    {
        return getSqlValue(literal, 0);
    }


    @Override
    public PatternResourceClass getPatternResourceClass()
    {
        return this;
    }


    @Override
    public String getSqlPatternValue(String column, int part, boolean isBoxed)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return "sparql.rdfbox_extract_" + getName() + "(" + column + ")";
    }
}
