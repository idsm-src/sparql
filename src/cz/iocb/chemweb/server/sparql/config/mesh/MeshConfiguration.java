package cz.iocb.chemweb.server.sparql.config.mesh;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;



public class MeshConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "mesh";

    static final LangStringConstantTagClass rdfLangStringEn = LangStringConstantTagClass.get("en");


    public MeshConfiguration(DataSource connectionPool) throws SQLException
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

        config.addPrefix("meshv", "http://id.nlm.nih.gov/mesh/vocab#");
        config.addPrefix("mesh", "http://id.nlm.nih.gov/mesh/");
    }


    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        Mesh.addResourceClasses(config);
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        Mesh.addQuadMapping(config);
    }
}
