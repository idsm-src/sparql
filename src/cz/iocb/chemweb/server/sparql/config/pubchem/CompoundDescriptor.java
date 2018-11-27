package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class CompoundDescriptor
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/CID[0-9]+_";

        config.addIriClass(new UserIriClass("hydrogen_bond_acceptor_count", Arrays.asList("integer"),
                prefix + "Hydrogen_Bond_Acceptor_Count"));
        config.addIriClass(new UserIriClass("tautomer_count", Arrays.asList("integer"), prefix + "Tautomer_Count"));
        config.addIriClass(new UserIriClass("defined_atom_stereo_count", Arrays.asList("integer"),
                prefix + "Defined_Atom_Stereo_Count"));
        config.addIriClass(new UserIriClass("defined_bond_stereo_count", Arrays.asList("integer"),
                prefix + "Defined_Bond_Stereo_Count"));
        config.addIriClass(new UserIriClass("undefined_bond_stereo_count", Arrays.asList("integer"),
                prefix + "Undefined_Bond_Stereo_Count"));
        config.addIriClass(
                new UserIriClass("isotope_atom_count", Arrays.asList("integer"), prefix + "Isotope_Atom_Count"));
        config.addIriClass(
                new UserIriClass("covalent_unit_count", Arrays.asList("integer"), prefix + "Covalent_Unit_Count"));
        config.addIriClass(new UserIriClass("hydrogen_bond_donor_count", Arrays.asList("integer"),
                prefix + "Hydrogen_Bond_Donor_Count"));
        config.addIriClass(new UserIriClass("non_hydrogen_atom_count", Arrays.asList("integer"),
                prefix + "Non-hydrogen_Atom_Count"));
        config.addIriClass(
                new UserIriClass("rotatable_bond_count", Arrays.asList("integer"), prefix + "Rotatable_Bond_Count"));
        config.addIriClass(new UserIriClass("undefined_atom_stereo_count", Arrays.asList("integer"),
                prefix + "Undefined_Atom_Stereo_Count"));
        config.addIriClass(
                new UserIriClass("total_formal_charge", Arrays.asList("integer"), prefix + "Total_Formal_Charge"));
        config.addIriClass(
                new UserIriClass("structure_complexity", Arrays.asList("integer"), prefix + "Structure_Complexity"));
        config.addIriClass(
                new UserIriClass("mono_isotopic_weight", Arrays.asList("integer"), prefix + "Mono_Isotopic_Weight"));
        config.addIriClass(new UserIriClass("xlogp3_aa", Arrays.asList("integer"), prefix + "XLogP3-AA"));
        config.addIriClass(new UserIriClass("exact_mass", Arrays.asList("integer"), prefix + "Exact_Mass"));
        config.addIriClass(new UserIriClass("molecular_weight", Arrays.asList("integer"), prefix + "Molecular_Weight"));
        config.addIriClass(new UserIriClass("tpsa", Arrays.asList("integer"), prefix + "TPSA"));
        config.addIriClass(
                new UserIriClass("molecular_formula", Arrays.asList("integer"), prefix + "Molecular_Formula"));
        config.addIriClass(new UserIriClass("isomeric_smiles", Arrays.asList("integer"), prefix + "Isomeric_SMILES"));
        config.addIriClass(new UserIriClass("canonical_smiles", Arrays.asList("integer"), prefix + "Canonical_SMILES"));
        config.addIriClass(new UserIriClass("iupac_inchi", Arrays.asList("integer"), prefix + "IUPAC_InChI"));
        config.addIriClass(
                new UserIriClass("preferred_iupac_name", Arrays.asList("integer"), prefix + "Preferred_IUPAC_Name"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        NodeMapping graph = config.createIriMapping("descriptor:compound");
        NodeMapping type = config.createIriMapping("rdf:type");
        NodeMapping template = config.createIriMapping("template:itemTemplate");
        NodeMapping value = config.createIriMapping("sio:has-value");
        NodeMapping unit = config.createIriMapping("sio:has-unit");
        String directory = "pubchem/descriptor/";


        {
            String table = "descriptor_compound_bases";
            String field = "hydrogen_bond_acceptor_count";
            NodeMapping subject = config.createIriMapping("hydrogen_bond_acceptor_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000388"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tautomer_count";
            NodeMapping subject = config.createIriMapping("tautomer_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000391"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_atom_stereo_count";
            NodeMapping subject = config.createIriMapping("defined_atom_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000370"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_bond_stereo_count";
            NodeMapping subject = config.createIriMapping("defined_bond_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000371"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_bond_stereo_count";
            NodeMapping subject = config.createIriMapping("undefined_bond_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000375"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "isotope_atom_count";
            NodeMapping subject = config.createIriMapping("isotope_atom_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000372"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "covalent_unit_count";
            NodeMapping subject = config.createIriMapping("covalent_unit_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000369"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "hydrogen_bond_donor_count";
            NodeMapping subject = config.createIriMapping("hydrogen_bond_donor_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000387"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "non_hydrogen_atom_count";
            NodeMapping subject = config.createIriMapping("non_hydrogen_atom_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000373"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "rotatable_bond_count";
            NodeMapping subject = config.createIriMapping("rotatable_bond_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000389"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_atom_stereo_count";
            NodeMapping subject = config.createIriMapping("undefined_atom_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000374"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "total_formal_charge";
            NodeMapping subject = config.createIriMapping("total_formal_charge", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000336"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "structure_complexity";
            NodeMapping subject = config.createIriMapping("structure_complexity", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000390"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "mono_isotopic_weight";
            NodeMapping subject = config.createIriMapping("mono_isotopic_weight", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000337"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "xlogp3_aa";
            NodeMapping subject = config.createIriMapping("xlogp3_aa", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000395"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "exact_mass";
            NodeMapping subject = config.createIriMapping("exact_mass", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000338"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "molecular_weight";
            NodeMapping subject = config.createIriMapping("molecular_weight", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000334"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tpsa";
            NodeMapping subject = config.createIriMapping("tpsa", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000392"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000324"));
        }

        {
            String table = "descriptor_compound_molecular_formulas";
            String field = "molecular_formula";
            NodeMapping subject = config.createIriMapping("molecular_formula", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000335"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_isomeric_smileses";
            String field = "isomeric_smiles";
            NodeMapping subject = config.createIriMapping("isomeric_smiles", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000379"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_canonical_smileses";
            String field = "canonical_smiles";
            NodeMapping subject = config.createIriMapping("canonical_smiles", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000376"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_iupac_inchis";
            String field = "iupac_inchi";
            NodeMapping subject = config.createIriMapping("iupac_inchi", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000396"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_preferred_iupac_names";
            String field = "preferred_iupac_name";
            NodeMapping subject = config.createIriMapping("preferred_iupac_name", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000382"));
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(rdfLangStringEn, field));
        }
    }
}
