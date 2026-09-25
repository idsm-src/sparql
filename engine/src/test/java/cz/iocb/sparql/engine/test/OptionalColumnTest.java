package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.Database;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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



/**
 * Terms represented by a class with an optional column (see {@link OptionalSuffixIriClass}): the optional column may be
 * NULL while the term is bound, so joins, constants, deduplication and the presence tests must treat NULL there as a
 * value rather than as an unbound term.
 */
public class OptionalColumnTest
{
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    private static DataSource connectionPool = null;

    private static DatabaseSchema schema = null;

    private static SparqlDatabaseConfiguration config = null;


    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema optional_test");

            // the unique index over the nullable column does not make the rows with a NULL sub unique
            statement.execute("create table optional_test.item (id int not null, sub int, "
                    + "label varchar collate \"C\" not null, unique (id, sub))");
            statement.execute("insert into optional_test.item values (1, null, 'one'), (1, null, 'one again'), "
                    + "(2, null, 'two'), (3, 1, 'three one'), (3, 2, 'three two')");

            statement.execute("create table optional_test.synonym (id int not null, sub int, "
                    + "synonym varchar collate \"C\" not null)");
            statement.execute("insert into optional_test.synonym values (1, null, 'uno'), (3, 1, 'tres uno'), "
                    + "(3, null, 'tres')");

            // the optional column of the class is not nullable in this table
            statement.execute("create table optional_test.alias (id int not null, sub int not null, "
                    + "alias varchar collate \"C\" not null)");
            statement.execute(
                    "insert into optional_test.alias values (3, 1, 'III/1'), (3, 2, 'III/2'), " + "(2, 0, 'II/0')");
        }

        schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new OptionalSuffixIriClass("item", "http://example.org/item/"));

        DatabaseTable item = new DatabaseTable("optional_test", "item");
        DatabaseTable synonym = new DatabaseTable("optional_test", "synonym");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(item, graph, config.createIriMapping("item", "id", "sub"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(synonym, graph, config.createIriMapping("item", "id", "sub"),
                config.createIriMapping("ex:synonym"), config.createLiteralMapping(xsdString, "synonym"));

        DatabaseTable alias = new DatabaseTable("optional_test", "alias");

        config.addQuadMapping(alias, graph, config.createIriMapping("item", "id", "sub"),
                config.createIriMapping("ex:alias"), config.createLiteralMapping(xsdString, "alias"));
    }


    private static String translate(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


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


    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    private static Iri item(int id, int sub)
    {
        return new Iri("http://example.org/item/" + id + "/" + sub);
    }


    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("terms with and without the optional part are generated")
    void access() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s ?l WHERE { ?s ex:label ?l }");

        assertThat(result,
                containsInAnyOrder(row(item(1), string("one")), row(item(1), string("one again")),
                        row(item(2), string("two")), row(item(3, 1), string("three one")),
                        row(item(3, 2), string("three two"))));
    }


    @Test
    @DisplayName("a constant term without the optional part matches only NULL")
    void constantWithoutSuffix() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?l WHERE { <http://example.org/item/1> ex:label ?l }");

        assertThat(result, containsInAnyOrder(row(string("one")), row(string("one again"))));

        result = execute("SELECT ?l WHERE { <http://example.org/item/3> ex:label ?l }");

        assertThat(result, containsInAnyOrder());
    }


    @Test
    @DisplayName("a constant term with the optional part matches the value")
    void constantWithSuffix() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?l WHERE { <http://example.org/item/3/1> ex:label ?l }");

        assertThat(result, containsInAnyOrder(row(string("three one"))));
    }


    @Test
    @DisplayName("a join compares the optional column null-safely")
    void join() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s ?l ?y WHERE { ?s ex:label ?l . ?s ex:synonym ?y }");

        assertThat(result,
                containsInAnyOrder(row(item(1), string("one"), string("uno")),
                        row(item(1), string("one again"), string("uno")),
                        row(item(3, 1), string("three one"), string("tres uno"))));
    }


    @Test
    @DisplayName("sameTerm and = follow the same identity")
    void sameTerm() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT ?l ?y WHERE { ?s ex:label ?l . ?t ex:synonym ?y . FILTER(sameTerm(?s, ?t)) }");

        assertThat(result, containsInAnyOrder(row(string("one"), string("uno")),
                row(string("one again"), string("uno")), row(string("three one"), string("tres uno"))));

        result = execute("SELECT ?l ?y WHERE { ?s ex:label ?l . ?t ex:synonym ?y . FILTER(?s = ?t) }");

        assertThat(result, containsInAnyOrder(row(string("one"), string("uno")),
                row(string("one again"), string("uno")), row(string("three one"), string("tres uno"))));
    }


    @Test
    @DisplayName("a term with a NULL optional column is bound")
    void optionalAndBound() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT ?s ?l WHERE { ?s ex:label ?l OPTIONAL { ?s ex:synonym ?y } FILTER(!BOUND(?y)) }");

        assertThat(result, containsInAnyOrder(row(item(2), string("two")), row(item(3, 2), string("three two"))));

        result = execute("SELECT ?s ?y WHERE { ?s ex:label ?l OPTIONAL { ?s ex:synonym ?y } FILTER(BOUND(?s)) }");

        assertThat(result, containsInAnyOrder(row(item(1), string("uno")), row(item(1), string("uno")),
                row(item(2), null), row(item(3, 1), string("tres uno")), row(item(3, 2), null)));
    }


    @Test
    @DisplayName("MINUS and NOT EXISTS compare the optional column null-safely")
    void minus() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s WHERE { ?s ex:label ?l MINUS { ?s ex:synonym ?y } }");

        assertThat(result, containsInAnyOrder(row(item(2)), row(item(3, 2))));

        result = execute("SELECT ?s ?y WHERE { ?s ex:synonym ?y FILTER NOT EXISTS { ?s ex:label ?l } }");

        assertThat(result, containsInAnyOrder(row(item(3), string("tres"))));
    }


    @Test
    @DisplayName("VALUES with terms with and without the optional part")
    void values() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s ?l WHERE { VALUES ?s { <http://example.org/item/1> "
                + "<http://example.org/item/3/2> <http://example.org/item/3> } ?s ex:label ?l }");

        assertThat(result, containsInAnyOrder(row(item(1), string("one")), row(item(1), string("one again")),
                row(item(3, 2), string("three two"))));
    }


    @Test
    @DisplayName("a unique index over the nullable optional column does not imply distinct rows")
    void distinct() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT DISTINCT ?s WHERE { ?s ex:label ?l }");

        assertThat(result, containsInAnyOrder(row(item(1)), row(item(2)), row(item(3, 1)), row(item(3, 2))));

        result = execute("SELECT (COUNT(DISTINCT ?s) AS ?c) WHERE { ?s ex:label ?l }");

        assertThat(result, containsInAnyOrder(row(integer(4))));

        result = execute("SELECT ?s (COUNT(?l) AS ?c) WHERE { ?s ex:label ?l } GROUP BY ?s");

        assertThat(result, containsInAnyOrder(row(item(1), integer(2)), row(item(2), integer(1)),
                row(item(3, 1), integer(1)), row(item(3, 2), integer(1))));
    }


    @Test
    @DisplayName("ORDER BY sorts the generated IRIs")
    void order() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s WHERE { ?s ex:label ?l } ORDER BY ?s ?l");

        assertThat(result, contains(row(item(1)), row(item(1)), row(item(2)), row(item(3, 1)), row(item(3, 2))));
    }


    @Test
    @DisplayName("a constant compared in a filter")
    void filter() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT ?l WHERE { ?s ex:label ?l FILTER(?s = <http://example.org/item/3/1>) }");

        assertThat(result, containsInAnyOrder(row(string("three one"))));

        result = execute("SELECT ?l WHERE { ?s ex:label ?l FILTER(?s != <http://example.org/item/1>) }");

        assertThat(result, containsInAnyOrder(row(string("two")), row(string("three one")), row(string("three two"))));

        result = execute("SELECT ?l WHERE { ?s ex:label ?l FILTER(STR(?s) = 'http://example.org/item/3/2') }");

        assertThat(result, containsInAnyOrder(row(string("three two"))));
    }


    @Test
    @DisplayName("the same variable at two positions of one pattern")
    void selfJoin() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s WHERE { ?s ex:label ?l . ?s ex:label ?m FILTER(?l != ?m) }");

        assertThat(result, containsInAnyOrder(row(item(1)), row(item(1))));
    }


    @Test
    @DisplayName("UNION of terms with and without the optional part")
    void union() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s WHERE { { <http://example.org/item/2> ex:label ?l "
                + "BIND(<http://example.org/item/2> AS ?s) } UNION { ?s ex:synonym ?y } }");

        assertThat(result, containsInAnyOrder(row(item(2)), row(item(1)), row(item(3, 1)), row(item(3))));
    }


    @Test
    @DisplayName("a join with a table where the optional column is not nullable")
    void joinWithNotNullOptional() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s ?l ?a WHERE { ?s ex:label ?l . ?s ex:alias ?a }");

        assertThat(result, containsInAnyOrder(row(item(3, 1), string("three one"), string("III/1")),
                row(item(3, 2), string("three two"), string("III/2"))));

        result = execute("SELECT ?l ?a WHERE { ?s ex:label ?l . ?t ex:alias ?a FILTER(sameTerm(?s, ?t)) }");

        assertThat(result, containsInAnyOrder(row(string("three one"), string("III/1")),
                row(string("three two"), string("III/2"))));
    }


    @Test
    @DisplayName("a constant without the optional part cannot match a not nullable column")
    void constantWithoutSuffixOnNotNull() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?a WHERE { <http://example.org/item/3> ex:alias ?a }");

        assertThat(result, containsInAnyOrder());

        // the impossible access is recognised at translation time
        assertThat(translate("SELECT ?a WHERE { <http://example.org/item/3> ex:alias ?a }"),
                not(containsString("\"alias\"")));
    }


    @Test
    @DisplayName("the null-safe equality is generated only when both sides may be null")
    void generatedEquality() throws Exception
    {
        assertThat(translate("SELECT ?l ?y WHERE { ?s ex:label ?l . ?s ex:synonym ?y }"),
                containsString("IS NOT DISTINCT FROM"));

        assertThat(translate("SELECT ?l ?a WHERE { ?s ex:label ?l . ?s ex:alias ?a }"),
                not(containsString("IS NOT DISTINCT FROM")));

        assertThat(translate("SELECT ?l ?a WHERE { ?s ex:label ?l . ?t ex:alias ?a FILTER(?s = ?t) }"),
                not(containsString("IS NOT DISTINCT FROM")));

        assertThat(translate("SELECT ?l ?y WHERE { ?s ex:label ?l . ?t ex:synonym ?y FILTER(?s = ?t) }"),
                containsString("IS NOT DISTINCT FROM"));

        // the knowledge survives a union of accesses to the not nullable column
        assertThat(translate("SELECT ?y WHERE { { ?s ex:alias ?a } UNION { ?s ex:alias ?b } ?s ex:synonym ?y }"),
                not(containsString("IS NOT DISTINCT FROM")));

        // but not a union with an access to the nullable one
        assertThat(translate("SELECT ?y WHERE { { ?s ex:alias ?a } UNION { ?s ex:label ?l } ?s ex:synonym ?y }"),
                containsString("IS NOT DISTINCT FROM"));
    }
}
