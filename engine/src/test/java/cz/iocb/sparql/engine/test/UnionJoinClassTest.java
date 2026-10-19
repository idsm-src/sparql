package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
 * Joins of a variable of a general class (here a generic IRI column) with a union whose branches bind the variable in
 * disjoint subclasses (two user IRI classes). The union presents the subclasses as their common superclass, so that the
 * join compares a single pair of columns, which the database can hash, instead of a disjunction of one comparison per
 * subclass, which forces a nested loop over the whole union.
 */
public class UnionJoinClassTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code union_join_class_test} schema: the items referring to IRIs of the classes {@code a} and
     * {@code b} (or of neither) through a generic IRI column, and the labels of the two classes in a table each, mapped
     * as {@code ex:refers} and {@code rdfs:label}.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema union_join_class_test");
            statement.execute("create table union_join_class_test.items (item varchar not null, ref varchar not null)");
            statement.execute("""
                    insert into union_join_class_test.items values ('i', 'http://example.org/a/x'), \
                    ('j', 'http://example.org/b/y'), ('k', 'http://example.org/a/z'), \
                    ('l', 'http://example.org/c/q')""");
            statement.execute(
                    "create table union_join_class_test.labels_a (id varchar not null, " + "label varchar not null)");
            statement.execute("insert into union_join_class_test.labels_a values ('x', 'ax')");
            statement.execute(
                    "create table union_join_class_test.labels_b (id varchar not null, " + "label varchar not null)");
            statement.execute("insert into union_join_class_test.labels_b values ('y', 'by')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        config.addIriClass(new StringUserIriClass("item", "http://example.org/item/", "[a-z]"));
        config.addIriClass(new StringUserIriClass("a", "http://example.org/a/", "[a-z]"));
        config.addIriClass(new StringUserIriClass("b", "http://example.org/b/", "[a-z]"));

        DatabaseTable items = new DatabaseTable("union_join_class_test", "items");
        DatabaseTable labelsA = new DatabaseTable("union_join_class_test", "labels_a");
        DatabaseTable labelsB = new DatabaseTable("union_join_class_test", "labels_b");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(items, graph, config.createIriMapping("item", "item"),
                config.createIriMapping("ex:refers"), config.createIriMapping(iri, "ref"));

        config.addQuadMapping(labelsA, graph, config.createIriMapping("a", "id"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(labelsB, graph, config.createIriMapping("b", "id"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "label"));
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
     * IRI of the item with the given name.
     */
    private static Iri item(String name)
    {
        return new Iri("http://example.org/item/" + name);
    }


    /**
     * String literal.
     */
    private static TypedLiteral label(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("an optional union of two subclasses is joined in the superclass")
    void optionalUnion() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { ?s ex:refers ?x OPTIONAL { ?x rdfs:label ?l } }";

        assertThat(execute(query), containsInAnyOrder(row(item("i"), label("ax")), row(item("j"), label("by")),
                row(item("k"), null), row(item("l"), null)));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("an optional union with a filter is joined in the superclass")
    void optionalUnionWithFilter() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { ?s ex:refers ?x "
                + "OPTIONAL { ?x rdfs:label ?l FILTER(STRSTARTS(?l, 'a')) } }";

        assertThat(execute(query), containsInAnyOrder(row(item("i"), label("ax")), row(item("j"), null),
                row(item("k"), null), row(item("l"), null)));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("an optional sub-select over a union is joined in the superclass")
    void optionalSubSelect() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { ?s ex:refers ?x OPTIONAL { SELECT ?x ?l WHERE { ?x rdfs:label ?l } } }";

        assertThat(execute(query), containsInAnyOrder(row(item("i"), label("ax")), row(item("j"), label("by")),
                row(item("k"), null), row(item("l"), null)));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("a subtracted union of two subclasses is compared in the superclass")
    void minusUnion() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:refers ?x MINUS { ?x rdfs:label ?l } }";

        assertThat(execute(query), containsInAnyOrder(row(item("k")), row(item("l"))));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("a possibly unbound variable of two subclasses is joined in the superclass")
    void nullableUnion() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { OPTIONAL { ?x rdfs:label ?l } ?s ex:refers ?x }";

        assertThat(execute(query), containsInAnyOrder(row(item("i"), label("ax")), row(item("j"), label("by"))));
    }


    @Test
    @DisplayName("a union joined with one of its subclasses keeps the subclass")
    void joinWithSubclass() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { ?x rdfs:label ?l FILTER(?x = <http://example.org/a/x>) }";

        assertThat(execute(query), containsInAnyOrder(row(new Iri("http://example.org/a/x"), label("ax"))));
    }
}
