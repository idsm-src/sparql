package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



class CompoundDescriptor
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/CID";

        config.addIriClass(new IntegerUserIriClass("hydrogen_bond_acceptor_count", "integer", prefix,
                "Hydrogen_Bond_Acceptor_Count"));
        config.addIriClass(new IntegerUserIriClass("tautomer_count", "integer", prefix, "Tautomer_Count"));
        config.addIriClass(
                new IntegerUserIriClass("defined_atom_stereo_count", "integer", prefix, "Defined_Atom_Stereo_Count"));
        config.addIriClass(
                new IntegerUserIriClass("defined_bond_stereo_count", "integer", prefix, "Defined_Bond_Stereo_Count"));
        config.addIriClass(new IntegerUserIriClass("undefined_bond_stereo_count", "integer", prefix,
                "Undefined_Bond_Stereo_Count"));
        config.addIriClass(new IntegerUserIriClass("isotope_atom_count", "integer", prefix, "Isotope_Atom_Count"));
        config.addIriClass(new IntegerUserIriClass("covalent_unit_count", "integer", prefix, "Covalent_Unit_Count"));
        config.addIriClass(
                new IntegerUserIriClass("hydrogen_bond_donor_count", "integer", prefix, "Hydrogen_Bond_Donor_Count"));
        config.addIriClass(
                new IntegerUserIriClass("non_hydrogen_atom_count", "integer", prefix, "Non-hydrogen_Atom_Count"));
        config.addIriClass(new IntegerUserIriClass("rotatable_bond_count", "integer", prefix, "Rotatable_Bond_Count"));
        config.addIriClass(new IntegerUserIriClass("undefined_atom_stereo_count", "integer", prefix,
                "Undefined_Atom_Stereo_Count"));
        config.addIriClass(new IntegerUserIriClass("total_formal_charge", "integer", prefix, "Total_Formal_Charge"));
        config.addIriClass(new IntegerUserIriClass("structure_complexity", "integer", prefix, "Structure_Complexity"));
        config.addIriClass(new IntegerUserIriClass("mono_isotopic_weight", "integer", prefix, "Mono_Isotopic_Weight"));
        config.addIriClass(new IntegerUserIriClass("xlogp3_aa", "integer", prefix, "XLogP3-AA"));
        config.addIriClass(new IntegerUserIriClass("exact_mass", "integer", prefix, "Exact_Mass"));
        config.addIriClass(new IntegerUserIriClass("molecular_weight", "integer", prefix, "Molecular_Weight"));
        config.addIriClass(new IntegerUserIriClass("tpsa", "integer", prefix, "TPSA"));
        config.addIriClass(new IntegerUserIriClass("molecular_formula", "integer", prefix, "Molecular_Formula"));
        config.addIriClass(new IntegerUserIriClass("isomeric_smiles", "integer", prefix, "Isomeric_SMILES"));
        config.addIriClass(new IntegerUserIriClass("canonical_smiles", "integer", prefix, "Canonical_SMILES"));
        config.addIriClass(new IntegerUserIriClass("iupac_inchi", "integer", prefix, "IUPAC_InChI"));
        config.addIriClass(new IntegerUserIriClass("preferred_iupac_name", "integer", prefix, "Preferred_IUPAC_Name"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("descriptor:compound");
        ConstantIriMapping type = config.createIriMapping("rdf:type");
        ConstantIriMapping template = config.createIriMapping("template:itemTemplate");
        ConstantIriMapping value = config.createIriMapping("sio:has-value");
        ConstantIriMapping unit = config.createIriMapping("sio:has-unit");
        String directory = "pubchem/descriptor/";


        {
            String table = "descriptor_compound_bases";
            String field = "hydrogen_bond_acceptor_count";
            NodeMapping subject = config.createIriMapping("hydrogen_bond_acceptor_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000388"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tautomer_count";
            NodeMapping subject = config.createIriMapping("tautomer_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000391"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_atom_stereo_count";
            NodeMapping subject = config.createIriMapping("defined_atom_stereo_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000370"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_bond_stereo_count";
            NodeMapping subject = config.createIriMapping("defined_bond_stereo_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000371"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_bond_stereo_count";
            NodeMapping subject = config.createIriMapping("undefined_bond_stereo_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000375"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "isotope_atom_count";
            NodeMapping subject = config.createIriMapping("isotope_atom_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000372"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "covalent_unit_count";
            NodeMapping subject = config.createIriMapping("covalent_unit_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000369"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "hydrogen_bond_donor_count";
            NodeMapping subject = config.createIriMapping("hydrogen_bond_donor_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000387"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "non_hydrogen_atom_count";
            NodeMapping subject = config.createIriMapping("non_hydrogen_atom_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000373"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "rotatable_bond_count";
            NodeMapping subject = config.createIriMapping("rotatable_bond_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000389"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_atom_stereo_count";
            NodeMapping subject = config.createIriMapping("undefined_atom_stereo_count", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000374"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "total_formal_charge";
            NodeMapping subject = config.createIriMapping("total_formal_charge", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000336"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdShort, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "structure_complexity";
            NodeMapping subject = config.createIriMapping("structure_complexity", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000390"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "mono_isotopic_weight";
            NodeMapping subject = config.createIriMapping("mono_isotopic_weight", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000337"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(schema, table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"),
                    field + " is not null");
        }

        {
            String table = "descriptor_compound_bases";
            String field = "xlogp3_aa";
            NodeMapping subject = config.createIriMapping("xlogp3_aa", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000395"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "exact_mass";
            NodeMapping subject = config.createIriMapping("exact_mass", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000338"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(schema, table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"),
                    field + " is not null");
        }

        {
            String table = "descriptor_compound_bases";
            String field = "molecular_weight";
            NodeMapping subject = config.createIriMapping("molecular_weight", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000334"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(schema, table, graph, subject, unit, config.createIriMapping("obo:UO_0000055"),
                    field + " is not null");
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tpsa";
            NodeMapping subject = config.createIriMapping("tpsa", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000392"),
                    field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"), field + " is not null");
            config.addQuadMapping(schema, table, graph, subject, value, config.createLiteralMapping(xsdFloat, field));
            config.addQuadMapping(schema, table, graph, subject, unit, config.createIriMapping("obo:UO_0000324"),
                    field + " is not null");
        }

        {
            String table = "descriptor_compound_molecular_formulas";
            String field = "molecular_formula";
            NodeMapping subject = config.createIriMapping("molecular_formula", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000335"));
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(schema, table, graph, subject, value,
                    config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_isomeric_smileses";
            String field = "isomeric_smiles";
            NodeMapping subject = config.createIriMapping("isomeric_smiles", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000379"));
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(schema, table, graph, subject, value,
                    config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_canonical_smileses";
            String field = "canonical_smiles";
            NodeMapping subject = config.createIriMapping("canonical_smiles", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000376"));
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(schema, table, graph, subject, value,
                    config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_iupac_inchis";
            String field = "iupac_inchi";
            NodeMapping subject = config.createIriMapping("iupac_inchi", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000396"));
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(schema, table, graph, subject, value,
                    config.createLiteralMapping(rdfLangStringEn, field));
        }

        {
            String table = "descriptor_compound_preferred_iupac_names";
            String field = "preferred_iupac_name";
            NodeMapping subject = config.createIriMapping("preferred_iupac_name", "compound");

            config.addQuadMapping(schema, table, graph, subject, type, config.createIriMapping("sio:CHEMINF_000382"));
            config.addQuadMapping(schema, table, graph, subject, template,
                    config.createLiteralMapping(directory + field + ".vm"));
            config.addQuadMapping(schema, table, graph, subject, value,
                    config.createLiteralMapping(rdfLangStringEn, field));
        }
    }
}
