package cz.iocb.sparql.nextprot.integer;

import static cz.iocb.sparql.nextprot.integer.NeXtProtIntegerConfiguration.schema;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;



public class Context
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("context", "int4", "http://nextprot.org/rdf/context/"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "context_bases");
            NodeMapping subject = config.createIriMapping("context", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":ExperimentalContext"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":metadata"),
                    config.createIriMapping("publication", "metadata"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":detectionMethod"),
                    config.createIriMapping("terminology", "method"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":disease"),
                    config.createIriMapping("terminology", "disease"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":tissue"),
                    config.createIriMapping("terminology", "tissue"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cellLine"),
                    config.createIriMapping("terminology", "line"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":developmentalStage"),
                    config.createIriMapping("terminology", "stage"));
        }
    }
}
