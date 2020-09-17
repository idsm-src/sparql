package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Gene
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(
                new IntegerUserIriClass("pubchem:gene", "integer", "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/GID"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:gene");

        {
            Table table = new Table(schema, "gene_bases");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Gene"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:description"),
                    config.createLiteralMapping(rdfLangStringEn, "description"));
        }

        {
            Table table = new Table(schema, "gene_biosystems");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping("pubchem:biosystem", "biosystem"));
        }

        {
            Table table = new Table(schema, "gene_alternatives");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(rdfLangStringEn, "alternative"));
        }

        {
            Table table = new Table(schema, "gene_references");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }

        {
            Table table = new Table(schema, "gene_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("rdf:ensembl", "match"));
        }
    }
}
