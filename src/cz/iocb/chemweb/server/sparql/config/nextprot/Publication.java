package cz.iocb.chemweb.server.sparql.config.nextprot;

import static cz.iocb.chemweb.server.sparql.config.nextprot.NeXtProtConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Publication
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(
                new IntegerUserIriClass("nextprot:publication", "integer", "http://nextprot.org/rdf/publication/"));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "publication_bases");
            NodeMapping subject = config.createIriMapping("nextprot:publication", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Publication"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":LargeScalePublication"), "large");
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":title"),
                    config.createLiteralMapping(xsdString, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":journal"),
                    config.createLiteralMapping(xsdString, "journal"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":year"),
                    config.createLiteralMapping(xsdString, "year"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":volume"),
                    config.createLiteralMapping(xsdString, "volume"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":issue"),
                    config.createLiteralMapping(xsdString, "issue"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":pubType"),
                    config.createLiteralMapping(xsdString, "pub_type"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":firstPage"),
                    config.createLiteralMapping(xsdString, "first_page"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":lastPage"),
                    config.createLiteralMapping(xsdString, "last_page"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":publisher"),
                    config.createLiteralMapping(xsdString, "publisher"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":city"),
                    config.createLiteralMapping(xsdString, "city"));
        }

        {
            Table table = new Table(schema, "publication_links");
            NodeMapping subject = config.createIriMapping("nextprot:publication", "publication");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":from"),
                    config.createLiteralMapping(xsdString, "link"));
        }

        {
            Table table = new Table(schema, "publication_authors");
            NodeMapping subject = config.createBlankNodeMapping(config.getNewIntBlankNodeClass(), "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Person"), "person");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Consortium"), "not person");
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":suffix"),
                    config.createLiteralMapping(xsdString, "suffix"));

            config.addQuadMapping(table, graph, config.createIriMapping("nextprot:publication", "publication"),
                    config.createIriMapping(":author"), subject);
        }

        {
            Table table = new Table(schema, "publication_editors");
            NodeMapping subject = config.createBlankNodeMapping(config.getNewIntBlankNodeClass(), "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Person"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));

            config.addQuadMapping(table, graph, config.createIriMapping("nextprot:publication", "publication"),
                    config.createIriMapping(":editor"), subject);
        }
    }
}
