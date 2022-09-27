package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static java.util.Arrays.asList;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;



public class Endpoint
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new GeneralUserIriClass("pubchem:endpoint", "pubchem", "endpoint",
                asList("integer", "integer", "integer"),
                "http://rdf\\.ncbi\\.nlm\\.nih\\.gov/pubchem/endpoint/SID[1-9][0-9]*_AID[1-9][0-9]*(_(PMID([1-9][0-9]*)?|[1-9][0-9]*|0))?"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:endpoint");

        {
            Table table = new Table(schema, "endpoint_bases");
            NodeMapping subject = config.createIriMapping("pubchem:endpoint", "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:IAO_0000136"),
                    config.createIriMapping("pubchem:substance", "substance"));
        }

        {
            Table table = new Table(schema, "endpoint_outcomes");
            NodeMapping subject = config.createIriMapping("pubchem:endpoint", "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:PubChemAssayOutcome"),
                    config.createIriMapping("ontology:resource", Ontology.unitUncategorized, "outcome_id"));
        }

        {
            Table table = new Table(schema, "endpoint_measurements");
            NodeMapping subject = config.createIriMapping("pubchem:endpoint", "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000221"),
                    config.createIriMapping("obo:UO_0000064"));

            // deprecated
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-unit"),
                    config.createIriMapping("obo:UO_0000064"));
        }

        {
            Table table = new Table(schema, "endpoint_measurement_types");
            NodeMapping subject = config.createIriMapping("pubchem:endpoint", "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology:resource", Ontology.unitBAO, "type_id"));
        }

        {
            Table table = new Table(schema, "endpoint_measurement_labels");
            NodeMapping subject = config.createIriMapping("pubchem:endpoint", "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));
        }

        {
            Table table = new Table(schema, "endpoint_measurement_values");
            NodeMapping subject = config.createIriMapping("pubchem:endpoint", "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdFloat, "value"));

            // deprecated
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdFloat, "value"));
        }

        {
            Table table = new Table(schema, "endpoint_references");
            NodeMapping subject = config.createIriMapping("pubchem:endpoint", "substance", "bioassay", "measuregroup");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cito:citesAsDataSource"),
                    config.createIriMapping("pubchem:reference", "reference"));
        }
    }
}
