package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Endpoint.endpoint;
import static cz.iocb.chemweb.server.sparql.pubchem.Gene.gene;
import static cz.iocb.chemweb.server.sparql.pubchem.Protein.protein;
import static cz.iocb.chemweb.server.sparql.pubchem.Source.source;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Measuregroup extends PubChemMapping
{
    static IriClass measuregroup;


    static void loadClasses()
    {
        classmap(measuregroup = new IriClass("measuregroup", 2,
                "http://rdf.ncbi.nlm.nih.gov/pubchem/measuregroup/AID[0-9]+(_(PMID[0-9]*|[0-9]+))?"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:measuregroup");

        {
            String table = "measuregroup_bases";
            NodeMapping subject = iri(measuregroup, "bioassay", "measuregroup");

            quad(table, graph, subject, iri("rdf:type"), iri("bao:BAO_0000040"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
            quad(table, graph, subject, iri("dcterms:source"), iri(source, "source"), "source is not null");
        }

        {
            String table = "endpoint_bases";
            NodeMapping subject = iri(measuregroup, "bioassay", "measuregroup");

            quad(table, graph, subject, iri("obo:OBI_0000299"), iri(endpoint, "substance", "bioassay", "measuregroup"));
        }

        {
            String table = "measuregroup_genes";
            NodeMapping subject = iri(measuregroup, "bioassay", "measuregroup");

            quad(table, graph, subject, iri("obo:BFO_0000057"), iri(gene, "gene"));
        }

        {
            String table = "measuregroup_proteins";
            NodeMapping subject = iri(measuregroup, "bioassay", "measuregroup");

            quad(table, graph, subject, iri("obo:BFO_0000057"), iri(protein, "protein"));
        }
    }
}
