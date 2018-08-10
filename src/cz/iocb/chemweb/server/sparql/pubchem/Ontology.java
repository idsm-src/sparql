package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Ontology
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new IriClass("class", Arrays.asList("integer"), config.getIriValues("class_bases")));
        config.addIriClass(new IriClass("property", Arrays.asList("integer"), config.getIriValues("property_bases")));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        IriClass rdfclass = config.getIriClass("class");
        IriClass property = config.getIriClass("property");
        NodeMapping graph = config.createIriMapping("pubchem:ontology");

        {
            String table = "class_bases";
            NodeMapping subject = config.createIriMapping(rdfclass, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Class"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/Class.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("base/Class.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(rdfLangStringEn, "label"));
        }

        {
            String table = "class_superclasses";
            NodeMapping subject = config.createIriMapping(rdfclass, "class");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping(rdfclass, "superclass"));
        }

        {
            String table = "property_bases";
            NodeMapping subject = config.createIriMapping(property, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("rdf:Property"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/Property.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("base/Property.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(rdfLangStringEn, "label"));
        }

        {
            String table = "property_superproperties";
            NodeMapping subject = config.createIriMapping(property, "property");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subPropertyOf"),
                    config.createIriMapping(property, "superproperty"));
        }

        {
            String table = "property_domains";
            NodeMapping subject = config.createIriMapping(property, "property");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:domain"),
                    config.createIriMapping(rdfclass, "domain"));
        }

        {
            String table = "property_ranges";
            NodeMapping subject = config.createIriMapping(property, "property");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:range"),
                    config.createIriMapping(rdfclass, "range"));
        }
    }
}
