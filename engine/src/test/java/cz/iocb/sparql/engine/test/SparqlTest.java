package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.error.MessageCategory.ERROR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.Driver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.BlankNodeLiteral;
import cz.iocb.sparql.engine.mapping.ConstantBlankNodeMapping;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantLiteralMapping;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes;
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.mapping.classes.LangStringConstantTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.request.BNode;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.IriNode;
import cz.iocb.sparql.engine.request.LanguageTaggedLiteral;
import cz.iocb.sparql.engine.request.RdfNode;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.request.TypedLiteral;



@DisplayName("SPARQL 1.1 Tests")
@Testcontainers
public class SparqlTest
{
    public record Quad(RDFNode graph, RDFNode subject, RDFNode predicate, RDFNode object)
    {
    }


    @Container @SuppressWarnings("resource")
    private static final GenericContainer<?> myAppContainer = new GenericContainer<>(
            new ImageFromDockerfile("sparql-test", false)
                    .withFileFromPath("src/test/resources/docker", Paths.get("src/test/resources/docker"))
                    .withDockerfile(Paths.get("src/test/resources/docker/Dockerfile"))).withExposedPorts(5432)
                            .withEnv("POSTGRES_DB", "test").withEnv("POSTGRES_USER", "test")
                            .withEnv("POSTGRES_PASSWORD", "openacces");

    private static final UserStrBlankNodeClass bnodeClass = new UserStrBlankNodeClass();

    private static DataSource connectionPool = null;
    private static DatabaseSchema schema = null;
    private static Model model = null;


    @BeforeAll
    static void init() throws FileNotFoundException, IOException, SQLException
    {
        String url = String.format("jdbc:postgresql://localhost:%d/test", myAppContainer.getMappedPort(5432));

        PoolProperties p = new PoolProperties();
        p.setUrl(url);
        p.setUsername("test");
        p.setPassword("openacces");
        connectionPool = new DataSource();
        connectionPool.setPoolProperties(p);
        connectionPool.setTestOnBorrow(true);
        connectionPool.setDriverClassName(Driver.class.getCanonicalName());

        schema = new DatabaseSchema(connectionPool);

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
    }


    @AfterAll
    static void close()
    {
        if(connectionPool != null)
            connectionPool.close();
    }


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


    @DisplayName("Query Evaluation Syntax Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getQueryEvaluationSyntaxTests")
    void doQueryEvaluationSyntaxTests(String name, String query) throws SQLException, TranslateExceptions
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            request.execute(query);
        }
    }


    @DisplayName("Query Evaluation Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getQueryEvaluationTests")
    void doQueryEvaluationTests(String name, String query, List<Quad> quads, List<List<RdfNode>> expected)
            throws SQLException, TranslateExceptions
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        for(Quad quad : quads)
            config.addQuadMapping((ConstantIriMapping) getMapping(quad.graph, config), getMapping(quad.subject, config),
                    (ConstantIriMapping) getMapping(quad.predicate, config), getMapping(quad.object, config));

        Engine engine = new Engine(config);

        List<List<RdfNode>> result = new ArrayList<List<RdfNode>>();

        try(Request request = engine.getRequest())
        {
            Result it = request.execute(query);

            if(it.getHeads().get(0).equals("*"))
            {
                while(it.next())
                {
                    result.add(new ArrayList<RdfNode>());
                }
            }
            else
            {
                while(it.next())
                {
                    RdfNode[] row = it.getRow();

                    for(int i = 0; i < row.length; i++)
                        if(row[i] instanceof BNode)
                            row[i] = new BNode("");

                    result.add(Arrays.asList(row));
                }
            }
        }

        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }


    private static List<Arguments> getPositiveSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<Arguments>();

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


    private static List<Arguments> getNegativeSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<Arguments>();

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


    private static List<Arguments> getQueryEvaluationSyntaxTests() throws URISyntaxException, IOException
    {
        List<Arguments> queries = new LinkedList<Arguments>();

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


    private static List<Arguments> getQueryEvaluationTests()
            throws URISyntaxException, IOException, ParserConfigurationException, SAXException
    {
        List<Arguments> queries = new LinkedList<Arguments>();

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
                List<Quad> data = new ArrayList<Quad>();
                List<List<RdfNode>> expected = getResult(test.get("?RESULT"));

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


    private static List<Quad> getQuads(RDFNode data, boolean isDefault)
    {
        RDFNode graph = isDefault ? null : data;

        Model model = ModelFactory.createDefaultModel();
        model.read(data.asResource().getURI(), "TTL");

        List<Quad> quads = new ArrayList<Quad>();

        StmtIterator it = model.listStatements();

        while(it.hasNext())
        {
            Statement s = it.nextStatement();
            quads.add(new Quad(graph, s.getSubject(), s.getPredicate(), s.getObject()));
        }

        return quads;
    }


    private static NodeMapping getMapping(RDFNode node, SparqlDatabaseConfiguration config)
    {
        if(node == null)
        {
            return null;
        }
        else if(node.isURIResource())
        {
            return new ConstantIriMapping(new IRI(node.asResource().getURI().replaceFirst("file://.*/", "")));
        }
        else if(node.isLiteral() && node.asLiteral().getLanguage().isEmpty())
        {
            IRI iri = new IRI(node.asLiteral().getDatatypeURI());
            DataType datatype = config.getDataType(iri);
            LiteralClass literalClass = datatype == null ? BuiltinClasses.unsupportedLiteral :
                    datatype.getGeneralLiteralClass();

            Literal literal = new Literal(node.asLiteral().getLexicalForm(), datatype, iri);

            if(literal.getValue() == null)
                return new ConstantLiteralMapping(BuiltinClasses.unsupportedLiteral,
                        new Literal(node.asLiteral().getLexicalForm(), new IRI(node.asLiteral().getDatatypeURI())));

            return new ConstantLiteralMapping(literalClass, literal);
        }
        else if(node.isLiteral() && !node.asLiteral().getLanguage().isEmpty())
        {
            LiteralClass literalClass = LangStringConstantTagClass.get(node.asLiteral().getLanguage());

            return new ConstantLiteralMapping(literalClass,
                    new Literal(node.asLiteral().getLexicalForm(), node.asLiteral().getLanguage()));
        }
        else if(node.isAnon())
        {
            return new ConstantBlankNodeMapping(
                    new BlankNodeLiteral(node.asResource().getId().getLabelString(), bnodeClass));
        }

        return null;
    }


    private static List<List<RdfNode>> getResult(RDFNode result)
            throws ParserConfigurationException, SAXException, IOException, URISyntaxException
    {
        if(result.toString().endsWith(".ttl"))
            return getResultFromTTL(result);
        else
            return getResultFromXML(result);
    }


    private static List<List<RdfNode>> getResultFromTTL(RDFNode result) throws IOException, URISyntaxException
    {
        List<List<RdfNode>> results = new ArrayList<>();

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


    private static RdfNode getNode(RDFNode node)
    {
        if(node == null)
            return null;
        else if(node.isURIResource())
            return new IriNode(node.asResource().getURI().replaceFirst("file://.*/", ""));
        else if(node.isLiteral() && node.asLiteral().getLanguage().isEmpty())
            return new TypedLiteral(node.asLiteral().getLexicalForm(), new IRI(node.asLiteral().getDatatypeURI()));
        else if(node.isLiteral() && !node.asLiteral().getLanguage().isEmpty())
            return new LanguageTaggedLiteral(node.asLiteral().getLexicalForm(), node.asLiteral().getLanguage());
        else if(node.isAnon())
            return new BNode("");

        return null;
    }


    private static List<List<RdfNode>> getResultFromXML(RDFNode result)
            throws ParserConfigurationException, SAXException, IOException, URISyntaxException
    {
        List<List<RdfNode>> results = new ArrayList<>();

        List<String> variables = new LinkedList<String>();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler()
        {
            ArrayList<RdfNode> result;
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
                    result = new ArrayList<RdfNode>(variables.size());

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
                    result = new ArrayList<RdfNode>(1);
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

                RdfNode node = null;

                if(qName.equalsIgnoreCase("boolean"))
                {
                    node = new TypedLiteral(data.toString(), BuiltinDataTypes.xsdBooleanType.getTypeIri());
                }
                else if(qName.equalsIgnoreCase("uri"))
                {
                    node = new IriNode(data.toString());
                }
                else if(qName.equalsIgnoreCase("bnode"))
                {
                    node = new BNode("" /*data.toString()*/);
                }
                else if(!qName.equalsIgnoreCase("literal"))
                {
                    return;
                }
                else if(lang != null)
                {
                    node = new LanguageTaggedLiteral(data.toString(), lang);
                }
                else if(datatype != null)
                {
                    String text = data.toString();

                    if(datatype.equals("http://www.w3.org/2001/XMLSchema#date"))
                        text = text.replaceAll("\\+00:00$", "Z");
                    else if(datatype.equals("http://www.w3.org/2001/XMLSchema#decimal"))
                        text = text.replaceAll("\\.0*$", "");
                    else if(datatype.equals("http://www.w3.org/2001/XMLSchema#double"))
                        text = Double.valueOf(text).toString();
                    else if(datatype.equals("http://www.w3.org/2001/XMLSchema#float"))
                        text = Float.valueOf(text).toString();
                    else if(datatype.equals("http://www.w3.org/2001/XMLSchema#boolean") && text.equals("1"))
                        text = "true";
                    else if(datatype.equals("http://www.w3.org/2001/XMLSchema#boolean") && text.equals("0"))
                        text = "false";

                    node = new TypedLiteral(text, datatype);
                }
                else
                {
                    node = new TypedLiteral(data.toString(), BuiltinDataTypes.xsdStringType.getTypeIri());
                }

                result.set(varIndex, node);
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
}
