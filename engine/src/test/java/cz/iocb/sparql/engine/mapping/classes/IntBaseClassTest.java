package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class IntBaseClassTest extends AbstractResourceClassTest
{
    protected IntBaseClassTest()
    {
        super(new IntBaseClass(), List.of("int4", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0", List.of("0", "")),
            entry("1", List.of("1", "")),
            entry("-1", List.of("-1", "")),
            entry("2147483647", List.of("2147483647", "")),
            entry("-2147483648", List.of("-2147483648", "")),

            // non-canonical
            entry(" 0", List.of("0", " 0")),
            entry("0 ", List.of("0", "0 ")),
            entry(" 0 ", List.of("0", " 0 ")),
            entry("+0", List.of("0", "+0")),
            entry("-0", List.of("0", "-0")),
            entry("+1", List.of("1", "+1")),

            // invalid
            entry("1.0", invalid),
            entry(".0", invalid),
            entry("0.", invalid),
            entry(".", invalid),
            entry("", invalid),

            // out of range
            entry("2147483648", invalid),
            entry("-2147483649", invalid)
        // @formatter:on
        ));
    }
}
