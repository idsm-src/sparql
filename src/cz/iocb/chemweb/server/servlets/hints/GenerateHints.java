package cz.iocb.chemweb.server.servlets.hints;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.VirtuosoDatabase;
import cz.iocb.chemweb.server.servlets.hints.NormalizeIRI.PrefixedName;
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


    public GenerateHints() throws DatabaseException
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


    private static synchronized void generateHints() throws DatabaseException
    {
        if(hintsJS != null)
            return;

        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);

        String query = "sparql                                                "
                + "define input:storage virtrdf:PubchemQuadStorage            "
                + "select ?H ?T ?R ?D ?L where                                "
                + "{                                                          "
                + "  {                                                        "
                + "    ?H rdf:type rdf:Property.                              "
                + "    bind( 'P' as ?T)                                       "
                + "                                                           "
                + "    optional                                               "
                + "    {                                                      "
                //+ "      ?H rdfs:domain ?D. ?H rdfs:range ?R.                 "
                //+ "      filter(isIRI(?D) && isIRI(?R))                       "
                //+ "      filter (!strstarts(str(?D), 'http://blanknodes/'))   "
                //+ "      filter (!strstarts(str(?R), 'http://blanknodes/'))   "
                + "    }                                                      "
                + "  }                                                        "
                + "  union                                                    "
                + "  {                                                        "
                + "    ?H rdf:type owl:Class.                                 "
                + "    bind( 'C' as ?T)                                       "
                + "  }                                                        "
                + "                                                           "
                + "  optional                                                 "
                + "  {                                                        "
                + "    ?H rdfs:label ?L.                                      "
                + "  }                                                        "
                + "                                                           "
                + "  filter(isIRI(?H))                                        "
                + "  filter (!strstarts(str(?H), 'http://blanknodes/'))       "
                + "}                                                          ";

        VirtuosoDatabase database = new VirtuosoDatabase();
        Result result = database.query(query);

        LinkedHashMap<String, ArrayList<Item>> hints = new LinkedHashMap<String, ArrayList<Item>>();

        for(Row row : result)
        {
            RdfNode text = row.get("H");
            RdfNode type = row.get("T");
            RdfNode label = row.get("L");
            //RdfNode domain = row.get("D");
            //RdfNode range = row.get("R");

            PrefixedName iri = NormalizeIRI.decompose(text.getValue());

            if(iri == null)
                continue;

            ArrayList<Item> list = hints.get(iri.prefix.toLowerCase());

            if(list == null)
            {
                list = new ArrayList<Item>();
                hints.put(iri.prefix.toLowerCase(), list);
            }


            Item item = new Item();
            item.type = type.getValue();
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
