package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class CommonStrBlankNodeClass extends StrBlankNodeClass
{
    CommonStrBlankNodeClass()
    {
        super("str_blanknode", asList("varchar"));
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        throw new IllegalArgumentException();
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
            return asList(column);

        return asList(new ExpressionColumn("sparql.rdfbox_extract_str_blanknode" + "(" + column + ")"));
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        if(!rdfbox)
            return columns.get(0);

        return new ExpressionColumn("sparql.cast_as_rdfbox_from_str_blanknode(" + columns.get(0) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
    }
}
