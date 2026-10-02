package cz.iocb.sparql.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.testing.Database;



/**
 * The protocol handling of the servlet with stub requests and responses over an empty configuration: the
 * {@code version} parameter of the request (as a query parameter, a form parameter and a media type parameter), its
 * errors, and the announcement of RDF 1.2 in the results (the {@code version} member of the JSON head, the VERSION
 * directive of Turtle and N-Triples and the {@code version} parameter of the media types).
 */
public class EndpointServletTest
{
    private static final String select = "SELECT * WHERE {}";
    private static final String ask = "ASK {}";
    private static final String construct = "CONSTRUCT { <http://example.org/s> <http://example.org/p> 1 } WHERE {}";
    private static final String sparqlJson = "application/sparql-results+json";
    private static final String sparqlXml = "application/sparql-results+xml";
    private static final String turtle = "text/turtle";
    private static final String ntriples = "application/n-triples";
    private static final String form = "application/x-www-form-urlencoded";
    private static final String sparqlQuery = "application/sparql-query";

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


    @BeforeAll
    static void init() throws SQLException
    {
        servlet = new EndpointServlet(
                new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false)));
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
}
