package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class DrugIndication
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass("drug_indication", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/drug_indication/CHEMBL_IND_(0|[1-9][0-9]*)"));

        config.addIriClass(new UserIriClass("mesh", Arrays.asList("varchar"), "http://identifiers\\.org/mesh/D[0-9]+"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass indication = config.getIriClass("drug_indication");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "drug_indication";
        NodeMapping subject = config.createIriMapping(indication, "drugind_id");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:DrugIndication"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasMesh"),
                config.createIriMapping("mesh", "mesh_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasEFO"),
                config.createIriMapping("efo", "efo_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasMolecule"),
                config.createIriMapping("molecule", "molregno"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:highestDevelopmentPhase"),
                config.createLiteralMapping(xsdInt, "max_phase_for_ind"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "('CHEMBL_IND_' || drugind_id)"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('CHEMBL_IND_' || drugind_id)"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasMeshHeading"),
                config.createLiteralMapping(xsdString, "mesh_heading"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasEFOName"),
                config.createLiteralMapping(xsdString, "efo_term"));

        config.addQuadMapping(table, graph, config.createIriMapping("molecule", "molregno"),
                config.createIriMapping("cco:hasDrugIndication"), subject);
    }
}
