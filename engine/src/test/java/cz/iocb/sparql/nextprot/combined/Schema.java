package cz.iocb.sparql.nextprot.combined;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDateType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntegerType;
import static cz.iocb.sparql.nextprot.combined.NeXtProtCombinedConfiguration.schema;
import static java.util.Arrays.asList;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public class Schema
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new StringUserIriClass("source", "http://nextprot.org/rdf/source/"));
        config.addIriClass(new StringUserIriClass("database", "http://nextprot.org/rdf/db/"));
        config.addIriClass(new StringUserIriClass("schema", "http://nextprot.org/rdf#"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        addSourceQuadMapping(config);
        addDatabaseQuadMapping(config);
        addOntologyQuadMapping(config);
    }


    private static void addSourceQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "source_bases");
            NodeMapping subject = config.createIriMapping("source", "iri");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Source"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
        }
    }


    private static void addDatabaseQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");
        Table baseTable = new Table(schema, "database_bases");

        {
            Table table = new Table(schema, "database_bases");
            NodeMapping subject = config.createIriMapping("database", "iri");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Database"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
        }

        {
            Table table = new Table(schema, "database_comments");
            NodeMapping subject = config.createIriMapping("database", "iri");

            config.addQuadMapping(asList(baseTable, table),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("db"), "int4")), graph, subject,
                    config.createIriMapping("rdfs:comment"), config.createLiteralMapping(xsdString, "comment"));
        }

        {
            Table table = new Table(schema, "database_categories");
            NodeMapping subject = config.createIriMapping("database", "iri");

            config.addQuadMapping(asList(baseTable, table),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("db"), "int4")), graph, subject,
                    config.createIriMapping(":category"), config.createLiteralMapping(xsdString, "category"));
        }
    }


    private static void addOntologyQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");
        Table baseTable = new Table(schema, "schema_bases");

        {
            Table table = new Table(schema, "schema_bases");
            NodeMapping subject = config.createIriMapping("schema", "iri");

            config.addQuadMapping(asList(table, baseTable),
                    asList(new JoinColumns(new TableColumn("type"), new TableColumn("id"), "int4")), graph, subject,
                    config.createIriMapping("rdf:type"), config.createIriMapping("schema", "iri"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
        }

        {
            Table table = new Table(schema, "schema_classes");
            NodeMapping subject = config.createIriMapping("schema", "iri");

            config.addQuadMapping(asList(baseTable, table),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("entity"), "int4")), graph, subject,
                    config.createIriMapping("rdf:type"), config.createIriMapping("owl:Class"));
        }

        {
            Table table = new Table(schema, "schema_thing_subclasses");
            NodeMapping subject = config.createIriMapping("schema", "iri");

            config.addQuadMapping(asList(baseTable, table),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("entity"), "int4")), graph, subject,
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("owl:Thing"));
        }

        {
            Table table = new Table(schema, "schema_restrictions");
            NodeMapping subject = config.createIriMapping("schema", "iri");

            config.addQuadMapping(asList(baseTable, table, baseTable),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("entity"), "int4"),
                            new JoinColumns(new TableColumn("notin"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":notIn"), config.createIriMapping("schema", "iri"));
        }

        {
            Table table = new Table(schema, "schema_related_terms");
            NodeMapping subject = config.createIriMapping("schema", "iri");

            config.addQuadMapping(asList(baseTable, table, baseTable),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("entity"), "int4"),
                            new JoinColumns(new TableColumn("related"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping(":related"), config.createIriMapping("terminology", "iri"));
        }

        {
            Table table = new Table(schema, "schema_parent_classes");
            NodeMapping subject = config.createIriMapping("schema", "iri");

            config.addQuadMapping(asList(baseTable, table, baseTable),
                    asList(new JoinColumns(new TableColumn("id"), new TableColumn("entity"), "int4"),
                            new JoinColumns(new TableColumn("parent"), new TableColumn("id"), "int4")),
                    graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("schema", "iri"));
        }

        {
            // <http://np.org/rdf>
            config.addQuadMapping(null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("owl:Ontology"));
            config.addQuadMapping(null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("owl:imports"), config.createIriMapping("owl:"));
            config.addQuadMapping(null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("owl:imports"),
                    config.createIriMapping("<http://www.w3.org/2004/02/skos/core>"));
            config.addQuadMapping(null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("owl:versionInfo"), config.createLiteralMapping("Initial release"));

            // :Version
            config.addQuadMapping(null, graph, config.createIriMapping(":Version"),
                    config.createIriMapping(":ttlGenerationDate"),
                    config.createLiteralMapping(xsdCompositeDate, new Literal("2021-12-12", xsdDateType)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Version"),
                    config.createIriMapping(":databaseRelease"), config.createLiteralMapping("2021-11-19"));
            config.addQuadMapping(null, graph, config.createIriMapping(":Version"),
                    config.createIriMapping(":apiRelease"),
                    config.createLiteralMapping("2.31.0 (build 6462#4abd80b [branch develop])"));
            config.addQuadMapping(null, graph, config.createIriMapping(":Version"),
                    config.createIriMapping(":genomeAssembly:"), config.createLiteralMapping("GRCh38"));

            // :assignedBy
            config.addQuadMapping(null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("rdf:Property"));
            config.addQuadMapping(null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("owl:FunctionalProperty"));
            config.addQuadMapping(null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdfs:domain"), config.createIriMapping(":Evidence"));
            config.addQuadMapping(null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdfs:range"), config.createIriMapping(":Source"));

            // :childOf
            config.addQuadMapping(null, graph, config.createIriMapping(":childOf"), config.createIriMapping("rdf:type"),
                    config.createIriMapping("rdf:Property"));
            config.addQuadMapping(null, graph, config.createIriMapping(":childOf"), config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:TransitiveProperty"));
            config.addQuadMapping(null, graph, config.createIriMapping(":childOf"),
                    config.createIriMapping("rdfs:domain"), config.createIriMapping(":Term"));
            config.addQuadMapping(null, graph, config.createIriMapping(":childOf"),
                    config.createIriMapping("rdfs:range"), config.createIriMapping(":Term"));

            // :related
            config.addQuadMapping(null, graph, config.createIriMapping(":related"), config.createIriMapping("rdf:type"),
                    config.createIriMapping("rdf:Property"));
            config.addQuadMapping(null, graph, config.createIriMapping(":related"),
                    config.createIriMapping("rdfs:domain"), config.createIriMapping(":Term"));
            config.addQuadMapping(null, graph, config.createIriMapping(":related"),
                    config.createIriMapping("rdfs:range"), config.createIriMapping(":Term"));

            // owl:equivalentClass / owl:sameAs
            config.addQuadMapping(null, graph, config.createIriMapping(":Entry"),
                    config.createIriMapping("owl:equivalentClass"), config.createIriMapping("up:Protein"));
            config.addQuadMapping(null, graph, config.createIriMapping(":Term"),
                    config.createIriMapping("owl:equivalentClass"), config.createIriMapping("up:Concept"));
            config.addQuadMapping(null, graph, config.createIriMapping(":NotDetected"),
                    config.createIriMapping("owl:sameAs"), config.createIriMapping(":Negative"));

            // rdfs:subClassOf
            config.addQuadMapping(null, graph, config.createIriMapping("cv:ECO_0000045"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping(":IHC"));
            config.addQuadMapping(null, graph, config.createIriMapping("cv:ECO_0000104"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping(":EST"));
            config.addQuadMapping(null, graph, config.createIriMapping("cv:ECO_0000220"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping(":Microarray"));
            config.addQuadMapping(null, graph, config.createIriMapping("cv:ECO_0000045"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("cv:ECO_0000045"));
            config.addQuadMapping(null, graph, config.createIriMapping("cv:ECO_0000220"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("cv:ECO_0000220"));
            config.addQuadMapping(null, graph, config.createIriMapping("cv:ECO_0000104"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("cv:ECO_0000104"));
            config.addQuadMapping(null, graph, config.createIriMapping("owl:Thing"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("owl:Thing"));

            // :level
            config.addQuadMapping(null, graph, config.createIriMapping(":Evidence_at_protein_level"),
                    config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new Literal("1", xsdIntegerType)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Evidence_at_transcript_level"),
                    config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new Literal("2", xsdIntegerType)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Inferred_from_homology"),
                    config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new Literal("3", xsdIntegerType)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Predicted"), config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new Literal("4", xsdIntegerType)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Uncertain"), config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new Literal("5", xsdIntegerType)));
        }
    }
}
