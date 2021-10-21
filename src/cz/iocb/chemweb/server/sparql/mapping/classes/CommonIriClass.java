package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class CommonIriClass extends IriClass
{
    CommonIriClass()
    {
        super("iri", asList("varchar"), asList(ResultTag.IRI));
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return asList(new ConstantColumn("'" + (((IRI) node).getValue()).replaceAll("'", "''") + "'::varchar"));
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

        return asList(new ExpressionColumn("sparql.rdfbox_extract_iri" + "(" + column + ")"));
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        if(!rdfbox)
            return columns.get(0);

        return new ExpressionColumn("sparql.cast_as_rdfbox_from_iri(" + columns.get(0) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode || node instanceof IRI)
            return true;

        return false;
    }
}
