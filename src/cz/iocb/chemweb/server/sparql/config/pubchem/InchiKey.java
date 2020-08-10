package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class InchiKey
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("inchikey", "integer", new Table("pubchem", "inchikey_bases"),
                new TableColumn("id"), new TableColumn("inchikey"), "http://rdf.ncbi.nlm.nih.gov/pubchem/inchikey/",
                0));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass inchikey = config.getIriClass("inchikey");
        ConstantIriMapping graph = config.createIriMapping("pubchem:inchikey");

        {
            String table = "inchikey_bases";
            NodeMapping subject = config.createIriMapping(inchikey, "id");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("sio:CHEMINF_000399"));
            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(rdfLangStringEn, "inchikey"));
        }

        {
            String table = "inchikey_compounds";
            NodeMapping subject = config.createIriMapping(inchikey, "inchikey");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping("compound", "compound"));
        }

        {
            String table = "inchikey_subjects";
            NodeMapping subject = config.createIriMapping(inchikey, "inchikey");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("mesh", "subject"));
        }
    }
}
