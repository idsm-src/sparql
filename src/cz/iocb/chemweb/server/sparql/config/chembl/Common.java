package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration.schema;
import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.StringUserIriClass;



class Common
{
    static void addIriClasses(ChemblConfiguration config)
    {
        config.addIriClass(new GeneralUserIriClass(schema, "efo", Arrays.asList("varchar"),
                "http://www\\.ebi\\.ac\\.uk/efo/((EFO_|GO:)[0-9]{7}|DOID:[1-9][0-9]*)|"
                        + "http://purl\\.obolibrary\\.org/obo/HP_[0-9]{7}|"
                        + "http://www\\.orpha\\.net/consor/cgi-bin/OC_Exp\\.php\\?Expert=[1-9][0-9]*|"
                        + "http://www\\.ebi\\.ac\\.uk/ontology-lookup/browse\\.do\\?ontName=MP&termId=MP:[0-9]{7}"));

        config.addIriClass(new StringUserIriClass("bao", "http://www.bioassayontology.org/bao#", "BAO_[0-9]{7}"));

        config.addIriClass(new IntegerUserIriClass("taxonomy", "bigint", "http://identifiers.org/taxonomy/"));

        config.addIriClass(new IntegerUserIriClass("ncbi_taxonomy", "bigint",
                "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?mode=Info&id="));
    }
}
