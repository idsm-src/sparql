package cz.iocb.chemweb.server.sparql.config.sachem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import cz.iocb.chemweb.server.Utils;
import cz.iocb.chemweb.server.db.postgresql.ConnectionPool;
import cz.iocb.chemweb.server.db.postgresql.PostgresSchema;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.parser.Xsd;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;



public class SachemConfiguration extends SparqlDatabaseConfiguration
{
    protected SachemConfiguration(String name, String iriPrefix, String idPattern)
            throws FileNotFoundException, IOException, SQLException
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/datasource-" + name + ".properties"));

        connectionPool = new ConnectionPool(properties);
        schema = new PostgresSchema(connectionPool);

        loadPrefixes(iriPrefix);
        loadClasses(iriPrefix, idPattern);
        loadQuadMapping();
        loadProcedures();
    }


    private void loadPrefixes(String iriPrefix)
    {
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        prefixes.put("sio", "http://semanticscience.org/resource/");

        prefixes.put("compound", iriPrefix);
        prefixes.put("sachem", "http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#");
    }


    private void loadClasses(String iriPrefix, String idPattern)
    {
        String sachem = prefixes.get("sachem");

        HashSet<String> queryFormatValues = new HashSet<String>();
        queryFormatValues.add(sachem + "UnspecifiedFormat");
        queryFormatValues.add(sachem + "SMILES");
        queryFormatValues.add(sachem + "MolFile");
        queryFormatValues.add(sachem + "RGroup");
        addIriClass(new IriClass("queryFormat", Arrays.asList("integer"), queryFormatValues));

        HashSet<String> graphModeValues = new HashSet<String>();
        graphModeValues.add(sachem + "substructureSearch");
        graphModeValues.add(sachem + "exactSearch");
        addIriClass(new IriClass("graphMode", Arrays.asList("integer"), graphModeValues));

        HashSet<String> chargeModeValues = new HashSet<String>();
        chargeModeValues.add(sachem + "ignoreCharges");
        chargeModeValues.add(sachem + "defaultChargeAsZero");
        chargeModeValues.add(sachem + "defaultChargeAsAny");
        addIriClass(new IriClass("chargeMode", Arrays.asList("integer"), chargeModeValues));

        HashSet<String> isotopeModeValues = new HashSet<String>();
        isotopeModeValues.add(sachem + "ignoreIsotopes");
        isotopeModeValues.add(sachem + "defaultIsotopeAsStandard");
        isotopeModeValues.add(sachem + "defaultIsotopeAsAny");
        addIriClass(new IriClass("isotopeMode", Arrays.asList("integer"), isotopeModeValues));

        HashSet<String> stereoModeValues = new HashSet<String>();
        stereoModeValues.add(sachem + "ignoreStrereo");
        stereoModeValues.add(sachem + "strictStereo");
        addIriClass(new IriClass("stereoMode", Arrays.asList("integer"), stereoModeValues));

        HashSet<String> tautomerModeValues = new HashSet<String>();
        tautomerModeValues.add(sachem + "ignoreTautomers");
        tautomerModeValues.add(sachem + "inchiTautomers");
        addIriClass(new IriClass("tautomerMode", Arrays.asList("integer"), tautomerModeValues));

        addIriClass(new IriClass("compound", Arrays.asList("integer"), iriPrefix + idPattern));
        addIriClass(new IriClass("compound_molfile", Arrays.asList("integer"), iriPrefix + idPattern + "_Molfile"));

        addIriClass(new IriClass("class", Arrays.asList("integer"), getIriValues("class_bases")));
        addIriClass(new IriClass("property", Arrays.asList("integer"), getIriValues("property_bases")));
    }


    private void loadQuadMapping()
    {
        IriClass compound = getIriClass("compound");

        String table = "compounds";
        NodeMapping subject = createIriMapping("compound_molfile", "id");

        addQuadMapping(table, null, subject, createIriMapping("rdf:type"), createIriMapping("sio:SIO_011120"));
        addQuadMapping(table, null, subject, createIriMapping("sio:is-attribute-of"), createIriMapping(compound, "id"));
        addQuadMapping(table, null, subject, createIriMapping("sio:has-value"),
                createLiteralMapping(xsdString, "molfile"));
    }


    private void loadProcedures()
    {
        String sachem = prefixes.get("sachem");
        IriClass molClass = getIriClass("compound");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(sachem + "substructureSearch",
                "sachem_substructure_search");
        subsearch.addParameter(new ParameterDefinition(sachem + "query", LiteralClass.xsdString, null));
        subsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        subsearch.addParameter(new ParameterDefinition(sachem + "graphMode", getIriClass("graphMode"),
                new IRI(sachem + "substructureSearch")));
        subsearch.addParameter(new ParameterDefinition(sachem + "chargeMode", getIriClass("chargeMode"),
                new IRI(sachem + "defaultChargeAsAny")));
        subsearch.addParameter(new ParameterDefinition(sachem + "isotopeMode", getIriClass("isotopeMode"),
                new IRI(sachem + "ignoreIsotopes")));
        subsearch.addParameter(new ParameterDefinition(sachem + "stereoMode", getIriClass("stereoMode"),
                new IRI(sachem + "ignoreStrereo")));
        subsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomerMode"),
                new IRI(sachem + "ignoreTautomers")));
        subsearch.addResult(new ResultDefinition(molClass));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(sachem + "similaritySearch",
                "sachem_similarity_search");
        simsearch.addParameter(new ParameterDefinition(sachem + "query", LiteralClass.xsdString, null));
        simsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simsearch.addResult(new ResultDefinition(sachem + "compound", molClass, "compound"));
        simsearch.addResult(new ResultDefinition(sachem + "score", LiteralClass.xsdFloat, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(sachem + "similarCompoundSearch",
                "sachem_similarity_search");
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "query", LiteralClass.xsdString, null));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simcmpsearch.addResult(new ResultDefinition(null, molClass, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);
    }
}
