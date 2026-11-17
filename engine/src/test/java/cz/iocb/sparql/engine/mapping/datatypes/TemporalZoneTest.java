package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.TypedLiteral;



/**
 * The timezone offsets of xsd:date and xsd:dateTime literals and the classes of the literals in other than the
 * canonical form: every offset the lexical forms allow (Z, the offsets from -14:00 to +14:00 by the minute, the zero
 * offset of both signs) is its seconds east of UTC, a literal without an offset gets {@link Integer#MIN_VALUE}, and the
 * literals of one offset in other than the canonical form share one class, the base class of the offset without the
 * canonical class of all offsets.
 */
public class TemporalZoneTest
{
    /**
     * The offsets the lexical forms allow, by their texts.
     *
     * @return the seconds east of UTC by the texts of the offsets
     */
    private static Map<String, Integer> getOffsets()
    {
        Map<String, Integer> offsets = new LinkedHashMap<>();

        offsets.put("Z", 0);
        offsets.put("+00:00", 0);
        offsets.put("-00:00", 0);

        for(int minutes = -14 * 60; minutes <= 14 * 60; minutes++)
        {
            int value = Math.abs(minutes);
            String text = String.format(ROOT, "%s%02d:%02d", minutes < 0 ? "-" : "+", value / 60, value % 60);

            offsets.put(text, minutes * 60);
        }

        return offsets;
    }


    @Test
    @DisplayName("the offset of a literal in seconds east of UTC")
    void offsets()
    {
        for(Map.Entry<String, Integer> offset : getOffsets().entrySet())
        {
            assertEquals(offset.getValue(), DateTimeDatatype
                    .getZone(new TypedLiteral("2020-01-01T10:00:00" + offset.getKey(), xsdDateTimeIri)));
            assertEquals(offset.getValue(), DateTimeDatatype
                    .getZone(new TypedLiteral(" -0044-03-15T12:00:00.5" + offset.getKey() + " ", xsdDateTimeIri)));
            assertEquals(offset.getValue(),
                    DateDatatype.getZone(new TypedLiteral("2020-01-01" + offset.getKey(), xsdDateIri)));
            assertEquals(offset.getValue(),
                    DateDatatype.getZone(new TypedLiteral(" -0044-03-15" + offset.getKey() + " ", xsdDateIri)));
        }

        assertEquals(Integer.MIN_VALUE,
                DateTimeDatatype.getZone(new TypedLiteral("2020-01-01T10:00:00", xsdDateTimeIri)));
        assertEquals(Integer.MIN_VALUE, DateDatatype.getZone(new TypedLiteral(" 2020-01-01 ", xsdDateIri)));
    }


    @Test
    @DisplayName("the literals of an offset in other than the canonical form share one class")
    void otherForms()
    {
        for(Map.Entry<String, Integer> offset : getOffsets().entrySet())
        {
            int zone = offset.getValue();

            ResourceClass dateTimeClass = xsdDateTimeType
                    .getResourceClass(new TypedLiteral(" 2020-01-01T10:00:00" + offset.getKey(), xsdDateTimeIri));

            assertEquals(subtract(DateTimeInZoneBaseClass.get(zone), xsdScalarDateTime), dateTimeClass);
            assertSame(dateTimeClass, xsdDateTimeType
                    .getResourceClass(new TypedLiteral("2021-06-01T00:00:00.50" + offset.getKey(), xsdDateTimeIri)));

            ResourceClass dateClass = xsdDateType
                    .getResourceClass(new TypedLiteral(" 2020-01-01" + offset.getKey(), xsdDateIri));

            assertEquals(subtract(DateInZoneBaseClass.get(zone), xsdScalarDate), dateClass);
            assertSame(dateClass,
                    xsdDateType.getResourceClass(new TypedLiteral("2021-06-01" + offset.getKey() + " ", xsdDateIri)));
        }
    }
}
