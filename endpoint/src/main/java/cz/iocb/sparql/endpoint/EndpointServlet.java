package cz.iocb.sparql.endpoint;

import static cz.iocb.sparql.endpoint.EndpointServlet.OutputType.RDF_JSON;
import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.PREDICATE;
import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.SUBJECT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.model.DataSet;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.request.Result.ResultType;



/**
 * SPARQL 1.1 protocol endpoint. Queries arrive by GET or by POST (URL-encoded form or {@code application/sparql-query})
 * and the result is serialised in the format chosen from the {@code format} parameter or the Accept header. A GET
 * without a query serves the bundled YASGUI page to browsers and the service description otherwise; {@code ?info}
 * returns a JSON summary of prefixes, properties and classes. Init parameters: {@code resource} (JNDI name of the
 * configuration), {@code fetch-size}, {@code timeout} and {@code max-timeout} (seconds), {@code sql-query-size-limit}.
 */
public class EndpointServlet extends HttpServlet
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Supported result serialisations with their MIME type and the result forms they apply to.
     */
    static enum OutputType
    {
        /**
         * No acceptable serialisation.
         */
        NONE(""),

        /**
         * SPARQL Query Results XML Format.
         */
        SPARQL_XML("application/sparql-results+xml", ResultType.SELECT, ResultType.ASK),

        /**
         * SPARQL Query Results JSON Format.
         */
        SPARQL_JSON("application/sparql-results+json", ResultType.SELECT, ResultType.ASK),

        /**
         * RDF/XML.
         */
        RDF_XML("application/rdf+xml", ResultType.DESCRIBE, ResultType.CONSTRUCT),

        /**
         * RDF/JSON.
         */
        RDF_JSON("application/rdf+json", ResultType.DESCRIBE, ResultType.CONSTRUCT),

        /**
         * N-Triples.
         */
        NTRIPLES("application/n-triples", ResultType.DESCRIBE, ResultType.CONSTRUCT),

        /**
         * N-Quads (all triples in the default graph).
         */
        NQUADS("application/n-quads", ResultType.DESCRIBE, ResultType.CONSTRUCT),

        /**
         * TriG (all triples in the default graph).
         */
        TRIG("application/trig", ResultType.DESCRIBE, ResultType.CONSTRUCT),

        /**
         * Turtle.
         */
        TURTLE("text/turtle", ResultType.DESCRIBE, ResultType.CONSTRUCT),

        /**
         * Tab-separated values.
         */
        TSV("text/tab-separated-values", ResultType.SELECT, ResultType.ASK, ResultType.DESCRIBE, ResultType.CONSTRUCT),

        /**
         * Comma-separated values.
         */
        CSV("text/csv", ResultType.SELECT, ResultType.ASK, ResultType.DESCRIBE, ResultType.CONSTRUCT);

        /**
         * MIME type.
         */
        private final String mime;

        /**
         * Result forms the serialisation applies to.
         */
        private final ResultType[] variants;

        /**
         * Creates the output type.
         *
         * @param mime the MIME type
         * @param variants result forms the serialisation applies to
         */
        private OutputType(String mime, ResultType... variants)
        {
            this.mime = mime;
            this.variants = variants;
        }


        /**
         * Output type of the MIME type applicable to the result form (any form when null); {@link #NONE} if there is
         * none.
         *
         * @param mime the MIME type
         * @param variant the sign variant
         * @return output type of the MIME type applicable to the result form (any form when null); {@link #NONE} if
         *         there is none
         */
        public static OutputType getOutputType(String mime, ResultType variant)
        {
            for(OutputType value : OutputType.values())
            {
                if(value.mime.equals(mime))
                    for(ResultType v : value.variants)
                        if(v == variant || variant == null)
                            return value;
            }

            return NONE;
        }


        /**
         * MIME type of the serialisation.
         *
         * @return MIME type of the serialisation
         */
        public String getMime()
        {
            return mime;
        }
    }


    /**
     * Triples grouped by subject and predicate, in insertion order, for graph serialisations.
     */
    static class Graph extends LinkedHashMap<RdfTerm, LinkedHashMap<RdfTerm, LinkedHashSet<RdfTerm>>>
    {
        /**
         * Serialization version.
         */
        private static final long serialVersionUID = 1L;


        /**
         * Creates an empty graph.
         */
        Graph()
        {
        }

    }


    /**
     * Logger of request processing.
     */
    private static final Logger logger = LoggerFactory.getLogger(EndpointServlet.class);

    /**
     * Engine creating the requests.
     */
    private Engine engine;

    /**
     * Configuration of the endpoint.
     */
    private SparqlDatabaseConfiguration sparqlConfig;

    /**
     * JDBC fetch size ({@code fetch-size} init parameter).
     */
    private int fetchSize = 1000;

    /**
     * Default timeout in nanoseconds ({@code timeout} init parameter, seconds).
     */
    private long timeout = 1000 * 1000000000l;

    /**
     * Maximum timeout a client may ask for, in nanoseconds ({@code max-timeout} init parameter, seconds).
     */
    private long maxTimeout = 6000 * 1000000000l;

    /**
     * Maximum length of the generated SQL; 0 for none ({@code sql-query-size-limit} init parameter).
     */
    private int sqlSizeLimit = 0;


    /**
     * Creates the servlet; the configuration is read in {@link #init}.
     */
    public EndpointServlet()
    {
    }


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        try
        {
            String resourceName = config.getInitParameter("resource");

            if(resourceName == null || resourceName.isEmpty())
                throw new IllegalArgumentException("resource name is not set");


            String fetchSizeParameter = config.getInitParameter("fetch-size");

            if(fetchSizeParameter != null)
                fetchSize = Integer.parseInt(fetchSizeParameter);


            String timeoutParameter = config.getInitParameter("timeout");

            if(timeoutParameter != null)
                timeout = Integer.parseInt(timeoutParameter) * 1000000000l;


            String maxTimeoutParameter = config.getInitParameter("max-timeout");

            if(maxTimeoutParameter != null)
                maxTimeout = Integer.parseInt(maxTimeoutParameter) * 1000000000l;


            String sqlSizeLimitParameter = config.getInitParameter("sql-query-size-limit");

            if(sqlSizeLimitParameter != null)
                sqlSizeLimit = Integer.parseInt(sqlSizeLimitParameter);


            Context context = (Context) (new InitialContext()).lookup("java:comp/env");
            sparqlConfig = (SparqlDatabaseConfiguration) context.lookup(resourceName);
            engine = new Engine(sparqlConfig);
        }
        catch(NamingException | NumberFormatException e)
        {
            throw new ServletException(e);
        }
    }


    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        setBasicHttpHeaders(req, res);
        super.doOptions(req, res);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        setBasicHttpHeaders(req, res);

        if(isHtmlRequest(req))
        {
            processHtmlRequest(res);
        }
        else if(isInfoRequest(req))
        {
            processInfoRequest(res);
        }
        else
        {
            String query = req.getParameter("query");
            String[] defaultGraphs = req.getParameterValues("default-graph-uri");
            String[] namedGraphs = req.getParameterValues("named-graph-uri");

            if(query == null)
            {
                query = sparqlConfig.getServiceDescriptionQuery();
                defaultGraphs = null;
                namedGraphs = null;
            }

            process(req, res, query, defaultGraphs, namedGraphs, getTimeout(req), sqlSizeLimit);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        if(req.getCharacterEncoding() == null)
            req.setCharacterEncoding("UTF-8");

        setBasicHttpHeaders(req, res);

        String query = null;
        String[] defaultGraphs = req.getParameterValues("default-graph-uri");
        String[] namedGraphs = req.getParameterValues("named-graph-uri");

        if(req.getContentType() != null && req.getContentType().matches("application/x-www-form-urlencoded.*"))
            query = req.getParameter("query");
        else if(req.getContentType() != null && req.getContentType().matches("application/sparql-query.*"))
            query = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);


        process(req, res, query, defaultGraphs, namedGraphs, getTimeout(req), sqlSizeLimit);
    }


    /**
     * Sets the encoding and CORS headers, and the attachment file name requested by the {@code filename} parameter.
     *
     * @param req the HTTP request
     * @param res the HTTP response
     */
    private static void setBasicHttpHeaders(HttpServletRequest req, HttpServletResponse res)
    {
        res.setCharacterEncoding("UTF-8");

        res.setHeader("Access-Control-Allow-Headers",
                "x-requested-with, Content-Type, origin, authorization, accept, client-security-token");
        res.setHeader("Access-Control-Allow-Origin", "*");

        String filename = req.getParameter("filename");

        if(filename != null)
            res.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }


    /**
     * Timeout in nanoseconds from the {@code timeout} parameter (seconds), clamped to the configured maximum; the
     * default when absent or invalid.
     *
     * @param req the HTTP request
     * @return timeout in nanoseconds from the {@code timeout} parameter (seconds), clamped to the configured maximum;
     *         the default when absent or invalid
     */
    private long getTimeout(HttpServletRequest req)
    {
        String timeoutParameter = req.getParameter("timeout");

        if(timeoutParameter == null)
            return timeout;

        try
        {
            return Math.min(maxTimeout, Math.max(1l, Integer.parseInt(timeoutParameter)) * 1000000000l);
        }
        catch(NumberFormatException e)
        {
            return timeout;
        }
    }


    /**
     * Reconstructs the request URL with its query parameters, for logging.
     *
     * @param req the HTTP request
     * @return the request URL with its query parameters
     */
    private String getRequestString(HttpServletRequest req)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(req.getRequestURI());

        int hasParam = 0;


        String maxrows = req.getParameter("maxrows");

        if(maxrows != null)
            builder.append((hasParam++ == 0 ? "&" : "?") + "maxrows=" + URLEncoder.encode(maxrows, UTF_8));


        String timeout = req.getParameter("timeout");

        if(timeout != null)
            builder.append((hasParam++ == 0 ? "&" : "?") + "timeout=" + URLEncoder.encode(timeout, UTF_8));


        String[] defaultGraphUris = req.getParameterValues("default-graph-uri");

        if(defaultGraphUris != null)
            for(String v : defaultGraphUris)
                builder.append((hasParam++ == 0 ? "&" : "?") + "default-graph-uri=" + URLEncoder.encode(v, UTF_8));


        String[] namedGraphUris = req.getParameterValues("named-graph-uri");

        if(namedGraphUris != null)
            for(String v : namedGraphUris)
                builder.append((hasParam++ == 0 ? "&" : "?") + "named-graph-uri=" + URLEncoder.encode(v, UTF_8));


        String query = req.getParameter("query");

        if(query != null)
            builder.append((hasParam++ == 0 ? "&" : "?") + "query=" + URLEncoder.encode(query, UTF_8));


        return builder.toString();
    }


    /**
     * Prepares and runs the query with the protocol dataset parameters and writes the result in the negotiated format;
     * errors are reported by HTTP status codes.
     *
     * @param req the HTTP request
     * @param res the HTTP response
     * @param query the query text
     * @param defaultGraphs IRIs of the default graphs given by the protocol, or null
     * @param namedGraphs IRIs of the named graphs given by the protocol, or null
     * @param timeout time limit in nanoseconds, 0 for none
     * @param sqlSizeLimit maximum length of the generated SQL, 0 for none
     * @throws IOException on output errors
     */
    private void process(HttpServletRequest req, HttpServletResponse res, String query, String[] defaultGraphs,
            String[] namedGraphs, long timeout, int sqlSizeLimit) throws IOException
    {
        try
        {
            MDC.put("request", getRequestString(req));

            if(query == null)
            {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }


            List<DataSet> dataSets = new ArrayList<>();

            try
            {
                if(defaultGraphs != null)
                    for(String defaultGraph : defaultGraphs)
                        dataSets.add(new DataSet(new IriNode(defaultGraph), true));

                if(namedGraphs != null)
                    for(String namedGraph : namedGraphs)
                        dataSets.add(new DataSet(new IriNode(namedGraph), false));
            }
            catch(IllegalArgumentException e)
            {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }


            // IOCB SPARQL protocol extension
            String warnings = req.getParameter("warnings");
            boolean includeWarnings = warnings != null ? Boolean.parseBoolean(warnings) : false;

            String reorder = req.getParameter("service-reorder");
            boolean serviceReorder = reorder != null ? Boolean.parseBoolean(reorder) : false;


            int limit = -1;

            // Virtuoso extension
            try
            {
                String value = req.getParameter("maxrows");

                if(value != null)
                    limit = Integer.parseInt(value);
            }
            catch(NumberFormatException e)
            {
            }


            try(Request request = engine.getRequest(serviceReorder))
            {
                PreparedQuery preparedQuery = request.prepareQuery(query, dataSets);
                OutputType format = detectOutputType(req, preparedQuery.getResultType());
                List<Variable> order = format == RDF_JSON ? List.of(SUBJECT.getVariable(), PREDICATE.getVariable()) :
                        List.of();

                try(Result result = request.execute(preparedQuery, order, 0, limit, fetchSize, timeout, sqlSizeLimit))
                {
                    res.setContentType(format.getMime());

                    switch(result.getResultType())
                    {
                        case ASK:
                            switch(format)
                            {
                                case SPARQL_JSON:
                                    writeAskJson(res.getWriter(), result, includeWarnings);
                                    break;
                                case SPARQL_XML:
                                    writeAskXml(res.getWriter(), result, includeWarnings);
                                    break;
                                case TSV:
                                    // non-standard extension
                                    writeAskTsv(res.getWriter(), result);
                                    break;
                                case CSV:
                                    // non-standard extension
                                    writeAskCsv(res.getWriter(), result);
                                    break;
                                default:
                                    res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                                    break;
                            }
                            break;

                        case SELECT:
                            switch(format)
                            {
                                case SPARQL_JSON:
                                    writeSelectJson(res.getWriter(), result, includeWarnings);
                                    break;
                                case SPARQL_XML:
                                    writeSelectXml(res.getWriter(), result, includeWarnings);
                                    break;
                                case TSV:
                                    writeSelectTsv(res.getWriter(), result);
                                    break;
                                case CSV:
                                    writeSelectCsv(res.getWriter(), result);
                                    break;
                                default:
                                    res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                                    break;
                            }
                            break;

                        case DESCRIBE:
                        case CONSTRUCT:
                            switch(format)
                            {
                                case RDF_XML:
                                    writeGraphXml(res.getWriter(), result);
                                    break;
                                case RDF_JSON:
                                    writeGraphJson(res.getWriter(), result);
                                    break;
                                case TURTLE:
                                case TRIG:
                                    writeGraphTurtle(res.getWriter(), result, engine.getConfig().getPrefixes());
                                    break;
                                case NTRIPLES:
                                case NQUADS:
                                    writeGraphTriples(res.getWriter(), result);
                                    break;
                                case TSV:
                                    writeGraphTsv(res.getWriter(), result);
                                    break;
                                case CSV:
                                    writeGraphCsv(res.getWriter(), result);
                                    break;
                                default:
                                    res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                                    break;
                            }
                            break;
                    }
                }
            }
        }
        catch(TranslateExceptions e)
        {
            try
            {
                res.resetBuffer();
            }
            catch(Throwable x)
            {
            }

            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setContentType("text/plain");

            PrintWriter out = res.getWriter();

            for(TranslateMessage message : e.getMessages())
                out.println(message.getCategory().getText() + ": " + message.getRange() + " " + message.getMessage());
        }
        catch(LimitExceedException e)
        {
            try
            {
                res.resetBuffer();
            }
            catch(Throwable x)
            {
            }

            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.setContentType("text/plain");

            PrintWriter out = res.getWriter();

            out.println(e.getMessage());
        }
        catch(SQLException e)
        {
            try
            {
                res.resetBuffer();
            }
            catch(Throwable x)
            {
            }

            if(e.getErrorCode() == 0 && "57014".equals(e.getSQLState()))
            {
                res.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
                res.setContentType("text/plain");
                res.getWriter().println("request timeout");
            }
            else
            {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.setContentType("text/plain");
                res.getWriter().println("error: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            }
        }
        catch(Throwable e)
        {
            System.err.println("EndpointServlet: log begin");

            if(defaultGraphs != null)
                for(String defaultGraph : defaultGraphs)
                    System.err.println("default graph: " + defaultGraph);

            if(namedGraphs != null)
                for(String namedGraph : namedGraphs)
                    System.err.println("named graph: " + namedGraph);

            System.err.println(query);
            e.printStackTrace(System.err);
            System.err.println("EndpointServlet: log end");

            try
            {
                res.resetBuffer();
            }
            catch(Throwable x)
            {
            }

            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.setContentType("text/plain");
            res.getWriter().println("error: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
        }
        finally
        {
            int status = res.getStatus();

            switch(res.getStatus())
            {
                case HttpServletResponse.SC_OK -> logger.info(Integer.toString(status));
                case HttpServletResponse.SC_INTERNAL_SERVER_ERROR -> logger.error(Integer.toString(status));
                default -> logger.warn(Integer.toString(status));
            }

            MDC.remove("request");
        }
    }


    /**
     * Serves the bundled YASGUI page with the embedded endpoint script.
     *
     * @param res the HTTP response
     * @throws IOException on output errors
     */
    private void processHtmlRequest(HttpServletResponse res) throws IOException
    {
        res.setContentType("text/html");

        // @formatter:off
        res.getOutputStream().print(
                """
                    <!DOCTYPE html>
                    <html lang='en'>
                      <head>
                        <meta charset='utf-8'>
                        <meta name='viewport' content='width=device-width, initial-scale=1'>
                        <title>YASGUI</title>
                        <style>
                    """);

                try(InputStream stream = getClass().getResourceAsStream("yasgui.min.css"))
                {
                    stream.transferTo(res.getOutputStream());
                }

        res.getOutputStream().print(
                "    </style>\n" +
                "    <script>\n");

        try(InputStream stream = getClass().getResourceAsStream("yasgui.min.js"))
        {
            stream.transferTo(res.getOutputStream());
        }

        res.getOutputStream().print(
                "    </script>\n" +
                "    <script>\n");

        try(InputStream stream = getClass().getResourceAsStream("endpoint.js"))
        {
            stream.transferTo(res.getOutputStream());
        }

        res.getOutputStream().print(
                """
                      </script>
                      </head>
                      <body>
                        <div id='yasgui'></div>
                      </body>
                    </html>
                    """);
        // @formatter:on
    }


    /**
     * Serves the JSON summary of prefixes, predicates and classes known from constant mappings.
     *
     * @param res the HTTP response
     * @throws IOException on output errors
     */
    private void processInfoRequest(HttpServletResponse res) throws IOException
    {
        res.setContentType("application/json");

        final Iri type = new Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

        PrintWriter out = res.getWriter();

        out.append("{\n");
        out.append("  \"prefixes\": {\n");

        out.append(sparqlConfig.getPrefixes().entrySet().stream()
                .map(e -> "    \"" + e.getKey() + "\": \"" + e.getValue() + "\"").collect(joining(",\n")));

        out.append("\n");
        out.append("  },\n");
        out.append("  \"properties\": [\n");

        //TODO: include predicates and classes not only from constant iri mappings

        out.append(sparqlConfig.getMappings(sparqlConfig.getServiceIri()).stream()
                .filter(m -> m.getPredicate() instanceof ConstantIriMapping)
                .map(m -> ((Iri) ((ConstantIriMapping) m.getPredicate()).getValue()).getValue()).distinct().sorted()
                .map(i -> "    \"" + i + "\"").collect(joining(",\n")));

        out.append("\n");
        out.append("  ],\n");
        out.append("  \"classes\": [\n");

        out.append(sparqlConfig.getMappings(sparqlConfig.getServiceIri()).stream()
                .filter(m -> m.getPredicate() instanceof ConstantIriMapping
                        && ((ConstantIriMapping) m.getPredicate()).getValue().equals(type)
                        && m.getObject() instanceof ConstantIriMapping)
                .map(m -> ((Iri) ((ConstantIriMapping) m.getObject()).getValue()).getValue()).distinct().sorted()
                .map(i -> "    \"" + i + "\"").collect(joining(",\n")));

        out.append("\n");
        out.append("  ]\n");
        out.append("}");
    }


    /**
     * True for a request with the {@code info} parameter and no query.
     *
     * @param req the HTTP request
     * @return true for a request with the {@code info} parameter and no query, false otherwise
     */
    private static boolean isInfoRequest(HttpServletRequest req)
    {
        if(req.getParameter("info") != null && req.getParameter("query") == null)
            return true;

        return false;
    }


    /**
     * True for a browser request without a query, {@code format} or {@code info} parameter.
     *
     * @param req the HTTP request
     * @return true for a browser request without a query, {@code format} or {@code info} parameter, false otherwise
     */
    private static boolean isHtmlRequest(HttpServletRequest req)
    {
        if(req.getParameter("info") != null)
            return false;

        if(req.getParameter("format") != null)
            return false;

        if(req.getHeader("accept") == null)
            return true;


        String accepts = req.getHeader("accept");
        boolean html = false;
        double quality = 0;

        // to not split around a comma inside quotation marks
        accepts = accepts.replaceAll("\"[^\"]*\"", "");

        for(String value : accepts.split("[\t ]*,[\t ]*"))
        {
            String[] parts = value.split("[\t ]*;[\t ]*");

            if(parts.length == 0)
                continue;

            double qvalue = 1;

            for(int i = 1; i < parts.length; i++)
                if(parts[i].matches("q=(0(\\.[0-9]{0,3})?|1(\\.0{0,3})?)[\\t ]*"))
                    qvalue = Double.parseDouble(parts[i].substring(2).replaceAll("[\t ]", ""));

            if(qvalue == 0 || qvalue <= quality)
                continue;

            String mime = parts[0].replaceAll("[\t ]", "");

            if(mime.equals("text/html") || mime.equals("text/*") || mime.equals("*/*"))
            {
                html = true;
                quality = qvalue;
            }
            else if(OutputType.getOutputType(mime, null) != OutputType.NONE)
            {
                html = false;
                quality = qvalue;
            }
        }

        if(quality == 0)
            return true;

        return html;
    }


    /**
     * Chooses the serialisation: from the {@code format} parameter (short names or a MIME type), else from the Accept
     * header by quality, else the XML default of the result form.
     *
     * @param req the HTTP request
     * @param form the result form
     * @return the output type
     */
    private static OutputType detectOutputType(HttpServletRequest req, ResultType form)
    {
        String format = req.getParameter("format");

        if(format != null)
        {
            // extension for compatibility with sparqlwrapper

            if(form == ResultType.ASK || form == ResultType.SELECT)
            {
                switch(format)
                {
                    case "xml":
                        return OutputType.SPARQL_XML;
                    case "json":
                        return OutputType.SPARQL_JSON;
                    case "csv":
                        return OutputType.CSV;
                    case "tsv":
                        return OutputType.TSV;
                }
            }
            else if(form == ResultType.CONSTRUCT || form == ResultType.DESCRIBE)
            {
                switch(format)
                {
                    case "rdf":
                    case "xml":
                    case "rdf+xml":
                        return OutputType.RDF_XML;
                    case "json":
                        return OutputType.RDF_JSON;
                    case "turtle":
                        return OutputType.TURTLE;
                    case "n3":
                        return OutputType.NTRIPLES;
                    case "csv":
                        return OutputType.CSV;
                    case "tsv":
                        return OutputType.TSV;
                }
            }


            // IOCB SPARQL protocol extension

            OutputType type = detectOutputTypeFromMIME(form, format);

            if(type != OutputType.NONE)
                return type;
        }


        String accept = req.getHeader("accept");

        if(accept != null)
            return detectOutputTypeFromMIME(form, accept);
        else if(form == ResultType.ASK || form == ResultType.SELECT)
            return OutputType.SPARQL_XML;
        else
            return OutputType.RDF_XML;
    }


    /**
     * Best output type for the result form among the MIME types of an Accept-style list, honouring quality values.
     *
     * @param form the result form
     * @param accepts the Accept header value
     * @return best output type for the result form among the MIME types of an Accept-style list, honouring quality
     *         values
     */
    private static OutputType detectOutputTypeFromMIME(ResultType form, String accepts)
    {
        OutputType type = OutputType.NONE;
        double quality = 0;

        // to not split around a comma inside quotation marks
        accepts = accepts.replaceAll("\"[^\"]*\"", "");

        for(String value : accepts.split("[\t ]*,[\t ]*"))
        {
            String[] parts = value.split("[\t ]*;[\t ]*");

            if(parts.length == 0)
                continue;

            double qvalue = 1;

            for(int i = 1; i < parts.length; i++)
                if(parts[i].matches("q=(0(\\.[0-9]{0,3})?|1(\\.0{0,3})?)[\\t ]*"))
                    qvalue = Double.parseDouble(parts[i].substring(2).replaceAll("[\t ]", ""));

            if(qvalue == 0 || qvalue <= quality)
                continue;

            String mime = parts[0].replaceAll("[\t ]", "");
            OutputType detected = OutputType.getOutputType(mime, form);

            if(detected != OutputType.NONE)
            {
                type = detected;
                quality = qvalue;
            }
            else if(mime.equals("text/*"))
            {
                quality = qvalue;
                type = OutputType.TSV;
            }
            else if(mime.equals("*") || mime.equals("*/*") || mime.equals("application/*"))
            {
                quality = qvalue;
                type = form == ResultType.ASK || form == ResultType.SELECT ? OutputType.SPARQL_XML : OutputType.RDF_XML;
            }
        }

        return type;
    }


    /**
     * Writes SELECT results in the SPARQL XML format, optionally with the warnings.
     *
     * @param out the output writer
     * @param result the result to write
     * @param includeWarnings whether to include the statement warnings
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeSelectXml(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        out.println("<?xml version=\"1.0\"?>");
        out.println("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">");

        out.println("\t<head>");

        for(Variable head : result.getHeads())
        {
            out.print("\t\t<variable name=\"");
            writeXmlValue(out, head.getName());
            out.println("\"/>");
        }

        out.println("\t</head>");

        out.println("\t<results>");

        while(result.next())
        {
            out.println("\t\t<result>");

            for(int i = 0; i < result.getHeads().size(); i++)
            {
                RdfTerm term = result.get(i);

                if(term == null)
                    continue;


                out.print("\t\t\t<binding name=\"");
                writeXmlValue(out, result.getHeads().get(i).getName());
                out.print("\">");

                if(term instanceof Iri iri)
                {
                    out.print("<uri>");
                    writeXmlValue(out, iri.getValue());
                    out.print("</uri>");
                }
                else if(term instanceof LangStringLiteral literal)
                {
                    out.print("<literal xml:lang=\"");
                    writeXmlValue(out, literal.getTag());
                    out.print("\">");
                    writeXmlValue(out, literal.getValue());
                    out.print("</literal>");
                }
                else if(term instanceof TypedLiteral literal)
                {
                    out.print("<literal datatype=\"");
                    writeXmlValue(out, literal.getType().getValue());
                    out.print("\">");
                    writeXmlValue(out, literal.getValue());
                    out.print("</literal>");
                }
                else if(term instanceof BlankNode bnode)
                {
                    out.print("<bnode>");
                    writeXmlValue(out, bnode.getLabel());
                    out.print("</bnode>");
                }

                out.println("</binding>");
            }

            out.println("\t\t</result>");
        }

        out.println("\t</results>");

        if(includeWarnings)
        {
            out.println("\t<warnings>");

            for(String warning : result.getWarnings())
            {
                out.print("\t\t<warning>");
                writeXmlValue(out, warning);
                out.println("</warning>");
            }

            out.println("\t</warnings>");
        }

        out.println("</sparql>");
    }


    /**
     * Writes SELECT results in the SPARQL JSON format, optionally with the warnings.
     *
     * @param out the output writer
     * @param result the result to write
     * @param includeWarnings whether to include the statement warnings
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeSelectJson(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        out.print("{\n\t\"head\": { \"vars\": [ ");

        boolean hasHead = false;

        for(Variable head : result.getHeads())
        {
            if(hasHead)
                out.print(", ");
            else
                hasHead = true;

            out.print('"');
            writeJsonValue(out, head.getName());
            out.print('"');
        }

        out.println(" ]},\n\t\"results\": { \"bindings\": [");


        boolean hasResult = false;

        while(result.next())
        {
            if(hasResult)
                out.println(",");
            else
                hasResult = true;

            out.println("\t{");

            boolean hasResultHead = false;

            for(int i = 0; i < result.getHeads().size(); i++)
            {
                RdfTerm term = result.get(i);

                if(term == null)
                    continue;

                if(hasResultHead)
                    out.println(',');
                else
                    hasResultHead = true;

                out.print("\t\t\"");
                writeJsonValue(out, result.getHeads().get(i).getName());
                out.print("\": ");
                writeJsonNode(out, term);
            }

            out.print("\n\t}");
        }

        out.print("\n\t]}");

        if(includeWarnings)
        {
            out.print(",\n\t\"warnings\": { \"messages\": [\n");

            boolean hasWarning = false;

            for(String warning : result.getWarnings())
            {
                if(hasWarning)
                    out.print(",\n");
                else
                    hasWarning = true;

                out.print("\t\t\"");
                writeJsonValue(out, warning);
                out.print("\"");
            }

            out.print("\n\t]}");
        }

        out.print("\n}");
    }


    /**
     * Writes SELECT results as TSV.
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeSelectTsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        boolean hasHead = false;

        for(Variable head : result.getHeads())
        {
            if(hasHead)
                out.print("\t");
            else
                hasHead = true;

            writeTsvValue(out, head.getName());
        }

        out.print("\r\n");


        while(result.next())
        {
            boolean hasResult = false;

            for(int i = 0; i < result.getHeads().size(); i++)
            {
                if(hasResult)
                    out.print('\t');
                else
                    hasResult = true;

                writeTripleNode(out, result.get(i));
            }

            out.print("\r\n");
        }
    }


    /**
     * Writes SELECT results as CSV.
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeSelectCsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        boolean hasHead = false;

        for(Variable head : result.getHeads())
        {
            if(hasHead)
                out.print(",");
            else
                hasHead = true;

            writeCsvValue(out, head.getName());
        }

        out.print("\r\n");


        while(result.next())
        {
            boolean hasResult = false;

            for(int i = 0; i < result.getHeads().size(); i++)
            {
                if(hasResult)
                    out.print(',');
                else
                    hasResult = true;

                RdfTerm term = result.get(i);

                if(term != null)
                    writeCsvNode(out, term);
            }

            out.print("\r\n");
        }
    }


    /**
     * Writes an ASK result in the SPARQL XML format, optionally with the warnings.
     *
     * @param out the output writer
     * @param result the result to write
     * @param includeWarnings whether to include the statement warnings
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeAskXml(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        result.next();

        out.println("<?xml version=\"1.0\"?>");
        out.println("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">");
        out.println("\t<head></head>");
        out.println("\t<boolean>" + ((Literal) result.get(0)).getValue() + "</boolean>");
        out.println("</sparql>");
    }


    /**
     * Writes an ASK result in the SPARQL JSON format, optionally with the warnings.
     *
     * @param out the output writer
     * @param result the result to write
     * @param includeWarnings whether to include the statement warnings
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeAskJson(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        result.next();

        out.println("{");
        out.println("\t\"head\": { },");
        out.println("\t\"boolean\": " + ((Literal) result.get(0)).getValue());
        out.println("}");
    }


    /**
     * Writes an ASK result as TSV (non-standard).
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeAskTsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        result.next();

        out.println("\"bool\"");
        out.println(((Literal) result.get(0)).getValue());
    }


    /**
     * Writes an ASK result as CSV (non-standard).
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeAskCsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        result.next();

        out.println("\"bool\"");
        out.println(((Literal) result.get(0)).getValue());
    }


    /**
     * Writes a graph result as RDF/XML, grouping triples by subject.
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeGraphXml(PrintWriter out, Result result) throws IOException, SQLException
    {
        RdfTerm subject = null;

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");


        while(result.next())
        {
            if(!result.get(0).equals(subject))
            {
                if(subject != null)
                    out.println("\t</rdf:Description>");

                subject = result.get(0);

                out.print("\t<rdf:Description ");

                if(subject instanceof Iri iri)
                {
                    out.print("rdf:about=\"");
                    writeXmlValue(out, iri.getValue());
                }
                else if(subject instanceof BlankNode bnode)
                {
                    out.print("rdf:nodeID=\"");
                    writeXmlValue(out, bnode.getLabel());
                }
                else
                {
                    throw new IllegalArgumentException();
                }

                out.println("\">");
            }

            Iri predicate = (Iri) result.get(1);
            RdfTerm object = result.get(2);

            String prefix = predicate.getValue().replaceAll("[_a-zA-Z][_a-zA-Z0-9]*$", "");
            String name = predicate.getValue().substring(prefix.length());

            out.print("\t\t<p:");
            out.print(name);
            out.print(" xmlns:p=\"");
            writeXmlValue(out, prefix);
            out.print("\"");

            if(object instanceof Iri iri)
            {
                out.print(" rdf:resource=\"");
                writeXmlValue(out, iri.getValue());
                out.println("\"/>");
            }
            else if(object instanceof BlankNode node)
            {
                out.print(" rdf:nodeID=\"");
                writeXmlValue(out, node.getLabel());
                out.println("\"/>");
            }
            if(object instanceof LangStringLiteral literal)
            {
                out.print(" xml:lang=\"");
                writeXmlValue(out, literal.getTag());
                out.print("\">");
                writeXmlValue(out, literal.getValue());
                out.print("</p:");
                out.print(name);
                out.println(">");
            }
            else if(object instanceof TypedLiteral literal)
            {
                out.print(" rdf:datatype=\"");
                writeXmlValue(out, literal.getValue());
                out.print("\">");
                writeXmlValue(out, literal.getValue());
                out.print("</p:");
                out.print(name);
                out.println(">");
            }
            else
            {
                throw new IllegalArgumentException();
            }
        }

        if(subject != null)
            out.println("\t</rdf:Description>");

        out.println("</rdf:RDF>");
    }


    /**
     * Writes a graph result as RDF/JSON; the rows must be ordered by subject and predicate.
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeGraphJson(PrintWriter out, Result result) throws IOException, SQLException
    {
        RdfTerm subject = null;
        Iri predicate = null;

        out.println("{");

        while(result.next())
        {
            if(!result.get(0).equals(subject))
            {
                if(subject != null)
                    out.println("\n\t\t]\n\t},");

                subject = result.get(0);
                predicate = null;

                out.print("\t\"");

                if(subject instanceof Iri iri)
                    writeJsonValue(out, iri.getValue());
                else
                    writeJsonValue(out, ((BlankNode) subject).getLabel());

                out.println("\" : {");
            }

            if(!result.get(1).equals(predicate))
            {
                if(predicate != null)
                    out.println("\n\t\t],");

                predicate = (Iri) result.get(1);

                out.print("\t\t\"");
                writeJsonValue(out, predicate.getValue());
                out.println("\" : [");
            }
            else
            {
                out.println(",");
            }

            out.print("\t\t\t");
            writeJsonNode(out, result.get(2));
        }

        if(predicate != null)
            out.print("\n\t\t]");

        if(subject != null)
            out.print("\n\t}");

        out.println("\n}");
    }


    /**
     * Writes a graph result as Turtle, grouping triples by subject and abbreviating IRIs by the configured prefixes.
     *
     * @param out the output writer
     * @param result the result to write
     * @param systemPrefixes prefixes for abbreviating IRIs
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeGraphTurtle(PrintWriter out, Result result, Map<String, String> systemPrefixes)
            throws IOException, SQLException
    {
        Map<String, String> prefixes = new HashMap<>();

        RdfTerm subject = null;
        RdfTerm predicate = null;

        while(result.next())
        {
            if(!result.get(0).equals(subject))
            {
                if(subject != null)
                    out.print(" .\n");

                subject = result.get(0);
                predicate = null;

                writeTripleNode(out, subject, prefixes);
                out.print(" ");
            }

            if(!result.get(1).equals(predicate))
            {
                if(predicate != null)
                    out.print(";\n\t");

                predicate = result.get(1);

                writeTripleNode(out, predicate, prefixes);
                out.print(" ");
            }
            else
            {
                out.print(",\n\t\t");
            }

            writeTripleNode(out, result.get(2), prefixes);
        }

        if(subject != null)
            out.print(" .\n");
    }


    /**
     * Writes a graph result as N-Triples (also used for N-Quads and TriG).
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeGraphTriples(PrintWriter out, Result result) throws IOException, SQLException
    {
        while(result.next())
        {
            writeTripleNode(out, result.get(0));
            out.print(' ');
            writeTripleNode(out, result.get(1));
            out.print(' ');
            writeTripleNode(out, result.get(2));
            out.println('.');
        }
    }


    /**
     * Writes a graph result as TSV.
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeGraphTsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        out.print("subject\tpredicate\tobject\r\n");

        while(result.next())
        {
            writeTripleNode(out, result.get(0));
            out.print('\t');
            writeTripleNode(out, result.get(1));
            out.print('\t');
            writeTripleNode(out, result.get(2));
            out.print("\r\n");
        }
    }


    /**
     * Writes a graph result as CSV.
     *
     * @param out the output writer
     * @param result the result to write
     * @throws IOException on output errors
     * @throws SQLException on database errors
     */
    private static void writeGraphCsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        out.print("subject,predicate,object\r\n");

        while(result.next())
        {
            writeCsvNode(out, result.get(0));
            out.print(',');
            writeCsvNode(out, result.get(1));
            out.print(',');
            writeCsvNode(out, result.get(2));
            out.print("\r\n");
        }
    }


    /**
     * Writes a term as a SPARQL JSON result value object.
     *
     * @param out the output writer
     * @param term the RDF term
     * @throws IOException on output errors
     */
    private static void writeJsonNode(PrintWriter out, RdfTerm term) throws IOException
    {
        out.print("{ \"type\": ");

        if(term instanceof Iri iri)
        {
            out.print("\"uri\", \"value\": \"");
            writeJsonValue(out, iri.getValue());
            out.print('"');
        }
        else if(term instanceof LangStringLiteral literal)
        {
            out.print("\"literal\", \"value\": \"");
            writeJsonValue(out, literal.getValue());
            out.print("\", \"xml:lang\": \"");
            writeJsonValue(out, literal.getTag());
            out.print('"');
        }
        else if(term instanceof TypedLiteral literal)
        {
            out.print("\"literal\", \"value\": \"");
            writeJsonValue(out, literal.getValue());
            out.print("\", \"datatype\": \"");
            writeJsonValue(out, literal.getType().getValue());
            out.print('"');
        }
        else if(term instanceof BlankNode bnode)
        {
            out.print("\"bnode\", \"value\": \"");
            writeJsonValue(out, bnode.getLabel());
            out.print('"');
        }

        out.print(" }");
    }


    /**
     * Writes a term in N-Triples syntax.
     *
     * @param out the output writer
     * @param term the RDF term
     * @throws IOException on output errors
     */
    private static void writeTripleNode(PrintWriter out, RdfTerm term) throws IOException
    {
        if(term instanceof Iri iri)
        {
            out.print('<');
            writeTsvIriValue(out, iri.getValue());
            out.print('>');
        }
        else if(term instanceof LangStringLiteral literal)
        {
            out.print('"');
            writeTsvLiteralValue(out, literal.getValue());
            out.print("\"@");
            writeTsvValue(out, literal.getTag());
        }
        else if(term instanceof TypedLiteral literal)
        {
            out.print('"');
            writeTsvLiteralValue(out, literal.getValue());
            out.print("\"^^<");
            writeTsvIriValue(out, literal.getType().getValue());
            out.print('>');
        }
        else if(term instanceof BlankNode bnode)
        {
            out.print("_:");
            writeTsvValue(out, bnode.getLabel());
        }
    }


    /**
     * Writes a term in Turtle syntax, abbreviating IRIs by the prefixes.
     *
     * @param out the output writer
     * @param term the RDF term
     * @param prefixes prefixes by name
     * @throws IOException on output errors
     */
    private static void writeTripleNode(PrintWriter out, RdfTerm term, Map<String, String> prefixes) throws IOException
    {
        if(term instanceof Iri iri)
        {
            writeTripleIri(out, iri, prefixes);
        }
        else if(term instanceof TypedLiteral literal)
        {
            out.print('"');
            writeTsvLiteralValue(out, literal.getValue());
            out.print("\"^^");
            writeTripleIri(out, literal.getType(), prefixes);
        }
        else
        {
            writeTripleNode(out, term);
        }
    }


    /**
     * Writes an IRI as a prefixed name when a prefix applies, otherwise in angle brackets.
     *
     * @param out the output writer
     * @param node the AST node
     * @param prefixes prefixes by name
     * @throws IOException on output errors
     */
    private static void writeTripleIri(PrintWriter out, Iri node, Map<String, String> prefixes) throws IOException
    {
        String iri = node.getValue();

        for(Entry<String, String> prefix : prefixes.entrySet())
        {
            if(iri.startsWith(prefix.getValue()))
            {
                String name = iri.substring(prefix.getValue().length());

                if(name.matches("[_a-zA-Z][_a-zA-Z0-9]*"))
                {
                    out.print(prefix.getKey());
                    out.print(':');
                    out.print(name);
                    return;
                }
            }
        }

        writeTripleNode(out, node);
    }


    /**
     * Writes a term as a CSV field (IRIs and blank nodes as they are, literals by their lexical form).
     *
     * @param out the output writer
     * @param term the RDF term
     * @throws IOException on output errors
     */
    private static void writeCsvNode(PrintWriter out, RdfTerm term) throws IOException
    {
        if(term instanceof Iri iri)
        {
            writeCsvValue(out, iri.getValue());
        }
        else if(term instanceof Literal literal)
        {
            writeCsvValue(out, literal.getValue());
        }
        else if(term instanceof BlankNode bnode)
        {
            writeCsvValue(out, "_:" + bnode.getLabel());
        }
    }


    /**
     * Writes text with the XML special characters escaped.
     *
     * @param out the output writer
     * @param value the value
     * @throws IOException on output errors
     */
    private static void writeXmlValue(PrintWriter out, String value) throws IOException
    {
        for(char val : value.toCharArray())
        {
            if(val == '"')
                out.print("&quot;");
            else if(val == '\'')
                out.print("&apos;");
            else if(val == '<')
                out.print("&lt;");
            else if(val == '>')
                out.print("&gt;");
            else if(val == '&')
                out.print("&amp;");
            else
                out.print(val);
        }
    }


    /**
     * Writes text with the JSON special characters escaped.
     *
     * @param out the output writer
     * @param value the value
     * @throws IOException on output errors
     */
    private static void writeJsonValue(PrintWriter out, String value) throws IOException
    {
        for(char val : value.toCharArray())
        {
            if(val == '"')
                out.print("\\\"");
            else if(val == '\\')
                out.print("\\\\");
            else if(val == '\n')
                out.print("\\n");
            else if(val >= 32)
                out.print(val);
            else if(val < 10)
                out.print("\\u000" + (int) val);
            else if(val < 16)
                out.print("\\u000" + ('A' + val - 10));
            else if(val < 26)
                out.print("\\u001" + (val - 16));
            else if(val < 32)
                out.print("\\u001" + ('A' + val - 26));
        }
    }


    /**
     * Writes text with the TSV control characters escaped.
     *
     * @param out the output writer
     * @param value the value
     * @throws IOException on output errors
     */
    private static void writeTsvValue(PrintWriter out, String value) throws IOException
    {
        for(char val : value.toCharArray())
        {
            if(val == '\\')
                out.print("\\\\");
            else if(val == '\t')
                out.print("\\t");
            else if(val == '\r')
                out.print("\\r");
            else if(val == '\n')
                out.print("\\n");
            else
                out.print(val);
        }
    }


    /**
     * Writes an IRI as a TSV field in angle brackets, escaping control characters.
     *
     * @param out the output writer
     * @param value the value
     * @throws IOException on output errors
     */
    private static void writeTsvIriValue(PrintWriter out, String value) throws IOException
    {
        for(char val : value.toCharArray())
        {
            if(val == '\\')
                out.print("\\\\");
            else if(val == '\t')
                out.print("\\t");
            else if(val == '\r')
                out.print("\\r");
            else if(val == '\n')
                out.print("\\n");
            else if(val == '>')
                out.print("\\>");
            else
                out.print(val);
        }
    }


    /**
     * Writes a literal lexical form as a quoted TSV field with escapes.
     *
     * @param out the output writer
     * @param value the value
     * @throws IOException on output errors
     */
    private static void writeTsvLiteralValue(PrintWriter out, String value) throws IOException
    {
        for(char val : value.toCharArray())
        {
            if(val == '\\')
                out.print("\\\\");
            else if(val == '\t')
                out.print("\\t");
            else if(val == '\r')
                out.print("\\r");
            else if(val == '\n')
                out.print("\\n");
            else if(val == '"')
                out.print("\\\"");
            else
                out.print(val);
        }
    }


    /**
     * Writes text as a quoted CSV field when it contains special characters.
     *
     * @param out the output writer
     * @param value the value
     * @throws IOException on output errors
     */
    private static void writeCsvValue(PrintWriter out, String value) throws IOException
    {
        boolean mustBeQuoted = false;

        for(char val : value.toCharArray())
        {
            if(val == '"' || val == ',' || val == '\n' || val == '\r')
            {
                mustBeQuoted = true;
                break;
            }
        }


        if(mustBeQuoted)
            out.print('"');

        for(char val : value.toCharArray())
        {
            if(val == '"')
                out.print("\"\"");
            else
                out.print(val);
        }

        if(mustBeQuoted)
            out.print('"');
    }
}
