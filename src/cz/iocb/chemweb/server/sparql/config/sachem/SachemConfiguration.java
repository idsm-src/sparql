package cz.iocb.chemweb.server.sparql.config.sachem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.EnumUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public abstract class SachemConfiguration extends SparqlDatabaseConfiguration
{
    private final String schema;
    private final String table;


    protected SachemConfiguration(DataSource connectionPool, String schema, String table, String iriPrefix,
            String idPattern) throws SQLException
    {
        super(connectionPool);
        this.schema = schema;
        this.table = table;

        loadPrefixes(iriPrefix);
        loadClasses(iriPrefix.replaceAll("\\.", "\\\\\\.") + idPattern);
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


    @SuppressWarnings("serial")
    private void loadClasses(String pattern)
    {
        String sachem = "http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#";

        addIriClass(new EnumUserIriClass("query_format", "integer", new HashMap<String, String>()
        {
            {
                put("0", sachem + "UnspecifiedFormat");
                put("1", sachem + "SMILES");
                put("2", sachem + "MolFile");
                put("3", sachem + "RGroup");
            }
        }));

        addIriClass(new EnumUserIriClass("search_mode", "integer", new HashMap<String, String>()
        {
            {
                put("0", sachem + "substructureSearch");
                put("1", sachem + "exactSearch");
            }
        }));

        addIriClass(new EnumUserIriClass("charge_mode", "integer", new HashMap<String, String>()
        {
            {
                put("0", sachem + "ignoreCharges");
                put("1", sachem + "defaultChargeAsZero");
                put("2", sachem + "defaultChargeAsAny");
            }
        }));

        addIriClass(new EnumUserIriClass("isotope_mode", "integer", new HashMap<String, String>()
        {
            {
                put("0", sachem + "ignoreIsotopes");
                put("1", sachem + "defaultIsotopeAsStandard");
                put("2", sachem + "defaultIsotopeAsAny");
            }
        }));

        addIriClass(new EnumUserIriClass("stereo_mode", "integer", new HashMap<String, String>()
        {
            {
                put("0", sachem + "ignoreStrereo");
                put("1", sachem + "strictStereo");
            }
        }));

        addIriClass(new EnumUserIriClass("tautomer_mode", "integer", new HashMap<String, String>()
        {
            {
                put("0", sachem + "ignoreTautomers");
                put("1", sachem + "inchiTautomers");
            }
        }));
    }


    private void loadQuadMapping() throws SQLException
    {
        UserIriClass compound = getIriClass("compound");

        NodeMapping subject = createIriMapping("compound_molfile", "id");

        addQuadMapping(schema, table, null, subject, createIriMapping("rdf:type"), createIriMapping("sio:SIO_011120"));
        addQuadMapping(schema, table, null, subject, createIriMapping("sio:is-attribute-of"),
                createIriMapping(compound, "id"));
        addQuadMapping(schema, table, null, subject, createIriMapping("sio:has-value"),
                createLiteralMapping(xsdString, "molfile"));
    }


    private void loadProcedures()
    {
        String sachem = prefixes.get("sachem");
        UserIriClass molClass = getIriClass("compound");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(sachem + "substructureSearch",
                new Function(schema, "sachem_substructure_search"));
        subsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        subsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        subsearch.addParameter(new ParameterDefinition(sachem + "searchMode", getIriClass("search_mode"),
                new IRI(sachem + "substructureSearch")));
        subsearch.addParameter(new ParameterDefinition(sachem + "chargeMode", getIriClass("charge_mode"),
                new IRI(sachem + "defaultChargeAsAny")));
        subsearch.addParameter(new ParameterDefinition(sachem + "isotopeMode", getIriClass("isotope_mode"),
                new IRI(sachem + "ignoreIsotopes")));
        subsearch.addParameter(new ParameterDefinition(sachem + "stereoMode", getIriClass("stereo_mode"),
                new IRI(sachem + "ignoreStrereo")));
        subsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        subsearch.addResult(new ResultDefinition(molClass));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(sachem + "similaritySearch",
                new Function(schema, "sachem_similarity_search"));
        simsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simsearch.addResult(new ResultDefinition(sachem + "compound", molClass, "compound"));
        simsearch.addResult(new ResultDefinition(sachem + "score", xsdDouble, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(sachem + "similarCompoundSearch",
                new Function(schema, "sachem_similarity_search"));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simcmpsearch
                .addParameter(new ParameterDefinition(sachem + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simcmpsearch.addResult(new ResultDefinition(null, molClass, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);
    }
}
