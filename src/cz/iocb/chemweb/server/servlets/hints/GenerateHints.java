package cz.iocb.chemweb.server.servlets.hints;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.postgresql.PostgreDatabase;
import cz.iocb.chemweb.server.db.postgresql.PostgresSchema;
import cz.iocb.chemweb.server.servlets.hints.NormalizeIRI.PrefixedName;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseExceptions;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.pubchem.PubChemMapping;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.translator.sql.TranslateVisitor;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class GenerateHints extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static class Item
    {
        String type;
        String name;
        String info;
    }

    private static String hintsJS;


    public GenerateHints() throws DatabaseException, FileNotFoundException, TranslateExceptions, ParseExceptions,
            SQLException, IOException
    {
        if(hintsJS == null)
            generateHints();
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        processRequest(req, res);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        processRequest(req, res);
    }


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        PrintWriter out = res.getWriter();
        out.print(hintsJS);
        out.close();
    }


    private static synchronized void generateHints() throws DatabaseException, TranslateExceptions, ParseExceptions,
            FileNotFoundException, SQLException, IOException
    {
        if(hintsJS != null)
            return;

        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);

        String query = "select ?H ?T ?L where                                 "
                + "{                                                          "
                + "  graph pubchem:ontology                                   "
                + "  {                                                        "
                + "    {                                                      "
                + "      ?H rdf:type rdf:Property.                            "
                + "    }                                                      "
                + "    union                                                  "
                + "    {                                                      "
                + "      ?H rdf:type owl:Class.                               "
                + "    }                                                      "
                + "                                                           "
                + "    ?H rdf:type ?T.                                        "
                + "                                                           "
                + "    optional                                               "
                + "    {                                                      "
                + "      ?H rdfs:label ?L.                                    "
                + "    }                                                      "
                + "  }                                                        "
                + "}                                                          ";


        Parser parser = new Parser(PubChemMapping.getProcedures(), PubChemMapping.getPrefixes());
        SelectQuery syntaxTree = parser.parse(query);
        DatabaseSchema schema = PostgresSchema.get();
        List<ResourceClass> classes = PubChemMapping.getClasses();
        List<QuadMapping> mappings = PubChemMapping.getMappings();
        LinkedHashMap<String, ProcedureDefinition> procedures = PubChemMapping.getProcedures();

        String translatedQuery = new TranslateVisitor(classes, mappings, schema, procedures).translate(syntaxTree);


        System.err.println(translatedQuery);

        PostgreDatabase database = new PostgreDatabase();
        Result result = database.query(translatedQuery);

        LinkedHashMap<String, ArrayList<Item>> hints = new LinkedHashMap<String, ArrayList<Item>>();

        for(Row row : result)
        {
            RdfNode text = row.get("H");
            RdfNode type = row.get("T");
            RdfNode label = row.get("L");

            PrefixedName iri = NormalizeIRI.decompose(text.getValue());

            if(iri == null)
                continue;

            ArrayList<Item> list = hints.get(iri.prefix.toLowerCase());

            if(list == null)
            {
                list = new ArrayList<Item>();
                hints.put(iri.prefix.toLowerCase(), list);
            }


            String typeCode = null;

            switch(type.getValue())
            {
                case "http://www.w3.org/2002/07/owl#Class":
                    typeCode = "C";
                    break;

                case "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property":
                    typeCode = "P";
                    break;

                default:
                    continue;
            }

            Item item = new Item();
            item.type = typeCode;
            item.name = iri.name;

            if(label != null)
                item.info = label.getValue().replaceAll("\"", "\\\"");

            list.add(item);
        }


        for(ArrayList<Item> list : hints.values())
            Collections.sort(list,
                    (Item item1, Item item2) -> item1.name.toLowerCase().compareTo(item2.name.toLowerCase()));


        out.println("var hints = {");

        int i = 0;
        for(Entry<String, ArrayList<Item>> entry : hints.entrySet())
        {
            if(i++ > 0)
                out.println(",");

            out.print(entry.getKey());
            out.println(": {");
            out.println("\tprefix: \"" + entry.getKey() + "\",");
            out.println("\thints: [");

            int j = 0;
            for(Item hint : entry.getValue())
            {
                if(j++ > 0)
                    out.println(",");

                out.print("\t\t{");
                out.print(" type: \"" + hint.type + "\",");
                out.print(" name: \"" + hint.name + "\"");

                if(hint.info != null)
                    out.print(", info: \"" + hint.info + "\"");

                out.print(" }");
            }

            out.print("]");
            out.print("}");
        }

        out.println("};");


        out.close();
        hintsJS = stringWriter.toString();
    }


    @Override
    public String getServletInfo()
    {
        return "Hints Servlet";
    }
}
