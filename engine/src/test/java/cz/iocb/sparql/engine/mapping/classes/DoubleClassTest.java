package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class DoubleClassTest extends AbstractResourceClassTest
{
    protected DoubleClassTest()
    {
        super(new DoubleClass(), List.of("float8"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("NaN", List.of("NaN")),
            entry("INF", List.of("INF")),
            entry("-INF", List.of("-INF")),
            entry("0.0E0", List.of("0.0E0")),
            entry("-0.0E0", List.of("-0.0E0")),
            entry("1.0E0", List.of("1.0E0")),
            entry("-1.0E0", List.of("-1.0E0")),
            entry("4.9E-324", List.of("4.9E-324")),
            entry("-4.9E-324", List.of("-4.9E-324")),
            entry("1.7976931348623157E308", List.of("1.7976931348623157E308")),
            entry("-1.7976931348623157E308", List.of("-1.7976931348623157E308")),

            // non-canonical
            entry("0.0e0", invalid),
            entry("-0.0e0", invalid),
            entry("1.0e0", invalid),
            entry("-1.0e0", invalid),
            entry(" NaN", invalid),
            entry("NaN ", invalid),
            entry(" NaN ", invalid),
            entry(" INF", invalid),
            entry("INF ", invalid),
            entry(" INF ", invalid),
            entry(" -INF", invalid),
            entry("-INF ", invalid),
            entry(" -INF ", invalid),
            entry(" 0.0E0", invalid),
            entry("0.0E0 ", invalid),
            entry(" 0.0E0 ", invalid),
            entry(" -0.0E0", invalid),
            entry("-0.0E0 ", invalid),
            entry(" -0.0E0 ", invalid),
            entry(" 1.0E0", invalid),
            entry("1.0E0 ", invalid),
            entry(" 1.0E0 ", invalid),
            entry(" -1.0E0", invalid),
            entry("-1.0E0 ", invalid),
            entry(" -1.0E0 ", invalid),
            entry("0", invalid),
            entry("-0", invalid),
            entry("10", invalid),
            entry("-10", invalid),
            entry("0.1", invalid),
            entry("-0.1", invalid),
            entry("2.4703282292062327E-324", invalid),
            entry("-2.4703282292062327E-324", invalid),
            entry("1.7976931348623159E308", invalid),
            entry("-1.7976931348623159E308", invalid),

            // invalid
            entry(".", invalid),
            entry("", invalid)
        // @formatter:on
        ));
    }
}
