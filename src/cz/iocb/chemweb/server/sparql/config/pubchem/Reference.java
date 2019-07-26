package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.xsdDateM4;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Reference
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("reference", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/reference/PMID([1-9][0-9]*|0)"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass reference = config.getIriClass("reference");
        ConstantIriMapping graph = config.createIriMapping("pubchem:reference");

        {
            String table = "reference_bases";
            NodeMapping subject = config.createIriMapping(reference, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", Ontology.unitUncategorized, "type_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Reference.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:date"),
                    config.createLiteralMapping(xsdDateM4, "dcdate"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:bibliographicCitation"),
                    config.createLiteralMapping(rdfLangStringEn, "citation"));
        }

        {
            String table = "reference_discusses";
            NodeMapping subject = config.createIriMapping(reference, "reference");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:discusses"),
                    config.createIriMapping("mesh", "statement"));
        }

        {
            String table = "reference_subject_descriptors";
            NodeMapping subject = config.createIriMapping(reference, "reference");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("fabio:hasSubjectTerm"),
                    config.createIriMapping("dqmesh", "descriptor", "qualifier"));
        }
    }
}
