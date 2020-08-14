package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.xsdDateM4;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Reference
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(
                new IntegerUserIriClass("reference", "integer", "http://rdf.ncbi.nlm.nih.gov/pubchem/reference/PMID"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass reference = config.getIriClass("reference");
        ConstantIriMapping graph = config.createIriMapping("pubchem:reference");

        {
            String table = "reference_bases";
            NodeMapping subject = config.createIriMapping(reference, "id");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", Ontology.unitUncategorized, "type_id"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Reference.vm"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Reference.vm"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("dcterms:date"),
                    config.createLiteralMapping(xsdDateM4, "dcdate"));
            config.addQuadMapping("pubchem", table, graph, subject,
                    config.createIriMapping("dcterms:bibliographicCitation"),
                    config.createLiteralMapping(rdfLangStringEn, "citation"));
        }

        {
            String table = "reference_discusses";
            NodeMapping subject = config.createIriMapping(reference, "reference");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("cito:discusses"),
                    config.createIriMapping("mesh", "statement"));
        }

        {
            String table = "reference_subjects";
            NodeMapping subject = config.createIriMapping(reference, "reference");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("fabio:hasSubjectTerm"),
                    config.createIriMapping("mesh", "subject"));
        }

        {
            String table = "reference_primary_subjects";
            NodeMapping subject = config.createIriMapping(reference, "reference");

            config.addQuadMapping("pubchem", table, graph, subject,
                    config.createIriMapping("fabio:hasPrimarySubjectTerm"), config.createIriMapping("mesh", "subject"));
        }
    }
}
