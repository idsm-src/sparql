package cz.iocb.chemweb.server.sparql.config.nextprot;

import static cz.iocb.chemweb.server.sparql.config.nextprot.NeXtProtConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Schema
{
    static void addIriClasses(NeXtProtConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("source", "integer", new Table(schema, "source_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/source/", 0));
        config.addIriClass(new MapUserIriClass("database", "integer", new Table(schema, "database_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/db/", 0));
        config.addIriClass(new MapUserIriClass("schema", "integer", new Table(schema, "schema_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf#", 0));
    }


    static void addQuadMapping(NeXtProtConfiguration config)
    {
        addSourceQuadMapping(config);
        addDatabaseQuadMapping(config);
        addOntologyQuadMapping(config);
    }


    private static void addSourceQuadMapping(NeXtProtConfiguration config)
    {
        UserIriClass source = config.getIriClass("source");
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            String table = "source_bases";
            NodeMapping subject = config.createIriMapping(source, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Source"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("owl:Thing"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:subClassOf"), subject);
        }
    }


    private static void addDatabaseQuadMapping(NeXtProtConfiguration config)
    {
        UserIriClass database = config.getIriClass("database");
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            String table = "database_bases";
            NodeMapping subject = config.createIriMapping(database, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Database"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("owl:Thing"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:subClassOf"), subject);
        }

        {
            String table = "database_comments";
            NodeMapping subject = config.createIriMapping(database, "database");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
        }

        {
            String table = "database_categories";
            NodeMapping subject = config.createIriMapping(database, "database");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping(":category"),
                    config.createLiteralMapping(xsdString, "category"));
        }
    }


    private static void addOntologyQuadMapping(NeXtProtConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            String table = "schema_bases";
            NodeMapping subject = config.createIriMapping("schema", "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("schema", "type"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:seeAlso"),
                    config.createLiteralMapping(xsdString, "reference"));
        }

        {
            String table = "schema_classes";
            NodeMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Class"));
        }

        {
            String table = "schema_thing_subclasses";
            NodeMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("owl:Thing"));
        }

        {
            String table = "schema_restrictions";
            NodeMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping(":notIn"),
                    config.createIriMapping("schema", "notin"));
        }

        {
            String table = "schema_related_terms";
            NodeMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping(":related"),
                    config.createIriMapping("terminology", "related"));
        }

        {
            String table = "schema_parent_classes";
            NodeMapping subject = config.createIriMapping("schema", "entity");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping("schema", "parent"));
        }

        {
            config.addQuadMapping(null, null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("rdf:Property"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("owl:FunctionalProperty"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdfs:domain"), config.createIriMapping(":Evidence"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("rdfs:range"), config.createIriMapping(":Source"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Entry"),
                    config.createIriMapping("owl:equivalentClass"), config.createIriMapping("up:Protein"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":childOf"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("rdf:Property"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":childOf"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("owl:TransitiveProperty"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":childOf"),
                    config.createIriMapping("rdfs:domain"), config.createIriMapping(":Term"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":childOf"),
                    config.createIriMapping("rdfs:range"), config.createIriMapping(":Term"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":NotDetected"),
                    config.createIriMapping("owl:sameAs"), config.createIriMapping(":Negative"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":related"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("rdf:Property"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":related"),
                    config.createIriMapping("rdfs:domain"), config.createIriMapping(":Term"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":related"),
                    config.createIriMapping("rdfs:range"), config.createIriMapping(":Term"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Term"),
                    config.createIriMapping("owl:equivalentClass"), config.createIriMapping("up:Concept"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("cv:ECO_0000045"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping(":IHC"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("cv:ECO_0000104"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping(":EST"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("cv:ECO_0000220"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping(":Microarray"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("owl:Ontology"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("owl:imports"), config.createIriMapping("owl:"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("owl:imports"),
                    config.createIriMapping("<http://www.w3.org/2004/02/skos/core>"));

            config.addQuadMapping(null, null, graph, config.createIriMapping("cv:ECO_0000045"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("cv:ECO_0000045"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("cv:ECO_0000220"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("cv:ECO_0000220"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("cv:ECO_0000104"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("cv:ECO_0000104"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("owl:Thing"),
                    config.createIriMapping("rdfs:subClassOf"), config.createIriMapping("owl:Thing"));

            config.addQuadMapping(null, null, graph, config.createIriMapping(":Version"),
                    config.createIriMapping(":git"), config.createLiteralMapping("unknow"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Version"),
                    config.createIriMapping(":version"), config.createLiteralMapping("unknow"));
            config.addQuadMapping(null, null, graph, config.createIriMapping("<http://np.org/rdf>"),
                    config.createIriMapping("owl:versionInfo"), config.createLiteralMapping("Initial release"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Evidence_at_protein_level"),
                    config.createIriMapping(":level"), config.createLiteralMapping("1"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Evidence_at_transcript_level"),
                    config.createIriMapping(":level"), config.createLiteralMapping("2"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Inferred_from_homology"),
                    config.createIriMapping(":level"), config.createLiteralMapping("3"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Predicted"),
                    config.createIriMapping(":level"), config.createLiteralMapping("4"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Uncertain"),
                    config.createIriMapping(":level"), config.createLiteralMapping("5"));
            config.addQuadMapping(null, null, graph, config.createIriMapping(":Version"),
                    config.createIriMapping(":date"), config.createLiteralMapping("Feb 2, 2019 3:04:49 AM"));
        }
    }
}
