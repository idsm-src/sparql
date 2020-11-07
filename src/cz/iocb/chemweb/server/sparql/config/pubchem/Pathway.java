package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Pathway
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("pubchem:pathway", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/pathway/PWID"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:pathway");

        {
            Table table = new Table(schema, "pathway_bases");
            NodeMapping subject = config.createIriMapping("pubchem:pathway", "id");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Pathway"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:source"),
                    config.createIriMapping("pubchem:source", "source"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitTaxonomy, "organism_id"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:pathbank-pathway", "reference"), "reference_type = 'PATHBANK'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:biocyc-pathway", "reference"), "reference_type = 'BIOCYC'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("identifiers:reactome", "reference"), "reference_type = 'REACTOME'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("identifiers:wikipathway", "reference"), "reference_type = 'WIKIPATHWAY'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:plantcyc-pathway", "reference"), "reference_type = 'PLANTCYC'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:pid-pathway", "reference"), "reference_type = 'PID'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:inoh-pathway", "reference"), "reference_type = 'INOH'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:plantreactome-pathway", "reference"),
                    "reference_type = 'PLANTREACTOME'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:pharmgkb-pathway", "reference"), "reference_type = 'PHARMGKB'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:fairdomhub-model", "reference"),
                    "reference_type = 'FAIRDOMHUB'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("reference:lipidmaps-pathway", "reference"),
                    "reference_type = 'LIPIDMAPS'");

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Pathway.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Pathway.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "organism_id"));
        }

        {
            Table table = new Table(schema, "pathway_compounds");
            NodeMapping subject = config.createIriMapping("pubchem:pathway", "pathway");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0000057"),
                    config.createIriMapping("pubchem:compound", "compound"));
        }

        {
            Table table = new Table(schema, "pathway_proteins");
            NodeMapping subject = config.createIriMapping("pubchem:pathway", "pathway");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0000057"),
                    config.createIriMapping("pubchem:protein", "protein"));
        }

        {
            Table table = new Table(schema, "pathway_genes");
            NodeMapping subject = config.createIriMapping("pubchem:pathway", "pathway");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0000057"),
                    config.createIriMapping("pubchem:gene", "gene"));
        }

        {
            Table table = new Table(schema, "pathway_components");
            NodeMapping subject = config.createIriMapping("pubchem:pathway", "pathway");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:pathwayComponent"),
                    config.createIriMapping("pubchem:pathway", "component"));
        }

        {
            Table table = new Table(schema, "pathway_references");
            NodeMapping subject = config.createIriMapping("pubchem:pathway", "pathway");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }

        {
            Table table = new Table(schema, "pathway_related_pathways");
            NodeMapping subject = config.createIriMapping("pubchem:pathway", "pathway");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:related"),
                    config.createIriMapping("pubchem:pathway", "related"));
        }
    }
}
