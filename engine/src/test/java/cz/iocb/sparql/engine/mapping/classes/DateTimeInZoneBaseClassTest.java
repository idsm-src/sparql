package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Nested;



public class DateTimeInZoneBaseClassTest
{
    public static abstract class AbstractDateTimeInZoneBaseClassTest extends AbstractResourceClassTest
    {
        protected AbstractDateTimeInZoneBaseClassTest(int zone)
        {
            String noZone = Integer.toString(Integer.MIN_VALUE);

            super(DateTimeInZoneBaseClass.get(zone), List.of("timestamptz", "varchar"), filterZone(zone, Map.ofEntries(
            // @formatter:off
                // canonical
                entry("0000-01-01T00:00:00", List.of("0001-01-01T00:00:00Z BC", noZone, "")),
                entry("0001-01-01T00:00:00", List.of("0001-01-01T00:00:00Z", noZone, "")),
                entry("-0001-01-01T00:00:00", List.of("0002-01-01T00:00:00Z BC", noZone, "")),
                entry("1970-01-01T00:00:00", List.of("1970-01-01T00:00:00Z", noZone, "")),
                entry("10000-01-01T00:00:00", List.of("10000-01-01T00:00:00Z", noZone, "")),
                entry("0000-01-01T00:00:00Z", List.of("0001-01-01T00:00:00Z BC", "0", "")),
                entry("0001-01-01T00:00:00Z", List.of("0001-01-01T00:00:00Z", "0", "")),
                entry("-0001-01-01T00:00:00Z", List.of("0002-01-01T00:00:00Z BC", "0", "")),
                entry("1970-01-01T00:00:00Z", List.of("1970-01-01T00:00:00Z", "0", "")),
                entry("1970-01-01T00:00:00.1234560Z", List.of("1970-01-01T00:00:00.123456Z", "0", "1970-01-01T00:00:00.1234560Z")),
                entry("10000-01-01T00:00:00Z", List.of("10000-01-01T00:00:00Z", "0", "")),
                entry("0000-01-01T01:00:00+01:00", List.of("0001-01-01T00:00:00Z BC", "3600", "")),
                entry("0001-01-01T01:00:00+01:00", List.of("0001-01-01T00:00:00Z", "3600", "")),
                entry("-0001-01-01T01:00:00+01:00", List.of("0002-01-01T00:00:00Z BC", "3600", "")),
                entry("1970-01-01T01:00:00+01:00", List.of("1970-01-01T00:00:00Z", "3600", "")),
                entry("10000-01-01T01:00:00+01:00", List.of("10000-01-01T00:00:00Z", "3600", "")),
                entry("-0001-12-31T23:00:00-01:00", List.of("0001-01-01T00:00:00Z BC", "-3600", "")),
                entry("0000-12-31T23:00:00-01:00", List.of("0001-01-01T00:00:00Z", "-3600", "")),
                entry("-0002-12-31T23:00:00-01:00", List.of("0002-01-01T00:00:00Z BC", "-3600", "")),
                entry("1969-12-31T23:00:00-01:00", List.of("1970-01-01T00:00:00Z", "-3600", "")),
                entry("9999-12-31T23:00:00-01:00", List.of("10000-01-01T00:00:00Z", "-3600", "")),
                entry("0000-01-01T14:00:00+14:00", List.of("0001-01-01T00:00:00Z BC", "50400", "")),
                entry("0001-01-01T14:00:00+14:00", List.of("0001-01-01T00:00:00Z", "50400", "")),
                entry("-0001-01-01T14:00:00+14:00", List.of("0002-01-01T00:00:00Z BC", "50400", "")),
                entry("1970-01-01T14:00:00+14:00", List.of("1970-01-01T00:00:00Z", "50400", "")),
                entry("10000-01-01T14:00:00+14:00", List.of("10000-01-01T00:00:00Z", "50400", "")),
                entry("-0001-12-31T10:00:00-14:00", List.of("0001-01-01T00:00:00Z BC", "-50400", "")),
                entry("0000-12-31T10:00:00-14:00", List.of("0001-01-01T00:00:00Z", "-50400", "")),
                entry("-0002-12-31T10:00:00-14:00", List.of("0002-01-01T00:00:00Z BC", "-50400", "")),
                entry("1969-12-31T10:00:00-14:00", List.of("1970-01-01T00:00:00Z", "-50400", "")),
                entry("9999-12-31T10:00:00-14:00", List.of("10000-01-01T00:00:00Z", "-50400", "")),
                entry("294276-12-31T23:59:59.999999", List.of("294276-12-31T23:59:59.999999Z", noZone, "")),
                entry("-4713-11-24T00:00:00", List.of("4714-11-24T00:00:00Z BC", noZone, "")),
                entry("294276-12-31T23:59:59.999999Z", List.of("294276-12-31T23:59:59.999999Z", "0", "")),
                entry("-4713-11-24T00:00:00Z", List.of("4714-11-24T00:00:00Z BC", "0", "")),
                entry("294277-01-01T00:59:59.999999+01:00", List.of("294276-12-31T23:59:59.999999Z", "3600", "")),
                entry("-4713-11-24T01:00:00+01:00", List.of("4714-11-24T00:00:00Z BC", "3600", "")),
                entry("294276-12-31T22:59:59.999999-01:00", List.of("294276-12-31T23:59:59.999999Z", "-3600", "")),
                entry("-4713-11-23T23:00:00-01:00", List.of("4714-11-24T00:00:00Z BC", "-3600", "")),
                entry("294277-01-01T13:59:59.999999+14:00", List.of("294276-12-31T23:59:59.999999Z", "50400", "")),
                entry("-4713-11-24T14:00:00+14:00", List.of("4714-11-24T00:00:00Z BC", "50400", "")),
                entry("294276-12-31T09:59:59.999999-14:00", List.of("294276-12-31T23:59:59.999999Z", "-50400", "")),
                entry("-4713-11-23T10:00:00-14:00", List.of("4714-11-24T00:00:00Z BC", "-50400", "")),

                // non-canonical
                entry("1970-01-01T24:00:00+00:00", List.of("1970-01-02T00:00:00Z", "0", "1970-01-01T24:00:00+00:00")),
                entry("1970-01-01T24:00:00-00:00", List.of("1970-01-02T00:00:00Z", "0", "1970-01-01T24:00:00-00:00")),
                entry("1970-01-01T24:00:00Z", List.of("1970-01-02T00:00:00Z", "0", "1970-01-01T24:00:00Z")),
                entry(" 1970-01-01T00:00:00Z", List.of("1970-01-01T00:00:00Z", "0", " 1970-01-01T00:00:00Z")),
                entry("1970-01-01T00:00:00Z ", List.of("1970-01-01T00:00:00Z", "0", "1970-01-01T00:00:00Z ")),
                entry(" 1970-01-01T00:00:00Z ", List.of("1970-01-01T00:00:00Z", "0", " 1970-01-01T00:00:00Z ")),

                // invalid
                entry("1970-01-01T00:00:00+14:01", invalid),
                entry("1970-01-01T00:00:00-14:01", invalid),
                entry("01970-01-01T00:00:00Z", invalid),
                entry("1970-00-01T00:00:00Z", invalid),
                entry("1970-13-01T00:00:00Z", invalid),
                entry("1970-01-00T00:00:00Z", invalid),
                entry("1970-01-32T00:00:00Z", invalid),
                entry("1970-01-01T00:60:00Z", invalid),
                entry("1970-01-01T00:00:60Z", invalid),
                entry("197-01-01T10:10:10Z", invalid),
                entry("1970-1-01T10:10:10Z", invalid),
                entry("1970-01-1T10:10:10Z", invalid),
                entry("1970-01-01T1:10:10Z", invalid),
                entry("1970-01-01T10:1:10Z", invalid),
                entry("1970-01-01T10:10:1Z", invalid),
                entry("1970-01-01T10:10:10.Z", invalid),
                entry("1970-01-01T10:10Z", invalid),
                entry("1970-01-01Z", invalid),
                entry("", invalid),

                // out of range
                entry("294276-12-31T24:00:00", invalid),
                entry("-4713-11-23T23:59:59.999999", invalid),
                entry("294276-12-31T24:00:00Z", invalid),
                entry("-4713-11-23T23:59:59.999999Z", invalid),
                entry("294277-01-01T01:00:00+01:00", invalid),
                entry("-4713-11-24T00:59:59.999999+01:00", invalid),
                entry("294276-12-31T23:00:00-01:00", invalid),
                entry("-4713-11-23T22:59:59.999999-01:00", invalid),
                entry("294277-01-01T14:00:00+14:00", invalid),
                entry("-4713-11-24T13:59:59.999999+14:00", invalid),
                entry("294276-12-31T10:00:00-14:00", invalid),
                entry("-4713-11-23T09:59:59.999999-14:00", invalid),
                entry("1970-01-01T00:00:00.0000001Z", invalid)
            // @formatter:on
            )));
        }


        protected static Map<String, List<String>> filterZone(int zone, Map<String, List<String>> values)
        {
            String strZone = Integer.toString(zone);

            Map<String, List<String>> result = new HashMap<>();

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
    public class NoZone extends AbstractDateTimeInZoneBaseClassTest
    {
        protected NoZone()
        {
            super(Integer.MIN_VALUE);
        }
    }


    @Nested
    public class ZZone extends AbstractDateTimeInZoneBaseClassTest
    {
        protected ZZone()
        {
            super(0);
        }
    }


    @Nested
    public class Plus1Zone extends AbstractDateTimeInZoneBaseClassTest
    {
        protected Plus1Zone()
        {
            super(3600);
        }
    }


    @Nested
    public class Minus1Zone extends AbstractDateTimeInZoneBaseClassTest
    {
        protected Minus1Zone()
        {
            super(-3600);
        }
    }


    @Nested
    public class Plus14Zone extends AbstractDateTimeInZoneBaseClassTest
    {
        protected Plus14Zone()
        {
            super(50400);
        }
    }


    @Nested
    public class Minus14Zone extends AbstractDateTimeInZoneBaseClassTest
    {
        protected Minus14Zone()
        {
            super(-50400);
        }
    }
}
