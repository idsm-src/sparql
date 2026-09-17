package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;



public class DecimalBaseClassTest extends AbstractResourceClassTest
{
    protected DecimalBaseClassTest()
    {
        final String maxDec = "9".repeat(131072);
        final String maxFrac = "." + "9".repeat(16383);

        super(new DecimalBaseClass(), List.of("numeric", "varchar"), Map.ofEntries(
        // @formatter:off
            // canonical
            entry("0.0", List.of("0.0", "")),
            entry("1.0", List.of("1.0", "")),
            entry("-1.0", List.of("-1.0", "")),
            entry(maxDec + ".0", List.of(maxDec + ".0", "")),
            entry("-" + maxDec + ".0", List.of("-" + maxDec + ".0", "")),
            entry("0" + maxFrac, List.of("0" + maxFrac, "")),
            entry("-0" + maxFrac, List.of("-0" + maxFrac, "")),
            entry(maxDec + maxFrac, List.of(maxDec + maxFrac, "")),
            entry("-" + maxDec + maxFrac, List.of("-" + maxDec + maxFrac, "")),

            // non-canonical
            entry(" 0", List.of("0.0", " 0")),
            entry("0 ", List.of("0.0", "0 ")),
            entry(" 0 ", List.of("0.0", " 0 ")),
            entry("+0", List.of("0.0", "+0")),
            entry("-0", List.of("0.0", "-0")),
            entry("+1", List.of("1.0", "+1")),
            entry(" 0.0", List.of("0.0", " 0.0")),
            entry("0.0 ", List.of("0.0", "0.0 ")),
            entry(" 0.0 ", List.of("0.0", " 0.0 ")),
            entry("+0.0", List.of("0.0", "+0.0")),
            entry("-0.0", List.of("0.0", "-0.0")),
            entry("+1.0", List.of("1.0", "+1.0")),
            entry(" 0.00", List.of("0.0", " 0.00")),
            entry("0.00 ", List.of("0.0", "0.00 ")),
            entry(" 0.00 ", List.of("0.0", " 0.00 ")),
            entry("+0.00", List.of("0.0", "+0.00")),
            entry("-0.00", List.of("0.0", "-0.00")),
            entry("+1.00", List.of("1.0", "+1.00")),
            entry("0" + maxDec, List.of(maxDec + ".0", "0" + maxDec)),
            entry("-0" + maxDec, List.of("-" + maxDec + ".0", "-0" + maxDec)),
            entry("0" + maxDec + ".0", List.of(maxDec + ".0", "0" + maxDec + ".0")),
            entry("-0" + maxDec + ".0", List.of("-" + maxDec + ".0", "-0" + maxDec + ".0")),
            entry("0" + maxFrac + "0", List.of("0" + maxFrac, "0" + maxFrac + "0")),
            entry("-0" + maxFrac + "0", List.of("-0" + maxFrac, "-0" + maxFrac + "0")),
            entry("0" + maxDec + maxFrac + "0", List.of(maxDec + maxFrac, "0" + maxDec + maxFrac + "0")),
            entry("-0" + maxDec + maxFrac + "0", List.of("-" + maxDec + maxFrac, "-0" + maxDec + maxFrac + "0")),
            entry(".0", List.of("0.0", ".0")),
            entry("0.", List.of("0.0", "0.")),

            // invalid
            entry(".", invalid),
            entry("", invalid),

            // out of range
            entry("9" + maxDec + ".0", invalid),
            entry("-9" + maxDec + ".0", invalid),
            entry("0" + maxFrac + "9", invalid),
            entry("-0" + maxFrac + "9", invalid),
            entry("9" + maxDec + maxFrac + "9", invalid),
            entry("-9" + maxDec + maxFrac + "9", invalid)
        // @formatter:on
        ));
    }
}
