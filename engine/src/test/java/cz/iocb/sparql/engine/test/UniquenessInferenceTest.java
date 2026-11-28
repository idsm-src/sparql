package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Checks that a deduplication is left out where the keys of the tables make the solutions distinct by themselves,
 * across joins, left joins, unions, groupings and sub-selects, and kept where they do not, with the join optimizer off
 * and on.
 *
 * The table {@code gene} holds the genes 1 and 2; the table {@code protein} the proteins 1 and 2 of the gene 1 and the
 * protein 3 of the gene 2, referencing the gene by a foreign key; the table {@code protein_info}, keyed by the protein,
 * a null mass of the protein 1 and the mass 10 of the protein 2; the table {@code alias}, which has no key, the aliases
 * a, a and b of the protein 1 and a of the protein 2.
 */
public class UniquenessInferenceTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The configurations with the join optimizer off and on.
     */
    private static List<SparqlDatabaseConfiguration> configs = null;


    /**
     * Creates the {@code uniqueness_test} schema with its tables and the configurations mapping them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema uniqueness_test");
            statement.execute("create table uniqueness_test.gene (id int primary key, label varchar not null)");
            statement.execute("insert into uniqueness_test.gene values (1, 'g1'), (2, 'g2')");
            statement.execute("""
                    create table uniqueness_test.protein (id int primary key, name varchar not null, \
                    gene int not null references uniqueness_test.gene(id))""");
            statement.execute("insert into uniqueness_test.protein values (1, 'p1', 1), (2, 'p2', 1), (3, 'p3', 2)");
            statement.execute("""
                    create table uniqueness_test.protein_info (protein int primary key \
                    references uniqueness_test.protein(id), mass int)""");
            statement.execute("insert into uniqueness_test.protein_info values (1, null), (2, 10)");
            statement.execute("""
                    create table uniqueness_test.alias (protein int not null references uniqueness_test.protein(id), \
                    alias varchar not null)""");
            statement.execute("insert into uniqueness_test.alias values (1, 'a'), (1, 'a'), (1, 'b'), (2, 'a')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);
        configs = new ArrayList<>();

        for(boolean optimizer : new boolean[] { false, true })
        {
            SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
            config.setEngineSettings(config.getEngineSettings()
                    .withJoinOptimizer(optimizer ? JoinOptimizerMode.SETTINGS : JoinOptimizerMode.OFF));

            config.addPrefix("ex", "http://example.org/");
            config.addIriClass(new IntegerUserIriClass("protein", INT4, "http://example.org/protein/"));
            config.addIriClass(new IntegerUserIriClass("gene", INT4, "http://example.org/gene/"));

            DatabaseTable gene = new DatabaseTable("uniqueness_test", "gene");
            DatabaseTable protein = new DatabaseTable("uniqueness_test", "protein");
            DatabaseTable info = new DatabaseTable("uniqueness_test", "protein_info");
            DatabaseTable alias = new DatabaseTable("uniqueness_test", "alias");
            ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

            config.addQuadMapping(gene, graph, config.createIriMapping("gene", "id"),
                    config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));
            config.addQuadMapping(protein, graph, config.createIriMapping("protein", "id"),
                    config.createIriMapping("ex:name"), config.createLiteralMapping(xsdString, "name"));
            config.addQuadMapping(protein, graph, config.createIriMapping("protein", "id"),
                    config.createIriMapping("ex:gene"), config.createIriMapping("gene", "gene"));
            config.addQuadMapping(info, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:mass"), config.createLiteralMapping(xsdInt, "mass"));
            config.addQuadMapping(alias, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:alias"), config.createLiteralMapping(xsdString, "alias"));

            configs.add(config);
        }
    }


    /**
     * Translates the query into SQL.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Runs the query and returns all rows.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * How many times the SQL groups rows.
     *
     * @param sql the SQL
     * @return the number of the GROUP BY clauses in the SQL
     */
    private static int groupings(String sql)
    {
        return (sql.length() - sql.replace("GROUP BY", "").length()) / "GROUP BY".length();
    }


    /**
     * Expected row of a protein and a term.
     *
     * @param protein the protein id
     * @param term the term, or null for an unbound value
     * @return row of the protein and the term
     */
    private static List<RdfTerm> row(int protein, RdfTerm term)
    {
        return Arrays.asList(new Iri("http://example.org/protein/" + protein), term);
    }


    /**
     * A string literal.
     *
     * @param value the value
     * @return the literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a join with a table referenced by a foreign key keeps the solutions distinct")
    void joinAlongForeignKey() throws Exception
    {
        String query = "SELECT DISTINCT ?p ?l WHERE { ?p ex:gene ?g . ?g ex:label ?l }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, string("g1")), row(2, string("g1")), row(3, string("g2"))));
            assertThat(translate(config, query), not(containsString("GROUP BY")));
        }
    }


    @Test
    @DisplayName("an OPTIONAL of a table keyed by the shared variable keeps the solutions distinct")
    void optionalByKey() throws Exception
    {
        String query = "SELECT DISTINCT ?p ?m WHERE { ?p ex:name ?n OPTIONAL { ?p ex:mass ?m } }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, null), row(2, new TypedLiteral("10", xsdIntIri)), row(3, null)));
            assertThat(translate(config, query), not(containsString("GROUP BY")));
        }
    }


    @Test
    @DisplayName("a grouping by the projected variable gives distinct solutions")
    void groupingByProjectedVariable() throws Exception
    {
        String query = """
                SELECT DISTINCT ?p ?c WHERE { \
                { SELECT ?p (COUNT(*) AS ?c) WHERE { ?p ex:alias ?a } GROUP BY ?p } }""";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1, new TypedLiteral("3", xsdIntegerIri)),
                    row(2, new TypedLiteral("1", xsdIntegerIri))));
            assertThat(groupings(translate(config, query)), is(1));
        }
    }


    @Test
    @DisplayName("a union of branches binding a variable to differing constants gives distinct solutions")
    void unionOfDifferingConstants() throws Exception
    {
        String query = """
                SELECT DISTINCT ?p ?k WHERE { ?p ex:name ?n . \
                { BIND("a" AS ?k) } UNION { BIND("b" AS ?k) } }""";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1, string("a")), row(1, string("b")),
                    row(2, string("a")), row(2, string("b")), row(3, string("a")), row(3, string("b"))));
            assertThat(translate(config, query), not(containsString("GROUP BY")));
        }
    }


    @Test
    @DisplayName("a union of branches binding a variable to the same constant is deduplicated")
    void unionOfSameConstants() throws Exception
    {
        String query = """
                SELECT DISTINCT ?p ?k WHERE { ?p ex:name ?n . \
                { BIND("a" AS ?k) } UNION { BIND("a" AS ?k) } }""";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, string("a")), row(2, string("a")), row(3, string("a"))));
    }


    @Test
    @DisplayName("distinct solutions ordered by a variable the select does not project are not grouped")
    void orderedByUnprojectedVariable() throws Exception
    {
        String query = "SELECT DISTINCT ?p ?l WHERE { ?p ex:gene ?g . ?g ex:label ?l } ORDER BY DESC(?g) ?p";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query),
                    contains(row(3, string("g2")), row(1, string("g1")), row(2, string("g1"))));
            assertThat(translate(config, query), not(containsString("GROUP BY")));
        }
    }


    @Test
    @DisplayName("a join with a table without a key is deduplicated")
    void joinWithoutKey() throws Exception
    {
        String query = "SELECT DISTINCT ?p ?n WHERE { ?p ex:name ?n ; ex:alias ?a }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query), containsInAnyOrder(row(1, string("p1")), row(2, string("p2"))));
    }


    @Test
    @DisplayName("an OPTIONAL of a table without a key is deduplicated")
    void optionalWithoutKey() throws Exception
    {
        String query = "SELECT DISTINCT ?p ?a WHERE { ?p ex:name ?n OPTIONAL { ?p ex:alias ?a } }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, string("a")), row(1, string("b")), row(2, string("a")), row(3, null)));
    }


    @Test
    @DisplayName("an OPTIONAL joined by a variable the left side may leave unbound is deduplicated")
    void optionalByUnboundVariable() throws Exception
    {
        String query = """
                SELECT DISTINCT ?p ?x WHERE { ?p ex:name ?n \
                OPTIONAL { ?p ex:mass ?x } OPTIONAL { ?p ex:alias ?x } }""";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query), containsInAnyOrder(row(1, string("a")), row(1, string("b")),
                    row(2, new TypedLiteral("10", xsdIntIri)), row(3, null)));
    }
}
