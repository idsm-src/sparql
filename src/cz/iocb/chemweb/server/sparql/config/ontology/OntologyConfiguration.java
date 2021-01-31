package cz.iocb.chemweb.server.sparql.config.ontology;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;



public class OntologyConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "ontology";

    public static final LangStringConstantTagClass rdfLangStringEn = LangStringConstantTagClass.get("en");


    public OntologyConfiguration(String service, DataSource connectionPool, DatabaseSchema schema) throws SQLException
    {
        super(service, connectionPool, schema);

        addPrefixes(this);

        addResourceClasses(this);

        addQuadMapping(this);
    }


    public static void addPrefixes(SparqlDatabaseConfiguration config)
    {
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        config.addPrefix("owl", "http://www.w3.org/2002/07/owl#");
        config.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        config.addPrefix("template", "http://bioinfo.iocb.cz/0.9/template#");

        config.addPrefix("dataset", "http://bioinfo.iocb.cz/dataset/");
    }


    public static void addResourceClasses(SparqlDatabaseConfiguration config) throws SQLException
    {
        Ontology.addResourceClasses(config);
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        Ontology.addQuadMapping(config);
    }
}
