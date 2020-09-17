package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Target
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("chembl:target", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/target/CHEMBL"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        {
            Table table = new Table(schema, "target_dictionary");
            NodeMapping subject = config.createIriMapping("chembl:target", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:SingleProtein"), "target_type = 'SINGLE PROTEIN'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:Organism"), "target_type = 'ORGANISM'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:CellLineTarget"), "target_type = 'CELL-LINE'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ProteinComplex"), "target_type = 'PROTEIN COMPLEX'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ProteinFamily"), "target_type = 'PROTEIN FAMILY'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:Tissue"), "target_type = 'TISSUE'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ProteinSelectivityGroup"), "target_type = 'SELECTIVITY GROUP'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ProteinProteinInteraction"),
                    "target_type = 'PROTEIN-PROTEIN INTERACTION'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ProteinComplexGroup"), "target_type = 'PROTEIN COMPLEX GROUP'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:NucleicAcid"), "target_type = 'NUCLEIC-ACID'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:SmallMoleculeTarget"), "target_type = 'SMALL MOLECULE'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:UnknownTarget"), "target_type = 'UNKNOWN'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ChimericProtein"), "target_type = 'CHIMERIC PROTEIN'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:Macromolecule"), "target_type = 'MACROMOLECULE'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:SubCellular"), "target_type = 'SUBCELLULAR'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:OligosaccharideTarget"), "target_type = 'OLIGOSACCHARIDE'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:UnclassifiedTarget"),
                    "target_type = 'LIPID' or target_type = 'UNCHECKED' or target_type = 'NO TARGET'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:Metal"), "target_type = 'METAL'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ProteinNucleicAcidComplex"),
                    "target_type = 'PROTEIN NUCLEIC-ACID COMPLEX'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:Phenotype"), "target_type = 'PHENOTYPE'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:NonMolecular"), "target_type = 'NON-MOLECULAR'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("cco:ADMET"), "target_type = 'ADMET'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                    config.createIriMapping("ontology:resource", Ontology.unitTaxonomy, "tax_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                    config.createIriMapping("reference:ncbi-taxonomy", "tax_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:isTargetForCellLine"),
                    config.createIriMapping("chembl:cell_line", "cell_line_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:isSpeciesGroup"),
                    config.createLiteralMapping(true), "species_group_flag");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                    config.createLiteralMapping(xsdString, "chembl_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "pref_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(xsdString, "pref_name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:targetType"),
                    config.createLiteralMapping(xsdString, "target_type"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:organismName"),
                    config.createLiteralMapping(xsdString, "organism"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:cell_line", "cell_line_id"),
                    config.createIriMapping("cco:isCellLineForTarget"), subject);
        }

        {
            Table table = new Table(schema, "target_relations");
            NodeMapping subject = config.createIriMapping("chembl:target", "target_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:relOverlapsWith"),
                    config.createIriMapping("chembl:target", "related_target_id"), "relationship = 'OVERLAPS WITH'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:relSubsetOf"),
                    config.createIriMapping("chembl:target", "related_target_id"), "relationship = 'SUBSET OF'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:relHasSubset"),
                    config.createIriMapping("chembl:target", "related_target_id"), "relationship = 'SUPERSET OF'");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:relEquivalentTo"),
                    config.createIriMapping("chembl:target", "related_target_id"), "relationship = 'EQUIVALENT TO'");
        }

        {
            Table table = new Table(schema, "target_components");
            NodeMapping subject = config.createIriMapping("chembl:target", "target_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasTargetComponent"),
                    config.createIriMapping("chembl:targetcomponent", "component_id"));
            config.addQuadMapping(table, graph, config.createIriMapping("chembl:targetcomponent", "component_id"),
                    config.createIriMapping("cco:hasTarget"), subject);
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                    config.createIriMapping("chembl:targetcomponent", "component_id"), "is_exact");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:relatedMatch"),
                    config.createIriMapping("chembl:targetcomponent", "component_id"), "is_related");
        }
    }
}
