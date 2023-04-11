package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static java.util.Arrays.asList;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass.SqlCheck;



public class Cooccurrence
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        final String prefix = "http://rdf\\.ncbi\\.nlm\\.nih\\.gov/pubchem/cooccurrence/";

        config.addIriClass(new GeneralUserIriClass("pubchem:chemical_chemical_cooccurrence", schema,
                "chemical_chemical_cooccurrence", asList("integer", "integer"),
                prefix + "CID[1-9][0-9]*_CID[1-9][0-9]*"));

        config.addIriClass(new GeneralUserIriClass("pubchem:chemical_disease_cooccurrence", schema,
                "chemical_disease_cooccurrence", asList("integer", "integer"),
                prefix + "CID[1-9][0-9]*_DZID[1-9][0-9]*"));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:chemical_gene_cooccurrence", schema, "chemical_gene_cooccurrence",
                        asList("integer", "integer"), prefix + "CID[1-9][0-9]*_.*", SqlCheck.IF_MATCH));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:chemical_enzyme_cooccurrence", schema, "chemical_enzyme_cooccurrence",
                        asList("integer", "integer"), prefix + "CID[1-9][0-9]*_EC_.*", SqlCheck.IF_MATCH));

        config.addIriClass(new GeneralUserIriClass("pubchem:disease_chemical_cooccurrence", schema,
                "disease_chemical_cooccurrence", asList("integer", "integer"),
                prefix + "DZID[1-9][0-9]*_CID[1-9][0-9]*"));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:disease_disease_cooccurrence", schema, "disease_disease_cooccurrence",
                        asList("integer", "integer"), prefix + "DZID[1-9][0-9]*_DZID[1-9][0-9]*"));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:disease_gene_cooccurrence", schema, "disease_gene_cooccurrence",
                        asList("integer", "integer"), prefix + "DZID[1-9][0-9]*_.*", SqlCheck.IF_MATCH));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:disease_enzyme_cooccurrence", schema, "disease_enzyme_cooccurrence",
                        asList("integer", "integer"), prefix + "DZID[1-9][0-9]*_EC_.*", SqlCheck.IF_MATCH));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:gene_chemical_cooccurrence", schema, "gene_chemical_cooccurrence",
                        asList("integer", "integer"), prefix + ".*_CID[1-9][0-9]*", SqlCheck.IF_MATCH));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:enzyme_chemical_cooccurrence", schema, "enzyme_chemical_cooccurrence",
                        asList("integer", "integer"), prefix + "EC_.*_CID[1-9][0-9]*", SqlCheck.IF_MATCH));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:gene_disease_cooccurrence", schema, "gene_disease_cooccurrence",
                        asList("integer", "integer"), prefix + ".*_DZID[1-9][0-9]*", SqlCheck.IF_MATCH));

        config.addIriClass(
                new GeneralUserIriClass("pubchem:enzyme_disease_cooccurrence", schema, "enzyme_disease_cooccurrence",
                        asList("integer", "integer"), prefix + "EC_.*_DZID[1-9][0-9]*", SqlCheck.IF_MATCH));

        config.addIriClass(new GeneralUserIriClass("pubchem:gene_gene_cooccurrence", schema, "gene_gene_cooccurrence",
                asList("integer", "integer"), prefix + ".*_.*", SqlCheck.IF_MATCH));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:cooccurrence");
        {
            Table table = new Table(schema, "chemical_chemical_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:chemical_chemical_cooccurrence", "subject",
                    "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001435"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:compound", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:compound", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "chemical_disease_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:chemical_disease_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_000993"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:compound", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:disease", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "chemical_gene_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:chemical_gene_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001257"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:compound", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:gene_symbol", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "chemical_enzyme_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:chemical_enzyme_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001257"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:compound", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:enzyme", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "disease_chemical_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:disease_chemical_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_000993"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:disease", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:compound", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "disease_disease_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:disease_disease_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001436"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:disease", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:disease", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "disease_gene_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:disease_gene_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_000983"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:disease", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:gene_symbol", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "disease_enzyme_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:disease_enzyme_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_000983"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:disease", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:enzyme", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "gene_chemical_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:gene_chemical_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001257"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:gene_symbol", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:compound", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "enzyme_chemical_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:enzyme_chemical_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001257"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:enzyme", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:compound", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "gene_disease_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:gene_disease_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_000983"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:gene_symbol", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:disease", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "enzyme_disease_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:enzyme_disease_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_000983"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:enzyme", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:disease", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }

        {
            Table table = new Table(schema, "gene_gene_cooccurrences");
            NodeMapping subject = config.createIriMapping("pubchem:gene_gene_cooccurrence", "subject", "object");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_001437"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:subject"),
                    config.createIriMapping("pubchem:gene_symbol", "subject"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:object"),
                    config.createIriMapping("pubchem:gene_symbol", "object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_001157"),
                    config.createIriMapping("edam:operation_0306"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:SIO_000300"),
                    config.createLiteralMapping(xsdInteger, "value"));
        }
    }
}
