package cz.iocb.sparql.engine.rdf;



public final class Iri extends RdfTerm
{
    private final String value;


    public Iri(String value)
    {
        this.value = value;
    }


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
