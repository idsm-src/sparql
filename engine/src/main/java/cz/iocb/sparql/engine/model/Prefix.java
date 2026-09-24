package cz.iocb.sparql.engine.model;



/**
 * Plain pair of a prefix name (without the trailing colon) and the IRI it stands for.
 */
public class Prefix
{
    /**
     * Prefix name without the trailing colon.
     */
    private String name;

    /**
     * IRI the prefix stands for.
     */
    private String iri;


    /**
     * Creates the pair.
     *
     * @param name the name
     * @param iri the IRI
     */
    public Prefix(String name, String iri)
    {
        this.name = name;
        this.iri = iri;
    }


    /**
     * Prefix name without the trailing colon.
     *
     * @return prefix name without the trailing colon
     */
    public String getName()
    {
        return name;
    }


    /**
     * Sets the prefix name, dropping a trailing colon.
     *
     * @param name the name
     */
    public void setName(String name)
    {
        if(name == null)
            throw new IllegalArgumentException();

        if(name.endsWith(":"))
            name = name.substring(0, name.length() - 1);

        this.name = name;
    }


    /**
     * IRI the prefix stands for.
     *
     * @return IRI the prefix stands for
     */
    public String getIri()
    {
        return iri;
    }


    /**
     * Sets the IRI the prefix stands for (required).
     *
     * @param iri the IRI
     */
    public void setIri(String iri)
    {
        if(iri == null)
            throw new IllegalArgumentException();

        this.iri = iri;
    }
}
