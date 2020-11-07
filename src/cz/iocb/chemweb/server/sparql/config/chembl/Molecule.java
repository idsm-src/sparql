package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Molecule
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("chembl:molecule", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL"));
        config.addIriClass(new IntegerUserIriClass("chembl:molfile", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "_Molfile"));
        config.addIriClass(new IntegerUserIriClass("chembl:displayimage", "integer",
                "https://www.ebi.ac.uk/chembl/compound/displayimage_large/CHEMBL"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_cx_most_apka", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#cx_most_apka"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_cx_most_bpka", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#cx_most_bpka"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_cx_logd", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#cx_logd"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_cx_logp", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#cx_logp"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_alogp", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#alogp"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_aromatic_rings", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#aromatic_rings"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_hba", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#hba"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_hbd", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#hbd"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_heavy_atoms", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#heavy_atoms"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_num_ro5_violations", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#num_ro5_violations"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_psa", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#psa"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_qed_weighted", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#qed_weighted"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_rtb", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#rtb"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_mw_freebase", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#mw_freebase"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_mw_monoisotopic", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#mw_monoisotopic"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_full_mwt", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#full_mwt"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_molecular_species", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#molecular_species"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_ro3_pass", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#ro3_pass"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_full_molformula", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#full_molformula"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_standard_inchi_key", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#standard_inchi_key"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_standard_inchi", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#standard_inchi"));
        config.addIriClass(new IntegerUserIriClass("chembl:molecule_canonical_smiles", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/CHEMBL", "#canonical_smiles"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        {
            Table table = new Table(schema, "molecule_dictionary");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "id");

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
                    config.createIriMapping("chembl:displayimage", "id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("ontology:resource", Ontology.unitCHEBI, "chebi_par_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:highestDevelopmentPhase"),
                    config.createLiteralMapping(xsdInt, "max_phase"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                    config.createLiteralMapping(xsdString, "chembl_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "(coalesce(pref_name, chembl_id))"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:prefLabel"),
                    config.createLiteralMapping(xsdString, "(coalesce(pref_name, chembl_id))"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:substanceType"),
                    config.createLiteralMapping(xsdString, "molecule_type"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:displayimage", "id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("foaf:Image"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:displayimage", "id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "('PNG Image Depiction of ' || chembl_id)"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:Substance"));
        }

        {
            Table table = new Table(schema, "molecule_names");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:altLabel"),
                    config.createLiteralMapping(xsdString, "name"));
        }

        {
            Table table = new Table(schema, "biotherapeutics");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:isBiotherapeutic"),
                    config.createLiteralMapping(true));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:helmNotation"),
                    config.createLiteralMapping(xsdString, "helm_notation"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:description"),
                    config.createLiteralMapping(xsdString, "description"));
        }

        {
            Table table = new Table(schema, "molecule_atc_classification");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:atcClassification"),
                    config.createLiteralMapping(xsdString, "level5"));
        }

        {
            Table table = new Table(schema, "biotherapeutic_components");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasBioComponent"),
                    config.createIriMapping("chembl:biocomponent", "component_id"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:biocomponent", "component_id"),
                    config.createIriMapping("cco:hasMolecule"), subject);
        }

        {
            Table table = new Table(schema, "molecule_hrac_classification");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, new Table(schema, "hrac_classification"), "hrac_class_id", "hrac_class_id",
                    graph, subject, config.createIriMapping("cco:hracClassification"),
                    config.createLiteralMapping(xsdString, "hrac_code"));
        }

        {
            Table table = new Table(schema, "molecule_irac_classification");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, new Table(schema, "irac_classification"), "irac_class_id", "irac_class_id",
                    graph, subject, config.createIriMapping("cco:iracClassification"),
                    config.createLiteralMapping(xsdString, "level2"));
        }

        {
            Table table = new Table(schema, "molecule_frac_classification");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, new Table(schema, "frac_classification"), "frac_class_id", "frac_class_id",
                    graph, subject, config.createIriMapping("cco:fracClassification"),
                    config.createLiteralMapping(xsdString, "level2"));
        }

        {
            Table table = new Table(schema, "molecule_docs");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasDocument"),
                    config.createIriMapping("chembl:document", "document_id"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:document", "document_id"),
                    config.createIriMapping("cco:hasMolecule"), subject);
        }

        {
            Table table = new Table(schema, "compound_properties");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_cx_most_apka", "molecule_id"), "cx_most_apka is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_most_apka", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000195"),
                    "cx_most_apka is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_most_apka", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "cx_most_apka"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_most_apka", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' ChemAxon Most Acidic pKa: ' || rtrim(rtrim(cast(cx_most_apka as text), '0'), '.'))"),
                    "cx_most_apka is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_cx_most_bpka", "molecule_id"), "cx_most_bpka is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_most_bpka", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000195"),
                    "cx_most_bpka is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_most_bpka", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "cx_most_bpka"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_most_bpka", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' ChemAxon Most Basic pKa: ' || rtrim(rtrim(cast(cx_most_bpka as text), '0'), '.'))"),
                    "cx_most_bpka is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_cx_logd", "molecule_id"), "cx_logd is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_logd", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000322"),
                    "cx_logd is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_logd", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "cx_logd"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_logd", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' ChemAxon LogD: ' || rtrim(rtrim(cast(cx_logd as text), '0'), '.'))"),
                    "cx_logd is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_cx_logp", "molecule_id"), "cx_logp is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_logp", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000251"),
                    "cx_logp is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_logp", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "cx_logp"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_cx_logp", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' ChemAxon LogP: ' || rtrim(rtrim(cast(cx_logp as text), '0'), '.'))"),
                    "cx_logp is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_alogp", "molecule_id"), "alogp is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_alogp", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000251"),
                    "alogp is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_alogp", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "alogp"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_alogp", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' ALogP: ' || rtrim(rtrim(cast(alogp as text), '0'), '.'))"),
                    "alogp is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_aromatic_rings", "molecule_id"),
                    "aromatic_rings is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_aromatic_rings", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000381"),
                    "aromatic_rings is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_aromatic_rings", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "aromatic_rings"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_aromatic_rings", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Aromatic Rings: ' || aromatic_rings)"),
                    "aromatic_rings is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_hba", "molecule_id"), "hba is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_hba", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000245"),
                    "hba is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_hba", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "hba"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_hba", "molecule_id"),
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Hydrogen Bond Acceptors: ' || hba)"),
                    "hba is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_hbd", "molecule_id"), "hbd is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_hbd", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000244"),
                    "hbd is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_hbd", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "hbd"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_hbd", "molecule_id"),
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Hydrogen Bond Donors: ' || hbd)"),
                    "hbd is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_heavy_atoms", "molecule_id"), "heavy_atoms is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_heavy_atoms", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000300"),
                    "heavy_atoms is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_heavy_atoms", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "heavy_atoms"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_heavy_atoms", "molecule_id"),
                    config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Heavy Atoms: ' || heavy_atoms)"),
                    "heavy_atoms is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_num_ro5_violations", "molecule_id"),
                    "num_ro5_violations is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_num_ro5_violations", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000312"),
                    "num_ro5_violations is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_num_ro5_violations", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "num_ro5_violations"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_num_ro5_violations", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' RO5 Violations: ' || num_ro5_violations)"),
                    "num_ro5_violations is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_psa", "molecule_id"), "psa is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_psa", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000307"),
                    "psa is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_psa", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "psa"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_psa", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Polar Surface Area: ' || rtrim(rtrim(cast(psa as text), '0'), '.'))"),
                    "psa is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_qed_weighted", "molecule_id"), "qed_weighted is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_qed_weighted", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000431"),
                    "qed_weighted is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_qed_weighted", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "qed_weighted"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_qed_weighted", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' QED Weighted Score: ' || rtrim(rtrim(cast(qed_weighted as text), '0'), '.'))"),
                    "qed_weighted is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_rtb", "molecule_id"), "rtb is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_rtb", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000254"),
                    "rtb is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_rtb", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "rtb"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_rtb", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "('CHEMBL' || molecule_id || ' Rotatable Bonds: ' || rtb)"),
                    "rtb is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_mw_freebase", "molecule_id"), "mw_freebase is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_mw_freebase", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000350"),
                    "mw_freebase is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_mw_freebase", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "mw_freebase"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_mw_freebase", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Freebase Molecular Weight: ' || rtrim(rtrim(cast(mw_freebase as text), '0'), '.'))"),
                    "mw_freebase is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_mw_monoisotopic", "molecule_id"),
                    "mw_monoisotopic is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_mw_monoisotopic", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000218"),
                    "mw_monoisotopic is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_mw_monoisotopic", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdDouble, "mw_monoisotopic"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_mw_monoisotopic", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Monoisotopic Mass: ' || rtrim(rtrim(cast(mw_monoisotopic as text), '0'), '.'))"),
                    "mw_monoisotopic is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_full_mwt", "molecule_id"), "full_mwt is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_full_mwt", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000216"),
                    "full_mwt is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_full_mwt", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdDouble, "full_mwt"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_full_mwt", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Full Molecular Weight: ' || rtrim(rtrim(cast(full_mwt as text), '0'), '.'))"),
                    "full_mwt is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_molecular_species", "molecule_id"),
                    "molecular_species is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_molecular_species", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000326"),
                    "molecular_species is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_molecular_species", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdString, "molecular_species"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_molecular_species", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Molecular Species: ' || molecular_species)"),
                    "molecular_species is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_ro3_pass", "molecule_id"), "ro3_pass is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_ro3_pass", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000315"),
                    "ro3_pass is not null");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_ro3_pass", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"), config.createLiteralMapping(xsdString, "ro3_pass"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule_ro3_pass", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "('CHEMBL' || molecule_id || ' RO3 Pass: ' || ro3_pass)"),
                    "ro3_pass is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_full_molformula", "molecule_id"),
                    "full_molformula is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_full_molformula", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000042"),
                    "full_molformula is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_full_molformula", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdString, "full_molformula"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_full_molformula", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Molecular Formula: ' || full_molformula)"),
                    "full_molformula is not null");
        }

        {
            Table table = new Table(schema, "compound_structures");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_standard_inchi_key", "molecule_id"),
                    "standard_inchi_key is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_standard_inchi_key", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000059"),
                    "standard_inchi_key is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_standard_inchi_key", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdString, "standard_inchi_key"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_standard_inchi_key", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString,
                            "('CHEMBL' || molecule_id || ' Standard InChi Key: ' || standard_inchi_key)"),
                    "standard_inchi_key is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_standard_inchi", "molecule_id"),
                    "standard_inchi is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_standard_inchi", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000113"),
                    "standard_inchi is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_standard_inchi", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdString, "standard_inchi"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_standard_inchi", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "('CHEMBL' || molecule_id || ' Standard InChi')"),
                    "standard_inchi is not null");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cheminf:SIO_000008"),
                    config.createIriMapping("chembl:molecule_canonical_smiles", "molecule_id"),
                    "canonical_smiles is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_canonical_smiles", "molecule_id"),
                    config.createIriMapping("rdf:type"), config.createIriMapping("cheminf:CHEMINF_000018"),
                    "canonical_smiles is not null");
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_canonical_smiles", "molecule_id"),
                    config.createIriMapping("cheminf:SIO_000300"),
                    config.createLiteralMapping(xsdString, "canonical_smiles"));
            config.addQuadMapping(table, graph,
                    config.createIriMapping("chembl:molecule_canonical_smiles", "molecule_id"),
                    config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "('CHEMBL' || molecule_id || ' Canonical Smiles')"),
                    "canonical_smiles is not null");
        }

        {
            Table table = new Table(schema, "molecule_hierarchy");
            NodeMapping subject = config.createIriMapping("chembl:molecule", "molecule_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasParentMolecule"),
                    config.createIriMapping("chembl:molecule", "parent_molecule_id"),
                    "molecule_id != parent_molecule_id");
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:molecule", "parent_molecule_id"),
                    config.createIriMapping("cco:hasChildMolecule"), subject, "molecule_id != parent_molecule_id");
        }

        // extension
        {
            Table table = new Table("molecules", "chembl");
            NodeMapping subject = config.createIriMapping("chembl:molfile", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_011120"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping("chembl:molecule", "id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdString, "molfile"));
        }
    }
}
