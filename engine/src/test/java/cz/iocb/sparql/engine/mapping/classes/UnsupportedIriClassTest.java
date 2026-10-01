package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Tests of {@link UnsupportedIriClass}: an IRI matches it exactly when no user IRI class of the configuration matches.
 */
public class UnsupportedIriClassTest
{
    private static final Iri known = new Iri(TestRequest.prefix + "42");
    private static final Iri knownWithSuffix = new Iri(TestRequest.prefix + "42/7");
    private static final Iri unknown = new Iri("http://example.org/other/42");
    private static final Iri almost = new Iri(TestRequest.prefix + "x42");

    private static Request request;


    @BeforeAll
    static void init() throws SQLException
    {
        request = TestRequest.create();
    }


    @AfterAll
    static void close() throws SQLException
    {
        request.close();
    }


    @Test
    void matchTest()
    {
        assertTrue(unsupportedIri.match(request, unknown));
        assertTrue(unsupportedIri.match(request, almost));
        assertTrue(unsupportedIri.match(request, new Variable("v")));

        assertFalse(unsupportedIri.match(request, known));
        assertFalse(unsupportedIri.match(request, knownWithSuffix));
        assertFalse(unsupportedIri.match(request, new TypedLiteral(unknown.getValue(), xsdStringIri)));
        assertFalse(unsupportedIri.match(request, new IntBlankNode(1, 0)));

        assertTrue(request.match(unsupportedIri, unknown));
        assertFalse(request.match(unsupportedIri, known));
        assertFalse(request.match(TestRequest.iriClass, unknown));
        assertTrue(request.match(TestRequest.iriClass, known));
    }


    @Test
    void derivedMatchTest()
    {
        ResourceClass other = subtract(iri, TestRequest.iriClass);
        ResourceClass any = unionize(unsupportedIri, TestRequest.iriClass);

        assertTrue(other.match(request, unknown));
        assertFalse(other.match(request, known));

        assertTrue(any.match(request, unknown));
        assertTrue(any.match(request, known));
        assertFalse(any.match(request, new IntBlankNode(1, 0)));
    }


    @Test
    void toColumnsTest()
    {
        List<Column> expected = List.of(constant(unknown.getValue(), VARCHAR));

        assertEquals(expected, unsupportedIri.toColumns(request, unknown));
        assertEquals(expected, request.getColumns(unsupportedIri, unknown));
        assertEquals(expected, request.getColumns(request.getIriClass(unknown), unknown));
    }
}
