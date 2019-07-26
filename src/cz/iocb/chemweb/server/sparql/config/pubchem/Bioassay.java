package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Bioassay
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/AID[1-9][0-9]*";

        config.addIriClass(new UserIriClass("bioassay", Arrays.asList("integer"), prefix));
        config.addIriClass(new UserIriClass("bioassay_data", Arrays.asList("integer", "smallint"),
                prefix + "_(Description|Protocol|Comment)"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass bioassay = config.getIriClass("bioassay");
        ConstantIriMapping graph = config.createIriMapping("pubchem:bioassay");

        {
            String table = "bioassay_bases";
            NodeMapping subject = config.createIriMapping(bioassay, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bao:BAO_0000015"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Bioassay.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Bioassay.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:source"),
                    config.createIriMapping("source", "source"));
        }

        {
            String table = "bioassay_measuregroups";
            NodeMapping subject = config.createIriMapping(bioassay, "bioassay");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("bao:BAO_0000209"),
                    config.createIriMapping("measuregroup", "bioassay", "measuregroup"));
        }

        {
            String table = "bioassay_data";
            NodeMapping subject = config.createIriMapping("bioassay_data", "bioassay", "type_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", Ontology.unitSIO, "type_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping(bioassay, "bioassay"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(rdfLangStringEn, "value"));
        }
    }
}
