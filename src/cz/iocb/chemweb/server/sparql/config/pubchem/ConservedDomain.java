package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class ConservedDomain
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("conserveddomain", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/conserveddomain/PSSMID[0-9]+"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass conserveddomain = config.getIriClass("conserveddomain");
        NodeMapping graph = config.createIriMapping("pubchem:conserveddomain");

        {
            String table = "conserveddomain_bases";
            NodeMapping subject = config.createIriMapping(conserveddomain, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:SO_0000417"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/ConservedDomain.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:abstract"),
                    config.createLiteralMapping(rdfLangStringEn, "abstract"));
        }

        {
            String table = "conserveddomain_references";
            NodeMapping subject = config.createIriMapping(conserveddomain, "domain");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("reference", "reference"));
        }
    }
}
