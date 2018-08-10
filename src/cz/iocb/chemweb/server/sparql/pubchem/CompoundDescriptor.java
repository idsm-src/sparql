package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class CompoundDescriptor
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/CID[0-9]+_";

        config.addIriClass(new IriClass("hydrogen_bond_acceptor_count", Arrays.asList("integer"),
                prefix + "Hydrogen_Bond_Acceptor_Count"));
        config.addIriClass(new IriClass("tautomer_count", Arrays.asList("integer"), prefix + "Tautomer_Count"));
        config.addIriClass(new IriClass("defined_atom_stereo_count", Arrays.asList("integer"),
                prefix + "Defined_Atom_Stereo_Count"));
        config.addIriClass(new IriClass("defined_bond_stereo_count", Arrays.asList("integer"),
                prefix + "Defined_Bond_Stereo_Count"));
        config.addIriClass(new IriClass("undefined_bond_stereo_count", Arrays.asList("integer"),
                prefix + "Undefined_Bond_Stereo_Count"));
        config.addIriClass(new IriClass("isotope_atom_count", Arrays.asList("integer"), prefix + "Isotope_Atom_Count"));
        config.addIriClass(
                new IriClass("covalent_unit_count", Arrays.asList("integer"), prefix + "Covalent_Unit_Count"));
        config.addIriClass(new IriClass("hydrogen_bond_donor_count", Arrays.asList("integer"),
                prefix + "Hydrogen_Bond_Donor_Count"));
        config.addIriClass(
                new IriClass("non_hydrogen_atom_count", Arrays.asList("integer"), prefix + "Non-hydrogen_Atom_Count"));
        config.addIriClass(
                new IriClass("rotatable_bond_count", Arrays.asList("integer"), prefix + "Rotatable_Bond_Count"));
        config.addIriClass(new IriClass("undefined_atom_stereo_count", Arrays.asList("integer"),
                prefix + "Undefined_Atom_Stereo_Count"));
        config.addIriClass(
                new IriClass("total_formal_charge", Arrays.asList("integer"), prefix + "Total_Formal_Charge"));
        config.addIriClass(
                new IriClass("structure_complexity", Arrays.asList("integer"), prefix + "Structure_Complexity"));
        config.addIriClass(
                new IriClass("mono_isotopic_weight", Arrays.asList("integer"), prefix + "Mono_Isotopic_Weight"));
        config.addIriClass(new IriClass("xlogp3_aa", Arrays.asList("integer"), prefix + "XLogP3-AA"));
        config.addIriClass(new IriClass("exact_mass", Arrays.asList("integer"), prefix + "Exact_Mass"));
        config.addIriClass(new IriClass("molecular_weight", Arrays.asList("integer"), prefix + "Molecular_Weight"));
        config.addIriClass(new IriClass("tpsa", Arrays.asList("integer"), prefix + "TPSA"));
        config.addIriClass(new IriClass("molecular_formula", Arrays.asList("integer"), prefix + "Molecular_Formula"));
        config.addIriClass(new IriClass("isomeric_smiles", Arrays.asList("integer"), prefix + "Isomeric_SMILES"));
        config.addIriClass(new IriClass("canonical_smiles", Arrays.asList("integer"), prefix + "Canonical_SMILES"));
        config.addIriClass(new IriClass("iupac_inchi", Arrays.asList("integer"), prefix + "IUPAC_InChI"));
        config.addIriClass(
                new IriClass("preferred_iupac_name", Arrays.asList("integer"), prefix + "Preferred_IUPAC_Name"));
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
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("hydrogen_bond_acceptor_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000388"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tautomer_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("tautomer_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000391"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_atom_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("defined_atom_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000370"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_bond_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("defined_bond_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000371"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_bond_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("undefined_bond_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000375"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "isotope_atom_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("isotope_atom_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000372"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "covalent_unit_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("covalent_unit_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000369"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "hydrogen_bond_donor_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("hydrogen_bond_donor_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000387"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "non_hydrogen_atom_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("non_hydrogen_atom_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000373"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "rotatable_bond_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("rotatable_bond_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000389"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_atom_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("undefined_atom_stereo_count", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000374"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "total_formal_charge";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("total_formal_charge", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000336"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "structure_complexity";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("structure_complexity", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000390"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "mono_isotopic_weight";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("mono_isotopic_weight", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000337"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"), condition);
        }

        {
            String table = "descriptor_compound_bases";
            String field = "xlogp3_aa";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("xlogp3_aa", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000395"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "exact_mass";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("exact_mass", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000338"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"), condition);
        }

        {
            String table = "descriptor_compound_bases";
            String field = "molecular_weight";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("molecular_weight", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000334"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"), condition);
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tpsa";
            String condition = field + " is not null";
            NodeMapping subject = config.createIriMapping("tpsa", "compound");

            config.addQuadMapping(table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000392"),
                    condition);
            config.addQuadMapping(table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), condition);
            config.addQuadMapping(table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(table, graph, subject, unit, config.createIriMapping("obo:UO_0000324"), condition);
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
