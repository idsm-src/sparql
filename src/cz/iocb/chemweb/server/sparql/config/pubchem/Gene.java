package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Gene
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("gene", "integer", "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/GID"));
        config.addIriClass(new StringUserIriClass("ensembl", "http://rdf.ebi.ac.uk/resource/ensembl/"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass gene = config.getIriClass("gene");
        ConstantIriMapping graph = config.createIriMapping("pubchem:gene");

        {
            String table = "gene_bases";
            NodeMapping subject = config.createIriMapping(gene, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Gene"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:description"),
                    config.createLiteralMapping(rdfLangStringEn, "description"));
        }

        {
            String table = "gene_biosystems";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping("biosystem", "biosystem"));
        }

        {
            String table = "gene_alternatives";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(rdfLangStringEn, "alternative"));
        }

        {
            String table = "gene_references";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("reference", "reference"));
        }

        {
            String table = "gene_matches";
            NodeMapping subject = config.createIriMapping(gene, "gene");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("ensembl", "match"));
        }
    }
}
