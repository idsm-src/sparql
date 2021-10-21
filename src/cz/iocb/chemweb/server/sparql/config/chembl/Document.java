package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlTableAccess.Condition;



public class Document
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("chembl:document", "integer",
                "http://rdf.ebi.ac.uk/resource/chembl/document/CHEMBL"));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        Condition valueCondition = config.createAreNotEqualCondition("id", "'1158643'::integer");
        Condition notNullCondition = config.createIsNotNullCondition("journal_id");

        Condition fullCondition = new Condition();
        fullCondition.add(valueCondition);
        fullCondition.add(notNullCondition);

        Table table = new Table(schema, "docs");
        NodeMapping subject = config.createIriMapping("chembl:document", "id");
        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:Document"), valueCondition);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("bibo:pmid"),
                config.createIriMapping("identifiers:pubmed", "pubmed_id"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasJournal"),
                config.createIriMapping("chembl_journal:CHEMBL_JRN_null"), fullCondition);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:hasJournal"),
                config.createIriMapping("chembl:journal", "journal_id"), notNullCondition);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:date"),
                config.createLiteralMapping(xsdInt, "year"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "chembl_id"), valueCondition);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "chembl_id"), valueCondition);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("cco:documentType"),
                config.createLiteralMapping(xsdString, "doc_type"), valueCondition);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                config.createLiteralMapping(xsdString, "title"), valueCondition);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("bibo:pageStart"),
                config.createLiteralMapping(xsdString, "first_page"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("bibo:pageEnd"),
                config.createLiteralMapping(xsdString, "last_page"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("bibo:volume"),
                config.createLiteralMapping(xsdString, "volume"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("bibo:doi"),
                config.createLiteralMapping(xsdString, "doi"));
        config.addQuadMapping(table, graph, subject, config.createIriMapping("bibo:issue"),
                config.createLiteralMapping(xsdString, "issue"));
        config.addQuadMapping(table, graph, config.createIriMapping("chembl_journal:CHEMBL_JRN_null"),
                config.createIriMapping("cco:hasDocument"), subject, fullCondition);
        config.addQuadMapping(table, graph, config.createIriMapping("chembl:journal", "journal_id"),
                config.createIriMapping("cco:hasDocument"), subject, fullCondition);

        // extension
        config.addQuadMapping(table, graph, config.createIriMapping("pubchem:reference", "pubmed_id"),
                config.createIriMapping("skos:exactMatch"), subject);
        config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                config.createLiteralMapping("chembl/Document.vm"), valueCondition);
    }
}
