package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Assay
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("assay", "bigint", new Table(schema, "assays"),
                new TableColumn("assay_id"), new TableColumn("chembl_id"),
                "http://rdf.ebi.ac.uk/resource/chembl/assay/", " CHEMBL[1-9][0-9]*"));

        config.addIriClass(new StringUserIriClass("pubchem_assay",
                "http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?aid=", "[1-9][0-9]*"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass assay = config.getIriClass("assay");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "assays";
        NodeMapping subject = config.createIriMapping(assay, "assay_id");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Assay"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("taxonomy", "assay_tax_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ncbi_taxonomy", "assay_tax_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bao:BAO_0000205"),
                config.createIriMapping("bao", "bao_format"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasSource"),
                config.createIriMapping("source", "src_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:assayXref"),
                config.createIriMapping("pubchem_assay", "(replace(src_assay_id,'_',''))"), "src_id = 7");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasTarget"),
                config.createIriMapping("target", "tid"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasDocument"),
                config.createIriMapping("document", "doc_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasCellLine"),
                config.createIriMapping("cell_line", "cell_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetConfScore"),
                config.createLiteralMapping(xsdInt, "confidence_score"), "confidence_score != 0");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetRelType"),
                config.createLiteralMapping(xsdString, "relationship_type"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "chembl_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "chembl_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:description"),
                config.createLiteralMapping(xsdString, "description"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:organismName"),
                config.createLiteralMapping(xsdString, "assay_organism"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:assayCellType"),
                config.createLiteralMapping(xsdString, "assay_cell_type"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:assayStrain"),
                config.createLiteralMapping(xsdString, "assay_strain"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:assayTissue"),
                config.createLiteralMapping(xsdString, "assay_tissue"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:assayTestType"),
                config.createLiteralMapping(xsdString, "assay_test_type"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:assaySubCellFrac"),
                config.createLiteralMapping(xsdString, "assay_subcellular_fraction"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:assayCategory"),
                config.createLiteralMapping(xsdString, "assay_category"));

        config.addQuadMapping(schema, table, schema, "assay_type", "assay_type", "assay_type", graph, subject,
                config.createIriMapping("cco:assayType"), config.createLiteralMapping(xsdString, "assay_desc"));

        config.addQuadMapping(schema, table, schema, "relationship_type", "relationship_type", "relationship_type",
                graph, subject, config.createIriMapping("cco:targetRelDesc"),
                config.createLiteralMapping(xsdString, "relationship_desc"));

        config.addQuadMapping(schema, table, schema, "confidence_score_lookup", "confidence_score", "confidence_score",
                graph, subject, config.createIriMapping("cco:targetConfDesc"),
                config.createLiteralMapping(xsdString, "description"));

        config.addQuadMapping(schema, table, graph, config.createIriMapping("cell_line", "cell_id"),
                config.createIriMapping("cco:isCellLineForAssay"), subject);

        config.addQuadMapping(schema, table, graph, config.createIriMapping("source", "src_id"),
                config.createIriMapping("cco:hasAssay"), subject);

        config.addQuadMapping(schema, table, graph, config.createIriMapping("target", "tid"),
                config.createIriMapping("cco:hasAssay"), subject);

        config.addQuadMapping(schema, table, graph, config.createIriMapping("document", "doc_id"),
                config.createIriMapping("cco:hasAssay"), subject);

        config.addQuadMapping(schema, table, graph,
                config.createIriMapping("pubchem_assay", "(replace(src_assay_id,'_',''))"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:PubchemBioassayRef"), "src_id = 7");

        config.addQuadMapping(schema, table, graph,
                config.createIriMapping("pubchem_assay", "(replace(src_assay_id,'_',''))"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "(chembl_id || ' PubChem BioAssay Reference: ' || replace(src_assay_id,'_',''))"),
                "src_id = 7");


        config.addQuadMapping(schema, "activities", graph, subject, config.createIriMapping("cco:hasActivity"),
                config.createIriMapping("activity", "activity_id"));

        config.addQuadMapping(schema, "activities", graph, config.createIriMapping("activity", "activity_id"),
                config.createIriMapping("cco:hasAssay"), subject);
    }
}
