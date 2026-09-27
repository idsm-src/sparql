package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasTripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isTripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.literal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.reference;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.stringLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.tripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getExpressionClass;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * Tests of {@link TripleTermClass} without a database: the column layout, the structural subclass and disjointness
 * relations (also inside derived classes), matching of terms, constant columns and the SQL of the conversions to and
 * from the box and other triple term classes.
 */
public class TripleTermClassTest
{
    private static final TripleTermClass stringObject = new TripleTermClass(iri, iri, xsdString);
    private static final TripleTermClass integerObject = new TripleTermClass(iri, iri, xsdInteger);
    private static final TripleTermClass generalObject = new TripleTermClass(iri, iri, box);
    private static final TripleTermClass referenceSubject = new TripleTermClass(reference, iri, stringLiteral);
    private static final TripleTermClass blankSubject = new TripleTermClass(intBlankNode, iri, rdfLangString);
    private static final TripleTermClass nested = new TripleTermClass(iri, iri, stringObject);

    private static final Iri s = new Iri("http://example.org/s");
    private static final Iri p = new Iri("http://example.org/p");
    private static final TripleTerm term = new TripleTerm(s, p, new TypedLiteral("o", xsdStringType.getTypeIri()));


    @Test
    void columnsTest()
    {
        assertEquals(List.of(VARCHAR, VARCHAR, VARCHAR), stringObject.getSqlTypes());
        assertEquals(List.of(RDFBOX, VARCHAR, RDFBOX), tripleTerm.getSqlTypes());
        assertEquals(List.of(INT4, INT4, VARCHAR, VARCHAR, VARCHAR), blankSubject.getSqlTypes());
        assertEquals(List.of(VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR), nested.getSqlTypes());
        assertEquals(5, nested.getColumnCount());

        List<Column> columns = List.of(expression("s"), expression("p"), expression("a"), expression("b"),
                expression("c"));

        assertEquals(List.of(expression("s")), nested.getSubjectColumns(columns));
        assertEquals(List.of(expression("p")), nested.getPredicateColumns(columns));
        assertEquals(List.of(expression("a"), expression("b"), expression("c")), nested.getObjectColumns(columns));

        for(int i = 0; i < blankSubject.getColumnCount(); i++)
            assertFalse(blankSubject.isOptionalColumn(i));
    }


    @Test
    void identityTest()
    {
        assertEquals(stringObject, new TripleTermClass(iri, iri, xsdString));
        assertEquals(stringObject.hashCode(), new TripleTermClass(iri, iri, xsdString).hashCode());
        assertNotEquals(stringObject, integerObject);
        assertNotEquals(stringObject, generalObject);
        assertEquals("tripleterm(iri,iri,string)", stringObject.getResourceName());
    }


    @Test
    void subclassTest()
    {
        assertTrue(stringObject.isSubclassOf(stringObject));
        assertTrue(stringObject.isSubclassOf(generalObject));
        assertTrue(stringObject.isSubclassOf(tripleTerm));
        assertTrue(stringObject.isSubclassOf(box));
        assertTrue(stringObject.isSubclassOf(referenceSubject));
        assertTrue(blankSubject.isSubclassOf(referenceSubject));
        assertTrue(nested.isSubclassOf(new TripleTermClass(box, iri, tripleTerm)));
        assertTrue(nested.isSubclassOf(generalObject));

        assertFalse(generalObject.isSubclassOf(stringObject));
        assertFalse(stringObject.isSubclassOf(integerObject));
        assertFalse(stringObject.isSubclassOf(blankSubject));
        assertFalse(stringObject.isSubclassOf(nested));
        assertFalse(stringObject.isSubclassOf(iri));
        assertFalse(stringObject.isSubclassOf(literal));
        assertFalse(iri.isSubclassOf(stringObject));
        assertFalse(box.isSubclassOf(stringObject));

        assertTrue(isTripleTerm(nested));
        assertTrue(isTripleTerm(unionize(stringObject, integerObject)));
        assertFalse(isTripleTerm(unionize(stringObject, iri)));
        assertFalse(isTripleTerm(box));

        assertTrue(stringObject.isSubclassOf(unionize(integerObject, generalObject)));
        assertTrue(stringObject.isSubclassOf(subtract(box, iri)));
        assertFalse(stringObject.isSubclassOf(unionize(integerObject, iri)));
    }


    @Test
    void disjunctTest()
    {
        assertTrue(areDisjunct(stringObject, integerObject));
        assertTrue(areDisjunct(stringObject, blankSubject));
        assertTrue(areDisjunct(nested, stringObject));
        assertTrue(areDisjunct(stringObject, iri));
        assertTrue(areDisjunct(stringObject, reference));
        assertTrue(areDisjunct(stringObject, literal));
        assertTrue(areDisjunct(unionize(integerObject, iri), stringObject));

        assertFalse(areDisjunct(stringObject, stringObject));
        assertFalse(areDisjunct(stringObject, generalObject));
        assertFalse(areDisjunct(generalObject, referenceSubject));
        assertFalse(areDisjunct(referenceSubject, generalObject));
        assertFalse(areDisjunct(nested, generalObject));
        assertFalse(areDisjunct(stringObject, box));
        assertFalse(areDisjunct(unionize(generalObject, iri), referenceSubject));
        assertFalse(areDisjunct(subtract(box, iri), stringObject));

        assertTrue(hasTripleTerm(box));
        assertTrue(hasTripleTerm(unionize(stringObject, iri)));
        assertTrue(hasTripleTerm(subtract(box, literal)));
        assertFalse(hasTripleTerm(literal));
        assertFalse(hasTripleTerm(reference));
    }


    @Test
    void unionTest()
    {
        assertEquals(generalObject, unionize(stringObject, generalObject));
        assertEquals(tripleTerm, unionize(blankSubject, tripleTerm));
        assertEquals(box, getExpressionClass(stringObject));
        assertEquals(box, unionize(stringObject, iri).getEffectiveClass());
        assertEquals(box, unionize(generalObject, referenceSubject).getEffectiveClass());
    }


    @Test
    void matchTest()
    {
        assertTrue(stringObject.match(null, term));
        assertTrue(generalObject.match(null, term));
        assertTrue(referenceSubject.match(null, term));
        assertTrue(tripleTerm.match(null, term));
        assertTrue(nested.match(null, new TripleTerm(s, p, term)));
        assertTrue(stringObject.match(null, new Variable("v")));
        assertTrue(stringObject.match(null, new TripleTerm(new Variable("s"), p, new Variable("o"))));

        assertFalse(integerObject.match(null, term));
        assertFalse(blankSubject.match(null, term));
        assertFalse(nested.match(null, term));
        assertFalse(stringObject.match(null, new TripleTerm(s, p, term)));
        assertFalse(stringObject.match(null, new TripleTerm(new IntBlankNode(1, 0), p, term.getObject())));
        assertFalse(stringObject.match(null, s));
        assertFalse(stringObject.match(null, new TypedLiteral("1", xsdIntegerType.getTypeIri())));
    }


    @Test
    void toColumnsTest()
    {
        assertEquals(List.of(constant("http://example.org/s", VARCHAR), constant("http://example.org/p", VARCHAR),
                constant("o", VARCHAR)), stringObject.toColumns(null, term));

        assertEquals(List.of(constant("http://example.org/s", VARCHAR), constant("http://example.org/p", VARCHAR),
                constant("http://example.org/s", VARCHAR), constant("http://example.org/p", VARCHAR),
                constant("o", VARCHAR)), nested.toColumns(null, new TripleTerm(s, p, term)));
    }


    @Test
    void conversionTest()
    {
        List<Column> columns = List.of(expression("s"), expression("p"), expression("o"));
        Column x = expression("x");

        assertEquals(columns, stringObject.toGeneralClass(stringObject, columns, true));
        assertEquals(columns, stringObject.fromGeneralClass(stringObject, columns, false));

        assertEquals(List.of(expression("sparql.rdfbox_create_from_tripleterm(sparql.rdfbox_create_from_iri(s), p, "
                + "sparql.rdfbox_create_from_string(o))")), stringObject.toGeneralClass(box, columns, true));

        assertEquals(
                List.of(expression("sparql.rdfbox_create_from_iri(s)"), expression("p"),
                        expression("sparql.rdfbox_create_from_string(o)")),
                stringObject.toGeneralClass(tripleTerm, columns, true));

        assertEquals(List.of(expression("s"), expression("p"), expression("sparql.rdfbox_create_from_string(o)")),
                stringObject.toGeneralClass(generalObject, columns, true));

        assertEquals(
                List.of(expression("sparql.rdfbox_get_iri(sparql.rdfbox_get_tripleterm_subject(x))"),
                        expression("sparql.rdfbox_get_tripleterm_predicate(x)"),
                        expression("sparql.rdfbox_get_string(sparql.rdfbox_get_tripleterm_object(x))")),
                stringObject.fromGeneralClass(box, List.of(x), false));

        assertEquals(
                List.of(expression("sparql.rdfbox_get_tripleterm_subject(x)"),
                        expression("sparql.rdfbox_get_tripleterm_predicate(x)"),
                        expression("sparql.rdfbox_get_tripleterm_object(x)")),
                tripleTerm.fromGeneralClass(box, List.of(x), false));

        assertEquals(
                List.of(expression("sparql.rdfbox_get_iri(s)"), expression("p"),
                        expression("sparql.rdfbox_get_string(o)")),
                stringObject.fromGeneralClass(tripleTerm, columns, false));

        assertEquals(List.of(expression("s"), expression("p"), expression("sparql.rdfbox_get_string(o)")),
                stringObject.fromGeneralClass(generalObject, columns, false));

        assertEquals(stringObject.fromGeneralClass(tripleTerm, columns, false),
                tripleTerm.toClass(stringObject, columns, false));

        assertEquals(stringObject.toGeneralClass(tripleTerm, columns, false),
                stringObject.toClass(tripleTerm, columns, false));

        List<Column> nestedColumns = List.of(expression("s"), expression("p"), expression("a"), expression("b"),
                expression("c"));

        assertEquals(List.of(expression("""
                sparql.rdfbox_create_from_tripleterm(sparql.rdfbox_create_from_iri(s), p, \
                sparql.rdfbox_create_from_tripleterm(sparql.rdfbox_create_from_iri(a), b, \
                sparql.rdfbox_create_from_string(c)))""")), nested.toGeneralClass(box, nestedColumns, true));

        assertEquals(
                List.of(expression("sparql.rdfbox_get_iri(sparql.rdfbox_get_tripleterm_subject(x))"),
                        expression("sparql.rdfbox_get_tripleterm_predicate(x)"),
                        expression("sparql.rdfbox_get_iri(sparql.rdfbox_get_tripleterm_subject("
                                + "sparql.rdfbox_get_tripleterm_object(x)))"),
                        expression("sparql.rdfbox_get_tripleterm_predicate(sparql.rdfbox_get_tripleterm_object(x))"),
                        expression("sparql.rdfbox_get_string(sparql.rdfbox_get_tripleterm_object("
                                + "sparql.rdfbox_get_tripleterm_object(x)))")),
                nested.fromGeneralClass(box, List.of(x), false));
    }
}
