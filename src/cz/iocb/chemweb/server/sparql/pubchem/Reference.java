package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.dqmesh;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.mesh;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Reference extends PubChemMapping
{
    static IriClass reference;


    static void loadClasses()
    {
        classmap(reference = new IriClass("reference", 1, "http://rdf.ncbi.nlm.nih.gov/pubchem/reference/PMID[0-9]+"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:reference");

        {
            String table = "reference_bases";
            NodeMapping subject = iri(reference, "id");

            //TODO: quad(table, graph, subject, iri("rdf:type"), iri(rdfclass, "type"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
            quad(table, graph, subject, iri("dcterms:date"), literal(xsdDate, "dcdate"));
            quad(table, graph, subject, iri("dcterms:bibliographicCitation"), literal(xsdString, "citation"));
        }

        {
            String table = "reference_discusses";
            NodeMapping subject = iri(reference, "reference");

            quad(table, graph, subject, iri("cito:discusses"), iri(mesh, "statement"));
        }

        {
            String table = "reference_subject_descriptors";
            NodeMapping subject = iri(reference, "reference");

            quad(table, graph, subject, iri("fabio:hasSubjectTerm"), iri(dqmesh, "descriptor", "qualifier"));
        }
    }
}
