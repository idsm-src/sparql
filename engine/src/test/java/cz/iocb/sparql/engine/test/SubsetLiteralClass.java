package cz.iocb.sparql.engine.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultResourceClass;
import cz.iocb.sparql.engine.mapping.classes.SimpleLiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class SubsetLiteralClass extends LiteralClass
{
    private final SimpleLiteralClass original;


    public SubsetLiteralClass(SimpleLiteralClass org)
    {
        Set<ResourceClass> superClasses = new HashSet<>();
        superClasses.addAll(org.getSuperClasses());
        superClasses.add(org);

        super(org.getName() + "_sub", org.getDatatype(), org.getSqlTypes(), superClasses);

        original = org;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return original.getResultResourceClasses();
    }


    @Override
    public PrimitiveResourceClass getEffectiveClass()
    {
        return original.getEffectiveClass();
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return original.toColumns(literal);
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        ResourceClass sourceClass = superClass.getEffectiveClass();

        if(sourceClass.equals(this))
            return columns;

        return original.toGeneralClass(superClass.getEffectiveClass(), columns, canBeNull);
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        ResourceClass sourceClass = superClass.getEffectiveClass();

        if(sourceClass.equals(this))
            return columns;

        return original.fromGeneralClass(superClass, columns);
    }
}
