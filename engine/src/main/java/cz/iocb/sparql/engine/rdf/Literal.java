package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



/**
 * Literal term; the lexical value is kept here, the datatype is given by the subclass.
 */
public abstract class Literal extends RdfTerm
{
    /**
     * Lexical form.
     */
    protected final String value;


    /**
     * Creates the literal with the lexical form.
     *
     * @param value the lexical form
     */
    public Literal(String value)
    {
        this.value = value;
    }


    /**
     * Datatype IRI of the literal.
     *
     * @return datatype IRI of the literal
     */
    public abstract Iri getType();


    /**
     * Lexical form.
     *
     * @return lexical form
     */
    public String getValue()
    {
        return value;
    }


    /**
     * Single-quoted SPARQL-like rendering with quotes, backslashes and line breaks escaped.
     */
    @Override
    public String toString()
    {
        return "'" + value.replaceAll("(['\\\\])", "\\\\$1").replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r") + "'";
    }


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Literal literal = (Literal) object;

        if(!Objects.equals(value, literal.value))
            return false;

        return true;
    }
}
