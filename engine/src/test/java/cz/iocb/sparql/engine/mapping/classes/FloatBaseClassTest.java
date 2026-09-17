package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class FloatBaseClassTest extends AbstractResourceClassTest
{
    protected FloatBaseClassTest()
    {
        super(new FloatBaseClass(), List.of("float4", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("NaN", List.of("NaN", "")),
            entry("INF", List.of("INF", "")),
            entry("-INF", List.of("-INF", "")),
            entry("0.0E0", List.of("0.0E0", "")),
            entry("-0.0E0", List.of("-0.0E0", "")),
            entry("1.0E0", List.of("1.0E0", "")),
            entry("-1.0E0", List.of("-1.0E0", "")),
            entry("1.4E-45", List.of("1.4E-45", "")),
            entry("-1.4E-45", List.of("-1.4E-45", "")),
            entry("3.4028235E38", List.of("3.4028235E38", "")),
            entry("-3.4028235E38", List.of("-3.4028235E38", "")),

            // non-canonical
            entry("0.0e0", List.of("0.0E0", "0.0e0")),
            entry("-0.0e0", List.of("-0.0E0", "-0.0e0")),
            entry("1.0e0", List.of("1.0E0", "1.0e0")),
            entry("-1.0e0", List.of("-1.0E0", "-1.0e0")),
            entry(" NaN", List.of("NaN", " NaN")),
            entry("NaN ", List.of("NaN", "NaN ")),
            entry(" NaN ", List.of("NaN", " NaN ")),
            entry(" INF", List.of("INF", " INF")),
            entry("INF ", List.of("INF", "INF ")),
            entry(" INF ", List.of("INF", " INF ")),
            entry(" -INF", List.of("-INF", " -INF")),
            entry("-INF ", List.of("-INF", "-INF ")),
            entry(" -INF ", List.of("-INF", " -INF ")),
            entry(" 0.0E0", List.of("0.0E0", " 0.0E0")),
            entry("0.0E0 ", List.of("0.0E0", "0.0E0 ")),
            entry(" 0.0E0 ", List.of("0.0E0", " 0.0E0 ")),
            entry(" -0.0E0", List.of("-0.0E0", " -0.0E0")),
            entry("-0.0E0 ", List.of("-0.0E0", "-0.0E0 ")),
            entry(" -0.0E0 ", List.of("-0.0E0", " -0.0E0 ")),
            entry(" 1.0E0", List.of("1.0E0", " 1.0E0")),
            entry("1.0E0 ", List.of("1.0E0", "1.0E0 ")),
            entry(" 1.0E0 ", List.of("1.0E0", " 1.0E0 ")),
            entry(" -1.0E0", List.of("-1.0E0", " -1.0E0")),
            entry("-1.0E0 ", List.of("-1.0E0", "-1.0E0 ")),
            entry(" -1.0E0 ", List.of("-1.0E0", " -1.0E0 ")),
            entry("0", List.of("0.0E0", "0")),
            entry("-0", List.of("-0.0E0", "-0")),
            entry("10", List.of("1.0E1", "10")),
            entry("-10", List.of("-1.0E1", "-10")),
            entry("0.1", List.of("1.0E-1", "0.1")),
            entry("-0.1", List.of("-1.0E-1", "-0.1")),
            entry("7.00649232162E-46", List.of("0.0E0", "7.00649232162E-46")),
            entry("-7.00649232162E-46", List.of("-0.0E0", "-7.00649232162E-46")),
            entry("3.40282356780E38", List.of("INF", "3.40282356780E38")),
            entry("-3.40282356780E38", List.of("-INF", "-3.40282356780E38")),

            // invalid
            entry(".", invalid),
            entry("", invalid)
        // @formatter:on
        ));
    }
}
