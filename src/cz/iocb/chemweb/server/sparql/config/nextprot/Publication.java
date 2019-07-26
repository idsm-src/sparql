package cz.iocb.chemweb.server.sparql.config.nextprot;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Publication
{
    static void addIriClasses(NeXtProtConfiguration config)
    {
        config.addIriClass(new UserIriClass("publication", Arrays.asList("integer"),
                "http://nextprot.org/rdf/publication/[1-9][0-9]*"));
    }


    static void addQuadMapping(NeXtProtConfiguration config)
    {
        UserIriClass publication = config.getIriClass("publication");
        ConstantIriMapping graph = config.createIriMapping("<http://nextprot.org/rdf>");

        {
            String table = "publication_bases";
            NodeMapping subject = config.createIriMapping(publication, "id");

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
            String table = "publication_links";
            NodeMapping subject = config.createIriMapping(publication, "publication");

            config.addQuadMapping(table, graph, subject, config.createIriMapping(":from"),
                    config.createLiteralMapping(xsdString, "link"));
        }

        {
            String table = "publication_authors";
            NodeMapping subject = config.createBlankNodeMapping(new UserIntBlankNodeClass(config.blankNodeSegment++),
                    "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Person"), "person");
            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Consortium"), "not person");
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":suffix"),
                    config.createLiteralMapping(xsdString, "suffix"));

            config.addQuadMapping(table, graph, config.createIriMapping(publication, "publication"),
                    config.createIriMapping(":author"), subject);
        }

        {
            String table = "publication_editors";
            NodeMapping subject = config.createBlankNodeMapping(new UserIntBlankNodeClass(config.blankNodeSegment++),
                    "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping(":Person"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping(":name"),
                    config.createLiteralMapping(xsdString, "name"));

            config.addQuadMapping(table, graph, config.createIriMapping(publication, "publication"),
                    config.createIriMapping(":editor"), subject);
        }
    }
}
