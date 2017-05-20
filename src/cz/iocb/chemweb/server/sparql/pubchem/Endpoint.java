package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Reference.reference;
import static cz.iocb.chemweb.server.sparql.pubchem.Substance.substance;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Endpoint extends PubChemMapping
{
    static IriClass endpoint;
    static IriClass outcome;


    static void loadClasses()
    {
        classmap(endpoint = new IriClass("endpoint", Arrays.asList("integer", "integer", "integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/endpoint/SID[0-9]+_AID[0-9]+(_(PMID[0-9]*|[0-9]+))?"));
        classmap(outcome = new IriClass("outcome", Arrays.asList("smallint"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/vocabulary#(active|inactive|inconclusive|unspecified|probe)"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:endpoint");

        {
            String table = "endpoint_bases";
            NodeMapping subject = iri(endpoint, "substance", "bioassay", "measuregroup");

            quad(table, graph, subject, iri("obo:IAO_0000136"), iri(substance, "substance"));
            quad(table, graph, subject, iri("vocab:PubChemAssayOutcome"), iri(outcome, "outcome"));
        }

        {
            String table = "endpoint_measurements";
            NodeMapping subject = iri(endpoint, "substance", "bioassay", "measuregroup");

            quad(table, graph, subject, iri("sio:has-unit"), iri("obo:UO_0000064"));
            //TODO: quad(table, graph, subject, iri("rdf:type"), iri(rdfclass, "type"));
            quad(table, graph, subject, iri("rdfs:label"), literal(xsdString, "label"));
            quad(table, graph, subject, iri("sio:has-value"), literal(xsdFloat, "value"));
        }

        {
            String table = "endpoint_references";
            NodeMapping subject = iri(endpoint, "substance", "bioassay", "measuregroup");

            quad(table, graph, subject, iri("cito:citesAsDataSource"), iri(reference, "reference"));
        }

        {
            String table = "endpoint_outcomes__reftable";
            NodeMapping subject = iri(outcome, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("vocab:PubChemBioAssayOutcomeCategory"));
            quad(table, graph, subject, iri("template:itemTemplate"),
                    literal("pubchem/PubChemBioAssayOutcomeCategory.vm"));
            quad(table, graph, subject, iri("template:pageTemplate"),
                    literal("pubchem/PubChemBioAssayOutcomeCategory.vm"));
        }
    }
}
