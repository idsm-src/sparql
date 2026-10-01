package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.intersect;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.DerivedClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * Detection of the class of an IRI over user IRI classes that overlap: an IRI belonging to several classes gets their
 * intersection, classes that a found class makes needless are not tried, and the matching and the constant columns are
 * served per class. No database is needed, as the classes match by regular expressions only.
 */
public class IriClassDetectionTest
{
    private static final String prefix = "http://example.org/compound/";

    /**
     * Integer IRI class counting the calls of {@code match}, to tell which classes the detection tried.
     */
    private static class CountingIntegerIriClass extends IntegerUserIriClass
    {
        int matches = 0;

        CountingIntegerIriClass(String name, String prefix)
        {
            super(name, INT4, prefix);
        }

        @Override
        public boolean match(Request request, Iri iri)
        {
            matches++;
            return super.match(request, iri);
        }
    }


    /**
     * String IRI class counting the calls of {@code match}, to tell which classes the detection tried.
     */
    private static class CountingStringIriClass extends StringUserIriClass
    {
        int matches = 0;

        CountingStringIriClass(String name, String prefix, String pattern)
        {
            super(name, prefix, pattern);
        }

        @Override
        public boolean match(Request request, Iri iri)
        {
            matches++;
            return super.match(request, iri);
        }
    }


    /**
     * Subclass of the compound class counting the calls of {@code match}.
     */
    private static class CountingSmallIriClass extends SmallCompoundIriClass
    {
        int matches = 0;

        CountingSmallIriClass(String name, IntegerUserIriClass compound, String prefix)
        {
            super(name, compound, prefix);
        }

        @Override
        public boolean match(Request request, Iri iri)
        {
            matches++;
            return super.match(request, iri);
        }
    }


    /** all compounds, overlaps with {@link #even} */
    private static CountingIntegerIriClass compound;

    /** compounds with an even id, stored as text; overlaps with {@link #compound} and {@link #small} */
    private static CountingStringIriClass even;

    /** compounds with an id below 100, a subclass of {@link #compound}; overlaps with {@link #even} */
    private static CountingSmallIriClass small;

    /** unrelated and disjoint with everything */
    private static CountingIntegerIriClass other;

    private static SparqlDatabaseConfiguration config;

    private static Request request;


    @BeforeAll
    static void init() throws SQLException
    {
        compound = new CountingIntegerIriClass("compound", prefix);
        even = new CountingStringIriClass("even", prefix, "[0-9]*[02468]");
        small = new CountingSmallIriClass("small", compound, prefix);
        other = new CountingIntegerIriClass("other", "http://example.org/other/");

        config = new SparqlDatabaseConfiguration(null, null, null, false);

        // all have check cost 0, so the detection tries them in this order: the subclass before its superclass
        config.addIriClass(small);
        config.addIriClass(compound);
        config.addIriClass(even);
        config.addIriClass(other);
        config.addIriClassOverlap(compound, even);
        config.addIriClassOverlap(small, even);

        request = new Request(config);
    }


    @AfterAll
    static void close() throws SQLException
    {
        request.close();
    }


    @BeforeEach
    void reset()
    {
        compound.matches = 0;
        even.matches = 0;
        small.matches = 0;
        other.matches = 0;
    }


    private static Iri compound(int id)
    {
        return new Iri(prefix + id);
    }


    private static List<UserIriClass> tried()
    {
        return List.of(small, compound, even, other).stream().filter(c -> matches(c) > 0).toList();
    }


    private static int matches(UserIriClass resClass)
    {
        return switch(resClass)
        {
            case CountingIntegerIriClass c -> c.matches;
            case CountingStringIriClass c -> c.matches;
            case CountingSmallIriClass c -> c.matches;
            default -> throw new IllegalArgumentException();
        };
    }


    @Test
    @DisplayName("the registration keeps the cost order and the overlap declarations")
    void registration() throws SQLException
    {
        assertEquals(List.of(small, compound, even, other), config.getIriClasses());

        assertTrue(config.mayOverlap(compound, even));
        assertTrue(config.mayOverlap(even, small));
        assertFalse(config.mayOverlap(compound, other));
        assertFalse(config.mayOverlap(even, other));

        // the same registration and declaration are accepted again, a different definition under the same name is not
        config.addIriClass(even);
        config.addIriClassOverlap(even, compound);
        assertThrows(IllegalArgumentException.class,
                () -> config.addIriClass(new CountingIntegerIriClass("other", "http://example.org/x/")));
        assertEquals(List.of(small, compound, even, other), config.getIriClasses());

        // only registered classes that are neither equal nor related can be declared to overlap
        SparqlDatabaseConfiguration another = new SparqlDatabaseConfiguration(null, null, null, false);
        IntegerUserIriClass a = new IntegerUserIriClass("a", INT4, "http://example.org/a/");
        IntegerUserIriClass b = new IntegerUserIriClass("b", INT4, "http://example.org/b/");
        SmallCompoundIriClass smallA = new SmallCompoundIriClass("small-a", a, "http://example.org/a/");

        another.addIriClass(a);
        assertThrows(IllegalArgumentException.class, () -> another.addIriClassOverlap(a, b));
        another.addIriClass(b);
        another.addIriClass(smallA);
        assertThrows(IllegalArgumentException.class, () -> another.addIriClassOverlap(a, a));
        assertThrows(IllegalArgumentException.class, () -> another.addIriClassOverlap(a, smallA));

        assertTrue(areDisjunct(another, a, b));
        another.addIriClassOverlap(a, b);
        assertTrue(another.mayOverlap(a, b));
        assertTrue(another.mayOverlap(b, a));
        assertFalse(areDisjunct(another, a, b));

        // the declaration concerns the two classes only: it does not extend to their subclasses
        assertFalse(another.mayOverlap(smallA, b));
        assertTrue(areDisjunct(another, smallA, b));

        // the declarations are those of the configuration supplied: elsewhere the same classes stay disjoint
        SparqlDatabaseConfiguration yetAnother = new SparqlDatabaseConfiguration(null, null, null, false);
        yetAnother.addIriClass(a);
        yetAnother.addIriClass(b);
        assertTrue(areDisjunct(yetAnother, a, b));
        assertTrue(areDisjunct(ClassRelations.NONE, a, b));
        assertFalse(areDisjunct(another, a, b));
    }


    @Test
    @DisplayName("unrelated classes are disjoint unless both are registered as not disjoint")
    void disjointness()
    {
        assertFalse(areDisjunct(config, compound, even));
        assertFalse(areDisjunct(config, small, even));
        assertFalse(areDisjunct(config, compound, small));
        assertTrue(areDisjunct(config, compound, other));
        assertTrue(areDisjunct(config, even, other));
        assertTrue(areDisjunct(config, small, other));

        assertTrue(areDisjunct(config, unsupportedIri, even));
        assertTrue(areDisjunct(config, unsupportedIri, compound));
        assertFalse(areDisjunct(config, iri, even));
        assertFalse(areDisjunct(config, box, even));

        assertFalse(areDisjunct(config, intersect(config, compound, even), small));
        assertTrue(areDisjunct(config, intersect(config, compound, even), other));
        assertFalse(areDisjunct(config, subtract(config, compound, small), even));
        assertTrue(areDisjunct(config, subtract(config, compound, even), intersect(config, small, even)));
    }


    @Test
    @DisplayName("the intersection of overlapping classes is stored in one of them, deterministically")
    void intersection()
    {
        ResourceClass both = intersect(config, compound, even);

        assertTrue(both instanceof DerivedClass);
        assertSame(compound, both.getEffectiveClass());
        assertEquals("compound@compound&even", both.getResourceName());
        assertEquals(List.of(INT4), both.getSqlTypes());
        assertEquals(both, intersect(config, even, compound));

        assertTrue(both.isSubclassOf(compound));
        assertTrue(both.isSubclassOf(even));
        assertTrue(both.isSubclassOf(iri));
        assertFalse(compound.isSubclassOf(both));
        assertFalse(both.isSubclassOf(small));

        assertSame(small, intersect(config, compound, small));
        assertEquals(intersect(config, small, even), intersect(config, compound, small, even));
        assertSame(even, intersect(config, small, even).getEffectiveClass());
        assertTrue(intersect(config, small, even).isSubclassOf(both));

        assertSame(iri, unionize(compound, even).getEffectiveClass());
        assertSame(compound, unionize(compound, small));
        assertSame(compound, unionize(compound, both));

        assertEquals(Set.of(compound, even), DerivedClass.estimateAsUnion(both));
    }


    @Test
    @DisplayName("an IRI of several classes gets their intersection, needless classes are not tried")
    void detection()
    {
        // the subclass is found first, so its superclass is not tried; the disjoint class is not tried either
        assertEquals(intersect(config, small, even), request.getIriClass(compound(42)));
        assertThat(tried(), contains(small, even));

        reset();
        assertSame(small, request.getIriClass(compound(41)));
        assertThat(tried(), contains(small, even));

        reset();
        assertEquals(intersect(config, compound, even), request.getIriClass(compound(142)));
        assertThat(tried(), contains(small, compound, even));

        reset();
        assertSame(compound, request.getIriClass(compound(141)));
        assertThat(tried(), contains(small, compound, even));

        // after a match of a disjoint class, nothing else is tried
        reset();
        assertSame(other, request.getIriClass(new Iri("http://example.org/other/5")));
        assertThat(tried(), contains(small, compound, even, other));

        reset();
        assertSame(unsupportedIri, request.getIriClass(new Iri("http://example.org/x")));
        assertThat(tried(), contains(small, compound, even, other));

        // the detections are cached
        reset();
        assertEquals(intersect(config, small, even), request.getIriClass(compound(42)));
        assertSame(unsupportedIri, request.getIriClass(new Iri("http://example.org/x")));
        assertThat(tried(), empty());
    }


    @Test
    @DisplayName("the detection restricted to some classes intersects those only")
    void restrictedDetection()
    {
        assertSame(compound, request.detectIriClass(compound(42), List.of(compound, other)));
        assertThat(tried(), contains(compound));

        reset();
        assertEquals(intersect(config, compound, even), request.detectIriClass(compound(42), List.of(even, compound)));
        assertThat(tried(), contains(compound, even));

        reset();
        assertSame(unsupportedIri, request.detectIriClass(compound(42), List.of(other)));
        assertThat(tried(), contains(other));

        reset();
        assertSame(unsupportedIri, request.detectIriClass(compound(42), List.of()));
        assertThat(tried(), empty());
    }


    @Test
    @DisplayName("an IRI matches every class its detected class is a subclass of")
    void match()
    {
        assertTrue(request.match(compound, compound(42)));
        assertTrue(request.match(even, compound(42)));
        assertTrue(request.match(small, compound(42)));
        assertFalse(request.match(other, compound(42)));
        assertFalse(request.match(unsupportedIri, compound(42)));
        assertTrue(request.match(iri, compound(42)));
        assertTrue(request.match(box, compound(42)));

        assertTrue(request.match(compound, compound(141)));
        assertFalse(request.match(even, compound(141)));
        assertFalse(request.match(small, compound(141)));

        assertTrue(request.match(unsupportedIri, new Iri("http://example.org/x")));
        assertFalse(request.match(compound, new Iri("http://example.org/x")));

        // derived classes are matched literal by literal through the cached detections
        assertTrue(request.match(intersect(config, compound, even), compound(42)));
        assertFalse(request.match(intersect(config, compound, even), compound(41)));
        assertTrue(request.match(subtract(config, compound, even), compound(41)));
        assertFalse(request.match(subtract(config, compound, small), compound(42)));
        assertTrue(request.match(unionize(small, other), compound(42)));
        assertFalse(request.match(unionize(small, other), compound(142)));

        assertFalse(unsupportedIri.match(request, compound(42)));
        assertTrue(unsupportedIri.match(request, new Iri("http://example.org/x")));
    }


    @Test
    @DisplayName("the constant columns are those of the requested class")
    void columns()
    {
        assertEquals(List.of(constant("42", INT4)), request.getColumns(compound, compound(42)));
        assertEquals(List.of(constant("42", INT4)), request.getColumns(small, compound(42)));
        assertEquals(List.of(constant("42", VARCHAR)), request.getColumns(even, compound(42)));
        assertEquals(List.of(constant(prefix + "42", VARCHAR)), request.getColumns(iri, compound(42)));

        ResourceClass detected = request.getIriClass(compound(42));
        assertEquals(List.of(constant("42", VARCHAR)), request.getColumns(detected, compound(42)));
        assertEquals(request.getColumns(detected, compound(42)), request.getColumns(detected, compound(42)));

        assertEquals(List.of(constant("141", INT4)),
                request.getColumns(request.getIriClass(compound(141)), compound(141)));

        Iri unknown = new Iri("http://example.org/x");
        assertEquals(List.of(constant(unknown.getValue(), VARCHAR)), request.getColumns(unsupportedIri, unknown));
        assertEquals(List.of(constant(unknown.getValue(), VARCHAR)),
                request.getColumns(request.getIriClass(unknown), unknown));
    }


    @Test
    @DisplayName("the same prefix is a prefix of the IRI in each class")
    void prefix()
    {
        assertEquals(prefix + "42", request.getIriPrefix(compound, request.getColumns(compound, compound(42))));
        assertEquals(prefix + "42", request.getIriPrefix(even, request.getColumns(even, compound(42))));
    }


    @Test
    @DisplayName("unrelated overlapping classes cannot be declared disjoint when they share an IRI")
    void declaration()
    {
        Set<ResourceClass> classes = Set.of(compound, even);
        assertFalse(areDisjunct(config, small, classes));
        assertTrue(areDisjunct(config, other, classes));
    }


    @Test
    @DisplayName("the operations without declarations refuse operands whose result would depend on them")
    void withoutDeclarations()
    {
        // one user IRI class can meet the built-in classes without the declarations
        assertTrue(areDisjunct(compound, unsupportedIri));
        assertFalse(areDisjunct(compound, iri));
        assertFalse(areDisjunct(compound, Set.of(iri, unsupportedIri)));
        assertSame(compound, intersect(compound, iri));
        assertSame(compound, intersect(iri, compound));
        assertTrue(subtract(iri, compound).isSubclassOf(iri));

        // two of them need the declarations, in one operand as well as on both sides
        assertThrows(IllegalArgumentException.class, () -> areDisjunct(compound, even));
        assertThrows(IllegalArgumentException.class, () -> areDisjunct(compound, Set.of(iri, even)));
        assertThrows(IllegalArgumentException.class, () -> intersect(compound, even));
        assertThrows(IllegalArgumentException.class, () -> intersect(intersect(config, compound, even), iri));
        assertThrows(IllegalArgumentException.class, () -> subtract(compound, even));
        assertThrows(IllegalArgumentException.class, () -> subtract(iri, intersect(config, compound, even)));
        assertThrows(IllegalArgumentException.class, () -> areDisjunct(intersect(config, compound, even), small));
    }
}
