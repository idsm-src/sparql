package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Ontology
{
    public static final String unitUncategorized = "0::smallint";
    public static final String unitBlank = "1::smallint";
    public static final String unitSIO = "2::smallint";
    public static final String unitCHEMINF = "3::smallint";
    public static final String unitBAO = "4::smallint";
    public static final String unitGO = "5::smallint";
    public static final String unitPR = "6::smallint";
    public static final String unitCHEBI = "7::smallint";
    public static final String unitTaxonomy = "11::smallint";


    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("ontology_resource", Arrays.asList("smallint", "integer"),
                config.getPattern("ontology_resource_categories__reftable"),
                config.getIriValues("ontology_resources__reftable")));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass rdfResource = config.getIriClass("ontology_resource");
        NodeMapping graph = config.createIriMapping("pubchem:ontology");

        {
            String table = "ontology_resource_classes";
            NodeMapping subject = config.createIriMapping(rdfResource, "class_unit", "class_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Class"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/Class.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("base/Class.vm"));
        }

        {
            String table = "ontology_resource_properties";
            NodeMapping subject = config.createIriMapping(rdfResource, "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("rdf:Property"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/Property.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:pageTemplate"),
                    config.createLiteralMapping("base/Property.vm"));
        }

        {
            String table = "ontology_resource_individuals";
            NodeMapping subject = config.createIriMapping(rdfResource, "individual_unit", "individual_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:NamedIndividual"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("base/NamedIndividual.vm"));
        }

        {
            String table = "ontology_resource_labels";
            NodeMapping subject = config.createIriMapping(rdfResource, "resource_unit", "resource_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(rdfLangStringEn, "label"));
        }

        {
            String table = "ontology_resource_superclasses";
            NodeMapping subject = config.createIriMapping(rdfResource, "class_unit", "class_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subClassOf"),
                    config.createIriMapping(rdfResource, "superclass_unit", "superclass_id"));
        }


        {
            String table = "ontology_resource_superproperties";
            NodeMapping subject = config.createIriMapping(rdfResource, "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:subPropertyOf"),
                    config.createIriMapping(rdfResource, "superproperty_unit", "superproperty_id"));
        }

        {
            String table = "ontology_resource_domains";
            NodeMapping subject = config.createIriMapping(rdfResource, "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:domain"),
                    config.createIriMapping(rdfResource, "domain_unit", "domain_id"));
        }

        {
            String table = "ontology_resource_ranges";
            NodeMapping subject = config.createIriMapping(rdfResource, "property_unit", "property_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:range"),
                    config.createIriMapping(rdfResource, "range_unit", "range_id"));
        }

        {
            String table = "ontology_resource_somevaluesfrom_restrictions";
            NodeMapping subject = config.createIriMapping(rdfResource, unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping(rdfResource, "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:someValuesFrom"),
                    config.createIriMapping(rdfResource, "class_unit", "class_id"));
        }

        {
            String table = "ontology_resource_allvaluesfrom_restrictions";
            NodeMapping subject = config.createIriMapping(rdfResource, unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping(rdfResource, "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:allValuesFrom"),
                    config.createIriMapping(rdfResource, "class_unit", "class_id"));
        }

        {
            String table = "ontology_resource_cardinality_restrictions";
            NodeMapping subject = config.createIriMapping(rdfResource, unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping(rdfResource, "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:cardinality"),
                    config.createLiteralMapping(xsdInt, "cardinality"));
        }

        {
            String table = "ontology_resource_mincardinality_restrictions";
            NodeMapping subject = config.createIriMapping(rdfResource, unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping(rdfResource, "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:minCardinality"),
                    config.createLiteralMapping(xsdInt, "cardinality"));
        }

        {
            String table = "ontology_resource_maxcardinality_restrictions";
            NodeMapping subject = config.createIriMapping(rdfResource, unitBlank, "restriction_id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("owl:Restriction"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:onProperty"),
                    config.createIriMapping(rdfResource, "property_unit", "property_id"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("owl:maxCardinality"),
                    config.createLiteralMapping(xsdInt, "cardinality"));
        }
    }
}
