package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.literal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
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
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Tests of {@link UnsupportedLiteralClass}: a literal matches it exactly when the request cannot classify it into a
 * class of its datatype.
 */
public class UnsupportedLiteralClassTest
{
    private static final Iri unknownIri = new Iri("http://example.org/unknown");
    private static final Iri intIri = UserDatatypes.intDatatype.getTypeIri();

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
        assertTrue(unsupportedType.match(request, new TypedLiteral("x", unknownIri)));
        assertTrue(unsupportedType.match(request, new TypedLiteral("abc", xsdIntegerIri)));
        assertTrue(unsupportedType.match(request, new TypedLiteral("x", rdfLangStringIri)));
        assertTrue(unsupportedType.match(request, new TypedLiteral("abc", intIri)));
        assertTrue(unsupportedType.match(request, new Variable("v")));

        assertFalse(unsupportedType.match(request, new TypedLiteral("1", xsdIntegerIri)));
        assertFalse(unsupportedType.match(request, new TypedLiteral("+01", xsdIntegerIri)));
        assertFalse(unsupportedType.match(request, new TypedLiteral("x", xsdStringIri)));
        assertFalse(unsupportedType.match(request, new LangStringLiteral("x", "en")));
        assertFalse(unsupportedType.match(request, new TypedLiteral("42", intIri)));
        assertFalse(unsupportedType.match(request, new TypedLiteral("+042", intIri)));
        assertFalse(unsupportedType.match(request, unknownIri));

        assertTrue(request.match(unsupportedType, new TypedLiteral("abc", xsdIntegerIri)));
        assertFalse(request.match(unsupportedType, new TypedLiteral("1", xsdIntegerIri)));
    }


    @Test
    void unionMatchTest()
    {
        assertTrue(literal.match(request, new TypedLiteral("x", unknownIri)));
        assertTrue(literal.match(request, new TypedLiteral("abc", xsdIntegerIri)));
        assertTrue(literal.match(request, new TypedLiteral("1", xsdIntegerIri)));
        assertTrue(literal.match(request, new LangStringLiteral("x", "en")));
        assertFalse(literal.match(request, unknownIri));
    }


    @Test
    void toColumnsTest() throws SQLException
    {
        Literal value = new TypedLiteral("abc", xsdIntegerIri);
        List<Column> columns = unsupportedType.toColumns(request, value);

        assertEquals(List.of(constant("abc", VARCHAR), constant(xsdIntegerIri.getValue(), VARCHAR)), columns);
        assertEquals(columns, request.getColumns(unsupportedType, value));

        Column boxed = unsupportedType.toGeneralClass(box, columns, false).get(0);

        assertEquals(value, TestRequest.evaluate(request, boxed.toString()));
    }
}
