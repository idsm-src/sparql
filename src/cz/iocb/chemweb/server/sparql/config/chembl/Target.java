package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass.SqlCheck;



class Target
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass("target", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/target/CHEMBL[1-9][0-9]*", SqlCheck.IF_MATCH));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass target = config.getIriClass("target");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "target_dictionary";
        NodeMapping subject = config.createIriMapping(target, "tid");

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
                config.createIriMapping("taxonomy", "tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ncbi_taxonomy", "tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:isTargetForCellLine"),
                config.createIriMapping("cell_line", "cell_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:isSpeciesGroup"),
                config.createLiteralMapping(true), "species_group_flag = 1::smallint");

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

        config.addQuadMapping(table, graph, config.createIriMapping("cell_line", "cell_id"),
                config.createIriMapping("cco:isCellLineForTarget"), subject);


        config.addQuadMapping("target_relations", graph, subject, config.createIriMapping("cco:relOverlapsWith"),
                config.createIriMapping("target", "related_tid"), "relationship = 'OVERLAPS WITH'");

        config.addQuadMapping("target_relations", graph, subject, config.createIriMapping("cco:relSubsetOf"),
                config.createIriMapping("target", "related_tid"), "relationship = 'SUBSET OF'");

        config.addQuadMapping("target_relations", graph, subject, config.createIriMapping("cco:relHasSubset"),
                config.createIriMapping("target", "related_tid"), "relationship = 'SUPERSET OF'");


        config.addQuadMapping("target_relations", graph, subject, config.createIriMapping("cco:relEquivalentTo"),
                config.createIriMapping("target", "related_tid"), "relationship = 'EQUIVALENT TO'");

        config.addQuadMapping("target_components", graph, subject, config.createIriMapping("cco:hasTargetComponent"),
                config.createIriMapping("targetcomponent", "component_id"));

        config.addQuadMapping("target_components", graph, config.createIriMapping("targetcomponent", "component_id"),
                config.createIriMapping("cco:hasTarget"), subject);

        config.addQuadMapping("target_components", graph, subject, config.createIriMapping("skos:exactMatch"),
                config.createIriMapping("targetcomponent", "component_id"),
                "exists(select true from target_dictionary where tid = target_components.tid and "
                        + "target_type = 'SINGLE PROTEIN')");

        config.addQuadMapping("target_components", graph, subject, config.createIriMapping("skos:relatedMatch"),
                config.createIriMapping("targetcomponent", "component_id"),
                "exists(select true from target_dictionary where tid = target_components.tid and "
                        + "target_type != 'NUCLEIC-ACID' and target_type != 'SINGLE PROTEIN')");


        config.addQuadMapping(null, graph,
                config.createIriMapping(
                        "<http://rdf.ebi.ac.uk/dataset/chembl/chembl_25.0_complextarget_targetcmpt_ls.ttl>"),
                config.createIriMapping("void:inDataset"), config.createIriMapping(
                        "<http://rdf.ebi.ac.uk/dataset/chembl/25.0/void.ttl#chembl_complextarget_targetcmpt_linkset>"));

        config.addQuadMapping(null, graph,
                config.createIriMapping(
                        "<http://rdf.ebi.ac.uk/dataset/chembl/chembl_25.0_grouptarget_targetcmpt_ls.ttl>"),
                config.createIriMapping("void:inDataset"), config.createIriMapping(
                        "<http://rdf.ebi.ac.uk/dataset/chembl/25.0/void.ttl#chembl_grouptarget_targetcmpt_linkset>"));

        config.addQuadMapping(null, graph,
                config.createIriMapping(
                        "<http://rdf.ebi.ac.uk/dataset/chembl/chembl_25.0_singletarget_targetcmpt_ls.ttl>"),
                config.createIriMapping("void:inDataset"), config.createIriMapping(
                        "<http://rdf.ebi.ac.uk/dataset/chembl/25.0/void.ttl#chembl_singletarget_targetcmpt_linkset>"));
    }
}
