package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Activity
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass(schema, "activity", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/activity/CHEMBL_ACT_(0|[1-9][0-9]*)"));

        config.addIriClass(new UserIriClass(schema, "uo_unit", Arrays.asList("varchar"),
                "http://purl\\.obolibrary\\.org/obo/UO_[0-9]{7}"));

        config.addIriClass(new UserIriClass(schema, "qudt_unit", Arrays.asList("varchar"),
                "(http://qudt\\.org/vocab/unit#(Centimeter|Day|DegreeCelsius|Gram|Hour|InternationalUnitPerLiter|"
                        + "Kilogram|Liter|Micrometer|Millimeter|MilliSecond|MinuteTime|Percent|SecondTime))|"
                        + "(http://www\\.openphacts\\.org/units/(GramPerLiter|MicrogramPerMilliliter|Micromolar|"
                        + "MilligramPerDeciliter|MilligramPerMilliliter|Millimolar|Molar|NanogramPerMilliliter|"
                        + "Nanomolar|PicogramPerMilliliter|Picomolar))"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass activity = config.getIriClass("activity");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "activities";
        NodeMapping subject = config.createIriMapping(activity, "activity_id");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Activity"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bao:BAO_0000208"),
                config.createIriMapping("bao", "bao_endpoint"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasUnitOnto"),
                config.createIriMapping("uo_unit", "uo_units"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasQUDT"),
                config.createIriMapping("qudt_unit", "qudt_units"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasMolecule"),
                config.createIriMapping("molecule", "molregno"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasDocument"),
                config.createIriMapping("document", "doc_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:dataValidityIssue"),
                config.createLiteralMapping(true), "data_validity_comment is not null");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:potentialDuplicate"),
                config.createLiteralMapping(true), "potential_duplicate = 1::smallint");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:value"),
                config.createLiteralMapping(xsdDouble, "(value::float8)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:standardValue"),
                config.createLiteralMapping(xsdDouble, "(standard_value::float8)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:pChembl"),
                config.createLiteralMapping(xsdDouble, "(pchembl_value::float8)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "('CHEMBL_ACT_' || activity_id)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('CHEMBL_ACT_' || activity_id)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:units"),
                config.createLiteralMapping(xsdString, "units"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:type"),
                config.createLiteralMapping(xsdString, "type"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:relation"),
                config.createLiteralMapping(xsdString, "relation"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:standardUnits"),
                config.createLiteralMapping(xsdString, "standard_units"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:standardType"),
                config.createLiteralMapping(xsdString, "standard_type"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:standardRelation"),
                config.createLiteralMapping(xsdString, "standard_relation"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:activityComment"),
                config.createLiteralMapping(xsdString, "activity_comment"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:dataValidityComment"),
                config.createLiteralMapping(xsdString, "data_validity_comment"));

        config.addQuadMapping(schema, table, graph, config.createIriMapping("molecule", "molregno"),
                config.createIriMapping("cco:hasActivity"), subject);

        config.addQuadMapping(schema, table, graph, config.createIriMapping("document", "doc_id"),
                config.createIriMapping("cco:hasActivity"), subject);
    }
}
