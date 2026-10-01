package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUserType;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Tests of {@link UserLiteralCompositeBaseClass}: every valid literal of the user datatypes of the configuration
 * matches it and is boxed, with its lexical form, from the class of its datatype.
 */
public class UserLiteralCompositeBaseClassTest
{
    private static final Iri intIri = UserDatatypes.intDatatype.getTypeIri();
    private static final Iri uuidIri = UserDatatypes.uuidDatatype.getTypeIri();
    private static final SqlType intType = UserDatatypes.intDatatype.getCanonicalLiteralClass().getSqlTypes().get(0);

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
        assertTrue(genUserType.match(request, new TypedLiteral("42", intIri)));
        assertTrue(genUserType.match(request, new TypedLiteral("+042", intIri)));
        assertTrue(genUserType.match(request, new TypedLiteral("123E4567-E89B-12D3-A456-426614174000", uuidIri)));
        assertTrue(genUserType.match(request, new Variable("v")));

        assertFalse(genUserType.match(request, new TypedLiteral("x", intIri)));
        assertFalse(genUserType.match(request, new TypedLiteral("42", xsdIntegerIri)));
        assertFalse(genUserType.match(request, new TypedLiteral("42", new Iri("http://example.org/unknown"))));
        assertFalse(genUserType.match(request, intIri));

        assertTrue(request.match(genUserType, new TypedLiteral("+042", intIri)));
        assertFalse(request.match(genUserType, new TypedLiteral("x", intIri)));
    }


    @Test
    void toColumnsTest() throws SQLException
    {
        Literal canonical = new TypedLiteral("42", intIri);
        Literal other = new TypedLiteral("+042", intIri);
        List<Column> canonicalColumns = genUserType.toColumns(request, canonical);
        List<Column> otherColumns = genUserType.toColumns(request, other);

        assertEquals(List.of(expression("sparql.ubox_create(%s)", constant("42", intType)),
                constant(intIri.getValue(), VARCHAR), constant("", VARCHAR)), canonicalColumns);
        assertEquals(List.of(expression("sparql.ubox_create(%s)", constant("42", intType)),
                constant(intIri.getValue(), VARCHAR), constant("+042", VARCHAR)), otherColumns);
        assertEquals(otherColumns, request.getColumns(genUserType, other));

        Column canonicalBoxed = genUserType.toGeneralClass(box, canonicalColumns, false).get(0);
        Column otherBoxed = genUserType.toGeneralClass(box, otherColumns, false).get(0);

        assertEquals(canonical, TestRequest.evaluate(request, canonicalBoxed.toString()));
        assertEquals(other, TestRequest.evaluate(request, otherBoxed.toString()));

        assertThrows(IllegalArgumentException.class,
                () -> genUserType.toColumns(request, new TypedLiteral("x", intIri)));
        assertThrows(IllegalArgumentException.class,
                () -> genUserType.toColumns(request, new TypedLiteral("42", xsdIntegerIri)));
    }
}
