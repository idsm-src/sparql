package cz.iocb.sparql.endpoint;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.testing.Database;



/**
 * The protocol handling of the servlet with stub requests and responses over a configuration of two constant quads: the
 * {@code version} parameter of the request (as a query parameter, a form parameter and a media type parameter), its
 * errors, and the announcement of RDF 1.2 in the results (the {@code version} member of the JSON head, the VERSION
 * directive of Turtle and N-Triples and the {@code version} parameter of the media types). The JSON formats are also
 * checked to escape the characters of a literal that a JSON string cannot hold as they are, the XML formats to write
 * the white space and the markup of a term so that an XML parser reads them back, the RDF languages to write an IRI
 * with the characters that their syntax excludes escaped, and the TSV and CSV formats to write the header and the ends
 * of the lines as their specification says.
 */
public class EndpointServletTest
{
    private static final String select = "SELECT * WHERE {}";
    private static final String ask = "ASK {}";
    private static final String construct = "CONSTRUCT { <http://example.org/s> <http://example.org/p> 1 } WHERE {}";
    private static final String sparqlJson = "application/sparql-results+json";
    private static final String sparqlXml = "application/sparql-results+xml";
    private static final String rdfJson = "application/rdf+json";
    private static final String rdfXml = "application/rdf+xml";
    private static final String turtle = "text/turtle";
    private static final String ntriples = "application/n-triples";
    private static final String tsv = "text/tab-separated-values";
    private static final String csv = "text/csv";
    private static final String form = "application/x-www-form-urlencoded";
    private static final String sparqlQuery = "application/sparql-query";
    private static final String sparqlNamespace = "http://www.w3.org/2005/sparql-results#";
    private static final String rdfNamespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * The characters a JSON string has to escape: the control characters (all of them but U+0000, which the database
     * cannot store), the quotation mark and the reverse solidus.
     */
    private static final String special = IntStream.range(1, 0x20).mapToObj(Character::toString).collect(joining())
            + "\"\\";

    /**
     * White space and markup of a term, which an XML parser reads as something else unless they are escaped: a carriage
     * return as a line feed, and a tabulator or a line feed of an attribute value as a space.
     */
    private static final String markup = " a\tb\nc\rd\r\ne<f>g&h\"i'j]]> ";

    /**
     * IRI holding the white space, the markup and the other characters that the syntax of IRIs excludes, which no query
     * can write: the configuration has it as the subject and the object of a constant quad, and as the datatype of the
     * object of another one.
     */
    private static final String markupIri = "http://example.org/" + markup + "{|}^`\\";

    /**
     * Texts between angle brackets, as the RDF languages write IRIs.
     */
    private static final Pattern irirefPattern = Pattern.compile("<([^>]*)>");

    /**
     * Text of an IRI in the RDF languages: characters that their {@code IRIREF} production does not exclude, and
     * numeric escapes.
     */
    private static final Pattern iriTextPattern = Pattern.compile("(?:[^\\x00-\\x20<>\"{}|^`\\\\]|\\\\u[0-9A-F]{4})*");

    /**
     * Numeric escape of a character.
     */
    private static final Pattern escapePattern = Pattern.compile("\\\\u([0-9A-F]{4})");

    private static EndpointServlet servlet = null;


    /**
     * Response stub: the status, the content type and the body written by the servlet.
     */
    private static final class Response
    {
        /**
         * HTTP status, OK until set.
         */
        int status = HttpServletResponse.SC_OK;

        /**
         * Content type, null until set.
         */
        String contentType = null;

        /**
         * The body.
         */
        final StringWriter body = new StringWriter();

        /**
         * Writer of the body.
         */
        final PrintWriter writer = new PrintWriter(body);

        /**
         * The stub given to the servlet: every setter records its value, every other method does nothing.
         */
        final HttpServletResponse stub = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[] { HttpServletResponse.class },
                (_, method, args) -> switch(method.getName())
                {
                    case "setStatus" -> status = (Integer) args[0];
                    case "getStatus" -> status;
                    case "setContentType" -> contentType = (String) args[0];
                    case "getContentType" -> contentType;
                    case "getWriter" -> writer;
                    case "resetBuffer" ->
                    {
                        body.getBuffer().setLength(0);
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                });


        /**
         * The body written so far.
         *
         * @return the body written so far
         */
        String getBody()
        {
            writer.flush();
            return body.toString();
        }
    }


    /**
     * Creates the servlet over the configuration of two constant quads, {@code <iri> ex:iri <iri>} and
     * {@code <iri> ex:typed "x"^^<iri>}, where {@code <iri>} is {@link #markupIri}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);

        ConstantIriMapping iri = new ConstantIriMapping(new Iri(markupIri));

        config.addQuadMapping(null, iri, new ConstantIriMapping(new Iri("http://example.org/iri")), iri);
        config.addQuadMapping(null, iri, new ConstantIriMapping(new Iri("http://example.org/typed")),
                config.createLiteralMapping(unsupportedType, new TypedLiteral("x", new Iri(markupIri))));

        servlet = new EndpointServlet(new Engine(config));
    }


    /**
     * Default value of a type: false, zero or null.
     *
     * @param type the type
     * @return default value of the type
     */
    private static Object defaultValue(Class<?> type)
    {
        if(type == boolean.class)
            return false;

        if(type == int.class)
            return 0;

        if(type == long.class)
            return 0l;

        if(type == double.class)
            return 0d;

        if(type == float.class)
            return 0f;

        if(type == short.class)
            return (short) 0;

        if(type == byte.class)
            return (byte) 0;

        if(type == char.class)
            return '\0';

        return null;
    }


    /**
     * Parameters of a request from pairs of names and values; a repeated name has all its values.
     *
     * @param pairs the names and values
     * @return parameters of a request
     */
    private static Map<String, String[]> parameters(String... pairs)
    {
        Map<String, List<String>> map = new HashMap<>();

        for(int i = 0; i < pairs.length; i += 2)
            map.computeIfAbsent(pairs[i], _ -> new ArrayList<>()).add(pairs[i + 1]);

        Map<String, String[]> result = new HashMap<>();

        for(Map.Entry<String, List<String>> entry : map.entrySet())
            result.put(entry.getKey(), entry.getValue().toArray(new String[0]));

        return result;
    }


    /**
     * Input stream of the body.
     *
     * @param body the body
     * @return input stream of the body
     */
    private static ServletInputStream inputStream(String body)
    {
        ByteArrayInputStream bytes = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));

        return new ServletInputStream()
        {
            @Override
            public int read()
            {
                return bytes.read();
            }


            @Override
            public boolean isFinished()
            {
                return bytes.available() == 0;
            }


            @Override
            public boolean isReady()
            {
                return true;
            }


            @Override
            public void setReadListener(ReadListener listener)
            {
            }
        };
    }


    /**
     * Request stub serving the method, the parameters, the Accept header, the content type and the body; every other
     * method returns the default value of its type.
     *
     * @param method the HTTP method
     * @param parameters the parameters
     * @param accept the Accept header, or null
     * @param contentType the content type, or null
     * @param body the body, or null
     * @return the request stub
     */
    private static HttpServletRequest request(String method, Map<String, String[]> parameters, String accept,
            String contentType, String body)
    {
        InvocationHandler handler = (proxy, m, args) -> switch(m.getName())
        {
            case "getMethod" -> method;
            case "getRequestURI" -> "/sparql";
            case "getParameter" -> parameters.containsKey(args[0]) ? parameters.get(args[0])[0] : null;
            case "getParameterValues" -> parameters.get(args[0]);
            case "getHeader" -> "accept".equalsIgnoreCase((String) args[0]) ? accept : null;
            case "getContentType" -> contentType;
            case "getCharacterEncoding" -> "UTF-8";
            case "getInputStream" -> inputStream(body != null ? body : "");
            case "toString" -> "request stub";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> defaultValue(m.getReturnType());
        };

        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, handler);
    }


    /**
     * Response of the servlet to a GET.
     *
     * @param parameters the parameters
     * @param accept the Accept header
     * @return response of the servlet
     * @throws IOException on output errors
     */
    private static Response get(Map<String, String[]> parameters, String accept) throws IOException
    {
        Response response = new Response();
        servlet.doGet(request("GET", parameters, accept, null, null), response.stub);
        return response;
    }


    /**
     * Response of the servlet to a POST.
     *
     * @param contentType the content type (with its parameters)
     * @param parameters the parameters (of the URL, or of the form for a form content type)
     * @param accept the Accept header
     * @param body the body, or null for a form
     * @return response of the servlet
     * @throws IOException on output errors
     */
    private static Response post(String contentType, Map<String, String[]> parameters, String accept, String body)
            throws IOException
    {
        Response response = new Response();
        servlet.doPost(request("POST", parameters, accept, contentType, body), response.stub);
        return response;
    }


    /**
     * String literal of a query holding the characters, each written by its numeric escape.
     *
     * @param value the characters
     * @return string literal of a query holding the characters
     */
    private static String literal(String value)
    {
        return value.chars().mapToObj(c -> String.format("\\u%04X", c)).collect(joining("", "\"", "\""));
    }


    /**
     * The body parsed as JSON.
     *
     * @param response the response
     * @return the body parsed as JSON
     * @throws IOException on parsing errors
     */
    private static JsonNode json(Response response) throws IOException
    {
        assertEquals(HttpServletResponse.SC_OK, response.status);
        return new ObjectMapper().readTree(response.getBody());
    }


    /**
     * The body parsed as XML.
     *
     * @param response the response
     * @return the body parsed as XML
     * @throws ParserConfigurationException if the parser cannot be created
     * @throws SAXException if the body is not well-formed
     * @throws IOException on parsing errors
     */
    private static Document xml(Response response) throws ParserConfigurationException, SAXException, IOException
    {
        assertEquals(HttpServletResponse.SC_OK, response.status);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(response.getBody())));
    }


    /**
     * The IRIs written in a document of an RDF language, in the order of their occurrence: the texts between angle
     * brackets with the numeric escapes resolved. The texts must not hold a character that the syntax of IRIs excludes.
     *
     * @param document the document
     * @return the IRIs written in the document
     */
    private static List<String> iris(String document)
    {
        List<String> iris = new ArrayList<>();
        Matcher matcher = irirefPattern.matcher(document);

        while(matcher.find())
        {
            String text = matcher.group(1);

            assertTrue(iriTextPattern.matcher(text).matches(), text);

            iris.add(escapePattern.matcher(text).replaceAll(
                    escape -> Matcher.quoteReplacement(Character.toString(Integer.parseInt(escape.group(1), 16)))));
        }

        return iris;
    }


    /**
     * The first element of the name in the document.
     *
     * @param document the document
     * @param namespace the namespace of the element
     * @param name the local name of the element
     * @return the first element of the name in the document
     */
    private static Element element(Document document, String namespace, String name)
    {
        return (Element) document.getElementsByTagNameNS(namespace, name).item(0);
    }


    @Test
    @DisplayName("a request without a version gets the results as before")
    void notAnnounced() throws Exception
    {
        Response response = get(parameters("query", select), sparqlJson);
        JsonNode json = json(response);

        assertEquals(sparqlJson, response.contentType);
        assertFalse(json.get("head").has("version"));
        assertEquals(0, json.get("head").get("vars").size());
        assertEquals(1, json.get("results").get("bindings").size());

        response = get(parameters("query", ask), sparqlJson);
        json = json(response);

        assertFalse(json.get("head").has("version"));
        assertTrue(json.get("boolean").asBoolean());
    }


    @Test
    @DisplayName("the version parameter of a GET makes the JSON results announce RDF 1.2")
    void announcedByParameter() throws Exception
    {
        Response response = get(parameters("query", select, "version", "1.2"), sparqlJson);
        JsonNode json = json(response);

        assertEquals(sparqlJson + ";version=1.2", response.contentType);
        assertEquals("1.2", json.get("head").get("version").asText());
        assertEquals(0, json.get("head").get("vars").size());
        assertEquals(1, json.get("results").get("bindings").size());

        response = get(parameters("query", ask, "version", "1.2-basic"), sparqlJson);
        json = json(response);

        assertEquals("1.2", json.get("head").get("version").asText());
        assertTrue(json.get("boolean").asBoolean());

        response = get(parameters("query", select, "version", "1.1"), sparqlJson);
        json = json(response);

        assertEquals(sparqlJson, response.contentType);
        assertFalse(json.get("head").has("version"));
    }


    @Test
    @DisplayName("the version parameter of the accepted media type announces RDF 1.2 too")
    void announcedByAccept() throws Exception
    {
        Response response = get(parameters("query", select), sparqlJson + ";version=1.2");

        assertEquals(sparqlJson + ";version=1.2", response.contentType);
        assertEquals("1.2", json(response).get("head").get("version").asText());

        response = get(parameters("query", select), sparqlJson + "; version=1.1");

        assertEquals(sparqlJson, response.contentType);
        assertFalse(json(response).get("head").has("version"));
    }


    @Test
    @DisplayName("the VERSION declaration announces RDF 1.2 unless the protocol says another version")
    void announcedByDeclaration() throws Exception
    {
        Response response = get(parameters("query", "VERSION \"1.2\" " + select), sparqlJson);

        assertEquals(sparqlJson + ";version=1.2", response.contentType);
        assertEquals("1.2", json(response).get("head").get("version").asText());

        response = get(parameters("query", "VERSION \"1.2\" " + select, "version", "1.1"), sparqlJson);

        assertEquals(sparqlJson, response.contentType);
        assertFalse(json(response).get("head").has("version"));
    }


    @Test
    @DisplayName("the XML results are not changed by the version")
    void xmlUnchanged() throws Exception
    {
        Response response = get(parameters("query", select, "version", "1.2"), sparqlXml);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals(sparqlXml, response.contentType);
        assertTrue(response.getBody().contains("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">"));
    }


    @Test
    @DisplayName("an unsupported version or more than one version is a bad request")
    void badVersions() throws Exception
    {
        Response response = get(parameters("query", select, "version", "2.0"), sparqlJson);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertEquals("text/plain", response.contentType);
        assertTrue(response.getBody().contains("version '2.0' is not supported"));

        response = get(parameters("query", select, "version", "1.2", "version", "1.1"), sparqlJson);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
    }


    @Test
    @DisplayName("a form POST takes the version from the form")
    void formPost() throws Exception
    {
        Response response = post(form, parameters("query", select, "version", "1.2"), sparqlJson, null);

        assertEquals(sparqlJson + ";version=1.2", response.contentType);
        assertEquals("1.2", json(response).get("head").get("version").asText());
    }


    @Test
    @DisplayName("a direct POST takes the version from the media type, which wins over the URL")
    void directPost() throws Exception
    {
        Response response = post(sparqlQuery + ";version=1.2", parameters(), turtle, construct);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals(turtle + ";version=1.2", response.contentType);
        assertTrue(response.getBody().startsWith("VERSION \"1.2\"\n<http://example.org/s>"));

        response = post(sparqlQuery, parameters(), turtle, construct);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals(turtle, response.contentType);
        assertTrue(response.getBody().startsWith("<http://example.org/s>"));

        response = post(sparqlQuery + "; charset=UTF-8; version=\"1.1\"", parameters("version", "1.2"), turtle,
                construct);

        assertEquals(turtle, response.contentType);
        assertTrue(response.getBody().startsWith("<http://example.org/s>"));

        response = post(sparqlQuery, parameters("version", "1.2"), turtle, construct);

        assertEquals(turtle + ";version=1.2", response.contentType);
        assertTrue(response.getBody().startsWith("VERSION \"1.2\"\n"));
    }


    @Test
    @DisplayName("N-Triples start with the VERSION directive when RDF 1.2 is announced")
    void ntriples() throws Exception
    {
        Response response = get(parameters("query", construct, "version", "1.2"), ntriples);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals(ntriples + ";version=1.2", response.contentType);
        assertTrue(response.getBody().startsWith("VERSION \"1.2\"\n<http://example.org/s> <http://example.org/p> "));

        response = get(parameters("query", construct), ntriples);

        assertEquals(ntriples, response.contentType);
        assertTrue(response.getBody().startsWith("<http://example.org/s> <http://example.org/p> "));
    }


    @Test
    @DisplayName("a failing federated call is a bad request with the message of the service error")
    void failingService() throws Exception
    {
        String pattern = "<http://127.0.0.1:1/sparql> { ?x <http://example.org/p> ?o } }";
        Response response = get(parameters("query", "SELECT ?x WHERE { SERVICE " + pattern), sparqlJson);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertEquals("text/plain", response.contentType);
        assertTrue(response.getBody().contains("cannot be evaluated by endpoint 'http://127.0.0.1:1/sparql'"));

        JsonNode results = json(get(parameters("query", "SELECT ?x WHERE { SERVICE SILENT " + pattern), sparqlJson));

        assertEquals(1, results.get("results").get("bindings").size());
    }


    @Test
    @DisplayName("the JSON results escape the special characters of a literal")
    void specialCharactersInJsonResults() throws Exception
    {
        String query = "SELECT (" + literal(special) + " AS ?x) WHERE {}";
        JsonNode json = json(get(parameters("query", query), sparqlJson));

        assertEquals(special, json.get("results").get("bindings").get(0).get("x").get("value").asText());
    }


    @Test
    @DisplayName("RDF/JSON escapes the special characters of a literal")
    void specialCharactersInRdfJson() throws Exception
    {
        String query = "CONSTRUCT { <http://example.org/s> <http://example.org/p> " + literal(special) + " } WHERE {}";
        JsonNode json = json(get(parameters("query", query), rdfJson));

        assertEquals(special,
                json.get("http://example.org/s").get("http://example.org/p").get(0).get("value").asText());
    }


    @Test
    @DisplayName("the XML results keep the white space and the markup of a term")
    void markupInXmlResults() throws Exception
    {
        String query = "SELECT (" + literal(markup) + " AS ?x) WHERE {}";
        Document document = xml(get(parameters("query", query), sparqlXml));

        assertEquals(markup, element(document, sparqlNamespace, "literal").getTextContent());

        document = xml(get(parameters("query", "SELECT ?o WHERE { ?s <http://example.org/iri> ?o }"), sparqlXml));

        assertEquals(markupIri, element(document, sparqlNamespace, "uri").getTextContent());

        document = xml(get(parameters("query", "SELECT ?o WHERE { ?s <http://example.org/typed> ?o }"), sparqlXml));

        assertEquals(markupIri, element(document, sparqlNamespace, "literal").getAttribute("datatype"));
    }


    @Test
    @DisplayName("RDF/XML keeps the white space and the markup of a term")
    void markupInRdfXml() throws Exception
    {
        String query = "CONSTRUCT { <http://example.org/s> <http://example.org/p> " + literal(markup) + " } WHERE {}";
        Document document = xml(get(parameters("query", query), rdfXml));

        assertEquals(markup, element(document, "http://example.org/", "p").getTextContent());

        document = xml(get(parameters("query", "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }"), rdfXml));

        assertEquals(markupIri, element(document, rdfNamespace, "Description").getAttributeNS(rdfNamespace, "about"));
        assertEquals(markupIri,
                element(document, "http://example.org/", "iri").getAttributeNS(rdfNamespace, "resource"));
        assertEquals(markupIri,
                element(document, "http://example.org/", "typed").getAttributeNS(rdfNamespace, "datatype"));
    }


    @Test
    @DisplayName("the RDF languages and TSV write the characters that the syntax of IRIs excludes by numeric escapes")
    void irisInRdfLanguages() throws Exception
    {
        for(String format : List.of(ntriples, turtle, tsv))
        {
            // an IRI as the subject and the object, and as the datatype of a literal
            for(String predicate : List.of("http://example.org/iri", "http://example.org/typed"))
            {
                String pattern = "{ ?s <" + predicate + "> ?o }";
                String query = "CONSTRUCT " + pattern + " WHERE " + pattern;
                Response response = get(parameters("query", query), format);

                assertEquals(HttpServletResponse.SC_OK, response.status);
                assertEquals(List.of(markupIri, predicate, markupIri), iris(response.getBody()), format);
            }
        }
    }


    @Test
    @DisplayName("the TSV results name the variables with question marks and end the lines by line feeds")
    void tsvResults() throws Exception
    {
        String integer = "\"1\"^^<http://www.w3.org/2001/XMLSchema#integer>";
        Response response = get(parameters("query", "SELECT (1 AS ?x) (<http://example.org/s> AS ?y) ?z WHERE {}"),
                tsv);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals("?x\t?y\t?z\n" + integer + "\t<http://example.org/s>\t\n", response.getBody());

        response = get(parameters("query", construct), tsv);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals("?subject\t?predicate\t?object\n<http://example.org/s>\t<http://example.org/p>\t" + integer + "\n",
                response.getBody());

        // the answer of an ASK query is written as the solution of the variable bool
        response = get(parameters("query", ask), tsv);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals("?bool\n\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>\n", response.getBody());
        assertEquals(get(parameters("query", "SELECT (true AS ?bool) WHERE {}"), tsv).getBody(), response.getBody());
    }


    @Test
    @DisplayName("the CSV results name the variables without question marks and end the lines by CRLF")
    void csvResults() throws Exception
    {
        Response response = get(parameters("query", "SELECT (1 AS ?x) (<http://example.org/s> AS ?y) ?z WHERE {}"),
                csv);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals("x,y,z\r\n1,http://example.org/s,\r\n", response.getBody());

        response = get(parameters("query", construct), csv);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals("subject,predicate,object\r\nhttp://example.org/s,http://example.org/p,1\r\n", response.getBody());

        // the answer of an ASK query is written as the solution of the variable bool
        response = get(parameters("query", ask), csv);

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals("bool\r\ntrue\r\n", response.getBody());
        assertEquals(get(parameters("query", "SELECT (true AS ?bool) WHERE {}"), csv).getBody(), response.getBody());
    }
}
