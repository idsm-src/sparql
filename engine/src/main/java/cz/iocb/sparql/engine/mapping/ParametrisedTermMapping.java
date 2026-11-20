package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Mapping of a position to table columns of an arbitrary resource class, meant for the classes without a mapping of
 * their own: the box ({@code sparql.rdfbox} columns holding any term), a class derived from the box that keeps some
 * kinds of terms only in such a column (see {@link cz.iocb.sparql.engine.mapping.classes.DerivedClass}; columns mapped
 * by disjoint classes are known to hold different terms) and the triple term classes (the columns of the subject,
 * predicate and object of the stored triple terms).
 */
public class ParametrisedTermMapping extends ParametrisedMapping
{
    /**
     * Creates the mapping.
     *
     * @param resourceClass the resource class
     * @param columns the columns
     */
    public ParametrisedTermMapping(ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass, columns);
    }


    @Override
    public TermMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedTermMapping(resourceClass, remapColumns(columnMap));
    }
}
