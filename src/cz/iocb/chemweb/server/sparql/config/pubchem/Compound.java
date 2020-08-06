package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.molecules;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Compound
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID";

        config.addIriClass(new IntegerUserIriClass("compound", "integer", prefix));
        config.addIriClass(new IntegerUserIriClass("compound_molfile", "integer", prefix, "_Molfile"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass compound = config.getIriClass("compound");
        ConstantIriMapping graph = config.createIriMapping("pubchem:compound");

        {
            String table = "compound_bases";
            NodeMapping subject = config.createIriMapping(compound, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_010004"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Compound.vm"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Compound.vm"));
        }

        {
            String table = "pubchem";
            NodeMapping subject = config.createIriMapping("compound_molfile", "id");

            config.addQuadMapping(molecules, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_011120"));
            config.addQuadMapping(molecules, table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping(compound, "id"));
            config.addQuadMapping(molecules, table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdString, "molfile"));
        }

        {
            String table = "compound_components";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:CHEMINF_000480"),
                    config.createIriMapping(compound, "component"));
        }

        {
            String table = "compound_isotopologues";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:CHEMINF_000455"),
                    config.createIriMapping(compound, "isotopologue"));
        }

        {
            String table = "compound_parents";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("vocab:has_parent"),
                    config.createIriMapping(compound, "parent"));
        }

        {
            String table = "compound_stereoisomers";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:CHEMINF_000461"),
                    config.createIriMapping(compound, "isomer"));
        }

        {
            String table = "compound_same_connectivities";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:CHEMINF_000462"),
                    config.createIriMapping(compound, "isomer"));
        }

        {
            String table = "compound_roles";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("obo:has-role"), config
                    .createIriMapping(config.getIriClass("ontology_resource"), Ontology.unitUncategorized, "role_id"));
        }

        {
            String table = "compound_biosystems";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("obo:BFO_0000056"),
                    config.createIriMapping(config.getIriClass("biosystem"), "biosystem"));
        }

        {
            String table = "compound_types";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", "type_unit", "type_id"));
        }

        {
            String table = "compound_active_ingredients";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject,
                    config.createIriMapping("vocab:is_active_ingredient_of"),
                    config.createIriMapping("ontology_resource", "ingredient_unit", "ingredient_id"));
        }

        {
            String table = "descriptor_compound_bases";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("hydrogen_bond_acceptor_count", "compound"),
                    "hydrogen_bond_acceptor_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("tautomer_count", "compound"), "tautomer_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("defined_atom_stereo_count", "compound"),
                    "defined_atom_stereo_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("defined_bond_stereo_count", "compound"),
                    "defined_bond_stereo_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("undefined_bond_stereo_count", "compound"),
                    "undefined_bond_stereo_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("isotope_atom_count", "compound"), "isotope_atom_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("covalent_unit_count", "compound"), "covalent_unit_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("hydrogen_bond_donor_count", "compound"),
                    "hydrogen_bond_donor_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("non_hydrogen_atom_count", "compound"),
                    "non_hydrogen_atom_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("rotatable_bond_count", "compound"), "rotatable_bond_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("undefined_atom_stereo_count", "compound"),
                    "undefined_atom_stereo_count is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("total_formal_charge", "compound"), "total_formal_charge is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("structure_complexity", "compound"), "structure_complexity is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("mono_isotopic_weight", "compound"), "mono_isotopic_weight is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("xlogp3_aa", "compound"), "xlogp3_aa is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("xlogp3", "compound"), "xlogp3 is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("exact_mass", "compound"), "exact_mass is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("molecular_weight", "compound"), "molecular_weight is not null");
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("tpsa", "compound"), "tpsa is not null");
        }

        {
            String table = "descriptor_compound_molecular_formulas";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("molecular_formula", "compound"));
        }

        {
            String table = "descriptor_compound_isomeric_smileses";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("isomeric_smiles", "compound"));
        }

        {
            String table = "descriptor_compound_canonical_smileses";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("canonical_smiles", "compound"));
        }

        {
            String table = "descriptor_compound_iupac_inchis";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("iupac_inchi", "compound"));
        }

        {
            String table = "descriptor_compound_preferred_iupac_names";
            NodeMapping subject = config.createIriMapping(compound, "compound");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("preferred_iupac_name", "compound"));
        }
    }
}
