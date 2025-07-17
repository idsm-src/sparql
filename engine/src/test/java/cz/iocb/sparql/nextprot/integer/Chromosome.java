package cz.iocb.sparql.nextprot.integer;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.nextprot.integer.NeXtProtIntegerConfiguration.schema;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.MapUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.UserIntBlankNodeClass;



public class Chromosome
{
    private static final BlankNodeClass nameBlankNode = new UserIntBlankNodeClass();
    private static final BlankNodeClass nameListBlankNode = new UserIntBlankNodeClass();


    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("isoform", "integer", new Table(schema, "isoform_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/isoform/", 0));
        config.addIriClass(new MapUserIriClass("annotation", "integer", new Table(schema, "annotation_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/annotation/", 0));
        config.addIriClass(new MapUserIriClass("evidence", "integer", new Table(schema, "evidence_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/evidence/", 0));
        config.addIriClass(new MapUserIriClass("entry", "integer", new Table(schema, "entry_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/entry/", 0));
        config.addIriClass(new MapUserIriClass("proteoform", "integer", new Table(schema, "proteoform_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/proteoform/", 0));
        config.addIriClass(new MapUserIriClass("gene", "integer", new Table(schema, "gene_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/gene/", 0));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        addIsomorfQuadMapping(config);
        addAnnotationQuadMapping(config);
        addEvidenceQuadMapping(config);
        addEntryQuadMapping(config);
        addProteoformQuadMapping(config);
        addGeneQuadMapping(config);
        addProteinSequenceQuadMapping(config);
        addProteinFamilyInfoQuadMapping(config);
        addHistoryQuadMapping(config);
        addIdentifierQuadMapping(config);
        addEntryXrefQuadMapping(config);
        addEvidenceXrefQuadMapping(config);
        addChebiXrefQuadMapping(config);
        addDrugBankXrefQuadMapping(config);
        addUniProtXrefQuadMapping(config);
        addNameQuadMapping(config);
        addNameListQuadMapping(config);
    }


    private static void addIsomorfQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "isoform_bases");
            NodeMapping subject = config.createIriMapping("isoform", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Isoform"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":canonicalIsoform"),
                    config.createLiteralMapping(xsdBoolean, "canonical_isoform"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":swissprotDisplayed"),
                    config.createLiteralMapping(xsdBoolean, "swissprot_displayed"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":absorptionMax"),
                    config.createIriMapping("annotation", "absorption_max"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":absorptionNote"),
                    config.createIriMapping("annotation", "absorption_note"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":allergen"),
                    config.createIriMapping("annotation", "allergen"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":initiatorMethionine"),
                    config.createIriMapping("annotation", "initiator_methionine"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":kineticKM"),
                    config.createIriMapping("annotation", "kinetic_k_m"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":kineticNote"),
                    config.createIriMapping("annotation", "kinetic_note"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":kineticVmax"),
                    config.createIriMapping("annotation", "kinetic_vmax"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":nonConsecutiveResidue"),
                    config.createIriMapping("annotation", "non_consecutive_residue"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":peroxisomeTransitPeptide"),
                    config.createIriMapping("annotation", "peroxisome_transit_peptide"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":pharmaceutical"),
                    config.createIriMapping("annotation", "pharmaceutical"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":phDependence"),
                    config.createIriMapping("annotation", "ph_dependence"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":redoxPotential"),
                    config.createIriMapping("annotation", "redox_potential"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":signalPeptide"),
                    config.createIriMapping("annotation", "signal_peptide"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":temperatureDependence"),
                    config.createIriMapping("annotation", "temperature_dependence"));
        }

        {
            Table table = new Table(schema, "isoform_proteoforms");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":proteoform"),
                    config.createIriMapping("proteoform", "proteoform"));
        }

        {
            Table table = new Table(schema, "isoform_active_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":activeSite"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_activity_regulations");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":activityRegulation"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_antibody_mappings");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":antibodyMapping"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_beta_strands");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":betaStrand"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_binary_interactions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":binaryInteraction"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_binding_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":bindingSite"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_biophysicochemical_properties");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":biophysicochemicalProperty"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_calcium_binding_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":calciumBindingRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_catalytic_activities");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":catalyticActivity"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_cautions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":caution"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_cellular_components");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cellularComponent"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_cleavage_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cleavageSite"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_cofactors");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cofactor"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_cofactor_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cofactorInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_coiled_coil_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":coiledCoilRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_compositionally_biased_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":compositionallyBiasedRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_cross_links");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":crossLink"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_detected_expressions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":detectedExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_developmental_stage_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":developmentalStageInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_diseases");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":disease"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_disulfide_bonds");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":disulfideBond"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_dna_binding_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":dnaBindingRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_domains");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":domain"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_domain_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":domainInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_electrophysiological_parameters");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":electrophysiologicalParameter"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_enzyme_classifications");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":enzymeClassification"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_expressions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":expression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_expression_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":expressionInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_expression_profiles");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":expressionProfile"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_functions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":function"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_function_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":functionInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_general_annotations");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":generalAnnotation"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_glycosylation_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":glycosylationSite"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_go_biological_processs");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":goBiologicalProcess"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_go_cellular_components");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":goCellularComponent"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_go_molecular_functions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":goMolecularFunction"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_helixs");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":helix"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_inductions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":induction"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_interacting_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":interactingRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_interactions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":interaction"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_interaction_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":interactionInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_intramembrane_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":intramembraneRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_keywords");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":keyword"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_lipidation_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":lipidationSite"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_mappings");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":mapping"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_mature_proteins");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":matureProtein"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_medicals");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":medical"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_metal_binding_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":metalBindingSite"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_miscellaneouss");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":miscellaneous"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_miscellaneous_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":miscellaneousRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_miscellaneous_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":miscellaneousSite"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_mitochondrial_transit_peptides");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":mitochondrialTransitPeptide"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_modified_residues");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":modifiedResidue"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_mutagenesiss");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":mutagenesis"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_non_terminal_residues");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":nonTerminalResidue"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_nucleotide_phosphate_binding_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":nucleotidePhosphateBindingRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_pathways");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":pathway"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_pdb_mappings");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":pdbMapping"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_peptide_mappings");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":peptideMapping"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_positional_annotations");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":positionalAnnotation"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_processing_products");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":processingProduct"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_propeptides");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":propeptide"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_ptms");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":ptm"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_ptm_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":ptmInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":region"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_repeats");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":repeat"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_secondary_structures");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":secondaryStructure"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_selenocysteines");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":selenocysteine"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_sequence_cautions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":sequenceCaution"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_sequence_conflicts");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":sequenceConflict"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_short_sequence_motifs");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":shortSequenceMotif"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_sites");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":site"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_small_molecule_interactions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":smallMoleculeInteraction"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_srm_peptide_mappings");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":srmPeptideMapping"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_subcellular_locations");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":subcellularLocation"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_subcellular_location_notes");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":subcellularLocationNote"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_topological_domains");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":topologicalDomain"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_topologies");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":topology"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_transmembrane_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":transmembraneRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_transport_activities");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":transportActivity"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_turns");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":turn"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_undetected_expressions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":undetectedExpression"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_uniprot_keywords");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":uniprotKeyword"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_variants");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":variant"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_variant_infos");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":variantInfo"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_zinc_finger_regions");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":zincFingerRegion"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "isoform_interaction_mappings");
            NodeMapping subject = config.createIriMapping("isoform", "isoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":interactionMapping"),
                    config.createIriMapping("annotation", "annotation"));
        }
    }


    private static void addAnnotationQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "annotation_bases");
            NodeMapping subject = config.createIriMapping("annotation", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("schema", "type"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":quality"),
                    config.createIriMapping("schema", "quality"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":term"),
                    config.createIriMapping("terminology", "term"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":impactedObject"),
                    config.createIriMapping("annotation", "impacted_object"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":entryAnnotationId"),
                    config.createLiteralMapping(xsdString, "entry_annotation"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":variation"),
                    config.createLiteralMapping(xsdString, "variation"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":original"),
                    config.createLiteralMapping(xsdString, "original"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":hgvs"),
                    config.createLiteralMapping(xsdString, "hgvs"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":method"),
                    config.createLiteralMapping(xsdString, "method"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":peptideUnicity"),
                    config.createLiteralMapping(xsdString, "peptide_unicity"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":peptideName"),
                    config.createLiteralMapping(xsdString, "peptide_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":antibodyUnicity"),
                    config.createLiteralMapping(xsdString, "antibody_unicity"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":antibodyName"),
                    config.createLiteralMapping(xsdString, "antibody_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":start"),
                    config.createLiteralMapping(xsdInteger, "position_start"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":end"),
                    config.createLiteralMapping(xsdInteger, "position_end"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":resolution"),
                    config.createLiteralMapping(xsdDouble, "resolution"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":proteotypic"),
                    config.createLiteralMapping(xsdBoolean, "proteotypic"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":selfInteraction"),
                    config.createLiteralMapping(xsdBoolean, "self_interaction"));
        }



        {
            Table table = new Table(schema, "annotation_evidences");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":evidence"),
                    config.createIriMapping("evidence", "evidence"));
        }

        {
            Table table = new Table(schema, "annotation_negative_evidences");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":negativeEvidence"),
                    config.createIriMapping("evidence", "evidence"));
        }

        {
            Table table = new Table(schema, "annotation_diseases");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":disease"),
                    config.createIriMapping("terminology", "disease"));
        }

        {
            Table table = new Table(schema, "annotation_comments");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:comment"),
                    config.createLiteralMapping(xsdString, "comment"));
        }

        {
            Table table = new Table(schema, "annotation_isoform_specificities");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":isoformSpecificity"),
                    config.createIriMapping("schema", "specificity"));
        }

        {
            Table table = new Table(schema, "annotation_entry_interactants");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":interactant"),
                    config.createIriMapping("entry", "interactant"));
        }

        {
            Table table = new Table(schema, "annotation_isoform_interactants");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":interactant"),
                    config.createIriMapping("isoform", "interactant"));
        }

        {
            Table table = new Table(schema, "annotation_peptide_sets");
            NodeMapping subject = config.createIriMapping("annotation", "annotation");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":peptideSet"),
                    config.createLiteralMapping(xsdString, "peptide_set"));
        }
    }


    private static void addEvidenceQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "evidence_bases");
            NodeMapping subject = config.createIriMapping("evidence", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Evidence"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":negative"),
                    config.createLiteralMapping(xsdBoolean, "negative"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":numberOfExperiments"),
                    config.createLiteralMapping(xsdInteger, "number_of_experiments"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":evidenceCode"),
                    config.createIriMapping("terminology", "evidence_code"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":experimentalContext"),
                    config.createIriMapping("context", "experimental_context"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":assignedBy"),
                    config.createIriMapping("source", "assigned_by"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":expressionLevel"),
                    config.createIriMapping("schema", "expression_level"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":fromXref"),
                    config.createIriMapping("database", "from_xref"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":integrationLevel"),
                    config.createIriMapping("schema", "integration_level"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":quality"),
                    config.createIriMapping("schema", "quality"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":isoformSpecificity"),
                    config.createLiteralMapping(xsdString, "isoform_specificity"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":antibodiesAcc"),
                    config.createLiteralMapping(xsdString, "antibodies_acc"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":assocType"),
                    config.createLiteralMapping(xsdString, "assoc_type"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cellLine"),
                    config.createLiteralMapping(xsdString, "cell_line"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":goQualifier"),
                    config.createLiteralMapping(xsdString, "go_qualifier"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":intensity"),
                    config.createLiteralMapping(xsdString, "intensity"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":negativeIsoformSpecificity"),
                    config.createLiteralMapping(xsdString, "negative_isoform_specificity"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":interactionDetectionMethod"),
                    config.createIriMapping("terminology", "interaction_detection_method"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":expressionScore"),
                    config.createLiteralMapping(xsdDouble, "expression_score"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":homozygote-count"),
                    config.createLiteralMapping(xsdInteger, "homozygote_count"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":allele-number"),
                    config.createLiteralMapping(xsdInteger, "allele_number"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":allele-count"),
                    config.createLiteralMapping(xsdInteger, "allele_count"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":allele-frequency"),
                    config.createLiteralMapping(xsdDouble, "allele_frequency"));
        }

        {
            Table table = new Table(schema, "evidence_publication_references");
            NodeMapping subject = config.createIriMapping("evidence", "evidence");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":reference"),
                    config.createIriMapping("publication", "publication"));
        }
    }


    private static void addEntryQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "entry_bases");
            NodeMapping subject = config.createIriMapping("entry", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Entry"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":isoformCount"),
                    config.createLiteralMapping(xsdInteger, "isoform_count"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":existence"),
                    config.createIriMapping("schema", "existence"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("uniprot", "uniprot"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":swissprotPage"),
                    config.createIriMapping("uniprotpage", "uniprot"));
        }

        {
            Table table = new Table(schema, "entry_classifiers");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":classifiedWith"),
                    config.createIriMapping("terminology", "classifier"));
        }

        {
            Table table = new Table(schema, "entry_genes");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":gene"),
                    config.createIriMapping("gene", "gene"));
        }

        {
            Table table = new Table(schema, "isoform_bases");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":isoform"),
                    config.createIriMapping("isoform", "id"));
        }

        {
            Table table = new Table(schema, "entry_publication_references");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":reference"),
                    config.createIriMapping("publication", "publication"));
        }

        {
            Table table = new Table(schema, "entry_recommended_names");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":recommendedName"),
                    config.createBlankNodeMapping(nameBlankNode, "name"));
        }

        {
            Table table = new Table(schema, "entry_alternative_names");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":alternativeName"),
                    config.createBlankNodeMapping(nameBlankNode, "name"));
        }

        {
            Table table = new Table(schema, "entry_additional_names");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":additionalNames"),
                    config.createBlankNodeMapping(nameListBlankNode, "name_list"));
        }

        {
            Table table = new Table(schema, "entry_cleaved_region_names");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cleavedRegionNames"),
                    config.createBlankNodeMapping(nameListBlankNode, "name_list"));
        }

        {
            Table table = new Table(schema, "entry_functional_region_names");
            NodeMapping subject = config.createIriMapping("entry", "entry");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":fonctionalRegionNames"),
                    config.createBlankNodeMapping(nameListBlankNode, "name_list"));
        }
    }


    private static void addProteoformQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "proteoform_bases");
            NodeMapping subject = config.createIriMapping("proteoform", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Proteoform"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));
        }

        {
            Table table = new Table(schema, "proteoform_general_annotations");
            NodeMapping subject = config.createIriMapping("proteoform", "proteoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":generalAnnotation"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "proteoform_generic_phenotypes");
            NodeMapping subject = config.createIriMapping("proteoform", "proteoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":genericPhenotype"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "proteoform_modifications");
            NodeMapping subject = config.createIriMapping("proteoform", "proteoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":modification"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "proteoform_phenotypic_variations");
            NodeMapping subject = config.createIriMapping("proteoform", "proteoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":phenotypicVariation"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "proteoform_positional_annotations");
            NodeMapping subject = config.createIriMapping("proteoform", "proteoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":positionalAnnotation"),
                    config.createIriMapping("annotation", "annotation"));
        }

        {
            Table table = new Table(schema, "proteoform_disease_related_variants");
            NodeMapping subject = config.createIriMapping("proteoform", "proteoform");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":diseaseRelatedVariant"),
                    config.createIriMapping("annotation", "annotation"));
        }
    }


    private static void addGeneQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "gene_bases");
            NodeMapping subject = config.createIriMapping("gene", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Gene"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":begin"),
                    config.createLiteralMapping(xsdInteger, "gene_begin"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":end"),
                    config.createLiteralMapping(xsdInteger, "gene_end"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":length"),
                    config.createLiteralMapping(xsdInteger, "length"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":band"),
                    config.createLiteralMapping(xsdString, "band"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":chromosome"),
                    config.createLiteralMapping(xsdString, "chromosome"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":strand"),
                    config.createLiteralMapping(xsdString, "strand"));
        }

        {
            Table table = new Table(schema, "gene_best_mappings");
            NodeMapping subject = config.createIriMapping("gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":bestGeneMapping"),
                    config.createIriMapping("entry", "mapping"));
        }

        {
            Table table = new Table(schema, "gene_names");
            NodeMapping subject = config.createIriMapping("gene", "gene");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));
        }
    }


    private static void addProteinSequenceQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass sequence = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "protein_sequence_bases");
            NodeMapping subject = config.createBlankNodeMapping(sequence, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":ProteinSequence"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":length"),
                    config.createLiteralMapping(xsdInteger, "length"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":molecularWeight"),
                    config.createLiteralMapping(xsdDouble, "molecular_weight"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":isoelectricPoint"),
                    config.createLiteralMapping(xsdDouble, "isoelectric_point"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":chain"),
                    config.createLiteralMapping(xsdString, "chain"));

            config.addQuadMapping(table, graph, config.createIriMapping("isoform", "isoform"),
                    config.createIriMapping(":sequence"), subject);
        }
    }


    private static void addProteinFamilyInfoQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass family = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "family_info_bases");
            NodeMapping subject = config.createBlankNodeMapping(family, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":FamilyInfo"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":term"),
                    config.createIriMapping("terminology", "term"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":region"),
                    config.createLiteralMapping(xsdString, "region"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":description"),
                    config.createLiteralMapping(xsdString, "description"));

            config.addQuadMapping(table, graph, config.createIriMapping("entry", "entry"),
                    config.createIriMapping(":family"), subject);
        }
    }


    private static void addHistoryQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass history = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "history_bases");
            NodeMapping subject = config.createBlankNodeMapping(history, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":History"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":integrated"),
                    config.createLiteralMapping(xsdString, "integrated"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":updated"),
                    config.createLiteralMapping(xsdString, "updated"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":lastSequenceUpdate"),
                    config.createLiteralMapping(xsdString, "last_sequence_update"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":version"),
                    config.createLiteralMapping(xsdString, "version"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":sequenceVersion"),
                    config.createLiteralMapping(xsdString, "sequence_version"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));

            config.addQuadMapping(table, graph, config.createIriMapping("entry", "entry"),
                    config.createIriMapping(":history"), subject);
        }
    }


    private static void addIdentifierQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass identifier = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "identifier_bases");
            NodeMapping subject = config.createBlankNodeMapping(identifier, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Identifier"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":provenance"),
                    config.createIriMapping("database", "provenance"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":accession"),
                    config.createLiteralMapping(xsdString, "accession"));

            config.addQuadMapping(table, graph, config.createIriMapping("entry", "entry"),
                    config.createIriMapping(":reference"), subject);
        }
    }


    private static void addEntryXrefQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass xref = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "entry_xref_bases");
            NodeMapping subject = config.createBlankNodeMapping(xref, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Xref"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":provenance"),
                    config.createIriMapping("database", "provenance"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":accession"),
                    config.createLiteralMapping(xsdString, "accession"));

            config.addQuadMapping(table, graph, config.createIriMapping("entry", "entry"),
                    config.createIriMapping(":reference"), subject);
        }
    }


    private static void addEvidenceXrefQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass xref = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "evidence_xref_bases");
            NodeMapping subject = config.createBlankNodeMapping(xref, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Xref"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":provenance"),
                    config.createIriMapping("database", "provenance"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":accession"),
                    config.createLiteralMapping(xsdString, "accession"));

            config.addQuadMapping(table, graph, config.createIriMapping("evidence", "evidence"),
                    config.createIriMapping(":reference"), subject);
        }
    }


    private static void addChebiXrefQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass xref = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "chebi_xref_bases");
            NodeMapping subject = config.createBlankNodeMapping(xref, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Xref"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":provenance"),
                    config.createIriMapping("db:ChEBI"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("chebi", "chebi"));

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":accession"),
                    config.createLiteralMapping(xsdString, "accession"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));

            config.addQuadMapping(table, graph, config.createIriMapping("annotation", "annotation"),
                    config.createIriMapping(":interactant"), subject);
        }
    }


    private static void addDrugBankXrefQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass xref = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "drugbank_xref_bases");
            NodeMapping subject = config.createBlankNodeMapping(xref, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Xref"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":provenance"),
                    config.createIriMapping("db:DrugBank"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("drugbank", "drugbank"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":accession"),
                    config.createLiteralMapping(xsdString, "accession"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));

            config.addQuadMapping(table, graph, config.createIriMapping("annotation", "annotation"),
                    config.createIriMapping(":interactant"), subject);
        }
    }


    private static void addUniProtXrefQuadMapping(SparqlDatabaseConfiguration config)
    {
        BlankNodeClass xref = new UserIntBlankNodeClass();
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "uniprot_xref_bases");
            NodeMapping subject = config.createBlankNodeMapping(xref, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Xref"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":provenance"),
                    config.createIriMapping("db:UniProtKB"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("uniprot", "uniprot"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":accession"),
                    config.createLiteralMapping(xsdString, "accession"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));

            config.addQuadMapping(table, graph, config.createIriMapping("annotation", "annotation"),
                    config.createIriMapping(":interactant"), subject);
        }
    }


    private static void addNameQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "name_bases");
            NodeMapping subject = config.createBlankNodeMapping(nameBlankNode, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":fullName"),
                    config.createLiteralMapping(xsdString, "full_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":shortName"),
                    config.createLiteralMapping(xsdString, "short_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":fullRegionName"),
                    config.createLiteralMapping(xsdString, "full_region_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":shortRegionName"),
                    config.createLiteralMapping(xsdString, "short_region_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":ecEnzymeName"),
                    config.createLiteralMapping(xsdString, "ec_enzyme_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":cdAntigenCdAntigen"),
                    config.createLiteralMapping(xsdString, "cd_antigen"));
            config.addQuadMapping(table, graph, subject,
                    config.createIriMapping(":innInternationalNonproprietaryNames"),
                    config.createLiteralMapping(xsdString, "inn_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":allergenAllergen"),
                    config.createLiteralMapping(xsdString, "allergen"));
        }
    }


    private static void addNameListQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "name_list_bases");
            NodeMapping subject = config.createBlankNodeMapping(nameListBlankNode, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":NameList"));
        }

        {
            Table table = new Table(schema, "name_list_recommended_names");
            NodeMapping subject = config.createBlankNodeMapping(nameListBlankNode, "list");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":recommendedName"),
                    config.createBlankNodeMapping(nameBlankNode, "name"));
        }

        {
            Table table = new Table(schema, "name_list_alternative_names");
            NodeMapping subject = config.createBlankNodeMapping(nameListBlankNode, "list");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":alternativeName"),
                    config.createBlankNodeMapping(nameBlankNode, "name"));
        }
    }
}
