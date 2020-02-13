package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class ProteinClassification
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("protclass", "bigint",
                "http://rdf.ebi.ac.uk/resource/chembl/protclass/CHEMBL_PC_"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass protclass = config.getIriClass("protclass");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "protein_classification";
        NodeMapping subject = config.createIriMapping(protclass, "protein_class_id");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:ProteinClassification"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "('CHEMBL_PC_' || protein_class_id)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "pref_name"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("skos:prefLabel"),
                config.createLiteralMapping(xsdString, "pref_name"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:classLevel"),
                config.createLiteralMapping(xsdString, "('L' || class_level)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("skos:broader"),
                config.createIriMapping("protclass", "parent_id"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                config.createIriMapping("protclass", "parent_id"));

        config.addQuadMapping(schema, table, graph, config.createIriMapping("protclass", "parent_id"),
                config.createIriMapping("skos:narrower"), subject);


        config.addQuadMapping(schema, "protein_classification_paths", graph, subject,
                config.createIriMapping("cco:classPath"), config.createLiteralMapping(xsdString, "path"));


        config.addQuadMapping(schema, "component_classes", graph, subject,
                config.createIriMapping("cco:hasTargetComponentDescendant"),
                config.createIriMapping("targetcomponent", "component_id"));

        config.addQuadMapping(schema, "component_classes", graph,
                config.createIriMapping("targetcomponent", "component_id"),
                config.createIriMapping("cco:hasProteinClassification"), subject);


        config.addQuadMapping(schema, "target_classes", graph, subject,
                config.createIriMapping("cco:hasTargetDescendant"), config.createIriMapping("target", "tid"));

        config.addQuadMapping(schema, "target_classes", graph, config.createIriMapping("target", "tid"),
                config.createIriMapping("cco:hasProteinClassification"), subject);
    }
}
