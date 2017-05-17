package cz.iocb.chemweb.server.sparql.pubchem;

import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



class Shared extends PubChemMapping
{
    static IriClass graph;

    static IriClass go;
    static IriClass dqmesh;
    static IriClass mesh;
    static IriClass pdblink;
    static IriClass taxonomy;
    static IriClass uniprot;


    static void loadClasses()
    {
        classmap(graph = new IriClass("graph", 1,
                "http://rdf.ncbi.nlm.nih.gov/pubchem/(bioassay|biosystem|compound|concept|conserveddomain|endpoint|"
                        + "gene|inchikey|measuregroup|ontology|protein|reference|source|substance|synonym|descriptor/(compound|substance))"));

        classmap(go = new IriClass("go", 1, "http://purl.obolibrary.org/obo/GO_[0-9]+"));
        classmap(dqmesh = new IriClass("dqmesh", 2, "http://id.nlm.nih.gov/mesh/D[0-9]+(Q[0-9]+)?"));
        classmap(mesh = new IriClass("mesh", 1, "http://id.nlm.nih.gov/mesh/(C|M)[0-9]+"));
        classmap(pdblink = new IriClass("pdblink", 1, "http://rdf.wwpdb.org/pdb/.*"));
        classmap(taxonomy = new IriClass("taxonomy", 1, "http://identifiers.org/taxonomy/[0-9]+"));
        classmap(uniprot = new IriClass("uniprot", 1, "http://purl.uniprot.org/uniprot/.*"));
    }
}
