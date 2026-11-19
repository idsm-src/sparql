package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
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
 * Keys of the GROUP BY clause computed by expressions: a key that a deterministic expression computes from the plain
 * grouping variables alone is the same in all solutions of a group, so it is bound above the aggregation instead of
 * being grouped by, whereas a key referring to another variable, one another key refers to, a nondeterministic one and
 * a key of a clause without a plain grouping variable stay grouped by. The table {@code item} holds the items 1, 2 and
 * 3 with the numbers {@code :a} 1, 1 and 2 and the values {@code :v} 10, 20 and 30.
 */
public class GroupKeyTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code group_key_test} schema with the table of the items and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema group_key_test");
            statement.execute(
                    "create table group_key_test.item (id int4 primary key, a int4 not null, v int4 not null)");
            statement.execute("insert into group_key_test.item values (1, 1, 10), (2, 1, 20), (3, 2, 30)");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("group_key_test", "item");

        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":a"),
                config.createLiteralMapping(xsdInt, "a"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":v"),
                config.createLiteralMapping(xsdInt, "v"));

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
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
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * The GROUP BY clause of the SQL the query is translated into.
     *
     * @param query the query, without the prefix declaration
     * @return the GROUP BY clause of the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String groupBy(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            String sql = imcode.optimize(request, false).optimize(request, true).translate(request);

            return sql.substring(sql.indexOf("GROUP BY"));
        }
    }


    /**
     * The xsd:int literal.
     *
     * @param value the value
     * @return the xsd:int literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
    }


    /**
     * The xsd:integer literal.
     *
     * @param value the value
     * @return the xsd:integer literal
     */
    private static TypedLiteral sum(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    /**
     * The xsd:string literal.
     *
     * @param value the value
     * @return the xsd:string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * The xsd:boolean literal.
     *
     * @param value the value
     * @return the xsd:boolean literal
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
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


    @Test
    @DisplayName("a key computed from the grouping variables alone is bound above the aggregation")
    void derivedKeys() throws Exception
    {
        String query = "SELECT ?a ?k (SUM(?v) AS ?t) WHERE { ?s :a ?a ; :v ?v } GROUP BY ?a (STR(?a) AS ?k)";

        assertThat(execute(query),
                containsInAnyOrder(row(integer(1), string("1"), sum(30)), row(integer(2), string("2"), sum(30))));
        assertThat(groupBy(query), not(containsString("\"k#")));

        // a key without a variable is left out
        String anonymous = "SELECT ?a (SUM(?v) AS ?t) WHERE { ?s :a ?a ; :v ?v } GROUP BY ?a (STR(?a))";

        assertThat(execute(anonymous), containsInAnyOrder(row(integer(1), sum(30)), row(integer(2), sum(30))));
        assertThat(groupBy(anonymous), not(containsString("@")));
    }


    @Test
    @DisplayName("the other keys stay grouped by")
    void groupedKeys() throws Exception
    {
        // a key referring to another variable splits the groups
        assertThat(execute("SELECT ?a ?b (SUM(?v) AS ?t) WHERE { ?s :a ?a ; :v ?v } GROUP BY ?a (?v > 15 AS ?b)"),
                containsInAnyOrder(row(integer(1), bool(false), sum(10)), row(integer(1), bool(true), sum(20)),
                        row(integer(2), bool(true), sum(30))));

        // a key another key refers to is computed before the other one
        String chained = "SELECT ?a ?k ?l WHERE { ?s :a ?a } GROUP BY ?a (STR(?a) AS ?k) (CONCAT(?k, 'x') AS ?l)";

        assertThat(execute(chained), containsInAnyOrder(row(integer(1), string("1"), string("1x")),
                row(integer(2), string("2"), string("2x"))));

        // a nondeterministic key gives every solution a group of its own
        assertThat(execute("SELECT ?a (COUNT(*) AS ?c) WHERE { ?s :a ?a } GROUP BY ?a (RAND() AS ?r)").size(), is(3));

        // no solution makes no group by a constant, whereas a clause without keys makes one group of no solution
        assertThat(execute("SELECT (COUNT(*) AS ?c) WHERE { ?s :none ?x } GROUP BY ('x' AS ?k)"), empty());
    }
}
