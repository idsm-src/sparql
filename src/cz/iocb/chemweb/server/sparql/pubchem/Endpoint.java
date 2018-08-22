package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Endpoint
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("endpoint", Arrays.asList("integer", "integer", "integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/endpoint/SID[0-9]+_AID[0-9]+(_(PMID[0-9]*|[0-9]+))?"));
        config.addIriClass(new UserIriClass("outcome", Arrays.asList("smallint"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/vocabulary#(active|inactive|inconclusive|unspecified|probe)"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass endpoint = config.getIriClass("endpoint");
        NodeMapping graph = config.createIriMapping("pubchem:endpoint");

        {
            String table = "endpoint_bases";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:IAO_0000136"),
                    config.createIriMapping("substance", "substance"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:PubChemAssayOutcome"),
                    config.createIriMapping("outcome", "outcome"));
        }

        {
            String table = "endpoint_measurements";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-unit"),
                    config.createIriMapping("obo:UO_0000064"));
            /* TODO:
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("class", "class"));
            */
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(rdfLangStringEn, "label"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdFloat, "value"));
        }

        {
            String table = "endpoint_references";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:citesAsDataSource"),
                    config.createIriMapping("reference", "reference"));
        }

        {
            String table = "endpoint_outcomes__reftable";
            NodeMapping subject = config.createIriMapping("outcome", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("vocab:PubChemBioAssayOutcomeCategory"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/PubChemBioAssayOutcomeCategory.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/PubChemBioAssayOutcomeCategory.vm"));
        }
    }
}
