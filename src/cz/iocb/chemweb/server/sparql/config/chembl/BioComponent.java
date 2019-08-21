package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class BioComponent
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new UserIriClass("biocomponent", Arrays.asList("bigint"),
                "http://rdf\\.ebi\\.ac\\.uk/resource/chembl/biocomponent/CHEMBL_BC_(0|[1-9][0-9]*)"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass biocomponent = config.getIriClass("biocomponent");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "bio_component_sequences";
        NodeMapping subject = config.createIriMapping(biocomponent, "component_id");

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:BioComponent"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("taxonomy", "tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:taxonomy"),
                config.createIriMapping("ncbi_taxonomy", "tax_id"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "('CHEMBL_BC_' || component_id)"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('CHEMBL_BC_' || component_id)"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:componentType"),
                config.createLiteralMapping(xsdString, "component_type"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:proteinSequence"),
                config.createLiteralMapping(xsdString, "sequence"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:description"),
                config.createLiteralMapping(xsdString, "description"));

        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:organismName"),
                config.createLiteralMapping(xsdString, "organism"));

        config.addQuadMapping(table, graph, config.createIriMapping("taxonomy", "tax_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(organism || ' (Identifiers.org)')"));

        config.addQuadMapping(table, graph, config.createIriMapping("ncbi_taxonomy", "tax_id"),
                config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "(organism || ' (NCBI Taxonomy)')"));
    }
}
