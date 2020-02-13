package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class CellLine
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("cell_line", "bigint", new Table(schema, "cell_dictionary"),
                new TableColumn("cell_id"), new TableColumn("chembl_id"),
                "http://rdf.ebi.ac.uk/resource/chembl/cell_line/", "CHEMBL[1-9][0-9]*"));

        config.addIriClass(new StringUserIriClass("clo", "http://purl.obolibrary.org/obo/", "CLO_[0-9]{7}"));

        config.addIriClass(new StringUserIriClass("lincs_cell_ref",
                "http://life.ccs.miami.edu/life/summary?mode=CellLine&source=LINCS&input=", "LCL-[0-9]{4}"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass line = config.getIriClass("cell_line");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "cell_dictionary";
        NodeMapping subject = config.createIriMapping(line, "cell_id");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:CellLine"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasCLO"),
                config.createIriMapping("clo", "clo_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasEFO"),
                config.createIriMapping("efo", "(replace(efo_id, '_', ':'))"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:cellXref"),
                config.createIriMapping("lincs_cell_ref", "cl_lincs_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("taxonomy", "cell_source_tax_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ncbi_taxonomy", "cell_source_tax_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "chembl_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "cell_name"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:description"),
                config.createLiteralMapping(xsdString, "cell_description"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:organismName"),
                config.createLiteralMapping(xsdString, "cell_source_organism"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:cellosaurusId"),
                config.createLiteralMapping(xsdString, "cellosaurus_id"));

        config.addQuadMapping(schema, table, graph, config.createIriMapping("lincs_cell_ref", "cl_lincs_id"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:LincsCellRef"));

        config.addQuadMapping(schema, table, graph, config.createIriMapping("lincs_cell_ref", "cl_lincs_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(chembl_id || ' LINCS Project Reference: ' || cl_lincs_id)"));
    }
}
