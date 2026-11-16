package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import java.sql.Connection;
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
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.MapUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * User IRI classes without a prefix, whose IRIs start with the id: string ids with a suffix, of a fixed length with a
 * suffix, constrained by a pattern and of a fixed length, and mapped ids with a suffix. A constant IRI of such a class
 * is held as its id, the IRI without the suffix, and the prefix the class guarantees for the IRIs of a column is empty.
 *
 * Every class has a table of two members with their values; the table {@code raw} holds the first member of every class
 * as text.
 */
public class NullPrefixIriClassTest
{
    /**
     * A class with two of its members, their values and the row of the table {@code raw} holding the first one.
     *
     * @param iriClass the class
     * @param iri the IRI of the first member
     * @param value the value of the first member
     * @param other the IRI of the other member
     * @param otherValue the value of the other member
     * @param row the row of the table {@code raw} holding the first member
     */
    private static record Members(UserIriClass iriClass, String iri, int value, String other, int otherValue, int row)
    {
    }


    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * The classes with their members.
     */
    private static final List<Members> members = List.of(
            new Members(new StringUserIriClass("suffixed", null, null, "_N"), "http://example.org/a_N", 1,
                    "http://example.org/b_N", 2, 1),
            new Members(new StringUserIriClass("fixed_suffixed", null, 10, null, "_F"), "urn:f:abcd_F", 3,
                    "urn:f:wxyz_F", 4, 2),
            new Members(new StringUserIriClass("patterned", null, "urn:r:[0-9]+"), "urn:r:1", 5, "urn:r:2", 6, 3),
            new Members(new StringUserIriClass("fixed", null, 9), "urn:g:abc", 7, "urn:g:xyz", 8, 4),
            new Members(new MapUserIriClass("mapped", INT4, new DatabaseTable("null_prefix_iri_class_test", "codes"),
                    new TableColumn("id", INT4), new TableColumn("code", VARCHAR), null, 0, "urn:m:[a-z]+", "_M"),
                    "urn:m:abc_M", 9, "urn:m:def_M", 10, 5));

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code null_prefix_iri_class_test} schema with the table {@code raw} of the IRIs and a table of
     * values for every class, and maps the IRIs by :raw and the values by :v.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "null_prefix_iri_class_test.";

            statement.execute("create schema null_prefix_iri_class_test");
            statement.execute("create table " + tables + "raw (l int primary key, t varchar not null)");
            statement.execute("insert into " + tables + "raw values (1, 'http://example.org/a_N'), "
                    + "(2, 'urn:f:abcd_F'), (3, 'urn:r:1'), (4, 'urn:g:abc'), (5, 'urn:m:abc_M')");

            for(String table : List.of("suffixed", "fixed_suffixed", "patterned", "fixed"))
                statement.execute("create table " + tables + table + " (id varchar not null, v int not null)");

            statement.execute("insert into " + tables + "suffixed values ('http://example.org/a', 1), "
                    + "('http://example.org/b', 2)");
            statement.execute("insert into " + tables + "fixed_suffixed values ('urn:f:abcd', 3), ('urn:f:wxyz', 4)");
            statement.execute("insert into " + tables + "patterned values ('urn:r:1', 5), ('urn:r:2', 6)");
            statement.execute("insert into " + tables + "fixed values ('urn:g:abc', 7), ('urn:g:xyz', 8)");

            statement.execute("create table " + tables + "codes (id int primary key, code varchar not null)");
            statement.execute("insert into " + tables + "codes values (1, 'urn:m:abc'), (2, 'urn:m:def')");
            statement.execute("create table " + tables + "mapped (id int not null, v int not null)");
            statement.execute("insert into " + tables + "mapped values (1, 9), (2, 10)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema, false);
        optimized = configuration(connectionPool, schema, true);
    }


    /**
     * Configuration mapping the tables.
     *
     * @param connectionPool pool of the test database
     * @param schema schema of the test database
     * @param joinOptimizer whether the join optimizer is enabled
     * @return configuration mapping the tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema,
            boolean joinOptimizer) throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");

        UserIriClass lnk = new IntegerUserIriClass("lnk", INT4, "http://example.org/lnk/");

        configuration.addIriClass(lnk);
        configuration.addQuadMapping(new DatabaseTable("null_prefix_iri_class_test", "raw"), null,
                configuration.createIriMapping(lnk, "l"), configuration.createIriMapping("ex:raw"),
                configuration.createIriMapping(BuiltinClasses.iri, "t"));

        for(Members m : members)
        {
            UserIriClass iriClass = m.iriClass();

            configuration.addIriClass(iriClass);
            configuration.addQuadMapping(new DatabaseTable("null_prefix_iri_class_test", iriClass.getResourceName()),
                    null, configuration.createIriMapping(iriClass, "id"), configuration.createIriMapping("ex:v"),
                    configuration.createLiteralMapping(xsdInt, "v"));
        }

        configuration.setJoinOptimizer(joinOptimizer);

        return configuration;
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled.
     *
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        List<List<RdfTerm>> rows = execute(config, query);

        assertThat(execute(optimized, query), containsInAnyOrder(rows.toArray()));

        return rows;
    }


    /**
     * Rows of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = new Engine(configuration).getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Rows of xsd:int literals, one literal each.
     *
     * @param values the values of the literals
     * @return rows of the literals
     */
    private static Object[] values(int... values)
    {
        return Arrays.stream(values).mapToObj(v -> List.<RdfTerm> of(new TypedLiteral(Integer.toString(v), xsdIntIri)))
                .toArray();
    }


    @Test
    @DisplayName("constant IRIs of classes without a prefix")
    void constants() throws Exception
    {
        for(Members m : members)
        {
            String iri = "<" + m.iri() + ">";
            String other = "<" + m.other() + ">";

            assertThat(execute("SELECT ?v WHERE { " + iri + " :v ?v }"), containsInAnyOrder(values(m.value())));

            assertThat(execute("SELECT ?v WHERE { VALUES ?s { " + iri + " " + other + " } ?s :v ?v }"),
                    containsInAnyOrder(values(m.value(), m.otherValue())));

            assertThat(execute("SELECT ?v WHERE { ?s :v ?v FILTER(?s = " + iri + ") }"),
                    containsInAnyOrder(values(m.value())));

            assertThat(execute("SELECT ?v WHERE { ?s :v ?v FILTER(?s IN (" + iri + ", " + other + ")) }"),
                    containsInAnyOrder(values(m.value(), m.otherValue())));

            // the IRIs held as text, converted to the class of the constant
            assertThat(execute("SELECT ?l WHERE { ?l :raw ?t FILTER(?t = " + iri + ") }"),
                    containsInAnyOrder(List.<RdfTerm> of(new Iri("http://example.org/lnk/" + m.row()))));
        }
    }


    @Test
    @DisplayName("the prefix a class without a prefix guarantees")
    void prefixes() throws Exception
    {
        for(Members m : members)
            assertThat(m.iriClass().getResourceName(), m.iriClass().getPrefix(List.of(new TableColumn("id", VARCHAR))),
                    is(""));

        // the columns of a constant string id are the whole IRI again
        try(Request request = new Engine(config).getRequest())
        {
            for(Members m : members)
                if(m.iriClass() instanceof StringUserIriClass iriClass)
                    assertThat(iriClass.getPrefix(request.getColumns(iriClass, new Iri(m.iri()))), is(m.iri()));
        }
    }
}
