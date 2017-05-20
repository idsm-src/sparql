package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Reference.reference;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.taxonomy;
import static cz.iocb.chemweb.server.sparql.pubchem.Source.source;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Biosystem extends PubChemMapping
{
    static IriClass biosystem;
    static IriClass wikipathway;


    static void loadClasses()
    {
        classmap(biosystem = new IriClass("biosystem", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/biosystem/BSID[0-9]+"));
        classmap(wikipathway = new IriClass("wikipathway", Arrays.asList("integer"),
                "http://identifiers.org/wikipathways/WP[0-9]+"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:biosystem");

        {
            String table = "biosystem_bases";
            NodeMapping subject = iri(biosystem, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("bp:Pathway"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
            quad(table, graph, subject, iri("dcterms:source"), iri(source, "source"));
            quad(table, graph, subject, iri("bp:organism"), iri(taxonomy, "organism"), "organism is not null");
        }

        {
            String table = "biosystem_components";
            NodeMapping subject = iri(biosystem, "biosystem");

            quad(table, graph, subject, iri("bp:pathwayComponent"), iri(biosystem, "component"));
        }

        {
            String table = "biosystem_references";
            NodeMapping subject = iri(biosystem, "biosystem");

            quad(table, graph, subject, iri("cito:isDiscussedBy"), iri(reference, "reference"));
        }

        {
            String table = "biosystem_matches";
            NodeMapping subject = iri(biosystem, "biosystem");

            quad(table, graph, subject, iri("skos:exactMatch"), iri(wikipathway, "wikipathway"));
        }
    }
}
