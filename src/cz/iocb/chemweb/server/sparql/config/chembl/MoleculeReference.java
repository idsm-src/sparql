package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class MoleculeReference
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass(schema, "pubchem_compound_reference", Arrays.asList("varchar"),
                "http://pubchem\\.ncbi\\.nlm\\.nih\\.gov/compound/[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "pubchem_substance_reference", Arrays.asList("varchar"),
                "http://pubchem\\.ncbi\\.nlm\\.nih\\.gov/substance/[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "zinc_reference", Arrays.asList("varchar"),
                "http://zinc15\\.docking\\.org/substances/ZINC[0-9]{12}"));
        config.addIriClass(new UserIriClass(schema, "sure_chembl_reference", Arrays.asList("varchar"),
                "https://www\\.surechembl\\.org/chemical/SCHEMBL[0-9]+"));
        config.addIriClass(new UserIriClass(schema, "molport_reference", Arrays.asList("varchar"),
                "https://www\\.molport\\.com/shop/molecule-link/MolPort(-[0-9]{3}){3}"));
        config.addIriClass(new UserIriClass(schema, "emolecules_reference", Arrays.asList("varchar"),
                "https://www\\.emolecules\\.com/cgi-bin/more\\?vid=[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "mcule_reference", Arrays.asList("varchar"),
                "https://mcule\\.com/MCULE-[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "ibm_patent_structure_reference", Arrays.asList("varchar"),
                "http://www-935\\.ibm\\.com/services/us/gbs/bao/siip/nih/\\?sid=[A-F0-9]+"));
        config.addIriClass(new UserIriClass(schema, "nikkaji_reference", Arrays.asList("varchar"),
                "http://jglobal\\.jst\\.go\\.jp/en/redirect\\?Nikkaji_No=[A-Z0-9.]+"));
        config.addIriClass(new UserIriClass(schema, "actor_reference", Arrays.asList("varchar"),
                "http://actor\\.epa\\.gov/actor/chemical\\.xhtml\\?casrn=[1-9][0-9]*-[0-9]{2}-[0-9]"));
        config.addIriClass(new UserIriClass(schema, "chebi_reference", Arrays.asList("varchar"),
                "http://www\\.ebi\\.ac\\.uk/chebi/searchId\\.do\\?chebiId=CHEBI%3A[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "pdbe_reference", Arrays.asList("varchar"),
                "http://www\\.ebi\\.ac\\.uk/pdbe-srv/pdbechem/chemicalCompound/show/[A-Z0-9]{1,3}"));
        config.addIriClass(new UserIriClass(schema, "nmr_shift_db2_reference", Arrays.asList("varchar"),
                "http://nmrshiftdb\\.org/molecule/[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "kegg_ligand_reference", Arrays.asList("varchar"),
                "http://www\\.genome\\.jp/dbget-bin/www_bget\\?C[0-9]{5}"));
        config.addIriClass(new UserIriClass(schema, "drugbank_reference", Arrays.asList("varchar"),
                "http://www\\.drugbank\\.ca/drugs/DB[0-9]{5}"));
        config.addIriClass(new UserIriClass(schema, "hmdb_reference", Arrays.asList("varchar"),
                "http://www\\.hmdb\\.ca/metabolites/HMDB[0-9]{7}"));
        config.addIriClass(new UserIriClass(schema, "iuphar_reference", Arrays.asList("varchar"),
                "http://www\\.guidetopharmacology\\.org/GRAC/LigandDisplayForward\\?ligandId=[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "selleck_reference", Arrays.asList("varchar"),
                "http://www\\.selleckchem\\.com/products/[^/]*\\.html"));
        config.addIriClass(new UserIriClass(schema, "pharm_gkb_reference", Arrays.asList("varchar"),
                "http://www\\.pharmgkb\\.org/drug/PA[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "atlas_reference", Arrays.asList("varchar"),
                "http://www\\.ebi\\.ac\\.uk/gxa/query\\?conditionQuery=.+"));
        config.addIriClass(new UserIriClass(schema, "recon_reference", Arrays.asList("varchar"),
                "http://vmh\\.uni\\.lu/#metabolite/[^/]+"));
        config.addIriClass(new UserIriClass(schema, "wikipedia_mol_reference", Arrays.asList("varchar"),
                "http://en\\.wikipedia\\.org/wiki/.+"));
        config.addIriClass(new UserIriClass(schema, "lincs_reference", Arrays.asList("varchar"),
                "http://identifiers\\.org/lincs\\.smallmolecule/LSM-[1-9][0-9]*"));
        config.addIriClass(new UserIriClass(schema, "fda_srs_reference", Arrays.asList("varchar"),
                "http://fdasis\\.nlm\\.nih\\.gov/srs/ProxyServlet\\?mergeData=true&objectHandle=DBMaint&APPLICATION_NAME=fdasrs&actionHandle=default&nextPage=jsp/srs/ResultScreen\\.jsp&TXTSUPERLISTID=[A-Z0-9]{10}"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass molecule = config.getIriClass("molecule");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "molecule_references";
        NodeMapping subject = config.createIriMapping(molecule, "molregno");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("pubchem_compound_reference", "reference"), "reference_type = 'PUBCHEM'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pubchem_compound_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' PubChem Reference: ' || reference)"),
                "reference_type = 'PUBCHEM'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("pubchem_compound_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:PubchemRef"), "reference_type = 'PUBCHEM'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("pubchem_substance_reference", "reference"),
                "reference_type = 'PUBCHEM THOM PHARM'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pubchem_substance_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || "
                                + "' PubChem Thomson Pharma Subset Reference: ' || reference)"),
                "reference_type = 'PUBCHEM THOM PHARM'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("pubchem_substance_reference", "reference"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:PubchemThomPharmRef"),
                "reference_type = 'PUBCHEM THOM PHARM'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("pubchem_substance_reference", "reference"), "reference_type = 'PUBCHEM DOTF'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pubchem_substance_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || "
                                + "' PubChem Drugs of the Future Subset Reference: ' || reference)"),
                "reference_type = 'PUBCHEM DOTF'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("pubchem_substance_reference", "reference"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:PubchemDotfRef"),
                "reference_type = 'PUBCHEM DOTF'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("zinc_reference", "reference"), "reference_type = 'ZINC'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("zinc_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' Zinc Reference: ' || reference)"),
                "reference_type = 'ZINC'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("zinc_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ZincRef"), "reference_type = 'ZINC'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("sure_chembl_reference", "reference"), "reference_type = 'SURE CHEMBL'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("sure_chembl_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' SureChEMBL Reference: ' || reference)"),
                "reference_type = 'SURE CHEMBL'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("sure_chembl_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:SureChemblRef"), "reference_type = 'SURE CHEMBL'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("molport_reference", "reference"), "reference_type = 'MOLPORT'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("molport_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' MolPort Reference: ' || reference)"),
                "reference_type = 'MOLPORT'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("molport_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:MolportRef"), "reference_type = 'MOLPORT'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("emolecules_reference", "reference"), "reference_type = 'EMOLECULES'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("emolecules_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' eMolecules Reference: ' || reference)"),
                "reference_type = 'EMOLECULES'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("emolecules_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:EmoleculesRef"), "reference_type = 'EMOLECULES'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("mcule_reference", "reference"), "reference_type = 'MCULE'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("mcule_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' Mcule Reference: ' || reference)"),
                "reference_type = 'MCULE'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("mcule_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:MculeRef"), "reference_type = 'MCULE'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("ibm_patent_structure_reference", "reference"),
                "reference_type = 'IBM PATENT STRUCTURE'");

        config.addQuadMapping(schema, table, graph,
                config.createIriMapping("ibm_patent_structure_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || "
                                + "' IBM Patent Structure Reference: ' || reference)"),
                "reference_type = 'IBM PATENT STRUCTURE'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("ibm_patent_structure_reference", "reference"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cco:IbmPatentStructureRef"),
                "reference_type = 'IBM PATENT STRUCTURE'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("nikkaji_reference", "reference"), "reference_type = 'NIKKAJI'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("nikkaji_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' Nikkaji Reference: ' || reference)"),
                "reference_type = 'NIKKAJI'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("nikkaji_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:NikkajiRef"), "reference_type = 'NIKKAJI'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("actor_reference", "reference"), "reference_type = 'ACTOR'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("actor_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' ACToR Reference: ' || reference)"),
                "reference_type = 'ACTOR'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("actor_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ActorRef"), "reference_type = 'ACTOR'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("chebi_reference", "reference"), "reference_type = 'CHEBI'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("chebi_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' ChEBI Reference: ' || reference)"),
                "reference_type = 'CHEBI'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("chebi_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ChebiRef"), "reference_type = 'CHEBI'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("pdbe_reference", "reference"), "reference_type = 'PDBE'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pdbe_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' PDBe Reference: ' || reference)"),
                "reference_type = 'PDBE'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("pdbe_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:PdbeRef"), "reference_type = 'PDBE'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("nmr_shift_db2_reference", "reference"), "reference_type = 'NMR SHIFT DB2'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("nmr_shift_db2_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' NMRShiftDB Reference: ' || reference)"),
                "reference_type = 'NMR SHIFT DB2'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("nmr_shift_db2_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:NmrShiftDb2Ref"), "reference_type = 'NMR SHIFT DB2'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("kegg_ligand_reference", "reference"), "reference_type = 'KEGG LIGAND'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("kegg_ligand_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' Kegg Ligand Reference: ' || reference)"),
                "reference_type = 'KEGG LIGAND'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("kegg_ligand_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:KeggLigandRef"), "reference_type = 'KEGG LIGAND'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("drugbank_reference", "reference"), "reference_type = 'DRUGBANK'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("drugbank_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' DrugBank Reference: ' || reference)"),
                "reference_type = 'DRUGBANK'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("drugbank_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:DrugbankRef"), "reference_type = 'DRUGBANK'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("hmdb_reference", "reference"), "reference_type = 'HMDB'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("hmdb_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || "
                                + "' The Human Metabolome Database Reference: ' || reference)"),
                "reference_type = 'HMDB'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("hmdb_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:HmdbRef"), "reference_type = 'HMDB'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("iuphar_reference", "reference"), "reference_type = 'IUPHAR'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("iuphar_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' IUPHAR Reference: ' || reference)"),
                "reference_type = 'IUPHAR'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("iuphar_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:IupharRef"), "reference_type = 'IUPHAR'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("selleck_reference", "reference"), "reference_type = 'SELLECK'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("selleck_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || "
                                + "' Selleck Chemicals Reference: ' || url_decode(reference))"),
                "reference_type = 'SELLECK'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("selleck_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:SelleckRef"), "reference_type = 'SELLECK'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("pharm_gkb_reference", "reference"), "reference_type = 'PHARM GKB'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("pharm_gkb_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' PharmGKB Reference: ' || reference)"),
                "reference_type = 'PHARM GKB'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("pharm_gkb_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:PharmGkbRef"), "reference_type = 'PHARM GKB'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("atlas_reference", "reference"), "reference_type = 'ATLAS'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("atlas_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || "
                                + "' Gene Expression Atlas Reference: ' || url_decode(reference))"),
                "reference_type = 'ATLAS'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("atlas_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:AtlasRef"), "reference_type = 'ATLAS'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("recon_reference", "reference"), "reference_type = 'RECON'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("recon_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' Recon Reference: ' || reference)"),
                "reference_type = 'RECON'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("recon_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ReconRef"), "reference_type = 'RECON'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("wikipedia_mol_reference", "reference"), "reference_type = 'WIKIPEDIA MOL'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("wikipedia_mol_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' Wikipedia Reference: ' || reference)"),
                "reference_type = 'WIKIPEDIA MOL'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("wikipedia_mol_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:WikipediaMolRef"), "reference_type = 'WIKIPEDIA MOL'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("lincs_reference", "reference"), "reference_type = 'LINCS'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("lincs_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "((select chembl_id from " + schema
                        + ".molecule_dictionary where molregno = " + schema + ".molecule_references.molregno) || "
                        + "' Library of Integrated Network-based Cellular Signatures Reference: ' || reference)"),
                "reference_type = 'LINCS'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("lincs_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:LincsRef"), "reference_type = 'LINCS'");


        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:moleculeXref"),
                config.createIriMapping("fda_srs_reference", "reference"), "reference_type = 'FDA SRS'");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("fda_srs_reference", "reference"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from " + schema + ".molecule_dictionary where molregno = " + schema
                                + ".molecule_references.molregno) || ' FDA/USP SRS Reference: ' || reference)"),
                "reference_type = 'FDA SRS'");

        config.addQuadMapping(schema, "molecule_reference_types", graph,
                config.createIriMapping("fda_srs_reference", "reference"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:FdaSrsRef"), "reference_type = 'FDA SRS'");
    }
}
