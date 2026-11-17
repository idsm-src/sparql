package cz.iocb.sparql.engine.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;



/**
 * Resolution of IRI references by {@link IriResolver}: the reference resolution examples of section 5.4 of RFC 3986 and
 * the cases in which {@link java.net.URI#resolve(java.net.URI)} differs from the algorithm of the RFC; and the syntax
 * of IRI references of RFC 3987, whose characters beyond ASCII the tests write as hexadecimal code points in braces:
 * the examples of RFC 3986, the forms of IPv6 addresses, the characters RFC 3987 allows in every component and in the
 * query alone, and the characters and forms it does not allow.
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


    /**
     * The text with every hexadecimal number in braces replaced by the character of that code point.
     *
     * @param text the text
     * @return the text with the characters
     */
    private static String chars(String text)
    {
        return Pattern.compile("\\{([0-9A-F]+)\\}").matcher(text)
                .replaceAll(m -> Matcher.quoteReplacement(Character.toString(Integer.parseInt(m.group(1), 16))));
    }


    @DisplayName("IRI references by the syntax of RFC 3987")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "ftp://ftp.is.co.za/rfc/rfc1808.txt", "ldap://[2001:db8::7]/c=GB?objectClass?one",
            "mailto:John.Doe@example.com", "news:comp.infosystems.www.servers.unix", "tel:+1-816-555-1212",
            "telnet://192.0.2.16:80/", "urn:oasis:names:specification:docbook:dtd:xml:4.1.2", "", "#s", "?y", "g",
            "./g", "../g", "//g", "/g", ";x", "g;x?y#s", "a/b:c", "urn:", "http:", "file:///dir/data.ttl",
            "http://user:pw@host:8080/p?q#f", "http://host:/", "http://a/%41%2f", "http://[::1]/",
            "http://[1:2:3:4:5:6:7:8]/", "http://[1::3:4:5:6:7:8]/", "http://[::ffff:192.0.2.1]/",
            "http://[v1.fe80::a+en1]/", "http://example.org/a{A0}b", "http://example.org/{2028}",
            "http://{3000}.example.org/", "http://u{E9}@example.org/", "http://example.org/{1F600}?{E000}#{D7FF}",
            "http://example.org/?{10FFFD}", "http://example.org/{EFFFD}" })
    void iriReferences(String text)
    {
        assertTrue(IriResolver.isIriReference(chars(text)));
    }


    @DisplayName("texts that are no IRI references by the syntax of RFC 3987")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "abc##def", "http://a/%zz", "http://a/%2", "1a:b", ":a", "%41:b", "http://a@b@c/",
            "//a:x/b", "http://a/[b]", "http://[::1", "http://[1::2::3]/", "http://[1:2:3:4:5:6:7:8:9]/",
            "http://[::ffff:1.2.3.256]/", "http://[fe80::1%25eth0]/", "http://a/b{20}c", "http://a/{85}",
            "http://a/{FDD0}", "http://a/{1FFFE}", "http://a/{E0001}", "http://a/{E000}", "http://a/#{F0000}" })
    void malformed(String text)
    {
        assertFalse(IriResolver.isIriReference(chars(text)));
    }


    @Test
    @DisplayName("a long IRI reference does not exhaust the stack")
    void longReference() throws Exception
    {
        String iri = "http://example.org/" + "a%41/".repeat(100000) + "?" + "q".repeat(100000);
        boolean[] result = new boolean[1];

        // a small stack, which the evaluation of a regular expression repeating a group fills on such a text
        Thread thread = new Thread(null, () -> result[0] = IriResolver.isIriReference(iri), "long", 1 << 18);
        thread.start();
        thread.join();

        assertTrue(result[0]);
    }


    @Test
    @DisplayName("an IRI reference with a scheme is an IRI")
    void scheme()
    {
        for(String iri : List.of("http://a", "a:b", "urn:", "g:h"))
            assertTrue(IriResolver.hasScheme(iri), iri);

        for(String relative : List.of("", "g", "//a", "/a:b", "?a:b", "#a:b", "./a:b"))
            assertFalse(IriResolver.hasScheme(relative), relative);
    }
}
