package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Biosystem
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass(schema, "biosystem", Arrays.asList("integer"),
                "http://rdf\\.ncbi\\.nlm\\.nih\\.gov/pubchem/biosystem/BSID[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "wikipathway", Arrays.asList("integer"),
                "http://identifiers\\.org/wikipathways/WP[1-9][0-9]*"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass biosystem = config.getIriClass("biosystem");
        ConstantIriMapping graph = config.createIriMapping("pubchem:biosystem");

        {
            String table = "biosystem_bases";
            NodeMapping subject = config.createIriMapping(biosystem, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Pathway"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Biosystem.vm"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:source"),
                    config.createIriMapping("source", "source"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology_resource", Ontology.unitTaxonomy, "organism_id"));
        }

        {
            String table = "biosystem_components";
            NodeMapping subject = config.createIriMapping(biosystem, "biosystem");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bp:pathwayComponent"),
                    config.createIriMapping(biosystem, "component"));
        }

        {
            String table = "biosystem_references";
            NodeMapping subject = config.createIriMapping(biosystem, "biosystem");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("reference", "reference"));
        }

        {
            String table = "biosystem_matches";
            NodeMapping subject = config.createIriMapping(biosystem, "biosystem");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("wikipathway", "wikipathway"));
        }
    }
}
