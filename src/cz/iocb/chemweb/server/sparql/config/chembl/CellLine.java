package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class CellLine
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("chembl:cell_line", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/cell_line/CHEMBL"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        Table table = new Table(schema, "cell_dictionary");
        NodeMapping subject = config.createIriMapping("chembl:cell_line", "id");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:CellLine"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasCLO"),
                config.createIriMapping("ontology:resource", Ontology.unitCLO, "clo_resource_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasCLO"),
                config.createIriMapping("ontology:resource", Ontology.unitCL, "cl_resource_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasEFO"),
                config.createIriMapping("ontology:resource", Ontology.unitEFO, "efo_resource_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:cellXref"),
                config.createIriMapping("reference:life", "cl_lincs_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ontology:resource", Ontology.unitTaxonomy, "cell_source_tax_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("reference:ncbi-taxonomy", "cell_source_tax_id"));
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
        config.addQuadMapping(table, graph, config.createIriMapping("reference:life", "cl_lincs_id"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:LincsCellRef"));
        config.addQuadMapping(table, graph, config.createIriMapping("reference:life", "cl_lincs_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(chembl_id || ' LINCS Project Reference: ' || cl_lincs_id)"));

        // extension
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ontology:resource", Ontology.unitNCBITaxon, "cell_source_tax_id"));
    }
}
