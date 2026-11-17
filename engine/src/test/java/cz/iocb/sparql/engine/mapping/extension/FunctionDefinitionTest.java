package cz.iocb.sparql.engine.mapping.extension;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Function;



/**
 * The result class of an extension function: the SQL function returns one value, so a class of several columns is
 * refused when the function is defined, not when a call of it is translated.
 */
public class FunctionDefinitionTest
{
    @Test
    @DisplayName("a result class of several columns is refused")
    void resultColumns()
    {
        Function now = new Function("pg_catalog", "now");

        assertEquals(xsdScalarDateTime,
                new FunctionDefinition("http://example.org/now", now, xsdScalarDateTime, List.of(), false, false)
                        .getResultClass());

        assertThrows(IllegalArgumentException.class,
                () -> new FunctionDefinition("http://example.org/now", now, xsdDateTime, List.of(), false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new FunctionDefinition("http://example.org/now", now, xsdDateTime, List.of(), 0, false, false));
    }
}
