package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass.SqlCheck;



class Gene
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("gene", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/GID[1-9][0-9]*"));
        config.addIriClass(new UserIriClass("ensembl", Arrays.asList("varchar"),
                "http://rdf.ebi.ac.uk/resource/ensembl/.*", SqlCheck.IF_MATCH));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass gene = config.getIriClass("gene");
        ConstantIriMapping graph = config.createIriMapping("pubchem:gene");

        {
            String table = "gene_bases";
            NodeMapping subject = config.createIriMapping(gene, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Gene"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:description"),
                    config.createLiteralMapping(rdfLangStringEn, "description"));
        }

        {
            String table = "gene_biosystems";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping("biosystem", "biosystem"));
        }

        {
            String table = "gene_alternatives";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(rdfLangStringEn, "alternative"));
        }

        {
            String table = "gene_references";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("reference", "reference"));
        }

        {
            String table = "gene_matches";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("ensembl", "match"));
        }
    }
}
