package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Tests of {@link InternalResourceClass}: a class of internal values is a subclass of the box, so that a variable
 * needed in any class keeps such values, but it is converted neither to the box nor from it.
 */
public class InternalResourceClassTest
{
    private static final InternalResourceClass internal = new InternalResourceClass(List.of(INT8));
    private static final List<Column> columns = List.of(new TableColumn("id", INT8));


    @Test
    void hierarchyTest()
    {
        InternalResourceClass other = new InternalResourceClass(List.of(INT8));

        assertTrue(internal.isSubclassOf(box));
        assertFalse(box.isSubclassOf(internal));
        assertFalse(ResourceClass.areDisjunct(internal, box));

        assertNotEquals(internal, other);
        assertFalse(internal.isSubclassOf(other));
        assertTrue(ResourceClass.areDisjunct(internal, other));
        assertTrue(ResourceClass.areDisjunct(internal, iri));
        assertTrue(ResourceClass.areDisjunct(internal, xsdLong));
    }


    @Test
    void conversionTest()
    {
        assertTrue(internal.canConvertTo(internal));
        assertFalse(internal.canConvertTo(box));
        assertFalse(internal.canConvertTo(iri));

        assertTrue(internal.canCheckRepresentability(internal));
        assertFalse(internal.canCheckRepresentability(box));

        assertEquals(columns, internal.toGeneralClass(internal, columns, false));
        assertEquals(columns, internal.fromGeneralClass(internal, columns, false));

        assertThrows(IllegalArgumentException.class, () -> internal.toGeneralClass(box, columns, false));
        assertThrows(IllegalArgumentException.class, () -> internal.fromGeneralClass(box, columns, false));
    }


    @Test
    void neededInAnyClassTest()
    {
        Variable variable = new Variable("@node");

        VariableBindings bindings = new VariableBindings();
        bindings.add(new VariableBinding(variable, internal, columns, false));

        // the box stands for any class in what a node needs of a variable
        VariableBindings needed = bindings.restrict(ClassRelations.NONE, new Restrictions(List.of(variable)));

        assertEquals(columns, needed.get(variable).getMapping(internal));

        assertNull(bindings.restrict(ClassRelations.NONE, new Restrictions()).get(variable));
    }
}
