package cz.iocb.chemweb.server.sparql.parser.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * The initial part of a query, containing declarations.
 *
 * <p>
 * For example, for a {@link SelectQuery}, the part before SELECT.
 */
public class Prologue extends BaseElement
{
    private IRI base;
    private final LinkedHashMap<String, Prefix> prefixes;
    private final List<PrefixDefinition> prefixDefinitions;
    //private final LinkedHashMap<String, String> prefixMap;


    public Prologue(HashMap<String, String> predefinedPrefixes)
    {
        setBase(new IRI(""));
        prefixDefinitions = new ArrayList<PrefixDefinition>();
        prefixes = new LinkedHashMap<String, Prefix>();

        for(Entry<String, String> entry : predefinedPrefixes.entrySet())
            prefixes.put(entry.getKey(), new Prefix(entry.getKey(), entry.getValue()));
    }


    public IRI getBase()
    {
        return base;
    }


    public void setBase(IRI base)
    {
        this.base = base;
    }


    public void addPrefixDefinition(PrefixDefinition definition)
    {
        prefixDefinitions.add(definition);

        prefixes.remove(definition.getName());
        prefixes.put(definition.getName(), new Prefix(definition.getName(), definition.getIri().getValue()));
    }

    public List<PrefixDefinition> getPrefixeDefinitions()
    {
        return prefixDefinitions;
    }


    public Collection<Prefix> getPrefixes()
    {
        return prefixes.values();
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
