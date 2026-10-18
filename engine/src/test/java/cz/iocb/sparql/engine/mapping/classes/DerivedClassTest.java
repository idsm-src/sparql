package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;



/**
 * The class algebra of {@link DerivedClass} on unions of many classes: an unevaluated SERVICE call binds its variables
 * in every class of the configuration, hundreds in a large deployment, and the unions and subclass tests over them must
 * not search for a covering combination of the classes, which no union of unrelated classes provides. Besides, a class
 * with a negated literal has to match the terms with variables, some values of which lie outside the negated class.
 */
public class DerivedClassTest
{
    /**
     * Declarations letting every two user IRI classes overlap.
     */
    private static final ClassRelations overlapping = (_, _) -> true;

    private static final PrimitiveResourceClass a = userClass("a");

    private static final PrimitiveResourceClass b = userClass("b");

    private static final PrimitiveResourceClass c = userClass("c");


    /**
     * Creates a user IRI class of the given name.
     *
     * @param name the name
     * @return the class
     */
    private static PrimitiveResourceClass userClass(String name)
    {
        return new IntegerUserIriClass(name, INT4, "http://example.org/" + name + "/");
    }


    /**
     * Creates the given number of user IRI classes.
     *
     * @param count the number of classes
     * @return the classes
     */
    private static Set<ResourceClass> userClasses(int count)
    {
        Set<ResourceClass> classes = new HashSet<>();

        for(int i = 0; i < count; i++)
            classes.add(userClass("c" + i));

        return classes;
    }


    @Test
    void unionOfClassesKeepsTheMaximalOnes()
    {
        assertEquals(iri, unionize(a, b, iri));
        assertEquals(box, unionize(a, iri, box));
        assertEquals(a, unionize(a, a));

        ResourceClass union = unionize(a, b);

        assertTrue(a.isSubclassOf(union));
        assertTrue(b.isSubclassOf(union));
        assertFalse(c.isSubclassOf(union));
        assertFalse(union.isSubclassOf(a));
        assertFalse(xsdString.isSubclassOf(union));
        assertTrue(union.isSubclassOf(iri));
        assertEquals(unionize(a, b, c), unionize(union, c));
    }


    @Test
    void coverageByPositiveClasses()
    {
        ResourceClass aNotB = subtract(overlapping, a, b);
        ResourceClass notIri = subtract(box, iri);

        assertTrue(aNotB.isSubclassOf(unionize(a, c)));
        assertTrue(aNotB.isSubclassOf(iri));
        assertFalse(aNotB.isSubclassOf(unionize(b, c)));
        assertFalse(notIri.isSubclassOf(unionize(a, b)));
        assertFalse(notIri.isSubclassOf(iri));
        assertTrue(notIri.isSubclassOf(box));

        // a right side with a term of two literals takes the general search
        assertTrue(a.isSubclassOf(unionize(a, aNotB)));
        assertTrue(aNotB.isSubclassOf(unionize(c, aNotB)));
        assertFalse(b.isSubclassOf(unionize(c, aNotB)));
        assertTrue(a
                .isSubclassOf(unionize(aNotB, subtract(overlapping, a, c), DerivedClass.intersect(overlapping, b, c))));
    }


    @Test
    void unionOfManyClasses()
    {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            Set<ResourceClass> classes = userClasses(300);
            ResourceClass union = unionize(classes);
            ResourceClass smaller = unionize(userClasses(299));

            assertTrue(classes.stream().allMatch(c -> c.isSubclassOf(union)));
            assertTrue(smaller.isSubclassOf(union));
            assertFalse(union.isSubclassOf(smaller));
            assertFalse(a.isSubclassOf(union));
            assertEquals(iri, unionize(union, iri));
            assertEquals(union, unionize(union, smaller));
            assertEquals(unionize(union, a), unionize(unionize(smaller, a), union));
        });
    }


    @Test
    void termsWithVariables()
    {
        Variable v = new Variable("v");
        Iri p = new Iri("http://example.org/p");
        TypedLiteral canonical = new TypedLiteral("1.0E0", xsdDoubleIri);
        TypedLiteral lexical = new TypedLiteral("1e0", xsdDoubleIri);

        assertTrue(lexDouble.match(null, v));
        assertTrue(lexDouble.match(null, lexical));
        assertFalse(lexDouble.match(null, canonical));

        TripleTermClass lexObject = new TripleTermClass(iri, iri, lexDouble);

        assertTrue(lexObject.match(null, new TripleTerm(v, p, v)));
        assertTrue(lexObject.match(null, new TripleTerm(p, p, lexical)));
        assertFalse(lexObject.match(null, new TripleTerm(p, p, canonical)));

        ResourceClass nonCanonicalObject = subtract(new TripleTermClass(iri, iri, genDouble),
                new TripleTermClass(iri, iri, xsdDouble));

        assertTrue(nonCanonicalObject.match(null, v));
        assertTrue(nonCanonicalObject.match(null, new TripleTerm(p, p, v)));
        assertTrue(nonCanonicalObject.match(null, new TripleTerm(p, p, lexical)));
        assertFalse(nonCanonicalObject.match(null, new TripleTerm(p, p, canonical)));
    }
}
