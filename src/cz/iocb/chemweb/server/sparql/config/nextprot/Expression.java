package cz.iocb.chemweb.server.sparql.config.nextprot;

import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Expression
{
    static void addIriClasses(NeXtProtConfiguration config)
    {
    }


    static void addQuadMapping(NeXtProtConfiguration config)
    {
        UserIriClass isoform = config.getIriClass("isoform");
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            String table = "isoform_low_expressions";
            NodeMapping subject = config.createIriMapping(isoform, "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":lowExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            String table = "isoform_medium_expressions";
            NodeMapping subject = config.createIriMapping(isoform, "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":mediumExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            String table = "isoform_high_expressions";
            NodeMapping subject = config.createIriMapping(isoform, "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":highExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }
    }
}
