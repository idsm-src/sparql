package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.RdfTerm;



/**
 * The universal class: any term boxed in one {@code sparql.rdfbox} column. It is a superclass of every other class and
 * the fallback when the class of a value cannot be narrowed. It is also its own result class: a boxed value is
 * delivered to the result as the text form of the box and decoded by
 * {@link cz.iocb.sparql.engine.request.RdfBoxParser}.
 */
public final class RdfBoxClass extends PrimitiveResourceClass implements ResultResourceClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected RdfBoxClass()
    {
        super("rdfbox", List.of(RDFBOX), Set.of());
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public boolean match(Statement statement, RdfTerm term)
    {
        return true;
    }


    /**
     * Not supported: a constant is boxed from its most specific class, which only the request knows
     * ({@code Request.getResourceClass}), by converting its columns to the box with {@link #toGeneralClass}.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<Column> toColumns(Statement statement, RdfTerm term)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        if(superClass.getEffectiveClass().equals(getEffectiveClass()))
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
}
