package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
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
 * A group graph pattern starting with OPTIONAL: the left join of the single empty solution with the optional pattern
 * keeps the variables of the pattern possibly unbound, so a later join on them admits their unbound values by a
 * disjunction, which the database can neither hash nor serve by an index. The engine joins the union the leading
 * OPTIONAL stands for instead: the solutions of the pattern, compared by plain equality, and the empty solution guarded
 * by an existence test of the pattern.
 */
public class LeadingOptionalTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code leading_optional_test} schema: the things {@code a} and {@code b} have a value of
     * {@code ex:p}, the things {@code a} and {@code c} a value of {@code ex:q}.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema leading_optional_test");
            statement.execute("create table leading_optional_test.p (x varchar not null, y varchar not null)");
            statement.execute("insert into leading_optional_test.p values ('a', 'ay'), ('b', 'by')");
            statement.execute("create table leading_optional_test.q (x varchar not null, z varchar not null)");
            statement.execute("insert into leading_optional_test.q values ('a', 'az'), ('c', 'cz')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new StringUserIriClass("thing", "http://example.org/thing/", "[a-z]"));

        DatabaseTable p = new DatabaseTable("leading_optional_test", "p");
        DatabaseTable q = new DatabaseTable("leading_optional_test", "q");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(p, graph, config.createIriMapping("thing", "x"), config.createIriMapping("ex:p"),
                config.createLiteralMapping(xsdString, "y"));

        config.addQuadMapping(q, graph, config.createIriMapping("thing", "x"), config.createIriMapping("ex:q"),
                config.createLiteralMapping(xsdString, "z"));
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
     * Returns the SQL code the query is translated to.
     */
    private static String translate(String query) throws Exception
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
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the thing with the given name.
     */
    private static Iri thing(String name)
    {
        return new Iri("http://example.org/thing/" + name);
    }


    /**
     * String literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a leading optional joined on its variable is joined by equality")
    void joinedLeadingOptional() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { OPTIONAL { ?x ex:p ?y } ?x ex:q ?z }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), string("ay"), string("az"))));
        assertThat(translate(query), containsString("NOT EXISTS"));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("a leading optional without solutions leaves its variables unbound")
    void emptyLeadingOptional() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { OPTIONAL { ?x ex:p ?y FILTER(?y = 'none') } ?x ex:q ?z }";

        assertThat(execute(query),
                containsInAnyOrder(row(thing("a"), null, string("az")), row(thing("c"), null, string("cz"))));
        assertThat(translate(query), containsString("NOT EXISTS"));
    }


    @Test
    @DisplayName("a leading optional with solutions joins only them")
    void filteredLeadingOptional() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { OPTIONAL { ?x ex:p ?y FILTER(STRSTARTS(?y, 'b')) } ?x ex:q ?z }";

        assertThat(execute(query), empty());
        assertThat(translate(query), containsString("NOT EXISTS"));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("a leading optional followed by an optional on its variable is joined by equality")
    void leadingOptionalLeftJoin() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { OPTIONAL { ?x ex:p ?y } OPTIONAL { ?x ex:q ?z } }";

        assertThat(execute(query),
                containsInAnyOrder(row(thing("a"), string("ay"), string("az")), row(thing("b"), string("by"), null)));
        assertThat(translate(query), containsString("NOT EXISTS"));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("a leading optional in a union branch is joined by equality")
    void leadingOptionalInUnion() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { { OPTIONAL { ?x ex:p ?y } ?x ex:q ?z } UNION { ?x ex:p ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), string("ay"), string("az")),
                row(thing("a"), string("ay"), null), row(thing("b"), string("by"), null)));
        assertThat(translate(query), containsString("NOT EXISTS"));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("a leading optional under distinct is joined by equality")
    void distinctLeadingOptional() throws Exception
    {
        String query = "SELECT DISTINCT ?x WHERE { OPTIONAL { ?x ex:p ?y } ?x ex:q ?z }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"))));
        assertThat(translate(query), containsString("NOT EXISTS"));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("a leading optional sharing no variable stays a left join")
    void unsharedLeadingOptional() throws Exception
    {
        String query = "SELECT ?x ?y ?a ?b WHERE { OPTIONAL { ?x ex:p ?y } ?a ex:q ?b }";

        assertThat(execute(query),
                containsInAnyOrder(row(thing("a"), string("ay"), thing("a"), string("az")),
                        row(thing("a"), string("ay"), thing("c"), string("cz")),
                        row(thing("b"), string("by"), thing("a"), string("az")),
                        row(thing("b"), string("by"), thing("c"), string("cz"))));
        assertThat(translate(query), not(containsString("NOT EXISTS")));
    }


    @Test
    @DisplayName("a leading optional alone stays a left join")
    void aloneLeadingOptional() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { OPTIONAL { ?x ex:p ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), string("ay")), row(thing("b"), string("by"))));
        assertThat(translate(query), not(containsString("NOT EXISTS")));
    }
}
