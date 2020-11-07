package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;



public class TargetComponentReference
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        {
            Table table = new Table(schema, "component_references");
            NodeMapping subject = config.createIriMapping("chembl:targetcomponent", "component_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:obo.go", "reference"), "reference_type = 'GO PROCESS'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' GO Function Process: ' || reference)"),
                    "reference_type = 'GO PROCESS'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:obo.go", "reference"), "reference_type = 'GO FUNCTION'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' GO Function Reference: ' || reference)"),
                    "reference_type = 'GO FUNCTION'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:obo.go", "reference"), "reference_type = 'GO COMPONENT'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' GO Component Reference: ' || reference)"),
                    "reference_type = 'GO COMPONENT'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:pdb", "reference"), "reference_type = 'PDB'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pdb", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' PDBe Reference: ' || reference)"),
                    "reference_type = 'PDB'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:interpro", "reference"), "reference_type = 'INTERPRO'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:interpro", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' InterPro Reference: ' || reference)"),
                    "reference_type = 'INTERPRO'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:reactome", "reference"), "reference_type = 'REACTOME'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:reactome", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' Reactome Reference: ' || reference)"),
                    "reference_type = 'REACTOME'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:pfam", "reference"), "reference_type = 'PFAM'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pfam", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' Pfam Reference: ' || reference)"),
                    "reference_type = 'PFAM'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:ec-code", "reference"), "reference_type = 'ENZYME CLASS'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ec-code", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' EC Reference: ' || reference)"),
                    "reference_type = 'ENZYME CLASS'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:intact", "reference"), "reference_type = 'INTACT'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:intact", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' IntAct Reference: ' || reference)"),
                    "reference_type = 'INTACT'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:ensembl", "reference"), "reference_type = 'ENSEMBL GENE'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ensembl", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' ENSEMBL Reference: ' || reference)"),
                    "reference_type = 'ENSEMBL GENE'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("purl:uniprot", "reference"), "reference_type = 'UNIPROT'");
            config.addQuadMapping(table, graph, config.createIriMapping("purl:uniprot", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' UniProt Reference: ' || reference)"),
                    "reference_type = 'UNIPROT'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("reference:pharmgkb-gene", "reference"), "reference_type = 'PHARMGKB'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pharmgkb-gene", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' PharmGKB Reference: ' || reference)"),
                    "reference_type = 'PHARMGKB'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("reference:timbal", "reference"), "reference_type = 'TIMBAL'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:timbal", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' TIMBAL Reference: ' || reference)"),
                    "reference_type = 'TIMBAL'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("reference:cgd", "reference"), "reference_type = 'CGD'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:cgd", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "('CHEMBL_TC_' || component_id || ' CGD: ' || reference)"),
                    "reference_type = 'CGD'");

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("pdbo:link_to_pdb"),
                    config.createIriMapping("rdf:wwpdb", "reference"), "reference_type = 'PDB'");
        }

        {
            Table table = new Table(schema, "component_reference_types");

            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:GoProcessRef"),
                    "reference_type = 'GO PROCESS'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:GoFunctionRef"),
                    "reference_type = 'GO FUNCTION'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:GoComponentRef"),
                    "reference_type = 'GO COMPONENT'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pdb", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ProteinDataBankRef"),
                    "reference_type = 'PDB'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:interpro", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:InterproRef"),
                    "reference_type = 'INTERPRO'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:reactome", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ReactomeRef"),
                    "reference_type = 'REACTOME'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pfam", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PfamRef"),
                    "reference_type = 'PFAM'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ec-code", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:EnzymeClassRef"),
                    "reference_type = 'ENZYME CLASS'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:intact", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:IntactRef"),
                    "reference_type = 'INTACT'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ensembl", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:EnsemblGeneRef"),
                    "reference_type = 'ENSEMBL GENE'");
            config.addQuadMapping(table, graph, config.createIriMapping("purl:uniprot", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:UniprotRef"),
                    "reference_type = 'UNIPROT'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pharmgkb-gene", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PharmgkbRef"),
                    "reference_type = 'PHARMGKB'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:timbal", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:TimbalRef"),
                    "reference_type = 'TIMBAL'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:cgd", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:CGDRef"),
                    "reference_type = 'CGD'");
        }
    }
}
