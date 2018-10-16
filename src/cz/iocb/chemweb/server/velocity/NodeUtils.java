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
        catch(Throwable e)
        {
            System.err.println("#" + node.getValue() + "#"); // FIXME: log error

            return "NODE_";
        }
    }
}
