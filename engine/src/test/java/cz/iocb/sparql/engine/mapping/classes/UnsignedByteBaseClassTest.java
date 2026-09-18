package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class UnsignedByteBaseClassTest extends AbstractResourceClassTest
{
    protected UnsignedByteBaseClassTest()
    {
        super(new UnsignedByteBaseClass(), List.of("int2", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0", List.of("0", "")),
            entry("1", List.of("1", "")),
            entry("255", List.of("255", "")),

            // non-canonical
            entry(" 0", List.of("0", " 0")),
            entry("0 ", List.of("0", "0 ")),
            entry(" 0 ", List.of("0", " 0 ")),
            entry("+0", List.of("0", "+0")),
            entry("-0", List.of("0", "-0")),
            entry("+1", List.of("1", "+1")),
            entry("01", List.of("1", "01")),

            // invalid
            entry("1.0", invalid),
            entry(".0", invalid),
            entry("0.", invalid),
            entry(".", invalid),
            entry("", invalid),
            entry("-01", invalid),

            // out of range
            entry("256", invalid),
            entry("-1", invalid)
        // @formatter:on
        ));
    }
}
