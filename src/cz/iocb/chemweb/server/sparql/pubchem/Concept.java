package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Concept extends PubChemMapping
{
    static IriClass concept;


    static void loadClasses()
    {
        classmap(concept = new IriClass("concept", Arrays.asList("smallint"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/concept/.*"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:concept");

        {
            quad(null, graph, iri("concept:ATC"), iri("rdf:type"), iri("skos:ConceptScheme"));
            quad(null, graph, iri("concept:SubstanceCategorization"), iri("rdf:type"), iri("skos:ConceptScheme"));
        }

        {
            String table = "concept_bases";
            NodeMapping subject = iri(concept, "id");

            quad(table, graph, subject, iri("skos:prefLabel"), literal(xsdString, "label"));
            quad(table, graph, subject, iri("skos:inScheme"), iri(concept, "scheme"));
            quad(table, graph, subject, iri("skos:broader"), iri(concept, "broader"));

            quad(table, graph, subject, iri("<http://purl.org/pav/importedFrom>"), iri("source:WHO"),
                    "iri like 'http://rdf.ncbi.nlm.nih.gov/pubchem/concept/ATC%'");
            quad(table, graph, subject, iri("rdf:type"), iri("skos:Concept"),
                    "(iri <> 'http://rdf.ncbi.nlm.nih.gov/pubchem/concept/SubstanceCategorization'"
                            + " and iri <> 'http://rdf.ncbi.nlm.nih.gov/pubchem/concept/ATC')");
        }
    }
}
