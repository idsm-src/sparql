package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Biosystem.biosystem;
import static cz.iocb.chemweb.server.sparql.pubchem.Reference.reference;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Gene extends PubChemMapping
{
    static IriClass gene;
    static IriClass ensembl;


    static void loadClasses()
    {
        classmap(gene = new IriClass("gene", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/GID[0-9]+"));
        classmap(ensembl = new IriClass("ensembl", Arrays.asList("varchar"),
                "http://rdf.ebi.ac.uk/resource/ensembl/.*"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:gene");

        {
            String table = "gene_bases";
            NodeMapping subject = iri(gene, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("bp:Gene"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
            quad(table, graph, subject, iri("dcterms:description"), literal(xsdString, "description"));
        }

        {
            String table = "gene_biosystems";
            NodeMapping subject = iri(gene, "gene");

            quad(table, graph, subject, iri("obo:BFO_0000056"), iri(biosystem, "biosystem"));
        }

        {
            String table = "gene_alternatives";
            NodeMapping subject = iri(gene, "gene");

            quad(table, graph, subject, iri("dcterms:alternative"), literal(xsdString, "alternative"));
        }

        {
            String table = "gene_references";
            NodeMapping subject = iri(gene, "gene");

            quad(table, graph, subject, iri("cito:isDiscussedBy"), iri(reference, "reference"));
        }

        {
            String table = "gene_matches";
            NodeMapping subject = iri(gene, "gene");

            quad(table, graph, subject, iri("skos:closeMatch"), iri(ensembl, "match"));
        }
    }
}
