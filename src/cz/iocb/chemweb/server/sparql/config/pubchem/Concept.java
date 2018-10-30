package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Concept
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("concept", Arrays.asList("smallint"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/concept/.*"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass concept = config.getIriClass("concept");
        NodeMapping graph = config.createIriMapping("pubchem:concept");

        {
            config.addQuadMapping(null, graph, config.createIriMapping("concept:ATC"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("skos:ConceptScheme"));
            config.addQuadMapping(null, graph, config.createIriMapping("concept:SubstanceCategorization"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("skos:ConceptScheme"));
        }

        {
            String table = "concept_bases";
            NodeMapping subject = config.createIriMapping(concept, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:prefLabel"),
                    config.createLiteralMapping(rdfLangStringEn, "label"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:inScheme"),
                    config.createIriMapping(concept, "scheme"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:broader"),
                    config.createIriMapping(concept, "broader"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("<http://purl.org/pav/importedFrom>"),
                    config.createIriMapping("source:WHO"),
                    "iri like 'http://rdf.ncbi.nlm.nih.gov/pubchem/concept/ATC%'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("skos:Concept"),
                    "(iri <> 'http://rdf.ncbi.nlm.nih.gov/pubchem/concept/SubstanceCategorization'"
                            + " and iri <> 'http://rdf.ncbi.nlm.nih.gov/pubchem/concept/ATC')");
        }
    }
}
