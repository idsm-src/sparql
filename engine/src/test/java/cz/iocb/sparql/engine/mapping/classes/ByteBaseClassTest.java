package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class ByteBaseClassTest extends AbstractResourceClassTest
{
    protected ByteBaseClassTest()
    {
        super(new ByteBaseClass(), List.of("int2", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0", List.of("0", "")),
            entry("1", List.of("1", "")),
            entry("-1", List.of("-1", "")),
            entry("127", List.of("127", "")),
            entry("-128", List.of("-128", "")),

            // non-canonical
            entry(" 0", List.of("0", " 0")),
            entry("0 ", List.of("0", "0 ")),
            entry(" 0 ", List.of("0", " 0 ")),
            entry("+0", List.of("0", "+0")),
            entry("-0", List.of("0", "-0")),
            entry("+1", List.of("1", "+1")),
            entry("01", List.of("1", "01")),
            entry("-01", List.of("-1", "-01")),

            // invalid
            entry("1.0", invalid),
            entry(".0", invalid),
            entry("0.", invalid),
            entry(".", invalid),
            entry("", invalid),

            // out of range
            entry("128", invalid),
            entry("-129", invalid)
        // @formatter:on
        ));
    }
}
