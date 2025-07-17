create schema nextprot_indirect;

create table nextprot_indirect.context_bases
(
    id          integer not null,
    metadata    integer not null,
    method      integer,
    disease     integer,
    tissue      integer,
    line        integer,
    stage       integer,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.isoform_low_expressions
(
    isoform         integer not null,
    annotation      integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_medium_expressions
(
    isoform         integer not null,
    annotation      integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_high_expressions
(
    isoform         integer not null,
    annotation      integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.evidence_bases
(
    expression_score                double precision,
    allele_frequency                double precision,
    experimental_context            integer,
    id                              integer not null,
    evidence_code                   integer not null,
    assigned_by                     integer not null,
    expression_level                integer,
    from_xref                       integer,
    integration_level               integer,
    quality                         integer,
    interaction_detection_method    integer,
    negative                        boolean not null,
    number_of_experiments           numeric,
    homozygote_count                numeric,
    allele_number                   numeric,
    allele_count                    numeric,
    iri                             varchar unique with(fillfactor=100) not null,
    isoform_specificity             varchar,
    antibodies_acc                  varchar,
    assoc_type                      varchar not null,
    cell_line                       varchar,
    go_qualifier                    varchar,
    intensity                       varchar,
    negative_isoform_specificity    varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.evidence_publication_references
(
    publication     integer not null,
    evidence        integer not null,
    primary key(evidence, publication) with(fillfactor=100)
);

create table nextprot_indirect.annotation_bases
(
    resolution          double precision,
    id                  integer not null,
    type                integer,
    quality             integer,
    term                integer,
    impacted_object     integer,
    self_interaction    boolean,
    proteotypic         boolean,
    position_start      numeric,
    position_end        numeric,
    iri                 varchar unique with(fillfactor=100) not null,
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

create table nextprot_indirect.annotation_evidences
(
    annotation  integer not null,
    evidence    integer not null,
    primary key(annotation, evidence) with(fillfactor=100)
);

create table nextprot_indirect.annotation_negative_evidences
(
    annotation  integer not null,
    evidence    integer not null,
    primary key(annotation, evidence) with(fillfactor=100)
);

create table nextprot_indirect.annotation_diseases
(
    annotation  integer not null,
    disease     integer not null,
    primary key(annotation, disease) with(fillfactor=100)
);

create table nextprot_indirect.annotation_comments
(
    seq         integer not null,
    annotation  integer not null,
    comment     varchar not null,
    primary key(seq) with(fillfactor=100)
);

create table nextprot_indirect.annotation_isoform_specificities
(
    annotation  integer not null,
    specificity integer not null,
    primary key(annotation, specificity) with(fillfactor=100)
);

create table nextprot_indirect.annotation_entry_interactants
(
    annotation  integer not null,
    interactant integer not null,
    primary key(annotation, interactant) with(fillfactor=100)
);

create table nextprot_indirect.annotation_isoform_interactants
(
    annotation  integer not null,
    interactant integer not null,
    primary key(annotation, interactant) with(fillfactor=100)
);

create table nextprot_indirect.annotation_peptide_sets
(
    annotation  integer not null,
    peptide_set varchar not null,
    primary key(annotation, peptide_set) with(fillfactor=100)
);

create table nextprot_indirect.isoform_bases
(
    id                          integer not null,
    entry                       integer not null,
    absorption_max              integer,
    absorption_note             integer,
    allergen                    integer,
    initiator_methionine        integer,
    kinetic_k_m                 integer,
    kinetic_note                integer,
    kinetic_vmax                integer,
    non_consecutive_residue     integer,
    peroxisome_transit_peptide  integer,
    pharmaceutical              integer,
    ph_dependence               integer,
    redox_potential             integer,
    signal_peptide              integer,
    temperature_dependence      integer,
    canonical_isoform           boolean not null,
    swissprot_displayed         boolean not null,
    iri                         varchar unique with(fillfactor=100) not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.isoform_proteoforms
(
    isoform     integer not null,
    proteoform  integer not null,
    primary key(isoform, proteoform) with(fillfactor=100)
);

create table nextprot_indirect.isoform_active_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_activity_regulations
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_antibody_mappings
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_beta_strands
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_binary_interactions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_binding_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_biophysicochemical_properties
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_calcium_binding_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_catalytic_activities
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_cautions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_cellular_components
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_cleavage_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_cofactors
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_cofactor_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_coiled_coil_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_compositionally_biased_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_cross_links
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_detected_expressions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_developmental_stage_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_diseases
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_disulfide_bonds
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_dna_binding_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_domains
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_domain_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_electrophysiological_parameters
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_enzyme_classifications
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_expressions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_expression_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_expression_profiles
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_functions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_function_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_general_annotations
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_glycosylation_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_go_biological_processs
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_go_cellular_components
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_go_molecular_functions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_helixs
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_inductions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_interacting_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_interactions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_interaction_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_intramembrane_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_keywords
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_lipidation_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_mappings
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_mature_proteins
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_medicals
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_metal_binding_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_miscellaneouss
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_miscellaneous_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_miscellaneous_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_mitochondrial_transit_peptides
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_modified_residues
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_mutagenesiss
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_non_terminal_residues
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_nucleotide_phosphate_binding_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_pathways
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_pdb_mappings
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_peptide_mappings
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_positional_annotations
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_processing_products
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_propeptides
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_ptms
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_ptm_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_repeats
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_secondary_structures
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_selenocysteines
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_sequence_cautions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_sequence_conflicts
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_short_sequence_motifs
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_sites
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_small_molecule_interactions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_srm_peptide_mappings
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_subcellular_locations
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_subcellular_location_notes
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_topological_domains
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_topologies
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_transmembrane_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_transport_activities
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_turns
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_undetected_expressions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_uniprot_keywords
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_variants
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_variant_infos
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_zinc_finger_regions
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.isoform_interaction_mappings
(
    isoform     integer not null,
    annotation  integer not null,
    primary key(isoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.entry_bases
(
    id              integer not null,
    existence       integer not null,
    isoform_count   numeric not null,
    iri             varchar unique with(fillfactor=100) not null,
    uniprot         varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.entry_classifiers
(
    entry       integer not null,
    classifier  integer not null,
    primary key(entry, classifier) with(fillfactor=100)
);

create table nextprot_indirect.entry_genes
(
    entry       integer not null,
    gene        integer not null,
    primary key(entry, gene) with(fillfactor=100)
);

create table nextprot_indirect.entry_publication_references
(
    publication  integer not null,
    entry        integer not null,
    primary key(entry, publication) with(fillfactor=100)
);

create table nextprot_indirect.entry_recommended_names
(
    name     integer  not null,
    entry    integer not null,
    primary key(entry, name) with(fillfactor=100)
);

create table nextprot_indirect.entry_alternative_names
(
    name     integer  not null,
    entry    integer not null,
    primary key(entry, name) with(fillfactor=100)
);

create table nextprot_indirect.entry_additional_names
(
    name_list   integer  not null,
    entry       integer not null,
    primary key(entry, name_list) with(fillfactor=100)
);

create table nextprot_indirect.entry_cleaved_region_names
(
    name_list   integer  not null,
    entry       integer not null,
    primary key(entry, name_list) with(fillfactor=100)
);

create table nextprot_indirect.entry_functional_region_names
(
    name_list   integer  not null,
    entry       integer not null,
    primary key(entry, name_list) with(fillfactor=100)
);

create table nextprot_indirect.proteoform_bases
(
    id      integer not null,
    iri     varchar unique with(fillfactor=100) not null,
    label   varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.proteoform_general_annotations
(
    proteoform   integer not null,
    annotation   integer not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.proteoform_generic_phenotypes
(
    proteoform   integer not null,
    annotation   integer not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.proteoform_modifications
(
    proteoform   integer not null,
    annotation   integer not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.proteoform_phenotypic_variations
(
    proteoform   integer not null,
    annotation   integer not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.proteoform_positional_annotations
(
    proteoform   integer not null,
    annotation   integer not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.proteoform_disease_related_variants
(
    proteoform   integer not null,
    annotation   integer not null,
    primary key(proteoform, annotation) with(fillfactor=100)
);

create table nextprot_indirect.gene_bases
(
    id          integer not null,
    gene_begin  numeric not null,
    gene_end    numeric not null,
    length      numeric not null,
    iri         varchar unique with(fillfactor=100) not null,
    band        varchar not null,
    chromosome  varchar not null,
    strand      varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.gene_best_mappings
(
    gene            integer not null,
    mapping         integer not null,
    primary key(gene, mapping) with(fillfactor=100)
);

create table nextprot_indirect.gene_names
(
    gene   integer not null,
    name   varchar not null,
    primary key(gene, name) with(fillfactor=100)
);

create table nextprot_indirect.protein_sequence_bases
(
    molecular_weight    double precision not null,
    isoelectric_point   double precision not null,
    id                  integer not null,
    isoform             integer not null,
    length              numeric not null,
    chain               varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.family_info_bases
(
    id              integer not null,
    entry           integer not null,
    term            integer not null,
    region          varchar,
    description     varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.history_bases
(
    id                      integer not null,
    entry                   integer not null,
    integrated              varchar not null,
    updated                 varchar not null,
    last_sequence_update    varchar,
    version                 varchar,
    sequence_version        varchar,
    name                    varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.identifier_bases
(
    id              integer not null,
    entry           integer not null,
    provenance      integer not null,
    accession       varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.entry_xref_bases
(
    id              integer not null,
    entry           integer not null,
    provenance      integer not null,
    accession       varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.evidence_xref_bases
(
    id              integer not null,
    evidence        integer not null,
    provenance      integer not null,
    accession       varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.chebi_xref_bases
(
    id              integer not null,
    annotation      integer not null,
    chebi           integer not null,
    accession       varchar not null,
    label           varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.drugbank_xref_bases
(
    id              integer not null,
    annotation      integer not null,
    drugbank        varchar not null,
    accession       varchar not null,
    label           varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.uniprot_xref_bases
(
    id              integer not null,
    annotation      integer not null,
    uniprot         varchar not null,
    accession       varchar not null,
    label           varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.name_bases
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

create table nextprot_indirect.name_list_bases
(
    id      integer not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.name_list_recommended_names
(
    list    integer not null,
    name    integer not null,
    primary key(list, name) with(fillfactor=100)
);

create table nextprot_indirect.name_list_alternative_names
(
    list    integer not null,
    name    integer not null,
    primary key(list, name) with(fillfactor=100)
);

create table nextprot_indirect.publication_bases
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

create table nextprot_indirect.publication_links
(
    publication integer not null,
    link        varchar not null,
    primary key(publication, link) with(fillfactor=100)
);

create table nextprot_indirect.publication_authors
(
    id          integer not null,
    publication integer not null,
    person      boolean not null,
    name        varchar not null,
    suffix      varchar,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.publication_editors
(
    id          integer not null,
    publication integer not null,
    name        varchar not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.source_bases
(
    id          integer not null,
    iri         varchar unique with(fillfactor=100) not null,
    comment     varchar,
    reference   varchar,
    primary key(id) with(fillfactor=100)
);


create table nextprot_indirect.database_bases
(
    id          integer not null,
    iri         varchar unique with(fillfactor=100) not null,
    reference   varchar,
    primary key(id) with(fillfactor=100)
);


create table nextprot_indirect.database_comments
(
    db          integer not null,
    comment     varchar not null,
    primary key(db, comment) with(fillfactor=100)
);


create table nextprot_indirect.database_categories
(
    db          integer not null,
    category    varchar not null,
    primary key(db, category) with(fillfactor=100)
);


create table nextprot_indirect.schema_bases
(
    id          integer not null,
    type        integer,
    iri         varchar unique with(fillfactor=100) not null,
    label       varchar,
    comment     varchar,
    reference   varchar,
    primary key(id) with(fillfactor=100)
);


create table nextprot_indirect.schema_classes
(
    entity      integer not null,
    primary key(entity) with(fillfactor=100)
);


create table nextprot_indirect.schema_thing_subclasses
(
    entity      integer not null,
    primary key(entity) with(fillfactor=100)
);


create table nextprot_indirect.schema_restrictions
(
    entity      integer not null,
    notin       integer not null,
    primary key(entity, notin) with(fillfactor=100)
);

create table nextprot_indirect.schema_related_terms
(
    entity      integer not null,
    related     integer not null,
    primary key(entity, related) with(fillfactor=100)
);

create table nextprot_indirect.schema_parent_classes
(
    entity      integer not null,
    parent      integer not null,
    primary key(entity, parent) with(fillfactor=100)
);

create table nextprot_indirect.terminology_bases
(
    id      integer not null,
    type    integer,
    iri     varchar unique with(fillfactor=100) not null,
    primary key(id) with(fillfactor=100)
);

create table nextprot_indirect.terminology_labels
(
    term    integer not null,
    label   varchar not null,
    primary key(term, label) with(fillfactor=100)
);

create table nextprot_indirect.terminology_parents
(
    term    integer not null,
    parent  integer not null,
    primary key(term, parent) with(fillfactor=100)
);

create table nextprot_indirect.terminology_related_terms
(
    term    integer not null,
    related integer not null,
    primary key(term, related) with(fillfactor=100)
);


alter table nextprot_indirect.context_bases add foreign key (metadata) references nextprot_indirect.publication_bases(id);
alter table nextprot_indirect.context_bases add foreign key (method) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.context_bases add foreign key (disease) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.context_bases add foreign key (tissue) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.context_bases add foreign key (line) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.context_bases add foreign key (stage) references nextprot_indirect.terminology_bases(id);

alter table nextprot_indirect.isoform_low_expressions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_low_expressions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.isoform_medium_expressions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_medium_expressions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.isoform_high_expressions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_high_expressions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.evidence_bases add foreign key (evidence_code) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.evidence_bases add foreign key (assigned_by) references nextprot_indirect.source_bases(id);
alter table nextprot_indirect.evidence_bases add foreign key (expression_level) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.evidence_bases add foreign key (from_xref) references nextprot_indirect.database_bases(id);
alter table nextprot_indirect.evidence_bases add foreign key (integration_level) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.evidence_bases add foreign key (quality) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.evidence_bases add foreign key (experimental_context) references nextprot_indirect.context_bases(id);
alter table nextprot_indirect.evidence_bases add foreign key (interaction_detection_method)  references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.evidence_publication_references add foreign key (evidence) references nextprot_indirect.evidence_bases(id);
alter table nextprot_indirect.evidence_publication_references add foreign key (publication) references nextprot_indirect.publication_bases(id);

alter table nextprot_indirect.annotation_bases add foreign key (type) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.annotation_bases add foreign key (quality) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.annotation_bases add foreign key (term) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.annotation_bases add foreign key (impacted_object) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_evidences add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_evidences add foreign key (evidence) references nextprot_indirect.evidence_bases(id);
alter table nextprot_indirect.annotation_negative_evidences add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_negative_evidences add foreign key (evidence) references nextprot_indirect.evidence_bases(id);
alter table nextprot_indirect.annotation_diseases add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_diseases add foreign key (disease) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.annotation_comments add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_isoform_specificities add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_isoform_specificities add foreign key (specificity) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.annotation_entry_interactants add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_entry_interactants add foreign key (interactant) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.annotation_isoform_interactants add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.annotation_isoform_interactants add foreign key (interactant) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.annotation_peptide_sets add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.isoform_bases add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (absorption_max) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (absorption_note) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (allergen) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (initiator_methionine) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (kinetic_k_m) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (kinetic_note) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (kinetic_vmax) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (non_consecutive_residue) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (peroxisome_transit_peptide) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (pharmaceutical) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (ph_dependence) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (redox_potential) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (signal_peptide) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_bases add foreign key (temperature_dependence) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_proteoforms add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_proteoforms add foreign key (proteoform) references nextprot_indirect.proteoform_bases(id);
alter table nextprot_indirect.isoform_active_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_active_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_activity_regulations add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_activity_regulations add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_antibody_mappings add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_antibody_mappings add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_beta_strands add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_beta_strands add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_binary_interactions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_binary_interactions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_binding_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_binding_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_biophysicochemical_properties add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_biophysicochemical_properties add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_calcium_binding_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_calcium_binding_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_catalytic_activities add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_catalytic_activities add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_cautions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_cautions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_cellular_components add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_cellular_components add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_cleavage_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_cleavage_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_cofactors add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_cofactors add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_cofactor_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_cofactor_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_coiled_coil_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_coiled_coil_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_compositionally_biased_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_compositionally_biased_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_cross_links add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_cross_links add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_detected_expressions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_detected_expressions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_developmental_stage_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_developmental_stage_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_diseases add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_diseases add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_disulfide_bonds add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_disulfide_bonds add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_dna_binding_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_dna_binding_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_domains add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_domains add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_domain_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_domain_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_electrophysiological_parameters add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_electrophysiological_parameters add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_enzyme_classifications add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_enzyme_classifications add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_expressions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_expressions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_expression_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_expression_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_expression_profiles add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_expression_profiles add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_functions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_functions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_function_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_function_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_general_annotations add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_general_annotations add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_glycosylation_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_glycosylation_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_go_biological_processs add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_go_biological_processs add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_go_cellular_components add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_go_cellular_components add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_go_molecular_functions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_go_molecular_functions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_helixs add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_helixs add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_inductions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_inductions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_interacting_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_interacting_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_interactions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_interactions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_interaction_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_interaction_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_intramembrane_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_intramembrane_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_keywords add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_keywords add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_lipidation_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_lipidation_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_mappings add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_mappings add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_mature_proteins add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_mature_proteins add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_medicals add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_medicals add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_metal_binding_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_metal_binding_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_miscellaneouss add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_miscellaneouss add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_miscellaneous_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_miscellaneous_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_miscellaneous_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_miscellaneous_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_mitochondrial_transit_peptides add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_mitochondrial_transit_peptides add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_modified_residues add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_modified_residues add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_mutagenesiss add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_mutagenesiss add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_non_terminal_residues add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_non_terminal_residues add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_nucleotide_phosphate_binding_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_nucleotide_phosphate_binding_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_pathways add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_pathways add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_pdb_mappings add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_pdb_mappings add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_peptide_mappings add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_peptide_mappings add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_positional_annotations add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_positional_annotations add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_processing_products add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_processing_products add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_propeptides add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_propeptides add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_ptms add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_ptms add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_ptm_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_ptm_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_repeats add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_repeats add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_secondary_structures add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_secondary_structures add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_selenocysteines add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_selenocysteines add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_sequence_cautions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_sequence_cautions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_sequence_conflicts add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_sequence_conflicts add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_short_sequence_motifs add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_short_sequence_motifs add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_sites add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_sites add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_small_molecule_interactions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_small_molecule_interactions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_srm_peptide_mappings add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_srm_peptide_mappings add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_subcellular_locations add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_subcellular_locations add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_subcellular_location_notes add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_subcellular_location_notes add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_topological_domains add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_topological_domains add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_topologies add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_topologies add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_transmembrane_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_transmembrane_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_transport_activities add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_transport_activities add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_turns add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_turns add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_undetected_expressions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_undetected_expressions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_uniprot_keywords add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_uniprot_keywords add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_variants add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_variants add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_variant_infos add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_variant_infos add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_zinc_finger_regions add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_zinc_finger_regions add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.isoform_interaction_mappings add foreign key (isoform) references nextprot_indirect.isoform_bases(id);
alter table nextprot_indirect.isoform_interaction_mappings add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.entry_bases add foreign key (existence) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.entry_classifiers add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_classifiers add foreign key (classifier) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.entry_genes add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_genes add foreign key (gene) references nextprot_indirect.gene_bases(id);
alter table nextprot_indirect.entry_publication_references add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_publication_references add foreign key (publication) references nextprot_indirect.publication_bases(id);
alter table nextprot_indirect.entry_recommended_names add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_recommended_names add foreign key (name) references nextprot_indirect.name_bases(id);
alter table nextprot_indirect.entry_alternative_names add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_alternative_names add foreign key (name) references nextprot_indirect.name_bases(id);
alter table nextprot_indirect.entry_additional_names add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_additional_names add foreign key (name_list) references nextprot_indirect.name_list_bases(id);
alter table nextprot_indirect.entry_cleaved_region_names add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_cleaved_region_names add foreign key (name_list) references nextprot_indirect.name_list_bases(id);
alter table nextprot_indirect.entry_functional_region_names add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_functional_region_names add foreign key (name_list) references nextprot_indirect.name_list_bases(id);

alter table nextprot_indirect.proteoform_general_annotations add foreign key (proteoform) references nextprot_indirect.proteoform_bases(id);
alter table nextprot_indirect.proteoform_general_annotations add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.proteoform_generic_phenotypes add foreign key (proteoform) references nextprot_indirect.proteoform_bases(id);
alter table nextprot_indirect.proteoform_generic_phenotypes add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.proteoform_modifications add foreign key (proteoform) references nextprot_indirect.proteoform_bases(id);
alter table nextprot_indirect.proteoform_modifications add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.proteoform_phenotypic_variations add foreign key (proteoform) references nextprot_indirect.proteoform_bases(id);
alter table nextprot_indirect.proteoform_phenotypic_variations add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.proteoform_positional_annotations add foreign key (proteoform) references nextprot_indirect.proteoform_bases(id);
alter table nextprot_indirect.proteoform_positional_annotations add foreign key (annotation) references nextprot_indirect.annotation_bases(id);
alter table nextprot_indirect.proteoform_disease_related_variants add foreign key (proteoform) references nextprot_indirect.proteoform_bases(id);
alter table nextprot_indirect.proteoform_disease_related_variants add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.gene_best_mappings add foreign key (gene) references nextprot_indirect.gene_bases(id);
alter table nextprot_indirect.gene_best_mappings add foreign key (mapping) references nextprot_indirect.entry_bases(id);

alter table nextprot_indirect.gene_names add foreign key (gene) references nextprot_indirect.gene_bases(id);

alter table nextprot_indirect.protein_sequence_bases add foreign key (isoform) references nextprot_indirect.isoform_bases(id);

alter table nextprot_indirect.family_info_bases add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.family_info_bases add foreign key (term) references nextprot_indirect.terminology_bases(id);

alter table nextprot_indirect.history_bases add foreign key (entry) references nextprot_indirect.entry_bases(id);

alter table nextprot_indirect.identifier_bases add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.identifier_bases add foreign key (provenance) references nextprot_indirect.database_bases(id);

alter table nextprot_indirect.entry_xref_bases add foreign key (entry) references nextprot_indirect.entry_bases(id);
alter table nextprot_indirect.entry_xref_bases add foreign key (provenance) references nextprot_indirect.database_bases(id);

alter table nextprot_indirect.evidence_xref_bases add foreign key (evidence) references nextprot_indirect.evidence_bases(id);
alter table nextprot_indirect.evidence_xref_bases add foreign key (provenance) references nextprot_indirect.database_bases(id);

alter table nextprot_indirect.chebi_xref_bases add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.drugbank_xref_bases add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.uniprot_xref_bases add foreign key (annotation) references nextprot_indirect.annotation_bases(id);

alter table nextprot_indirect.name_list_recommended_names add foreign key (list) references nextprot_indirect.name_list_bases(id);
alter table nextprot_indirect.name_list_recommended_names add foreign key (name) references nextprot_indirect.name_bases(id);

alter table nextprot_indirect.name_list_alternative_names add foreign key (list) references nextprot_indirect.name_list_bases(id);
alter table nextprot_indirect.name_list_alternative_names add foreign key (name) references nextprot_indirect.name_bases(id);

alter table nextprot_indirect.publication_links add foreign key (publication) references nextprot_indirect.publication_bases(id);
alter table nextprot_indirect.publication_authors add foreign key (publication) references nextprot_indirect.publication_bases(id);
alter table nextprot_indirect.publication_editors add foreign key (publication) references nextprot_indirect.publication_bases(id);

alter table nextprot_indirect.database_comments add foreign key (db) references nextprot_indirect.database_bases(id);
alter table nextprot_indirect.database_categories add foreign key (db) references nextprot_indirect.database_bases(id);
alter table nextprot_indirect.schema_bases add foreign key (type) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.schema_classes add foreign key (entity) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.schema_thing_subclasses add foreign key (entity) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.schema_restrictions add foreign key (entity) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.schema_restrictions add foreign key (notin) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.schema_related_terms add foreign key (entity) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.schema_related_terms add foreign key (related) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.schema_parent_classes add foreign key (entity) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.schema_parent_classes add foreign key (parent) references nextprot_indirect.schema_bases(id);

alter table nextprot_indirect.terminology_bases add foreign key (type) references nextprot_indirect.schema_bases(id);
alter table nextprot_indirect.terminology_parents add foreign key (term) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.terminology_parents add foreign key (parent) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.terminology_related_terms add foreign key (term) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.terminology_related_terms add foreign key (related) references nextprot_indirect.terminology_bases(id);
alter table nextprot_indirect.terminology_labels add foreign key (term) references nextprot_indirect.terminology_bases(id);
