package cz.iocb.chemweb.server.sparql.config.pubchem;

import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class Anatomy
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("pubchem:anatomy", "integer",
                "http://rdf.ncbi.nlm.nih.gov/pubchem/anatomy/ANATOMYID"));
    }
}
