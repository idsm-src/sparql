package cz.iocb.chemweb.server.sparql.mapping.classes;

import static java.util.Arrays.asList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class SimpleUserIriClass extends UserIriClass
{
    public SimpleUserIriClass(String name, String sqlType)
    {
        super(name, asList(sqlType), asList(ResultTag.IRI));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        return asList(generateInverseFunction(columns.get(0), true));
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return asList(generateFunction(columns.get(0)));
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        Column parameter = isBoxed ? new ExpressionColumn("sparql.rdfbox_extract_iri(" + column + ")") : column;
        return asList(generateInverseFunction(parameter, check));
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        StringBuilder builder = new StringBuilder();

        if(rdfbox)
            builder.append("sparql.cast_as_rdfbox_from_iri(");

        builder.append(generateFunction(columns.get(0)));

        if(rdfbox)
            builder.append(")");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return asList(generateFunction(columns.get(0)));
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof IRI))
            return false;

        return match((IRI) node);
    }


    protected abstract Column generateFunction(Column parameter);


    protected abstract Column generateInverseFunction(Column parameter, boolean check);
}
