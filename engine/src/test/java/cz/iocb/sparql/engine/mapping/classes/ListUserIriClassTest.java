package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.testing.Database;



/**
 * Conversions of {@link ListUserIriClass} from the IRI class and from the box: with the representability check, an IRI
 * yields itself when the column of the table lists it, also when it lists the IRI several times, and NULL otherwise;
 * without the check, an IRI yields itself always. The check reads the listed IRIs, whatever the columns of the checked
 * value are called. A query joining a computed IRI with a pattern of the class takes the checked conversion.
 */
public class ListUserIriClassTest
{
    /**
     * An IRI the table lists twice.
     */
    private static final String repeated = "http://example.org/license/a";

    /**
     * An IRI the table lists once.
     */
    private static final String single = "http://example.org/license/b";

    /**
     * An IRI the table does not list.
     */
    private static final String foreign = "http://example.org/license/x";

    /**
     * Connection pool of the test database.
     */
    private static DataSource pool = null;

    /**
     * The class of the IRIs listed by the column {@code license} of the table {@code sources}.
     */
    private static ListUserIriClass licenses = null;

    /**
     * Configuration mapping the table: a source {@code http://example.org/source/<id>} has the license of its row.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the schema {@code list_iri_test} with the table {@code sources}, whose column {@code license} lists one
     * IRI twice, another one once and holds a NULL.
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
            statement.execute("create schema list_iri_test");
            statement.execute("create table list_iri_test.sources (id int not null, license varchar)");
            statement.execute("insert into list_iri_test.sources values (1, '" + repeated + "'), (2, '" + repeated
                    + "'), (3, '" + single + "'), (4, null)");
        }

        DatabaseTable table = new DatabaseTable("list_iri_test", "sources");
        licenses = new ListUserIriClass("license", table, new TableColumn("license", VARCHAR));

        config = new SparqlDatabaseConfiguration(null, pool, new DatabaseSchema(pool), true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("source", INT4, "http://example.org/source/"));
        config.addIriClass(licenses);
        config.addQuadMapping(table, null, config.createIriMapping("source", "id"),
                config.createIriMapping("ex:license"), config.createIriMapping("license", "license"));
    }


    /**
     * Value of the column evaluated by the database for a row whose column {@code license} holds the given value, null
     * for NULL.
     *
     * @param column the column
     * @param value the value of the column {@code license} of the row, or null
     * @return value of the column evaluated by the database, null for NULL
     * @throws SQLException on database errors
     */
    private static String evaluate(Column column, String value) throws SQLException
    {
        Column row = value == null ? new NullColumn(VARCHAR) : constant(value, VARCHAR);

        try(Connection connection = pool.getConnection(); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(true);

            try(ResultSet result = statement
                    .executeQuery("SELECT " + column + " FROM (SELECT " + row + " AS \"license\") AS \"row\""))
            {
                result.next();
                return result.getString(1);
            }
        }
    }


    /**
     * The column of the class converted from the IRI given as a constant of the superclass.
     *
     * @param superClass the IRI class or the box
     * @param value the IRI
     * @param checkOptional whether the representability check may be skipped
     * @return the column of the class converted from the IRI
     */
    private static Column convert(ResourceClass superClass, String value, boolean checkOptional)
    {
        Column column = superClass.equals(box) ? constant("<" + value + ">", RDFBOX) : constant(value, VARCHAR);

        return licenses.fromGeneralClass(superClass, List.of(column), checkOptional).get(0);
    }


    /**
     * Rows of the query.
     *
     * @param query the query
     * @return rows of the query
     * @throws Exception on errors
     */
    private static List<List<RdfTerm>> execute(String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest();
                Result result = request.execute("PREFIX ex: <http://example.org/> " + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * IRI of a source.
     *
     * @param id the id
     * @return IRI of a source
     */
    private static RdfTerm source(int id)
    {
        return new Iri("http://example.org/source/" + id);
    }


    @Test
    @DisplayName("with the check a listed IRI yields itself, also when it is listed several times")
    void listed() throws SQLException
    {
        assertEquals(repeated, evaluate(convert(iri, repeated, false), null));
        assertEquals(single, evaluate(convert(iri, single, false), null));
        assertEquals(repeated, evaluate(convert(box, repeated, false), null));
        assertEquals(single, evaluate(convert(box, single, false), null));
    }


    @Test
    @DisplayName("with the check an IRI that is not listed yields NULL")
    void foreign() throws SQLException
    {
        assertNull(evaluate(convert(iri, foreign, false), null));
        assertNull(evaluate(convert(box, foreign, false), null));

        // neither does an unbound value, nor a box holding the IRI as a string
        Column string = constant("\"" + repeated + "\"", RDFBOX);

        assertNull(evaluate(licenses.fromGeneralClass(iri, List.of(new NullColumn(VARCHAR)), false).get(0), null));
        assertNull(evaluate(licenses.fromGeneralClass(box, List.of(string), false).get(0), null));
    }


    @Test
    @DisplayName("without the check an IRI yields itself")
    void unchecked() throws SQLException
    {
        assertEquals(repeated, evaluate(convert(iri, repeated, true), null));
        assertEquals(foreign, evaluate(convert(iri, foreign, true), null));
        assertEquals(foreign, evaluate(convert(box, foreign, true), null));
    }


    @Test
    @DisplayName("the check reads the listed IRIs also for a value in a column named as the column of the table")
    void sameName() throws SQLException
    {
        Column from = licenses.fromGeneralClass(iri, List.of(new TableColumn("license", VARCHAR)), false).get(0);

        assertEquals(repeated, evaluate(from, repeated));
        assertEquals(single, evaluate(from, single));
        assertNull(evaluate(from, foreign));
        assertNull(evaluate(from, null));
    }


    @Test
    @DisplayName("an IRI belongs to the class when it is listed")
    void match() throws SQLException
    {
        try(Request request = new Request(config))
        {
            assertTrue(licenses.match(request, new Iri(repeated)));
            assertTrue(licenses.match(request, new Iri(single)));
            assertFalse(licenses.match(request, new Iri(foreign)));
        }
    }


    @Test
    @DisplayName("a pattern with a constant IRI")
    void constantTerm() throws Exception
    {
        assertThat(execute("SELECT ?s WHERE { ?s ex:license <" + repeated + "> }"),
                containsInAnyOrder(List.of(source(1)), List.of(source(2))));
        assertTrue(execute("SELECT ?s WHERE { ?s ex:license <" + foreign + "> }").isEmpty());
    }


    @Test
    @DisplayName("a computed IRI joined with a pattern of the class")
    void computedTerm() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { VALUES ?v { '" + repeated + "' '" + single + "' '" + foreign + "' } "
                + "BIND(IRI(?v) AS ?l) ?s ex:license ?l }";

        assertThat(execute(query), containsInAnyOrder(List.of(source(1), new Iri(repeated)),
                List.of(source(2), new Iri(repeated)), List.of(source(3), new Iri(single))));
    }
}
