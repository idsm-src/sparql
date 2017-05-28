package cz.iocb.chemweb.server.servlets.hints;

import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.pubchem.PubChemMapping;



public class NormalizeIRI
{
    public static class PrefixedName
    {
        String prefix;
        String name;
    }


    public static String normalize(String iri)
    {
        if(iri == null)
            return null;

        for(Entry<String, String> prefix : PubChemMapping.getPrefixes().entrySet())
            if(iri.startsWith(prefix.getValue()))
                return iri.replaceFirst(prefix.getValue(), prefix.getKey() + ":");

        return iri;
    }


    public static PrefixedName decompose(String iri)
    {
        if(iri == null)
            return null;

        for(Entry<String, String> prefix : PubChemMapping.getPrefixes().entrySet())
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
