package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Biosystem.biosystem;
import static cz.iocb.chemweb.server.sparql.pubchem.ConservedDomain.conserveddomain;
import static cz.iocb.chemweb.server.sparql.pubchem.Gene.gene;
import static cz.iocb.chemweb.server.sparql.pubchem.Reference.reference;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.go;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.pdblink;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.taxonomy;
import static cz.iocb.chemweb.server.sparql.pubchem.Shared.uniprot;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Protein extends PubChemMapping
{
    static IriClass protein;


    static void loadClasses()
    {
        classmap(protein = new IriClass("protein", Arrays.asList("integer"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/protein/.*"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:protein");

        {
            String table = "protein_bases";
            NodeMapping subject = iri(protein, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("bp:Protein"));
            quad(table, graph, subject, iri("dcterms:title"), literal(xsdString, "title"));
            quad(table, graph, subject, iri("bp:organism"), iri(taxonomy, "organism"), "organism is not null");
        }

        {
            String table = "protein_references";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("cito:isDiscussedBy"), iri(reference, "reference"));
        }

        {
            String table = "protein_pdblinks";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("pdbo:link_to_pdb"), iri(pdblink, "pdblink"));
        }

        {
            String table = "protein_similarproteins";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("vocab:hasSimilarProtein"), iri(protein, "simprotein"));
        }

        {
            String table = "protein_genes";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("vocab:encodedBy"), iri(gene, "gene"));
        }

        {
            String table = "protein_closematches";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("skos:closeMatch"), iri(uniprot, "match"));
        }

        {
            String table = "protein_conserveddomains";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("obo:BFO_0000110"), iri(conserveddomain, "domain"));
        }

        {
            String table = "protein_continuantparts";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("obo:BFO_0000178"), iri(protein, "part"));
        }

        {
            String table = "protein_participates_goes";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("obo:BFO_0000056"), iri(go, "participation"));
        }

        {
            String table = "protein_participates_biosystems";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("obo:BFO_0000056"), iri(biosystem, "biosystem"));
        }

        {
            String table = "protein_functions";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("obo:BFO_0000160"), iri(go, "gofunction"));
        }

        {
            String table = "protein_locations";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("obo:BFO_0000171"), iri(go, "location"));
        }

        {
            String table = "protein_types";
            NodeMapping subject = iri(protein, "protein");

            //TODO: quad(table, graph, subject, iri("rdf:type"), iri(pr, "type"));
        }

        {
            String table = "protein_complexes";
            NodeMapping subject = iri(protein, "protein");

            quad(table, graph, subject, iri("rdf:type"), iri("obo:GO_0043234"));
        }
    }
}
