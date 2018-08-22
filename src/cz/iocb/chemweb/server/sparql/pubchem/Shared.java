package cz.iocb.chemweb.server.sparql.pubchem;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Shared
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new UserIriClass("graph", Arrays.asList("smallint"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/(bioassay|biosystem|compound|concept|conserveddomain|endpoint|"
                        + "gene|inchikey|measuregroup|ontology|protein|reference|source|substance|synonym|descriptor/(compound|substance))"));

        config.addIriClass(
                new UserIriClass("go", Arrays.asList("integer"), "http://purl.obolibrary.org/obo/GO_[0-9]+"));
        config.addIriClass(new UserIriClass("dqmesh", Arrays.asList("integer", "integer"),
                "http://id.nlm.nih.gov/mesh/D[0-9]+(Q[0-9]+)?"));
        config.addIriClass(
                new UserIriClass("mesh", Arrays.asList("integer"), "http://id.nlm.nih.gov/mesh/(C|M)[0-9]+"));
        config.addIriClass(new UserIriClass("pdblink", Arrays.asList("char(4)"), "http://rdf.wwpdb.org/pdb/.*"));
        config.addIriClass(
                new UserIriClass("taxonomy", Arrays.asList("integer"), "http://identifiers.org/taxonomy/[0-9]+"));
        config.addIriClass(new UserIriClass("uniprot", Arrays.asList("varchar"), "http://purl.uniprot.org/uniprot/.*"));
    }
}
