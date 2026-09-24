package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Resource class of blank nodes with an integer value; their result class is {@link BuiltinClasses#intBlankNode}.
 */
public abstract class IntBlankNodeClass extends BlankNodeClass
{
    /**
     * Creates the class with its name, column types and superclasses.
     *
     * @param name the name
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected IntBlankNodeClass(String name, List<SqlType> sqlTypes, Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    /**
     * Constant columns representing the blank node.
     *
     * @param bnode the blank node
     * @return the constant columns
     */
    public abstract List<Column> toColumns(IntBlankNode bnode);


    /**
     * True if the blank node belongs to this class.
     *
     * @param statement database statement used for lookups in the database
     * @param term the RDF term
     * @return true if the blank node belongs to this class, false otherwise
     */
    public abstract boolean match(Statement statement, IntBlankNode term);


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(intBlankNode);
    }


    @Override
    public final boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case IntBlankNode bnode -> match(statement, bnode);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(BlankNode term)
    {
        if(term instanceof IntBlankNode bnode)
            return toColumns(bnode);
        else
            throw new IllegalArgumentException();
    }
}
