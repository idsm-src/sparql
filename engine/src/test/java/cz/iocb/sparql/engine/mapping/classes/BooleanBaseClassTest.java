package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class BooleanBaseClassTest extends AbstractResourceClassTest
{
    protected BooleanBaseClassTest()
    {
        super(new BooleanBaseClass(), List.of("bool", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("false", List.of("false", "")),
            entry("true", List.of("true", "")),

            // non-canonical
            entry("0", List.of("false", "0")),
            entry("1", List.of("true", "1")),
            entry(" 0", List.of("false", " 0")),
            entry(" 1", List.of("true", " 1")),
            entry("0 ", List.of("false", "0 ")),
            entry("1 ", List.of("true", "1 ")),
            entry(" 0 ", List.of("false", " 0 ")),
            entry(" 1 ", List.of("true", " 1 ")),
            entry(" false", List.of("false", " false")),
            entry(" true", List.of("true", " true")),
            entry("false ", List.of("false", "false ")),
            entry("true ", List.of("true", "true ")),
            entry(" false ", List.of("false", " false ")),
            entry(" true ", List.of("true", " true ")),

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
