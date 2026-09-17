package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class DayTimeDurationBaseClassTest extends AbstractResourceClassTest
{
    protected DayTimeDurationBaseClassTest()
    {
        super(new DayTimeDurationBaseClass(), List.of("int8", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("PT0S", List.of("0", "")),
            entry("PT1S", List.of("1000000", "")),
            entry("PT59S", List.of("59000000", "")),
            entry("PT1M", List.of("60000000", "")),
            entry("PT59M", List.of("3540000000", "")),
            entry("PT1H", List.of("3600000000", "")),
            entry("PT23H", List.of("82800000000", "")),
            entry("P1D", List.of("86400000000", "")),
            entry("PT0.000001S", List.of("1", "")),
            entry("P1DT1H1M1S", List.of("90061000000", "")),
            entry("P1DT1H1M1.1S", List.of("90061100000", "")),
            entry("P1DT1H1M1.000001S", List.of("90061000001", "")),
            entry("P106751991DT4H54.775807S", List.of("9223372036854775807", "")),
            entry("-P106751991DT4H54.775808S", List.of("-9223372036854775808", "")),

            // non-canonical
            entry(" PT0S", List.of("0", " PT0S")),
            entry("PT0S ", List.of("0", "PT0S ")),
            entry(" PT0S ", List.of("0", " PT0S ")),
            entry("-PT0S", List.of("0", "-PT0S")),
            entry(" -PT0S", List.of("0", " -PT0S")),
            entry("-PT0S ", List.of("0", "-PT0S ")),
            entry(" -PT0S ", List.of("0", " -PT0S ")),
            entry(" PT1S", List.of("1000000", " PT1S")),
            entry("PT1S ", List.of("1000000", "PT1S ")),
            entry(" PT1S ", List.of("1000000", " PT1S ")),
            entry(" -PT1S", List.of("-1000000", " -PT1S")),
            entry("-PT1S ", List.of("-1000000", "-PT1S ")),
            entry(" -PT1S ", List.of("-1000000", " -PT1S ")),
            entry("PT60S", List.of("60000000", "PT60S")),
            entry("PT60M", List.of("3600000000", "PT60M")),
            entry("PT24H", List.of("86400000000", "PT24H")),
            entry("PT0.0000010S", List.of("1", "PT0.0000010S")),

            // invalid
            entry("", invalid),
            entry("-", invalid),
            entry("P", invalid),
            entry("T", invalid),
            entry("PT", invalid),
            entry("P1DT", invalid),
            entry("PD", invalid),
            entry("PTS", invalid),
            entry("PTM", invalid),
            entry("PTH", invalid),
            entry("--PT0S", invalid),
            entry("--PT1S", invalid),
            entry("- PT1S", invalid),

            // out of range
            entry("PT0.0000001S", invalid),
            entry("P106751991DT4H54.775808S", invalid),
            entry("-P106751991DT4H54.775809S", invalid)
        // @formatter:on
        ));
    }
}
