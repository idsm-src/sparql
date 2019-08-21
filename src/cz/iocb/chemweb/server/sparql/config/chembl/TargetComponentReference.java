package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class TargetComponentReference
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass("go_process_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/obo\\.go/GO:[0-9]{7}"));
        config.addIriClass(new UserIriClass("go_function_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/obo\\.go/GO:[0-9]{7}"));
        config.addIriClass(new UserIriClass("go_component_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/obo\\.go/GO:[0-9]{7}"));
        config.addIriClass(new UserIriClass("pdb_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/pdb/[0-9][A-Z0-9]{3}"));
        config.addIriClass(new UserIriClass("interpro_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/interpro/IPR[0-9]{6}"));
        config.addIriClass(new UserIriClass("reactome_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/reactome/R-[A-Z]{3}-[1-9][0-9]*"));
        config.addIriClass(new UserIriClass("pfam_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/pfam/PF[0-9]{5}"));
        config.addIriClass(new UserIriClass("enzyme_class_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/ec-code/((-|[1-9][0-9]*)\\.){3}(-|n?[1-9][0-9]*)"));
        config.addIriClass(new UserIriClass("intact_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/intact/[A-Z0-9]{6}"));
        config.addIriClass(new UserIriClass("ensembl_gene_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/ensembl/ENSG[0-9]{11}"));
        config.addIriClass(new UserIriClass("pharmgkb_reference", Arrays.asList("varchar"),
                "http://www\\.pharmgkb\\.org/gene/PA[1-9][0-9]*"));
        config.addIriClass(new UserIriClass("timbal_reference", Arrays.asList("varchar"),
                "http://mordred\\.bioc\\.cam\\.ac\\.uk/timbal/[A-Za-z0-9%()-]+"));
        config.addIriClass(new UserIriClass("cgd_reference", Arrays.asList("varchar"),
                "http://research\\.nhgri\\.nih\\.gov/CGD/view/\\?g=[A-Z0-9-]+"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass component = config.getIriClass("targetcomponent");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "component_references";
        NodeMapping subject = config.createIriMapping(component, "component_id");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("go_process_reference", "reference"), "reference_type = 'GO PROCESS'");

        config.addQuadMapping(table, graph, config.createIriMapping("go_process_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' GO Function Process: ' || reference)"),
                "reference_type = 'GO PROCESS'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("go_process_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:GoProcessRef"), "reference_type = 'GO PROCESS'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("go_function_reference", "reference"), "reference_type = 'GO FUNCTION'");

        config.addQuadMapping(table, graph, config.createIriMapping("go_function_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' GO Function Reference: ' || reference)"),
                "reference_type = 'GO FUNCTION'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("go_function_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:GoFunctionRef"), "reference_type = 'GO FUNCTION'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("go_component_reference", "reference"), "reference_type = 'GO COMPONENT'");

        config.addQuadMapping(table, graph, config.createIriMapping("go_component_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' GO Component Reference: ' || reference)"),
                "reference_type = 'GO COMPONENT'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("go_component_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:GoComponentRef"), "reference_type = 'GO COMPONENT'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("pdb_reference", "reference"), "reference_type = 'PDB'");

        config.addQuadMapping(table, graph, config.createIriMapping("pdb_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' PDBe Reference: ' || reference)"),
                "reference_type = 'PDB'");

        config.addQuadMapping("component_reference_types", graph, config.createIriMapping("pdb_reference", "reference"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:ProteinDataBankRef"),
                "reference_type = 'PDB'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("interpro_reference", "reference"), "reference_type = 'INTERPRO'");

        config.addQuadMapping(table, graph, config.createIriMapping("interpro_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' InterPro Reference: ' || reference)"),
                "reference_type = 'INTERPRO'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("interpro_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:InterproRef"), "reference_type = 'INTERPRO'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("reactome_reference", "reference"), "reference_type = 'REACTOME'");

        config.addQuadMapping(table, graph, config.createIriMapping("reactome_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' Reactome Reference: ' || reference)"),
                "reference_type = 'REACTOME'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("reactome_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ReactomeRef"), "reference_type = 'REACTOME'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("pfam_reference", "reference"), "reference_type = 'PFAM'");

        config.addQuadMapping(table, graph, config.createIriMapping("pfam_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' Pfam Reference: ' || reference)"),
                "reference_type = 'PFAM'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("pfam_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:PfamRef"), "reference_type = 'PFAM'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("enzyme_class_reference", "reference"), "reference_type = 'ENZYME CLASS'");

        config.addQuadMapping(table, graph, config.createIriMapping("enzyme_class_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' EC Reference: ' || reference)"),
                "reference_type = 'ENZYME CLASS'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("enzyme_class_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:EnzymeClassRef"), "reference_type = 'ENZYME CLASS'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("intact_reference", "reference"), "reference_type = 'INTACT'");

        config.addQuadMapping(table, graph, config.createIriMapping("intact_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' IntAct Reference: ' || reference)"),
                "reference_type = 'INTACT'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("intact_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:IntactRef"), "reference_type = 'INTACT'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("ensembl_gene_reference", "reference"), "reference_type = 'ENSEMBL GENE'");

        config.addQuadMapping(table, graph, config.createIriMapping("ensembl_gene_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' ENSEMBL Reference: ' || reference)"),
                "reference_type = 'ENSEMBL GENE'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("ensembl_gene_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:EnsemblGeneRef"), "reference_type = 'ENSEMBL GENE'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("uniprot", "reference"), "reference_type = 'UNIPROT'");

        config.addQuadMapping(table, graph, config.createIriMapping("uniprot", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' UniProt Reference: ' || reference)"),
                "reference_type = 'UNIPROT'");

        config.addQuadMapping("component_reference_types", graph, config.createIriMapping("uniprot", "reference"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:UniprotRef"),
                "reference_type = 'UNIPROT'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("pharmgkb_reference", "reference"), "reference_type = 'PHARMGKB'");

        config.addQuadMapping(table, graph, config.createIriMapping("pharmgkb_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' PharmGKB Reference: ' || reference)"),
                "reference_type = 'PHARMGKB'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("pharmgkb_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:PharmgkbRef"), "reference_type = 'PHARMGKB'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("timbal_reference", "reference"), "reference_type = 'TIMBAL'");

        config.addQuadMapping(table, graph, config.createIriMapping("timbal_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' TIMBAL Reference: ' || reference)"),
                "reference_type = 'TIMBAL'");

        config.addQuadMapping("component_reference_types", graph,
                config.createIriMapping("timbal_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:TimbalRef"), "reference_type = 'TIMBAL'");


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("cgd_reference", "reference"), "reference_type = 'CGD'");

        config.addQuadMapping(table, graph, config.createIriMapping("cgd_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('CHEMBL_TC_' || component_id || ' CGD: ' || reference)"),
                "reference_type = 'CGD'");

        config.addQuadMapping("component_reference_types", graph, config.createIriMapping("cgd_reference", "reference"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:CGDRef"), "reference_type = 'CGD'");
    }
}
