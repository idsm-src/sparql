package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class ConservedDomain
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("pubchem:conserveddomain", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/conserveddomain/PSSMID"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:conserveddomain");

        {
            Table table = new Table(schema, "conserveddomain_bases");
            NodeMapping subject = config.createIriMapping("pubchem:conserveddomain", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:SO_0000417"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:abstract"),
                    config.createLiteralMapping(rdfLangStringEn, "abstract"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/ConservedDomain.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/ConservedDomain.vm"));
        }

        {
            Table table = new Table(schema, "conserveddomain_references");
            NodeMapping subject = config.createIriMapping("pubchem:conserveddomain", "domain");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }
    }
}
