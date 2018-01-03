package cz.iocb.chemweb.server.sparql.parser.model;

import java.net.URI;
import java.util.Optional;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.error.ErrorType;
import cz.iocb.chemweb.server.sparql.parser.error.UncheckedParseException;



/**
 * A shortened form of IRI, represented using a prefix (see {@link PrefixDefinition}) and a local name.
 *
 * <p>
 * In the final AST, PrefixedName can appear only as a {@link Define.DefineValue}. In other cases, it is expanded to the
 * full {@link IRI}.
 */
public class PrefixedName extends BaseElement implements Define.DefineValue
{
    private String prefix;
    private String localName;

    public PrefixedName(String prefix, String localName)
    {
        setPrefix(prefix);
        setLocalName(localName);
    }

    /**
     * Prefix without the trailing ':'.
     */
    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        if(prefix == null)
            throw new IllegalArgumentException();

        if(prefix.endsWith(":"))
            prefix = prefix.substring(0, prefix.length() - 1);

        this.prefix = prefix;
    }

    public String getLocalName()
    {
        return localName;
    }

    public void setLocalName(String localName)
    {
        if(localName == null)
            throw new IllegalArgumentException();

        this.localName = localName;
    }

    public URI getAbsoluteURI(Prologue prologue)
    {
        Optional<Prefix> prefix = prologue.getAllPrefixes().stream().filter(p -> p.getName().equals(this.prefix))
                .findFirst();

        if(!prefix.isPresent())
            throw new UncheckedParseException(ErrorType.unknownPrefix, this.getRange(), this.prefix);

        String prefixURI = prefix.get().getIri();

        try
        {
            return URI.create(prologue.getBase().getUri().resolve(prefixURI).toString() + localName);
        }
        catch(IllegalArgumentException e)
        {
            // bif: or sql: need a special treatment
            return URI.create(prefixURI + localName);
        }
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
