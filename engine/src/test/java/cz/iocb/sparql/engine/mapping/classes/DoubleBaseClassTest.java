package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class DoubleBaseClassTest extends AbstractResourceClassTest
{
    protected DoubleBaseClassTest()
    {
        super(new DoubleBaseClass(), List.of("float8", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("NaN", List.of("NaN", "")),
            entry("INF", List.of("INF", "")),
            entry("-INF", List.of("-INF", "")),
            entry("0.0E0", List.of("0.0E0", "")),
            entry("-0.0E0", List.of("-0.0E0", "")),
            entry("1.0E0", List.of("1.0E0", "")),
            entry("-1.0E0", List.of("-1.0E0", "")),
            entry("4.9E-324", List.of("4.9E-324", "")),
            entry("-4.9E-324", List.of("-4.9E-324", "")),
            entry("1.7976931348623157E308", List.of("1.7976931348623157E308", "")),
            entry("-1.7976931348623157E308", List.of("-1.7976931348623157E308", "")),

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
            entry("2.4703282292062327E-324", List.of("0.0E0", "2.4703282292062327E-324")),
            entry("-2.4703282292062327E-324", List.of("-0.0E0", "-2.4703282292062327E-324")),
            entry("1.7976931348623159E308", List.of("INF", "1.7976931348623159E308")),
            entry("-1.7976931348623159E308", List.of("-INF", "-1.7976931348623159E308")),

            // invalid
            entry(".", invalid),
            entry("", invalid)
        // @formatter:on
        ));
    }
}
