package cz.iocb.sparql.nextprot.string;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.nextprot.string.NeXtProtStringConfiguration.schema;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;



/**
 * Mappings of the controlled vocabulary terms and their hierarchy.
 */
public class Terminology
{
    /**
     * Registers the IRI class of the terms.
     */
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new StringUserIriClass("terminology", "http://nextprot.org/rdf/terminology/"));
    }


    /**
     * Registers the quad mappings of the terms.
     */
    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_bases");
            TermMapping subject = config.createIriMapping("terminology", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("schema", "type"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_parents");
            TermMapping subject = config.createIriMapping("terminology", "term");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":childOf"),
                    config.createIriMapping("terminology", "parent"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_related_terms");
            TermMapping subject = config.createIriMapping("terminology", "term");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":related"),
                    config.createIriMapping("terminology", "related"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_labels");
            TermMapping subject = config.createIriMapping("terminology", "term");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));
        }
    }
}
