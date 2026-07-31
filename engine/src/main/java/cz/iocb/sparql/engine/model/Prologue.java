package cz.iocb.sparql.engine.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The initial part of a query, containing declarations.
 *
 * <p>
 * For example, for a {@link SelectQuery}, the part before SELECT.
 */
public class Prologue extends BaseElement
{
    private String base;
    private final LinkedHashMap<String, String> prefixes;
    private final List<PrefixDefinition> prefixDefinitions;


    public Prologue(Map<String, String> predefinedPrefixes)
    {
        setBase("");
        prefixDefinitions = new ArrayList<>();
        prefixes = new LinkedHashMap<>(predefinedPrefixes);
    }


    public String getBase()
    {
        return base;
    }


    public void setBase(String base)
    {
        this.base = base;
    }


    public void addPrefixDefinition(PrefixDefinition definition)
    {
        prefixDefinitions.add(definition);
        prefixes.put(definition.getName(), definition.getIri().getValue());
    }


    public List<PrefixDefinition> getPrefixeDefinitions()
    {
        return prefixDefinitions;
    }


    public LinkedHashMap<String, String> getPrefixes()
    {
        return prefixes;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
