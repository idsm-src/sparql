package cz.iocb.sparql.engine.mapping.classes;

import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Resource class of literals of one datatype (null datatype for the classes of arbitrary literals).
 */
public sealed abstract class LiteralClass extends PrimitiveResourceClass permits CanonicalLiteralClass, BaseLiteralClass
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
        this(name, datatype, sqlTypes, superClasses, null);
    }


    /**
     * Creates the class with its name, datatype, column types, superclasses and the class that effectively represents
     * it.
     *
     * @param name the name
     * @param datatype the datatype
     * @param sqlTypes the SQL types
     * @param superClasses the superclasses
     * @param effectiveClass primitive class whose columns store the values, or null for the class itself
     */
    protected LiteralClass(String name, Datatype datatype, List<SqlType> sqlTypes,
            Set<PrimitiveResourceClass> superClasses, PrimitiveResourceClass effectiveClass)
    {
        super(name, sqlTypes, superClasses, effectiveClass);
        this.datatype = datatype;
    }


    /**
     * True if the literal has the datatype of this class and is representable in it.
     *
     * @param request the current request
     * @param literal the literal
     * @return true if the literal has the datatype of this class and is representable in it, false otherwise
     */
    public abstract boolean match(Request request, Literal literal);


    /**
     * Constant columns representing the literal.
     *
     * @param request the current request
     * @param literal the literal
     * @return the constant columns
     */
    public abstract List<Column> toColumns(Request request, Literal literal);


    @Override
    public final boolean match(Request request, RdfTerm term)
    {
        return switch(term)
        {
            case Variable _ -> true;
            case Literal literal -> match(request, literal);
            default -> false;
        };
    }


    @Override
    public final List<Column> toColumns(Request request, RdfTerm term)
    {
        if(term instanceof Literal literal)
            return toColumns(request, literal);
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
