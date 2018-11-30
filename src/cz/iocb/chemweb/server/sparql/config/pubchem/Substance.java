package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.xsdDateM4;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Substance
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("substance", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/substance/SID[0-9]+"));
        config.addIriClass(new UserIriClass("substance_chembl", Arrays.asList("integer"),
                "http://linkedchemistry.info/chembl/chemblid/S?CHEMBL[0-9]+"));
        config.addIriClass(new UserIriClass("substance_ebi_chembl", Arrays.asList("integer"),
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/S?CHEMBL[0-9]+"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass substance = config.getIriClass("substance");
        NodeMapping graph = config.createIriMapping("pubchem:substance");

        {
            String table = "substance_bases";
            NodeMapping subject = config.createIriMapping(substance, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Substance.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:available"),
                    config.createLiteralMapping(xsdDateM4, "available"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:source"),
                    config.createIriMapping("source", "source"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:modified"),
                    config.createLiteralMapping(xsdDateM4, "modified"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:CHEMINF_000477"),
                    config.createIriMapping("compound", "compound"));
        }

        {
            String table = "substance_types";
            NodeMapping subject = config.createIriMapping(substance, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", Ontology.unitCHEBI, "chebi"));
        }

        {
            String table = "endpoint_bases";
            NodeMapping subject = config.createIriMapping(substance, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping("measuregroup", "bioassay", "measuregroup"));
        }

        {
            String table = "substance_matches";
            NodeMapping subject = config.createIriMapping(substance, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("substance_chembl", "match"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("substance_ebi_chembl", "match"));
        }

        {
            String table = "substance_references";
            NodeMapping subject = config.createIriMapping(substance, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("reference", "reference"));
        }

        {
            String table = "substance_pdblinks";
            NodeMapping subject = config.createIriMapping(substance, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("pdbo:link_to_pdb"),
                    config.createIriMapping("pdblink", "pdblink"));
        }

        {
            String table = "substance_synonyms";
            NodeMapping subject = config.createIriMapping(substance, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("synonym", "synonym"));
        }

        {
            String table = "descriptor_substance_bases";
            NodeMapping subject = config.createIriMapping(substance, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("substance_version", "substance"));
        }
    }
}
