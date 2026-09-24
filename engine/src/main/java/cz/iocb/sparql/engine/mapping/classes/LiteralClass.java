package cz.iocb.sparql.engine.mapping.classes;

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Resource class of literals of one datatype (null datatype for the classes of arbitrary literals).
 */
public abstract class LiteralClass extends PrimitiveResourceClass
{
    /**
     * Datatype of the literals, or null for classes spanning datatypes.
     */
    final protected Datatype datatype;


    /**
     * Creates the class with its name, datatype, column types and superclasses.
     *
     * @param name the name
     * @param datatype the datatype
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     */
    protected LiteralClass(String name, Datatype datatype, List<SqlType> sqlTypes,
            Set<PrimitiveResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
        this.datatype = datatype;
    }


    /**
     * True if the literal has the datatype of this class and is representable in it.
     *
     * @param statement database statement used for lookups in the database
     * @param literal the literal
     * @return true if the literal has the datatype of this class and is representable in it, false otherwise
     */
    public abstract boolean match(Statement statement, Literal literal);


    /**
     * Constant columns representing the literal.
     *
     * @param literal the literal
     * @return the constant columns
     */
    public abstract List<Column> toColumns(Literal literal);


    @Override
    public final boolean match(Statement statement, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case Literal literal -> match(statement, literal);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Statement statement, RdfTerm term)
    {
        if(term instanceof Literal literal)
            return toColumns(literal);
        else
            throw new IllegalArgumentException();
    }


    /**
     * Datatype of the literals, or null for classes spanning datatypes.
     *
     * @return datatype of the literals, or null for classes spanning datatypes
     */
    public final Datatype getDatatype()
    {
        return datatype;
    }


    /**
     * Datatype IRI of the class, or null when it covers several datatypes.
     *
     * @return datatype IRI of the class, or null when it covers several datatypes
     */
    public final Iri getTypeIri()
    {
        return datatype != null ? datatype.getTypeIri() : null;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        return true;
    }
}
