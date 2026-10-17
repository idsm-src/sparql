package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
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
        ResourceClass other = subtract(request.getConfiguration(), iri, TestRequest.iriClass);
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


    /**
     * True if {@link ResourceClass#fromGeneralClass} converts the columns of the superclass with the representability
     * check, false if it refuses to.
     *
     * @param resClass the class to convert to
     * @param superClass the class to convert from
     * @return true if the conversion with the check succeeds, false if it is not supported
     */
    private static boolean checks(ResourceClass resClass, ResourceClass superClass)
    {
        List<Column> columns = superClass.getSqlTypes().stream().map(t -> expression(t, "c")).toList();

        try
        {
            resClass.fromGeneralClass(superClass, columns, false);
            return true;
        }
        catch(UnsupportedOperationException e)
        {
            return false;
        }
    }


    @Test
    void canCheckRepresentabilityTest()
    {
        ResourceClass other = subtract(request.getConfiguration(), iri, TestRequest.iriClass);
        ResourceClass any = unionize(unsupportedIri, TestRequest.iriClass);
        ResourceClass mixed = unionize(unsupportedIri, xsdString);

        assertTrue(unsupportedIri.canCheckRepresentability(unsupportedIri));
        assertFalse(unsupportedIri.canCheckRepresentability(iri));
        assertFalse(unsupportedIri.canCheckRepresentability(box));

        assertTrue(TestRequest.iriClass.canCheckRepresentability(iri));
        assertTrue(TestRequest.iriClass.canCheckRepresentability(box));
        assertTrue(other.canCheckRepresentability(iri));
        assertTrue(other.canCheckRepresentability(box));

        assertFalse(any.canCheckRepresentability(iri));
        assertFalse(any.canCheckRepresentability(box));
        assertFalse(mixed.canCheckRepresentability(box));

        assertTrue(unsupportedIri.canConvertTo(box));
        assertFalse(box.canConvertTo(unsupportedIri));
        assertFalse(iri.canConvertTo(any));
        assertTrue(iri.canConvertTo(other));

        for(ResourceClass resClass : List.of(unsupportedIri, TestRequest.iriClass, other, any, mixed))
            for(ResourceClass superClass : List.of(box, iri, any, resClass))
                if(resClass.isSubclassOf(superClass))
                    assertEquals(checks(resClass, superClass), resClass.canCheckRepresentability(superClass),
                            resClass + " from " + superClass);
    }
}
