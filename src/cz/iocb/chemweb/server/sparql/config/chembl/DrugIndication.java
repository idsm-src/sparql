package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class DrugIndication
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("chembl:drug_indication", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/drug_indication/CHEMBL_IND_"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        Table table = new Table(schema, "drug_indication");
        NodeMapping subject = config.createIriMapping("chembl:drug_indication", "id");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:DrugIndication"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasMesh"),
                config.createIriMapping("identifiers:mesh", "mesh_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasEFO"),
                config.createIriMapping("ontology:resource", "efo_resource_unit", "efo_resource_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasMolecule"),
                config.createIriMapping("chembl:compound", "molecule_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:highestDevelopmentPhase"),
                config.createLiteralMapping(xsdInt, "max_phase_for_ind"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "chembl_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "chembl_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasMeshHeading"),
                config.createLiteralMapping(xsdString, "mesh_heading"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasEFOName"),
                config.createLiteralMapping(xsdString, "efo_term"));
        config.addQuadMapping(table, graph, config.createIriMapping("chembl:compound", "molecule_id"),
                config.createIriMapping("cco:hasDrugIndication"), subject);

        // extension
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasMesh"),
                config.createIriMapping("mesh:heading", "mesh_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                config.createLiteralMapping("chembl/DrugIndication.vm"));
    }
}
