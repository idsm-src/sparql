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


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        {
            Table table = new Table(schema, "component_references");
            NodeMapping subject = config.createIriMapping("chembl:targetcomponent", "component_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createAreEqualCondition("reference_type", "'GO PROCESS'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' GO Function Process: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'GO PROCESS'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createAreEqualCondition("reference_type", "'GO FUNCTION'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' GO Function Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'GO FUNCTION'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createAreEqualCondition("reference_type", "'GO COMPONENT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' GO Component Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'GO COMPONENT'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:pdb", "reference"),
                    config.createAreEqualCondition("reference_type", "'PDB'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pdb", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' PDBe Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'PDB'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:interpro", "reference"),
                    config.createAreEqualCondition("reference_type", "'INTERPRO'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:interpro", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' InterPro Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'INTERPRO'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:reactome", "reference"),
                    config.createAreEqualCondition("reference_type", "'REACTOME'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:reactome", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' Reactome Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'REACTOME'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:pfam", "reference"),
                    config.createAreEqualCondition("reference_type", "'PFAM'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pfam", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' Pfam Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'PFAM'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:ec-code", "reference"),
                    config.createAreEqualCondition("reference_type", "'ENZYME CLASS'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ec-code", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' EC Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'ENZYME CLASS'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:intact", "reference"),
                    config.createAreEqualCondition("reference_type", "'INTACT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:intact", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' IntAct Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'INTACT'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("identifiers:ensembl", "reference"),
                    config.createAreEqualCondition("reference_type", "'ENSEMBL GENE'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ensembl", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' ENSEMBL Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'ENSEMBL GENE'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("purl:uniprot", "reference"),
                    config.createAreEqualCondition("reference_type", "'UNIPROT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("purl:uniprot", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' UniProt Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'UNIPROT'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("reference:pharmgkb-gene", "reference"),
                    config.createAreEqualCondition("reference_type", "'PHARMGKB'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pharmgkb-gene", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' PharmGKB Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'PHARMGKB'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("reference:timbal", "reference"),
                    config.createAreEqualCondition("reference_type", "'TIMBAL'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:timbal", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL_TC_' || component_id || ' TIMBAL Reference: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'TIMBAL'::chembl.component_reference_type"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetCmptXref"),
                    config.createIriMapping("reference:cgd", "reference"),
                    config.createAreEqualCondition("reference_type", "'CGD'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:cgd", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "('CHEMBL_TC_' || component_id || ' CGD: ' || reference)"),
                    config.createAreEqualCondition("reference_type", "'CGD'::chembl.component_reference_type"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("pdbo:link_to_pdb"),
                    config.createIriMapping("rdf:wwpdb", "reference"),
                    config.createAreEqualCondition("reference_type", "'PDB'::chembl.component_reference_type"));
        }

        {
            Table table = new Table(schema, "component_reference_types");

            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:GoProcessRef"),
                    config.createAreEqualCondition("reference_type", "'GO PROCESS'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:GoFunctionRef"),
                    config.createAreEqualCondition("reference_type", "'GO FUNCTION'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:GoComponentRef"),
                    config.createAreEqualCondition("reference_type", "'GO COMPONENT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pdb", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ProteinDataBankRef"),
                    config.createAreEqualCondition("reference_type", "'PDB'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:interpro", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:InterproRef"),
                    config.createAreEqualCondition("reference_type", "'INTERPRO'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:reactome", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ReactomeRef"),
                    config.createAreEqualCondition("reference_type", "'REACTOME'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pfam", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PfamRef"),
                    config.createAreEqualCondition("reference_type", "'PFAM'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ec-code", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:EnzymeClassRef"),
                    config.createAreEqualCondition("reference_type", "'ENZYME CLASS'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:intact", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:IntactRef"),
                    config.createAreEqualCondition("reference_type", "'INTACT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ensembl", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:EnsemblGeneRef"),
                    config.createAreEqualCondition("reference_type", "'ENSEMBL GENE'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("purl:uniprot", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:UniprotRef"),
                    config.createAreEqualCondition("reference_type", "'UNIPROT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pharmgkb-gene", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PharmgkbRef"),
                    config.createAreEqualCondition("reference_type", "'PHARMGKB'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:timbal", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:TimbalRef"),
                    config.createAreEqualCondition("reference_type", "'TIMBAL'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:cgd", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:CGDRef"),
                    config.createAreEqualCondition("reference_type", "'CGD'::chembl.component_reference_type"));

            // extension
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'GO PROCESS'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'GO FUNCTION'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:obo.go", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'GO COMPONENT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pdb", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'PDB'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:interpro", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'INTERPRO'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:reactome", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'REACTOME'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:pfam", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'PFAM'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ec-code", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'ENZYME CLASS'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:intact", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'INTACT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:ensembl", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'ENSEMBL GENE'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("purl:uniprot", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'UNIPROT'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pharmgkb-gene", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'PHARMGKB'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:timbal", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'TIMBAL'::chembl.component_reference_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:cgd", "reference"),
                    config.createIriMapping("dc:identifier"), config.createLiteralMapping(xsdString, "reference"),
                    config.createAreEqualCondition("reference_type", "'CGD'::chembl.component_reference_type"));
        }
    }
}
