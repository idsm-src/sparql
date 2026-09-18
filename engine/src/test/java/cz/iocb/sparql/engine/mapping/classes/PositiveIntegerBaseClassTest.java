package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class PositiveIntegerBaseClassTest extends AbstractResourceClassTest
{
    protected PositiveIntegerBaseClassTest()
    {
        final String maxDec = "9".repeat(131072);

        super(new PositiveIntegerBaseClass(), List.of("numeric", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("1", List.of("1", "")),
            entry(maxDec, List.of(maxDec, "")),

            // non-canonical
            entry(" 1", List.of("1", " 1")),
            entry("1 ", List.of("1", "1 ")),
            entry(" 1 ", List.of("1", " 1 ")),
            entry("+1", List.of("1", "+1")),
            entry("01", List.of("1", "01")),
            entry("0" + maxDec, List.of(maxDec, "0" + maxDec)),

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
