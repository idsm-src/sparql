package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class TargetComponentReference
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(
                new StringUserIriClass("go_process_reference", "http://identifiers.org/obo.go/", "GO:[0-9]{7}"));

        config.addIriClass(
                new StringUserIriClass("go_function_reference", "http://identifiers.org/obo.go/", "GO:[0-9]{7}"));

        config.addIriClass(
                new StringUserIriClass("go_component_reference", "http://identifiers.org/obo.go/", "GO:[0-9]{7}"));

        config.addIriClass(new StringUserIriClass("pdb_reference", "http://identifiers.org/pdb/", "[0-9][A-Z0-9]{3}"));

        config.addIriClass(
                new StringUserIriClass("interpro_reference", "http://identifiers.org/interpro/", "IPR[0-9]{6}"));

        config.addIriClass(new StringUserIriClass("reactome_reference", "http://identifiers.org/reactome/",
                "R-[A-Z]{3}-[1-9][0-9]*"));

        config.addIriClass(new StringUserIriClass("pfam_reference", "http://identifiers.org/pfam/", "PF[0-9]{5}"));

        config.addIriClass(new StringUserIriClass("enzyme_class_reference", "http://identifiers.org/ec-code/",
                "((-|[1-9][0-9]*)\\.){3}(-|n?[1-9][0-9]*)"));

        config.addIriClass(new StringUserIriClass("intact_reference", "http://identifiers.org/intact/", "[A-Z0-9]{6}"));

        config.addIriClass(
                new StringUserIriClass("ensembl_gene_reference", "http://identifiers.org/ensembl/", "ENSG[0-9]{11}"));

        config.addIriClass(
                new StringUserIriClass("pharmgkb_reference", "http://www.pharmgkb.org/gene/", "PA[1-9][0-9]*"));

        config.addIriClass(new StringUserIriClass("timbal_reference", "http://mordred.bioc.cam.ac.uk/timbal/",
                "[A-Za-z0-9%()-]+"));

        config.addIriClass(
                new StringUserIriClass("cgd_reference", "http://research.nhgri.nih.gov/CGD/view/?g=", "[A-Z0-9-]+"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass component = config.getIriClass("targetcomponent");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "component_references";
        NodeMapping subject = config.createIriMapping(component, "component_id");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("go_process_reference", "reference"), "reference_type = 'GO PROCESS'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("go_process_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' GO Function Process: ' || reference)"),
                "reference_type = 'GO PROCESS'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("go_process_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'GO PROCESS'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("go_process_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:GoProcessRef"), "reference_type = 'GO PROCESS'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("go_function_reference", "reference"), "reference_type = 'GO FUNCTION'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("go_function_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' GO Function Reference: ' || reference)"),
                "reference_type = 'GO FUNCTION'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("go_function_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'GO FUNCTION'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("go_function_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:GoFunctionRef"), "reference_type = 'GO FUNCTION'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("go_component_reference", "reference"), "reference_type = 'GO COMPONENT'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("go_component_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' GO Component Reference: ' || reference)"),
                "reference_type = 'GO COMPONENT'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("go_component_reference", "reference"),
                config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                "reference_type = 'GO COMPONENT'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("go_component_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:GoComponentRef"), "reference_type = 'GO COMPONENT'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("pdb_reference", "reference"), "reference_type = 'PDB'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pdb_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' PDBe Reference: ' || reference)"),
                "reference_type = 'PDB'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("pdb_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'PDB'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("pdb_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ProteinDataBankRef"), "reference_type = 'PDB'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("interpro_reference", "reference"), "reference_type = 'INTERPRO'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("interpro_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' InterPro Reference: ' || reference)"),
                "reference_type = 'INTERPRO'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("interpro_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'INTERPRO'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("interpro_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:InterproRef"), "reference_type = 'INTERPRO'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("reactome_reference", "reference"), "reference_type = 'REACTOME'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("reactome_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' Reactome Reference: ' || reference)"),
                "reference_type = 'REACTOME'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("reactome_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'REACTOME'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("reactome_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ReactomeRef"), "reference_type = 'REACTOME'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("pfam_reference", "reference"), "reference_type = 'PFAM'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pfam_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' Pfam Reference: ' || reference)"),
                "reference_type = 'PFAM'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("pfam_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'PFAM'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("pfam_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:PfamRef"), "reference_type = 'PFAM'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("enzyme_class_reference", "reference"), "reference_type = 'ENZYME CLASS'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("enzyme_class_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' EC Reference: ' || reference)"),
                "reference_type = 'ENZYME CLASS'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("enzyme_class_reference", "reference"),
                config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                "reference_type = 'ENZYME CLASS'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("enzyme_class_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:EnzymeClassRef"), "reference_type = 'ENZYME CLASS'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("intact_reference", "reference"), "reference_type = 'INTACT'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("intact_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' IntAct Reference: ' || reference)"),
                "reference_type = 'INTACT'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("intact_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'INTACT'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("intact_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:IntactRef"), "reference_type = 'INTACT'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("ensembl_gene_reference", "reference"), "reference_type = 'ENSEMBL GENE'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("ensembl_gene_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' ENSEMBL Reference: ' || reference)"),
                "reference_type = 'ENSEMBL GENE'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("ensembl_gene_reference", "reference"),
                config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                "reference_type = 'ENSEMBL GENE'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("ensembl_gene_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:EnsemblGeneRef"), "reference_type = 'ENSEMBL GENE'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("uniprot", "reference"), "reference_type = 'UNIPROT'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("uniprot", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' UniProt Reference: ' || reference)"),
                "reference_type = 'UNIPROT'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("uniprot", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'UNIPROT'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("uniprot", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:UniprotRef"), "reference_type = 'UNIPROT'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("pharmgkb_reference", "reference"), "reference_type = 'PHARMGKB'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pharmgkb_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' PharmGKB Reference: ' || reference)"),
                "reference_type = 'PHARMGKB'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("pharmgkb_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'PHARMGKB'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("pharmgkb_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:PharmgkbRef"), "reference_type = 'PHARMGKB'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("timbal_reference", "reference"), "reference_type = 'TIMBAL'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("timbal_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "('CHEMBL_TC_' || component_id || ' TIMBAL Reference: ' || reference)"),
                "reference_type = 'TIMBAL'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("timbal_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'TIMBAL'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("timbal_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:TimbalRef"), "reference_type = 'TIMBAL'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                config.createIriMapping("cgd_reference", "reference"), "reference_type = 'CGD'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("cgd_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('CHEMBL_TC_' || component_id || ' CGD: ' || reference)"),
                "reference_type = 'CGD'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("cgd_reference", "reference"), config.createIriMapping("dc:identifier"),
                config.createLiteralMapping(xsdString, "reference"), "reference_type = 'CGD'");

        config.addQuadMapping(schema, "component_reference_types", graph,
                config.createIriMapping("cgd_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:CGDRef"), "reference_type = 'CGD'");
    }
}
