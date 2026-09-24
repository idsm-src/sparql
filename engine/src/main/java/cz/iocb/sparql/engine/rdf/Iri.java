package cz.iocb.sparql.engine.rdf;



/**
 * An IRI term.
 */
public final class Iri extends RdfTerm
{
    /**
     * Full IRI text.
     */
    private final String value;


    /**
     * Creates the IRI.
     *
     * @param value the IRI text
     */
    public Iri(String value)
    {
        this.value = value;
    }


    /**
     * Full IRI text.
     *
     * @return full IRI text
     */
    public String getValue()
    {
        return value;
    }


    @Override
    public String toString()
    {
        return '<' + value + '>';
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

        Iri iri = (Iri) object;

        return value.equals(iri.value);
    }
}
