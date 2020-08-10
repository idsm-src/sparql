package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.rdfLangStringEn;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class Synonym
{
    static void addIriClasses(PubChemConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("synonym", "integer", new Table("pubchem", "synonym_bases"),
                new TableColumn("id"), new TableColumn("md5"), "http://rdf.ncbi.nlm.nih.gov/pubchem/synonym/MD5_", 0));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass synonym = config.getIriClass("synonym");
        ConstantIriMapping graph = config.createIriMapping("pubchem:synonym");

        {
            String table = "synonym_bases";
            NodeMapping subject = config.createIriMapping(synonym, "id");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Synonym.vm"));
        }

        {
            String table = "synonym_values";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(rdfLangStringEn, "value"));
        }

        {
            String table = "synonym_types";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("ontology_resource", Ontology.unitCHEMINF, "type_id"));
        }

        {
            String table = "synonym_compounds";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("sio:is-attribute-of"),
                    config.createIriMapping("compound", "compound"));
        }

        {
            String table = "synonym_mesh_subjects";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("mesh", "subject"));
        }

        {
            String table = "synonym_concept_subjects";
            NodeMapping subject = config.createIriMapping(synonym, "synonym");

            config.addQuadMapping("pubchem", table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("concept", "concept"));
        }
    }
}
