package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class ConservedDomain
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("conserveddomain", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/conserveddomain/PSSMID"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass conserveddomain = config.getIriClass("conserveddomain");
        ConstantIriMapping graph = config.createIriMapping("pubchem:conserveddomain");

        {
            String table = "conserveddomain_bases";
            NodeMapping subject = config.createIriMapping(conserveddomain, "id");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:SO_0000417"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/ConservedDomain.vm"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/ConservedDomain.vm"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("dcterms:abstract"),
                    config.createLiteralMapping(rdfLangStringEn, "abstract"));
        }

        {
            String table = "conserveddomain_references";
            NodeMapping subject = config.createIriMapping(conserveddomain, "domain");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("reference", "reference"));
        }
    }
}
