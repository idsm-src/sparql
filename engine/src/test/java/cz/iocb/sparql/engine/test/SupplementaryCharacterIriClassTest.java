package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
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
 * User IRI classes whose prefix or suffix holds a character beyond the Basic Multilingual Plane, which a Java string
 * counts twice, whereas the SQL functions of strings that cut the id out of an IRI count it once: an integer, string or
 * mapped id without padding, of a fixed width and with a suffix, and a string or mapped id with a suffix and no prefix.
 * The ids were cut one character too far, so a mapped class did not find the id of a constant IRI, and an IRI converted
 * to a class in the database lost a character of its id or failed as no number.
 *
 * Every class has a table of one member with its value; the table {@code raw} holds the member of every class as text.
 */
public class SupplementaryCharacterIriClassTest
{
    /**
     * A class with one of its members and the value of the member.
     *
     * @param iriClass the class
     * @param member the IRI of the member
     * @param id the id of the member as the table of the class holds it, written as an SQL constant
     * @param value the value of the member
     */
    private static record Member(UserIriClass iriClass, String member, String id, int value)
    {
    }


    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Namespace of the IRIs of the classes.
     */
    private static final String ns = "http://example.org/";

    /**
     * A character beyond the Basic Multilingual Plane, U+1F600.
     */
    private static final String face = Character.toString(0x1F600);

    /**
     * Table mapping the codes of the mapped classes to the ids.
     */
    private static final DatabaseTable codes = new DatabaseTable("supplementary_character_iri_class_test", "codes");

    /**
     * The classes with their members.
     */
    private static final List<Member> members = List.of(
            new Member(new IntegerUserIriClass("int_plain", INT4, iri("int_plain", "")), iri("int_plain", "123"), "123",
                    1),
            new Member(new IntegerUserIriClass("int_fixed", INT4, iri("int_fixed", ""), 3), iri("int_fixed", "123"),
                    "123", 2),
            new Member(new IntegerUserIriClass("int_suffixed", INT4, iri("int_suffixed", ""), "_" + face),
                    iri("int_suffixed", "123_" + face), "123", 3),
            new Member(new StringUserIriClass("str_plain", iri("str_plain", "")), iri("str_plain", "abc"), "'abc'", 4),
            new Member(new StringUserIriClass("str_fixed", iri("str_fixed", ""), 3), iri("str_fixed", "abc"), "'abc'",
                    5),
            new Member(new StringUserIriClass("str_suffixed", iri("str_suffixed", ""), null, "_" + face),
                    iri("str_suffixed", "abc_" + face), "'abc'", 6),
            new Member(new StringUserIriClass("str_unprefixed", null, "urn:[a-z]+", "_" + face), "urn:abc_" + face,
                    "'urn:abc'", 7),
            new Member(map("map_plain", iri("map_plain", ""), "[A-Z]+", 0, null), iri("map_plain", "ABC"), "1", 8),
            new Member(map("map_fixed", iri("map_fixed", ""), "[A-Z]{3}", 3, "_" + face),
                    iri("map_fixed", "ABC_" + face), "1", 9),
            new Member(map("map_suffixed", iri("map_suffixed", ""), "[A-Z]+", 0, "_" + face),
                    iri("map_suffixed", "ABC_" + face), "1", 10),
            new Member(map("map_unprefixed", null, "urn:[a-z]+", 0, "~" + face), "urn:abc~" + face, "2", 11));

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * IRI of the namespace of a class with a supplementary character in its last segment.
     *
     * @param name the name of the class
     * @param id the text after the namespace
     * @return the IRI
     */
    private static String iri(String name, String id)
    {
        return ns + name + face + "/" + id;
    }


    /**
     * Class of the ids that the table {@code codes} maps to codes.
     *
     * @param name the name of the class
     * @param prefix the prefix of the IRIs, or null
     * @param pattern the regular expression of the codes
     * @param length the fixed length of the code, or zero
     * @param suffix the suffix of the IRIs, or null
     * @return the class
     */
    private static UserIriClass map(String name, String prefix, String pattern, int length, String suffix)
    {
        return new MapUserIriClass(name, INT4, codes, new TableColumn("id", INT4), new TableColumn("code", VARCHAR),
                prefix, length, pattern, suffix);
    }


    /**
     * Creates the {@code supplementary_character_iri_class_test} schema with the table {@code raw} of the IRIs, the
     * table {@code codes} and a table of values for every class, and maps the IRIs by :raw and the values by :v.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "supplementary_character_iri_class_test.";

            statement.execute("create schema supplementary_character_iri_class_test");
            statement.execute("create table " + tables + "raw (l int primary key, t varchar not null)");
            statement.execute("create table " + tables + "codes (id int primary key, code varchar not null)");
            statement.execute("insert into " + tables + "codes values (1, 'ABC'), (2, 'urn:abc'), (3, 'BC')");

            try(PreparedStatement insert = connection.prepareStatement("insert into " + tables + "raw values (?, ?)"))
            {
                for(int i = 0; i < members.size(); i++)
                {
                    insert.setInt(1, i + 1);
                    insert.setString(2, members.get(i).member());
                    insert.execute();
                }
            }

            for(Member m : members)
            {
                String table = tables + m.iriClass().getResourceName();
                String type = m.iriClass() instanceof StringUserIriClass ? "varchar" : "int";

                statement.execute("create table " + table + " (id " + type + " not null, v int not null)");
                statement.execute("insert into " + table + " values (" + m.id() + ", " + m.value() + ")");
            }
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
        configuration.addPrefix("ex", ns);

        UserIriClass lnk = new IntegerUserIriClass("lnk", INT4, ns + "lnk/");

        configuration.addIriClass(lnk);
        configuration.addQuadMapping(new DatabaseTable("supplementary_character_iri_class_test", "raw"), null,
                configuration.createIriMapping(lnk, "l"), configuration.createIriMapping("ex:raw"),
                configuration.createIriMapping(BuiltinClasses.iri, "t"));

        for(Member m : members)
        {
            UserIriClass iriClass = m.iriClass();

            configuration.addIriClass(iriClass);
            configuration.addQuadMapping(
                    new DatabaseTable("supplementary_character_iri_class_test", iriClass.getResourceName()), null,
                    configuration.createIriMapping(iriClass, "id"), configuration.createIriMapping("ex:v"),
                    configuration.createLiteralMapping(xsdInt, "v"));
        }

        configuration.setEngineSettings(configuration.getEngineSettings()
                .withJoinOptimizer(joinOptimizer ? JoinOptimizerMode.SETTINGS : JoinOptimizerMode.OFF));

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


    @Test
    @DisplayName("constant IRIs of classes with a supplementary character in the prefix or the suffix")
    void constants() throws Exception
    {
        for(Member m : members)
        {
            Object[] value = { List.<RdfTerm> of(new TypedLiteral(Integer.toString(m.value()), xsdIntIri)) };

            assertThat(m.iriClass().getResourceName(), execute("SELECT ?v WHERE { <" + m.member() + "> :v ?v }"),
                    containsInAnyOrder(value));
        }
    }


    @Test
    @DisplayName("IRIs held as text converted to classes with a supplementary character in the prefix or the suffix")
    void conversions() throws Exception
    {
        Object[] rows = IntStream.rangeClosed(1, members.size())
                .mapToObj(l -> List.<RdfTerm> of(new Iri(ns + "lnk/" + l))).toArray();

        assertThat(
                execute("SELECT ?l WHERE { ?l :raw ?t FILTER EXISTS { { SELECT (COUNT(*) AS ?c) WHERE { ?t :v ?v } } "
                        + "FILTER(?c > 0) } }"),
                containsInAnyOrder(rows));

        assertThat(execute("SELECT ?l WHERE { ?l :raw ?t FILTER NOT EXISTS { { SELECT ?v WHERE { ?t :v ?v } } } }"),
                is(empty()));
    }
}
