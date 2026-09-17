package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class DateScalarClassTest extends AbstractResourceClassTest
{
    protected DateScalarClassTest()
    {
        super(new DateScalarClass(), List.of("sparql.zoneddate"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0000-01-01", List.of("0000-01-01")),
            entry("0001-01-01", List.of("0001-01-01")),
            entry("-0001-01-01", List.of("-0001-01-01")),
            entry("1970-01-01", List.of("1970-01-01")),
            entry("10000-01-01", List.of("10000-01-01")),
            entry("0000-01-01Z", List.of("0000-01-01Z")),
            entry("0001-01-01Z", List.of("0001-01-01Z")),
            entry("-0001-01-01Z", List.of("-0001-01-01Z")),
            entry("1970-01-01Z", List.of("1970-01-01Z")),
            entry("10000-01-01Z", List.of("10000-01-01Z")),
            entry("0000-01-01+01:00", List.of("0000-01-01+01:00")),
            entry("0001-01-01+01:00", List.of("0001-01-01+01:00")),
            entry("-0001-01-01+01:00", List.of("-0001-01-01+01:00")),
            entry("1970-01-01+01:00", List.of("1970-01-01+01:00")),
            entry("10000-01-01+01:00", List.of("10000-01-01+01:00")),
            entry("0000-01-01-01:00", List.of("0000-01-01-01:00")),
            entry("0001-01-01-01:00", List.of("0001-01-01-01:00")),
            entry("-0001-01-01-01:00", List.of("-0001-01-01-01:00")),
            entry("1970-01-01-01:00", List.of("1970-01-01-01:00")),
            entry("10000-01-01-01:00", List.of("10000-01-01-01:00")),
            entry("0000-01-01+14:00", List.of("0000-01-01+14:00")),
            entry("0001-01-01+14:00", List.of("0001-01-01+14:00")),
            entry("-0001-01-01+14:00", List.of("-0001-01-01+14:00")),
            entry("1970-01-01+14:00", List.of("1970-01-01+14:00")),
            entry("10000-01-01+14:00", List.of("10000-01-01+14:00")),
            entry("0000-01-01-14:00", List.of("0000-01-01-14:00")),
            entry("0001-01-01-14:00", List.of("0001-01-01-14:00")),
            entry("-0001-01-01-14:00", List.of("-0001-01-01-14:00")),
            entry("1970-01-01-14:00", List.of("1970-01-01-14:00")),
            entry("10000-01-01-14:00", List.of("10000-01-01-14:00")),
            entry("5874897-12-31", List.of("5874897-12-31")),
            entry("-4713-11-24", List.of("-4713-11-24")),
            entry("5874897-12-31Z", List.of("5874897-12-31Z")),
            entry("-4713-11-24Z", List.of("-4713-11-24Z")),
            entry("5874897-12-31+01:00", List.of("5874897-12-31+01:00")),
            entry("-4713-11-24+01:00", List.of("-4713-11-24+01:00")),
            entry("5874897-12-31-01:00", List.of("5874897-12-31-01:00")),
            entry("-4713-11-24-01:00", List.of("-4713-11-24-01:00")),
            entry("5874897-12-31+14:00", List.of("5874897-12-31+14:00")),
            entry("-4713-11-24+14:00", List.of("-4713-11-24+14:00")),
            entry("5874897-12-31-14:00", List.of("5874897-12-31-14:00")),
            entry("-4713-11-24-14:00", List.of("-4713-11-24-14:00")),

            // non-canonical
            entry("1970-01-01+00:00", invalid),
            entry("1970-01-01-00:00", invalid),
            entry(" 1970-01-01Z", invalid),
            entry("1970-01-01Z ", invalid),
            entry(" 1970-01-01Z ", invalid) ,

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
        ));
    }
}
