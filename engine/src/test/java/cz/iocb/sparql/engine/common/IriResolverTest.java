package cz.iocb.sparql.engine.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;



/**
 * Resolution of IRI references by {@link IriResolver}: the reference resolution examples of section 5.4 of RFC 3986 and
 * the cases in which {@link java.net.URI#resolve(java.net.URI)} differs from the algorithm of the RFC.
 */
public class IriResolverTest
{
    /**
     * The base IRI of the examples of RFC 3986.
     */
    private static final String base = "http://a/b/c/d;p?q";


    @DisplayName("the normal examples of RFC 3986")
    @ParameterizedTest(name = "{0}")
    @CsvSource(quoteCharacter = '"', value = { "g:h, g:h", "g, http://a/b/c/g", "./g, http://a/b/c/g",
            "g/, http://a/b/c/g/", "/g, http://a/g", "//g, http://g", "?y, http://a/b/c/d;p?y", "g?y, http://a/b/c/g?y",
            "#s, http://a/b/c/d;p?q#s", "g#s, http://a/b/c/g#s", "g?y#s, http://a/b/c/g?y#s", ";x, http://a/b/c/;x",
            "g;x, http://a/b/c/g;x", "g;x?y#s, http://a/b/c/g;x?y#s", "\"\", http://a/b/c/d;p?q", "., http://a/b/c/",
            "./, http://a/b/c/", ".., http://a/b/", "../, http://a/b/", "../g, http://a/b/g", "../.., http://a/",
            "../../, http://a/", "../../g, http://a/g" })
    void normalExamples(String reference, String target)
    {
        assertEquals(target, IriResolver.resolve(base, reference));
    }


    @DisplayName("the abnormal examples of RFC 3986")
    @ParameterizedTest(name = "{0}")
    @CsvSource({ "../../../g, http://a/g", "../../../../g, http://a/g", "/./g, http://a/g", "/../g, http://a/g",
            "g., http://a/b/c/g.", ".g, http://a/b/c/.g", "g.., http://a/b/c/g..", "..g, http://a/b/c/..g",
            "./../g, http://a/b/g", "./g/., http://a/b/c/g/", "g/./h, http://a/b/c/g/h", "g/../h, http://a/b/c/h",
            "g;x=1/./y, http://a/b/c/g;x=1/y", "g;x=1/../y, http://a/b/c/y", "g?y/./x, http://a/b/c/g?y/./x",
            "g?y/../x, http://a/b/c/g?y/../x", "g#s/./x, http://a/b/c/g#s/./x", "g#s/../x, http://a/b/c/g#s/../x",
            "http:g, http:g" })
    void abnormalExamples(String reference, String target)
    {
        assertEquals(target, IriResolver.resolve(base, reference));
    }


    @Test
    @DisplayName("an empty authority of the base IRI is kept")
    void emptyAuthority()
    {
        assertEquals("file:///dir/data.ttl", IriResolver.resolve("file:///dir/query.rq", "data.ttl"));
        assertEquals("file:///data.ttl", IriResolver.resolve("file:///dir/query.rq", "../../data.ttl"));
        assertEquals("file:///dir/query.rq#part", IriResolver.resolve("file:///dir/query.rq", "#part"));
    }


    @Test
    @DisplayName("a base IRI with an authority and an empty path gets a slash")
    void emptyPath()
    {
        assertEquals("http://example.org/data", IriResolver.resolve("http://example.org", "data"));
        assertEquals("http://example.org?query", IriResolver.resolve("http://example.org", "?query"));
        assertEquals("http://example.org", IriResolver.resolve("http://example.org#part", ""));
    }


    @Test
    @DisplayName("a base IRI without an authority loses the last segment of its path, or a whole path without slashes")
    void noAuthority()
    {
        assertEquals("urn:data", IriResolver.resolve("urn:example:base", "data"));
        assertEquals("a:/b/data", IriResolver.resolve("a:/b/c", "data"));
        assertEquals("a:data", IriResolver.resolve("a:c", "./data"));
    }


    @Test
    @DisplayName("a reference without a scheme stays relative when there is no base IRI")
    void noBase()
    {
        assertEquals("dir/data", IriResolver.resolve("", "dir/./data"));
        assertEquals("//example.org/data", IriResolver.resolve("", "//example.org/data"));
        assertEquals("http://example.org/data", IriResolver.resolve("", "http://example.org/dir/../data"));
    }
}
