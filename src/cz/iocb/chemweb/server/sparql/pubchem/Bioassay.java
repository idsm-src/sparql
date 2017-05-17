package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Measuregroup.measuregroup;
import static cz.iocb.chemweb.server.sparql.pubchem.Source.source;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;;



class Bioassay extends PubChemMapping
{
    static IriClass bioassay;
    static IriClass bioassayData;


    static void loadClasses()
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/AID[0-9]+";

        classmap(bioassay = new IriClass("bioassay", 1, prefix));
        classmap(bioassayData = new IriClass("bioassay_data", 2, prefix + "_(Description|Protocol|Comment)"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:bioassay");

        {
            String table = "bioassay_bases";
            NodeMapping subject = iri(bioassay, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("bao:BAO_0000015"));
            quad(table, graph, subject, iri("template:itemTemplate"), literal("pubchem/Bioassay.vm"));
            quad(table, graph, subject, iri("template:pageTemplate"), literal("pubchem/Bioassay.vm"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
            quad(table, graph, subject, iri("dcterms:source"), iri(source, "source"));
        }

        {
            String table = "bioassay_measuregroups";
            NodeMapping subject = iri(bioassay, "bioassay");

            quad(table, graph, subject, iri("bao:BAO_0000209"), iri(measuregroup, "bioassay", "measuregroup"));
        }

        {
            String table = "bioassay_data";
            NodeMapping subject = iri(bioassayData, "bioassay", "type");

            //TODO: quad(table, graph, subject, iri("rdf:type"), iri(rdfclass, "type"));
            quad(table, graph, subject, iri("sio:is-attribute-of"), iri(bioassay, "bioassay"));
            quad(table, graph, subject, iri("sio:has-value"), literal(xsdString, "value"));
        }
    }
}
