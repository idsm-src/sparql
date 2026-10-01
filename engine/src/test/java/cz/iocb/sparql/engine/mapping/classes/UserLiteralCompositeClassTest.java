package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.UBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.userType;
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
 * Tests of {@link UserLiteralCompositeClass}: canonical literals of the user datatypes of the configuration match it
 * and are boxed from the class of their datatype.
 */
public class UserLiteralCompositeClassTest
{
    private static final Iri intIri = UserDatatypes.intDatatype.getTypeIri();
    private static final Iri uuidIri = UserDatatypes.uuidDatatype.getTypeIri();
    private static final Iri tokenIri = UserDatatypes.tokenDatatype.getTypeIri();
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
        assertTrue(userType.match(request, new TypedLiteral("42", intIri)));
        assertTrue(userType.match(request, new TypedLiteral("123e4567-e89b-12d3-a456-426614174000", uuidIri)));
        assertTrue(userType.match(request, new TypedLiteral("a b", tokenIri)));
        assertTrue(userType.match(request, new Variable("v")));

        assertFalse(userType.match(request, new TypedLiteral("+042", intIri)));
        assertFalse(userType.match(request, new TypedLiteral("x", intIri)));
        assertFalse(userType.match(request, new TypedLiteral("123E4567-E89B-12D3-A456-426614174000", uuidIri)));
        assertFalse(userType.match(request, new TypedLiteral("42", xsdIntegerIri)));
        assertFalse(userType.match(request, new TypedLiteral("42", new Iri("http://example.org/unknown"))));
        assertFalse(userType.match(request, intIri));

        assertTrue(request.match(userType, new TypedLiteral("42", intIri)));
        assertFalse(request.match(userType, new TypedLiteral("+042", intIri)));
    }


    @Test
    void toColumnsTest() throws SQLException
    {
        Literal value = new TypedLiteral("42", intIri);
        List<Column> columns = userType.toColumns(request, value);

        assertEquals(List.of(expression(UBOX, "sparql.ubox_create(%s)", constant("42", intType)),
                constant(intIri.getValue(), VARCHAR)), columns);
        assertEquals(columns, request.getColumns(userType, value));

        Column boxed = userType.toGeneralClass(box, columns, false).get(0);

        assertEquals(value, TestRequest.evaluate(request, boxed.toString()));

        assertThrows(IllegalArgumentException.class,
                () -> userType.toColumns(request, new TypedLiteral("+042", intIri)));
        assertThrows(IllegalArgumentException.class, () -> userType.toColumns(request, new TypedLiteral("x", intIri)));
        assertThrows(IllegalArgumentException.class,
                () -> userType.toColumns(request, new TypedLiteral("42", xsdIntegerIri)));
    }
}
