package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Compound.compound;
import static cz.iocb.chemweb.server.sparql.pubchem.Concept.concept;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.mesh;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Synonym extends PubChemMapping
{
    static IriClass synonym;


    static void loadClasses()
    {
        classmap(synonym = new IriClass("synonym", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/synonym/MD5_.*"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:synonym");

        {
            String table = "synonym_values";
            NodeMapping subject = iri(synonym, "synonym");

            quad(table, graph, subject, iri("sio:has-value"), literal(xsdString, "value"));
        }

        {
            String table = "synonym_types";
            NodeMapping subject = iri(synonym, "synonym");

            //TODO: quad(table, graph, subject, iri("rdf:type"), iri(rdfclass, "type"));
        }

        {
            String table = "synonym_compounds";
            NodeMapping subject = iri(synonym, "synonym");

            quad(table, graph, subject, iri("sio:is-attribute-of"), iri(compound, "compound"));
        }

        {
            String table = "synonym_mesh_subjects";
            NodeMapping subject = iri(synonym, "synonym");

            quad(table, graph, subject, iri("dcterms:subject"), iri(mesh, "subject"));
        }

        {
            String table = "synonym_concept_subjects";
            NodeMapping subject = iri(synonym, "synonym");

            quad(table, graph, subject, iri("dcterms:subject"), iri(concept, "concept"));
        }
    }
}
