package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;



/**
 * The identity of primitive classes: a class is identified by its name, so two classes of the same column layout
 * registered under different names are different classes, which the subclass test and the unions keep apart even when
 * the classes compare no other property of their own.
 */
public class PrimitiveResourceClassTest
{
    /**
     * Creates a user IRI class of the given name over a fixed IRI prefix.
     *
     * @param name the name
     * @return the class
     */
    private static PrimitiveResourceClass userClass(String name)
    {
        return new IntegerUserIriClass(name, INT4, "http://example.org/");
    }


    @Test
    void userIriClassesAreIdentifiedByTheirNames()
    {
        PrimitiveResourceClass a = userClass("a");
        PrimitiveResourceClass b = userClass("b");

        assertEquals(userClass("a"), a);
        assertEquals(userClass("a").hashCode(), a.hashCode());
        assertNotEquals(a, b);
        assertFalse(a.isSubclassOf(b));
        assertFalse(b.isSubclassOf(a));

        ResourceClass union = unionize(a, b);

        assertNotEquals(a, union);
        assertNotEquals(b, union);
        assertTrue(a.isSubclassOf(union));
        assertTrue(b.isSubclassOf(union));
        assertEquals(union, unionize(a, b, userClass("a")));
    }


    @Test
    void subsetsOfOneLiteralClassAreDifferent()
    {
        PrimitiveResourceClass x = new SubsetLiteralClass("x", xsdString);
        PrimitiveResourceClass y = new SubsetLiteralClass("y", xsdString);

        assertEquals(new SubsetLiteralClass("x", xsdString), x);
        assertNotEquals(x, y);
        assertFalse(x.isSubclassOf(y));
        assertTrue(x.isSubclassOf(xsdString));
        assertNotEquals(x, unionize(x, y));
        assertEquals(xsdString, unionize(x, y, xsdString));
    }
}
