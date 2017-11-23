package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Protein
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(
                new IriClass("protein", Arrays.asList("integer"), "http://rdf.ncbi.nlm.nih.gov/pubchem/protein/.*"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        IriClass protein = config.getIriClass("protein");
        NodeMapping graph = config.createIriMapping("pubchem:protein");

        {
            String table = "protein_bases";
            NodeMapping subject = config.createIriMapping(protein, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Protein"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(xsdString, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("taxonomy", "organism"), "organism is not null");
        }

        {
            String table = "protein_references";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("reference", "reference"));
        }

        {
            String table = "protein_pdblinks";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("pdbo:link_to_pdb"),
                    config.createIriMapping("pdblink", "pdblink"));
        }

        {
            String table = "protein_similarproteins";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:hasSimilarProtein"),
                    config.createIriMapping(protein, "simprotein"));
        }

        {
            String table = "protein_genes";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:encodedBy"),
                    config.createIriMapping("gene", "gene"));
        }

        {
            String table = "protein_closematches";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("uniprot", "match"));
        }

        {
            String table = "protein_conserveddomains";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000110"),
                    config.createIriMapping("conserveddomain", "domain"));
        }

        {
            String table = "protein_continuantparts";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000178"),
                    config.createIriMapping(protein, "part"));
        }

        {
            String table = "protein_participates_goes";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping("go", "participation"));
        }

        {
            String table = "protein_participates_biosystems";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping("biosystem", "biosystem"));
        }

        {
            String table = "protein_functions";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000160"),
                    config.createIriMapping("go", "gofunction"));
        }

        {
            String table = "protein_locations";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000171"),
                    config.createIriMapping("go", "location"));
        }

        {
            /* TODO:
            String table = "protein_types";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("class", "type"));
            */
        }

        {
            String table = "protein_complexes";
            NodeMapping subject = config.createIriMapping(protein, "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:GO_0043234"));
        }
    }
}
