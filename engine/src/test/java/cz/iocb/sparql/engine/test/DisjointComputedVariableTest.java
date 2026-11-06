package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
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
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * A variable computed by a BIND, an aggregate or a procedure that a join compares only with classes it is never bound
 * in: the parent needs the variable in no class, yet the variable has to stay bound, as its classes make the join empty
 * (or leave the right side of an OPTIONAL unmatched); left out, it gave a cross product, or a join over missing columns
 * when the BIND, inside EXISTS, was merged with the variable of the current solution.
 */
public class DisjointComputedVariableTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * A row with the only variable unbound.
     */
    private static final List<RdfTerm> unbound = Arrays.asList((RdfTerm) null);

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code disjoint_computed_variable_test} schema, whose tables are mapped with the IRI classes
     * {@code a} ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}):
     * {@code a/1 ex:p b/10}, {@code a/2 ex:p b/20}, {@code a/1 ex:q "x"} and {@code b/10 ex:r a/1}; the procedure
     * {@code ex:search} returns {@code b/10} and {@code b/20} for any {@code ex:query}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema disjoint_computed_variable_test");
            statement.execute("create table disjoint_computed_variable_test.ab (s int not null, o int not null)");
            statement.execute("insert into disjoint_computed_variable_test.ab values (1, 10), (2, 20)");
            statement.execute("create table disjoint_computed_variable_test.lit (s int not null, v varchar not null)");
            statement.execute("insert into disjoint_computed_variable_test.lit values (1, 'x')");
            statement.execute("create table disjoint_computed_variable_test.ba (s int not null, o int not null)");
            statement.execute("insert into disjoint_computed_variable_test.ba values (10, 1)");
            statement.execute("create function disjoint_computed_variable_test.search(query varchar) returns setof int "
                    + "language sql as $$ select o from disjoint_computed_variable_test.ab $$");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema);
        optimized = configuration(connectionPool, schema);
        optimized.setJoinOptimizer(true);
    }


    /**
     * Configuration mapping the test tables.
     *
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @return configuration mapping the test tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");

        IntegerUserIriClass a = new IntegerUserIriClass("a", INT4, "http://example.org/a/");
        IntegerUserIriClass b = new IntegerUserIriClass("b", INT4, "http://example.org/b/");
        configuration.addIriClass(a);
        configuration.addIriClass(b);

        configuration.addQuadMapping(new DatabaseTable("disjoint_computed_variable_test", "ab"), null,
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:p"),
                configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(new DatabaseTable("disjoint_computed_variable_test", "lit"), null,
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:q"),
                configuration.createLiteralMapping(xsdString, "v"));

        configuration.addQuadMapping(new DatabaseTable("disjoint_computed_variable_test", "ba"), null,
                configuration.createIriMapping("b", "s"), configuration.createIriMapping("ex:r"),
                configuration.createIriMapping("a", "o"));

        ProcedureDefinition search = new ProcedureDefinition("http://example.org/search",
                new Function("disjoint_computed_variable_test", "search"));
        search.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        search.addResult(new ResultDefinition(b));
        configuration.addProcedure(search);

        return configuration;
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled.
     *
     * @param query the query, without the prefix declaration
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
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(configuration);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Row of the terms.
     *
     * @param terms the terms
     * @return row of the terms
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the class {@code a}.
     *
     * @param id the id
     * @return IRI of the class {@code a}
     */
    private static Iri a(int id)
    {
        return new Iri("http://example.org/a/" + id);
    }


    @Test
    @DisplayName("a BIND of a constant")
    void bindConstant() throws Exception
    {
        String query = "SELECT ?l WHERE { BIND(<http://example.org/b/10> AS ?v) ?v ex:q ?l }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a union of BINDs of constants")
    void bindUnion() throws Exception
    {
        String query = "SELECT ?l WHERE { { BIND(<http://example.org/b/10> AS ?v) } "
                + "UNION { BIND(<http://example.org/b/20> AS ?v) } ?v ex:q ?l }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a BIND of a constant with an OPTIONAL")
    void bindOptional() throws Exception
    {
        String query = "SELECT ?l WHERE { BIND(<http://example.org/b/10> AS ?v) OPTIONAL { ?v ex:q ?l } }";

        assertThat(execute(query), containsInAnyOrder(unbound));
    }


    @Test
    @DisplayName("a BIND of a constant in an OPTIONAL inside EXISTS")
    void bindOptionalExists() throws Exception
    {
        String query = "SELECT ?v WHERE { OPTIONAL { ?v ex:p ?q } "
                + "FILTER EXISTS { OPTIONAL { BIND(<http://example.org/b/10> AS ?v) } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(a(2))));
    }


    @Test
    @DisplayName("a BIND of a constant in an OPTIONAL inside EXISTS, the variable not projected")
    void bindOptionalExistsUnprojected() throws Exception
    {
        String query = "SELECT ?u WHERE { OPTIONAL { ?v ex:p ?q } "
                + "FILTER EXISTS { OPTIONAL { BIND(<http://example.org/b/10> AS ?v) } } }";

        assertThat(execute(query), containsInAnyOrder(unbound, unbound));
    }


    @Test
    @DisplayName("a BIND of a variable")
    void bindVariable() throws Exception
    {
        String query = "SELECT ?l WHERE { ?x ex:r ?y BIND(?x AS ?v) ?v ex:q ?l }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a BIND of a function of a variable")
    void bindFunction() throws Exception
    {
        String query = "SELECT ?l WHERE { ?x ex:r ?y BIND(STR(?x) AS ?v) ?v ex:q ?l }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("an aggregate")
    void aggregate() throws Exception
    {
        String query = "SELECT ?l WHERE { { SELECT (COUNT(*) AS ?v) WHERE { ?x ex:r ?y } } ?v ex:q ?l }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("an aggregate of groups with an OPTIONAL")
    void groupedAggregateOptional() throws Exception
    {
        String query = "SELECT ?y ?l WHERE { { SELECT ?y (COUNT(*) AS ?v) WHERE { ?x ex:r ?y } GROUP BY ?y } "
                + "OPTIONAL { ?v ex:q ?l } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1), null)));
    }


    @Test
    @DisplayName("an aggregate of groups joined with an OPTIONAL")
    void groupedAggregateJoinedOptional() throws Exception
    {
        String query = "SELECT ?y ?l WHERE { { SELECT ?y (COUNT(*) AS ?v) WHERE { ?x ex:r ?y } GROUP BY ?y } "
                + "{ OPTIONAL { ?v ex:q ?l } } }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a result of a procedure")
    void procedure() throws Exception
    {
        String query = "SELECT ?l WHERE { ?y ex:search [ ex:query \"x\" ] . ?y ex:q ?l }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a result of a procedure with an OPTIONAL")
    void procedureOptional() throws Exception
    {
        String query = "SELECT ?l WHERE { ?y ex:search [ ex:query \"x\" ] OPTIONAL { ?y ex:q ?l } }";

        assertThat(execute(query), containsInAnyOrder(unbound, unbound));
    }
}
