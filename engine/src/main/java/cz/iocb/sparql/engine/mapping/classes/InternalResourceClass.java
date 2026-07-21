package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public final class InternalResourceClass extends PrimitiveResourceClass
{
    static private AtomicInteger counter = new AtomicInteger();


    public InternalResourceClass(List<String> sqlTypes)
    {
        super("internal-" + Integer.toHexString(counter.getAndIncrement()), sqlTypes, Set.of());
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        throw new IllegalArgumentException();
    }


    @Override
    public boolean match(Statement statement, Node node)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(Statement statement, Node node)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        throw new IllegalArgumentException();
    }


    @Override
    public boolean equals(Object object)
    {
        return object == this;
    }
}
