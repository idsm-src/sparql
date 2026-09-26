package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import cz.iocb.sparql.engine.Database;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.RdfBoxParser;



/**
 * Tests of {@link RdfBoxClass} as a result class: a term boxed from its most specific built-in class and printed by the
 * database is decoded back to the same term, and a user literal box, which needs a configured datatype, is decoded to
 * the expected literal.
 */
public class RdfBoxClassTest
{
    private static final String xsd = "http://www.w3.org/2001/XMLSchema#";

    public static final List<Datatype> datatypes = List.of(xsdBooleanType, xsdByteType, xsdUnsignedByteType,
            xsdShortType, xsdUnsignedShortType, xsdIntType, xsdUnsignedIntType, xsdLongType, xsdUnsignedLongType,
            xsdIntegerType, xsdNonPositiveIntegerType, xsdNegativeIntegerType, xsdNonNegativeIntegerType,
            xsdPositiveIntegerType, xsdDecimalType, xsdFloatType, xsdDoubleType, xsdDateTimeType, xsdDateType,
            xsdDayTimeDurationType, xsdStringType, rdfLangStringType);


    static Stream<Arguments> roundTripArguments()
    {
        return Stream.of(
        // @formatter:off
            Arguments.of(new Iri("http://example.org/a")),
            Arguments.of(new Iri("http://example.org/a?x=1&y=\"2\"#f")),
            Arguments.of(new IntBlankNode(2, 1)),
            Arguments.of(new IntBlankNode(-1, -2)),
            Arguments.of(new StrBlankNode("", 1)),
            Arguments.of(new StrBlankNode("abc", 10)),
            Arguments.of(new StrBlankNode("a-b\u00e9 \n'\"\\", -1)),
            Arguments.of(new TypedLiteral("true", new Iri(xsd + "boolean"))),
            Arguments.of(new TypedLiteral("1", new Iri(xsd + "boolean"))),
            Arguments.of(new TypedLiteral("-1", new Iri(xsd + "byte"))),
            Arguments.of(new TypedLiteral("255", new Iri(xsd + "unsignedByte"))),
            Arguments.of(new TypedLiteral("-32768", new Iri(xsd + "short"))),
            Arguments.of(new TypedLiteral("65535", new Iri(xsd + "unsignedShort"))),
            Arguments.of(new TypedLiteral("-2147483648", new Iri(xsd + "int"))),
            Arguments.of(new TypedLiteral("4294967295", new Iri(xsd + "unsignedInt"))),
            Arguments.of(new TypedLiteral("-9223372036854775808", new Iri(xsd + "long"))),
            Arguments.of(new TypedLiteral("18446744073709551615", new Iri(xsd + "unsignedLong"))),
            Arguments.of(new TypedLiteral("1", new Iri(xsd + "integer"))),
            Arguments.of(new TypedLiteral("+01", new Iri(xsd + "integer"))),
            Arguments.of(new TypedLiteral("123456789012345678901234567890", new Iri(xsd + "integer"))),
            Arguments.of(new TypedLiteral("-1", new Iri(xsd + "negativeInteger"))),
            Arguments.of(new TypedLiteral("0", new Iri(xsd + "nonPositiveInteger"))),
            Arguments.of(new TypedLiteral("0", new Iri(xsd + "nonNegativeInteger"))),
            Arguments.of(new TypedLiteral("1", new Iri(xsd + "positiveInteger"))),
            Arguments.of(new TypedLiteral("1.5", new Iri(xsd + "decimal"))),
            Arguments.of(new TypedLiteral("1.50", new Iri(xsd + "decimal"))),
            Arguments.of(new TypedLiteral("1.5E0", new Iri(xsd + "float"))),
            Arguments.of(new TypedLiteral("1.5", new Iri(xsd + "float"))),
            Arguments.of(new TypedLiteral("NaN", new Iri(xsd + "double"))),
            Arguments.of(new TypedLiteral("-INF", new Iri(xsd + "double"))),
            Arguments.of(new TypedLiteral("1.0E0", new Iri(xsd + "double"))),
            Arguments.of(new TypedLiteral("2000-01-01T00:00:00Z", new Iri(xsd + "dateTime"))),
            Arguments.of(new TypedLiteral("2000-01-01T00:00:00.5+01:00", new Iri(xsd + "dateTime"))),
            Arguments.of(new TypedLiteral("2000-01-01T00:00:00", new Iri(xsd + "dateTime"))),
            Arguments.of(new TypedLiteral("2000-01-01", new Iri(xsd + "date"))),
            Arguments.of(new TypedLiteral("2000-01-01-14:00", new Iri(xsd + "date"))),
            Arguments.of(new TypedLiteral("P1DT2H3M4.5S", new Iri(xsd + "dayTimeDuration"))),
            Arguments.of(new TypedLiteral("PT0S", new Iri(xsd + "dayTimeDuration"))),
            Arguments.of(new TypedLiteral("", new Iri(xsd + "string"))),
            Arguments.of(new TypedLiteral("a\"b\\c\n\t\r\f\b'd\u00e9", new Iri(xsd + "string"))),
            Arguments.of(new TypedLiteral("@x^^<y>", new Iri(xsd + "string"))),
            Arguments.of(new LangStringLiteral("hello", "en")),
            Arguments.of(new LangStringLiteral("a\"@b", "en-US")),
            Arguments.of(new TypedLiteral("x", new Iri("http://example.org/unknown"))),
            Arguments.of(new TypedLiteral("a\"b", new Iri("http://example.org/unknown"))),
            Arguments.of(new TypedLiteral("abc", new Iri(xsd + "integer")))
        // @formatter:on
        );
    }


    static Stream<Arguments> userLiteralArguments()
    {
        return Stream.of(
        // @formatter:off
            Arguments.of("sparql.rdfbox_create_from_userliteral(42, 'http://example.org/t')",
                    new TypedLiteral("42", new Iri("http://example.org/t"))),
            Arguments.of("sparql.rdfbox_create_from_userliteral_with_lexical(42, 'http://example.org/t', '+042')",
                    new TypedLiteral("+042", new Iri("http://example.org/t"))),
            Arguments.of("sparql.rdfbox_create_from_userliteral('a:b''c\"d:'::varchar, 'http://example.org/t')",
                    new TypedLiteral("a:b'c\"d:", new Iri("http://example.org/t"))),
            Arguments.of("sparql.rdfbox_create_from_userliteral(''::varchar, 'http://example.org/t')",
                    new TypedLiteral("", new Iri("http://example.org/t"))),
            Arguments.of("sparql.rdfbox_create_from_userliteral('{1,2}'::int[], 'http://example.org/t')",
                    new TypedLiteral("{1,2}", new Iri("http://example.org/t")))
        // @formatter:on
        );
    }


    /**
     * Most specific built-in class of the term, as {@code Request.getResourceClass} would select it for a configuration
     * without user datatypes.
     */
    private static ResourceClass getResourceClass(RdfTerm term)
    {
        return switch(term)
        {
            case Iri _ -> iri;
            case IntBlankNode _ -> intBlankNode;
            case StrBlankNode _ -> strBlankNode;
            case Literal literal -> datatypes.stream()
                    .filter(d -> d.getTypeIri().equals(literal.getType()) && d.isValidForm(literal.getValue()))
                    .findFirst().map(d -> d.getResourceClass(literal)).orElse(unsupportedType);
            default -> throw new IllegalArgumentException();
        };
    }


    /**
     * Evaluates the SQL expression and decodes the printed box.
     */
    private static RdfTerm evaluate(String expression) throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT " + expression))
        {
            assertTrue(result.next());
            return RdfBoxParser.parse(result.getString(1));
        }
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTripArguments")
    void roundTripTest(RdfTerm term) throws SQLException
    {
        ResourceClass resClass = getResourceClass(term);
        Column column = resClass.toGeneralClass(box, resClass.toColumns(null, term), false).get(0);

        assertEquals(term, evaluate(column.toString()));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("userLiteralArguments")
    void userLiteralTest(String expression, RdfTerm expected) throws SQLException
    {
        assertEquals(expected, evaluate(expression));
    }
}
