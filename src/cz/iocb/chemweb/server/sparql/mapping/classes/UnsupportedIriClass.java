package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class UnsupportedIriClass extends IriClass
{
    UnsupportedIriClass()
    {
        super("unsupported", List.of("varchar"), List.of(ResultTag.IRI));
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return List.of(new ConstantColumn((((IRI) node).getValue()), "varchar"));
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
        return List.of(column);
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return columns.get(0);
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        if(check)
            throw new IllegalArgumentException();
        else
            return List.of(new ExpressionColumn("sparql.rdfbox_get_iri" + "(" + column + ")"));
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.rdfbox_create_from_iri(" + columns.get(0) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        if(columns.get(0) instanceof ConstantColumn col)
            return col.getValue();

        return "";
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode || node instanceof IRI)
            return true;

        return false;
    }


    @Override
    public boolean canBeDerivatedFromGeneral()
    {
        return false;
    }
}
