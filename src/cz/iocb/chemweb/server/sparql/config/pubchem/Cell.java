package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Cell
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(
                new IntegerUserIriClass("pubchem:cell", "integer", "http://rdf.ncbi.nlm.nih.gov/pubchem/cell/CELLID"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:cell");

        {
            Table table = new Table(schema, "cell_bases");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_010054"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:CLO_0000031"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("up:organism"),
                    config.createIriMapping("pubchem:taxonomy", "organism"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:prefLabel"),
                    config.createLiteralMapping(xsdString, "label"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Cell.vm"));
        }

        {
            Table table = new Table(schema, "cell_alternatives");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "cell");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:altLabel"),
                    config.createLiteralMapping("alternative"));
        }

        {
            Table table = new Table(schema, "cell_occurrences");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "cell");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000050"),
                    config.createLiteralMapping("occurrence"));
        }

        {
            Table table = new Table(schema, "cell_references");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "cell");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:isDiscussedBy"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }

        {
            Table table = new Table(schema, "cell_matches");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "cell");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("ontology:resource", "match_unit", "match_id"));
        }

        {
            Table table = new Table(schema, "cell_cellosaurus_matches");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "cell");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("expasy:cell", "match"));
        }

        {
            Table table = new Table(schema, "cell_mesh_matches");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "cell");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:closeMatch"),
                    config.createIriMapping("mesh:heading", "match"));
        }

        {
            Table table = new Table(schema, "cell_anatomies");
            NodeMapping subject = config.createIriMapping("pubchem:cell", "cell");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:RO_0001000"),
                    config.createIriMapping("pubchem:anatomy", "anatomy"));
        }
    }
}
