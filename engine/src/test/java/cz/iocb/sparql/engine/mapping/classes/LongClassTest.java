package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class LongClassTest extends AbstractResourceClassTest
{
    protected LongClassTest()
    {
        super(new LongClass(), List.of("int8"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0", List.of("0")),
            entry("1", List.of("1")),
            entry("-1", List.of("-1")),
            entry("9223372036854775807", List.of("9223372036854775807")),
            entry("-9223372036854775808", List.of("-9223372036854775808")),

            // non-canonical
            entry(" 0", invalid),
            entry("0 ", invalid),
            entry(" 0 ", invalid),
            entry("+0", invalid),
            entry("-0", invalid),
            entry("+1", invalid),

            // invalid
            entry("1.0", invalid),
            entry(".0", invalid),
            entry("0.", invalid),
            entry(".", invalid),
            entry("", invalid),

            // out of range
            entry("9223372036854775808", invalid),
            entry("-9223372036854775809", invalid)
        // @formatter:on
        ));
    }
}
