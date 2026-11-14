package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.intersect;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.function.IntFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.TypedLiteral;



/**
 * The relations of the classes of the xsd:dateTime and xsd:date values of one timezone offset. The base class of an
 * offset holds every valid value of the offset, the canonical values with an empty lexical form, so the canonical class
 * of the offset is its subclass. The canonical classes that are not bound to an offset (the scalar and the composite
 * one, and their subsets) hold the canonical values of the offset as well, so the base class overlaps them, although it
 * is neither their subclass nor their superclass. A literal in another than the canonical form belongs to the base
 * class of its offset and to no canonical class.
 */
public class InZoneClassRelationsTest
{
    /**
     * The classes of one datatype.
     */
    private enum Family
    {
        /**
         * The classes of xsd:dateTime.
         */
        DATE_TIME(
                DateTimeInZoneBaseClass::get,
                DateTimeInZoneClass::get,
                genScalarDateTime,
                genDateTime,
                xsdScalarDateTime,
                xsdDateTime,
                lexDateTime,
                "2020-01-01T10:00:00Z",
                "2020-01-01T10:00:00.0Z"),

        /**
         * The classes of xsd:date.
         */
        DATE(
                DateInZoneBaseClass::get,
                DateInZoneClass::get,
                genScalarDate,
                genDate,
                xsdScalarDate,
                xsdDate,
                lexDate,
                "2020-01-01Z",
                "2020-01-01+00:00");


        /**
         * The base classes by timezone offset.
         */
        private final IntFunction<LiteralClass> base;

        /**
         * The canonical classes by timezone offset.
         */
        private final IntFunction<CanonicalLiteralClass> canonical;

        /**
         * The scalar base class of all offsets.
         */
        private final LiteralClass scalarBase;

        /**
         * The composite base class of all offsets.
         */
        private final LiteralClass compositeBase;

        /**
         * The scalar canonical class of all offsets.
         */
        private final CanonicalLiteralClass scalar;

        /**
         * The composite canonical class of all offsets.
         */
        private final CanonicalLiteralClass composite;

        /**
         * The class of the values of all offsets in other than the canonical form.
         */
        private final ResourceClass nonCanonical;

        /**
         * A literal of the offset 0 in the canonical form.
         */
        private final Literal canonicalLiteral;

        /**
         * The same value in another form.
         */
        private final Literal nonCanonicalLiteral;


        /**
         * Creates the family.
         *
         * @param base the base classes by timezone offset
         * @param canonical the canonical classes by timezone offset
         * @param scalarBase the scalar base class of all offsets
         * @param compositeBase the composite base class of all offsets
         * @param scalar the scalar canonical class of all offsets
         * @param composite the composite canonical class of all offsets
         * @param nonCanonical the class of the values of all offsets in other than the canonical form
         * @param canonicalForm the canonical form of a value of the offset 0
         * @param nonCanonicalForm another form of the same value
         */
        private Family(IntFunction<LiteralClass> base, IntFunction<CanonicalLiteralClass> canonical,
                LiteralClass scalarBase, LiteralClass compositeBase, CanonicalLiteralClass scalar,
                CanonicalLiteralClass composite, ResourceClass nonCanonical, String canonicalForm,
                String nonCanonicalForm)
        {
            this.base = base;
            this.canonical = canonical;
            this.scalarBase = scalarBase;
            this.compositeBase = compositeBase;
            this.scalar = scalar;
            this.composite = composite;
            this.nonCanonical = nonCanonical;
            this.canonicalLiteral = new TypedLiteral(canonicalForm, scalar.getDatatype().getTypeIri());
            this.nonCanonicalLiteral = new TypedLiteral(nonCanonicalForm, scalar.getDatatype().getTypeIri());
        }


        /**
         * The other family.
         *
         * @return the other family
         */
        private Family other()
        {
            return this == DATE ? DATE_TIME : DATE;
        }
    }


    /**
     * Checks that the classes overlap and that neither is a subclass of the other, asking both ways.
     *
     * @param a one class
     * @param b the other class
     */
    private static void assertOverlap(ResourceClass a, ResourceClass b)
    {
        assertFalse(areDisjunct(a, b), a + " and " + b);
        assertFalse(areDisjunct(b, a), b + " and " + a);
        assertFalse(a.isSubclassOf(b), a + " in " + b);
        assertFalse(b.isSubclassOf(a), b + " in " + a);
    }


    /**
     * Checks that the classes are disjoint, asking both ways.
     *
     * @param a one class
     * @param b the other class
     */
    private static void assertDisjoint(ResourceClass a, ResourceClass b)
    {
        assertTrue(areDisjunct(a, b), a + " and " + b);
        assertTrue(areDisjunct(b, a), b + " and " + a);
    }


    /**
     * Checks that the class is a proper subclass of the other one, so that they are not disjoint.
     *
     * @param subclass the subclass
     * @param superclass the superclass
     */
    private static void assertSubclass(ResourceClass subclass, ResourceClass superclass)
    {
        assertTrue(subclass.isSubclassOf(superclass), subclass + " in " + superclass);
        assertFalse(superclass.isSubclassOf(subclass), superclass + " in " + subclass);
        assertFalse(areDisjunct(subclass, superclass), subclass + " and " + superclass);
        assertFalse(areDisjunct(superclass, subclass), superclass + " and " + subclass);
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Family.class)
    @DisplayName("the canonical class of an offset is a subclass of the base class of the offset")
    void canonicalClassInBaseClass(Family family)
    {
        for(int zone : List.of(0, 3600, -3600, Integer.MIN_VALUE))
        {
            assertSubclass(family.canonical.apply(zone), family.base.apply(zone));
            assertSubclass(family.base.apply(zone), family.compositeBase);
            assertSubclass(family.base.apply(zone), family.scalarBase);
        }

        assertDisjoint(family.canonical.apply(3600), family.base.apply(0));
        assertDisjoint(family.canonical.apply(0), family.base.apply(Integer.MIN_VALUE));
        assertDisjoint(family.base.apply(3600), family.base.apply(0));
        assertDisjoint(family.canonical.apply(3600), family.canonical.apply(0));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Family.class)
    @DisplayName("the base class of an offset overlaps the canonical classes of all offsets")
    void baseClassOverlapsCanonicalClasses(Family family)
    {
        LiteralClass base = family.base.apply(0);

        assertOverlap(base, family.scalar);
        assertOverlap(base, family.composite);

        // the canonical values of the offset are what they share
        assertSubclass(family.canonical.apply(0), intersect(base, family.composite));
        assertSubclass(family.canonical.apply(0), intersect(base, family.scalar));
        assertDisjoint(family.canonical.apply(3600), intersect(base, family.composite));

        // a union with a class it overlaps is held in a class of all offsets
        assertEquals(family.compositeBase, unionize(base, family.composite).getEffectiveClass());
        assertEquals(base, unionize(base, family.canonical.apply(0)));

        // the classes of the other datatype stay disjoint
        assertDisjoint(base, family.other().scalar);
        assertDisjoint(base, family.other().composite);
        assertDisjoint(base, family.other().base.apply(0));
        assertDisjoint(base, family.other().canonical.apply(0));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Family.class)
    @DisplayName("the base class of an offset overlaps a subset of a canonical class unless it is bound to an offset")
    void baseClassAndSubsetClasses(Family family)
    {
        LiteralClass base = family.base.apply(0);

        assertOverlap(base, new SubsetLiteralClass("composite-subset", family.composite));
        assertOverlap(base, new SubsetLiteralClass("scalar-subset", family.scalar));
        assertSubclass(new SubsetLiteralClass("canonical-subset", family.canonical.apply(0)), base);
        assertDisjoint(base, new SubsetLiteralClass("other-offset-subset", family.canonical.apply(3600)));
    }


    @ParameterizedTest(name = "{0}")
    @EnumSource(Family.class)
    @DisplayName("a literal in another than the canonical form belongs to no canonical class")
    void classOfLiteral(Family family)
    {
        ResourceClass canonicalClass = family.scalar.getDatatype().getResourceClass(family.canonicalLiteral);
        ResourceClass otherClass = family.scalar.getDatatype().getResourceClass(family.nonCanonicalLiteral);

        assertEquals(family.canonical.apply(0), canonicalClass);

        assertTrue(otherClass.match(null, family.nonCanonicalLiteral));
        assertFalse(otherClass.match(null, family.canonicalLiteral));
        assertEquals(family.base.apply(0), otherClass.getEffectiveClass());

        assertSubclass(otherClass, family.base.apply(0));
        assertSubclass(otherClass, family.nonCanonical);
        assertSubclass(otherClass, family.compositeBase);

        assertDisjoint(otherClass, canonicalClass);
        assertDisjoint(otherClass, family.scalar);
        assertDisjoint(otherClass, family.composite);
        assertDisjoint(otherClass, intersect(family.base.apply(0), family.composite));
        assertDisjoint(otherClass, family.base.apply(3600));
    }
}
