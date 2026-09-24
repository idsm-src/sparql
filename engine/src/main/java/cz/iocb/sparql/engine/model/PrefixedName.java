package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * A shortened form of IRI, represented using a prefix (see {@link PrefixDefinition}) and a local name.
 *
 * <p>
 * In the final AST, it is expanded to the full {@link IriNode}.
 */
public class PrefixedName extends BaseElement
{
    /**
     * Prefix without the trailing colon.
     */
    private final String prefix;

    /**
     * Local part after the colon.
     */
    private final String localName;


    /**
     * Creates the name from its prefix and local part.
     *
     * @param prefix the prefix
     * @param localName the local part
     */
    public PrefixedName(String prefix, String localName)
    {
        this.prefix = prefix;
        this.localName = localName;
    }


    /**
     * Prefix without the trailing ':'.
     *
     * @return prefix without the trailing ':'
     */
    public String getPrefix()
    {
        return prefix;
    }


    /**
     * Local part after the colon.
     *
     * @return local part after the colon
     */
    public String getLocalName()
    {
        return localName;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
