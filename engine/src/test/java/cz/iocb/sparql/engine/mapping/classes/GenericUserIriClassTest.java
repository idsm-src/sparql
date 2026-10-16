package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.GenericUserIriClass.SqlCheck;
import cz.iocb.sparql.testing.Database;



/**
 * Conversions of {@link GenericUserIriClass} from the IRI class and from the box: with the representability check, an
 * IRI the regular expression does not recognise yields NULL without reaching the inverse functions, which may assume a
 * recognised IRI; without the check, and for a class whose unrecognised IRIs the functions verify, the functions get
 * every IRI.
 */
public class GenericUserIriClassTest
{
    /**
     * Regular expression recognising the IRIs of the items.
     */
    private static final String regexp = "http://example\\.org/item/[1-9][0-9]*";

    /**
     * An IRI of the class.
     */
    private static final String member = "http://example.org/item/5";

    /**
     * An IRI outside the class, on which the inverse function assuming a recognised IRI fails.
     */
    private static final String foreign = "http://example.org/other";

    /**
     * Connection pool of the test database.
     */
    private static DataSource pool = null;

    /**
     * Class trusting the regular expression; its inverse function assumes a recognised IRI.
     */
    private static GenericUserIriClass trusting = null;

    /**
     * Class whose unrecognised IRIs are verified in the database; its inverse function accepts every IRI.
     */
    private static GenericUserIriClass verifying = null;


    /**
     * Creates the schema {@code generic_iri_test} with the functions of the two classes.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        pool = Database.getPool();

        try(Connection connection = pool.getConnection(); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(true);
            statement.execute("create schema generic_iri_test");
            statement.execute("create function generic_iri_test.item(id int4) returns varchar language sql immutable "
                    + "as $$ select ('http://example.org/item/' || id)::varchar $$");
            statement.execute("create function generic_iri_test.item_inverse(iri varchar) returns int4 language sql "
                    + "immutable as $$ select substring(iri, 25)::int4 $$");
            statement.execute("create function generic_iri_test.checked(id int4) returns varchar language sql "
                    + "immutable as $$ select ('http://example.org/item/' || id)::varchar $$");
            statement.execute("""
                    create function generic_iri_test.checked_inverse(iri varchar) returns int4 language sql \
                    immutable as $$ select case when iri ~ '^http://example\\.org/item/[1-9][0-9]*$' \
                    then substring(iri, 25)::int4 end $$""");
        }

        trusting = new GenericUserIriClass("item", "generic_iri_test", "item", List.of(INT4), regexp);
        verifying = new GenericUserIriClass("checked", "generic_iri_test", "checked", List.of(INT4), regexp,
                SqlCheck.IF_NOT_MATCH);
    }


    /**
     * Value of the column evaluated by the database, null for NULL; the statement runs in a transaction of its own, so
     * that an expected failure does not abort the transaction of the pooled connection for the following statements.
     *
     * @param column the column
     * @return value of the column evaluated by the database, null for NULL
     * @throws SQLException on database errors
     */
    private static String evaluate(Column column) throws SQLException
    {
        try(Connection connection = pool.getConnection(); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(true);

            try(ResultSet result = statement.executeQuery("SELECT " + column))
            {
                result.next();
                return result.getString(1);
            }
        }
    }


    /**
     * The first column of the class converted from the IRI given as a constant of the superclass.
     *
     * @param resClass the class
     * @param superClass the IRI class or the box
     * @param value the IRI, or its text form for the box
     * @param checkOptional whether the representability check may be skipped
     * @return the first column of the class converted from the IRI
     */
    private static Column convert(GenericUserIriClass resClass, ResourceClass superClass, String value,
            boolean checkOptional)
    {
        Column column = superClass.equals(box) ? constant("<" + value + ">", RDFBOX) : constant(value, VARCHAR);

        return resClass.fromGeneralClass(superClass, List.of(column), checkOptional).get(0);
    }


    @Test
    @DisplayName("with the check an IRI the expression does not recognise yields NULL")
    void checked() throws SQLException
    {
        Column from = convert(trusting, iri, member, false);

        assertThat(from.toString(), containsString("sparql.regex_string("));
        assertEquals("5", evaluate(from));
        assertNull(evaluate(convert(trusting, iri, foreign, false)));
        assertEquals("5", evaluate(convert(trusting, box, member, false)));
        assertNull(evaluate(convert(trusting, box, foreign, false)));
    }


    @Test
    @DisplayName("without the check the inverse function gets every IRI")
    void unchecked() throws SQLException
    {
        Column from = convert(trusting, iri, member, true);

        assertThat(from.toString(), not(containsString("sparql.regex_string(")));
        assertEquals("5", evaluate(from));
        assertThrows(SQLException.class, () -> evaluate(convert(trusting, iri, foreign, true)));
    }


    @Test
    @DisplayName("a class verifying the unrecognised IRIs in the database leaves them to its inverse function")
    void verifying() throws SQLException
    {
        Column from = convert(verifying, iri, foreign, false);

        assertThat(from.toString(), not(containsString("sparql.regex_string(")));
        assertNull(evaluate(from));
        assertEquals("5", evaluate(convert(verifying, iri, member, false)));
    }


    @Test
    @DisplayName("the IRI built by the function converts back to the columns")
    void roundTrip() throws SQLException
    {
        List<Column> columns = List.of(constant("5", INT4));

        assertEquals("5",
                evaluate(trusting.fromGeneralClass(iri, trusting.toGeneralClass(iri, columns, false), false).get(0)));
        assertEquals("5",
                evaluate(trusting.fromGeneralClass(box, trusting.toGeneralClass(box, columns, false), false).get(0)));
    }
}
