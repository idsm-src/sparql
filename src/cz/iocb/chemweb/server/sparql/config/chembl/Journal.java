package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Journal
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("journal", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/journal/CHEMBL_JRN_"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass journal = config.getIriClass("journal");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "journal_dictionary";
        NodeMapping subject = config.createIriMapping(journal, "journal_id");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Journal"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "('CHEMBL_JRN_' || journal_id)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:title"),
                config.createLiteralMapping(xsdString, "title"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:shortTitle"),
                config.createLiteralMapping(xsdString, "short_title"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:issn"),
                config.createLiteralMapping(xsdString, "issn"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:eissn"),
                config.createLiteralMapping(xsdString, "eissn"));
    }
}
