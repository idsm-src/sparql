package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class InternalResourceClass extends ResourceClass
{
    public InternalResourceClass(int size)
    {
        super("internal", new ArrayList<String>(Collections.nCopies(size, "any")),
                new ArrayList<ResultTag>(Collections.nCopies(size, ResultTag.NULL)));
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> fromExpression(Column column, boolean isBoxed, boolean check)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Column toExpression(Node node)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public Column toExpression(List<Column> columns, boolean rdfbox)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean match(Node node)
    {
        throw new UnsupportedOperationException();
    }
}
