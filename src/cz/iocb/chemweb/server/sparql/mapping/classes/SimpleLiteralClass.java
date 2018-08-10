package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class SimpleLiteralClass extends LiteralClass
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
    public String getSqlLiteralValue(Literal node, int i)
    {
        Object value = node.getValue();

        if(value instanceof String)
            return "'" + ((String) value).replace("'", "''") + "'";
        else
            return "'" + value.toString() + "'::" + sqlTypes.get(i);
    }
}
