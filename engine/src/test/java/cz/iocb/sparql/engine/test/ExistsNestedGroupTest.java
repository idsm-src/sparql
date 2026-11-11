package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
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
 * EXISTS patterns with nested groups (the pattern of a GRAPH clause, a UNION branch, the right side of an OPTIONAL or a
 * MINUS), each of which is evaluated for the current solution too, tested for a variable of several classes (IRIs of
 * two user classes and string literals) of which the pattern can join only one. The pattern is optimised for the
 * classes it can join, so a class it cannot join may be left without columns, which neither the comparison with the
 * current solution nor the nested groups may read.
 */
public class ExistsNestedGroupTest
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
     * Creates the {@code exists_nested_group_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): in the default graph
     * {@code a/1 ex:p b/10}, {@code a/2 ex:p b/20} and {@code a/1 ex:q "x"}; in the graph {@code ex:g1}, which the
     * default graph includes, {@code b/10 ex:r a/1}, {@code b/10 ex:t "w"}, {@code b/10 ex:u b/30} and
     * {@code b/30 ex:u b/40}, so that a pattern over its subjects is a union of three accesses binding them as
     * {@code b}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema exists_nested_group_test");
            statement.execute("create table exists_nested_group_test.ab (s int not null, o int not null)");
            statement.execute("insert into exists_nested_group_test.ab values (1, 10), (2, 20)");
            statement.execute("create table exists_nested_group_test.astring (s int not null, v varchar not null)");
            statement.execute("insert into exists_nested_group_test.astring values (1, 'x')");
            statement.execute("create table exists_nested_group_test.ba (s int not null, o int not null)");
            statement.execute("insert into exists_nested_group_test.ba values (10, 1)");
            statement.execute("create table exists_nested_group_test.bstring (s int not null, v varchar not null)");
            statement.execute("insert into exists_nested_group_test.bstring values (10, 'w')");
            statement.execute("create table exists_nested_group_test.bb (s int not null, o int not null)");
            statement.execute("insert into exists_nested_group_test.bb values (10, 30), (30, 40)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        config.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        ConstantIriMapping graph = config.createIriMapping("ex:g1");

        config.addQuadMapping(new DatabaseTable("exists_nested_group_test", "ab"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:p"), config.createIriMapping("b", "o"));

        config.addQuadMapping(new DatabaseTable("exists_nested_group_test", "astring"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:q"),
                config.createLiteralMapping(xsdString, "v"));

        config.addQuadMapping(new DatabaseTable("exists_nested_group_test", "ba"), graph,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:r"), config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("exists_nested_group_test", "bstring"), graph,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:t"),
                config.createLiteralMapping(xsdString, "v"));

        config.addQuadMapping(new DatabaseTable("exists_nested_group_test", "bb"), graph,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:u"), config.createIriMapping("b", "o"));
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


    /**
     * IRI of the class {@code b}.
     *
     * @param id the id
     * @return IRI of the class {@code b}
     */
    private static Iri b(int id)
    {
        return new Iri("http://example.org/b/" + id);
    }


    /**
     * String literal.
     *
     * @param value the value
     * @return string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Boolean literal.
     *
     * @param value the value
     * @return boolean literal
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    @Test
    @DisplayName("NOT EXISTS of a GRAPH pattern")
    void notExistsGraph() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o FILTER NOT EXISTS { GRAPH ex:g1 { ?o ?d ?a } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(b(20)), row(b(40)), row(a(1)), row(string("x")), row(string("w"))));
    }


    @Test
    @DisplayName("EXISTS of a GRAPH pattern")
    void existsGraph() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o FILTER EXISTS { GRAPH ex:g1 { ?o ?d ?a } } }";

        assertThat(execute(query), containsInAnyOrder(row(b(10)), row(b(30))));
    }


    @Test
    @DisplayName("EXISTS of a GRAPH pattern over the objects of the same graph")
    void graphOnBothSides() throws Exception
    {
        String notExists = "SELECT ?o WHERE { GRAPH ex:g1 { ?s ?p ?o } "
                + "FILTER NOT EXISTS { GRAPH ex:g1 { ?o ?d ?a } } }";
        String exists = "SELECT ?o WHERE { GRAPH ex:g1 { ?s ?p ?o } FILTER EXISTS { GRAPH ex:g1 { ?o ?d ?a } } }";

        assertThat(execute(notExists), containsInAnyOrder(row(b(40)), row(a(1)), row(string("w"))));
        assertThat(execute(exists), containsInAnyOrder(row(b(30))));
    }


    @Test
    @DisplayName("NOT EXISTS of a pattern in every named graph")
    void variableGraph() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o FILTER NOT EXISTS { GRAPH ?g { ?o ?d ?a } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(b(20)), row(b(40)), row(a(1)), row(string("x")), row(string("w"))));
    }


    @Test
    @DisplayName("NOT EXISTS of a union with a GRAPH pattern")
    void unionBranch() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o "
                + "FILTER NOT EXISTS { { ?o ex:r ?a } UNION { GRAPH ex:g1 { ?o ?d ?a } } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(b(20)), row(b(40)), row(a(1)), row(string("x")), row(string("w"))));
    }


    @Test
    @DisplayName("NOT EXISTS of a GRAPH pattern followed by OPTIONAL")
    void optional() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o "
                + "FILTER NOT EXISTS { GRAPH ex:g1 { ?o ?d ?a } OPTIONAL { ?a ?x ?o } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(b(20)), row(b(40)), row(a(1)), row(string("x")), row(string("w"))));
    }


    @Test
    @DisplayName("EXISTS of a GRAPH pattern followed by MINUS")
    void minus() throws Exception
    {
        String notExists = "SELECT ?o WHERE { ?s ?p ?o "
                + "FILTER NOT EXISTS { GRAPH ex:g1 { ?o ?d ?a } MINUS { ?o ex:t ?z } } }";
        String exists = "SELECT ?o WHERE { ?s ?p ?o "
                + "FILTER EXISTS { GRAPH ex:g1 { ?o ex:u ?a } MINUS { ?a ex:u ?z } } }";

        assertThat(execute(notExists),
                containsInAnyOrder(row(b(10)), row(b(20)), row(b(40)), row(a(1)), row(string("x")), row(string("w"))));
        assertThat(execute(exists), containsInAnyOrder(row(b(30))));
    }


    @Test
    @DisplayName("EXISTS of a property path and a GRAPH pattern")
    void pathAndGraph() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o FILTER EXISTS { ?o ex:r* ?a GRAPH ex:g1 { ?a ?e ?f } } }";
        String subject = "SELECT ?x WHERE { ?x ex:p ?o FILTER EXISTS { ?o ex:r* ?a . GRAPH ex:g1 { ?a ex:t ?f } } }";

        assertThat(execute(query), containsInAnyOrder(row(b(10)), row(b(30))));
        assertThat(execute(subject), containsInAnyOrder(row(a(1))));
    }


    @Test
    @DisplayName("EXISTS of a GRAPH pattern in the projection")
    void projection() throws Exception
    {
        String query = "SELECT ?o (EXISTS { GRAPH ex:g1 { ?o ?d ?a } } AS ?e) WHERE { ?s ?p ?o }";

        assertThat(execute(query),
                containsInAnyOrder(row(b(10), bool(true)), row(b(20), bool(false)), row(b(30), bool(true)),
                        row(b(40), bool(false)), row(a(1), bool(false)), row(string("x"), bool(false)),
                        row(string("w"), bool(false))));
    }
}
