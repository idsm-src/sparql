package cz.iocb.sparql.engine.test;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.SimpleLiteralClass;



public class SubsetLiteralClass extends SimpleLiteralClass
{
    private final SimpleLiteralClass original;

    public SubsetLiteralClass(SimpleLiteralClass org)
    {
        super(org.getName() + "_sub", org.getResultTags().get(0), org.getSqlTypes().get(0), org.getTypeIri());
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
}
