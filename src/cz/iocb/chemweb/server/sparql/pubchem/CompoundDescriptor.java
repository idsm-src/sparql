package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class CompoundDescriptor extends PubChemMapping
{
    static IriClass descriptorHydrogenBondAcceptorCount;
    static IriClass descriptorTautomerCount;
    static IriClass descriptorDefinedAtomStereoCount;
    static IriClass descriptorDefinedBondStereoCount;
    static IriClass descriptorUndefinedBondStereoCount;
    static IriClass descriptorIsotopeAtomCount;
    static IriClass descriptorCovalentUnitCount;
    static IriClass descriptorHydrogenBondDonorCount;
    static IriClass descriptorNonHydrogenAtomCount;
    static IriClass descriptorRotatableBondCount;
    static IriClass descriptorUndefinedAtomStereoCount;
    static IriClass descriptorTotalFormalCharge;
    static IriClass descriptorStructureComplexity;
    static IriClass descriptorMonoIsotopicWeight;
    static IriClass descriptorXLogP3AA;
    static IriClass descriptorExactMass;
    static IriClass descriptorMolecularWeight;
    static IriClass descriptorTPSA;
    static IriClass descriptorMolecularFormula;
    static IriClass descriptorIsomericSmiles;
    static IriClass descriptorCanonicalSmiles;
    static IriClass descriptorIupacInChI;
    static IriClass descriptorPreferredIupacName;


    static void loadClasses()
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/CID[0-9]+_";

        classmap(descriptorHydrogenBondAcceptorCount = new IriClass("hydrogen_bond_acceptor_count",
                Arrays.asList("integer"), prefix + "Hydrogen_Bond_Acceptor_Count"));
        classmap(descriptorTautomerCount = new IriClass("tautomer_count", Arrays.asList("integer"),
                prefix + "Tautomer_Count"));
        classmap(descriptorDefinedAtomStereoCount = new IriClass("defined_atom_stereo_count", Arrays.asList("integer"),
                prefix + "Defined_Atom_Stereo_Count"));
        classmap(descriptorDefinedBondStereoCount = new IriClass("defined_bond_stereo_count", Arrays.asList("integer"),
                prefix + "Defined_Bond_Stereo_Count"));
        classmap(descriptorUndefinedBondStereoCount = new IriClass("undefined_bond_stereo_count",
                Arrays.asList("integer"), prefix + "Undefined_Bond_Stereo_Count"));
        classmap(descriptorIsotopeAtomCount = new IriClass("isotope_atom_count", Arrays.asList("integer"),
                prefix + "Isotope_Atom_Count"));
        classmap(descriptorCovalentUnitCount = new IriClass("covalent_unit_count", Arrays.asList("integer"),
                prefix + "Covalent_Unit_Count"));
        classmap(descriptorHydrogenBondDonorCount = new IriClass("hydrogen_bond_donor_count", Arrays.asList("integer"),
                prefix + "Hydrogen_Bond_Donor_Count"));
        classmap(descriptorNonHydrogenAtomCount = new IriClass("non_hydrogen_atom_count", Arrays.asList("integer"),
                prefix + "Non-hydrogen_Atom_Count"));
        classmap(descriptorRotatableBondCount = new IriClass("rotatable_bond_count", Arrays.asList("integer"),
                prefix + "Rotatable_Bond_Count"));
        classmap(descriptorUndefinedAtomStereoCount = new IriClass("undefined_atom_stereo_count",
                Arrays.asList("integer"), prefix + "Undefined_Atom_Stereo_Count"));
        classmap(descriptorTotalFormalCharge = new IriClass("total_formal_charge", Arrays.asList("integer"),
                prefix + "Total_Formal_Charge"));
        classmap(descriptorStructureComplexity = new IriClass("structure_complexity", Arrays.asList("integer"),
                prefix + "Structure_Complexity"));
        classmap(descriptorMonoIsotopicWeight = new IriClass("mono_isotopic_weight", Arrays.asList("integer"),
                prefix + "Mono_Isotopic_Weight"));
        classmap(descriptorXLogP3AA = new IriClass("xlogp3_aa", Arrays.asList("integer"), prefix + "XLogP3-AA"));
        classmap(descriptorExactMass = new IriClass("exact_mass", Arrays.asList("integer"), prefix + "Exact_Mass"));
        classmap(descriptorMolecularWeight = new IriClass("molecular_weight", Arrays.asList("integer"),
                prefix + "Molecular_Weight"));
        classmap(descriptorTPSA = new IriClass("tpsa", Arrays.asList("integer"), prefix + "TPSA"));
        classmap(descriptorMolecularFormula = new IriClass("molecular_formula", Arrays.asList("integer"),
                prefix + "Molecular_Formula"));
        classmap(descriptorIsomericSmiles = new IriClass("isomeric_smiles", Arrays.asList("integer"),
                prefix + "Isomeric_SMILES"));
        classmap(descriptorCanonicalSmiles = new IriClass("canonical_smiles", Arrays.asList("integer"),
                prefix + "Canonical_SMILES"));
        classmap(descriptorIupacInChI = new IriClass("iupac_inchi", Arrays.asList("integer"), prefix + "IUPAC_InChI"));
        classmap(descriptorPreferredIupacName = new IriClass("preferred_iupac_name", Arrays.asList("integer"),
                prefix + "Preferred_IUPAC_Name"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("descriptor:compound");
        NodeMapping type = iri("rdf:type");
        NodeMapping template = iri("template:itemTemplate");
        NodeMapping value = iri("sio:has-value");
        NodeMapping unit = iri("sio:has-unit");
        String directory = "pubchem/descriptor/";


        {
            String table = "descriptor_compound_bases";
            String field = "hydrogen_bond_acceptor_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorHydrogenBondAcceptorCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000388"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tautomer_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorTautomerCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000391"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_atom_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorDefinedAtomStereoCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000370"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "defined_bond_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorDefinedBondStereoCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000371"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_bond_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorUndefinedBondStereoCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000375"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "isotope_atom_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorIsotopeAtomCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000372"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "covalent_unit_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorCovalentUnitCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000369"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "hydrogen_bond_donor_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorHydrogenBondDonorCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000387"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "non_hydrogen_atom_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorNonHydrogenAtomCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000373"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "rotatable_bond_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorRotatableBondCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000389"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "undefined_atom_stereo_count";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorUndefinedAtomStereoCount, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000374"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "total_formal_charge";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorTotalFormalCharge, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000336"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdInteger, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "structure_complexity";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorStructureComplexity, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000390"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "mono_isotopic_weight";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorMonoIsotopicWeight, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000337"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdFloat, field));
            quad(table, graph, subject, unit, iri("obo:UO_0000055"), condition);
        }

        {
            String table = "descriptor_compound_bases";
            String field = "xlogp3_aa";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorXLogP3AA, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000395"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdFloat, field));
        }

        {
            String table = "descriptor_compound_bases";
            String field = "exact_mass";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorExactMass, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000338"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdFloat, field));
            quad(table, graph, subject, unit, iri("obo:UO_0000055"), condition);
        }

        {
            String table = "descriptor_compound_bases";
            String field = "molecular_weight";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorMolecularWeight, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000334"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdFloat, field));
            quad(table, graph, subject, unit, iri("obo:UO_0000055"), condition);
        }

        {
            String table = "descriptor_compound_bases";
            String field = "tpsa";
            String condition = field + " is not null";
            NodeMapping subject = iri(descriptorTPSA, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000392"), condition);
            quad(table, graph, subject, template, literal(directory + field + ".vm"), condition);
            quad(table, graph, subject, value, literal(xsdFloat, field));
            quad(table, graph, subject, unit, iri("obo:UO_0000324"), condition);
        }

        {
            String table = "descriptor_compound_molecular_formulas";
            String field = "molecular_formula";
            NodeMapping subject = iri(descriptorMolecularFormula, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000335"));
            quad(table, graph, subject, template, literal(directory + field + ".vm"));
            quad(table, graph, subject, value, literal(xsdString, field));
        }

        {
            String table = "descriptor_compound_isomeric_smileses";
            String field = "isomeric_smiles";
            NodeMapping subject = iri(descriptorIsomericSmiles, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000379"));
            quad(table, graph, subject, template, literal(directory + field + ".vm"));
            quad(table, graph, subject, value, literal(xsdString, field));
        }

        {
            String table = "descriptor_compound_canonical_smileses";
            String field = "canonical_smiles";
            NodeMapping subject = iri(descriptorCanonicalSmiles, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000376"));
            quad(table, graph, subject, template, literal(directory + field + ".vm"));
            quad(table, graph, subject, value, literal(xsdString, field));
        }

        {
            String table = "descriptor_compound_iupac_inchis";
            String field = "iupac_inchi";
            NodeMapping subject = iri(descriptorIupacInChI, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000396"));
            quad(table, graph, subject, template, literal(directory + field + ".vm"));
            quad(table, graph, subject, value, literal(xsdString, field));
        }

        {
            String table = "descriptor_compound_preferred_iupac_names";
            String field = "preferred_iupac_name";
            NodeMapping subject = iri(descriptorPreferredIupacName, "compound");

            quad(table, graph, subject, type, iri("sio:CHEMINF_000382"));
            quad(table, graph, subject, template, literal(directory + field + ".vm"));
            quad(table, graph, subject, value, literal(xsdString, field));
        }
    }
}
