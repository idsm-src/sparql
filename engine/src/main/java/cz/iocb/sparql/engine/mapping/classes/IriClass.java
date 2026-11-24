package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Resource class of IRIs.
 */
public sealed abstract class IriClass extends PrimitiveResourceClass
        permits IriScalarClass, UnsupportedIriClass, UserIriClass
{
    /**
     * Table holding the order column (see {@link IriClass#toOrderColumns}) of the IRIs of a class by the values of the
     * class, like a mapping table pairing the stored values with the ids of the IRIs; an IRI is the prefix followed by
     * its order column.
     *
     * @param table the table
     * @param values the columns of the table holding the values of the class
     * @param order the order column of the IRI a row holds, written over the columns of the table
     * @param prefix the text before the order column in the IRIs
     */
    public record OrderTable(DatabaseTable table, List<Column> values, Column order, String prefix)
    {
    }


    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected IriClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    /**
     * True if the IRI belongs to this class; may consult the database.
     *
     * @param request the current request
     * @param iri the IRI
     * @return true if the IRI belongs to this class, false otherwise
     */
    public abstract boolean match(Request request, Iri iri);


    /**
     * Constant columns representing the IRI; may consult the database.
     *
     * @param request the current request
     * @param iri the IRI
     * @return the constant columns
     */
    public abstract List<Column> toColumns(Request request, Iri iri);


    /**
     * Prefix common to all IRIs the given columns can represent (the whole IRI for a constant); an empty string when
     * nothing is known.
     *
     * @param columns the columns
     * @return prefix common to all IRIs the given columns can represent (the whole IRI for a constant); an empty string
     *         when nothing is known
     */
    public abstract String getPrefix(List<Column> columns);


    @Override
    public final ResourceClass getResultResourceClass()
    {
        return iri;
    }


    @Override
    public final boolean match(Request request, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case Iri iri -> match(request, iri);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Request request, RdfTerm term)
    {
        if(term instanceof Iri iri)
            return toColumns(request, iri);
        else
            throw new IllegalArgumentException();
    }


    /**
     * Columns whose SQL ordering agrees with the code-point ordering of the full IRIs, used in ORDER BY instead of
     * rebuilding the IRIs, and in MIN and MAX; a class of a single column has a single order column.
     *
     * @param columns the columns
     * @return columns whose SQL ordering agrees with the code-point ordering of the full IRIs, used in ORDER BY instead
     *         of rebuilding the IRIs, and in MIN and MAX
     */
    public List<Column> toOrderColumns(List<Column> columns)
    {
        return toGeneralClass(iri, columns, true);
    }


    /**
     * Columns of this class holding the IRI whose order columns (see {@link #toOrderColumns}) are given, so that MIN
     * and MAX, which compare the order columns of the IRIs, get the IRI of the least or the greatest of them back.
     *
     * @param columns the order columns
     * @return columns of this class holding the IRI whose order columns are given
     */
    public List<Column> fromOrderColumns(List<Column> columns)
    {
        return fromGeneralClass(iri, columns, true);
    }


    /**
     * The table holding the order column of the IRIs of this class by the values of the class (see
     * {@link #toOrderColumns}), from which a join reads it along with the values, or null when the order columns are
     * computed from the values themselves.
     *
     * @return the table holding the order column of the IRIs of this class by the values of the class, or null
     */
    public OrderTable getOrderTable()
    {
        return null;
    }
}
