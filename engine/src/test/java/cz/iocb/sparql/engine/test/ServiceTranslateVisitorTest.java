package cz.iocb.sparql.engine.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.model.pattern.Service;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.translator.ServiceTranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The SPARQL text sent to a remote endpoint for the pattern of a SERVICE call has to be the pattern as it was written:
 * every pattern of the list is parsed as the pattern of a call, written back by {@link ServiceTranslateVisitor}, and
 * the text is evaluated by Jena over fixed triples next to the pattern itself, which has to give the same solutions.
 * The patterns cover the triples and what the parser expands into them (property and object lists, collections, blank
 * node property lists, reified triples and annotations), property paths, sub-selects with their modifiers, groups,
 * VALUES, expressions and literals.
 *
 * The triples are those of the items 1 to 5 and 9 with the values ten times the item ({@code ex:rv}), the labels of the
 * items 1, 3 and 9, the links between the items, texts with characters that have to be escaped, names with language
 * tags, flags, a collection, a triple term with a reifier, and two triples in the named graph {@code ex:g1}.
 */
public class ServiceTranslateVisitorTest
{
    /**
     * Namespace of the example IRIs.
     */
    private static final String namespace = "http://example.org/";

    /**
     * Namespace of the RDF vocabulary.
     */
    private static final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Engine parsing the queries; its configuration maps nothing.
     */
    private static Engine engine = null;

    /**
     * The triples the patterns are evaluated over.
     */
    private static Dataset dataset = null;


    /**
     * Creates the engine and the triples.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        engine = new Engine(new SparqlDatabaseConfiguration("http://example.org/sparql", connectionPool,
                new DatabaseSchema(connectionPool), true));

        Model model = ModelFactory.createDefaultModel();
        Graph graph = model.getGraph();

        for(int item : new int[] { 1, 2, 3, 4, 5, 9 })
            graph.add(Triple.create(item(item), iri("rv"), integer(10 * item)));

        for(int item : new int[] { 1, 3, 9 })
            graph.add(Triple.create(item(item), iri("rlabel"), NodeFactory.createLiteralString("L" + item)));

        graph.add(Triple.create(item(1), iri("rlabel"), NodeFactory.createLiteralString("K1")));

        for(int[] link : new int[][] { { 1, 2 }, { 1, 3 }, { 2, 3 }, { 4, 9 }, { 9, 1 }, { 3, 5 } })
            graph.add(Triple.create(item(link[0]), iri("rlink"), item(link[1])));

        String[] texts = { "a'b", "line\nbreak", "tab\there", "back\\slash", "quote\"x", "cr\rhere", "plain" };

        for(int i = 0; i < texts.length; i++)
            graph.add(Triple.create(item(i + 1), iri("rtext"), NodeFactory.createLiteralString(texts[i])));

        graph.add(Triple.create(item(1), iri("rname"), NodeFactory.createLiteralLang("name", "en")));
        graph.add(Triple.create(item(2), iri("rname"), NodeFactory.createLiteralLang("jmeno", "cs-CZ")));
        graph.add(Triple.create(item(1), iri("rflag"), NodeFactory.createLiteralDT("true", XSDDatatype.XSDboolean)));
        graph.add(Triple.create(item(2), iri("rflag"), NodeFactory.createLiteralDT("false", XSDDatatype.XSDboolean)));
        graph.add(Triple.create(item(1), NodeFactory.createURI(rdf + "type"), iri("Thing")));

        // the collection ( 1 2 3 )
        Node[] cells = { NodeFactory.createBlankNode(), NodeFactory.createBlankNode(), NodeFactory.createBlankNode(),
                NodeFactory.createURI(rdf + "nil") };

        graph.add(Triple.create(item(1), iri("rlist"), cells[0]));

        for(int i = 0; i < 3; i++)
        {
            graph.add(Triple.create(cells[i], NodeFactory.createURI(rdf + "first"), integer(i + 1)));
            graph.add(Triple.create(cells[i], NodeFactory.createURI(rdf + "rest"), cells[i + 1]));
        }

        // a triple term and a reifier of it
        Node term = NodeFactory.createTripleTerm(item(1), iri("rv"), integer(10));
        Node reifier = NodeFactory.createBlankNode();

        graph.add(Triple.create(item(3), iri("rsays"), term));
        graph.add(Triple.create(reifier, NodeFactory.createURI(rdf + "reifies"), term));
        graph.add(Triple.create(reifier, iri("rcertainty"), integer(9)));

        Model named = ModelFactory.createDefaultModel();
        named.getGraph().add(Triple.create(item(1), iri("gv"), integer(1)));
        named.getGraph().add(Triple.create(item(2), iri("gv"), integer(2)));

        dataset = DatasetFactory.create(model);
        dataset.addNamedModel(namespace + "g1", named);
    }


    /**
     * IRI of the example namespace.
     *
     * @param name the local name
     * @return IRI of the example namespace
     */
    private static Node iri(String name)
    {
        return NodeFactory.createURI(namespace + name);
    }


    /**
     * IRI of the item.
     *
     * @param id identifier of the item
     * @return IRI of the item
     */
    private static Node item(int id)
    {
        return NodeFactory.createURI(namespace + "item/" + id);
    }


    /**
     * The xsd:integer literal of the value.
     *
     * @param value the value
     * @return the xsd:integer literal of the value
     */
    private static Node integer(int value)
    {
        return NodeFactory.createLiteralDT(Integer.toString(value), XSDDatatype.XSDinteger);
    }


    /**
     * The text sent to a remote endpoint for the pattern.
     *
     * @param pattern the pattern of a SERVICE call, without the braces
     * @return the text sent to a remote endpoint for the pattern
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the pattern has errors
     */
    private static String getText(String pattern) throws SQLException, TranslateExceptions
    {
        String query = "SELECT * WHERE { SERVICE <http://remote.example.org/sparql> { " + pattern + " } }";

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            List<Service> services = new ArrayList<>();

            new ElementVisitor<Void>()
            {
                @Override
                public Void visit(Service service)
                {
                    services.add(service);
                    return null;
                }
            }.visitElement(prepared.getSyntaxTree().getSelect().getPattern());

            return new ServiceTranslateVisitor().getResultCode(services.get(0).getPattern());
        }
    }


    /**
     * The solutions Jena gives for the query over the triples, each as its variables with their terms, sorted.
     *
     * @param query the query, without the prefix declarations
     * @return the solutions of the query
     */
    private static List<String> evaluate(String query)
    {
        try(QueryExecution execution = QueryExecution.dataset(dataset).query(prefix + query).build())
        {
            ResultSet results = execution.execSelect();
            List<String> rows = new ArrayList<>();

            while(results.hasNext())
            {
                QuerySolution solution = results.next();
                TreeMap<String, String> row = new TreeMap<>();

                for(String name : results.getResultVars())
                {
                    RDFNode node = solution.get(name);

                    // the labels of blank nodes differ from an evaluation to another
                    if(node != null)
                        row.put(name, node.asNode().isBlank() ? "_:" : node.asNode().toString());
                }

                rows.add(row.toString());
            }

            rows.sort(null);

            return rows;
        }
    }


    /**
     * The patterns of the calls, one on a line; the lines starting with a hash sign name the groups of the patterns.
     */
    private static final String patterns = """
            # triples and what the parser expands into them
            ?s ex:rv ?r ; ex:rlabel ?l
            ?s ex:rlink ?a , ?b
            ?s ex:rlink [ ex:rlink [ ex:rv ?r ] ]
            _:b ex:rv ?r . _:b ex:rlabel ?l
            ?s a ?t
            ?s ex:rlist ( 1 ?x 3 )
            ?s ex:rsays <<( ?a ex:rv ?r )>>
            << ?a ex:rv ?r >> ex:rcertainty ?c
            ?a ex:rv ?r ~ ?reifier {| ex:rcertainty ?c |}
            # property paths
            ?s ex:rlink/ex:rlink ?t
            ?s ex:rlink+ ?t
            ?s ex:rlink* ?t
            ?s ^ex:rlink ?t
            ?s !(ex:rv|^ex:rlink) ?t
            ?s (ex:rlink/ex:rlink)+ ?t
            ?s ^(ex:rlink/ex:rlink) ?t
            ?s (ex:rlink|ex:rlabel)/ex:rv ?t
            ?s ex:rlink|ex:rlink/ex:rv ?t
            ?s (ex:rlink/ex:rlink)? ?t
            # sub-selects
            SELECT ?s (COUNT(*) AS ?c) WHERE { ?s ex:rlink ?q } GROUP BY ?s
            SELECT ?s (SUM(?r) + 1 AS ?t) (MAX(?r) AS ?m) WHERE { ?s ex:rlink ?q . ?q ex:rv ?r } GROUP BY ?s
            SELECT ?s (COUNT(*) AS ?c) WHERE { ?s ex:rlink ?q } GROUP BY ?s HAVING (COUNT(*) > 0) (COUNT(*) < 2)
            SELECT ?s WHERE { ?s ex:rlink ?q } GROUP BY ?s HAVING BOUND(?s)
            SELECT ?k (COUNT(*) AS ?c) WHERE { ?s ex:rv ?r } GROUP BY (?r > 20 AS ?k)
            SELECT (COUNT(DISTINCT ?t) AS ?c) (MIN(?t) AS ?x) WHERE { ?s ex:rlink ?t }
            SELECT (GROUP_CONCAT(DISTINCT ?l; SEPARATOR=', ') AS ?g) WHERE { item:3 ex:rlabel ?l }
            SELECT ?s ?r WHERE { ?s ex:rv ?r } ORDER BY ?r LIMIT 2
            SELECT ?s ?r WHERE { ?s ex:rv ?r } ORDER BY ?r LIMIT 2 OFFSET 1
            SELECT ?s ?r WHERE { ?s ex:rv ?r } ORDER BY ?r OFFSET 4
            SELECT ?s ?r WHERE { ?s ex:rv ?r } ORDER BY DESC(?r + 1) ?s LIMIT 2 OFFSET 1
            SELECT ?s ?r WHERE { ?s ex:rv ?r } ORDER BY (?r * -1) STR(?s) LIMIT 2
            SELECT DISTINCT ?t WHERE { ?s ex:rlink ?t }
            SELECT ?s ?r WHERE { ?s ex:rv ?r } VALUES ?s { item:1 item:9 }
            ?s ex:rv ?r { SELECT ?s WHERE { ?s ex:rlabel ?l } ORDER BY ?s LIMIT 1 OFFSET 1 }
            # groups
            ?s ex:rv ?r OPTIONAL { ?s ex:rlabel ?l FILTER(?r > 10) }
            ?s ex:rv ?r MINUS { ?s ex:rlabel ?l }
            { ?s ex:rv ?r } UNION { ?s ex:rlabel ?r } UNION { ?s ex:rlink ?r }
            ?s ex:rv ?r GRAPH ?g { ?s ex:gv ?x }
            GRAPH ex:g1 { ?s ex:gv ?r }
            VALUES (?s ?r) { (item:1 10) (UNDEF 20) } ?s ex:rv ?r
            ?s ex:rv ?r BIND(?r + 1 AS ?b) BIND(?b * 2 AS ?c)
            # expressions
            ?s ex:rv ?r FILTER(?r NOT IN (10, 20))
            ?s ex:rv ?r FILTER(?r IN (10, 20))
            ?s ex:rv ?r FILTER(?r NOT IN ())
            ?s ex:rv ?r FILTER((?r + 10) * 2 > 70)
            ?s ex:rv ?r FILTER(?r + 10 * 2 > 45)
            ?s ex:rv ?r FILTER(!(?r > 20 || ?r = 10))
            ?s ex:rv ?r FILTER(-?r < -20 && ?r - -5 > 30)
            ?s ex:rv ?r FILTER(?r - (10 - 10) = 10)
            ?s ex:rv ?r FILTER((?r = 10 || ?r = 20) && ?r > 15)
            ?s ex:rv ?r FILTER(?r * 1.5 > 2.5e1)
            ?s ex:rv ?r FILTER(REGEX(STR(?s), 'ITEM/[13]$', 'i'))
            ?s ex:rv ?r OPTIONAL { ?s ex:rlabel ?l } BIND(COALESCE(?l, IF(?r > 20, 'big', 'small')) AS ?x)
            ?s ex:rv ?r BIND(xsd:string(?r) AS ?x)
            ?s ex:rv ?r FILTER NOT EXISTS { ?s ex:rlink ?t }
            ?s ex:rv ?r FILTER(EXISTS { ?s ex:rlabel ?l } && ?r > 10)
            ?s ex:rsays ?t FILTER(?t = <<( item:1 ex:rv 10 )>>) BIND(SUBJECT(?t) AS ?a)
            # literals
            ?s ex:rtext ?l FILTER(?l = "a'b")
            ?s ex:rtext ?l FILTER(?l = "line\\nbreak")
            ?s ex:rtext ?l FILTER(?l = "tab\\there")
            ?s ex:rtext ?l FILTER(?l = "back\\\\slash")
            ?s ex:rtext ?l FILTER(?l = "quote\\"x")
            ?s ex:rtext ?l FILTER(?l = 'cr\\rhere')
            ?s ex:rname ?l FILTER(?l = 'jmeno'@cs-CZ)
            ?s ex:rv ?r FILTER(?r = '10'^^xsd:int)
            ?s ex:rflag ?f FILTER(?f = false || ?f)
            """;


    /**
     * The patterns of the calls.
     *
     * @return the patterns of the calls
     */
    static Stream<String> getPatterns()
    {
        return patterns.lines().filter(line -> !line.startsWith("#"));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getPatterns")
    @DisplayName("the text sent for a pattern has the solutions of the pattern")
    void solutions(String pattern) throws Exception
    {
        List<String> expected = evaluate("SELECT * WHERE { " + pattern + " }");

        assertThat(expected, is(not(empty())));
        assertThat(evaluate("SELECT * WHERE " + getText(pattern)), is(expected));
    }


    @Test
    @DisplayName("an expression of a projection stands before its variable")
    void projection() throws Exception
    {
        assertThat(getText("SELECT ?s (COUNT(*) AS ?c) WHERE { ?s ex:rlink ?q } GROUP BY ?s"),
                containsString("( COUNT(*) as  ?c )"));
    }


    @Test
    @DisplayName("an ordering condition without a direction is written in brackets")
    void order() throws Exception
    {
        String text = getText("SELECT ?s WHERE { ?s ex:rv ?r } ORDER BY ?r DESC(?s) LIMIT 2 OFFSET 1");

        assertThat(text, containsString(" order by ( ?r )DESC( ?s ) limit 2 offset 1"));
    }


    @Test
    @DisplayName("the negation of IN is kept")
    void notIn() throws Exception
    {
        assertThat(getText("?s ex:rv ?r FILTER(?r NOT IN (10))"), containsString(" ?r  not in ("));
        assertThat(getText("?s ex:rv ?r FILTER(?r IN (10))"), not(containsString(" not ")));
    }


    @Test
    @DisplayName("the modifier of a nested call and the DISTINCT of a function are kept")
    void modifiers() throws Exception
    {
        assertThat(getText("SERVICE SILENT <http://other.example.org/sparql> { ?s ex:rv ?r }"),
                containsString(" service silent <http://other.example.org/sparql>{"));

        assertThat(getText("SELECT (<http://example.org/agg>(DISTINCT ?r) AS ?x) WHERE { ?s ex:rv ?r }"),
                containsString("<http://example.org/agg>( distinct  ?r )"));
    }
}
