package cz.iocb.sparql.engine.mapping.extension;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.stringLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * The result and argument classes of an extension function: the SQL function returns one value and takes one value per
 * argument, so a class of several columns is refused when the function is defined, not when a call of it is translated;
 * a class of a single column takes the place of each such class.
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


    @Test
    @DisplayName("an argument class of several columns is refused")
    void argumentColumns()
    {
        Function year = new Function("pg_catalog", "year");

        for(ResourceClass argument : List.of(xsdScalarDateTime, xsdDouble, stringLiteral, box))
            assertEquals(List.of(argument),
                    new FunctionDefinition("http://example.org/year", year, xsdInteger, List.of(argument), false, true)
                            .getArgumentClasses());

        for(ResourceClass argument : List.of(xsdDateTime, xsdDate, genDouble, rdfLangString))
            assertThrows(IllegalArgumentException.class, () -> new FunctionDefinition("http://example.org/year", year,
                    xsdInteger, List.of(xsdString, argument), 1, false, true));
    }
}
