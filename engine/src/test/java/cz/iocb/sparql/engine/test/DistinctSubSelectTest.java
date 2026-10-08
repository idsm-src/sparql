package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
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
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Solutions of a DISTINCT sub-select counted by a parent that needs none of its variables. The deduplication is left
 * out when the rows below are already distinct (a unique column, a key, disjoint union branches, or a join with an
 * access pinned to one row), but the rows must still be counted one by one, although they look alike to the parent. The
 * same holds when the sub-select keeps its own deduplication (ORDER BY a variable it does not project), and for values
 * that are new in every solution (STRUUID).
 */
public class DistinctSubSelectTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code distinct_subselect_test} schema and the configuration: the items with a unique label and a
     * repeated tag, and two kinds of things in two tables, whose IRIs belong to two disjoint classes.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema distinct_subselect_test");

            statement.execute("create table distinct_subselect_test.item (id int primary key, "
                    + "label varchar collate \"C\" not null unique, tag varchar collate \"C\" not null)");
            statement.execute(
                    "insert into distinct_subselect_test.item values (1, 'a', 'x'), (2, 'b', 'x'), " + "(3, 'c', 'y')");

            statement.execute("create table distinct_subselect_test.first (id int primary key)");
            statement.execute("insert into distinct_subselect_test.first values (1), (2), (3)");

            statement.execute("create table distinct_subselect_test.second (id int primary key)");
            statement.execute("insert into distinct_subselect_test.second values (1), (2)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new IntegerUserIriClass("first", INT4, "http://example.org/first/"));
        config.addIriClass(new IntegerUserIriClass("second", INT4, "http://example.org/second/"));

        DatabaseTable item = new DatabaseTable("distinct_subselect_test", "item");
        DatabaseTable first = new DatabaseTable("distinct_subselect_test", "first");
        DatabaseTable second = new DatabaseTable("distinct_subselect_test", "second");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(item, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(item, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));

        config.addQuadMapping(first, graph, config.createIriMapping("first", "id"), config.createIriMapping("ex:kind"),
                config.createIriMapping("ex:Thing"));

        config.addQuadMapping(second, graph, config.createIriMapping("second", "id"),
                config.createIriMapping("ex:kind"), config.createIriMapping("ex:Thing"));
    }


    /**
     * Runs the query and returns all rows.
     */
    private static List<List<RdfTerm>> execute(String query)
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
     * Runs the SQL of the query as it is after the first optimisation pass alone, which has to give the same results as
     * both passes, and returns the first column of all rows.
     */
    private static List<String> executeFirstPass(String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            String sql = imcode.optimize(request, false).translate(request);

            try(Connection connection = connectionPool.getConnection();
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(sql))
            {
                List<String> rows = new ArrayList<>();

                while(result.next())
                    rows.add(result.getString(1));

                return rows;
            }
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * Expected xsd:string literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Expected xsd:integer literal.
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT of a unique column")
    void countDistinctUnique() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?l WHERE { ?i ex:label ?l } }");

        assertThat(result, contains(row(integer(3))));
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT of a key")
    void countDistinctKey() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?i WHERE { ?i ex:label ?l } }");

        assertThat(result, contains(row(integer(3))));
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT of a repeated column")
    void countDistinctRepeated() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?t WHERE { ?i ex:tag ?t } }");

        assertThat(result, contains(row(integer(2))));
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT of a union of disjoint classes")
    void countDistinctUnion() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?s WHERE { ?s ex:kind ex:Thing } }");

        assertThat(result, contains(row(integer(5))));
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT of a join with an access pinned to one row")
    void countDistinctJoin() throws Exception
    {
        List<List<RdfTerm>> result = execute("""
                SELECT (COUNT(*) AS ?c) WHERE {
                  SELECT DISTINCT ?i WHERE { ?i ex:label ?l . <http://example.org/item/1> ex:tag ?t }
                }""");

        assertThat(result, contains(row(integer(3))));
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT ordered by a variable it does not project")
    void countDistinctOrdered() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?t WHERE { ?i ex:tag ?t } ORDER BY ?i }");

        assertThat(result, contains(row(integer(2))));
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT ordered by an expression")
    void countDistinctOrderedByExpression() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?t WHERE { ?i ex:tag ?t } ORDER BY STRLEN(?t) }");

        assertThat(result, contains(row(integer(2))));
    }


    @Test
    @DisplayName("COUNT(*) over a slice of DISTINCT ordered by a variable it does not project")
    void countDistinctOrderedSlice() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?t WHERE { ?i ex:tag ?t } ORDER BY ?i OFFSET 1 }");

        assertThat(result, contains(row(integer(1))));
    }


    @Test
    @DisplayName("EXISTS over a slice of DISTINCT ordered by a variable it does not project")
    void existsDistinctOrderedSlice() throws Exception
    {
        List<List<RdfTerm>> result = execute("""
                SELECT (COUNT(*) AS ?c) WHERE {
                  ?s ex:kind ex:Thing
                  FILTER EXISTS { SELECT DISTINCT ?t WHERE { ?i ex:tag ?t } ORDER BY ?i OFFSET 1 }
                }""");

        assertThat(result, contains(row(integer(5))));
    }


    @Test
    @DisplayName("COUNT(*) over DISTINCT of a value new in every solution")
    void countDistinctStruuid() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?r WHERE { ?i ex:tag ?t BIND(STRUUID() AS ?r) } }");

        assertThat(result, contains(row(integer(3))));
    }


    @Test
    @DisplayName("the first optimisation pass counts DISTINCT of a unique column")
    void firstPassCountDistinctUnique() throws Exception
    {
        List<String> result = executeFirstPass(
                "SELECT (COUNT(*) AS ?c) WHERE { SELECT DISTINCT ?l WHERE { ?i ex:label ?l } }");

        assertThat(result, contains("3"));
    }


    @Test
    @DisplayName("the first optimisation pass keeps the rows of a sliced DISTINCT under EXISTS")
    void firstPassExistsDistinctSlice() throws Exception
    {
        List<String> result = executeFirstPass("SELECT ?x WHERE { ?x ex:tag ?t "
                + "FILTER EXISTS { SELECT DISTINCT ?i WHERE { ?i ex:label ?l } OFFSET 1 } }");

        assertThat(result.size(), equalTo(3));
    }


    @Test
    @DisplayName("DISTINCT of a unique column keeps its values")
    void distinctUnique() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT DISTINCT ?l WHERE { ?i ex:label ?l }");

        assertThat(result, containsInAnyOrder(row(string("a")), row(string("b")), row(string("c"))));
    }
}
