package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass.SqlCheck;



class Source
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("source", Arrays.asList("smallint"),
                "http://rdf\\.ncbi\\.nlm\\.nih\\.gov/pubchem/source/.*", SqlCheck.IF_MATCH));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass source = config.getIriClass("source");
        ConstantIriMapping graph = config.createIriMapping("pubchem:source");

        {
            String table = "source_bases";
            NodeMapping subject = config.createIriMapping(source, "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("dcterms:Dataset"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Source.vm"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
        }

        {
            String table = "source_subjects";
            NodeMapping subject = config.createIriMapping(source, "source");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("concept", "subject"));
        }

        {
            String table = "source_alternatives";
            NodeMapping subject = config.createIriMapping(source, "source");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(rdfLangStringEn, "alternative"));
        }
    }
}
