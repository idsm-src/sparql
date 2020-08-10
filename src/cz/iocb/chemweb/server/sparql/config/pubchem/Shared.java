package cz.iocb.chemweb.server.sparql.config.pubchem;

import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;



class Shared
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new StringUserIriClass("pdblink", "http://rdf.wwpdb.org/pdb/", 4));
        config.addIriClass(new StringUserIriClass("uniprot", "http://purl.uniprot.org/uniprot/"));
    }
}
