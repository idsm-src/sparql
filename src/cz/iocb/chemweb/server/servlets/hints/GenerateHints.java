package cz.iocb.chemweb.server.servlets.hints;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.VirtuosoDatabase;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class GenerateHints extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static class Item
    {
        String text;
        String type;
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
                + "select ?H ?T ?R ?D where                                   "
                + "{                                                          "
                + "  {                                                        "
                + "    ?H rdf:type rdf:Property.                              "
                + "    bind( 'property' as ?T)                                "
                + "                                                           "
                + "    optional                                               "
                + "    {                                                      "
                + "      ?H rdfs:domain ?D. ?H rdfs:range ?R.                 "
                + "      filter(isIRI(?D) && isIRI(?R))                       "
                + "      filter (!strstarts(str(?D), 'http://blanknodes/'))   "
                + "      filter (!strstarts(str(?R), 'http://blanknodes/'))   "
                + "    }                                                      "
                + "  }                                                        "
                + "  union                                                    "
                + "  {                                                        "
                + "    ?H rdf:type owl:Class.                                 "
                + "    bind( 'class' as ?T)                                   "
                + "  }                                                        "
                + "                                                           "
                + "  filter(isIRI(?H))                                        "
                + "  filter (!strstarts(str(?H), 'http://blanknodes/'))       "
                + "}                                                          ";

        VirtuosoDatabase database = new VirtuosoDatabase();
        Result result = database.query(query);

        ArrayList<Item> hints = new ArrayList<Item>();

        for(Row row : result)
        {
            RdfNode text = row.get("H");
            RdfNode type = row.get("T");
            RdfNode domain = row.get("D");
            RdfNode range = row.get("R");

            Item item = new Item();
            item.text = NormalizeIRI.normalize(text.getValue());
            item.type = type.getValue();

            if(domain != null && range != null)
                item.info = NormalizeIRI.normalize(domain.getValue()) + " ➜ "
                        + NormalizeIRI.normalize(range.getValue());

            hints.add(item);
        }

        Collections.sort(hints, new Comparator<Item>()
        {
            @Override
            public int compare(Item item1, Item item2)
            {
                return item1.text.toLowerCase().compareTo(item2.text.toLowerCase());
            }
        });

        out.println("var hints = [");
        for(Item hint : hints)
        {
            out.print("  { text: \"" + hint.text + "\"");
            out.print(", type: \"" + hint.type + "\"");

            if(hint.info != null)
                out.print(", info: \"" + hint.info + "\"");

            out.println("},");
        }
        out.println("];");

        out.close();
        hintsJS = stringWriter.toString();
    }


    @Override
    public String getServletInfo()
    {
        return "Hints Servlet";
    }
}
