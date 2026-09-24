package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.RdfTerm;



/**
 * Class of engine-internal values that never become RDF terms (e.g. the join keys between the tables of a join
 * mapping). Every instance is a distinct class disjoint with everything else.
 */
public final class InternalResourceClass extends PrimitiveResourceClass
{
    /**
     * Counter making every instance name unique.
     */
    static private AtomicInteger counter = new AtomicInteger();


    /**
     * Creates a fresh class over the given SQL types.
     *
     * @param sqlTypes the SQL types
     */
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
    public boolean match(Statement statement, RdfTerm term)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(Statement statement, RdfTerm term)
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
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns, boolean checkOptional)
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
