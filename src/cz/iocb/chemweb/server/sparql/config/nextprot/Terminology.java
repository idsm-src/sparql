package cz.iocb.chemweb.server.sparql.config.nextprot;

import static cz.iocb.chemweb.server.sparql.config.nextprot.NeXtProtConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Terminology
{
    static void addIriClasses(NeXtProtConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("terminology", "integer", new Table(schema, "terminology_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://nextprot.org/rdf/terminology/", 0));
    }


    static void addQuadMapping(NeXtProtConfiguration config)
    {
        UserIriClass terminology = config.getIriClass("terminology");
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            String table = "terminology_bases";
            NodeMapping subject = config.createIriMapping(terminology, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("schema", "type"));
        }

        {
            String table = "terminology_parents";
            NodeMapping subject = config.createIriMapping(terminology, "term");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping(":childOf"),
                    config.createIriMapping("terminology", "parent"));
        }

        {
            String table = "terminology_related_terms";
            NodeMapping subject = config.createIriMapping(terminology, "term");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping(":related"),
                    config.createIriMapping("terminology", "related"));
        }

        {
            String table = "terminology_labels";
            NodeMapping subject = config.createIriMapping(terminology, "term");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                    config.createLiteralMapping(xsdString, "label"));
        }
    }
}
