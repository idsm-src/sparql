package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Biosystem
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("pubchem:biosystem", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/biosystem/BSID"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:biosystem");

        {
            Table table = new Table(schema, "biosystem_bases");
            NodeMapping subject = config.createIriMapping("pubchem:biosystem", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Pathway"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Biosystem.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Biosystem.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:source"),
                    config.createIriMapping("pubchem:source", "source"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitTaxonomy, "organism_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "organism_id"));
        }

        {
            Table table = new Table(schema, "biosystem_components");
            NodeMapping subject = config.createIriMapping("pubchem:biosystem", "biosystem");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:pathwayComponent"),
                    config.createIriMapping("pubchem:biosystem", "component"));
        }

        {
            Table table = new Table(schema, "biosystem_references");
            NodeMapping subject = config.createIriMapping("pubchem:biosystem", "biosystem");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }

        {
            Table table = new Table(schema, "biosystem_matches");
            NodeMapping subject = config.createIriMapping("pubchem:biosystem", "biosystem");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("identifiers:wikipathway", "wikipathway"));
        }
    }
}
