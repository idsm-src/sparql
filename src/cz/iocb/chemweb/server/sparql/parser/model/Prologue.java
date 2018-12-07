package cz.iocb.chemweb.server.sparql.parser.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.visitor.QueryVisitor;



/**
 * The initial part of a query, containing declarations.
 *
 * <p>
 * For example, for a {@link SelectQuery}, the part before SELECT.
 */
public class Prologue extends BaseElement
{
    private IRI base;
    private final List<PrefixDefinition> prefixDefinitions;
    private final LinkedHashMap<String, Prefix> userPrefixes;
    private final LinkedHashMap<String, Prefix> allPrefixes;


    public Prologue(List<Prefix> predefinedPrefixes)
    {
        setBase(new IRI(""));

        prefixDefinitions = new ArrayList<>();
        userPrefixes = new LinkedHashMap<>();
        allPrefixes = new LinkedHashMap<>();

        for(Prefix prefix : predefinedPrefixes)
            allPrefixes.put(prefix.getName(), prefix);
    }

    /**
     * The spec allows multiple BASEs, but we and Virtuoso don't.
     */
    public IRI getBase()
    {
        return base;
    }

    public void setBase(IRI base)
    {
        if(base == null)
            throw new IllegalArgumentException();

        this.base = base;
    }


    public void addPrefixDefinition(PrefixDefinition definition)
    {
        prefixDefinitions.add(definition);

        Prefix prefix = new Prefix(definition.getName(), definition.getIri().getAbsoluteURI(this).toString());

        if(userPrefixes.containsKey(definition.getName()))
            userPrefixes.remove(definition.getName());

        if(allPrefixes.containsKey(definition.getName()))
            allPrefixes.remove(definition.getName());

        userPrefixes.put(definition.getName(), prefix);
        allPrefixes.put(definition.getName(), prefix);
    }

    public List<PrefixDefinition> getPrefixes()
    {
        return prefixDefinitions;
    }

    /**
     * Returns all prefixes, including those from {@link QueryVisitor#getPredefinedPrefixes()}.
     *
     * Prefixes from this prologue go first.
     */
    public Collection<Prefix> getAllPrefixes()
    {
        return allPrefixes.values();
    }

    public Collection<Prefix> getUserPrefixes()
    {
        return userPrefixes.values();
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
