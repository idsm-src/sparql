package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Random;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;



/**
 * The canonical lexical form of xsd:dayTimeDuration: a valid lexical form is brought to the form of its length, in
 * which the parts that are zero are left out, the parts are within their ranges and the seconds have no trailing zeros
 * in their fraction, also for a negative duration, for the least and the greatest length, and the datatype recognises
 * exactly this form as canonical.
 */
public class DayTimeDurationDatatypeTest
{
    /**
     * Valid lexical forms with their canonical forms.
     *
     * @return valid lexical forms with their canonical forms
     */
    static Stream<Arguments> getForms()
    {
        return Stream.of(Arguments.of("PT0S", "PT0S"), Arguments.of("-PT0S", "PT0S"), Arguments.of("PT0.0S", "PT0S"),
                Arguments.of("PT1M", "PT1M"), Arguments.of("PT60S", "PT1M"), Arguments.of(" PT1M ", "PT1M"),
                Arguments.of("P0DT1M", "PT1M"), Arguments.of("PT1M0S", "PT1M"), Arguments.of("PT10S", "PT10S"),
                Arguments.of("PT30S", "PT30S"), Arguments.of("PT100S", "PT1M40S"), Arguments.of("PT1M30S", "PT1M30S"),
                Arguments.of("PT90M", "PT1H30M"), Arguments.of("PT3600S", "PT1H"), Arguments.of("P1D", "P1D"),
                Arguments.of("PT24H", "P1D"), Arguments.of("-PT1M", "-PT1M"), Arguments.of("-PT90S", "-PT1M30S"),
                Arguments.of("-P1DT24H", "-P2D"), Arguments.of("PT0.5S", "PT0.5S"), Arguments.of("PT0.50S", "PT0.5S"),
                Arguments.of("PT1.5S", "PT1.5S"), Arguments.of("PT0.000001S", "PT0.000001S"),
                Arguments.of("PT0.000050S", "PT0.00005S"), Arguments.of("PT59.999999S", "PT59.999999S"),
                Arguments.of("P1DT0.5S", "P1DT0.5S"), Arguments.of("P1DT1H1M1.000001S", "P1DT1H1M1.000001S"),
                Arguments.of("P106751991DT4H54.775807S", "P106751991DT4H54.775807S"),
                Arguments.of("PT2562047788H54.775807S", "P106751991DT4H54.775807S"),
                Arguments.of("-P106751991DT4H54.775808S", "-P106751991DT4H54.775808S"),
                Arguments.of("-PT2562047788H54.775808S", "-P106751991DT4H54.775808S"));
    }


    /**
     * Lengths in microseconds: zero, the units and their neighbours, the limits of the range and pseudo-random ones.
     *
     * @return lengths in microseconds
     */
    private static LongStream getLengths()
    {
        Random random = new Random(20261114);

        return LongStream.concat(
                LongStream.of(0, 1, -1, 10, 999999, 1000000, 1000001, 30000000, 59999999, 60000000, 3600000000l,
                        86399999999l, 86400000000l, 86400000001l, -90000000, Long.MAX_VALUE, Long.MAX_VALUE - 1,
                        Long.MIN_VALUE, Long.MIN_VALUE + 1),
                LongStream.generate(() -> random.nextLong() >> random.nextInt(64)).limit(1000));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getForms")
    @DisplayName("a valid form is brought to the canonical form of its length")
    void canonicalForm(String form, String canonical)
    {
        assertTrue(xsdDayTimeDurationType.isValidForm(form));
        assertEquals(canonical, xsdDayTimeDurationType.getCanonicalLexicalForm(form));
        assertEquals(form.equals(canonical), xsdDayTimeDurationType.isCanonicalForm(form));
    }


    @Test
    @DisplayName("the form of a length is valid, canonical and has that length")
    void lexicalForm()
    {
        getLengths().forEach(length -> {
            String form = DayTimeDurationDatatype.getLexicalForm(length);

            assertTrue(xsdDayTimeDurationType.isValidForm(form), form);
            assertTrue(xsdDayTimeDurationType.isCanonicalForm(form), form);
            assertEquals(form, xsdDayTimeDurationType.getCanonicalLexicalForm(form));
            assertEquals(length, DayTimeDurationDatatype.parseValue(form).longValueExact(), form);
        });
    }
}
