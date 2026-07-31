package cz.iocb.sparql.nextprot.string;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.nextprot.string.NeXtProtStringConfiguration.schema;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.rdf.TypedLiteral;



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
            TermMapping subject = config.createIriMapping("source", "id");

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

        {
            Table table = new Table(schema, "database_bases");
            TermMapping subject = config.createIriMapping("database", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Database"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
        }

        {
            Table table = new Table(schema, "database_comments");
            TermMapping subject = config.createIriMapping("database", "db");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
        }

        {
            Table table = new Table(schema, "database_categories");
            TermMapping subject = config.createIriMapping("database", "db");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":category"),
                    config.createLiteralMapping(xsdString, "category"));
        }
    }


    private static void addOntologyQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "schema_bases");
            TermMapping subject = config.createIriMapping("schema", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("schema", "type"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
        }

        {
            Table table = new Table(schema, "schema_classes");
            TermMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Class"));
        }

        {
            Table table = new Table(schema, "schema_thing_subclasses");
            TermMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("owl:Thing"));
        }

        {
            Table table = new Table(schema, "schema_restrictions");
            TermMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":notIn"),
                    config.createIriMapping("schema", "notin"));
        }

        {
            Table table = new Table(schema, "schema_related_terms");
            TermMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":related"),
                    config.createIriMapping("terminology", "related"));
        }

        {
            Table table = new Table(schema, "schema_parent_classes");
            TermMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("schema", "parent"));
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
                    config.createLiteralMapping(xsdCompositeDate, new TypedLiteral("2021-12-12", xsdDateIri)));
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
                    config.createLiteralMapping(xsdInteger, new TypedLiteral("1", xsdIntegerIri)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Evidence_at_transcript_level"),
                    config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new TypedLiteral("2", xsdIntegerIri)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Inferred_from_homology"),
                    config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new TypedLiteral("3", xsdIntegerIri)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Predicted"), config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new TypedLiteral("4", xsdIntegerIri)));
            config.addQuadMapping(null, graph, config.createIriMapping(":Uncertain"), config.createIriMapping(":level"),
                    config.createLiteralMapping(xsdInteger, new TypedLiteral("5", xsdIntegerIri)));
        }
    }
}
