package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Reference.reference;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class ConservedDomain extends PubChemMapping
{
    static IriClass conserveddomain;


    static void loadClasses()
    {
        classmap(conserveddomain = new IriClass("conserveddomain", 1,
                "http://rdf.ncbi.nlm.nih.gov/pubchem/conserveddomain/PSSMID[0-9]+"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:conserveddomain");

        {
            String table = "conserveddomain_bases";
            NodeMapping subject = iri(conserveddomain, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("obo:SO_0000417"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
            quad(table, graph, subject, iri("dcterms:abstract"), literal(xsdString, "abstract"));
        }

        {
            String table = "conserveddomain_references";
            NodeMapping subject = iri(conserveddomain, "domain");

            quad(table, graph, subject, iri("cito:isDiscussedBy"), iri(reference, "reference"));
        }
    }
}
