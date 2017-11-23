package cz.iocb.chemweb.server.velocity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringEscapeUtils;
import cz.iocb.chemweb.server.db.Literal;
import cz.iocb.chemweb.server.db.ReferenceNode;
import cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.shared.utils.Encode;



public class NodeUtils
{
    public static String escapeHtml(Literal node)
    {
        if(node == null)
            return null;

        return StringEscapeUtils.escapeHtml4(node.getValue());
    }


    public static String prefixedIRI(ReferenceNode node) throws FileNotFoundException, IOException, SQLException
    {
        SparqlDatabaseConfiguration dbConfig = PubChemConfiguration.get();

        String result = node.getValue();

        for(Entry<String, String> prefix : dbConfig.getPrefixes().entrySet())
            if(result.startsWith(prefix.getValue()))
                return result.replaceFirst(prefix.getValue(), prefix.getKey() + ":");

        return result;
    }


    public static String nodeId(ReferenceNode node)
    {
        try
        {
            return "NODE_" + Encode.base32m(node.getValue());
        }
        catch (Throwable e)
        {
            System.err.println("#" + node.getValue() + "#"); // FIXME: log error

            return "NODE_";
        }
    }


    public static String chebiXmlToHtml(Literal node)
    {
        String string = node.getValue();

        // represented by unicode
        string = string.replace("<ampersand/>", "&amp;");
        string = string.replace("<minus/>", "&minus;");
        string = string.replace("<radical_dot/>", "•");
        string = string.replace("<parenthesis>open</parenthesis>", "(");
        string = string.replace("<parenthesis>close</parenthesis>", ")");
        string = string.replace("<bracket>open</bracket>", "[");
        string = string.replace("<bracket>close</bracket>", "]");
        string = string.replace("<arrow>left</arrow>", "←");
        string = string.replace("<arrow>right</arrow>", "→");
        string = string.replace("<arrow>leftright</arrow>", "↔");
        string = string.replace("<bond>1</bond>", "‒");
        string = string.replace("<bond>2</bond>", "=");
        string = string.replace("<bond>3</bond>", "≡");
        string = string.replace("<bond>\\(</bond>", "((̶");
        string = string.replace("<bond>\\)</bond>", "))̶");
        string = string.replace("<apostrophe/>", "ʼ");
        string = string.replace("<greaterthan/>", ">");
        string = string.replace("<lessthan/>", "<");
        string = string.replace("<trademark/>", "&trade;");
        string = string.replace("<em_dash/>", "—");
        string = string.replace("<middle_dot/>", "·");
        string = string.replace("<plusmn/>", "±");
        string = string.replace("<degree/>", "&deg;");
        string = string.replace("<shy/>", "&shy;");
        string = string.replace("<->", "↔");

        string = string.replaceAll("<acute>(.)</acute>", "&$1acute;");
        string = string.replaceAll("<circ>(.)</circ>", "&$1circ;");
        string = string.replaceAll("<grave>(.)</grave>", "&$1grave;");
        string = string.replaceAll("<umlaut>(.)</umlaut>", "&$1uml;");
        string = string.replaceAll("<tilde>(.)</tilde>", "&$1tilde;");
        string = string.replace("<sharp>s</sharp>", "ß");
        string = string.replace("<slash>o</slash>", "ø");
        string = string.replaceAll("<ligature>(..)</ligature>", "&$1lig;");
        string = string.replaceAll("<greek>([^<]*?)</greek>", "&$1;");
        string = string.replaceAll("<locant>([^<]*?)</locant>", "&$1;");
        string = string.replaceAll("<protein>([^<]*?)</protein>", "[$1]");

        // represented by HTML: <sub>, <sup>, <small>, <b>, <i>
        string = string.replace("<oxs>0</oxs>", "<small>0</small>");
        string = string.replace("<oxs>1</oxs>", "<small>I</small>");
        string = string.replace("<oxs>2</oxs>", "<small>II</small>");
        string = string.replace("<oxs>3</oxs>", "<small>III</small>");
        string = string.replace("<oxs>4</oxs>", "<small>IV</small>");
        string = string.replace("<oxs>5</oxs>", "<small>V</small>");
        string = string.replace("<oxs>6</oxs>", "<small>VI</small>");
        string = string.replace("<oxs>7</oxs>", "<small>ⱽᴵᴵ</small>");
        string = string.replace("<oxs>8</oxs>", "<small>VIII</small>");

        string = string.replace("<smallsub>", "<small><sub>").replace("</smallsub>", "</sub></small>");
        string = string.replace("<smallsup>", "<small><sup>").replace("</smallsup>", "</sup></small>");
        string = string.replaceAll("(<|</)bold>", "$1b>");

        string = string.replaceAll("(<|</)ringsugar>", "$1i>");
        string = string.replaceAll("(<|</)ital>", "$1i>");
        string = string.replaceAll("(<|</)element>", "$1i>");
        string = string.replaceAll("(<|</)stereo>", "$1i>");
        string = string.replaceAll("(<|</)stereoref>", "$1i>");

        // fix ChEBI errors
        string = string.replace("&MINUS;", "&minus;");

        string = StringEscapeUtils.unescapeHtml4(string);

        //FIXME: special, not supported by unescapeHtml
        string = string.replace("&comma;", ",");
        string = string.replace("&sol;", "/");
        string = string.replace("&bgr;", "β");
        string = string.replace("&Agr;", "Α");

        string = string.replace("&", "&amp;");
        string = string.replace("<", "&lt;");
        string = string.replace(">", "&gt;");
        string = string.replaceAll("&lt;(/?(sub|sup|small|b|i))&gt;", "<$1>");

        return string;
    }
}
