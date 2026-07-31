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
    private String name;
    private IriNode iri;


    public PrefixDefinition(String name, String iri)
    {
        this(name, new IriNode(iri));
    }


    public PrefixDefinition(String name, IriNode iri)
    {
        setName(name);
        setIri(iri);
    }


    public String getName()
    {
        return name;
    }


    public void setName(String name)
    {
        if(name == null)
            throw new IllegalArgumentException();

        if(name.endsWith(":"))
            name = name.substring(0, name.length() - 1);

        this.name = name;
    }


    public IriNode getIri()
    {
        return iri;
    }


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
