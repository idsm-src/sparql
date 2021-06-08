package cz.iocb.chemweb.server.sparql.config.nextprot;

import static cz.iocb.chemweb.server.sparql.config.nextprot.NeXtProtConfiguration.schema;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Context
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("nextprot:context", "integer", "http://nextprot.org/rdf/context/"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "context_bases");
            NodeMapping subject = config.createIriMapping("nextprot:context", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":ExperimentalContext"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":metadata"),
                    config.createIriMapping("nextprot:publication", "metadata"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":detectionMethod"),
                    config.createIriMapping("nextprot:terminology", "method"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":disease"),
                    config.createIriMapping("nextprot:terminology", "disease"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":tissue"),
                    config.createIriMapping("nextprot:terminology", "tissue"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cellLine"),
                    config.createIriMapping("nextprot:terminology", "line"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":developmentalStage"),
                    config.createIriMapping("nextprot:terminology", "stage"));
        }
    }
}
