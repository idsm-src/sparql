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
    /**
     * BASE IRI, empty when not declared.
     */
    private String base;

    /**
     * All prefixes in effect, predefined ones first.
     */
    private final LinkedHashMap<String, String> prefixes;

    /**
     * PREFIX declarations of the query, in order.
     */
    private final List<PrefixDefinition> prefixDefinitions;


    /**
     * Creates a prologue with an empty BASE and the given predefined prefixes (typically those of the configuration).
     *
     * @param predefinedPrefixes prefixes configured for the endpoint
     */
    public Prologue(Map<String, String> predefinedPrefixes)
    {
        setBase("");
        prefixDefinitions = new ArrayList<>();
        prefixes = new LinkedHashMap<>(predefinedPrefixes);
    }


    /**
     * The BASE IRI; an empty string when none is declared.
     *
     * @return the BASE IRI; an empty string when none is declared
     */
    public String getBase()
    {
        return base;
    }


    /**
     * Sets the BASE IRI.
     *
     * @param base the BASE IRI
     */
    public void setBase(String base)
    {
        this.base = base;
    }


    /**
     * Registers a PREFIX declaration, overriding an earlier definition of the same name.
     *
     * @param definition the PREFIX declaration
     */
    public void addPrefixDefinition(PrefixDefinition definition)
    {
        prefixDefinitions.add(definition);
        prefixes.put(definition.getName(), definition.getIri().getValue());
    }


    /**
     * PREFIX declarations of the query, in order.
     *
     * @return PREFIX declarations of the query, in order
     */
    public List<PrefixDefinition> getPrefixeDefinitions()
    {
        return prefixDefinitions;
    }


    /**
     * All prefixes in effect, predefined ones first, mapped to their IRIs.
     *
     * @return all prefixes in effect, predefined ones first, mapped to their IRIs
     */
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
