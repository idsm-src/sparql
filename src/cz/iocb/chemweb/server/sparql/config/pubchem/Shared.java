package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;



class Shared
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new GeneralUserIriClass(schema, "dqmesh", Arrays.asList("integer", "integer"),
                "http://id\\.nlm\\.nih\\.gov/mesh/D[0-9]{6}(Q[0-9]{6})?"));
        config.addIriClass(new GeneralUserIriClass(schema, "mesh", Arrays.asList("integer"),
                "http://id\\.nlm\\.nih\\.gov/mesh/(C[0-9]{9}|M[0-9]{7})"));
        config.addIriClass(new StringUserIriClass("pdblink", "http://rdf.wwpdb.org/pdb/", 4));
        config.addIriClass(new StringUserIriClass("uniprot", "http://purl.uniprot.org/uniprot/"));
    }
}
