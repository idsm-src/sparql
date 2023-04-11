package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;



public class Gene
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        String pattern = "([0123569]|[1-9][0-9]{1,8}|[1-1][0-9]{9}|2[0-0][0-9]{8}|21[0-3][0-9]{7}|214[0-6][0-9]{6}|2147[0-3][0-9]{5}|21474[0-7][0-9]{4}|214748[0-2][0-9]{3}|2147483[0-5][0-9]{2}|21474836[0-3][0-9]{1}|214748364[0-7])";

        config.addIriClass(new MapUserIriClass("pubchem:gene_symbol", "integer", new Table(schema, "gene_symbol_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/",
                "(GI?D?|GID[478]|([^G]|G[^I]|GI[^D]).*)"));
        config.addIriClass(new IntegerUserIriClass("pubchem:gene", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/GID", 0, pattern, null));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:gene");

        {
            Table table = new Table(schema, "gene_symbol_bases");
            NodeMapping subject = config.createIriMapping("pubchem:gene_symbol", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001383"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdString, "iri"));
        }

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
                    config.createIriMapping("pubchem:gene_symbol", "gene_symbol"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("up:organism"),
                    config.createIriMapping("pubchem:taxonomy", "organism"));

            // deprecated
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bp:Gene"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(xsdString, "title"));
            config.addQuadMapping(table, new Table(schema, "gene_symbol_bases"), "gene_symbol", "id", graph, subject,
                    config.createIriMapping("sio:gene-symbol"), config.createLiteralMapping(xsdString, "iri"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("pubchem:taxonomy", "organism"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("up:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "organism"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Gene.vm"));

            // deprecated extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("bp:organism"),
                    config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "organism"));
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
