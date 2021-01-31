package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;



public class MoleculeReference
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        {
            Table table = new Table(schema, "molecule_pubchem_references");
            NodeMapping subject = config.createIriMapping("chembl:compound", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("pubchem:compound", "compound_id"));
            config.addQuadMapping(table, graph, config.createIriMapping("pubchem:compound", "compound_id"),
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' PubChem Reference: ' || compound_id)"));
            config.addQuadMapping(table, graph, config.createIriMapping("pubchem:compound", "compound_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PubchemRef"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("pubchem:compound", "compound_id"));
        }

        {
            Table table = new Table(schema, "molecule_pubchem_thom_pharm_references");
            NodeMapping subject = config.createIriMapping("chembl:compound", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("pubchem:substance", "substance_id"));
            config.addQuadMapping(table, graph, config.createIriMapping("pubchem:substance", "substance_id"),
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' PubChem Thomson Pharma Subset Reference: ' || substance_id)"));
            config.addQuadMapping(table, graph, config.createIriMapping("pubchem:substance", "substance_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PubchemThomPharmRef"));
        }

        {
            Table table = new Table(schema, "molecule_pubchem_dotf_references");
            NodeMapping subject = config.createIriMapping("chembl:compound", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("pubchem:substance", "substance_id"));
            config.addQuadMapping(table, graph, config.createIriMapping("pubchem:substance", "substance_id"),
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' PubChem Drugs of the Future Subset Reference: ' || substance_id)"));
            config.addQuadMapping(table, graph, config.createIriMapping("pubchem:substance", "substance_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PubchemDotfRef"));
        }

        {
            Table table = new Table(schema, "molecule_chebi_references");
            NodeMapping subject = config.createIriMapping("chembl:compound", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:chebi", "chebi_id"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:chebi", "chebi_id"),
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' ChEBI Reference: ' || chebi_id)"));
            config.addQuadMapping(table, graph, config.createIriMapping("reference:chebi", "chebi_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ChebiRef"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology:resource", Ontology.unitCHEBI, "chebi_id"));
        }

        {
            Table table = new Table(schema, "molecule_references");
            NodeMapping subject = config.createIriMapping("chembl:compound", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:zinc", "reference"), "reference_type = 'ZINC'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:zinc", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Zinc Reference: ' || reference)"),
                    "reference_type = 'ZINC'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:surechembl", "reference"), "reference_type = 'SURE CHEMBL'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:surechembl", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' SureChEMBL Reference: ' || reference)"),
                    "reference_type = 'SURE CHEMBL'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:molport", "reference"), "reference_type = 'MOLPORT'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:molport", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' MolPort Reference: ' || reference)"),
                    "reference_type = 'MOLPORT'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:emolecules", "reference"), "reference_type = 'EMOLECULES'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:emolecules", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' eMolecules Reference: ' || reference)"),
                    "reference_type = 'EMOLECULES'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:mcule", "reference"), "reference_type = 'MCULE'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:mcule", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Mcule Reference: ' || reference)"),
                    "reference_type = 'MCULE'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:ibm_patent_structure", "reference"),
                    "reference_type = 'IBM PATENT STRUCTURE'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:ibm_patent_structure", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' IBM Patent Structure Reference: ' || reference)"),
                    "reference_type = 'IBM PATENT STRUCTURE'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:nikkaji", "reference"), "reference_type = 'NIKKAJI'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:nikkaji", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Nikkaji Reference: ' || reference)"),
                    "reference_type = 'NIKKAJI'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:actor", "reference"), "reference_type = 'ACTOR'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:actor", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' ACToR Reference: ' || reference)"),
                    "reference_type = 'ACTOR'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:pdbe", "reference"), "reference_type = 'PDBE'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pdbe", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' PDBe Reference: ' || reference)"),
                    "reference_type = 'PDBE'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:nmrshiftdb2", "reference"), "reference_type = 'NMR SHIFT DB2'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:nmrshiftdb2", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' NMRShiftDB Reference: ' || reference)"),
                    "reference_type = 'NMR SHIFT DB2'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:kegg", "reference"), "reference_type = 'KEGG LIGAND'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:kegg", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Kegg Ligand Reference: ' || reference)"),
                    "reference_type = 'KEGG LIGAND'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:drugbank", "reference"), "reference_type = 'DRUGBANK'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:drugbank", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' DrugBank Reference: ' || reference)"),
                    "reference_type = 'DRUGBANK'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:hmdb", "reference"), "reference_type = 'HMDB'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:hmdb", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' The Human Metabolome Database Reference: ' || reference)"),
                    "reference_type = 'HMDB'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:iuphar", "reference"), "reference_type = 'IUPHAR'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:iuphar", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' IUPHAR Reference: ' || reference)"),
                    "reference_type = 'IUPHAR'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:selleck", "reference"), "reference_type = 'SELLECK'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:selleck", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Selleck Chemicals Reference: ' || chembl.url_decode(reference))"),
                    "reference_type = 'SELLECK'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:pharmgkb-drug", "reference"), "reference_type = 'PHARM GKB'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pharmgkb-drug", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' PharmGKB Reference: ' || reference)"),
                    "reference_type = 'PHARM GKB'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:expression_atlas", "reference"), "reference_type = 'ATLAS'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:expression_atlas", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Gene Expression Atlas Reference: ' || chembl.url_decode(reference))"),
                    "reference_type = 'ATLAS'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:recon", "reference"), "reference_type = 'RECON'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:recon", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Recon Reference: ' || reference)"),
                    "reference_type = 'RECON'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:wikipedia", "reference"), "reference_type = 'WIKIPEDIA MOL'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:wikipedia", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Wikipedia Reference: ' || reference)"),
                    "reference_type = 'WIKIPEDIA MOL'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("identifiers:lincs.smallmolecule", "reference"),
                    "reference_type = 'LINCS'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:lincs.smallmolecule", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Library of Integrated Network-based Cellular Signatures Reference: ' || reference)"),
                    "reference_type = 'LINCS'");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                    config.createIriMapping("reference:fda_srs", "reference"), "reference_type = 'FDA SRS'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:fda_srs", "reference"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' FDA/USP SRS Reference: ' || reference)"),
                    "reference_type = 'FDA SRS'");
        }

        {
            Table table = new Table(schema, "molecule_reference_types");

            config.addQuadMapping(table, graph, config.createIriMapping("reference:zinc", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ZincRef"),
                    "reference_type = 'ZINC'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:surechembl", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:SureChemblRef"),
                    "reference_type = 'SURE CHEMBL'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:molport", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:MolportRef"),
                    "reference_type = 'MOLPORT'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:emolecules", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:EmoleculesRef"),
                    "reference_type = 'EMOLECULES'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:mcule", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:MculeRef"),
                    "reference_type = 'MCULE'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:ibm_patent_structure", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:IbmPatentStructureRef"),
                    "reference_type = 'IBM PATENT STRUCTURE'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:nikkaji", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:NikkajiRef"),
                    "reference_type = 'NIKKAJI'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:actor", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ActorRef"),
                    "reference_type = 'ACTOR'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pdbe", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PdbeRef"),
                    "reference_type = 'PDBE'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:nmrshiftdb2", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:NmrShiftDb2Ref"),
                    "reference_type = 'NMR SHIFT DB2'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:kegg", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:KeggLigandRef"),
                    "reference_type = 'KEGG LIGAND'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:drugbank", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:DrugbankRef"),
                    "reference_type = 'DRUGBANK'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:hmdb", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:HmdbRef"),
                    "reference_type = 'HMDB'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:iuphar", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:IupharRef"),
                    "reference_type = 'IUPHAR'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:selleck", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:SelleckRef"),
                    "reference_type = 'SELLECK'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:pharmgkb-drug", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:PharmGkbRef"),
                    "reference_type = 'PHARM GKB'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:expression_atlas", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:AtlasRef"),
                    "reference_type = 'ATLAS'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:recon", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:ReconRef"),
                    "reference_type = 'RECON'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:wikipedia", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:WikipediaMolRef"),
                    "reference_type = 'WIKIPEDIA MOL'");
            config.addQuadMapping(table, graph, config.createIriMapping("identifiers:lincs.smallmolecule", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:LincsRef"),
                    "reference_type = 'LINCS'");
            config.addQuadMapping(table, graph, config.createIriMapping("reference:fda_srs", "reference"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cco:FdaSrsRef"),
                    "reference_type = 'FDA SRS'");
        }
    }
}
