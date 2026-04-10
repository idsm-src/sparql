package cz.iocb.sparql.nextprot.string;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.nextprot.string.NeXtProtStringConfiguration.schema;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.UserIntBlankNodeClass;



public class Publication
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("publication", "int4", "http://nextprot.org/rdf/publication/"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            Table table = new Table(schema, "publication_bases");
            NodeMapping subject = config.createIriMapping("publication", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Publication"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":LargeScalePublication"),
                    config.createAreEqualCondition("large", "'true'::boolean"));
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
            NodeMapping subject = config.createIriMapping("publication", "publication");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":from"),
                    config.createLiteralMapping(xsdString, "link"));
        }

        {
            Table table = new Table(schema, "publication_authors");
            NodeMapping subject = config.createBlankNodeMapping(new UserIntBlankNodeClass(), "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Person"), config.createAreEqualCondition("person", "'true'::boolean"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Consortium"),
                    config.createAreEqualCondition("person", "'false'::boolean"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":suffix"),
                    config.createLiteralMapping(xsdString, "suffix"));

            config.addQuadMapping(table, graph, config.createIriMapping("publication", "publication"),
                    config.createIriMapping(":author"), subject);
        }

        {
            Table table = new Table(schema, "publication_editors");
            NodeMapping subject = config.createBlankNodeMapping(new UserIntBlankNodeClass(), "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Person"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));

            config.addQuadMapping(table, graph, config.createIriMapping("publication", "publication"),
                    config.createIriMapping(":editor"), subject);
        }
    }
}
