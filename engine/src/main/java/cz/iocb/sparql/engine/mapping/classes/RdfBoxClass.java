package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.resultClasses;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.RdfTerm;



/**
 * The universal class: any term boxed in one {@code sparql.rdfbox} column. It is a superclass of every other class and
 * the fallback when the class of a value cannot be narrowed.
 */
public class RdfBoxClass extends PrimitiveResourceClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected RdfBoxClass()
    {
        super("rdfbox", List.of("sparql.rdfbox"), Set.of());
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return resultClasses;
    }


    @Override
    public boolean match(Statement statement, RdfTerm term)
    {
        return true;
    }


    @Override
    public List<Column> toColumns(Statement statement, RdfTerm term)
    {
        return resultClasses.stream().map(r -> (ResourceClass) r).filter(r -> r.match(statement, term))
                .map(r -> r.toGeneralClass(box, r.toColumns(statement, term), false)).findFirst().get();
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
