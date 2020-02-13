package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Document
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("document", "bigint", new Table(schema, "docs"),
                new TableColumn("doc_id"), new TableColumn("chembl_id"),
                "http://rdf.ebi.ac.uk/resource/chembl/document/", "CHEMBL[1-9][0-9]*"));

        config.addIriClass(new IntegerUserIriClass("pubmed", "bigint", "http://identifiers.org/pubmed/"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass document = config.getIriClass("document");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "docs";
        NodeMapping subject = config.createIriMapping(document, "doc_id");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Document"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:pmid"),
                config.createIriMapping("pubmed", "pubmed_id"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasJournal"),
                config.createIriMapping("chembl_journal:CHEMBL_JRN_null"), "doc_id != -1 and journal_id is null");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasJournal"),
                config.createIriMapping("journal", "journal_id"), "doc_id != -1 and journal_id is not null");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:date"),
                config.createLiteralMapping(xsdInt, "year"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "chembl_id"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "chembl_id"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:documentType"),
                config.createLiteralMapping(xsdString, "doc_type"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("dcterms:title"),
                config.createLiteralMapping(xsdString, "title"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:pageStart"),
                config.createLiteralMapping(xsdString, "first_page"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:pageEnd"),
                config.createLiteralMapping(xsdString, "last_page"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:volume"),
                config.createLiteralMapping(xsdString, "volume"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:doi"),
                config.createLiteralMapping(xsdString, "doi"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("bibo:issue"),
                config.createLiteralMapping(xsdString, "issue"), "doc_id != -1");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("chembl_journal:CHEMBL_JRN_null"),
                config.createIriMapping("cco:hasDocument"), subject, "doc_id != -1 and journal_id is null");

        config.addQuadMapping(schema, table, graph, config.createIriMapping("journal", "journal_id"),
                config.createIriMapping("cco:hasDocument"), subject, "doc_id != -1 and journal_id is not null");
    }
}
