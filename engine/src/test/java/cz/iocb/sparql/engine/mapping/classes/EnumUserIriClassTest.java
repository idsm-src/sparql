package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * The prefix an {@link EnumUserIriClass} gives for columns of its values: the whole member IRI for a constant storing
 * it, and the longest common prefix of the members for a column of the table and for a constant the class does not
 * store. The class holds the shapes {@code circle} and {@code square}, stored as 1 and 2.
 */
public class EnumUserIriClassTest
{
    /**
     * Namespace of the members.
     */
    private static final String shape = "http://example.org/shape/";

    /**
     * The class of the shapes.
     */
    private static final EnumUserIriClass shapes = new EnumUserIriClass("shape", INT4,
            Map.of(new Iri(shape + "circle"), "1", new Iri(shape + "square"), "2"));


    @Test
    @DisplayName("a constant has the prefix of the whole member IRI it stores")
    void constants()
    {
        assertEquals(shape + "circle", shapes.getPrefix(List.of(constant("1", INT4))));
        assertEquals(shape + "square", shapes.getPrefix(List.of(constant("2", INT4))));
    }


    @Test
    @DisplayName("any other column has the common prefix of the members")
    void columns()
    {
        assertEquals(shape, shapes.getPrefix(List.of(new TableColumn("shape", INT4))));
        assertEquals(shape, shapes.getPrefix(List.of(constant("3", INT4))));
    }
}
