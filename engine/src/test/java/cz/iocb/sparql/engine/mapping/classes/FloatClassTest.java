package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class FloatClassTest extends AbstractResourceClassTest
{
    protected FloatClassTest()
    {
        super(new FloatClass(), List.of("float4"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("NaN", List.of("NaN")),
            entry("INF", List.of("INF")),
            entry("-INF", List.of("-INF")),
            entry("0.0E0", List.of("0.0E0")),
            entry("-0.0E0", List.of("-0.0E0")),
            entry("1.0E0", List.of("1.0E0")),
            entry("-1.0E0", List.of("-1.0E0")),
            entry("1.4E-45", List.of("1.4E-45")),
            entry("-1.4E-45", List.of("-1.4E-45")),
            entry("3.4028235E38", List.of("3.4028235E38")),
            entry("-3.4028235E38", List.of("-3.4028235E38")),

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
            entry("7.00649232162E-46", invalid),
            entry("-7.00649232162E-46", invalid),
            entry("3.40282356780E38", invalid),
            entry("-3.40282356780E38", invalid),

            // invalid
            entry(".", invalid),
            entry("", invalid)
        // @formatter:on
        ));
    }
}
