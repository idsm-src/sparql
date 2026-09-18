package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class UnsignedIntClassTest extends AbstractResourceClassTest
{
    protected UnsignedIntClassTest()
    {
        super(new UnsignedIntClass(), List.of("int8"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0", List.of("0")),
            entry("1", List.of("1")),
            entry("4294967295", List.of("4294967295")),

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
            entry("4294967296", invalid),
            entry("-1", invalid)
        // @formatter:on
        ));
    }
}
