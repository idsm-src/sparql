package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdDate;
import static cz.iocb.chemweb.server.sparql.pubchem.Compound.compound;
import static cz.iocb.chemweb.server.sparql.pubchem.Measuregroup.measuregroup;
import static cz.iocb.chemweb.server.sparql.pubchem.Reference.reference;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.pdblink;
import static cz.iocb.chemweb.server.sparql.pubchem.Source.source;
import static cz.iocb.chemweb.server.sparql.pubchem.SubstanceDescriptor.descriptorSubstanceVersion;
import static cz.iocb.chemweb.server.sparql.pubchem.Synonym.synonym;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Substance extends PubChemMapping
{
    static IriClass substance;
    static IriClass substanceChembl;
    static IriClass substanceEbiChembl;


    static void loadClasses()
    {
        classmap(substance = new IriClass("substance", 1, "http://rdf.ncbi.nlm.nih.gov/pubchem/substance/SID[0-9]+"));
        classmap(substanceChembl = new IriClass("substance_chembl", 1,
                "http://linkedchemistry.info/chembl/chemblid/S?CHEMBL[0-9]+"));
        classmap(substanceEbiChembl = new IriClass("substance_ebi_chembl", 1,
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/S?CHEMBL[0-9]+"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:substance");

        {
            String table = "substance_bases";
            NodeMapping subject = iri(substance, "id");

            quad(table, graph, subject, iri("dcterms:available"), literal(xsdDate, "available"));
            quad(table, graph, subject, iri("dcterms:source"), iri(source, "source"));
            quad(table, graph, subject, iri("dcterms:modified"), literal(xsdDate, "modified"));
            quad(table, graph, subject, iri("sio:CHEMINF_000477"), iri(compound, "compound"), "compound is not null");
        }

        {
            String table = "substance_types";
            NodeMapping subject = iri(substance, "substance");

            //TODO: quad(table, graph, subject, iri("rdf:type"), iri(rdfclass, "chebi"));
        }

        {
            String table = "endpoint_bases";
            NodeMapping subject = iri(substance, "substance");

            quad(table, graph, subject, iri("obo:BFO_0000056"), iri(measuregroup, "bioassay", "measuregroup"));
        }

        {
            String table = "substance_matches";
            NodeMapping subject = iri(substance, "substance");

            quad(table, graph, subject, iri("skos:exactMatch"), iri(substanceChembl, "match"));
            quad(table, graph, subject, iri("skos:exactMatch"), iri(substanceEbiChembl, "match"));
        }

        {
            String table = "substance_references";
            NodeMapping subject = iri(substance, "substance");

            quad(table, graph, subject, iri("cito:isDiscussedBy"), iri(reference, "reference"));
        }

        {
            String table = "substance_pdblinks";
            NodeMapping subject = iri(substance, "substance");

            quad(table, graph, subject, iri("pdbo:link_to_pdb"), iri(pdblink, "pdblink"));
        }

        {
            String table = "substance_synonyms";
            NodeMapping subject = iri(substance, "substance");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(synonym, "synonym"));
        }

        {
            String table = "descriptor_substance_bases";
            NodeMapping subject = iri(substance, "substance");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorSubstanceVersion, "substance"));
        }
    }
}
