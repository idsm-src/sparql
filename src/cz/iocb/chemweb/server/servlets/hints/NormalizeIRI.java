package cz.iocb.chemweb.server.servlets.hints;

import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;



public class NormalizeIRI
{
    public static class PrefixedName
    {
        String prefix;
        String name;
    }


    private static String PN_CHARS_BASE = "([A-Za-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-"
            + "\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|"
            + "[\\uD840-\\uDBBF][\\uDC00–\\uDFFF])";
    private static String PN_CHARS_U = "(" + PN_CHARS_BASE + "|_)";
    private static String PN_CHARS = "(" + PN_CHARS_U + "|[-0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040])";
    private static String PERCENT = "(%[0-9A-Fa-f][0-9A-Fa-f])";
    private static String PN_LOCAL_ESC = "(\\\\[-_~.!$&'()*+,;=/?#@%])";
    private static String PLX = "(" + PERCENT + "|" + PN_LOCAL_ESC + ")";
    private static String PN_LOCAL = "((" + PN_CHARS_U + "|[0-9:]|" + PLX + ")((" + PN_CHARS + "|[.:]|" + PLX + ")*("
            + PN_CHARS + "|:|" + PLX + "))?)?";


    public static String normalize(SparqlDatabaseConfiguration dbConfig, String iri)
    {
        if(iri == null)
            return null;

        String prefixedIRI = IRI.toPrefixedIRI(iri, dbConfig.getPrefixes());
        return prefixedIRI != null ? prefixedIRI : iri;
    }


    public static PrefixedName decompose(SparqlDatabaseConfiguration dbConfig, String iri)
    {
        if(iri == null)
            return null;


        PrefixedName result = null;
        int size = iri.length();

        for(Entry<String, String> prefix : dbConfig.getPrefixes().entrySet())
        {
            if(iri.startsWith(prefix.getValue()))
            {
                String name = iri.substring(prefix.getValue().length());

                if(name.length() < size && name.matches(PN_LOCAL))
                {
                    size = name.length();

                    result = new PrefixedName();
                    result.prefix = prefix.getKey();
                    result.name = iri.replaceFirst(prefix.getValue(), "");
                }
            }
        }

        return result;
    }
}
