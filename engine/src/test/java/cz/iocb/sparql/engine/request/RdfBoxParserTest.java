package cz.iocb.sparql.engine.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;



/**
 * Tests of {@link RdfBoxParser} on hand-written text forms; {@code RdfBoxClassTest} checks the forms the database
 * actually prints.
 */
public class RdfBoxParserTest
{
    private static final Iri xsdInteger = new Iri("http://www.w3.org/2001/XMLSchema#integer");
    private static final Iri xsdString = new Iri("http://www.w3.org/2001/XMLSchema#string");
    private static final Iri userType = new Iri("http://example.org/datatype#user");
    private static final Iri s = new Iri("http://example.org/s");
    private static final Iri p = new Iri("http://example.org/p");


    static Stream<Arguments> validArguments()
    {
        return Stream.of(
        // @formatter:off
            Arguments.of("<http://example.org/a>", new Iri("http://example.org/a")),
            Arguments.of("<>", new Iri("")),
            Arguments.of("_:i0000000100000002", new IntBlankNode(2, 1)),
            Arguments.of("_:iffffffffffffffff", new IntBlankNode(-1, -1)),
            Arguments.of("_:i00000000fffffffe", new IntBlankNode(-2, 0)),
            Arguments.of("_:s00000001", new StrBlankNode("", 1)),
            Arguments.of("_:s0000000aabc", new StrBlankNode("abc", 10)),
            Arguments.of("_:s0000000aa-2db-c3-a9-20", new StrBlankNode("a-b\u00e9 ", 10)),
            Arguments.of("_:sffffffffx", new StrBlankNode("x", -1)),
            Arguments.of("\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>", new TypedLiteral("1", xsdInteger)),
            Arguments.of("\"\"^^<http://www.w3.org/2001/XMLSchema#string>", new TypedLiteral("", xsdString)),
            Arguments.of("\"a\\\"b\\\\c\\n\\t\\r\\f\\b\\'d\"^^<http://www.w3.org/2001/XMLSchema#string>",
                    new TypedLiteral("a\"b\\c\n\t\r\f\b'd", xsdString)),
            Arguments.of("\"@x^^<y>\"^^<http://www.w3.org/2001/XMLSchema#string>",
                    new TypedLiteral("@x^^<y>", xsdString)),
            Arguments.of("\"hello\"@en", new LangStringLiteral("hello", "en")),
            Arguments.of("\"a@b\"@en-US", new LangStringLiteral("a@b", "en-US")),
            Arguments.of("\"hello\"@en--ltr", new DirLangStringLiteral("hello", "en", Direction.LTR)),
            Arguments.of("\"a@b--c\"@en-US--rtl", new DirLangStringLiteral("a@b--c", "en-US", Direction.RTL)),
            Arguments.of("'42:integer'^^<http://example.org/datatype#user>", new TypedLiteral("42", userType)),
            Arguments.of("'a:b:\"x:y\".\"t:z\"'^^<http://example.org/datatype#user>",
                    new TypedLiteral("a:b", userType)),
            Arguments.of("'a\\'b\\\"c:character varying'^^<http://example.org/datatype#user>",
                    new TypedLiteral("a'b\"c", userType)),
            Arguments.of("':integer'^^<http://example.org/datatype#user>", new TypedLiteral("", userType)),
            Arguments.of("<<( <http://example.org/s> <http://example.org/p> "
                    + "\"o\"^^<http://www.w3.org/2001/XMLSchema#string> )>>",
                    new TripleTerm(s, p, new TypedLiteral("o", xsdString))),
            Arguments.of("<<( _:i0000000100000002 <http://example.org/p> <http://example.org/o> )>>",
                    new TripleTerm(new IntBlankNode(2, 1), p, new Iri("http://example.org/o"))),
            Arguments.of("<<( _:s0000000aa-2db-c3-a9-20 <http://example.org/p> \"a b\"@en--ltr )>>",
                    new TripleTerm(new StrBlankNode("a-b\u00e9 ", 10), p,
                            new DirLangStringLiteral("a b", "en", Direction.LTR))),
            Arguments.of("<<( <http://example.org/s> <http://example.org/p> "
                    + "'42:integer'^^<http://example.org/datatype#user> )>>",
                    new TripleTerm(s, p, new TypedLiteral("42", userType))),
            Arguments.of("""
                <<( <http://example.org/s> <http://example.org/p> \
                <<( <http://example.org/a> <http://example.org/b> \
                "c )>>"^^<http://www.w3.org/2001/XMLSchema#string> )>> )>>""",
                    new TripleTerm(s, p, new TripleTerm(new Iri("http://example.org/a"),
                            new Iri("http://example.org/b"), new TypedLiteral("c )>>", xsdString))))
        // @formatter:on
        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("validArguments")
    void parseTest(String text, RdfTerm expected)
    {
        assertEquals(expected, RdfBoxParser.parse(text));
    }


    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "", "foo", "<a", "a>", "_:", "_:x", "_:i", "_:i123", "_:i000000010000000g",
            "_:i00000001000000020", "_:s", "_:s0000000", "_:s0000000g", "_:s00000001-", "_:s00000001-a",
            "_:s00000001-gg", "_:s00000001.", "\"", "\"abc", "\"abc\"", "\"a\\qb\"^^<t>", "\"a\\", "\"a\"^^t",
            "\"a\"^^<t", "\"a\"^^", "\"a\"x", "\"a\"@en--", "\"a\"@en--LTR", "\"a\"@en--xyz", "'a'^^<t>", "'a:b'@en",
            "'a:b'", "'a:b'^^<t", "<<( )>>", "<<( <http://example.org/s> )>>",
            "<<( <http://example.org/s> <http://example.org/p> )>>",
            "<<( <http://example.org/s> <http://example.org/p>  )>>",
            "<<(  <http://example.org/s> <http://example.org/p> \"o\"^^<t> )>>",
            "<<( \"s\" <http://example.org/p> \"o\"^^<t> )>>",
            "<<( <http://example.org/s> _:i0000000100000002 \"o\"^^<t> )>>",
            "<<( <http://example.org/s> \"p\" \"o\"^^<t> )>>",
            "<<( <http://example.org/s> <http://example.org/p> o )>>",
            "<<( <http://example.org/s><http://example.org/p> \"o\"^^<t> )>>",
            "<<( <http://example.org/s> <http://example.org/p> "
                    + "<<( <http://example.org/a> <http://example.org/b> )>> )>>",
            "<<( <<( <http://example.org/a> <http://example.org/b> \"c\"^^<t> )>> "
                    + "<http://example.org/p> \"o\"^^<t> )>>" })
    void invalidTest(String text)
    {
        assertThrows(IllegalArgumentException.class, () -> RdfBoxParser.parse(text));
    }
}
