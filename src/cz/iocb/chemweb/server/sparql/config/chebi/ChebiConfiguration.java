package cz.iocb.chemweb.server.sparql.config.chebi;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;



public class ChebiConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "chebi";


    public ChebiConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

        addPrefixes(this);

        Ontology.addResourceClasses(this);
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

        config.addPrefix("ebi", "http://ebi.rdf.ac.uk/dataset/");
        config.addPrefix("obo", "http://purl.obolibrary.org/obo/");
        config.addPrefix("chebi", "http://purl.obolibrary.org/obo/chebi/");
        config.addPrefix("oboInOwl", "http://www.geneontology.org/formats/oboInOwl#");
    }


    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        Chebi.addResourceClasses(config);
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        Chebi.addQuadMapping(config);
    }
}
