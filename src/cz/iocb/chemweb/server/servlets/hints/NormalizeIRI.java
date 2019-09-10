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

        for(Entry<String, String> prefix : dbConfig.getPrefixes().entrySet())
        {
            if(iri.startsWith(prefix.getValue()))
            {
                PrefixedName result = new PrefixedName();
                result.prefix = prefix.getKey();
                result.name = iri.replaceFirst(prefix.getValue(), "");
                return result;
            }
        }

        return null;
    }
}
