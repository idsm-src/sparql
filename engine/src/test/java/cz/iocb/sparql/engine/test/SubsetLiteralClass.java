package cz.iocb.sparql.engine.test;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.mapping.classes.SimpleLiteralClass;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class SubsetLiteralClass extends LiteralClass
{
    private final SimpleLiteralClass original;

    public SubsetLiteralClass(SimpleLiteralClass org)
    {
        super(org.getName() + "_sub", org.getSqlTypes(), org.getTypeIri());
        original = org;
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return original;
    }


    @Override
    public boolean canBeDerivatedFromGeneral()
    {
        return false;
    }

    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        return original.fromBoxedExpression(column, check);
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return original.toBoxedExpression(columns);
    }

    @Override
    public String toBoxedExpression(String code)
    {
        return original.toBoxedExpression(code);
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        return original.toUnboxedExpression(code, check);
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return original.toColumns(node);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return original.getResultResourceClasses();
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        return original.fromGeneralClass(columns);
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return original.toGeneralClass(columns, check);
    }


    @Override
    public List<Column> fromExpression(Column column)
    {
        return original.fromExpression(column);
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return original.toExpression(columns);
    }


    @Override
    public Column toExpression(Statement statement, Node node)
    {
        return original.toExpression(statement, node);
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return original.fromGeneralExpression(code);
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return original.toGeneralExpression(code);
    }
}
