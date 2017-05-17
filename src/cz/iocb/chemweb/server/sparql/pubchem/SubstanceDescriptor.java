package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdInteger;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class SubstanceDescriptor extends PubChemMapping
{
    static IriClass descriptorSubstanceVersion;


    static void loadClasses()
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/SID[0-9]+_";

        classmap(descriptorSubstanceVersion = new IriClass("substance_version", 1, prefix + "Substance_Version"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("descriptor:substance");

        {
            String table = "descriptor_substance_bases";
            NodeMapping subject = iri(descriptorSubstanceVersion, "substance");

            quad(table, graph, subject, iri("rdf:type"), iri("obo:IAO_0000129"));
            quad(table, graph, subject, iri("sio:has-value"), literal(xsdInteger, "version"));
        }
    }
}
