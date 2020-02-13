package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class BindingSite
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new IntegerUserIriClass("binding_site", "bigint",
                "http://rdf.ebi.ac.uk/resource/chembl/binding_site/CHEMBL_BS_"));
    }


    static void addQuadMapping(ChemblConfiguration config)
    {
        UserIriClass site = config.getIriClass("binding_site");
        ConstantIriMapping graph = config.createIriMapping("<http://rdf.ebi.ac.uk/dataset/chembl>");

        String table = "binding_sites";
        NodeMapping subject = config.createIriMapping(site, "site_id");

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                config.createIriMapping("cco:BindingSite"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:hasTarget"),
                config.createIriMapping("target", "tid"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:chemblId"),
                config.createLiteralMapping(xsdString, "('CHEMBL_BS_' || site_id)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdfs:label"),
                config.createLiteralMapping(xsdString, "('CHEMBL_BS_' || site_id)"));

        config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("cco:bindingSiteName"),
                config.createLiteralMapping(xsdString, "site_name"));

        config.addQuadMapping(schema, table, graph, config.createIriMapping("target", "tid"),
                config.createIriMapping("cco:hasBindingSite"), subject);
    }
}
