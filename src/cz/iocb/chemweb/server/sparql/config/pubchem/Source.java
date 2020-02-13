package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Source
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(
                new MapUserIriClass("source", "smallint", new Table(schema, "source_bases"), new TableColumn("id"),
                        new TableColumn("iri"), "http://rdf\\.ncbi\\.nlm\\.nih\\.gov/pubchem/source/.*"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass source = config.getIriClass("source");
        ConstantIriMapping graph = config.createIriMapping("pubchem:source");

        {
            String table = "source_bases";
            NodeMapping subject = config.createIriMapping(source, "id");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("dcterms:Dataset"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Source.vm"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(rdfLangStringEn, "title"));
        }

        {
            String table = "source_subjects";
            NodeMapping subject = config.createIriMapping(source, "source");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("concept", "subject"));
        }

        {
            String table = "source_alternatives";
            NodeMapping subject = config.createIriMapping(source, "source");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(rdfLangStringEn, "alternative"));
        }
    }
}
