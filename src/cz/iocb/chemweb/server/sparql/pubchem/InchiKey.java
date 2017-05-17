package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Compound.compound;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.mesh;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class InchiKey extends PubChemMapping
{
    static IriClass inchikey;


    static void loadClasses()
    {
        classmap(inchikey = new IriClass("inchikey", 1, "http://rdf.ncbi.nlm.nih.gov/pubchem/inchikey/.*"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:inchikey");

        {
            String table = "inchikey_bases";
            NodeMapping subject = iri(inchikey, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("sio:CHEMINF_000399"));
            quad(table, graph, subject, iri("sio:has-value"), literal(xsdString, "inchikey"));
        }

        {
            String table = "inchikey_compounds";
            NodeMapping subject = iri(inchikey, "inchikey");

            quad(table, graph, subject, iri("sio:is-attribute-of"), iri(compound, "compound"));
        }

        {
            String table = "inchikey_subjects";
            NodeMapping subject = iri(inchikey, "inchikey");

            quad(table, graph, subject, iri("dcterms:subject"), iri(mesh, "subject"));
        }
    }
}
