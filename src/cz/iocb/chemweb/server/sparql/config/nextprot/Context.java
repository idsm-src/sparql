package cz.iocb.chemweb.server.sparql.config.nextprot;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Context
{
    static void addIriClasses(NeXtProtConfiguration config)
    {
        config.addIriClass(
                new UserIriClass("context", Arrays.asList("integer"), "http://nextprot.org/rdf/context/[1-9][0-9]*"));
    }


    static void addQuadMapping(NeXtProtConfiguration config)
    {
        UserIriClass context = config.getIriClass("context");
        NodeMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            String table = "context_bases";
            NodeMapping subject = config.createIriMapping(context, "id");

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
