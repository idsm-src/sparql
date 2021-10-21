package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.LITERAL;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.TYPE;
import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class UnsupportedLiteralClass extends LiteralClass
{
    UnsupportedLiteralClass()
    {
        super("literal", asList("varchar", "varchar"), asList(LITERAL, TYPE), null);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ConstantColumn("'" + ((Literal) node).getStringValue().replaceAll("'", "''") + "'::varchar"));
        result.add(new ConstantColumn("'" + ((Literal) node).getTypeIri().getValue() + "'::varchar"));

        return result;
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

        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.rdfbox_extract_typed_literal_literal(" + column + ")"));
        result.add(new ExpressionColumn("sparql.rdfbox_extract_typed_literal_type(" + column + ")"));

        return result;
    }


    @Override
    public Column toExpression(Node node)
    {
        String value = ((Literal) node).getStringValue().replaceAll("'", "''");

        return new ExpressionColumn("sparql.cast_as_rdfbox_from_typed_literal('" + value + "', '"
                + ((Literal) node).getTypeIri().getValue() + "')");
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        return new ExpressionColumn(
                "sparql.cast_as_rdfbox_from_typed_literal(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
    }
}
