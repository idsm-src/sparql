package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Nested;



public class DateInZoneBaseClassTest
{
    public static abstract class AbstractDateInZoneBaseClassTest extends AbstractResourceClassTest
    {
        protected AbstractDateInZoneBaseClassTest(int zone)
        {
            String noZone = Integer.toString(Integer.MIN_VALUE);

            super(DateInZoneBaseClass.get(zone), List.of("date", "varchar"), filterZone(zone, Map.ofEntries(
            // @formatter:off
                // canonical
                entry("0000-01-01", List.of("0001-01-01 BC", noZone, "")),
                entry("0001-01-01", List.of("0001-01-01", noZone, "")),
                entry("-0001-01-01", List.of("0002-01-01 BC", noZone, "")),
                entry("1970-01-01", List.of("1970-01-01", noZone, "")),
                entry("10000-01-01", List.of("10000-01-01", noZone, "")),
                entry("0000-01-01Z", List.of("0001-01-01 BC", "0", "")),
                entry("0001-01-01Z", List.of("0001-01-01", "0", "")),
                entry("-0001-01-01Z", List.of("0002-01-01 BC", "0", "")),
                entry("1970-01-01Z", List.of("1970-01-01", "0", "")),
                entry("10000-01-01Z", List.of("10000-01-01", "0", "")),
                entry("0000-01-01+01:00", List.of("0001-01-01 BC", "3600", "")),
                entry("0001-01-01+01:00", List.of("0001-01-01", "3600", "")),
                entry("-0001-01-01+01:00", List.of("0002-01-01 BC", "3600", "")),
                entry("1970-01-01+01:00", List.of("1970-01-01", "3600", "")),
                entry("10000-01-01+01:00", List.of("10000-01-01", "3600", "")),
                entry("0000-01-01-01:00", List.of("0001-01-01 BC", "-3600", "")),
                entry("0001-01-01-01:00", List.of("0001-01-01", "-3600", "")),
                entry("-0001-01-01-01:00", List.of("0002-01-01 BC", "-3600", "")),
                entry("1970-01-01-01:00", List.of("1970-01-01", "-3600", "")),
                entry("10000-01-01-01:00", List.of("10000-01-01", "-3600", "")),
                entry("0000-01-01+14:00", List.of("0001-01-01 BC", "50400", "")),
                entry("0001-01-01+14:00", List.of("0001-01-01", "50400", "")),
                entry("-0001-01-01+14:00", List.of("0002-01-01 BC", "50400", "")),
                entry("1970-01-01+14:00", List.of("1970-01-01", "50400", "")),
                entry("10000-01-01+14:00", List.of("10000-01-01", "50400", "")),
                entry("0000-01-01-14:00", List.of("0001-01-01 BC", "-50400", "")),
                entry("0001-01-01-14:00", List.of("0001-01-01", "-50400", "")),
                entry("-0001-01-01-14:00", List.of("0002-01-01 BC", "-50400", "")),
                entry("1970-01-01-14:00", List.of("1970-01-01", "-50400", "")),
                entry("10000-01-01-14:00", List.of("10000-01-01", "-50400", "")),
                entry("5874897-12-31", List.of("5874897-12-31", noZone, "")),
                entry("-4713-11-24", List.of("4714-11-24 BC", noZone, "")),
                entry("5874897-12-31Z", List.of("5874897-12-31", "0", "")),
                entry("-4713-11-24Z", List.of("4714-11-24 BC", "0", "")),
                entry("5874897-12-31+01:00", List.of("5874897-12-31", "3600", "")),
                entry("-4713-11-24+01:00", List.of("4714-11-24 BC", "3600", "")),
                entry("5874897-12-31-01:00", List.of("5874897-12-31", "-3600", "")),
                entry("-4713-11-24-01:00", List.of("4714-11-24 BC", "-3600", "")),
                entry("5874897-12-31+14:00", List.of("5874897-12-31", "50400", "")),
                entry("-4713-11-24+14:00", List.of("4714-11-24 BC", "50400", "")),
                entry("5874897-12-31-14:00", List.of("5874897-12-31", "-50400", "")),
                entry("-4713-11-24-14:00", List.of("4714-11-24 BC", "-50400", "")),

                // non-canonical
                entry("1970-01-01+00:00", List.of("1970-01-01", "0", "1970-01-01+00:00")),
                entry("1970-01-01-00:00", List.of("1970-01-01", "0", "1970-01-01-00:00")),
                entry(" 1970-01-01Z", List.of("1970-01-01", "0", " 1970-01-01Z")),
                entry("1970-01-01Z ", List.of("1970-01-01", "0", "1970-01-01Z ")),
                entry(" 1970-01-01Z ", List.of("1970-01-01", "0", " 1970-01-01Z ")) ,

                // invalid
                entry("1970-01-01+14:01", invalid),
                entry("1970-01-01-14:01", invalid),
                entry("01970-01-01Z", invalid),
                entry("1970-00-01Z", invalid),
                entry("1970-13-01Z", invalid),
                entry("1970-01-00Z", invalid),
                entry("1970-01-32Z", invalid),
                entry("197-01-01Z", invalid),
                entry("1970-1-01Z", invalid),
                entry("1970-01-1Z", invalid),
                entry("", invalid),

                // out of range
                entry("-4714-11-23", invalid),
                entry("5874898-01-01", invalid),
                entry("-4714-11-23Z", invalid),
                entry("5874898-01-01Z", invalid),
                entry("-4714-11-23+01:00", invalid),
                entry("5874898-01-01+01:00", invalid),
                entry("-4714-11-23-01:00", invalid),
                entry("5874898-01-01-01:00", invalid),
                entry("-4714-11-23+14:00", invalid),
                entry("5874898-01-01+14:00", invalid),
                entry("-4714-11-23-14:00", invalid),
                entry("5874898-01-01-14:00", invalid)
            // @formatter:on
            )));
        }


        protected static Map<String, List<String>> filterZone(int zone, Map<String, List<String>> values)
        {
            String strZone = Integer.toString(zone);

            Map<String, List<String>> result = new HashMap<String, List<String>>();

            for(Entry<String, List<String>> e : values.entrySet())
            {
                if(e.getValue().isEmpty() || !e.getValue().get(1).equals(strZone))
                    result.put(e.getKey(), invalid);
                else
                    result.put(e.getKey(), List.of(e.getValue().get(0), e.getValue().get(2)));
            }

            return result;
        }
    }


    @Nested
    public class NoZone extends AbstractDateInZoneBaseClassTest
    {
        protected NoZone()
        {
            super(Integer.MIN_VALUE);
        }
    }


    @Nested
    public class ZZone extends AbstractDateInZoneBaseClassTest
    {
        protected ZZone()
        {
            super(0);
        }
    }


    @Nested
    public class Plus1Zone extends AbstractDateInZoneBaseClassTest
    {
        protected Plus1Zone()
        {
            super(3600);
        }
    }


    @Nested
    public class Minus1Zone extends AbstractDateInZoneBaseClassTest
    {
        protected Minus1Zone()
        {
            super(-3600);
        }
    }


    @Nested
    public class Plus14Zone extends AbstractDateInZoneBaseClassTest
    {
        protected Plus14Zone()
        {
            super(50400);
        }
    }


    @Nested
    public class Minus14Zone extends AbstractDateInZoneBaseClassTest
    {
        protected Minus14Zone()
        {
            super(-50400);
        }
    }
}
