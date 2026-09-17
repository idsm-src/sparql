package cz.iocb.sparql.engine.mapping.classes;

import static java.util.Map.entry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;



public class UserLiteralClassTest
{
    @Nested
    public class IntType extends AbstractResourceClassTest
    {
        protected IntType()
        {
            super(UserDatatypes.intDatatype.getCanonicalLiteralClass(), List.of("int4"), Map.ofEntries(
            // @formatter:off
                // canonical
                entry("0", List.of("0")),
                entry("1", List.of("1")),
                entry("-1", List.of("-1")),
                entry("12345", List.of("12345")),
                entry("2147483647", List.of("2147483647")),
                entry("-2147483648", List.of("-2147483648")),

                // non-canonical
                entry("+0", invalid),
                entry("-0", invalid),
                entry("00", invalid),
                entry("+1", invalid),
                entry("01", invalid),
                entry("0000000001", invalid),
                entry(" 1", invalid),
                entry("1 ", invalid),
                entry(" 1 ", invalid),
                entry("\t1", invalid),
                entry(" -1 ", invalid),
                entry("-01", invalid),
                entry("+2147483647", invalid),
                entry("-0000000000", invalid),

                // invalid
                entry("", invalid),
                entry(" ", invalid),
                entry("-", invalid),
                entry("+", invalid),
                entry("--1", invalid),
                entry("1.0", invalid),
                entry("1e1", invalid),
                entry("0x1", invalid),
                entry("abc", invalid),
                entry("1 1", invalid),
                entry("١", invalid),

                // out of range
                entry("2147483648", invalid),
                entry("-2147483649", invalid),
                entry("99999999999999999999", invalid)
            // @formatter:on
            ));
        }
    }


    @Nested
    public class UuidType extends AbstractResourceClassTest
    {
        protected UuidType()
        {
            super(UserDatatypes.uuidDatatype.getCanonicalLiteralClass(), List.of("uuid"), Map.ofEntries(
            // @formatter:off
                // canonical
                entry("00000000-0000-0000-0000-000000000000", List.of("00000000-0000-0000-0000-000000000000")),
                entry("123e4567-e89b-12d3-a456-426614174000", List.of("123e4567-e89b-12d3-a456-426614174000")),
                entry("01234567-89ab-cdef-0123-456789abcdef", List.of("01234567-89ab-cdef-0123-456789abcdef")),
                entry("ffffffff-ffff-ffff-ffff-ffffffffffff", List.of("ffffffff-ffff-ffff-ffff-ffffffffffff")),

                // non-canonical
                entry("123E4567-E89B-12D3-A456-426614174000", invalid),
                entry("01234567-89AB-cdef-0123-456789ABCDEF", invalid),
                entry("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF", invalid),
                entry("00000000-0000-0000-0000-00000000000A", invalid),

                // invalid
                entry("123e4567e89b12d3a456426614174000", invalid),
                entry("123e4567-e89b-12d3-a456-42661417400", invalid),
                entry("123e4567-e89b-12d3-a456-4266141740000", invalid),
                entry("123e4567-e89b-12d3-a456-42661417400g", invalid),
                entry("123e4567_e89b_12d3_a456_426614174000", invalid),
                entry("{123e4567-e89b-12d3-a456-426614174000}", invalid),
                entry(" 123e4567-e89b-12d3-a456-426614174000", invalid),
                entry("123e4567-e89b-12d3-a456-426614174000 ", invalid),
                entry("", invalid)
            // @formatter:on
            ));
        }
    }


    @Nested
    public class TokenType extends AbstractResourceClassTest
    {
        protected TokenType()
        {
            super(UserDatatypes.tokenDatatype.getCanonicalLiteralClass(), List.of("varchar"), Map.ofEntries(
            // @formatter:off
                // canonical
                entry("a", List.of("a")),
                entry("0", List.of("0")),
                entry("abc", List.of("abc")),
                entry("a b", List.of("a b")),
                entry("it's", List.of("it's")),
                entry("a\"b", List.of("a\"b")),
                entry("a\\b", List.of("a\\b")),
                entry("100%", List.of("100%")),
                entry("%s", List.of("%s")),
                entry("ěščřžýáíé", List.of("ěščřžýáíé")),

                // non-canonical
                entry(" a", invalid),
                entry("a ", invalid),
                entry(" a ", invalid),
                entry("\ta\t", invalid),
                entry("a  b", invalid),
                entry("a\tb", invalid),
                entry("a\nb", invalid),
                entry(" a  b ", invalid),
                entry(" it's ", invalid),
                entry("%s  %s", invalid),

                // invalid
                entry("", invalid),
                entry(" ", invalid),
                entry("  ", invalid),
                entry("\t", invalid),
                entry("\n", invalid),
                entry("\r", invalid),
                entry(" \t\n\r ", invalid)
            // @formatter:on
            ));
        }
    }
}
