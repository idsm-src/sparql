package cz.iocb.chemweb.server.sparql.config.sachem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import cz.iocb.chemweb.server.Utils;
import cz.iocb.chemweb.server.db.postgresql.ConnectionPool;
import cz.iocb.chemweb.server.db.postgresql.PostgresSchema;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.parser.Xsd;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;



public class DrugbankConfiguration extends SparqlDatabaseConfiguration
{
    private static DrugbankConfiguration singleton;


    public DrugbankConfiguration() throws FileNotFoundException, IOException, SQLException
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/datasource-drugbank.properties"));

        connectionPool = new ConnectionPool(properties);
        schema = new PostgresSchema(connectionPool);

        loadPrefixes();
        loadClasses();
        loadQuadMapping();
        loadProcedures();
    }


    public static DrugbankConfiguration get() throws FileNotFoundException, IOException, SQLException
    {
        if(singleton != null)
            return singleton;

        synchronized(DrugbankConfiguration.class)
        {
            if(singleton != null)
                return singleton;

            singleton = new DrugbankConfiguration();
        }

        return singleton;
    }


    private void loadPrefixes()
    {
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");

        prefixes.put("drugbank", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/");
        prefixes.put("sachem", "http://bioinfo.uochb.cas.cz/sparql-endpoint/sachem/");
    }


    private void loadQuadMapping()
    {

    }


    private void loadClasses()
    {
        addIriClass(new IriClass("drugbank", Arrays.asList("integer"),
                "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/DB[0-9]+"));
    }


    private void loadProcedures()
    {
        String sachem = prefixes.get("sachem");
        IriClass drugbank = getIriClass("drugbank");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(sachem + "substructureSearch",
                "orchem_substructure_search");
        subsearch.addParameter(new ParameterDefinition(sachem + "query", LiteralClass.xsdString, null));
        subsearch.addParameter(
                new ParameterDefinition(sachem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        subsearch.addParameter(new ParameterDefinition(sachem + "strictStereo", LiteralClass.xsdBoolean,
                new Literal("false", new IRI(Xsd.BOOLEAN))));
        subsearch.addParameter(new ParameterDefinition(sachem + "exact", LiteralClass.xsdBoolean,
                new Literal("false", new IRI(Xsd.BOOLEAN))));
        subsearch.addParameter(new ParameterDefinition(sachem + "tautomers", LiteralClass.xsdBoolean,
                new Literal("false", new IRI(Xsd.BOOLEAN))));
        subsearch.addResult(new ResultDefinition(drugbank));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(sachem + "similaritySearch",
                "orchem_similarity_search");
        simsearch.addParameter(new ParameterDefinition(sachem + "query", LiteralClass.xsdString, null));
        simsearch.addParameter(
                new ParameterDefinition(sachem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simsearch.addResult(new ResultDefinition(sachem + "compound", drugbank, "compound"));
        simsearch.addResult(new ResultDefinition(sachem + "score", LiteralClass.xsdFloat, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(sachem + "similarCompoundSearch",
                "orchem_similarity_search");
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "query", LiteralClass.xsdString, null));
        simcmpsearch.addParameter(
                new ParameterDefinition(sachem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simcmpsearch.addResult(new ResultDefinition(null, drugbank, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);
    }
}
