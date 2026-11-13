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
     * The SPARQL version labels of SPARQL 1.2 Query (section 4.4.1): {@code 1.2}, {@code 1.2-basic} and {@code 1.1}.
     */
    public static final List<String> versionLabels = List.of("1.2", "1.2-basic", "1.1");

    /**
     * Base IRI in effect: that of the last BASE declaration, before it the base IRI the prologue was created with;
     * empty when there is none.
     */
    private String base;

    /**
     * Version label of the VERSION declaration, null when not declared.
     */
    private String version;

    /**
     * All prefixes in effect, predefined ones first.
     */
    private final LinkedHashMap<String, String> prefixes;

    /**
     * PREFIX declarations of the query, in order.
     */
    private final List<PrefixDefinition> prefixDefinitions;


    /**
     * Creates a prologue with no base IRI and the given predefined prefixes (typically those of the configuration).
     *
     * @param predefinedPrefixes prefixes configured for the endpoint
     */
    public Prologue(Map<String, String> predefinedPrefixes)
    {
        this("", predefinedPrefixes);
    }


    /**
     * Creates a prologue with the base IRI that is in effect until a BASE declaration replaces it (the default base IRI
     * of the endpoint) and the given predefined prefixes (typically those of the configuration).
     *
     * @param base the base IRI, an empty string for none
     * @param predefinedPrefixes prefixes configured for the endpoint
     */
    public Prologue(String base, Map<String, String> predefinedPrefixes)
    {
        setBase(base);
        prefixDefinitions = new ArrayList<>();
        prefixes = new LinkedHashMap<>(predefinedPrefixes);
    }


    /**
     * The base IRI in effect, which the relative IRIs are resolved against: that of the last BASE declaration, or the
     * one the prologue was created with; an empty string when there is none.
     *
     * @return the base IRI in effect; an empty string when there is none
     */
    public String getBase()
    {
        return base;
    }


    /**
     * Sets the base IRI in effect.
     *
     * @param base the base IRI
     */
    public void setBase(String base)
    {
        this.base = base;
    }


    /**
     * Version label (for example {@code 1.2}) of the last VERSION declaration; null when none is declared.
     *
     * @return version label (for example {@code 1.2}) of the last VERSION declaration; null when none is declared
     */
    public String getVersion()
    {
        return version;
    }


    /**
     * Sets the version label.
     *
     * @param version the version label, or null
     */
    public void setVersion(String version)
    {
        this.version = version;
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
