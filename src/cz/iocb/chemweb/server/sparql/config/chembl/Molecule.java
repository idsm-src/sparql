package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass.SqlCheck;



class Molecule
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass("molecule", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chebi", Arrays.asList("bigint"),
                "http://purl\\.obolibrary\\.org/obo/CHEBI_[1-9][0-9]*"));

        config.addIriClass(new UserIriClass("displayimage", Arrays.asList("varchar"),
                "https://www\\.ebi\\.ac\\.uk/chembl/compound/displayimage_large/CHEMBL[1-9][0-9]*"));

        config.addIriClass(new UserIriClass("chembl_acd_most_apka", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#acd_most_apka",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_acd_most_bpka", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#acd_most_bpka",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_acd_logd", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#acd_logd", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_acd_logp", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#acd_logp", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_alogp", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#alogp", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_aromatic_rings", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#aromatic_rings",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_hba", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#hba", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_hbd", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#hbd", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_heavy_atoms", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#heavy_atoms",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_num_ro5_violations", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#num_ro5_violations",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_psa", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#psa", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_qed_weighted", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#qed_weighted",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_rtb", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#rtb", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_mw_freebase", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#mw_freebase",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_mw_monoisotopic", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#mw_monoisotopic",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_full_mwt", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#full_mwt", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_molecular_species", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#molecular_species",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_ro3_pass", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#ro3_pass", SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_full_molformula", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#full_molformula",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_standard_inchi_key", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#standard_inchi_key",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_standard_inchi", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#standard_inchi",
                SqlCheck.IF_MATCH));

        config.addIriClass(new UserIriClass("chembl_canonical_smiles", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/molecule/CHEMBL[1-9][0-9]*#canonical_smiles",
                SqlCheck.IF_MATCH));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass molecule = config.getIriClass("molecule");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "molecule_dictionary";
        NodeMapping subject = config.createIriMapping(molecule, "molregno");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:UnclassifiedSubstance"), "molecule_type = 'Unclassified'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:CellTherapy"), "molecule_type = 'Cell'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Oligosaccharide"), "molecule_type = 'Oligosaccharide'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Enzyme"), "molecule_type = 'Enzyme'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Oligonucleotide"), "molecule_type = 'Oligonucleotide'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Antibody"), "molecule_type = 'Antibody'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:UnknownSubstance"), "molecule_type = 'Unknown'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ProteinMolecule"), "molecule_type = 'Protein'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:SmallMolecule"), "molecule_type = 'Small molecule'");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("foaf:depiction"),
                config.createIriMapping("displayimage", "chembl_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                config.createIriMapping("chebi", "chebi_par_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:highestDevelopmentPhase"),
                config.createLiteralMapping(xsdInt, "(max_phase::int4)"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "chembl_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(coalesce(pref_name, chembl_id))"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:prefLabel"),
                config.createLiteralMapping(xsdString, "(coalesce(pref_name, chembl_id))"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:substanceType"),
                config.createLiteralMapping(xsdString, "molecule_type"));

        config.addQuadMapping(table, graph, config.createIriMapping("displayimage", "chembl_id"),
                config.createIriMapping("rdf:type"), config.createIriMapping("foaf:Image"));

        config.addQuadMapping(table, graph, config.createIriMapping("displayimage", "chembl_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('PNG Image Depiction of ' || chembl_id)"));


        config.addQuadMapping("molecule_names", graph, subject, config.createIriMapping("skos:altLabel"),
                config.createLiteralMapping(xsdString, "name"));


        config.addQuadMapping("biotherapeutics", graph, subject, config.createIriMapping("cco:isBiotherapeutic"),
                config.createLiteralMapping(true));

        config.addQuadMapping("biotherapeutics", graph, subject, config.createIriMapping("cco:helmNotation"),
                config.createLiteralMapping(xsdString, "helm_notation"));

        config.addQuadMapping("biotherapeutics", graph, subject, config.createIriMapping("dcterms:description"),
                config.createLiteralMapping(xsdString, "description"));


        config.addQuadMapping("molecule_atc_classification", graph, subject,
                config.createIriMapping("cco:atcClassification"), config.createLiteralMapping(xsdString, "level5"));


        config.addQuadMapping("biotherapeutic_components", graph, subject,
                config.createIriMapping("cco:hasBioComponent"),
                config.createIriMapping("biocomponent", "component_id"));

        config.addQuadMapping("biotherapeutic_components", graph,
                config.createIriMapping("biocomponent", "component_id"), config.createIriMapping("cco:hasMolecule"),
                subject);


        config.addQuadMapping("molecule_hrac_classification", "hrac_classification", "hrac_class_id", "hrac_class_id",
                graph, subject, config.createIriMapping("cco:hracClassification"),
                config.createLiteralMapping(xsdString, "hrac_code"));

        config.addQuadMapping("molecule_irac_classification", "irac_classification", "irac_class_id", "irac_class_id",
                graph, subject, config.createIriMapping("cco:iracClassification"),
                config.createLiteralMapping(xsdString, "level2"));

        config.addQuadMapping("molecule_frac_classification", "frac_classification", "frac_class_id", "frac_class_id",
                graph, subject, config.createIriMapping("cco:fracClassification"),
                config.createLiteralMapping(xsdString, "level2"));


        config.addQuadMapping("molecule_docs", graph, subject, config.createIriMapping("cco:hasDocument"),
                config.createIriMapping("document", "doc_id"));

        config.addQuadMapping("molecule_docs", graph, config.createIriMapping("document", "doc_id"),
                config.createIriMapping("cco:hasMolecule"), subject);


        config.addQuadMapping(null, graph,
                config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl/chembl_25.0_molecule_chebi_ls.ttl>"),
                config.createIriMapping("void:inDataset"),
                config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl/25.0/void.ttl#/molecule_chebi_linkset>"));


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_acd_most_apka", "molregno"), "acd_most_apka is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_most_apka", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000324"),
                "acd_most_apka is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_most_apka", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(acd_most_apka::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_most_apka", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' ACD Most Acidic pKa: ' || rtrim(rtrim(cast(acd_most_apka as text), '0'), '.'))"),
                "acd_most_apka is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_acd_most_bpka", "molregno"), "acd_most_bpka is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_most_bpka", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000325"),
                "acd_most_bpka is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_most_bpka", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(acd_most_bpka::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_most_bpka", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' ACD Most Basic pKa: ' || rtrim(rtrim(cast(acd_most_bpka as text), '0'), '.'))"),
                "acd_most_bpka is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_acd_logd", "molregno"), "acd_logd is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_logd", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000323"),
                "acd_logd is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_logd", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(acd_logd::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_logd", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' ACD LogD: ' || rtrim(rtrim(cast(acd_logd as text), '0'), '.'))"),
                "acd_logd is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_acd_logp", "molregno"), "acd_logp is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_logp", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000321"),
                "acd_logp is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_logp", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(acd_logp::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_acd_logp", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' ACD LogP: ' || rtrim(rtrim(cast(acd_logp as text), '0'), '.'))"),
                "acd_logp is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_alogp", "molregno"), "alogp is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_alogp", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000305"),
                "alogp is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_alogp", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(alogp::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_alogp", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' ALogP: ' || rtrim(rtrim(cast(alogp as text), '0'), '.'))"),
                "alogp is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_aromatic_rings", "molregno"), "aromatic_rings is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_aromatic_rings", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000381"), "aromatic_rings is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_aromatic_rings", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(aromatic_rings::float8)"));

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_aromatic_rings", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Aromatic Rings: ' || aromatic_rings)"),
                "aromatic_rings is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_hba", "molregno"), "hba is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_hba", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000309"),
                "hba is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_hba", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "(hba::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_hba", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Hydrogen Bond Acceptors: ' || hba)"),
                "hba is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_hbd", "molregno"), "hbd is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_hbd", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000310"),
                "hbd is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_hbd", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "(hbd::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_hbd", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Hydrogen Bond Donors: ' || hbd)"),
                "hbd is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_heavy_atoms", "molregno"), "heavy_atoms is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_heavy_atoms", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000300"),
                "heavy_atoms is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_heavy_atoms", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(heavy_atoms::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_heavy_atoms", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Heavy Atoms: ' || heavy_atoms)"),
                "heavy_atoms is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_num_ro5_violations", "molregno"), "num_ro5_violations is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_num_ro5_violations", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000312"), "num_ro5_violations is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_num_ro5_violations", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(num_ro5_violations::float8)"));

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_num_ro5_violations", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' RO5 Violations: ' || num_ro5_violations)"),
                "num_ro5_violations is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_psa", "molregno"), "psa is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_psa", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000308"),
                "psa is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_psa", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "(psa::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_psa", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Polar Surface Area: ' || rtrim(rtrim(cast(psa as text), '0'), '.'))"),
                "psa is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_qed_weighted", "molregno"), "qed_weighted is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_qed_weighted", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000431"),
                "qed_weighted is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_qed_weighted", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(qed_weighted::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_qed_weighted", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' QED Weighted Score: ' || rtrim(rtrim(cast(qed_weighted as text), '0'), '.'))"),
                "qed_weighted is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_rtb", "molregno"), "rtb is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_rtb", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000311"),
                "rtb is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_rtb", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "(rtb::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_rtb", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Rotatable Bonds: ' || rtb)"),
                "rtb is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_mw_freebase", "molregno"), "mw_freebase is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_mw_freebase", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000350"),
                "mw_freebase is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_mw_freebase", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(mw_freebase::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_mw_freebase", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Freebase Molecular Weight: ' || rtrim(rtrim(cast(mw_freebase as text), '0'), '.'))"),
                "mw_freebase is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_mw_monoisotopic", "molregno"), "mw_monoisotopic is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_mw_monoisotopic", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000218"), "mw_monoisotopic is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_mw_monoisotopic", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(mw_monoisotopic::float8)"));

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_mw_monoisotopic", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Monoisotopic Mass: ' || rtrim(rtrim(cast(mw_monoisotopic as text), '0'), '.'))"),
                "mw_monoisotopic is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_full_mwt", "molregno"), "full_mwt is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_full_mwt", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000198"),
                "full_mwt is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_full_mwt", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdDouble, "(full_mwt::float8)"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_full_mwt", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Full Molecular Weight: ' || rtrim(rtrim(cast(full_mwt as text), '0'), '.'))"),
                "full_mwt is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_molecular_species", "molregno"), "molecular_species is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_molecular_species", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000326"), "molecular_species is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_molecular_species", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdString, "molecular_species"));

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_molecular_species", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Molecular Species: ' || molecular_species)"),
                "molecular_species is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_ro3_pass", "molregno"), "ro3_pass is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_ro3_pass", "molregno"),
                config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000317"),
                "ro3_pass is not null");

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_ro3_pass", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdString, "ro3_pass"));

        config.addQuadMapping("compound_properties", graph, config.createIriMapping("chembl_ro3_pass", "molregno"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' RO3 Pass: ' || ro3_pass)"),
                "ro3_pass is not null");


        config.addQuadMapping("compound_properties", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_full_molformula", "molregno"), "full_molformula is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_full_molformula", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000042"), "full_molformula is not null");

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_full_molformula", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdString, "full_molformula"));

        config.addQuadMapping("compound_properties", graph,
                config.createIriMapping("chembl_full_molformula", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_properties.molregno) ||"
                                + " ' Molecular Formula: ' || full_molformula)"),
                "full_molformula is not null");


        config.addQuadMapping("compound_structures", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_standard_inchi_key", "molregno"), "standard_inchi_key is not null");

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_standard_inchi_key", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000059"), "standard_inchi_key is not null");

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_standard_inchi_key", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdString, "standard_inchi_key"));

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_standard_inchi_key", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_structures.molregno) ||"
                                + " ' Standard InChi Key: ' || standard_inchi_key)"),
                "standard_inchi_key is not null");


        config.addQuadMapping("compound_structures", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_standard_inchi", "molregno"), "standard_inchi is not null");

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_standard_inchi", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000113"), "standard_inchi is not null");

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_standard_inchi", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdString, "standard_inchi"));

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_standard_inchi", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_structures.molregno) ||"
                                + " ' Standard InChi')"),
                "standard_inchi is not null");


        config.addQuadMapping("compound_structures", graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                config.createIriMapping("chembl_canonical_smiles", "molregno"), "canonical_smiles is not null");

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_canonical_smiles", "molregno"), config.createIriMapping("rdf:type"),
                config.createIriMapping("cheminf:CHEMINF_000018"), "canonical_smiles is not null");

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_canonical_smiles", "molregno"),
                config.createIriMapping("cheminf:SIO_000300"),
                config.createLiteralMapping(xsdString, "canonical_smiles"));

        config.addQuadMapping("compound_structures", graph,
                config.createIriMapping("chembl_canonical_smiles", "molregno"), config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString,
                        "((select chembl_id from molecule_dictionary where molregno = compound_structures.molregno) ||"
                                + " ' Canonical Smiles')"),
                "canonical_smiles is not null");


        config.addQuadMapping("molecule_hierarchy", graph, subject, config.createIriMapping("cco:hasParentMolecule"),
                config.createIriMapping("molecule", "parent_molregno"), "molregno != parent_molregno");

        config.addQuadMapping("molecule_hierarchy", graph, config.createIriMapping("molecule", "parent_molregno"),
                config.createIriMapping("cco:hasChildMolecule"), subject, "molregno != parent_molregno");
    }
}
