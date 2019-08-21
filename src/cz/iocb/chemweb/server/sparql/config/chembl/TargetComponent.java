package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class TargetComponent
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass("targetcomponent", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/targetcomponent/CHEMBL_TC_(0|[1-9][0-9]*)"));

        config.addIriClass(
                new UserIriClass("uniprot", Arrays.asList("varchar"), "http://purl\\.uniprot\\.org/uniprot/[A-Z0-9]+"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass component = config.getIriClass("targetcomponent");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "component_sequences";
        NodeMapping subject = config.createIriMapping(component, "component_id");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:TargetComponent"));


        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("taxonomy", "tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ncbi_taxonomy", "tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("skos:exactMatch"),
                config.createIriMapping("uniprot", "accession"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "('CHEMBL_TC_' || component_id)"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('CHEMBL_TC_' || component_id)"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:organismName"),
                config.createLiteralMapping(xsdString, "organism"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:componentType"),
                config.createLiteralMapping(xsdString, "component_type"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:description"),
                config.createLiteralMapping(xsdString, "description"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:proteinSequence"),
                config.createLiteralMapping(xsdString, "sequence"));

        config.addQuadMapping("component_names", graph, subject, config.createIriMapping("skos:altLabel"),
                config.createLiteralMapping(xsdString, "component_name"));

        config.addQuadMapping(table, graph, config.createIriMapping("taxonomy", "tax_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(organism || ' (Identifiers.org)')"));

        config.addQuadMapping(table, graph, config.createIriMapping("ncbi_taxonomy", "tax_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(organism || ' (NCBI Taxonomy)')"));

        config.addQuadMapping(null, graph,
                config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl/chembl_25.0_targetcmpt_uniprot_ls.ttl>"),
                config.createIriMapping("void:inDataset"), config.createIriMapping(
                        "<http://rdf.ebi.ac.uk/dataset/chembl/25.0/void.ttl#chembl_targetcmpt_uniprot_linkset>"));
    }
}
