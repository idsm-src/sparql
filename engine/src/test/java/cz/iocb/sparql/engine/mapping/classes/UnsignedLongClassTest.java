package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class UnsignedLongClassTest extends AbstractResourceClassTest
{
    protected UnsignedLongClassTest()
    {
        super(new UnsignedLongClass(), List.of("numeric"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0", List.of("0")),
            entry("1", List.of("1")),
            entry("18446744073709551615", List.of("18446744073709551615")),

            // non-canonical
            entry(" 0", invalid),
            entry("0 ", invalid),
            entry(" 0 ", invalid),
            entry("+0", invalid),
            entry("-0", invalid),
            entry("+1", invalid),
            entry("01", invalid),

            // invalid
            entry("1.0", invalid),
            entry(".0", invalid),
            entry("0.", invalid),
            entry(".", invalid),
            entry("", invalid),
            entry("-01", invalid),

            // out of range
            entry("18446744073709551616", invalid),
            entry("-1", invalid)
        // @formatter:on
        ));
    }
}
