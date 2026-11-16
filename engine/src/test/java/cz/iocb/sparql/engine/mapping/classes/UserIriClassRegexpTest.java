package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.GenericUserIriClass.SqlCheck;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.testing.Database;



/**
 * The regular expressions of the user IRI classes, which Java evaluates when a class matches an IRI and PCRE2 of
 * {@code sparql.regex_string} when a value is converted to the class with the representability check. Both evaluate the
 * same text, anchored to the whole IRI with {@code .} matching every character, so they decide alike: an IRI that ends
 * with a line feed is no member unless the id may be any text (the {@code $} of PCRE2 used to let it pass), an id may
 * hold the other line terminators, and the backreferences of a pattern constraining the digits keep their numbers. The
 * configuration refuses a class whose expression PCRE2 does not compile.
 */
public class UserIriClassRegexpTest
{
    /**
     * Ids of the IRIs every class is tested on, after its prefix and with a final line feed as well.
     */
    private static final List<String> ids = List.of("1", "007", "1234", "99999", "2147483648", "11", "12", "abcd",
            "ab_X", "ABC_C", "abc_N", "x1", "1/x.y", "a\rb", "a\u2028b", "a\u0085b", "a\nb", "");


    /**
     * A class with one of its members.
     *
     * @param iriClass the class
     * @param prefix the text before the id in the IRIs of the class
     * @param id the id of the member
     * @param anyId whether the id may be any text, so that the member with a final line feed is a member too
     */
    private static record Member(UserIriClass iriClass, String prefix, String id, boolean anyId)
    {
        /**
         * The IRI of the member.
         *
         * @return the IRI of the member
         */
        String iri()
        {
            return prefix + id;
        }
    }


    /**
     * Namespace of the IRIs of a class.
     *
     * @param name the name of the class
     * @return namespace of the IRIs of the class
     */
    private static String prefix(String name)
    {
        return "http://example.org/" + name + "/";
    }


    /**
     * Classes of all the kinds and shapes, each with one of its members.
     *
     * @return classes of all the kinds and shapes, each with one of its members
     */
    private static List<Member> classes()
    {
        DatabaseTable table = new DatabaseTable("user_iri_class_regexp_test", "map");
        TableColumn from = new TableColumn("id", INT4);
        TableColumn to = new TableColumn("code", VARCHAR);
        String quoted = "http://example.org/q\\E'\"/";

        return List.of(new Member(new IntegerUserIriClass("i2", INT2, prefix("i2")), prefix("i2"), "1", false),
                new Member(new IntegerUserIriClass("i4m", INT4, prefix("i4m"), -3), prefix("i4m"), "007", false),
                new Member(new IntegerUserIriClass("i4f", INT4, prefix("i4f"), 3), prefix("i4f"), "007", false),
                new Member(new IntegerUserIriClass("i8", INT8, prefix("i8")), prefix("i8"), "1", false),
                new Member(new IntegerUserIriClass("i4s", INT4, prefix("i4s"), "/x.y"), prefix("i4s"), "1/x.y", false),
                new Member(new IntegerUserIriClass("i4p", INT4, prefix("i4p"), 0, "([1-9])\\1", null), prefix("i4p"),
                        "11", false),
                new Member(new StringUserIriClass("s", prefix("s")), prefix("s"), "a\u2028b", true),
                new Member(new StringUserIriClass("sf", prefix("sf"), 4), prefix("sf"), "abcd", false),
                new Member(new StringUserIriClass("sp", prefix("sp"), "[a-z]+"), prefix("sp"), "abcd", false),
                new Member(new StringUserIriClass("ss", prefix("ss"), null, "_X"), prefix("ss"), "ab_X", false),
                new Member(new StringUserIriClass("sq", quoted), quoted, "abcd", true),
                new Member(new StringUserIriClass("sn", null, 0, null, "_N"), "", "abc_N", false),
                new Member(new MapUserIriClass("m", INT4, table, from, to, prefix("m"), 3, "[A-Z]{3}", "_C"),
                        prefix("m"), "ABC_C", false),
                new Member(new GenericUserIriClass("g", "s", "f", List.of(INT4), "http://example\\.org/g/[a-z][0-9]+"),
                        prefix("g"), "x1", false));
    }


    /**
     * The text with its line terminators written as escapes.
     *
     * @param text the text
     * @return the text with its line terminators written as escapes
     */
    private static String shown(String text)
    {
        return text.replace("\n", "\\n").replace("\r", "\\r").replace("\u0085", "\\u0085").replace("\u2028", "\\u2028");
    }


    /**
     * The decision of PCRE2 whether the regular expression matches the text.
     *
     * @param statement statement calling {@code sparql.regex_string}
     * @param text the text
     * @param regexp the regular expression
     * @return the decision of PCRE2, or null when it gives none
     * @throws SQLException on database errors
     */
    private static Boolean pcre2(PreparedStatement statement, String text, String regexp) throws SQLException
    {
        statement.setString(1, text);
        statement.setString(2, regexp);

        try(ResultSet result = statement.executeQuery())
        {
            result.next();
            return (Boolean) result.getObject(1);
        }
    }


    @Test
    @DisplayName("Java and PCRE2 decide alike which IRIs belong to a class")
    void dialectsAgree() throws Exception
    {
        try(Request request = TestRequest.create();
                Connection connection = Database.getPool().getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT sparql.regex_string(?, ?)"))
        {
            for(Member member : classes())
            {
                UserIriClass iriClass = member.iriClass();
                String regexp = iriClass.getSqlRegexps().get(0);
                Pattern pattern = Pattern.compile(regexp);

                for(String id : ids)
                {
                    for(String text : List.of(member.prefix() + id, member.prefix() + id + "\n"))
                    {
                        boolean java = pattern.matcher(text).matches();

                        assertThat(iriClass.getResourceName() + " " + shown(text), pcre2(statement, text, regexp),
                                is(java));

                        // a class matching the IRIs by the expression alone decides by the same text
                        if(!(iriClass instanceof MapUserIriClass))
                            assertThat(iriClass.getResourceName() + " " + shown(text),
                                    iriClass.match(request, new Iri(text)), is(java));
                    }
                }

                // a member is a member, the same IRI with a final line feed only where the id may be any text
                assertThat(iriClass.getResourceName(), pcre2(statement, member.iri(), regexp), is(true));
                assertThat(iriClass.getResourceName(), pcre2(statement, member.iri() + "\n", regexp),
                        is(member.anyId()));
            }
        }
    }


    @Test
    @DisplayName("the configuration refuses a class whose regular expression PCRE2 does not compile")
    void uncompilable() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, Database.getPool(), null, false);

        assertThrows(IllegalArgumentException.class, () -> config
                .addIriClass(new IntegerUserIriClass("jd", INT4, prefix("jd"), 0, "\\p{javaDigit}+", null)));

        assertThrows(IllegalArgumentException.class, () -> config.addIriClass(new GenericUserIriClass("jl", "s", "f",
                List.of(INT4), "http://example\\.org/jl/\\p{javaLowerCase}[0-9]+", SqlCheck.NEVER)));

        // a class that leaves the IRIs it does not recognise to the database never hands the expression to PCRE2
        config.addIriClass(new GenericUserIriClass("jn", "s", "f", List.of(INT4),
                "http://example\\.org/jn/\\p{javaLowerCase}[0-9]+", SqlCheck.IF_NOT_MATCH));

        // the check matches the expression against the empty text, which is no failure where the expression matches it
        config.addIriClass(new StringUserIriClass("je", null, 0, null, null));

        // without a database, nothing is checked
        new SparqlDatabaseConfiguration(null, null, null, false)
                .addIriClass(new IntegerUserIriClass("jd", INT4, prefix("jd"), 0, "\\p{javaDigit}+", null));
    }


    @Test
    @DisplayName("an expression that its anchoring breaks fails when the class is created")
    void brokenByAnchoring()
    {
        assertThrows(PatternSyntaxException.class, () -> new GenericUserIriClass("x", "s", "f", List.of(INT4),
                "http://example\\.org/x/[0-9]+(?x)# a comment to the end", SqlCheck.NEVER));
    }
}
