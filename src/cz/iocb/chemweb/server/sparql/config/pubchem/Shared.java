package cz.iocb.chemweb.server.sparql.config.pubchem;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Shared
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("graph", Arrays.asList("smallint"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/(bioassay|biosystem|compound|concept|conserveddomain|endpoint|"
                        + "gene|inchikey|measuregroup|ontology|protein|reference|source|substance|synonym|descriptor/(compound|substance))"));

        config.addIriClass(new UserIriClass("dqmesh", Arrays.asList("integer", "integer"),
                "http://id.nlm.nih.gov/mesh/D[0-9]{6}(Q[0-9]{6})?"));
        config.addIriClass(
                new UserIriClass("mesh", Arrays.asList("integer"), "http://id.nlm.nih.gov/mesh/(C[0-9]{9}|M[0-9]{7})"));
        config.addIriClass(new UserIriClass("pdblink", Arrays.asList("char(4)"), "http://rdf.wwpdb.org/pdb/.*"));
        config.addIriClass(new UserIriClass("uniprot", Arrays.asList("varchar"), "http://purl.uniprot.org/uniprot/.*"));
    }
}
