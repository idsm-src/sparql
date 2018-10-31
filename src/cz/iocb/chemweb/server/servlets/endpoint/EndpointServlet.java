package cz.iocb.chemweb.server.servlets.endpoint;

import java.io.IOException;
import java.io.PrintWriter;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import cz.iocb.chemweb.server.db.BlankNode;
import cz.iocb.chemweb.server.db.IriNode;
import cz.iocb.chemweb.server.db.LanguageTaggedLiteral;
import cz.iocb.chemweb.server.db.Literal;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.TypedLiteral;
import cz.iocb.chemweb.server.db.postgresql.PostgresDatabase;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseExceptions;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.shared.services.DatabaseException;



@SuppressWarnings("serial")
public class EndpointServlet extends HttpServlet
{
    static enum OutputType
    {
        NONE, XML, JSON, TSV, CSV
    }


    private SparqlDatabaseConfiguration dbConfig;
    private Parser parser;
    private PostgresDatabase db;


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        String resourceName = config.getInitParameter("resource");

        if(resourceName == null || resourceName.isEmpty())
            throw new IllegalArgumentException("Resource name is not set");


        try
        {
            Context context = (Context) (new InitialContext()).lookup("java:comp/env");
            dbConfig = (SparqlDatabaseConfiguration) context.lookup(resourceName);
            parser = new Parser(dbConfig.getProcedures(), dbConfig.getPrefixes());
            db = new PostgresDatabase(dbConfig.getConnectionPool());
        }
        catch(NamingException e)
        {
            throw new ServletException(e);
        }
    }


    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        res.setHeader("access-control-allow-headers",
                "x-requested-with, Content-Type, origin, authorization, accept, client-security-token");
        res.setHeader("access-control-allow-origin", "*");
        super.doOptions(req, res);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        String query = req.getParameter("query");
        String[] defaultGraphs = req.getParameterValues("default-graph-uri");
        String[] namedGraphs = req.getParameterValues("named-graph-uri");

        process(req, res, query, defaultGraphs, namedGraphs);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        String query = null;
        String[] defaultGraphs = req.getParameterValues("default-graph-uri");
        String[] namedGraphs = req.getParameterValues("named-graph-uri");

        if(req.getContentType().matches("application/x-www-form-urlencoded.*"))
            query = req.getParameter("query");
        else if(req.getContentType().matches("application/sparql-query.*"))
            query = IOUtils.toString(req.getInputStream());

        process(req, res, query, defaultGraphs, namedGraphs);
    }


    public void process(HttpServletRequest req, HttpServletResponse res, String query, String[] defaultGraphs,
            String[] namedGraphs) throws IOException
    {
        /* TODO: take defaultGraphs and namedGraphs into the account */

        res.setHeader("access-control-allow-headers",
                "x-requested-with, Content-Type, origin, authorization, accept, client-security-token");
        res.setHeader("access-control-allow-origin", "*");


        // IOCB SPARQL protocol extension
        String warnings = req.getParameter("warnings");
        String filename = req.getParameter("filename");

        if(filename != null)
            res.setHeader("content-disposition", "attachment; filename=\"" + filename + "\"");


        try
        {
            SelectQuery syntaxTree = parser.parse(query);
            String code = new TranslateVisitor(dbConfig).translate(syntaxTree);

            Result result = db.query(code);
            boolean includeWarnings = warnings != null ? Boolean.parseBoolean(warnings) : false;


            switch(detectOutputType(req))
            {
                case JSON:
                    writeJson(res, result, includeWarnings);
                    break;
                case XML:
                    writeXml(res, result, includeWarnings);
                    break;
                case TSV:
                    writeTsv(res, result);
                    break;
                case CSV:
                    writeCsv(res, result);
                    break;
                default:
                    res.setStatus(406);
            }
        }
        catch(TranslateExceptions | DatabaseException | ParseExceptions e)
        {
            throw new IOException(e);
        }
    }


    private static OutputType detectOutputType(HttpServletRequest req)
    {
        // IOCB SPARQL protocol extension
        String format = req.getParameter("format");

        if(format != null)
        {
            switch(format)
            {
                case "xml":
                    return OutputType.XML;
                case "json":
                    return OutputType.JSON;
                case "tsv":
                    return OutputType.TSV;
                case "csv":
                    return OutputType.CSV;
                default:
                    return OutputType.NONE;
            }
        }


        String accepts = req.getHeader("accept");

        if(accepts == null)
            return OutputType.XML;

        boolean common = false;

        for(String value : accepts.split(" *, *"))
        {
            if(value.equals("*") || value.equals("*/*") || value.equals("application/*"))
                common = true;

            if(value.matches("application/sparql-results\\+xml;?.*"))
                return OutputType.XML;

            if(value.matches("application/sparql-results\\+json;?.*"))
                return OutputType.JSON;

            if(value.matches("text/tab-separated-values;?.*"))
                return OutputType.TSV;

            if(value.matches("text/csv;?.*"))
                return OutputType.CSV;
        }

        if(common)
            return OutputType.XML;


        return OutputType.NONE;
    }


    private static void writeXml(HttpServletResponse res, Result result, boolean includeWarnings) throws IOException
    {
        res.setHeader("content-type", "application/sparql-results+xml");
        res.setCharacterEncoding("UTF-8");

        PrintWriter out = res.getWriter();

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

        for(Row row : result)
        {
            out.println("\t\t<result>");

            for(String head : result.getHeads())
            {
                RdfNode node = row.get(head);

                if(node == null)
                    continue;


                out.print("\t\t\t<binding name=\"");
                writeXmlValue(out, head);
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
                else if(node instanceof Literal)
                {
                    out.print("<literal>");
                    writeXmlValue(out, node.getValue());
                    out.print("</literal>");
                }
                else if(node instanceof BlankNode)
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


    private static void writeJson(HttpServletResponse res, Result result, boolean includeWarnings) throws IOException
    {
        res.setHeader("content-type", "application/sparql-results+json");
        res.setCharacterEncoding("UTF-8");

        PrintWriter out = res.getWriter();

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

        for(Row row : result)
        {
            if(hasResult)
                out.println(",");
            else
                hasResult = true;

            out.println("\t{");

            boolean hasResultHead = false;

            for(String head : result.getHeads())
            {
                RdfNode node = row.get(head);

                if(node == null)
                    continue;


                if(hasResultHead)
                    out.println(',');
                else
                    hasResultHead = true;

                out.print("\t\t\"");
                writeJsonValue(out, head);
                out.println("\": {");


                out.print("\t\t\t\"type\": ");

                if(node instanceof IriNode)
                    out.println("\"uri\",");
                else if(node instanceof Literal)
                    out.println("\"literal\",");
                else
                    out.println("\"bnode\",");

                out.print("\t\t\t\"value\": ");

                out.print('"');
                writeJsonValue(out, node.getValue());
                out.print('"');

                if(node instanceof LanguageTaggedLiteral)
                {
                    out.print(",\n\t\t\t\"xml:lang\": ");

                    out.print('"');
                    writeJsonValue(out, ((LanguageTaggedLiteral) node).getLanguage());
                    out.print('"');
                }
                else if(node instanceof TypedLiteral)
                {
                    out.print(",\n\t\t\t\"datatype\": ");

                    out.print('"');
                    writeJsonValue(out, ((TypedLiteral) node).getDatatype().getValue());
                    out.print('"');
                }

                out.print("\n\t\t}");
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


    private static void writeTsv(HttpServletResponse res, Result result) throws IOException
    {
        res.setHeader("content-type", "text/tab-separated-values");
        res.setCharacterEncoding("UTF-8");

        PrintWriter out = res.getWriter();


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


        for(Row row : result)
        {
            boolean hasResult = false;

            for(String head : result.getHeads())
            {
                if(hasResult)
                    out.print('\t');
                else
                    hasResult = true;

                RdfNode node = row.get(head);

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
                else if(node instanceof Literal)
                {
                    out.print('"');
                    writeTsvLiteralValue(out, node.getValue());
                    out.print('"');
                }
                else if(node instanceof BlankNode)
                {
                    out.print("_:");
                    writeTsvValue(out, node.getValue());
                }
            }

            out.print("\r\n");
        }
    }


    private static void writeCsv(HttpServletResponse res, Result result) throws IOException
    {
        res.setHeader("content-type", "text/csv");
        res.setCharacterEncoding("UTF-8");

        PrintWriter out = res.getWriter();


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


        for(Row row : result)
        {
            boolean hasResult = false;

            for(String head : result.getHeads())
            {
                if(hasResult)
                    out.print(',');
                else
                    hasResult = true;

                RdfNode node = row.get(head);

                if(node != null)
                    writeCsvValue(out, node.getValue());
            }

            out.print("\r\n");
        }
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
