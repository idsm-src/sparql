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
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
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
 * A SERVICE pattern naming a service of the configuration, the queried service itself or an imported one (see
 * {@link SparqlDatabaseConfiguration#addService}), is evaluated by the named service: its patterns are matched against
 * all mappings of that service, which the dataset clauses of the query do not restrict, its named graphs are the graphs
 * of these mappings and its procedures are those of that service. After the pattern, the mappings of the query are in
 * effect again.
 */
public class LocalServiceTest
{
    /**
     * Prefix declarations prepended to every query; {@code service:main} is the queried service and
     * {@code service:other} the imported one.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX service: <http://example.org/service/> ";

    private static final Iri gm = new Iri("http://example.org/gm");
    private static final Iri go = new Iri("http://example.org/go");
    private static final Iri shared = new Iri("http://example.org/shared");
    private static final TypedLiteral red = new TypedLiteral("red", xsdStringIri);

    private static Engine engine = null;


    /**
     * Creates the {@code local_service_test} schema and the two services. The queried service has the values
     * {@code ex:v} of the nodes 1, 2 and 3 in its default graph and the values {@code ex:n} in the graphs {@code ex:gm}
     * (nodes 1 and 2) and {@code ex:shared} (node 1). The imported service has the values {@code ex:w} of the nodes 1
     * and 7 and the tags of three things in its default graph, the values {@code ex:m} in the graphs {@code ex:go}
     * (node 7) and {@code ex:shared} (node 9), and the procedure {@code ex:search}, which returns the things with the
     * tag given as {@code ex:query}.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema local_service_test");
            statement.execute("create table local_service_test.main_v (s int not null, v int not null)");
            statement.execute("insert into local_service_test.main_v values (1, 1), (2, 2), (3, 3)");
            statement.execute("create table local_service_test.main_gm (s int not null, n int not null)");
            statement.execute("insert into local_service_test.main_gm values (1, 11), (2, 12)");
            statement.execute("create table local_service_test.main_shared (s int not null, n int not null)");
            statement.execute("insert into local_service_test.main_shared values (1, 5)");
            statement.execute("create table local_service_test.other_w (s int not null, w int not null)");
            statement.execute("insert into local_service_test.other_w values (1, 100), (7, 700)");
            statement.execute("create table local_service_test.other_go (s int not null, m int not null)");
            statement.execute("insert into local_service_test.other_go values (7, 77)");
            statement.execute("create table local_service_test.other_shared (s int not null, m int not null)");
            statement.execute("insert into local_service_test.other_shared values (9, 6)");
            statement.execute("create table local_service_test.item (x varchar not null, tag varchar not null)");
            statement.execute("insert into local_service_test.item values ('a', 'red'), ('b', 'red'), ('c', 'blue')");
            statement.execute("""
                    create function local_service_test.search(query varchar) returns setof varchar language sql as \
                    $$ select x from local_service_test.item where tag = query $$""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);
        IntegerUserIriClass node = new IntegerUserIriClass("node", INT4, "http://example.org/node/");
        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/thing/", "[a-z]");


        SparqlDatabaseConfiguration other = new SparqlDatabaseConfiguration("http://example.org/service/other",
                connectionPool, schema, false);
        other.addPrefix("ex", "http://example.org/");
        other.addIriClass(node);
        other.addIriClass(thing);

        other.addQuadMapping(table("other_w"), null, other.createIriMapping(node, "s"), other.createIriMapping("ex:w"),
                other.createLiteralMapping(xsdInt, "w"));
        other.addQuadMapping(table("item"), null, other.createIriMapping(thing, "x"), other.createIriMapping("ex:tag"),
                other.createLiteralMapping(xsdString, "tag"));
        other.addQuadMapping(table("other_go"), other.createIriMapping(go), other.createIriMapping(node, "s"),
                other.createIriMapping("ex:m"), other.createLiteralMapping(xsdInt, "m"));
        other.addQuadMapping(table("other_shared"), other.createIriMapping(shared), other.createIriMapping(node, "s"),
                other.createIriMapping("ex:m"), other.createLiteralMapping(xsdInt, "m"));

        ProcedureDefinition search = new ProcedureDefinition("http://example.org/search",
                new Function("local_service_test", "search"));
        search.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        search.addResult(new ResultDefinition(thing));
        other.addProcedure(search);


        SparqlDatabaseConfiguration main = new SparqlDatabaseConfiguration("http://example.org/service/main",
                connectionPool, schema, false);
        main.addPrefix("ex", "http://example.org/");
        main.addIriClass(node);

        main.addQuadMapping(table("main_v"), null, main.createIriMapping(node, "s"), main.createIriMapping("ex:v"),
                main.createLiteralMapping(xsdInt, "v"));
        main.addQuadMapping(table("main_gm"), main.createIriMapping(gm), main.createIriMapping(node, "s"),
                main.createIriMapping("ex:n"), main.createLiteralMapping(xsdInt, "n"));
        main.addQuadMapping(table("main_shared"), main.createIriMapping(shared), main.createIriMapping(node, "s"),
                main.createIriMapping("ex:n"), main.createLiteralMapping(xsdInt, "n"));

        main.addService(other, false);

        engine = new Engine(main);
    }


    /**
     * Table of the test schema.
     *
     * @param name the name of the table
     * @return table of the test schema
     */
    private static DatabaseTable table(String name)
    {
        return new DatabaseTable("local_service_test", name);
    }


    /**
     * Rows of the query run against the queried service.
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
     * IRI of a node.
     *
     * @param id the number of the node
     * @return IRI of a node
     */
    private static Iri node(int id)
    {
        return new Iri("http://example.org/node/" + id);
    }


    /**
     * IRI of a thing.
     *
     * @param name the name of the thing
     * @return IRI of a thing
     */
    private static Iri thing(String name)
    {
        return new Iri("http://example.org/thing/" + name);
    }


    /**
     * The xsd:int literal.
     *
     * @param value the value
     * @return the xsd:int literal
     */
    private static TypedLiteral value(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
    }


    /**
     * The xsd:integer literal.
     *
     * @param value the value
     * @return the xsd:integer literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("the patterns of a SERVICE are matched against the mappings of the named service")
    void serviceMappings() throws Exception
    {
        assertThat(execute("SELECT * WHERE { SERVICE service:other { ?x ex:w ?w } }"),
                containsInAnyOrder(row(node(1), value(100)), row(node(7), value(700))));
        assertThat(execute("SELECT * WHERE { SERVICE service:other { ?x ex:v ?v } }"), empty());
        assertThat(execute("SELECT (COUNT(*) AS ?count) WHERE { SERVICE service:other { ?s ?p ?o } }"),
                contains(row(integer(5))));
        assertThat(execute("SELECT (COUNT(*) AS ?count) WHERE { SERVICE service:main { ?s ?p ?o } }"),
                contains(row(integer(3))));

        // also the patterns nested in the pattern of the SERVICE
        assertThat(
                execute("SELECT * WHERE { SERVICE service:other "
                        + "{ ?x ex:w ?w FILTER NOT EXISTS { ?x ex:v ?v } OPTIONAL { ?x ex:v ?u } } }"),
                containsInAnyOrder(row(node(1), value(100), null), row(node(7), value(700), null)));
        assertThat(execute("SELECT * WHERE { SERVICE service:other { { SELECT ?x WHERE { ?x (ex:w|ex:v) ?y } } } }"),
                containsInAnyOrder(row(node(1)), row(node(7))));
    }


    @Test
    @DisplayName("the mappings of the query are in effect again after a SERVICE")
    void enclosingMappings() throws Exception
    {
        assertThat(execute("SELECT * WHERE { ?x ex:v ?v SERVICE service:other { ?x ex:w ?w } ?x ex:v ?u }"),
                contains(row(node(1), value(1), value(100), value(1))));
        assertThat(
                execute("SELECT * WHERE { SERVICE service:other "
                        + "{ ?x ex:w ?w SERVICE service:main { ?x ex:v ?v } ?x ex:w ?u } }"),
                contains(row(node(1), value(100), value(1), value(100))));
    }


    @Test
    @DisplayName("the named graphs of a SERVICE are the graphs of the mappings of the named service")
    void namedGraphs() throws Exception
    {
        assertThat(execute("SELECT ?g WHERE { GRAPH ?g { } }"), containsInAnyOrder(row(gm), row(shared)));
        assertThat(execute("SELECT ?g WHERE { SERVICE service:other { GRAPH ?g { } } }"),
                containsInAnyOrder(row(go), row(shared)));
        assertThat(execute("SELECT * WHERE { SERVICE service:other { GRAPH ex:gm { ?x ?p ?o } } }"), empty());

        // the same variable takes the graphs of the query outside the SERVICE and those of the service inside it
        assertThat(execute("SELECT ?g WHERE { GRAPH ?g { } SERVICE service:other { GRAPH ?g { } } }"),
                contains(row(shared)));
        assertThat(execute("SELECT ?g WHERE { SERVICE service:other { GRAPH ?g { } } GRAPH ?g { } }"),
                contains(row(shared)));
        assertThat(execute(
                "SELECT ?g WHERE { GRAPH ?g { } FILTER NOT EXISTS { SERVICE service:other { GRAPH ?g { } } } }"),
                contains(row(gm)));
        assertThat(
                execute("SELECT * WHERE { GRAPH ?g { ?x ex:n ?n } SERVICE service:other { GRAPH ?g { ?y ex:m ?m } } }"),
                contains(row(shared, node(1), value(5), node(9), value(6))));

        // a SERVICE inside a GRAPH is evaluated for every graph of the query
        assertThat(execute("SELECT * WHERE { GRAPH ?g { SERVICE service:other { ?x ex:m ?m } } }"), empty());
        assertThat(execute("SELECT * WHERE { GRAPH ?g { SERVICE service:other { ?x ex:w ?w } } }"),
                containsInAnyOrder(row(gm, node(1), value(100)), row(gm, node(7), value(700)),
                        row(shared, node(1), value(100)), row(shared, node(7), value(700))));
    }


    @Test
    @DisplayName("the dataset clauses of the query do not restrict a SERVICE")
    void datasetClauses() throws Exception
    {
        assertThat(execute("SELECT * FROM ex:gm WHERE { ?x ex:n ?n }"),
                containsInAnyOrder(row(node(1), value(11)), row(node(2), value(12))));
        assertThat(execute("SELECT * FROM ex:gm WHERE { SERVICE service:main { ?x ex:v ?v } }"),
                containsInAnyOrder(row(node(1), value(1)), row(node(2), value(2)), row(node(3), value(3))));
        assertThat(execute("SELECT * FROM ex:gm WHERE { SERVICE service:other { ?x ex:w ?w } }"),
                containsInAnyOrder(row(node(1), value(100)), row(node(7), value(700))));
        assertThat(execute("SELECT * FROM ex:gm WHERE { ?x ex:n ?n SERVICE service:main { ?x ex:v ?v } ?x ex:n ?k }"),
                containsInAnyOrder(row(node(1), value(11), value(1), value(11)),
                        row(node(2), value(12), value(2), value(12))));

        assertThat(execute("SELECT ?g FROM NAMED ex:gm WHERE { GRAPH ?g { } }"), contains(row(gm)));
        assertThat(execute("SELECT ?g FROM NAMED ex:gm WHERE { SERVICE service:main { GRAPH ?g { } } }"),
                containsInAnyOrder(row(gm), row(shared)));
    }


    @Test
    @DisplayName("a procedure of an imported service is called together with the mappings of that service")
    void importedProcedure() throws Exception
    {
        assertThat(execute(
                "SELECT * WHERE { SERVICE service:other { ?t ex:search [ ex:query \"red\" ] . ?t ex:tag ?tag } }"),
                containsInAnyOrder(row(thing("a"), red), row(thing("b"), red)));

        // the queried service has neither the procedure nor the mapping
        assertThat(execute("SELECT * WHERE { ?t ex:search [ ex:query \"red\" ] }"), empty());
        assertThat(execute("SELECT * WHERE { ?t ex:tag ?tag }"), empty());
    }
}
