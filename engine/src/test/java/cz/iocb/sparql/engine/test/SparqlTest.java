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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
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
import cz.iocb.sparql.engine.Database;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantBlankNodeMapping;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantLiteralMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.LangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.nextprot.combined.NeXtProtCombinedConfiguration;
import cz.iocb.sparql.nextprot.integer.NeXtProtIntegerConfiguration;
import cz.iocb.sparql.nextprot.string.NeXtProtStringConfiguration;



/**
 * Conformance tests driven by the W3C-style manifests under {@code src/test/resources/sparql11}: syntax tests check
 * that a query is or is not accepted, evaluation tests load the test data as constant quad mappings of a fresh
 * configuration and compare the results with the expected {@code .srx} or {@code .ttl} file. The NeXtProt families only
 * translate and run the queries of {@code nextprot/queryset.sparql} against the three NeXtProt configurations. Requires
 * the {@link Database} container.
 */
@DisplayName("SPARQL 1.1 Tests")
public class SparqlTest
{
    /**
     * Quad of the test data as read by Jena; a null graph denotes the default graph.
     */
    public record Quad(RDFNode graph, RDFNode subject, RDFNode predicate, RDFNode object)
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

        File directory = new File("src/test/resources/sparql11");

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


        literalClassMap.put(xsdBoolean, new SubsetLiteralClass(xsdBoolean));
        literalClassMap.put(xsdByte, new SubsetLiteralClass(xsdByte));
        literalClassMap.put(xsdUnsignedByte, new SubsetLiteralClass(xsdUnsignedByte));
        literalClassMap.put(xsdShort, new SubsetLiteralClass(xsdShort));
        literalClassMap.put(xsdUnsignedShort, new SubsetLiteralClass(xsdUnsignedShort));
        literalClassMap.put(xsdInt, new SubsetLiteralClass(xsdInt));
        literalClassMap.put(xsdUnsignedInt, new SubsetLiteralClass(xsdUnsignedInt));
        literalClassMap.put(xsdLong, new SubsetLiteralClass(xsdLong));
        literalClassMap.put(xsdUnsignedLong, new SubsetLiteralClass(xsdUnsignedLong));
        literalClassMap.put(xsdInteger, new SubsetLiteralClass(xsdInteger));
        literalClassMap.put(xsdNonPositiveInteger, new SubsetLiteralClass(xsdNonPositiveInteger));
        literalClassMap.put(xsdNegativeInteger, new SubsetLiteralClass(xsdNegativeInteger));
        literalClassMap.put(xsdNonNegativeInteger, new SubsetLiteralClass(xsdNonNegativeInteger));
        literalClassMap.put(xsdPositiveInteger, new SubsetLiteralClass(xsdPositiveInteger));
        literalClassMap.put(xsdDecimal, new SubsetLiteralClass(xsdDecimal));
        literalClassMap.put(xsdFloat, new SubsetLiteralClass(xsdFloat));
        literalClassMap.put(xsdDouble, new SubsetLiteralClass(xsdDouble));
        literalClassMap.put(xsdString, new SubsetLiteralClass(xsdString));
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
    void doQueryEvaluationTests(String name, String query, List<Quad> quads, List<List<RdfTerm>> expected)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        for(Quad quad : quads)
            config.addQuadMapping((ConstantIriMapping) getMapping(quad.graph, config), getMapping(quad.subject, config),
                    (ConstantIriMapping) getMapping(quad.predicate, config), getMapping(quad.object, config));

        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            List<List<RdfTerm>> result = getResult(request.execute(query));

            MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
        }
    }


    /**
     * Evaluation tests with the literals of the test data mapped to {@link SubsetLiteralClass}es, exercising the
     * conversions between subclasses and their built-in superclasses.
     */
    @DisplayName("Query Evaluation Tests (with subset literals)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getQueryEvaluationTests")
    void doQueryEvaluationTestsWithLiteralMap(String name, String query, List<Quad> quads, List<List<RdfTerm>> expected)
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
            List<List<RdfTerm>> result = getResult(request.execute(query));

            MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
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
     * Names and texts of the {@code mf:PositiveSyntaxTest11} entries of the manifests.
     */
    static List<Arguments> getPositiveSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<>();

        Query info = QueryFactory.create("""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>

                SELECT ?NAME ?QUERY WHERE
                {
                  ?MANIFEST rdf:type mf:Manifest; mf:entries / rdf:rest* / rdf:first ?TEST.
                  ?TEST rdf:type mf:PositiveSyntaxTest11; mf:name ?NAME; mf:action ?QUERY.
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
     * Names and texts of the {@code mf:NegativeSyntaxTest11} entries of the manifests.
     */
    static List<Arguments> getNegativeSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<>();

        Query info = QueryFactory.create("""
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>

                SELECT ?NAME ?QUERY WHERE
                {
                  ?MANIFEST rdf:type mf:Manifest; mf:entries / rdf:rest* / rdf:first ?TEST.
                  ?TEST rdf:type mf:NegativeSyntaxTest11; mf:name ?NAME; mf:action ?QUERY.
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
                List<List<RdfTerm>> expected = getResult(test.get("?RESULT"));

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
     * Quads of a Turtle data file, in the default graph or in the graph named by the file.
     */
    static List<Quad> getQuads(RDFNode data, boolean isDefault)
    {
        RDFNode graph = isDefault ? null : data;

        Model model = ModelFactory.createDefaultModel();
        model.read(data.asResource().getURI(), "TTL");

        List<Quad> quads = new ArrayList<>();

        StmtIterator it = model.listStatements();

        while(it.hasNext())
        {
            Statement s = it.nextStatement();
            quads.add(new Quad(graph, s.getSubject(), s.getPredicate(), s.getObject()));
        }

        return quads;
    }


    /**
     * Constant term mapping of a Jena node, see {@link #getMapping(RDFNode, SparqlDatabaseConfiguration, Map)}.
     */
    static TermMapping getMapping(RDFNode node, SparqlDatabaseConfiguration config)
    {
        return getMapping(node, config, Map.of());
    }


    /**
     * Constant term mapping of a Jena node: file IRIs are shortened to their name, literals get the class their
     * datatype assigns (replaced according to {@code map}), blank nodes the test blank node class; null for a null
     * node.
     */
    static TermMapping getMapping(RDFNode node, SparqlDatabaseConfiguration config,
            Map<ResourceClass, ResourceClass> map)
    {
        if(node == null)
        {
            return null;
        }
        else if(node.isURIResource())
        {
            return new ConstantIriMapping(new Iri(node.asResource().getURI().replaceFirst("file://.*/", "")));
        }
        else if(node.isLiteral() && node.asLiteral().getLanguage().isEmpty())
        {
            Iri iri = new Iri(node.asLiteral().getDatatypeURI());

            Datatype datatype = config.getDatatype(iri);

            TypedLiteral literal = new TypedLiteral(node.asLiteral().getLexicalForm(), iri);
            ResourceClass literalClass = datatype == null ? unsupportedType : datatype.getResourceClass(literal);

            return new ConstantLiteralMapping(map.getOrDefault(literalClass, literalClass), literal);
        }
        else if(node.isLiteral() && !node.asLiteral().getLanguage().isEmpty())
        {
            LiteralClass literalClass = LangStringWithTagClass.get(node.asLiteral().getLanguage());

            return new ConstantLiteralMapping(literalClass,
                    new LangStringLiteral(node.asLiteral().getLexicalForm(), node.asLiteral().getLanguage()));
        }
        else if(node.isAnon())
        {
            return new ConstantBlankNodeMapping(
                    new StrBlankNode(node.asResource().getId().getLabelString(), bnodeClass.getSegment()), bnodeClass);
        }

        return null;
    }


    /**
     * Expected rows of a test, read from a Turtle graph or a SPARQL XML result file.
     */
    static List<List<RdfTerm>> getResult(RDFNode result)
            throws ParserConfigurationException, SAXException, IOException, URISyntaxException
    {
        if(result.toString().endsWith(".ttl"))
            return getResultFromTTL(result);
        else
            return getResultFromXML(result);
    }


    /**
     * Triples of an expected Turtle graph as rows of subject, predicate and object.
     */
    static List<List<RdfTerm>> getResultFromTTL(RDFNode result) throws IOException, URISyntaxException
    {
        List<List<RdfTerm>> results = new ArrayList<>();

        Model model = ModelFactory.createDefaultModel();
        model.read(result.asResource().getURI(), "TTL");

        StmtIterator it = model.listStatements();

        while(it.hasNext())
        {
            Statement s = it.nextStatement();
            results.add(List.of(getNode(s.getSubject()), getNode(s.getPredicate()), getNode(s.getObject())));
        }

        return results;
    }


    /**
     * Engine term of a Jena node; blank nodes lose their label since labels are not compared.
     */
    static RdfTerm getNode(RDFNode node)
    {
        if(node == null)
            return null;
        else if(node.isURIResource())
            return new Iri(node.asResource().getURI().replaceFirst("file://.*/", ""));
        else if(node.isLiteral() && node.asLiteral().getLanguage().isEmpty())
            return new TypedLiteral(node.asLiteral().getLexicalForm(), new Iri(node.asLiteral().getDatatypeURI()));
        else if(node.isLiteral() && !node.asLiteral().getLanguage().isEmpty())
            return new LangStringLiteral(node.asLiteral().getLexicalForm(), node.asLiteral().getLanguage());
        else if(node.isAnon())
            return new StrBlankNode("", 0);

        return null;
    }


    /**
     * Rows of an expected SPARQL XML result; an ASK result becomes a single boolean row.
     */
    static List<List<RdfTerm>> getResultFromXML(RDFNode result)
            throws ParserConfigurationException, SAXException, IOException, URISyntaxException
    {
        List<List<RdfTerm>> results = new ArrayList<>();

        List<String> variables = new LinkedList<>();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler()
        {
            List<RdfTerm> result;
            int varIndex;
            StringBuilder data;
            String datatype;
            String lang;

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
                    result = new ArrayList<>(variables.size());

                    for(int i = 0; i < variables.size(); i++)
                        result.add(null);

                    results.add(result);
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

                    datatype = attributes.getValue("datatype");
                    data = new StringBuilder();
                }
                else if(qName.equalsIgnoreCase("uri") || qName.equalsIgnoreCase("bnode"))
                {
                    data = new StringBuilder();
                }
                else if(qName.equalsIgnoreCase("boolean"))
                {
                    result = new ArrayList<>(1);
                    result.add(null);
                    results.add(result);
                    varIndex = 0;
                    data = new StringBuilder();
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException
            {
                if(varIndex == -1)
                    return;

                RdfTerm term = null;

                if(qName.equalsIgnoreCase("boolean"))
                    term = new TypedLiteral(data.toString(), BuiltinDatatypes.xsdBooleanType.getTypeIri());
                else if(qName.equalsIgnoreCase("uri"))
                    term = new Iri(data.toString());
                else if(qName.equalsIgnoreCase("bnode"))
                    term = new StrBlankNode("" /*data.toString()*/, 0);
                else if(!qName.equalsIgnoreCase("literal"))
                    return;
                else if(lang != null)
                    term = new LangStringLiteral(data.toString(), lang);
                else if(datatype != null)
                    term = new TypedLiteral(data.toString(), new Iri(datatype));
                else
                    term = new TypedLiteral(data.toString(), BuiltinDatatypes.xsdStringType.getTypeIri());

                result.set(varIndex, term);
            }

            @Override
            public void characters(char ch[], int start, int length)
            {
                if(data != null)
                    data.append(new String(ch, start, length));
            }
        };

        saxParser.parse((new URI(result.asResource().getURI())).toURL().openStream(), handler);

        return results;
    }


    /**
     * Rows of an engine result.
     */
    private List<List<RdfTerm>> getResult(Result it) throws SQLException
    {
        List<List<RdfTerm>> result = new ArrayList<>();

        if(it.getHeads().isEmpty())
        {
            while(it.next())
            {
                result.add(new ArrayList<>());
            }
        }
        else
        {
            while(it.next())
            {
                RdfTerm[] row = it.getRow();

                for(int i = 0; i < row.length; i++)
                    if(row[i] instanceof BlankNode)
                        row[i] = new StrBlankNode("", 0);

                result.add(Arrays.asList(row));
            }
        }

        return result;
    }
}
