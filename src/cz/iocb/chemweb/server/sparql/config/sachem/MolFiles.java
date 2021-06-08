package cz.iocb.chemweb.server.sparql.config.sachem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.List;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;



public class MolFiles
{
    public static void addPrefixes(SparqlDatabaseConfiguration config)
    {
        config.addPrefix("sio", "http://semanticscience.org/resource/");
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config, String compoundClass, String molfileClass,
            Table table, List<Column> compoundFields)
    {
        NodeMapping subject = config.createIriMapping(molfileClass, "id");

        config.addQuadMapping(table, null, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("sio:SIO_011120"));
        config.addQuadMapping(table, null, subject, config.createIriMapping("sio:is-attribute-of"),
                config.createIriMapping(compoundClass, compoundFields));
        config.addQuadMapping(table, null, subject, config.createIriMapping("sio:has-value"),
                config.createLiteralMapping(xsdString, "molfile"));
    }
}
