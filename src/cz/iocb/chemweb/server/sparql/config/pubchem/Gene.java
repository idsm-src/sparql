package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Gene
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(
                new IntegerUserIriClass("pubchem:gene", "integer", "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/GID"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:gene");

        {
            Table table = new Table(schema, "gene_bases");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_010035"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:sameAs"),
                    config.createIriMapping("ncbi:gene", "id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:prefLabel"),
                    config.createLiteralMapping(xsdString, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bao:BAO_0002870"),
                    config.createLiteralMapping(xsdString, "symbol"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("up:organism"),
                    config.createIriMapping("pubchem:taxonomy", "organism_id"));

            // deprecated
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Gene"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(xsdString, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:gene-symbol"),
                    config.createLiteralMapping(xsdString, "symbol"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("pubchem:taxonomy", "organism_id"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("up:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "organism_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));

            // deprecated extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "organism_id"));
        }

        {
            Table table = new Table(schema, "gene_alternatives");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:altLabel"),
                    config.createLiteralMapping(xsdString, "alternative"));

            // deprecated
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(xsdString, "alternative"));
        }

        {
            Table table = new Table(schema, "gene_references");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }

        {
            Table table = new Table(schema, "gene_ensembl_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("rdf:ensembl", "match"));
        }

        {
            Table table = new Table(schema, "gene_mesh_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("mesh:heading", "match"));
        }

        {
            Table table = new Table(schema, "gene_thesaurus_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("ontology:resource", Ontology.unitThesaurus, "match"));
        }

        {
            Table table = new Table(schema, "gene_ctdbase_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("ctdbase:gene", "match"));
        }

        {
            Table table = new Table(schema, "gene_expasy_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("expasy:enzyme", "match"));
        }

        {
            Table table = new Table(schema, "gene_medlineplus_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("medlineplus:gene", "match"));
        }

        {
            Table table = new Table(schema, "gene_omim_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("omim:entry", "match"));
        }

        {
            Table table = new Table(schema, "gene_alliancegenome_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("alliancegenome:gene", "match"));
        }

        {
            Table table = new Table(schema, "gene_genenames_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("genenames:gene", "match"));
        }

        {
            Table table = new Table(schema, "gene_kegg_matches");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("kegg:entry", "match"));
        }

        {
            Table table = new Table(schema, "gene_processes");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0000056"),
                    config.createIriMapping("ontology:resource", Ontology.unitGO, "process_id"));
        }

        {
            Table table = new Table(schema, "gene_functions");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0000085"),
                    config.createIriMapping("ontology:resource", Ontology.unitGO, "function_id"));
        }

        {
            Table table = new Table(schema, "gene_locations");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0001025"),
                    config.createIriMapping("ontology:resource", Ontology.unitGO, "location_id"));
        }

        {
            Table table = new Table(schema, "gene_orthologs");
            NodeMapping subject = config.createIriMapping("pubchem:gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000558"),
                    config.createIriMapping("pubchem:gene", "ortholog"));
        }
    }
}
