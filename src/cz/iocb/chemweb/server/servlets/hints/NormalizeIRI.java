package cz.iocb.chemweb.server.servlets.hints;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import cz.iocb.chemweb.server.db.Prefixes;
import cz.iocb.chemweb.server.sparql.parser.model.Prefix;



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


        try
        {
            for(Prefix prefix : Prefixes.getPrefixes())
            {
                if(iri.startsWith(prefix.getIri()))
                    return iri.replaceFirst(prefix.getIri(), prefix.getName() + ":");
            }
        }
        catch (SQLException | IOException | PropertyVetoException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return iri;
    }


    public static PrefixedName decompose(String iri)
    {
        if(iri == null)
            return null;

        try
        {
            for(Prefix prefix : Prefixes.getPrefixes())
            {
                if(iri.startsWith(prefix.getIri()))
                {
                    PrefixedName result = new PrefixedName();
                    result.prefix = prefix.getName();
                    result.name = iri.replaceFirst(prefix.getIri(), "");
                    return result;
                }
            }
        }
        catch (SQLException | IOException | PropertyVetoException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
