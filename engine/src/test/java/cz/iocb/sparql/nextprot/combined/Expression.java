package cz.iocb.sparql.nextprot.combined;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.nextprot.combined.NeXtProtCombinedConfiguration.schema;
import static java.util.Arrays.asList;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
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
            TermMapping subject = config.createIriMapping("isoform", "iri");

            config.addQuadMapping(
                    asList(new DatabaseTable(schema, "isoform_bases"), table,
                            new DatabaseTable(schema, "annotation_bases")),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("isoform"), INT4),
                            new JoinColumns(new TableColumn("annotation"), new TableColumn("id"), INT4)),
                    graph, subject, config.createIriMapping(":lowExpression"),
                    config.createIriMapping("annotation", "iri"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "isoform_medium_expressions");
            TermMapping subject = config.createIriMapping("isoform", "iri");

            config.addQuadMapping(
                    asList(new DatabaseTable(schema, "isoform_bases"), table,
                            new DatabaseTable(schema, "annotation_bases")),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("isoform"), INT4),
                            new JoinColumns(new TableColumn("annotation"), new TableColumn("id"), INT4)),
                    graph, subject, config.createIriMapping(":mediumExpression"),
                    config.createIriMapping("annotation", "iri"));
        }

        {
            DatabaseTable table = new DatabaseTable(schema, "isoform_high_expressions");
            TermMapping subject = config.createIriMapping("isoform", "iri");

            config.addQuadMapping(
                    asList(new DatabaseTable(schema, "isoform_bases"), table,
                            new DatabaseTable(schema, "annotation_bases")),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("isoform"), INT4),
                            new JoinColumns(new TableColumn("annotation"), new TableColumn("id"), INT4)),
                    graph, subject, config.createIriMapping(":highExpression"),
                    config.createIriMapping("annotation", "iri"));
        }
    }
}
