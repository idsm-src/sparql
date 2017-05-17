package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Source extends PubChemMapping
{
    static IriClass source;


    static void loadClasses()
    {
        classmap(source = new IriClass("source", 1, "http://rdf.ncbi.nlm.nih.gov/pubchem/source/.*"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:source");

        {
            String table = "source_bases";
            NodeMapping subject = iri(source, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("dcterms:Dataset"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
        }

        {
            String table = "source_subjects";
            NodeMapping subject = iri(source, "source");

            //TODO: quad(table, graph, subject, iri("dcterms:subject"), iri(concept, "subject"));
        }

        {
            String table = "source_alternatives";
            NodeMapping subject = iri(source, "source");

            quad(table, graph, subject, iri("dcterms:alternative"), literal(xsdString, "alternative"));
        }
    }
}
