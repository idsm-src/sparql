package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Endpoint
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new GeneralUserIriClass("pubchem", "endpoint",
                Arrays.asList("integer", "integer", "integer"),
                "http://rdf\\.ncbi\\.nlm\\.nih\\.gov/pubchem/endpoint/SID[1-9][0-9]*_AID[1-9][0-9]*(_(PMID([1-9][0-9]*)?|[1-9][0-9]*|0))?"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass endpoint = config.getIriClass("endpoint");
        ConstantIriMapping graph = config.createIriMapping("pubchem:endpoint");

        {
            String table = "endpoint_bases";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("obo:IAO_0000136"),
                    config.createIriMapping("substance", "substance"));
        }

        {
            String table = "endpoint_outcomes";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping("pubchem", table, graph, subject,
                    config.createIriMapping("vocab:PubChemAssayOutcome"),
                    config.createIriMapping("ontology_resource", Ontology.unitUncategorized, "outcome_id"));
        }

        {
            String table = "endpoint_measurements";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("sio:has-unit"),
                    config.createIriMapping("obo:UO_0000064"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", Ontology.unitBAO, "type_id"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(rdfLangStringEn, "label"));
        }

        {
            String table = "endpoint_measurement_values";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdFloat, "value"));
        }

        {
            String table = "endpoint_references";
            NodeMapping subject = config.createIriMapping(endpoint, "substance", "bioassay", "measuregroup");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("cito:citesAsDataSource"),
                    config.createIriMapping("reference", "reference"));
        }
    }
}
