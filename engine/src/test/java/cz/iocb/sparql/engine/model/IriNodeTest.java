package cz.iocb.sparql.engine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;



/**
 * Abbreviation of IRIs to prefixed names: the local names the production PN_LOCAL of the SPARQL grammar allows, long
 * local names, and a prefix with an empty IRI.
 */
public class IriNodeTest
{
    private static final Map<String, String> prefixes = Map.of("e", "http://example.org/");


    @Test
    @DisplayName("a local name may contain dots, but may neither start nor end with one")
    void dots()
    {
        assertEquals("e:a..b", IriNode.toPrefixedIri("http://example.org/a..b", prefixes));
        assertEquals("e:a.:", IriNode.toPrefixedIri("http://example.org/a.:", prefixes));
        assertEquals("e:a.%41", IriNode.toPrefixedIri("http://example.org/a.%41", prefixes));
        assertNull(IriNode.toPrefixedIri("http://example.org/.a", prefixes));
        assertNull(IriNode.toPrefixedIri("http://example.org/a.", prefixes));
        assertNull(IriNode.toPrefixedIri("http://example.org/a.%4", prefixes));
    }


    @Test
    @DisplayName("a long local name is matched without overflowing the stack")
    void longLocalName()
    {
        String name = "a.".repeat(500000) + "a";

        assertEquals("e:" + name, IriNode.toPrefixedIri("http://example.org/" + name, prefixes));
        assertNull(IriNode.toPrefixedIri("http://example.org/" + name + ".", prefixes));
    }


    @Test
    @DisplayName("a prefix with an empty IRI abbreviates only the IRIs that are valid local names")
    void emptyPrefixIri()
    {
        Prologue prologue = new Prologue(Map.of());
        prologue.addPrefixDefinition(new PrefixDefinition("e", ""));

        assertEquals("<http://example.org/f>", new IriNode("http://example.org/f").toString(prologue));
        assertEquals("e:urn:x", new IriNode("urn:x").toString(prologue));
    }
}
