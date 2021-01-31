package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Compound
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID";

        config.addIriClass(new IntegerUserIriClass("pubchem:compound", "integer", prefix));
        config.addIriClass(new IntegerUserIriClass("pubchem:molfile", "integer", prefix, "_Molfile"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:compound");

        {
            Table table = new Table(schema, "compound_bases");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_010004"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Compound.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("pubchem/Compound.vm"));
        }

        {
            Table table = new Table("molecules", "pubchem");
            NodeMapping subject = config.createIriMapping("pubchem:molfile", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:SIO_011120"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping("pubchem:compound", "id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdString, "molfile"));
        }

        {
            Table table = new Table(schema, "compound_components");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:CHEMINF_000480"),
                    config.createIriMapping("pubchem:compound", "component"));
        }

        {
            Table table = new Table(schema, "compound_isotopologues");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:CHEMINF_000455"),
                    config.createIriMapping("pubchem:compound", "isotopologue"));
        }

        {
            Table table = new Table(schema, "compound_parents");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:has_parent"),
                    config.createIriMapping("pubchem:compound", "parent"));
        }

        {
            Table table = new Table(schema, "compound_stereoisomers");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:CHEMINF_000461"),
                    config.createIriMapping("pubchem:compound", "isomer"));
        }

        {
            Table table = new Table(schema, "compound_same_connectivities");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:CHEMINF_000462"),
                    config.createIriMapping("pubchem:compound", "isomer"));
        }

        {
            Table table = new Table(schema, "compound_roles");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("obo:has-role"),
                    config.createIriMapping("ontology:resource", Ontology.unitUncategorized, "role_id"));
        }

        {
            Table table = new Table(schema, "compound_types");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology:resource", "type_unit", "type_id"));
        }

        {
            Table table = new Table(schema, "compound_active_ingredients");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("vocab:is_active_ingredient_of"),
                    config.createIriMapping("ontology:resource", "ingredient_unit", "ingredient_id"));
        }

        {
            Table table = new Table(schema, "descriptor_compound_bases");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:hydrogen_bond_acceptor_count", "compound"),
                    "hydrogen_bond_acceptor_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:tautomer_count", "compound"), "tautomer_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:defined_atom_stereo_count", "compound"),
                    "defined_atom_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:defined_bond_stereo_count", "compound"),
                    "defined_bond_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:undefined_bond_stereo_count", "compound"),
                    "undefined_bond_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:isotope_atom_count", "compound"),
                    "isotope_atom_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:covalent_unit_count", "compound"),
                    "covalent_unit_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:hydrogen_bond_donor_count", "compound"),
                    "hydrogen_bond_donor_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:non_hydrogen_atom_count", "compound"),
                    "non_hydrogen_atom_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:rotatable_bond_count", "compound"),
                    "rotatable_bond_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:undefined_atom_stereo_count", "compound"),
                    "undefined_atom_stereo_count is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:total_formal_charge", "compound"),
                    "total_formal_charge is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:structure_complexity", "compound"),
                    "structure_complexity is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:mono_isotopic_weight", "compound"),
                    "mono_isotopic_weight is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:xlogp3_aa", "compound"), "xlogp3_aa is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:xlogp3", "compound"), "xlogp3 is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:exact_mass", "compound"), "exact_mass is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:molecular_weight", "compound"), "molecular_weight is not null");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:tpsa", "compound"), "tpsa is not null");
        }

        {
            Table table = new Table(schema, "descriptor_compound_molecular_formulas");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:molecular_formula", "compound"));
        }

        {
            Table table = new Table(schema, "descriptor_compound_isomeric_smileses");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:isomeric_smiles", "compound"));
        }

        {
            Table table = new Table(schema, "descriptor_compound_canonical_smileses");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:canonical_smiles", "compound"));
        }

        {
            Table table = new Table(schema, "descriptor_compound_iupac_inchis");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:iupac_inchi", "compound"));
        }

        {
            Table table = new Table(schema, "descriptor_compound_preferred_iupac_names");
            NodeMapping subject = config.createIriMapping("pubchem:compound", "compound");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-attribute"),
                    config.createIriMapping("pubchem:preferred_iupac_name", "compound"));
        }
    }
}
