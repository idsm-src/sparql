package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Mapping between a short name and an IRI prefix.
 *
 * <p>
 * Used when resolving {@link PrefixedName}s.
 */
public class PrefixDefinition extends BaseElement
{
    /**
     * Prefix name without the trailing colon.
     */
    private String name;

    /**
     * IRI the prefix stands for.
     */
    private IriNode iri;


    /**
     * Creates the definition from the prefix name and IRI text.
     *
     * @param name the name
     * @param iri the IRI
     */
    public PrefixDefinition(String name, String iri)
    {
        this(name, new IriNode(iri));
    }


    /**
     * Creates the definition from the prefix name and IRI node.
     *
     * @param name the name
     * @param iri the IRI node
     */
    public PrefixDefinition(String name, IriNode iri)
    {
        setName(name);
        setIri(iri);
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
    public IriNode getIri()
    {
        return iri;
    }


    /**
     * Sets the IRI the prefix stands for (required).
     *
     * @param iri the IRI node
     */
    public void setIri(IriNode iri)
    {
        if(iri == null)
            throw new IllegalArgumentException();

        this.iri = iri;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
