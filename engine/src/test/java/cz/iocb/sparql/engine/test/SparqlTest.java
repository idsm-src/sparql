package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.error.MessageCategory.ERROR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedShort;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantBlankNodeMapping;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantLiteralMapping;
import cz.iocb.sparql.engine.mapping.ConstantTripleTermMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.CanonicalLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.DirLangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.SubsetLiteralClass;
import cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.nextprot.combined.NeXtProtCombinedConfiguration;
import cz.iocb.sparql.nextprot.integer.NeXtProtIntegerConfiguration;
import cz.iocb.sparql.nextprot.string.NeXtProtStringConfiguration;
import cz.iocb.sparql.testing.Database;



/**
 * Conformance tests driven by the W3C-style manifests under {@code src/test/resources/sparql11} and
 * {@code src/test/resources/sparql12}: syntax tests check that a query is or is not accepted, evaluation tests load the
 * test data as constant quad mappings of a fresh configuration and compare the results with the expected {@code .srx}
 * or {@code .ttl} file. The NeXtProt families only translate and run the queries of {@code nextprot/queryset.sparql}
 * against the three NeXtProt configurations. Requires the {@link Database} container.
 */
@DisplayName("SPARQL Tests")
public class SparqlTest
{
    /**
     * Quad of the test data as read by Jena; a null graph denotes the default graph.
     */
    public record Quad(Node graph, Node subject, Node predicate, Node object)
    {
    }


    /**
     * Expected result of a test: the rows and the variables giving their columns, null for a graph or a boolean whose
     * rows are compared positionally.
     */
    public record ExpectedResult(List<String> variables, List<List<RdfTerm>> rows)
    {
    }


    /**
     * Class of the blank nodes of the test data.
     */
    static final StrBlankNodeInSegmentClass bnodeClass = new StrBlankNodeInSegmentClass(0);

    /**
     * Replacement of each built-in literal class by a {@link SubsetLiteralClass}, used by the subset literal family.
     */
    static final Map<ResourceClass, ResourceClass> literalClassMap = new HashMap<>();


    /**
     * The subset class standing in for the given built-in literal class, named {@code <original>_sub}; it keeps all the
     * literals of the original, so that the test data can be mapped to it without a change.
     */
    private static SubsetLiteralClass subset(CanonicalLiteralClass original)
    {
        return new SubsetLiteralClass(original.getResourceName() + "_sub", original);
    }

    /**
     * Pool of the test database.
     */
    static DataSource connectionPool = null;

    /**
     * Catalog of the test database.
     */
    static DatabaseSchema schema = null;

    /**
     * All manifests merged into one model.
     */
    static Model model = null;

    /**
     * Engine over the NeXtProt string configuration.
     */
    static Engine stringEngine = null;

    /**
     * Engine over the NeXtProt integer configuration.
     */
    static Engine integerEngine = null;

    /**
     * Engine over the NeXtProt combined configuration.
     */
    static Engine combinedEngine = null;


    /**
     * Creates the NeXtProt engines, reads every {@code manifest.ttl} into the model and prepares the subset literal
     * classes.
     */
    @BeforeAll
    static void init() throws FileNotFoundException, IOException, SQLException
    {
        connectionPool = Database.getPool();
        schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration stringConfig = new NeXtProtStringConfiguration(null, connectionPool, schema);
        SparqlDatabaseConfiguration integerConfig = new NeXtProtIntegerConfiguration(null, connectionPool, schema);
        SparqlDatabaseConfiguration combinedConfig = new NeXtProtCombinedConfiguration(null, connectionPool, schema);

        stringEngine = new Engine(stringConfig);
        integerEngine = new Engine(integerConfig);
        combinedEngine = new Engine(combinedConfig);

        model = ModelFactory.createDefaultModel();

        for(String name : List.of("sparql11", "sparql12"))
        {
            File directory = new File("src/test/resources/" + name);

            for(File subdirectory : directory.listFiles())
            {
                if(!subdirectory.isDirectory())
                    continue;

                for(File manifest : subdirectory.listFiles())
                {
                    if(!manifest.isFile() || !manifest.getName().equals("manifest.ttl"))
                        continue;

                    Model m = ModelFactory.createDefaultModel();
                    m.read(new FileReader(manifest), subdirectory.getCanonicalPath() + File.separator, "TTL");

                    model.add(m);
                }
            }
        }


        literalClassMap.put(xsdBoolean, subset(xsdBoolean));
        literalClassMap.put(xsdByte, subset(xsdByte));
        literalClassMap.put(xsdUnsignedByte, subset(xsdUnsignedByte));
        literalClassMap.put(xsdShort, subset(xsdShort));
        literalClassMap.put(xsdUnsignedShort, subset(xsdUnsignedShort));
        literalClassMap.put(xsdInt, subset(xsdInt));
        literalClassMap.put(xsdUnsignedInt, subset(xsdUnsignedInt));
        literalClassMap.put(xsdLong, subset(xsdLong));
        literalClassMap.put(xsdUnsignedLong, subset(xsdUnsignedLong));
        literalClassMap.put(xsdInteger, subset(xsdInteger));
        literalClassMap.put(xsdNonPositiveInteger, subset(xsdNonPositiveInteger));
        literalClassMap.put(xsdNegativeInteger, subset(xsdNegativeInteger));
        literalClassMap.put(xsdNonNegativeInteger, subset(xsdNonNegativeInteger));
        literalClassMap.put(xsdPositiveInteger, subset(xsdPositiveInteger));
        literalClassMap.put(xsdDecimal, subset(xsdDecimal));
        literalClassMap.put(xsdFloat, subset(xsdFloat));
        literalClassMap.put(xsdDouble, subset(xsdDouble));
        literalClassMap.put(xsdString, subset(xsdString));
    }


    /**
     * Closes the connection pool.
     */
    @AfterAll
    static void close()
    {
        if(connectionPool != null)
            connectionPool.close();
    }


    /**
     * Positive syntax tests: the query must produce no error message; an extension function used by the tests is
     * registered under {@code http://example/function}.
     */
    @DisplayName("Positive Syntax Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getPositiveSyntaxTests")
    void doPositiveSyntaxTests(String name, String query) throws SQLException
    {
        String function = "http://example/function";

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        config.addFunction(new FunctionDefinition(function, null, xsdString, List.of(xsdString), false, true));
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            Assertions.assertTrue(request.check(query).stream().noneMatch(m -> m.getCategory() == ERROR));
        }
    }


    /**
     * Negative syntax tests: the query must produce an error message.
     */
    @DisplayName("Negative Syntax Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getNegativeSyntaxTests")
    void doNegativeSyntaxTests(String name, String query) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            Assertions.assertTrue(request.check(query).stream().anyMatch(m -> m.getCategory() == ERROR));
        }
    }


    /**
     * Syntax tests of evaluation queries: the query must translate and run against an empty configuration.
     */
    @DisplayName("Query Evaluation Syntax Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getQueryEvaluationSyntaxTests")
    void doQueryEvaluationSyntaxTests(String name, String query)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            request.execute(query);
        }
    }


    /**
     * Evaluation tests: the test data become constant quad mappings and the result must match the expected rows in any
     * order.
     */
    @DisplayName("Query Evaluation Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getQueryEvaluationTests")
    void doQueryEvaluationTests(String name, String query, List<Quad> quads, ExpectedResult expected)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        for(Quad quad : quads)
            config.addQuadMapping((ConstantIriMapping) getMapping(quad.graph, config), getMapping(quad.subject, config),
                    (ConstantIriMapping) getMapping(quad.predicate, config), getMapping(quad.object, config));

        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            List<List<RdfTerm>> result = getResult(request.execute(query), expected.variables());

            MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expected.rows().toArray()));
        }
    }


    /**
     * Evaluation tests with the literals of the test data mapped to {@link SubsetLiteralClass}es, exercising the
     * conversions between subclasses and their built-in superclasses.
     */
    @DisplayName("Query Evaluation Tests (with subset literals)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getQueryEvaluationTests")
    void doQueryEvaluationTestsWithLiteralMap(String name, String query, List<Quad> quads, ExpectedResult expected)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        for(Quad quad : quads)
            config.addQuadMapping((ConstantIriMapping) getMapping(quad.graph, config), getMapping(quad.subject, config),
                    (ConstantIriMapping) getMapping(quad.predicate, config),
                    getMapping(quad.object, config, literalClassMap));

        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            List<List<RdfTerm>> result = getResult(request.execute(query), expected.variables());

            MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expected.rows().toArray()));
        }
    }


    /**
     * NeXtProt queries must translate and run against the string configuration.
     */
    @DisplayName("NeXtProt String Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getNextProtTests")
    void doNextProtStringTests(String name, String query)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        try(Request request = stringEngine.getRequest())
        {
            request.execute(query);
        }
    }


    /**
     * NeXtProt queries must translate and run against the integer configuration.
     */
    @DisplayName("NeXtProt Integer Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getNextProtTests")
    void doNextProtIntegerTests(String name, String query)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        try(Request request = integerEngine.getRequest())
        {
            request.execute(query);
        }
    }



    /**
     * NeXtProt queries must translate and run against the combined configuration.
     */
    @DisplayName("NeXtProt Combined Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getNextProtTests")
    void doNextProtCombinedTests(String name, String query)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        try(Request request = combinedEngine.getRequest())
        {
            request.execute(query);
        }
    }


    /**
     * Names and texts of the {@code mf:PositiveSyntaxTest11} and {@code mf:PositiveSyntaxTest} entries of the
     * manifests.
     */
    static List<Arguments> getPositiveSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<>();

        Query info = QueryFactory.create("""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>

                SELECT ?NAME ?QUERY WHERE
                {
                  VALUES ?TYPE { mf:PositiveSyntaxTest11 mf:PositiveSyntaxTest }
                  ?MANIFEST rdf:type mf:Manifest; mf:entries / rdf:rest* / rdf:first ?TEST.
                  ?TEST rdf:type ?TYPE; mf:name ?NAME; mf:action ?QUERY.
                }
                ORDER BY ?NAME
                """);

        try(QueryExecution qexec = QueryExecutionFactory.create(info, model))
        {
            ResultSet tests = qexec.execSelect();

            while(tests.hasNext())
            {
                QuerySolution test = tests.nextSolution();

                Path queryPath = Paths.get(new URI(test.get("?QUERY").asResource().getURI()).getPath());
                String query = new String(Files.readAllBytes(queryPath));
                String name = test.get("?NAME").asLiteral().getLexicalForm();

                queries.add(Arguments.of(name, query));
            }
        }

        return queries;
    }


    /**
     * Names and texts of the {@code mf:NegativeSyntaxTest11} and {@code mf:NegativeSyntaxTest} entries of the
     * manifests.
     */
    static List<Arguments> getNegativeSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<>();

        Query info = QueryFactory.create("""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>

                SELECT ?NAME ?QUERY WHERE
                {
                  VALUES ?TYPE { mf:NegativeSyntaxTest11 mf:NegativeSyntaxTest }
                  ?MANIFEST rdf:type mf:Manifest; mf:entries / rdf:rest* / rdf:first ?TEST.
                  ?TEST rdf:type ?TYPE; mf:name ?NAME; mf:action ?QUERY.
                }
                ORDER BY ?NAME
                """);

        try(QueryExecution qexec = QueryExecutionFactory.create(info, model))
        {
            ResultSet tests = qexec.execSelect();

            while(tests.hasNext())
            {
                QuerySolution test = tests.nextSolution();

                Path queryPath = Paths.get(new URI(test.get("?QUERY").asResource().getURI()).getPath());
                String query = new String(Files.readAllBytes(queryPath));
                String name = test.get("?NAME").asLiteral().getLexicalForm();

                queries.add(Arguments.of(name, query));
            }
        }

        return queries;
    }


    /**
     * Names and texts of the {@code mf:QueryEvaluationTest} entries, used for syntax checking only.
     */
    static List<Arguments> getQueryEvaluationSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<>();

        Query info = QueryFactory.create("""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>
                PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>

                SELECT ?NAME ?QUERY WHERE
                {
                  ?MANIFEST rdf:type mf:Manifest; mf:entries / rdf:rest* / rdf:first ?TEST.
                  ?TEST rdf:type mf:QueryEvaluationTest; mf:name ?NAME; mf:action / qt:query ?QUERY.
                }
                ORDER BY ?NAME
                """);

        try(QueryExecution qexec = QueryExecutionFactory.create(info, model))
        {
            ResultSet tests = qexec.execSelect();

            while(tests.hasNext())
            {
                QuerySolution test = tests.nextSolution();

                Path queryPath = Paths.get(new URI(test.get("?QUERY").asResource().getURI()).getPath());
                String query = new String(Files.readAllBytes(queryPath));
                String name = test.get("?NAME").asLiteral().getLexicalForm();

                queries.add(Arguments.of(name, query));
            }
        }

        return queries;
    }


    /**
     * Names, texts, data quads (default and named graphs) and expected results of the {@code mf:QueryEvaluationTest}
     * entries.
     */
    static List<Arguments> getQueryEvaluationTests()
            throws URISyntaxException, IOException, ParserConfigurationException, SAXException
    {
        List<Arguments> queries = new LinkedList<>();

        Query info = QueryFactory.create("""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>
                PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>

                SELECT ?TEST ?NAME ?QUERY ?DATA ?RESULT WHERE
                {
                  ?MANIFEST rdf:type mf:Manifest; mf:entries / rdf:rest* / rdf:first ?TEST.
                  ?TEST rdf:type mf:QueryEvaluationTest; mf:name ?NAME; mf:action ?ACTION; mf:result ?RESULT.
                  ?ACTION qt:query ?QUERY.
                  optional { ?ACTION qt:data ?DATA }
                }
                ORDER BY ?NAME
                """);

        try(QueryExecution qexec = QueryExecutionFactory.create(info, model))
        {
            ResultSet tests = qexec.execSelect();

            while(tests.hasNext())
            {
                QuerySolution test = tests.nextSolution();

                Path queryPath = Paths.get(new URI(test.get("?QUERY").asResource().getURI()).getPath());
                String query = new String(Files.readAllBytes(queryPath));
                String name = test.get("?NAME").asLiteral().getLexicalForm();
                List<Quad> data = new ArrayList<>();
                ExpectedResult expected = getResult(test.get("?RESULT"));

                if(test.get("DATA") != null)
                    data.addAll(getQuads(test.get("DATA"), true));

                Query graphs = QueryFactory.create(String.format("""
                        PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>
                        PREFIX qt: <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>

                        SELECT ?G WHERE
                        {
                          <%s> mf:action / qt:graphData ?G.
                        }
                        """, test.get("?TEST").asResource().getURI()));

                try(QueryExecution dexec = QueryExecutionFactory.create(graphs, model))
                {
                    ResultSet it = dexec.execSelect();

                    while(it.hasNext())
                        data.addAll(getQuads(it.nextSolution().get("?G"), false));
                }

                queries.add(Arguments.of(name, query, data, expected));
            }
        }

        return queries;
    }


    /**
     * Queries of {@code nextprot/queryset.sparql}, each introduced by a {@code ### id ###} line.
     */
    static List<Arguments> getNextProtTests() throws URISyntaxException, IOException
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream("nextprot/queryset.sparql");

        List<Arguments> queries = new LinkedList<>();

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            String line = null;

            String id = null;
            StringBuffer query = null;

            while((line = reader.readLine()) != null)
            {
                if(line.startsWith("### "))
                {
                    if(id != null)
                        queries.add(Arguments.of(id, query.toString()));

                    id = line.substring(4, line.length() - 4);
                    query = new StringBuffer();
                }
                else if(!line.isEmpty())
                {
                    query.append(line);
                    query.append('\n');
                }
            }

            if(id != null)
                queries.add(Arguments.of(id, query.toString()));
        }

        return queries;
    }


    /**
     * Quads of a data file: the triples of its default graph in the default graph or in the graph named by the file,
     * and the triples of its named graphs (TriG, N-Quads) in those graphs.
     */
    static List<Quad> getQuads(RDFNode data, boolean isDefault)
    {
        Dataset dataset = RDFDataMgr.loadDataset(data.asResource().getURI());
        List<Quad> quads = new ArrayList<>();

        Node graph = isDefault ? null : data.asNode();
        Iterator<Triple> triples = dataset.getDefaultModel().getGraph().find();

        while(triples.hasNext())
        {
            Triple t = triples.next();
            quads.add(new Quad(graph, t.getSubject(), t.getPredicate(), t.getObject()));
        }

        Iterator<String> names = dataset.listNames();

        while(names.hasNext())
        {
            String name = names.next();
            Node named = NodeFactory.createURI(name);
            Iterator<Triple> namedTriples = dataset.getNamedModel(name).getGraph().find();

            while(namedTriples.hasNext())
            {
                Triple t = namedTriples.next();
                quads.add(new Quad(named, t.getSubject(), t.getPredicate(), t.getObject()));
            }
        }

        return quads;
    }


    /**
     * Constant term mapping of a Jena node, see {@link #getMapping(Node, SparqlDatabaseConfiguration, Map)}.
     */
    static TermMapping getMapping(Node node, SparqlDatabaseConfiguration config)
    {
        return getMapping(node, config, Map.of());
    }


    /**
     * Constant term mapping of a Jena node (see {@link #getTerm}): literals get the class their datatype assigns
     * (replaced according to {@code map}), blank nodes the test blank node class, the class of a triple term is
     * detected by the request; null for a null node.
     */
    static TermMapping getMapping(Node node, SparqlDatabaseConfiguration config, Map<ResourceClass, ResourceClass> map)
    {
        if(node == null)
        {
            return null;
        }
        else if(node.isURI())
        {
            return new ConstantIriMapping((Iri) getTerm(node, true));
        }
        else if(node.isLiteral() && node.getLiteralBaseDirection() != null)
        {
            DirLangStringLiteral literal = (DirLangStringLiteral) getTerm(node, true);
            LiteralClass literalClass = DirLangStringWithTagClass.get(literal.getDirection(), literal.getTag());

            return new ConstantLiteralMapping(literalClass, literal);
        }
        else if(node.isLiteral() && !node.getLiteralLanguage().isEmpty())
        {
            LangStringLiteral literal = (LangStringLiteral) getTerm(node, true);
            LiteralClass literalClass = LangStringWithTagClass.get(literal.getTag());

            return new ConstantLiteralMapping(literalClass, literal);
        }
        else if(node.isLiteral())
        {
            TypedLiteral literal = (TypedLiteral) getTerm(node, true);
            Datatype datatype = config.getDatatype(literal.getType());
            ResourceClass literalClass = datatype == null ? unsupportedType : datatype.getResourceClass(literal);

            return new ConstantLiteralMapping(map.getOrDefault(literalClass, literalClass), literal);
        }
        else if(node.isBlank())
        {
            return new ConstantBlankNodeMapping((StrBlankNode) getTerm(node, true), bnodeClass);
        }
        else if(node.isTripleTerm())
        {
            return new ConstantTripleTermMapping((TripleTerm) getTerm(node, true));
        }

        return null;
    }


    /**
     * Engine term of a Jena node: file IRIs are shortened to their name, blank nodes keep their label (in the test
     * blank node segment) when {@code keepLabels}, which the data need, or lose it, as the labels of expected results
     * are not compared; the components of a triple term are converted recursively.
     */
    static RdfTerm getTerm(Node node, boolean keepLabels)
    {
        if(node == null)
            return null;
        else if(node.isURI())
            return new Iri(node.getURI().replaceFirst("file://.*/", ""));
        else if(node.isLiteral() && node.getLiteralBaseDirection() != null)
            return new DirLangStringLiteral(node.getLiteralLexicalForm(), node.getLiteralLanguage(),
                    Direction.fromText(node.getLiteralBaseDirection().direction()));
        else if(node.isLiteral() && !node.getLiteralLanguage().isEmpty())
            return new LangStringLiteral(node.getLiteralLexicalForm(), node.getLiteralLanguage());
        else if(node.isLiteral())
            return new TypedLiteral(node.getLiteralLexicalForm(), new Iri(node.getLiteralDatatypeURI()));
        else if(node.isBlank())
            return keepLabels ? new StrBlankNode(node.getBlankNodeLabel(), bnodeClass.getSegment()) :
                    new StrBlankNode("", 0);
        else if(node.isTripleTerm())
            return new TripleTerm(getTerm(node.getTriple().getSubject(), keepLabels),
                    getTerm(node.getTriple().getPredicate(), keepLabels),
                    getTerm(node.getTriple().getObject(), keepLabels));

        return null;
    }


    /**
     * Expected result of a test, read from a Turtle or N-Triples graph or a SPARQL XML or JSON result file.
     */
    static ExpectedResult getResult(RDFNode result)
            throws ParserConfigurationException, SAXException, IOException, URISyntaxException
    {
        if(result.toString().endsWith(".ttl") || result.toString().endsWith(".nt"))
            return getResultFromTTL(result);
        else if(result.toString().endsWith(".srj"))
            return getResultFromJSON(result);
        else
            return getResultFromXML(result);
    }


    /**
     * Triples of an expected Turtle or N-Triples graph as rows of subject, predicate and object.
     */
    static ExpectedResult getResultFromTTL(RDFNode result) throws IOException, URISyntaxException
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        Iterator<Triple> triples = RDFDataMgr.loadModel(result.asResource().getURI()).getGraph().find();

        while(triples.hasNext())
        {
            Triple t = triples.next();
            rows.add(List.of(getTerm(t.getSubject(), false), getTerm(t.getPredicate(), false),
                    getTerm(t.getObject(), false)));
        }

        return new ExpectedResult(null, rows);
    }


    /**
     * Rows of an expected SPARQL JSON result; an ASK result becomes a single boolean row.
     */
    static ExpectedResult getResultFromJSON(RDFNode result) throws IOException, URISyntaxException
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        JsonNode root;

        try(InputStream in = new URI(result.asResource().getURI()).toURL().openStream())
        {
            root = new ObjectMapper().readTree(in);
        }

        if(root.has("boolean"))
        {
            List<RdfTerm> row = new ArrayList<>(1);
            row.add(new TypedLiteral(root.get("boolean").asText(), BuiltinDatatypes.xsdBooleanType.getTypeIri()));
            rows.add(row);

            return new ExpectedResult(null, rows);
        }

        List<String> variables = new ArrayList<>();

        for(JsonNode variable : root.get("head").get("vars"))
            variables.add(variable.asText());

        for(JsonNode binding : root.get("results").get("bindings"))
        {
            List<RdfTerm> row = new ArrayList<>(variables.size());

            for(String variable : variables)
                row.add(getNode(binding.get(variable)));

            rows.add(row);
        }

        return new ExpectedResult(variables, rows);
    }


    /**
     * Engine term of a term of a SPARQL JSON result; null for a missing binding, blank nodes lose their label, the
     * components of a triple term are read recursively.
     */
    static RdfTerm getNode(JsonNode node)
    {
        if(node == null)
            return null;

        JsonNode value = node.get("value");

        return switch(node.get("type").asText())
        {
            case "uri" -> new Iri(value.asText());
            case "bnode" -> new StrBlankNode("", 0);
            case "literal" ->
            {
                if(node.has("its:dir"))
                    yield new DirLangStringLiteral(value.asText(), node.get("xml:lang").asText(),
                            Direction.fromText(node.get("its:dir").asText()));

                if(node.has("xml:lang"))
                    yield new LangStringLiteral(value.asText(), node.get("xml:lang").asText());

                if(node.has("datatype"))
                    yield new TypedLiteral(value.asText(), new Iri(node.get("datatype").asText()));

                yield new TypedLiteral(value.asText(), BuiltinDatatypes.xsdStringType.getTypeIri());
            }
            case "triple" -> new TripleTerm(getNode(value.get("subject")), getNode(value.get("predicate")),
                    getNode(value.get("object")));
            default -> throw new IllegalArgumentException("unknown term type: " + node.get("type").asText());
        };
    }


    /**
     * Rows of an expected SPARQL XML result; an ASK result becomes a single boolean row, triple terms are read
     * recursively.
     */
    static ExpectedResult getResultFromXML(RDFNode result)
            throws ParserConfigurationException, SAXException, IOException, URISyntaxException
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        List<String> variables = new LinkedList<>();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler()
        {
            /**
             * Triple term being read: its components and the position of the component being read.
             */
            class Frame
            {
                RdfTerm[] components = new RdfTerm[3];
                int position;
            }

            List<RdfTerm> row;
            int varIndex;
            StringBuilder data;
            String datatype;
            String lang;
            String direction;
            Deque<Frame> triples = new ArrayDeque<>();

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException
            {
                if(qName.equalsIgnoreCase("variable"))
                {
                    variables.add(attributes.getValue("name"));
                }
                else if(qName.equalsIgnoreCase("result"))
                {
                    row = new ArrayList<>(variables.size());

                    for(int i = 0; i < variables.size(); i++)
                        row.add(null);

                    rows.add(row);
                }
                else if(qName.equalsIgnoreCase("binding"))
                {
                    varIndex = variables.indexOf(attributes.getValue("name"));
                }
                else if(qName.equalsIgnoreCase("literal"))
                {
                    lang = attributes.getValue("xml:lang");

                    if(lang != null)
                        lang = lang.toLowerCase();

                    direction = attributes.getValue("its:dir");
                    datatype = attributes.getValue("datatype");
                    data = new StringBuilder();
                }
                else if(qName.equalsIgnoreCase("uri") || qName.equalsIgnoreCase("bnode"))
                {
                    data = new StringBuilder();
                }
                else if(qName.equalsIgnoreCase("boolean"))
                {
                    row = new ArrayList<>(1);
                    row.add(null);
                    rows.add(row);
                    varIndex = 0;
                    data = new StringBuilder();
                }
                else if(qName.equalsIgnoreCase("triple"))
                {
                    triples.push(new Frame());
                }
                else if(qName.equalsIgnoreCase("subject") && !triples.isEmpty())
                {
                    triples.peek().position = 0;
                }
                else if(qName.equalsIgnoreCase("predicate") && !triples.isEmpty())
                {
                    triples.peek().position = 1;
                }
                else if(qName.equalsIgnoreCase("object") && !triples.isEmpty())
                {
                    triples.peek().position = 2;
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException
            {
                if(varIndex == -1)
                    return;

                RdfTerm term = null;

                if(qName.equalsIgnoreCase("triple"))
                {
                    Frame frame = triples.pop();
                    term = new TripleTerm(frame.components[0], frame.components[1], frame.components[2]);
                }
                else if(qName.equalsIgnoreCase("boolean"))
                {
                    term = new TypedLiteral(data.toString(), BuiltinDatatypes.xsdBooleanType.getTypeIri());
                }
                else if(qName.equalsIgnoreCase("uri"))
                {
                    term = new Iri(data.toString());
                }
                else if(qName.equalsIgnoreCase("bnode"))
                {
                    term = new StrBlankNode("", 0);
                }
                else if(!qName.equalsIgnoreCase("literal"))
                {
                    return;
                }
                else if(direction != null)
                {
                    term = new DirLangStringLiteral(data.toString(), lang, Direction.fromText(direction));
                }
                else if(lang != null)
                {
                    term = new LangStringLiteral(data.toString(), lang);
                }
                else if(datatype != null)
                {
                    term = new TypedLiteral(data.toString(), new Iri(datatype));
                }
                else
                {
                    term = new TypedLiteral(data.toString(), BuiltinDatatypes.xsdStringType.getTypeIri());
                }

                data = null;

                if(!triples.isEmpty())
                    triples.peek().components[triples.peek().position] = term;
                else
                    row.set(varIndex, term);
            }

            @Override
            public void characters(char ch[], int start, int length)
            {
                if(data != null)
                    data.append(new String(ch, start, length));
            }
        };

        saxParser.parse((new URI(result.asResource().getURI())).toURL().openStream(), handler);

        return new ExpectedResult(variables.isEmpty() ? null : variables, rows);
    }


    /**
     * The term with the labels of its blank nodes erased, inside triple terms too, so that it compares equal to the
     * expected term.
     */
    private static RdfTerm eraseBlankNodeLabels(RdfTerm term)
    {
        return switch(term)
        {
            case BlankNode _ -> new StrBlankNode("", 0);
            case TripleTerm triple -> new TripleTerm(eraseBlankNodeLabels(triple.getSubject()),
                    eraseBlankNodeLabels(triple.getPredicate()), eraseBlankNodeLabels(triple.getObject()));
            case null, default -> term;
        };
    }


    /**
     * Rows of an engine result with the labels of blank nodes erased, in the order of the given variables when they are
     * given (the files of the W3C tests list the variables of {@code SELECT *} in another order than the engine
     * projects them), otherwise as projected.
     */
    private static List<List<RdfTerm>> getResult(Result it, List<String> variables) throws SQLException
    {
        List<List<RdfTerm>> result = new ArrayList<>();

        if(it.getHeads().isEmpty())
        {
            while(it.next())
                result.add(new ArrayList<>());

            return result;
        }

        int[] order = null;

        if(variables != null)
        {
            Map<Variable, Integer> indexes = it.getVariableIndexes();
            order = new int[variables.size()];

            for(int i = 0; i < order.length; i++)
            {
                Integer index = indexes.get(new Variable(variables.get(i)));

                if(index == null)
                    throw new AssertionError("variable " + variables.get(i) + " is not projected");

                order[i] = index;
            }
        }

        while(it.next())
        {
            RdfTerm[] row = it.getRow();
            int size = order == null ? row.length : order.length;
            List<RdfTerm> terms = new ArrayList<>(size);

            for(int i = 0; i < size; i++)
                terms.add(eraseBlankNodeLabels(row[order == null ? i : order[i]]));

            result.add(terms);
        }

        return result;
    }
}
