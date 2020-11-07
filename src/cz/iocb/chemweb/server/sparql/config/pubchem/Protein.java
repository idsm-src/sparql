package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;



public class Protein
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("pubchem:protein", "integer", new Table("pubchem", "protein_bases"),
                new TableColumn("id"), new TableColumn("name"), "http://rdf.ncbi.nlm.nih.gov/pubchem/protein/", 0));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:protein");

        {
            Table table = new Table(schema, "protein_bases");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Protein"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitTaxonomy, "organism_id"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "organism_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Protein.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Protein.vm"));
        }

        {
            Table table = new Table(schema, "protein_references");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }

        {
            Table table = new Table(schema, "protein_pdblinks");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("pdbo40:link_to_pdb"),
                    config.createIriMapping("rdf:wwpdb_old", "pdblink"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("pdbo:link_to_pdb"),
                    config.createIriMapping("rdf:wwpdb", "pdblink"));
        }

        {
            Table table = new Table(schema, "protein_similarproteins");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:hasSimilarProtein"),
                    config.createIriMapping("pubchem:protein", "simprotein"));
        }

        {
            Table table = new Table(schema, "protein_genes");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("up:encodedBy"),
                    config.createIriMapping("pubchem:gene", "gene"));
        }

        {
            Table table = new Table(schema, "protein_uniprot_closematches");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("purl:uniprot", "match"));
        }

        {
            Table table = new Table(schema, "protein_closematches");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("ncbi:protein", "match"));
        }

        {
            Table table = new Table(schema, "protein_conserveddomains");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0002180"),
                    config.createIriMapping("pubchem:conserveddomain", "domain"));
        }

        {
            Table table = new Table(schema, "protein_continuantparts");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0002180"),
                    config.createIriMapping("pubchem:protein", "part"));
        }

        {
            Table table = new Table(schema, "protein_types");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology:resource", Ontology.unitPR, "type_id"));
        }

        {
            Table table = new Table(schema, "protein_complexes");
            NodeMapping subject = config.createIriMapping("pubchem:protein", "protein");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:GO_0043234"));
        }
    }
}
