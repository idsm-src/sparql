package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Resource class of IRIs.
 */
public abstract class IriClass extends PrimitiveResourceClass
{
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
     * @param statement database statement used for lookups in the database
     * @param iri the IRI
     * @return true if the IRI belongs to this class, false otherwise
     */
    public abstract boolean match(Statement statement, Iri iri);


    /**
     * Constant columns representing the IRI; may consult the database.
     *
     * @param statement database statement used for lookups in the database
     * @param iri the IRI
     * @return the constant columns
     */
    public abstract List<Column> toColumns(Statement statement, Iri iri);


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
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(iri);
    }


    @Override
    public final boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case Iri iri -> match(statement, iri);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Statement statement, RdfTerm term)
    {
        if(term instanceof Iri iri)
            return toColumns(statement, iri);
        else
            throw new IllegalArgumentException();
    }


    /**
     * Columns whose SQL ordering agrees with the code-point ordering of the full IRIs, used in ORDER BY instead of
     * rebuilding the IRIs.
     *
     * @param columns the columns
     * @return columns whose SQL ordering agrees with the code-point ordering of the full IRIs, used in ORDER BY instead
     *         of rebuilding the IRIs
     */
    public List<Column> toOrderColumns(List<Column> columns)
    {
        return toGeneralClass(iri, columns, true);
    }
}
