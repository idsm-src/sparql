create schema nextprot_direct;

create table nextprot_direct.context_bases
(
    id          integer not null,
    metadata    integer not null,
    method      varchar,
    disease     varchar,
    tissue      varchar,
    line        varchar,
    stage       varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.isoform_low_expressions
(
    isoform         varchar not null,
    annotation      varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_medium_expressions
(
    isoform         varchar not null,
    annotation      varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_high_expressions
(
    isoform         varchar not null,
    annotation      varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.evidence_bases
(
    expression_score                double precision,
    allele_frequency                double precision,
    experimental_context            integer,
    negative                        boolean not null,
    id                              varchar not null,
    evidence_code                   varchar not null,
    assigned_by                     varchar not null,
    expression_level                varchar,
    from_xref                       varchar,
    integration_level               varchar,
    quality                         varchar,
    interaction_detection_method    varchar,
    number_of_experiments           numeric,
    homozygote_count                numeric,
    allele_number                   numeric,
    allele_count                    numeric,
    isoform_specificity             varchar,
    antibodies_acc                  varchar,
    assoc_type                      varchar not null,
    cell_line                       varchar,
    go_qualifier                    varchar,
    intensity                       varchar,
    negative_isoform_specificity    varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.evidence_publication_references
(
    publication     integer not null,
    evidence        varchar not null,
    primary key(evidence, publication) with(fillfactor=100)
);

create table nextprot_direct.annotation_bases
(
    resolution          double precision,
    self_interaction    boolean,
    proteotypic         boolean,
    id                  varchar not null,
    type                varchar,
    quality             varchar,
    term                varchar,
    impacted_object     varchar,
    position_start      numeric,
    position_end        numeric,
    entry_annotation    varchar,
    variation           varchar,
    original            varchar,
    hgvs                varchar,
    method              varchar,
    peptide_name        varchar,
    peptide_unicity     varchar,
    antibody_name       varchar,
    antibody_unicity    varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.annotation_evidences
(
    annotation  varchar not null,
    evidence    varchar not null,
    primary key(annotation, evidence) with(fillfactor=100)
);

create table nextprot_direct.annotation_negative_evidences
(
    annotation  varchar not null,
    evidence    varchar not null,
    primary key(annotation, evidence) with(fillfactor=100)
);

create table nextprot_direct.annotation_diseases
(
    annotation  varchar not null,
    disease     varchar not null,
    primary key(annotation, disease) with(fillfactor=100)
);

create table nextprot_direct.annotation_comments
(
    seq         integer not null,
    annotation  varchar not null,
    comment     varchar not null,
    primary key(seq) with(fillfactor=100)
);

create table nextprot_direct.annotation_isoform_specificities
(
    annotation  varchar not null,
    specificity varchar not null,
    primary key(annotation, specificity) with(fillfactor=100)
);

create table nextprot_direct.annotation_entry_interactants
(
    annotation  varchar not null,
    interactant varchar not null,
    primary key(annotation, interactant) with(fillfactor=100)
);

create table nextprot_direct.annotation_isoform_interactants
(
    annotation  varchar not null,
    interactant varchar not null,
    primary key(annotation, interactant) with(fillfactor=100)
);

create table nextprot_direct.annotation_peptide_sets
(
    annotation  varchar not null,
    peptide_set varchar not null,
    primary key(annotation, peptide_set) with(fillfactor=100)
);

create table nextprot_direct.isoform_bases
(
    canonical_isoform           boolean not null,
    swissprot_displayed         boolean not null,
    id                          varchar not null,
    entry                       varchar not null,
    absorption_max              varchar,
    absorption_note             varchar,
    allergen                    varchar,
    initiator_methionine        varchar,
    kinetic_k_m                 varchar,
    kinetic_note                varchar,
    kinetic_vmax                varchar,
    non_consecutive_residue     varchar,
    peroxisome_transit_peptide  varchar,
    pharmaceutical              varchar,
    ph_dependence               varchar,
    redox_potential             varchar,
    signal_peptide              varchar,
    temperature_dependence      varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.isoform_proteoforms
(
    isoform     varchar not null,
    proteoform  varchar not null,
    primary key(isoform, proteoform) with(fillfactor=100)
);

create table nextprot_direct.isoform_active_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_activity_regulations
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_antibody_mappings
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_beta_strands
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_binary_interactions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_binding_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_biophysicochemical_properties
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_calcium_binding_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_catalytic_activities
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_cautions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_cellular_components
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_cleavage_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_cofactors
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_cofactor_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_coiled_coil_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_compositionally_biased_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_cross_links
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_detected_expressions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_developmental_stage_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_diseases
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_disulfide_bonds
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_dna_binding_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_domains
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_domain_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_electrophysiological_parameters
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_enzyme_classifications
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_expressions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_expression_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_expression_profiles
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_functions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_function_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_general_annotations
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_glycosylation_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_go_biological_processs
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_go_cellular_components
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_go_molecular_functions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_helixs
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_inductions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_interacting_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_interactions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_interaction_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_intramembrane_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_keywords
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_lipidation_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_mappings
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_mature_proteins
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_medicals
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_metal_binding_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_miscellaneouss
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_miscellaneous_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_miscellaneous_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_mitochondrial_transit_peptides
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_modified_residues
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_mutagenesiss
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_non_terminal_residues
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_nucleotide_phosphate_binding_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_pathways
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_pdb_mappings
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_peptide_mappings
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_positional_annotations
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_processing_products
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_propeptides
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_ptms
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_ptm_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_repeats
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_secondary_structures
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_selenocysteines
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_sequence_cautions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_sequence_conflicts
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_short_sequence_motifs
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_sites
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_small_molecule_interactions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_srm_peptide_mappings
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_subcellular_locations
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_subcellular_location_notes
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_topological_domains
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_topologies
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_transmembrane_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_transport_activities
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_turns
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_undetected_expressions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_uniprot_keywords
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_variants
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_variant_infos
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_zinc_finger_regions
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.isoform_interaction_mappings
(
    isoform     varchar not null,
    annotation  varchar not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.entry_bases
(
    id              varchar not null,
    existence       varchar not null,
    isoform_count   numeric not null,
    uniprot         varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.entry_classifiers
(
    entry       varchar not null,
    classifier  varchar not null,
    primary key(entry, classifier) with(fillfactor=100)
);

create table nextprot_direct.entry_genes
(
    entry       varchar not null,
    gene        varchar not null,
    primary key(entry, gene) with(fillfactor=100)
);

create table nextprot_direct.entry_publication_references
(
    publication  integer not null,
    entry        varchar not null,
    primary key(entry, publication) with(fillfactor=100)
);

create table nextprot_direct.entry_recommended_names
(
    name     integer  not null,
    entry    varchar not null,
    primary key(entry, name) with(fillfactor=100)
);

create table nextprot_direct.entry_alternative_names
(
    name     integer  not null,
    entry    varchar not null,
    primary key(entry, name) with(fillfactor=100)
);

create table nextprot_direct.entry_additional_names
(
    name_list   integer  not null,
    entry       varchar not null,
    primary key(entry, name_list) with(fillfactor=100)
);

create table nextprot_direct.entry_cleaved_region_names
(
    name_list   integer  not null,
    entry       varchar not null,
    primary key(entry, name_list) with(fillfactor=100)
);

create table nextprot_direct.entry_functional_region_names
(
    name_list   integer  not null,
    entry       varchar not null,
    primary key(entry, name_list) with(fillfactor=100)
);

create table nextprot_direct.proteoform_bases
(
    id      varchar not null,
    label   varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.proteoform_general_annotations
(
    proteoform   varchar not null,
    annotation   varchar not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.proteoform_generic_phenotypes
(
    proteoform   varchar not null,
    annotation   varchar not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.proteoform_modifications
(
    proteoform   varchar not null,
    annotation   varchar not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.proteoform_phenotypic_variations
(
    proteoform   varchar not null,
    annotation   varchar not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.proteoform_positional_annotations
(
    proteoform   varchar not null,
    annotation   varchar not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.proteoform_disease_related_variants
(
    proteoform   varchar not null,
    annotation   varchar not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_direct.gene_bases
(
    id          varchar not null,
    gene_begin  numeric not null,
    gene_end    numeric not null,
    length      numeric not null,
    band        varchar not null,
    chromosome  varchar not null,
    strand      varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.gene_best_mappings
(
    gene            varchar not null,
    mapping         varchar not null,
    primary key(gene, mapping) with(fillfactor=100)
);

create table nextprot_direct.gene_names
(
    gene   varchar not null,
    name   varchar not null,
    primary key(gene, name) with(fillfactor=100)
);

create table nextprot_direct.protein_sequence_bases
(
    molecular_weight    double precision not null,
    isoelectric_point   double precision not null,
    id                  integer not null,
    isoform             varchar not null,
    length              numeric not null,
    chain               varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.family_info_bases
(
    id              integer not null,
    entry           varchar not null,
    term            varchar not null,
    region          varchar,
    description     varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.history_bases
(
    id                      integer not null,
    entry                   varchar not null,
    integrated              varchar not null,
    updated                 varchar not null,
    last_sequence_update    varchar,
    version                 varchar,
    sequence_version        varchar,
    name                    varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.identifier_bases
(
    id              integer not null,
    entry           varchar not null,
    provenance      varchar not null,
    accession       varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.entry_xref_bases
(
    id              integer not null,
    entry           varchar not null,
    provenance      varchar not null,
    accession       varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.evidence_xref_bases
(
    id              integer not null,
    evidence        varchar not null,
    provenance      varchar not null,
    accession       varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.chebi_xref_bases
(
    id              integer not null,
    annotation      varchar not null,
    chebi           integer not null,
    accession       varchar not null,
    label           varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.drugbank_xref_bases
(
    id              integer not null,
    annotation      varchar not null,
    drugbank        varchar not null,
    accession       varchar not null,
    label           varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.uniprot_xref_bases
(
    id              integer not null,
    annotation      varchar not null,
    uniprot         varchar not null,
    accession       varchar not null,
    label           varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.name_bases
(
    id                  integer not null,
    full_name           varchar,
    short_name          varchar,
    full_region_name    varchar,
    short_region_name   varchar,
    ec_enzyme_name      varchar,
    cd_antigen          varchar,
    inn_name            varchar,
    allergen            varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.name_list_bases
(
    id      integer not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.name_list_recommended_names
(
    list    integer not null,
    name    integer not null,
    primary key(list, name) with(fillfactor=100)
);

create table nextprot_direct.name_list_alternative_names
(
    list    integer not null,
    name    integer not null,
    primary key(list, name) with(fillfactor=100)
);

create table nextprot_direct.publication_bases
(
    id          integer not null,
    large       boolean not null,
    title       varchar,
    journal     varchar,
    year        varchar,
    volume      varchar,
    issue       varchar,
    pub_type    varchar,
    first_page  varchar,
    last_page   varchar,
    publisher   varchar,
    city        varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.publication_links
(
    publication integer not null,
    link        varchar not null,
    primary key(publication, link) with(fillfactor=100)
);

create table nextprot_direct.publication_authors
(
    id          integer not null,
    publication integer not null,
    person      boolean not null,
    name        varchar not null,
    suffix      varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.publication_editors
(
    id          integer not null,
    publication integer not null,
    name        varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.source_bases
(
    id          varchar not null,
    comment     varchar,
    reference   varchar,
    primary key(id) with(fillfactor=100)
);


create table nextprot_direct.database_bases
(
    id          varchar not null,
    reference   varchar,
    primary key(id) with(fillfactor=100)
);


create table nextprot_direct.database_comments
(
    db          varchar not null,
    comment     varchar not null,
    primary key(db, comment) with(fillfactor=100)
);


create table nextprot_direct.database_categories
(
    db          varchar not null,
    category    varchar not null,
    primary key(db, category) with(fillfactor=100)
);


create table nextprot_direct.schema_bases
(
    id          varchar not null,
    type        varchar,
    label       varchar,
    comment     varchar,
    reference   varchar,
    primary key(id) with(fillfactor=100)
);


create table nextprot_direct.schema_classes
(
    entity      varchar not null,
    primary key(entity) with(fillfactor=100)
);


create table nextprot_direct.schema_thing_subclasses
(
    entity      varchar not null,
    primary key(entity) with(fillfactor=100)
);


create table nextprot_direct.schema_restrictions
(
    entity      varchar not null,
    notin       varchar not null,
    primary key(entity, notin) with(fillfactor=100)
);

create table nextprot_direct.schema_related_terms
(
    entity      varchar not null,
    related     varchar not null,
    primary key(entity, related) with(fillfactor=100)
);

create table nextprot_direct.schema_parent_classes
(
    entity      varchar not null,
    parent      varchar not null,
    primary key(entity, parent) with(fillfactor=100)
);

create table nextprot_direct.terminology_bases
(
    id      varchar not null,
    type    varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_direct.terminology_labels
(
    term    varchar not null,
    label   varchar not null,
    primary key(term, label) with(fillfactor=100)
);

create table nextprot_direct.terminology_parents
(
    term    varchar not null,
    parent  varchar not null,
    primary key(term, parent) with(fillfactor=100)
);

create table nextprot_direct.terminology_related_terms
(
    term    varchar not null,
    related varchar not null,
    primary key(term, related) with(fillfactor=100)
);


alter table nextprot_direct.context_bases add foreign key (metadata) references nextprot_direct.publication_bases(id);
alter table nextprot_direct.context_bases add foreign key (method) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.context_bases add foreign key (disease) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.context_bases add foreign key (tissue) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.context_bases add foreign key (line) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.context_bases add foreign key (stage) references nextprot_direct.terminology_bases(id);

alter table nextprot_direct.isoform_low_expressions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_low_expressions add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.isoform_medium_expressions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_medium_expressions add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.isoform_high_expressions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_high_expressions add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.evidence_bases add foreign key (evidence_code) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.evidence_bases add foreign key (assigned_by) references nextprot_direct.source_bases(id);
alter table nextprot_direct.evidence_bases add foreign key (expression_level) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.evidence_bases add foreign key (from_xref) references nextprot_direct.database_bases(id);
alter table nextprot_direct.evidence_bases add foreign key (integration_level) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.evidence_bases add foreign key (quality) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.evidence_bases add foreign key (experimental_context) references nextprot_direct.context_bases(id);
alter table nextprot_direct.evidence_bases add foreign key (interaction_detection_method)  references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.evidence_publication_references add foreign key (evidence) references nextprot_direct.evidence_bases(id);
alter table nextprot_direct.evidence_publication_references add foreign key (publication) references nextprot_direct.publication_bases(id);

alter table nextprot_direct.annotation_bases add foreign key (type) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.annotation_bases add foreign key (quality) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.annotation_bases add foreign key (term) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.annotation_bases add foreign key (impacted_object) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_evidences add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_evidences add foreign key (evidence) references nextprot_direct.evidence_bases(id);
alter table nextprot_direct.annotation_negative_evidences add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_negative_evidences add foreign key (evidence) references nextprot_direct.evidence_bases(id);
alter table nextprot_direct.annotation_diseases add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_diseases add foreign key (disease) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.annotation_comments add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_isoform_specificities add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_isoform_specificities add foreign key (specificity) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.annotation_entry_interactants add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_entry_interactants add foreign key (interactant) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.annotation_isoform_interactants add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.annotation_isoform_interactants add foreign key (interactant) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.annotation_peptide_sets add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.isoform_bases add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (absorption_max) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (absorption_note) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (allergen) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (initiator_methionine) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (kinetic_k_m) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (kinetic_note) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (kinetic_vmax) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (non_consecutive_residue) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (peroxisome_transit_peptide) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (pharmaceutical) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (ph_dependence) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (redox_potential) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (signal_peptide) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_bases add foreign key (temperature_dependence) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_proteoforms add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_proteoforms add foreign key (proteoform) references nextprot_direct.proteoform_bases(id);
alter table nextprot_direct.isoform_active_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_active_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_activity_regulations add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_activity_regulations add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_antibody_mappings add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_antibody_mappings add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_beta_strands add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_beta_strands add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_binary_interactions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_binary_interactions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_binding_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_binding_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_biophysicochemical_properties add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_biophysicochemical_properties add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_calcium_binding_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_calcium_binding_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_catalytic_activities add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_catalytic_activities add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_cautions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_cautions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_cellular_components add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_cellular_components add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_cleavage_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_cleavage_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_cofactors add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_cofactors add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_cofactor_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_cofactor_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_coiled_coil_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_coiled_coil_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_compositionally_biased_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_compositionally_biased_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_cross_links add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_cross_links add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_detected_expressions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_detected_expressions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_developmental_stage_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_developmental_stage_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_diseases add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_diseases add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_disulfide_bonds add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_disulfide_bonds add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_dna_binding_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_dna_binding_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_domains add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_domains add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_domain_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_domain_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_electrophysiological_parameters add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_electrophysiological_parameters add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_enzyme_classifications add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_enzyme_classifications add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_expressions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_expressions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_expression_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_expression_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_expression_profiles add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_expression_profiles add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_functions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_functions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_function_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_function_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_general_annotations add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_general_annotations add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_glycosylation_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_glycosylation_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_go_biological_processs add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_go_biological_processs add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_go_cellular_components add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_go_cellular_components add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_go_molecular_functions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_go_molecular_functions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_helixs add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_helixs add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_inductions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_inductions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_interacting_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_interacting_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_interactions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_interactions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_interaction_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_interaction_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_intramembrane_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_intramembrane_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_keywords add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_keywords add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_lipidation_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_lipidation_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_mappings add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_mappings add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_mature_proteins add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_mature_proteins add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_medicals add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_medicals add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_metal_binding_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_metal_binding_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_miscellaneouss add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_miscellaneouss add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_miscellaneous_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_miscellaneous_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_miscellaneous_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_miscellaneous_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_mitochondrial_transit_peptides add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_mitochondrial_transit_peptides add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_modified_residues add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_modified_residues add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_mutagenesiss add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_mutagenesiss add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_non_terminal_residues add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_non_terminal_residues add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_nucleotide_phosphate_binding_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_nucleotide_phosphate_binding_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_pathways add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_pathways add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_pdb_mappings add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_pdb_mappings add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_peptide_mappings add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_peptide_mappings add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_positional_annotations add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_positional_annotations add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_processing_products add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_processing_products add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_propeptides add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_propeptides add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_ptms add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_ptms add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_ptm_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_ptm_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_repeats add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_repeats add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_secondary_structures add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_secondary_structures add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_selenocysteines add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_selenocysteines add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_sequence_cautions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_sequence_cautions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_sequence_conflicts add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_sequence_conflicts add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_short_sequence_motifs add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_short_sequence_motifs add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_sites add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_sites add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_small_molecule_interactions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_small_molecule_interactions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_srm_peptide_mappings add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_srm_peptide_mappings add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_subcellular_locations add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_subcellular_locations add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_subcellular_location_notes add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_subcellular_location_notes add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_topological_domains add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_topological_domains add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_topologies add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_topologies add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_transmembrane_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_transmembrane_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_transport_activities add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_transport_activities add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_turns add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_turns add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_undetected_expressions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_undetected_expressions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_uniprot_keywords add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_uniprot_keywords add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_variants add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_variants add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_variant_infos add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_variant_infos add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_zinc_finger_regions add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_zinc_finger_regions add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.isoform_interaction_mappings add foreign key (isoform) references nextprot_direct.isoform_bases(id);
alter table nextprot_direct.isoform_interaction_mappings add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.entry_bases add foreign key (existence) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.entry_classifiers add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_classifiers add foreign key (classifier) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.entry_genes add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_genes add foreign key (gene) references nextprot_direct.gene_bases(id);
alter table nextprot_direct.entry_publication_references add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_publication_references add foreign key (publication) references nextprot_direct.publication_bases(id);
alter table nextprot_direct.entry_recommended_names add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_recommended_names add foreign key (name) references nextprot_direct.name_bases(id);
alter table nextprot_direct.entry_alternative_names add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_alternative_names add foreign key (name) references nextprot_direct.name_bases(id);
alter table nextprot_direct.entry_additional_names add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_additional_names add foreign key (name_list) references nextprot_direct.name_list_bases(id);
alter table nextprot_direct.entry_cleaved_region_names add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_cleaved_region_names add foreign key (name_list) references nextprot_direct.name_list_bases(id);
alter table nextprot_direct.entry_functional_region_names add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_functional_region_names add foreign key (name_list) references nextprot_direct.name_list_bases(id);

alter table nextprot_direct.proteoform_general_annotations add foreign key (proteoform) references nextprot_direct.proteoform_bases(id);
alter table nextprot_direct.proteoform_general_annotations add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.proteoform_generic_phenotypes add foreign key (proteoform) references nextprot_direct.proteoform_bases(id);
alter table nextprot_direct.proteoform_generic_phenotypes add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.proteoform_modifications add foreign key (proteoform) references nextprot_direct.proteoform_bases(id);
alter table nextprot_direct.proteoform_modifications add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.proteoform_phenotypic_variations add foreign key (proteoform) references nextprot_direct.proteoform_bases(id);
alter table nextprot_direct.proteoform_phenotypic_variations add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.proteoform_positional_annotations add foreign key (proteoform) references nextprot_direct.proteoform_bases(id);
alter table nextprot_direct.proteoform_positional_annotations add foreign key (annotation) references nextprot_direct.annotation_bases(id);
alter table nextprot_direct.proteoform_disease_related_variants add foreign key (proteoform) references nextprot_direct.proteoform_bases(id);
alter table nextprot_direct.proteoform_disease_related_variants add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.gene_best_mappings add foreign key (gene) references nextprot_direct.gene_bases(id);
alter table nextprot_direct.gene_best_mappings add foreign key (mapping) references nextprot_direct.entry_bases(id);

alter table nextprot_direct.gene_names add foreign key (gene) references nextprot_direct.gene_bases(id);

alter table nextprot_direct.protein_sequence_bases add foreign key (isoform) references nextprot_direct.isoform_bases(id);

alter table nextprot_direct.family_info_bases add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.family_info_bases add foreign key (term) references nextprot_direct.terminology_bases(id);

alter table nextprot_direct.history_bases add foreign key (entry) references nextprot_direct.entry_bases(id);

alter table nextprot_direct.identifier_bases add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.identifier_bases add foreign key (provenance) references nextprot_direct.database_bases(id);

alter table nextprot_direct.entry_xref_bases add foreign key (entry) references nextprot_direct.entry_bases(id);
alter table nextprot_direct.entry_xref_bases add foreign key (provenance) references nextprot_direct.database_bases(id);

alter table nextprot_direct.evidence_xref_bases add foreign key (evidence) references nextprot_direct.evidence_bases(id);
alter table nextprot_direct.evidence_xref_bases add foreign key (provenance) references nextprot_direct.database_bases(id);

alter table nextprot_direct.chebi_xref_bases add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.drugbank_xref_bases add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.uniprot_xref_bases add foreign key (annotation) references nextprot_direct.annotation_bases(id);

alter table nextprot_direct.name_list_recommended_names add foreign key (list) references nextprot_direct.name_list_bases(id);
alter table nextprot_direct.name_list_recommended_names add foreign key (name) references nextprot_direct.name_bases(id);

alter table nextprot_direct.name_list_alternative_names add foreign key (list) references nextprot_direct.name_list_bases(id);
alter table nextprot_direct.name_list_alternative_names add foreign key (name) references nextprot_direct.name_bases(id);

alter table nextprot_direct.publication_links add foreign key (publication) references nextprot_direct.publication_bases(id);
alter table nextprot_direct.publication_authors add foreign key (publication) references nextprot_direct.publication_bases(id);
alter table nextprot_direct.publication_editors add foreign key (publication) references nextprot_direct.publication_bases(id);

alter table nextprot_direct.database_comments add foreign key (db) references nextprot_direct.database_bases(id);
alter table nextprot_direct.database_categories add foreign key (db) references nextprot_direct.database_bases(id);
alter table nextprot_direct.schema_bases add foreign key (type) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.schema_classes add foreign key (entity) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.schema_thing_subclasses add foreign key (entity) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.schema_restrictions add foreign key (entity) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.schema_restrictions add foreign key (notin) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.schema_related_terms add foreign key (entity) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.schema_related_terms add foreign key (related) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.schema_parent_classes add foreign key (entity) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.schema_parent_classes add foreign key (parent) references nextprot_direct.schema_bases(id);

alter table nextprot_direct.terminology_bases add foreign key (type) references nextprot_direct.schema_bases(id);
alter table nextprot_direct.terminology_parents add foreign key (term) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.terminology_parents add foreign key (parent) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.terminology_related_terms add foreign key (term) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.terminology_related_terms add foreign key (related) references nextprot_direct.terminology_bases(id);
alter table nextprot_direct.terminology_labels add foreign key (term) references nextprot_direct.terminology_bases(id);
