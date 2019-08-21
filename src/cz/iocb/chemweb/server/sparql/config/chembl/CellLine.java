package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass.SqlCheck;



class CellLine
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass("cell_line", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/cell_line/CHEMBL[1-9][0-9]*", SqlCheck.IF_MATCH));

        config.addIriClass(
                new UserIriClass("clo", Arrays.asList("varchar"), "http://purl\\.obolibrary\\.org/obo/CLO_[0-9]{7}"));

        config.addIriClass(new UserIriClass("lincs_cell_ref", Arrays.asList("varchar"),
                "http://life\\.ccs\\.miami\\.edu/life/summary\\?mode=CellLine&source=LINCS&input=LCL-[0-9]{4}"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass line = config.getIriClass("cell_line");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "cell_dictionary";
        NodeMapping subject = config.createIriMapping(line, "cell_id");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:CellLine"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasCLO"),
                config.createIriMapping("clo", "clo_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasEFO"),
                config.createIriMapping("efo", "(replace(efo_id, '_', ':'))"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:cellXref"),
                config.createIriMapping("lincs_cell_ref", "cl_lincs_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("taxonomy", "cell_source_tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ncbi_taxonomy", "cell_source_tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "chembl_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "cell_name"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:description"),
                config.createLiteralMapping(xsdString, "cell_description"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:organismName"),
                config.createLiteralMapping(xsdString, "cell_source_organism"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:cellosaurusId"),
                config.createLiteralMapping(xsdString, "cellosaurus_id"));

        config.addQuadMapping(table, graph, config.createIriMapping("lincs_cell_ref", "cl_lincs_id"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:LincsCellRef"));

        config.addQuadMapping(table, graph, config.createIriMapping("lincs_cell_ref", "cl_lincs_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(chembl_id || ' LINCS Project Reference: ' || cl_lincs_id)"));
    }
}
