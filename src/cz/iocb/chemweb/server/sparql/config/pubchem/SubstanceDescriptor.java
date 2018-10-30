package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



class SubstanceDescriptor
{
    static void addIriClasses(PubChemConfiguration config)
    {
        String prefix = "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/SID[0-9]+_";

        config.addIriClass(
                new UserIriClass("substance_version", Arrays.asList("integer"), prefix + "Substance_Version"));
    }


    static void addQuadMapping(PubChemConfiguration config)
    {
        UserIriClass descriptorSubstanceVersion = config.getIriClass("substance_version");
        NodeMapping graph = config.createIriMapping("descriptor:substance");

        {
            String table = "descriptor_substance_bases";
            NodeMapping subject = config.createIriMapping(descriptorSubstanceVersion, "substance");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("obo:IAO_0000129"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("sio:has-value"),
                    config.createLiteralMapping(xsdInt, "version"));
        }
    }
}
