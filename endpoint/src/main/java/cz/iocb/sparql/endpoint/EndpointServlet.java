package cz.iocb.sparql.endpoint;

import static cz.iocb.sparql.endpoint.EndpointServlet.OutputType.RDF_JSON;
import static cz.iocb.sparql.engine.translator.imcode.SqlConstruct.ConstructColumn.PREDICATE;
import static cz.iocb.sparql.engine.translator.imcode.SqlConstruct.ConstructColumn.SUBJECT;
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
import cz.iocb.sparql.engine.parser.model.DataSet;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.BNode;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.IriNode;
import cz.iocb.sparql.engine.request.LanguageTaggedLiteral;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.LiteralNode;
import cz.iocb.sparql.engine.request.RdfNode;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.request.Result.ResultType;
import cz.iocb.sparql.engine.request.TypedLiteral;



@SuppressWarnings("serial")
public class EndpointServlet extends HttpServlet
{
    static enum OutputType
    {
        NONE(""),
        SPARQL_XML("application/sparql-results+xml", ResultType.SELECT, ResultType.ASK),
        SPARQL_JSON("application/sparql-results+json", ResultType.SELECT, ResultType.ASK),
        RDF_XML("application/rdf+xml", ResultType.DESCRIBE, ResultType.CONSTRUCT),
        RDF_JSON("application/rdf+json", ResultType.DESCRIBE, ResultType.CONSTRUCT),
        NTRIPLES("application/n-triples", ResultType.DESCRIBE, ResultType.CONSTRUCT),
        NQUADS("application/n-quads", ResultType.DESCRIBE, ResultType.CONSTRUCT),
        TRIG("application/trig", ResultType.DESCRIBE, ResultType.CONSTRUCT),
        TURTLE("text/turtle", ResultType.DESCRIBE, ResultType.CONSTRUCT),
        TSV("text/tab-separated-values", ResultType.SELECT, ResultType.ASK, ResultType.DESCRIBE, ResultType.CONSTRUCT),
        CSV("text/csv", ResultType.SELECT, ResultType.ASK, ResultType.DESCRIBE, ResultType.CONSTRUCT);

        private final String mime;
        private final ResultType[] variants;

        private OutputType(String mime, ResultType... variants)
        {
            this.mime = mime;
            this.variants = variants;
        }

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

        public String getMime()
        {
            return mime;
        }
    }

    static class Graph extends LinkedHashMap<RdfNode, LinkedHashMap<RdfNode, LinkedHashSet<RdfNode>>>
    {
    }


    private static final Logger logger = LoggerFactory.getLogger(EndpointServlet.class);

    private Engine engine;
    private SparqlDatabaseConfiguration sparqlConfig;
    private int fetchSize = 1000;
    private long timeout = 1000 * 1000000000l;
    private long maxTimeout = 6000 * 1000000000l;
    private int sqlSizeLimit = 0;


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
                        dataSets.add(new DataSet(new IRI(defaultGraph), true));

                if(namedGraphs != null)
                    for(String namedGraph : namedGraphs)
                        dataSets.add(new DataSet(new IRI(namedGraph), false));
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
                List<String> order = format == RDF_JSON ? List.of(SUBJECT.getName(), PREDICATE.getName()) : List.of();

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


    private void processHtmlRequest(HttpServletResponse res) throws IOException
    {
        res.setContentType("text/html");

        // @formatter:off
        res.getOutputStream().print(
                "<!DOCTYPE html>\n" +
                "<html lang='en'>\n" +
                "  <head>\n" +
                "    <meta charset='utf-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1'>\n" +
                "    <title>YASGUI</title>\n" +
                "    <style>\n");

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
                "  </script>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <div id='yasgui'></div>\n" +
                "  </body>\n" +
                "</html>\n");
        // @formatter:on
    }


    private void processInfoRequest(HttpServletResponse res) throws IOException
    {
        res.setContentType("application/json");

        final IRI type = new IRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

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
                .map(m -> ((IRI) ((ConstantIriMapping) m.getPredicate()).getValue()).getValue()).distinct().sorted()
                .map(i -> "    \"" + i + "\"").collect(joining(",\n")));

        out.append("\n");
        out.append("  ],\n");
        out.append("  \"classes\": [\n");

        out.append(sparqlConfig.getMappings(sparqlConfig.getServiceIri()).stream()
                .filter(m -> m.getPredicate() instanceof ConstantIriMapping
                        && ((ConstantIriMapping) m.getPredicate()).getValue().equals(type)
                        && m.getObject() instanceof ConstantIriMapping)
                .map(m -> ((IRI) ((ConstantIriMapping) m.getObject()).getValue()).getValue()).distinct().sorted()
                .map(i -> "    \"" + i + "\"").collect(joining(",\n")));

        out.append("\n");
        out.append("  ]\n");
        out.append("}");
    }


    private static boolean isInfoRequest(HttpServletRequest req)
    {
        if(req.getParameter("info") != null && req.getParameter("query") == null)
            return true;

        return false;
    }


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


    private static void writeSelectXml(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        out.println("<?xml version=\"1.0\"?>");
        out.println("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">");

        out.println("\t<head>");

        for(String head : result.getHeads())
        {
            out.print("\t\t<variable name=\"");
            writeXmlValue(out, head);
            out.println("\"/>");
        }

        out.println("\t</head>");

        out.println("\t<results>");

        while(result.next())
        {
            out.println("\t\t<result>");

            for(int i = 0; i < result.getHeads().size(); i++)
            {
                RdfNode node = result.get(i);

                if(node == null)
                    continue;


                out.print("\t\t\t<binding name=\"");
                writeXmlValue(out, result.getHeads().get(i));
                out.print("\">");

                if(node instanceof IriNode)
                {
                    out.print("<uri>");
                    writeXmlValue(out, node.getValue());
                    out.print("</uri>");
                }
                else if(node instanceof LanguageTaggedLiteral)
                {
                    out.print("<literal xml:lang=\"");
                    writeXmlValue(out, ((LanguageTaggedLiteral) node).getLanguage());
                    out.print("\">");
                    writeXmlValue(out, node.getValue());
                    out.print("</literal>");
                }
                else if(node instanceof TypedLiteral)
                {
                    out.print("<literal datatype=\"");
                    writeXmlValue(out, ((TypedLiteral) node).getDatatype().getValue());
                    out.print("\">");
                    writeXmlValue(out, node.getValue());
                    out.print("</literal>");
                }
                else if(node instanceof LiteralNode)
                {
                    out.print("<literal>");
                    writeXmlValue(out, node.getValue());
                    out.print("</literal>");
                }
                else if(node instanceof BNode)
                {
                    out.print("<bnode>");
                    writeXmlValue(out, node.getValue());
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


    private static void writeSelectJson(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        out.print("{\n\t\"head\": { \"vars\": [ ");

        boolean hasHead = false;

        for(String head : result.getHeads())
        {
            if(hasHead)
                out.print(", ");
            else
                hasHead = true;

            out.print('"');
            writeJsonValue(out, head);
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
                RdfNode node = result.get(i);

                if(node == null)
                    continue;

                if(hasResultHead)
                    out.println(',');
                else
                    hasResultHead = true;

                out.print("\t\t\"");
                writeJsonValue(out, result.getHeads().get(i));
                out.print("\": ");
                writeJsonNode(out, node);
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


    private static void writeSelectTsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        boolean hasHead = false;

        for(String head : result.getHeads())
        {
            if(hasHead)
                out.print("\t");
            else
                hasHead = true;

            writeTsvValue(out, head);
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


    private static void writeSelectCsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        boolean hasHead = false;

        for(String head : result.getHeads())
        {
            if(hasHead)
                out.print(",");
            else
                hasHead = true;

            writeCsvValue(out, head);
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

                RdfNode node = result.get(i);

                if(node != null)
                    writeCsvValue(out, node.getValue());
            }

            out.print("\r\n");
        }
    }


    private static void writeAskXml(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        result.next();

        out.println("<?xml version=\"1.0\"?>");
        out.println("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">");
        out.println("\t<head></head>");
        out.println("\t<boolean>" + result.get(0).getValue() + "</boolean>");
        out.println("</sparql>");
    }


    private static void writeAskJson(PrintWriter out, Result result, boolean includeWarnings)
            throws IOException, SQLException
    {
        result.next();

        out.println("{");
        out.println("\t\"head\": { },");
        out.println("\t\"boolean\": " + result.get(0).getValue());
        out.println("}");
    }


    private static void writeAskTsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        result.next();

        out.println("\"bool\"");
        out.println(result.get(0).getValue());
    }


    private static void writeAskCsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        result.next();

        out.println("\"bool\"");
        out.println(result.get(0).getValue());
    }


    private static void writeGraphXml(PrintWriter out, Result result) throws IOException, SQLException
    {
        RdfNode subject = null;

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

                if(subject instanceof IriNode)
                    out.print("rdf:about=\"");
                else
                    out.print("rdf:nodeID=\"");

                writeXmlValue(out, subject.getValue());

                out.println("\">");
            }

            RdfNode predicate = result.get(1);
            RdfNode object = result.get(2);

            String prefix = predicate.getValue().replaceAll("[_a-zA-Z][_a-zA-Z0-9]*$", "");
            String name = predicate.getValue().substring(prefix.length());

            out.print("\t\t<p:");
            out.print(name);
            out.print(" xmlns:p=\"");
            writeXmlValue(out, prefix);
            out.print("\"");

            if(object instanceof IriNode)
            {
                out.print(" rdf:resource=\"");
                writeXmlValue(out, object.getValue());
                out.println("\"/>");
            }
            else if(object instanceof BNode)
            {
                out.print(" rdf:nodeID=\"");
                writeXmlValue(out, object.getValue());
                out.println("\"/>");
            }
            else
            {
                if(object instanceof LanguageTaggedLiteral)
                {
                    out.print(" xml:lang=\"");
                    writeXmlValue(out, ((LanguageTaggedLiteral) object).getLanguage());
                    out.print("\">");
                }
                else if(object instanceof TypedLiteral)
                {
                    out.print(" rdf:datatype=\"");
                    writeXmlValue(out, ((TypedLiteral) object).getDatatype().getValue());
                    out.print("\">");
                }
                else
                {
                    out.print(">");
                }

                writeXmlValue(out, object.getValue());
                out.print("</p:");
                out.print(name);
                out.println(">");
            }
        }

        if(subject != null)
            out.println("\t</rdf:Description>");

        out.println("</rdf:RDF>");
    }


    private static void writeGraphJson(PrintWriter out, Result result) throws IOException, SQLException
    {
        RdfNode subject = null;
        RdfNode predicate = null;

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
                writeJsonValue(out, subject.getValue());
                out.println("\" : {");
            }

            if(!result.get(1).equals(predicate))
            {
                if(predicate != null)
                    out.println("\n\t\t],");

                predicate = result.get(1);

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


    private static void writeGraphTurtle(PrintWriter out, Result result, HashMap<String, String> systemPrefixes)
            throws IOException, SQLException
    {
        HashMap<String, String> prefixes = new HashMap<String, String>();

        RdfNode subject = null;
        RdfNode predicate = null;

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


    private static void writeGraphCsv(PrintWriter out, Result result) throws IOException, SQLException
    {
        out.print("subject,predicate,object\r\n");

        while(result.next())
        {
            writeCsvValue(out, result.get(0).getValue());
            out.print(',');
            writeCsvValue(out, result.get(1).getValue());
            out.print(',');
            writeCsvValue(out, result.get(2).getValue());
            out.print("\r\n");
        }
    }


    private static void writeJsonNode(PrintWriter out, RdfNode node) throws IOException
    {
        out.print("{ \"type\": ");

        if(node instanceof IriNode)
            out.print("\"uri\",");
        else if(node instanceof LiteralNode)
            out.print("\"literal\",");
        else
            out.print("\"bnode\",");

        out.print(" \"value\": ");

        out.print('"');
        writeJsonValue(out, node.getValue());
        out.print('"');

        if(node instanceof LanguageTaggedLiteral)
        {
            out.print(", \"xml:lang\": ");

            out.print('"');
            writeJsonValue(out, ((LanguageTaggedLiteral) node).getLanguage());
            out.print('"');
        }
        else if(node instanceof TypedLiteral)
        {
            out.print(", \"datatype\": ");

            out.print('"');
            writeJsonValue(out, ((TypedLiteral) node).getDatatype().getValue());
            out.print('"');
        }

        out.print(" }");
    }


    private static void writeTripleNode(PrintWriter out, RdfNode node) throws IOException
    {
        if(node instanceof IriNode)
        {
            out.print('<');
            writeTsvIriValue(out, node.getValue());
            out.print('>');
        }
        else if(node instanceof LanguageTaggedLiteral)
        {
            out.print('"');
            writeTsvLiteralValue(out, node.getValue());
            out.print("\"@");
            writeTsvValue(out, ((LanguageTaggedLiteral) node).getLanguage());
        }
        else if(node instanceof TypedLiteral)
        {
            out.print('"');
            writeTsvLiteralValue(out, node.getValue());
            out.print("\"^^<");
            writeTsvIriValue(out, ((TypedLiteral) node).getDatatype().getValue());
            out.print('>');
        }
        else if(node instanceof LiteralNode)
        {
            out.print('"');
            writeTsvLiteralValue(out, node.getValue());
            out.print('"');
        }
        else if(node instanceof BNode)
        {
            out.print("_:");
            writeTsvValue(out, node.getValue());
        }
    }


    private static void writeTripleNode(PrintWriter out, RdfNode node, HashMap<String, String> prefixes)
            throws IOException
    {
        if(node instanceof IriNode)
        {
            writeTripleIri(out, (IriNode) node, prefixes);
        }
        else if(node instanceof TypedLiteral)
        {
            out.print('"');
            writeTsvLiteralValue(out, node.getValue());
            out.print("\"^^");
            writeTripleIri(out, ((TypedLiteral) node).getDatatype(), prefixes);
        }
        else
        {
            writeTripleNode(out, node);
        }
    }


    private static void writeTripleIri(PrintWriter out, IriNode node, HashMap<String, String> prefixes)
            throws IOException
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
