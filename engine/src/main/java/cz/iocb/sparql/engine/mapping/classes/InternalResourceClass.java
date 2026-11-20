package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Request;



/**
 * Class of engine-internal values that never become RDF terms (e.g. the join keys between the tables of a join
 * mapping). Every instance is a distinct class, disjoint with every other class but the box: like every class, it is a
 * subclass of the box, which stands for any class where a node says what it needs of a variable, so that a variable
 * needed in any class is needed with its internal values too. Its values are converted neither to the box nor from it,
 * though, which the class says before a conversion is tried (see {@link #canConvertTo} and
 * {@link #canCheckRepresentability}), and the conversions themselves refuse.
 */
public final class InternalResourceClass extends PrimitiveResourceClass
{
    /**
     * Counter making every instance name unique.
     */
    static private AtomicInteger counter = new AtomicInteger();


    /**
     * Creates a fresh class over the given SQL types, a subclass of the box.
     *
     * @param sqlTypes the SQL types
     */
    public InternalResourceClass(List<SqlType> sqlTypes)
    {
        super("internal-" + Integer.toHexString(counter.getAndIncrement()), sqlTypes, Set.of(box));
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        throw new IllegalArgumentException();
    }


    @Override
    public boolean match(Request request, RdfTerm term)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(Request request, RdfTerm term)
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
    public boolean canCheckRepresentability(ResourceClass superClass)
    {
        return superClass.equals(this);
    }


    @Override
    public boolean canConvertTo(ResourceClass targetClass)
    {
        return targetClass.equals(this);
    }


    @Override
    public boolean equals(Object object)
    {
        return object == this;
    }
}
