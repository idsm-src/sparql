package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class SubstanceDescriptor
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/SID";

        config.addIriClass(new IntegerUserIriClass("substance_version", "integer", prefix, "Substance_Version"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass descriptorSubstanceVersion = config.getIriClass("substance_version");
        ConstantIriMapping graph = config.createIriMapping("descriptor:substance");

        {
            String table = "descriptor_substance_bases";
            NodeMapping subject = config.createIriMapping(descriptorSubstanceVersion, "substance");

            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:IAO_0000129"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/descriptor/version.vm"));
            config.addQuadMapping(schema, table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdInt, "version"));
        }
    }
}
