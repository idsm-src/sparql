package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
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
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
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
 * A property mapped twice over one table, as a plain string when the language column is null and as a language-tagged
 * string otherwise, is a union of two accesses to the table whose conditions are complementary. The union is written as
 * a single scan of the table with the columns of each class guarded by the null test, so that the database can look the
 * rows up by an index and estimate the joins from the column statistics, which a UNION ALL of two filtered scans allows
 * neither.
 */
public class UnionScanMergeTest
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
     * Creates the {@code union_scan_merge_test} schema: the items referring to the resources {@code x}, {@code y},
     * {@code z} and {@code q}, and the labels of the resources with an optional language. The labels are mapped as
     * {@code rdfs:label} by the complementary pair of mappings (plain strings where the language is null, tagged
     * strings elsewhere), and as {@code ex:name} by a pair that is not complementary (the tagged strings of the
     * language {@code en} only).
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema union_scan_merge_test");
            statement.execute("create table union_scan_merge_test.items (item varchar not null, ref varchar not null)");
            statement.execute("insert into union_scan_merge_test.items values ('i', 'x'), ('j', 'y'), ('k', 'z'), "
                    + "('l', 'q')");
            statement.execute("create table union_scan_merge_test.labels (id varchar not null, "
                    + "label varchar not null, language varchar)");
            statement.execute("insert into union_scan_merge_test.labels values ('x', 'ax', null), "
                    + "('x', 'ax-en', 'en'), ('y', 'by', null), ('z', 'cz-de', 'de')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        config.addIriClass(new StringUserIriClass("item", "http://example.org/item/", "[a-z]"));
        config.addIriClass(new StringUserIriClass("a", "http://example.org/a/", "[a-z]"));

        DatabaseTable items = new DatabaseTable("union_scan_merge_test", "items");
        DatabaseTable labels = new DatabaseTable("union_scan_merge_test", "labels");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(items, graph, config.createIriMapping("item", "item"),
                config.createIriMapping("ex:refers"), config.createIriMapping("a", "ref"));

        config.addQuadMapping(labels, graph, config.createIriMapping("a", "id"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "label"), config.createIsNullCondition(labels, "language"));
        config.addQuadMapping(labels, graph, config.createIriMapping("a", "id"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(rdfLangString, "label", "language"));

        config.addQuadMapping(labels, graph, config.createIriMapping("a", "id"), config.createIriMapping("ex:name"),
                config.createLiteralMapping(xsdString, "label"), config.createIsNullCondition(labels, "language"));
        config.addQuadMapping(labels, graph, config.createIriMapping("a", "id"), config.createIriMapping("ex:name"),
                config.createLiteralMapping(rdfLangString, "label", "language"),
                config.createAreEqualCondition(labels, "language", "'en'::varchar"));
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
     * Plain string literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Language-tagged string literal.
     */
    private static LangStringLiteral tagged(String value, String language)
    {
        return new LangStringLiteral(value, language);
    }


    @Test
    @DisplayName("an optional pair of complementary mappings is read by a single scan")
    void optionalLabels() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { ?s ex:refers ?x OPTIONAL { ?x rdfs:label ?l } }";

        assertThat(execute(query),
                containsInAnyOrder(row(item("i"), string("ax")), row(item("i"), tagged("ax-en", "en")),
                        row(item("j"), string("by")), row(item("k"), tagged("cz-de", "de")), row(item("l"), null)));

        String sql = translate(query);

        assertThat(sql, not(containsString("UNION ALL")));
        assertThat(sql, containsString("CASE WHEN"));
    }


    @Test
    @DisplayName("a filter on the language keeps the rows of the right class only")
    void optionalLanguageFilter() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { ?s ex:refers ?x OPTIONAL { ?x rdfs:label ?l FILTER(lang(?l) = 'en') } }";

        assertThat(execute(query), containsInAnyOrder(row(item("i"), tagged("ax-en", "en")), row(item("j"), null),
                row(item("k"), null), row(item("l"), null)));
        assertThat(translate(query), not(containsString("UNION ALL")));
    }


    @Test
    @DisplayName("an optional sub-select over the pair is read by a single scan")
    void optionalSubSelect() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { ?s ex:refers ?x OPTIONAL { SELECT ?x ?l WHERE { ?x rdfs:label ?l } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(item("i"), string("ax")), row(item("i"), tagged("ax-en", "en")),
                        row(item("j"), string("by")), row(item("k"), tagged("cz-de", "de")), row(item("l"), null)));
        assertThat(translate(query), not(containsString("UNION ALL")));
    }


    @Test
    @DisplayName("a subtracted pair is read by a single scan")
    void minusLabels() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:refers ?x MINUS { ?x rdfs:label ?l } }";

        assertThat(execute(query), containsInAnyOrder(row(item("l"))));
        assertThat(translate(query), not(containsString("UNION ALL")));
    }


    @Test
    @DisplayName("distinct values of both classes are found")
    void distinctLabels() throws Exception
    {
        String query = "SELECT DISTINCT ?l WHERE { ?s ex:refers ?x . ?x rdfs:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(string("ax")), row(tagged("ax-en", "en")), row(string("by")),
                row(tagged("cz-de", "de"))));
    }


    @Test
    @DisplayName("a pair of mappings that is not complementary stays a union")
    void unmergedUnion() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { ?s ex:refers ?x OPTIONAL { ?x ex:name ?n } }";

        assertThat(execute(query),
                containsInAnyOrder(row(item("i"), string("ax")), row(item("i"), tagged("ax-en", "en")),
                        row(item("j"), string("by")), row(item("k"), null), row(item("l"), null)));
        assertThat(translate(query), containsString("UNION ALL"));
    }
}
