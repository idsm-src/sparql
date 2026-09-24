package cz.iocb.sparql.nextprot.string;

import static cz.iocb.sparql.nextprot.string.NeXtProtStringConfiguration.schema;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;



/**
 * Mappings of the low, medium and high expression levels of isoforms.
 */
public class Expression
{
    /**
     * Nothing to register: the mapped entities are defined elsewhere.
     */
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
    }


    /**
     * Registers the quad mappings of the expression levels.
     */
    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            DatabaseTable table = new DatabaseTable(schema, "isoform_low_expressions");
            TermMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":lowExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "isoform_medium_expressions");
            TermMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":mediumExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "isoform_high_expressions");
            TermMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":highExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }
    }
}
