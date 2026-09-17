package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class BooleanClassTest extends AbstractResourceClassTest
{
    protected BooleanClassTest()
    {
        super(new BooleanClass(), List.of("bool"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("false", List.of("false")),
            entry("true", List.of("true")),

            // non-canonical
            entry("0", invalid),
            entry("1", invalid),
            entry(" 0", invalid),
            entry(" 1", invalid),
            entry("0 ", invalid),
            entry("1 ", invalid),
            entry(" 0 ", invalid),
            entry(" 1 ", invalid),
            entry(" false", invalid),
            entry(" true", invalid),
            entry("false ", invalid),
            entry("true ", invalid),
            entry(" false ", invalid),
            entry(" true ", invalid),

            // invalid
            entry("False", invalid),
            entry("True", invalid),
            entry("0.0", invalid),
            entry("1.0", invalid),
            entry("", invalid)
        // @formatter:on
        ));
    }
}
