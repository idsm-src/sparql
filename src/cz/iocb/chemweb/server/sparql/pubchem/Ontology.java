package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Ontology extends PubChemMapping
{
    static IriClass rdfclass;
    static IriClass property;


    static void loadClasses()
    {
        classmap(rdfclass = new IriClass("class", 1, getIriValues("class_bases")));
        classmap(property = new IriClass("property", 1, getIriValues("property_bases")));
    }


    static void loadMapping()
    {
        NodeMapping graph = iri("pubchem:ontology");

        {
            String table = "class_bases";
            NodeMapping subject = iri(rdfclass, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("owl:Class"));
            quad(table, graph, subject, iri("template:itemTemplate"), literal("base/Class.vm"));
            quad(table, graph, subject, iri("template:pageTemplate"), literal("base/Class.vm"));
            quad(table, graph, subject, iri("rdfs:label"), literal(xsdString, "label"));
        }

        {
            String table = "class_superclasses";
            NodeMapping subject = iri(rdfclass, "class");

            quad(table, graph, subject, iri("rdfs:subClassOf"), iri(rdfclass, "superclass"));
        }

        {
            String table = "property_bases";
            NodeMapping subject = iri(property, "id");

            quad(table, graph, subject, iri("rdf:type"), iri("rdf:Property"));
            quad(table, graph, subject, iri("template:itemTemplate"), literal("base/Property.vm"));
            quad(table, graph, subject, iri("template:pageTemplate"), literal("base/Property.vm"));
            quad(table, graph, subject, iri("rdfs:label"), literal(xsdString, "label"));
        }

        {
            String table = "property_superproperties";
            NodeMapping subject = iri(property, "property");

            quad(table, graph, subject, iri("rdfs:subPropertyOf"), iri(property, "superproperty"));
        }

        {
            String table = "property_domains";
            NodeMapping subject = iri(property, "property");

            quad(table, graph, subject, iri("rdfs:domain"), iri(rdfclass, "domain"));
        }

        {
            String table = "property_ranges";
            NodeMapping subject = iri(property, "property");

            quad(table, graph, subject, iri("rdfs:range"), iri(rdfclass, "range"));
        }
    }
}
