package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Synonym
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new IriClass("synonym", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/synonym/MD5_.*"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        IriClass synonym = config.getIriClass("synonym");
        NodeMapping graph = config.createIriMapping("pubchem:synonym");

        {
            String table = "synonym_values";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdString, "value"));
        }

        {
            /* TODO:
            String table = "synonym_types";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("class", "type"));
            */
        }

        {
            String table = "synonym_compounds";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping("compound", "compound"));
        }

        {
            String table = "synonym_mesh_subjects";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("mesh", "subject"));
        }

        {
            String table = "synonym_concept_subjects";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("concept", "concept"));
        }
    }
}
