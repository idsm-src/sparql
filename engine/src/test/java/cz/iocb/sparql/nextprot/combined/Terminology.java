package cz.iocb.sparql.nextprot.combined;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.nextprot.combined.NeXtProtCombinedConfiguration.schema;
import static java.util.Arrays.asList;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
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
        DatabaseTable baseTable = new DatabaseTable(schema, "terminology_bases");

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_bases");
            TermMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(table, new DatabaseTable(schema, "schema_bases")),
                    asList(new JoinColumns(new TableColumn("type"), new TableColumn("id"), "int4")), graph, subject,
                    config.createIriMapping("rdf:type"), config.createIriMapping("schema", "iri"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_parents");
            TermMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(baseTable, table, baseTable),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("term"), "int4"),
                            new JoinColumns(new TableColumn("parent"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":childOf"), config.createIriMapping("terminology", "iri"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_related_terms");
            TermMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(baseTable, table, baseTable),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("term"), "int4"),
                            new JoinColumns(new TableColumn("related"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":related"), config.createIriMapping("terminology", "iri"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "terminology_labels");
            TermMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(baseTable, table),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("term"), "int4")), graph, subject,
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString, "label"));
        }
    }
}
