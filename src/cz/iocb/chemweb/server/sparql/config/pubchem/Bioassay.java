package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Bioassay
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(
                new IntegerUserIriClass("bioassay", "integer", "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/AID"));

        config.addIriClass(new IntegerUserIriClass("bioassay_description", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/AID", "_Description"));

        config.addIriClass(new IntegerUserIriClass("bioassay_protocol", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/AID", "_Protocol"));

        config.addIriClass(new IntegerUserIriClass("bioassay_comment", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/AID", "_Comment"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass bioassay = config.getIriClass("bioassay");
        ConstantIriMapping graph = config.createIriMapping("pubchem:bioassay");

        {
            String table = "bioassay_bases";
            NodeMapping subject = config.createIriMapping(bioassay, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("bao:BAO_0000015"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Bioassay.vm"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Bioassay.vm"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:source"),
                    config.createIriMapping("source", "source"));
        }

        {
            String table = "bioassay_measuregroups";
            NodeMapping subject = config.createIriMapping(bioassay, "bioassay");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bao:BAO_0000209"),
                    config.createIriMapping("measuregroup", "bioassay", "measuregroup"));
        }

        {
            String table = "bioassay_data";
            NodeMapping subject = config.createIriMapping("bioassay_description", "bioassay");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", "cheminf:SIO_000136"), "type_id = '136'::smallint");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping(bioassay, "bioassay"), "type_id = '136'::smallint");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(rdfLangStringEn, "value"), "type_id = '136'::smallint");
        }

        {
            String table = "bioassay_data";
            NodeMapping subject = config.createIriMapping("bioassay_protocol", "bioassay");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", "cheminf:SIO_001041"), "type_id = '1041'::smallint");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping(bioassay, "bioassay"), "type_id = '1041'::smallint");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(rdfLangStringEn, "value"), "type_id = '1041'::smallint");
        }

        {
            String table = "bioassay_data";
            NodeMapping subject = config.createIriMapping("bioassay_comment", "bioassay");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", "cheminf:SIO_001041"), "type_id = '1167'::smallint");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping(bioassay, "bioassay"), "type_id = '1167'::smallint");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(rdfLangStringEn, "value"), "type_id = '1167'::smallint");
        }
    }
}
