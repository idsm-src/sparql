package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * The universal class: any term boxed in one {@code sparql.rdfbox} column. It is a superclass of every other class and
 * the fallback when the class of a value cannot be narrowed. It is also its own result class: a boxed value is
 * delivered to the result as the text form of the box and decoded by
 * {@link cz.iocb.sparql.engine.request.RdfBoxParser}.
 */
public final class RdfBoxClass extends PrimitiveResourceClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected RdfBoxClass()
    {
        super("rdfbox", List.of(RDFBOX), Set.of());
    }


    @Override
    public ResourceClass getResultResourceClass()
    {
        return this;
    }


    @Override
    public boolean match(Request request, RdfTerm term)
    {
        return true;
    }


    /**
     * Boxes the constant from its most specific class, which the request determines from its configuration
     * ({@link Request#getResourceClass}), by converting the columns of that class to the box.
     *
     * @throws IllegalArgumentException if the term is a variable
     */
    @Override
    public List<Column> toColumns(Request request, RdfTerm term)
    {
        if(term instanceof Variable)
            throw new IllegalArgumentException();

        ResourceClass resClass = request.getResourceClass(term);

        return resClass.toGeneralClass(this, request.getColumns(resClass, term), false);
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
