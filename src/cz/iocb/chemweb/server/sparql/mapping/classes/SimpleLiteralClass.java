package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class SimpleLiteralClass extends LiteralClass
{
    protected SimpleLiteralClass(ResultTag resultTag, String sqlType, IRI sparqlTypeIri)
    {
        super(resultTag.getTag(), asList(sqlType), asList(resultTag), sparqlTypeIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        Object value = ((Literal) node).getValue();

        if(value instanceof String)
            return asList(new ConstantColumn("'" + ((String) value).replace("'", "''") + "'::varchar"));
        else
            return asList(new ConstantColumn("'" + value + "'::" + sqlTypes.get(0)));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        return columns;
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return columns;
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        if(isBoxed == false)
            throw new IllegalArgumentException();

        return asList(new ExpressionColumn("sparql.rdfbox_extract_" + getName() + "(" + column + ")"));
    }


    @Override
    public Column toExpression(Node node)
    {
        Object value = ((Literal) node).getValue();

        if(value instanceof String)
            return new ConstantColumn("'" + ((String) value).replace("'", "''") + "'::varchar");
        else
            return new ConstantColumn("'" + value + "'::" + sqlTypes.get(0));
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        if(!rdfbox)
            return columns.get(0);

        return new ExpressionColumn("sparql.cast_as_rdfbox_from_" + name + "(" + columns.get(0) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
    }
}
