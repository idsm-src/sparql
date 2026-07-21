package cz.iocb.sparql.nextprot.combined;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.nextprot.combined.NeXtProtCombinedConfiguration.schema;
import static java.util.Arrays.asList;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;



public class Terminology
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new StringUserIriClass("terminology", "http://nextprot.org/rdf/terminology/"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");
        Table baseTable = new Table(schema, "terminology_bases");

        {
            Table table = new Table(schema, "terminology_bases");
            NodeMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(table, new Table(schema, "schema_bases")),
                    asList(new JoinColumns(new TableColumn("type"), new TableColumn("id"), "int4")), graph, subject,
                    config.createIriMapping("rdf:type"), config.createIriMapping("schema", "iri"));
        }

        {
            Table table = new Table(schema, "terminology_parents");
            NodeMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(baseTable, table, baseTable),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("term"), "int4"),
                            new JoinColumns(new TableColumn("parent"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":childOf"), config.createIriMapping("terminology", "iri"));
        }

        {
            Table table = new Table(schema, "terminology_related_terms");
            NodeMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(baseTable, table, baseTable),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("term"), "int4"),
                            new JoinColumns(new TableColumn("related"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":related"), config.createIriMapping("terminology", "iri"));
        }

        {
            Table table = new Table(schema, "terminology_labels");
            NodeMapping subject = config.createIriMapping("terminology", "iri");

            config.addQuadMapping(asList(baseTable, table),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("term"), "int4")), graph, subject,
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString, "label"));
        }
    }
}
