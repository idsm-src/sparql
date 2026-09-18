package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class PositiveIntegerClassTest extends AbstractResourceClassTest
{
    protected PositiveIntegerClassTest()
    {
        final String maxDec = "9".repeat(131072);

        super(new PositiveIntegerClass(), List.of("numeric"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("1", List.of("1")),
            entry(maxDec, List.of(maxDec)),

            // non-canonical
            entry(" 1", invalid),
            entry("1 ", invalid),
            entry(" 1 ", invalid),
            entry("+1", invalid),
            entry("01", invalid),
            entry("0" + maxDec, invalid),

            // invalid
            entry("1.0", invalid),
            entry(".0", invalid),
            entry("0.", invalid),
            entry(".", invalid),
            entry("", invalid),
            entry("0", invalid),
            entry("+0", invalid),
            entry("-0", invalid),
            entry("-1", invalid),
            entry("-01", invalid),

            // out of range
            entry("9" + maxDec, invalid)
        // @formatter:on
        ));
    }
}
