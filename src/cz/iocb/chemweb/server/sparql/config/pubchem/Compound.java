package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Compound
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID[0-9]+";

        config.addIriClass(new UserIriClass("compound", Arrays.asList("integer"), prefix));
        config.addIriClass(new UserIriClass("compound_molfile", Arrays.asList("integer"), prefix + "_Molfile"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass compound = config.getIriClass("compound");
        NodeMapping graph = config.createIriMapping("pubchem:compound");

        {
            String table = "compound_bases";
            NodeMapping subject = config.createIriMapping(compound, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_010004"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Compound.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Compound.vm"));
        }

        {
            String table = "compounds";
            NodeMapping subject = config.createIriMapping("compound_molfile", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_011120"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping(compound, "id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdString, "molfile"));
        }

        {
            String table = "compound_relations";
            NodeMapping subject = config.createIriMapping(compound, "compound_from");

            config.addQuadMapping(table, graph, subject,
                    config.createIriMapping("ontology_resource", "relation_unit", "relation_id"),
                    config.createIriMapping(compound, "compound_to"));
        }

        {
            String table = "compound_roles";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:has-role"), config
                    .createIriMapping(config.getIriClass("ontology_resource"), Ontology.unitUncategorized, "role_id"));
        }

        {
            String table = "compound_biosystems";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping(config.getIriClass("biosystem"), "biosystem"));
        }

        {
            String table = "compound_types";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", "type_unit", "type_id"));
        }

        {
            String table = "compound_active_ingredients";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:is_active_ingredient_of"),
                    config.createIriMapping("ontology_resource", "ingredient_unit", "ingredient_id"));
        }

        {
            String table = "descriptor_compound_bases";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("hydrogen_bond_acceptor_count", "compound"),
                    "hydrogen_bond_acceptor_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("tautomer_count", "compound"), "tautomer_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("defined_atom_stereo_count", "compound"),
                    "defined_atom_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("defined_bond_stereo_count", "compound"),
                    "defined_bond_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("undefined_bond_stereo_count", "compound"),
                    "undefined_bond_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("isotope_atom_count", "compound"), "isotope_atom_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("covalent_unit_count", "compound"), "covalent_unit_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("hydrogen_bond_donor_count", "compound"),
                    "hydrogen_bond_donor_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("non_hydrogen_atom_count", "compound"),
                    "non_hydrogen_atom_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("rotatable_bond_count", "compound"), "rotatable_bond_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("undefined_atom_stereo_count", "compound"),
                    "undefined_atom_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("total_formal_charge", "compound"), "total_formal_charge is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("structure_complexity", "compound"), "structure_complexity is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("mono_isotopic_weight", "compound"), "mono_isotopic_weight is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("xlogp3_aa", "compound"), "xlogp3_aa is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("exact_mass", "compound"), "exact_mass is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("molecular_weight", "compound"), "molecular_weight is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("tpsa", "compound"), "tpsa is not null");
        }

        {
            String table = "descriptor_compound_molecular_formulas";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("molecular_formula", "compound"));
        }

        {
            String table = "descriptor_compound_isomeric_smileses";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("isomeric_smiles", "compound"));
        }

        {
            String table = "descriptor_compound_canonical_smileses";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("canonical_smiles", "compound"));
        }

        {
            String table = "descriptor_compound_iupac_inchis";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("iupac_inchi", "compound"));
        }

        {
            String table = "descriptor_compound_preferred_iupac_names";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("preferred_iupac_name", "compound"));
        }
    }
}
