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
    public List<Column> fromExpression(Column column)
    {
        return asList(column);
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return columns.get(0);
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        return asList(new ExpressionColumn("sparql.rdfbox_get_" + name + "(" + column + ")"));
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.rdfbox_create_from_" + name + "(" + columns.get(0) + ")");
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
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_" + name + "(" + code + ")";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        return "sparql.rdfbox_get_" + name + "(" + code + ")";
    }
}
