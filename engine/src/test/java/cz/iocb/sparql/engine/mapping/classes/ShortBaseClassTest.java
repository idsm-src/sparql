package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class ShortBaseClassTest extends AbstractResourceClassTest
{
    protected ShortBaseClassTest()
    {
        super(new ShortBaseClass(), List.of("int2", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0", List.of("0", "")),
            entry("1", List.of("1", "")),
            entry("-1", List.of("-1", "")),
            entry("32767", List.of("32767", "")),
            entry("-32768", List.of("-32768", "")),

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
            entry("32768", invalid),
            entry("-32769", invalid)
        // @formatter:on
        ));
    }
}
