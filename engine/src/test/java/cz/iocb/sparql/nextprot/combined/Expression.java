package cz.iocb.sparql.nextprot.combined;

import static cz.iocb.sparql.nextprot.combined.NeXtProtCombinedConfiguration.schema;
import static java.util.Arrays.asList;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.TermMapping;



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
            TermMapping subject = config.createIriMapping("isoform", "iri");

            config.addQuadMapping(
                    asList(new Table(schema, "isoform_bases"), table, new Table(schema, "annotation_bases")),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("isoform"), "int4"),
                            new JoinColumns(new TableColumn("annotation"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":lowExpression"),
                    config.createIriMapping("annotation", "iri"));
        }

        {
            Table table = new Table(schema, "isoform_medium_expressions");
            TermMapping subject = config.createIriMapping("isoform", "iri");

            config.addQuadMapping(
                    asList(new Table(schema, "isoform_bases"), table, new Table(schema, "annotation_bases")),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("isoform"), "int4"),
                            new JoinColumns(new TableColumn("annotation"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":mediumExpression"),
                    config.createIriMapping("annotation", "iri"));
        }

        {
            Table table = new Table(schema, "isoform_high_expressions");
            TermMapping subject = config.createIriMapping("isoform", "iri");

            config.addQuadMapping(
                    asList(new Table(schema, "isoform_bases"), table, new Table(schema, "annotation_bases")),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("isoform"), "int4"),
                            new JoinColumns(new TableColumn("annotation"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":highExpression"),
                    config.createIriMapping("annotation", "iri"));
        }
    }
}
