package cz.iocb.sparql.nextprot.integer;

import static cz.iocb.sparql.nextprot.integer.NeXtProtIntegerConfiguration.schema;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.NodeMapping;



public class Expression
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "isoform_low_expressions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":lowExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_medium_expressions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":mediumExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_high_expressions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":highExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }
    }
}
