package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
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
 * Queries over user IRI classes that overlap without being related (the compounds stored by their integer id and the
 * compounds with an even id stored as text, declared to share IRIs) and a class that is a subclass of one of them (the
 * compounds with a small id, see {@link SmallCompoundIriClass}): constants, joins, unions, filters and deduplication
 * must convert between the classes instead of pruning them as disjoint.
 */
public class OverlappingIriClassTest
{
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    private static final String iriPrefix = "http://example.org/compound/";

    private static DataSource connectionPool = null;

    private static DatabaseSchema schema = null;

    private static SparqlDatabaseConfiguration config = null;


    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema overlap_test");

            statement.execute("create table overlap_test.compound (id int not null primary key, "
                    + "label varchar collate \"C\" not null)");
            statement.execute("insert into overlap_test.compound values (2, 'two'), (41, 'forty-one'), "
                    + "(42, 'forty-two'), (141, 'hundred forty-one'), (142, 'hundred forty-two')");

            statement.execute("create table overlap_test.even (code varchar collate \"C\" not null primary key, "
                    + "note varchar collate \"C\" not null)");
            statement.execute("insert into overlap_test.even values ('2', 'II'), ('42', 'XLII'), ('142', 'CXLII')");

            statement.execute("create table overlap_test.small (id int not null primary key, "
                    + "flag varchar collate \"C\" not null)");
            statement.execute("insert into overlap_test.small values (2, 'a'), (41, 'b'), (42, 'c')");

            statement.execute("create table overlap_test.link (src int not null, dst varchar collate \"C\" not null)");
            statement.execute("insert into overlap_test.link values (2, '2'), (42, '41'), (142, '142'), (41, '2')");
        }

        schema = new DatabaseSchema(connectionPool);

        IntegerUserIriClass compound = new IntegerUserIriClass("compound", INT4, iriPrefix);
        StringUserIriClass even = new StringUserIriClass("even", iriPrefix, "[0-9]*[02468]");
        SmallCompoundIriClass small = new SmallCompoundIriClass("small", compound, iriPrefix);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(compound);
        config.addIriClass(even);
        config.addIriClass(small);
        config.addIriClass(new IntegerUserIriClass("other", INT4, "http://example.org/other/"));
        config.addIriClassOverlap(compound, even);
        config.addIriClassOverlap(small, even);

        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(new DatabaseTable("overlap_test", "compound"), graph,
                config.createIriMapping("compound", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(new DatabaseTable("overlap_test", "even"), graph, config.createIriMapping("even", "code"),
                config.createIriMapping("ex:note"), config.createLiteralMapping(xsdString, "note"));

        config.addQuadMapping(new DatabaseTable("overlap_test", "small"), graph, config.createIriMapping("small", "id"),
                config.createIriMapping("ex:flag"), config.createLiteralMapping(xsdString, "flag"));

        config.addQuadMapping(new DatabaseTable("overlap_test", "link"), graph,
                config.createIriMapping("compound", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("even", "dst"));
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


    private static Iri compound(int id)
    {
        return new Iri(iriPrefix + id);
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
    @DisplayName("a constant IRI of several classes matches the mappings of each of them")
    void constants() throws Exception
    {
        assertThat(execute("SELECT ?l WHERE { <http://example.org/compound/42> ex:label ?l }"),
                containsInAnyOrder(row(string("forty-two"))));

        assertThat(execute("SELECT ?n WHERE { <http://example.org/compound/42> ex:note ?n }"),
                containsInAnyOrder(row(string("XLII"))));

        assertThat(execute("SELECT ?f WHERE { <http://example.org/compound/42> ex:flag ?f }"),
                containsInAnyOrder(row(string("c"))));

        assertThat(execute("SELECT ?n WHERE { <http://example.org/compound/142> ex:note ?n }"),
                containsInAnyOrder(row(string("CXLII"))));

        assertThat(execute("SELECT ?f WHERE { <http://example.org/compound/142> ex:flag ?f }"), containsInAnyOrder());

        assertThat(execute("SELECT ?n WHERE { <http://example.org/compound/41> ex:note ?n }"), containsInAnyOrder());

        // the mappings of classes the constant does not belong to are pruned at translation time
        assertThat(translate("SELECT ?f WHERE { <http://example.org/compound/142> ex:flag ?f }"),
                not(containsString("\"small\"")));

        assertThat(translate("SELECT ?n WHERE { <http://example.org/compound/41> ex:note ?n }"),
                not(containsString("\"even\"")));

        assertThat(translate("SELECT ?l WHERE { <http://example.org/other/1> ex:label ?l }"),
                not(containsString("\"compound\"")));
    }


    @Test
    @DisplayName("a join of overlapping classes compares the terms in a common class")
    void join() throws Exception
    {
        assertThat(execute("SELECT ?s ?l ?n WHERE { ?s ex:label ?l . ?s ex:note ?n }"),
                containsInAnyOrder(row(compound(2), string("two"), string("II")),
                        row(compound(42), string("forty-two"), string("XLII")),
                        row(compound(142), string("hundred forty-two"), string("CXLII"))));

        assertThat(execute("SELECT ?s ?n ?f WHERE { ?s ex:note ?n . ?s ex:flag ?f }"), containsInAnyOrder(
                row(compound(2), string("II"), string("a")), row(compound(42), string("XLII"), string("c"))));

        assertThat(execute("SELECT ?s WHERE { ?s ex:label ?l . ?s ex:note ?n . ?s ex:flag ?f }"),
                containsInAnyOrder(row(compound(2)), row(compound(42))));

        // a subclass joined with its superclass keeps the subclass
        assertThat(execute("SELECT ?s ?l ?f WHERE { ?s ex:label ?l . ?s ex:flag ?f }"),
                containsInAnyOrder(row(compound(2), string("two"), string("a")),
                        row(compound(41), string("forty-one"), string("b")),
                        row(compound(42), string("forty-two"), string("c"))));
    }


    @Test
    @DisplayName("sameTerm and = between variables of overlapping classes")
    void comparison() throws Exception
    {
        assertThat(execute("SELECT ?l ?n WHERE { ?s ex:label ?l . ?t ex:note ?n FILTER(sameTerm(?s, ?t)) }"),
                containsInAnyOrder(row(string("two"), string("II")), row(string("forty-two"), string("XLII")),
                        row(string("hundred forty-two"), string("CXLII"))));

        assertThat(execute("SELECT ?n ?f WHERE { ?s ex:note ?n . ?t ex:flag ?f FILTER(?s = ?t) }"),
                containsInAnyOrder(row(string("II"), string("a")), row(string("XLII"), string("c"))));

        assertThat(execute("SELECT ?n ?f WHERE { ?s ex:note ?n . ?t ex:flag ?f FILTER(?s != ?t) }"),
                containsInAnyOrder(row(string("II"), string("b")), row(string("II"), string("c")),
                        row(string("XLII"), string("a")), row(string("XLII"), string("b")),
                        row(string("CXLII"), string("a")), row(string("CXLII"), string("b")),
                        row(string("CXLII"), string("c"))));
    }


    @Test
    @DisplayName("a constant compared with a variable of an overlapping class")
    void filter() throws Exception
    {
        assertThat(execute("SELECT ?l WHERE { ?s ex:label ?l FILTER(?s = <http://example.org/compound/42>) }"),
                containsInAnyOrder(row(string("forty-two"))));

        assertThat(execute("SELECT ?n WHERE { ?s ex:note ?n FILTER(?s = <http://example.org/compound/42>) }"),
                containsInAnyOrder(row(string("XLII"))));

        assertThat(execute("SELECT ?n WHERE { ?s ex:note ?n FILTER(?s = <http://example.org/compound/41>) }"),
                containsInAnyOrder());

        assertThat(execute("SELECT ?n WHERE { ?s ex:note ?n FILTER(?s != <http://example.org/compound/42>) }"),
                containsInAnyOrder(row(string("II")), row(string("CXLII"))));

        assertThat(execute("SELECT ?n WHERE { ?s ex:note ?n FILTER(STR(?s) = 'http://example.org/compound/142') }"),
                containsInAnyOrder(row(string("CXLII"))));

        assertThat(
                execute("SELECT ?f WHERE { ?s ex:flag ?f FILTER(?s IN (<http://example.org/compound/42>, "
                        + "<http://example.org/compound/142>, <http://example.org/other/1>)) }"),
                containsInAnyOrder(row(string("c"))));
    }


    @Test
    @DisplayName("VALUES of constants of different overlapping classes")
    void values() throws Exception
    {
        assertThat(execute("""
                SELECT ?s ?l WHERE { VALUES ?s { <http://example.org/compound/42> \
                <http://example.org/compound/41> <http://example.org/compound/141> <http://example.org/other/1> } \
                ?s ex:label ?l }"""), containsInAnyOrder(row(compound(42), string("forty-two")),
                row(compound(41), string("forty-one")), row(compound(141), string("hundred forty-one"))));

        assertThat(
                execute("SELECT ?s ?n WHERE { VALUES ?s { <http://example.org/compound/42> "
                        + "<http://example.org/compound/41> <http://example.org/compound/142> } ?s ex:note ?n }"),
                containsInAnyOrder(row(compound(42), string("XLII")), row(compound(142), string("CXLII"))));

        assertThat(execute("SELECT ?s WHERE { ?s ex:flag ?f VALUES ?s { <http://example.org/compound/42> "
                + "<http://example.org/compound/142> } }"), containsInAnyOrder(row(compound(42))));
    }


    @Test
    @DisplayName("UNION of overlapping classes keeps every term")
    void union() throws Exception
    {
        assertThat(execute("SELECT ?s WHERE { { ?s ex:note ?n } UNION { ?s ex:flag ?f } }"),
                containsInAnyOrder(row(compound(2)), row(compound(42)), row(compound(142)), row(compound(2)),
                        row(compound(41)), row(compound(42))));

        assertThat(execute("SELECT DISTINCT ?s WHERE { { ?s ex:note ?n } UNION { ?s ex:flag ?f } }"),
                containsInAnyOrder(row(compound(2)), row(compound(42)), row(compound(142)), row(compound(41))));

        assertThat(execute("SELECT ?s WHERE { { ?s ex:note ?n } UNION { ?s ex:flag ?f } } ORDER BY ?s"),
                contains(row(compound(142)), row(compound(2)), row(compound(2)), row(compound(41)), row(compound(42)),
                        row(compound(42))));

        assertThat(execute("SELECT ?s ?l WHERE { { ?s ex:note ?n } UNION { ?s ex:flag ?f } ?s ex:label ?l }"),
                containsInAnyOrder(row(compound(2), string("two")), row(compound(42), string("forty-two")),
                        row(compound(142), string("hundred forty-two")), row(compound(2), string("two")),
                        row(compound(41), string("forty-one")), row(compound(42), string("forty-two"))));

        assertThat(execute("SELECT (COUNT(DISTINCT ?s) AS ?c) WHERE { { ?s ex:note ?n } UNION { ?s ex:flag ?f } }"),
                containsInAnyOrder(row(integer(4))));

        assertThat(
                execute("SELECT ?s (COUNT(*) AS ?c) WHERE { { ?s ex:note ?n } UNION { ?s ex:flag ?f } } "
                        + "GROUP BY ?s"),
                containsInAnyOrder(row(compound(2), integer(2)), row(compound(42), integer(2)),
                        row(compound(142), integer(1)), row(compound(41), integer(1))));
    }


    @Test
    @DisplayName("OPTIONAL, MINUS and NOT EXISTS between overlapping classes")
    void optional() throws Exception
    {
        assertThat(execute("SELECT ?s ?n WHERE { ?s ex:label ?l OPTIONAL { ?s ex:note ?n } }"),
                containsInAnyOrder(row(compound(2), string("II")), row(compound(41), null),
                        row(compound(42), string("XLII")), row(compound(141), null),
                        row(compound(142), string("CXLII"))));

        assertThat(execute("SELECT ?s WHERE { ?s ex:label ?l MINUS { ?s ex:note ?n } }"),
                containsInAnyOrder(row(compound(41)), row(compound(141))));

        assertThat(execute("SELECT ?s WHERE { ?s ex:note ?n FILTER NOT EXISTS { ?s ex:flag ?f } }"),
                containsInAnyOrder(row(compound(142))));

        assertThat(execute("SELECT ?s WHERE { ?s ex:flag ?f FILTER EXISTS { ?s ex:note ?n } }"),
                containsInAnyOrder(row(compound(2)), row(compound(42))));
    }


    @Test
    @DisplayName("the same variable at two positions of overlapping classes in one pattern")
    void selfJoin() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:link ?x }"),
                containsInAnyOrder(row(compound(2)), row(compound(142))));

        assertThat(execute("SELECT ?x ?l WHERE { ?x ex:link ?x . ?x ex:label ?l }"),
                containsInAnyOrder(row(compound(2), string("two")), row(compound(142), string("hundred forty-two"))));

        assertThat(execute("SELECT ?x ?f WHERE { ?x ex:link ?x . ?x ex:flag ?f }"),
                containsInAnyOrder(row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("terms of overlapping classes are generated and sorted")
    void order() throws Exception
    {
        assertThat(execute("SELECT ?s ?o WHERE { ?s ex:link ?o } ORDER BY ?o ?s"),
                contains(row(compound(142), compound(142)), row(compound(2), compound(2)),
                        row(compound(41), compound(2)), row(compound(42), compound(41))));

        assertThat(execute("SELECT DISTINCT ?o WHERE { ?s ex:link ?o }"),
                containsInAnyOrder(row(compound(2)), row(compound(41)), row(compound(142))));
    }
}
