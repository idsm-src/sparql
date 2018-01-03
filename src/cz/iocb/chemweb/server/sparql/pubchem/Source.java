package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Source
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(
                new IriClass("source", Arrays.asList("smallint"), "http://rdf.ncbi.nlm.nih.gov/pubchem/source/.*"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        IriClass source = config.getIriClass("source");
        NodeMapping graph = config.createIriMapping("pubchem:source");

        {
            String table = "source_bases";
            NodeMapping subject = config.createIriMapping(source, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("dcterms:Dataset"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(xsdString, "title"));
        }

        {
            /* TODO:
            String table = "source_subjects";
            NodeMapping subject = config.createIriMapping(source, "source");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("concept", "subject"));
            */
        }

        {
            String table = "source_alternatives";
            NodeMapping subject = config.createIriMapping(source, "source");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(xsdString, "alternative"));
        }
    }
}
