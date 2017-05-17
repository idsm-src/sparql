package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import static cz.iocb.chemweb.server.sparql.pubchem.Biosystem.biosystem;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorCanonicalSmiles;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorCovalentUnitCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorDefinedAtomStereoCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorDefinedBondStereoCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorExactMass;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorHydrogenBondAcceptorCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorHydrogenBondDonorCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorIsomericSmiles;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorIsotopeAtomCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorIupacInChI;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorMolecularFormula;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorMolecularWeight;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorMonoIsotopicWeight;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorNonHydrogenAtomCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorPreferredIupacName;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorRotatableBondCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorStructureComplexity;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorTPSA;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorTautomerCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorTotalFormalCharge;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorUndefinedAtomStereoCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorUndefinedBondStereoCount;
import static cz.iocb.chemweb.server.sparql.pubchem.CompoundDescriptor.descriptorXLogP3AA;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Compound extends PubChemMapping
{
    static IriClass compound;
    static IriClass compoundSdfile;


    static void loadClasses()
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID[0-9]+";

        classmap(compound = new IriClass("compound", 1, prefix));
        classmap(compoundSdfile = new IriClass("compound_sdfile", 1, prefix + "_SDfile"));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:compound");

        {
            String table = "compound_bases";
            NodeMapping subject = iri(compound, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("sio:SIO_010004"));
            quad(table, graph, subject, iri("template:itemTemplate"), literal("pubchem/Compound.vm"));
            quad(table, graph, subject, iri("template:pageTemplate"), literal("pubchem/Compound.vm"));
        }

        {
            String table = "compound_sdfiles";
            NodeMapping subject = iri(compoundSdfile, "compound");

            quad(table, graph, subject, iri("rdf:type"), iri("sio:SIO_011120"));
            quad(table, graph, subject, iri("sio:is-attribute-of"), iri(compound, "compound"));
            quad(table, graph, subject, iri("sio:has-value"), literal(xsdString, "sdf"));
        }

        {
            String table = "compound_relations";
            NodeMapping subject = iri(compound, "compound_from");

            //TODO: quad(table, graph, subject, iri(property, "relation"), iri(compound, "compound_to"));
        }

        {
            String table = "compound_biosystems";
            NodeMapping subject = iri(compound, "compound");

            quad(table, graph, subject, iri("obo:BFO_0000056"), iri(biosystem, "biosystem"));
        }

        {
            String table = "compound_types";
            NodeMapping subject = iri(compound, "compound");

            //TODO: quad(table, graph, subject, iri("rdf:type"), iri(rdfclass, "type"));
        }

        {
            String table = "compound_active_ingredients";
            NodeMapping subject = iri(compound, "compound");

            //TODO: quad(table, graph, subject, iri("vocab:is_active_ingredient_of"), iri(rdfclass, "ingredient"));
        }

        {
            String table = "descriptor_compound_bases";
            NodeMapping subject = iri(compound, "compound");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorHydrogenBondAcceptorCount, "compound"),
                    "hydrogen_bond_acceptor_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorTautomerCount, "compound"),
                    "tautomer_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorDefinedAtomStereoCount, "compound"),
                    "defined_atom_stereo_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorDefinedBondStereoCount, "compound"),
                    "defined_bond_stereo_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorUndefinedBondStereoCount, "compound"),
                    "undefined_bond_stereo_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorIsotopeAtomCount, "compound"),
                    "isotope_atom_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorCovalentUnitCount, "compound"),
                    "covalent_unit_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorHydrogenBondDonorCount, "compound"),
                    "hydrogen_bond_donor_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorNonHydrogenAtomCount, "compound"),
                    "non_hydrogen_atom_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorRotatableBondCount, "compound"),
                    "rotatable_bond_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorUndefinedAtomStereoCount, "compound"),
                    "undefined_atom_stereo_count is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorTotalFormalCharge, "compound"),
                    "total_formal_charge is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorStructureComplexity, "compound"),
                    "structure_complexity is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorMonoIsotopicWeight, "compound"),
                    "mono_isotopic_weight is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorXLogP3AA, "compound"),
                    "xlogp3_aa is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorExactMass, "compound"),
                    "exact_mass is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorMolecularWeight, "compound"),
                    "molecular_weight is not null");
            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorTPSA, "compound"), "tpsa is not null");
        }

        {
            String table = "descriptor_compound_molecular_formulas";
            NodeMapping subject = iri(compound, "compound");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorMolecularFormula, "compound"));
        }

        {
            String table = "descriptor_compound_isomeric_smileses";
            NodeMapping subject = iri(compound, "compound");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorIsomericSmiles, "compound"));
        }

        {
            String table = "descriptor_compound_canonical_smileses";
            NodeMapping subject = iri(compound, "compound");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorCanonicalSmiles, "compound"));
        }

        {
            String table = "descriptor_compound_iupac_inchis";
            NodeMapping subject = iri(compound, "compound");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorIupacInChI, "compound"));
        }

        {
            String table = "descriptor_compound_preferred_iupac_names";
            NodeMapping subject = iri(compound, "compound");

            quad(table, graph, subject, iri("sio:has-attribute"), iri(descriptorPreferredIupacName, "compound"));
        }
    }
}
