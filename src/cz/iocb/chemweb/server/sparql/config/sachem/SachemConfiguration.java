package cz.iocb.chemweb.server.sparql.config.sachem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdFloatIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
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
    private final String index;
    private final String schema;
    private final String table;


    protected SachemConfiguration(DataSource connectionPool, String index, String schema, String table,
            String iriPrefix, String idPattern) throws SQLException
    {
        super(connectionPool);
        this.index = index;
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

        addIriClass(new EnumUserIriClass("query_format", "query_format", new HashMap<String, String>()
        {
            {
                put("UNSPECIFIED", sachem + "UnspecifiedFormat");
                put("SMILES", sachem + "SMILES");
                put("MOLFILE", sachem + "MolFile");
                put("RGROUP", sachem + "RGroup");
            }
        }));

        addIriClass(new EnumUserIriClass("search_mode", "search_mode", new HashMap<String, String>()
        {
            {
                put("SUBSTRUCTURE", sachem + "substructureSearch");
                put("EXACT", sachem + "exactSearch");
            }
        }));

        addIriClass(new EnumUserIriClass("charge_mode", "charge_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreCharges");
                put("DEFAULT_AS_UNCHARGED", sachem + "defaultChargeAsZero");
                put("DEFAULT_AS_ANY", sachem + "defaultChargeAsAny");
            }
        }));

        addIriClass(new EnumUserIriClass("isotope_mode", "isotope_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreIsotopes");
                put("DEFAULT_AS_STANDARD", sachem + "defaultIsotopeAsStandard");
                put("DEFAULT_AS_ANY", sachem + "defaultIsotopeAsAny");
            }
        }));

        addIriClass(new EnumUserIriClass("stereo_mode", "stereo_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreStereo");
                put("STRICT", sachem + "strictStereo");
            }
        }));

        addIriClass(new EnumUserIriClass("aromaticity_mode", "aromaticity_mode", new HashMap<String, String>()
        {
            {
                put("PRESERVE", sachem + "preserveAromaticity");
                put("DETECT", sachem + "detectAromaticity");
                put("AUTO", sachem + "detectAromaticityIfNeeded");
            }
        }));

        addIriClass(new EnumUserIriClass("tautomer_mode", "tautomer_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreTautomers");
                put("INCHI", sachem + "inchiTautomers");
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
        UserIriClass compound = getIriClass("compound");


        /* sachem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(sachem + "substructureSearch",
                new Function(sachem, "substructure_search"));

        subsearch.addParameter(new ParameterDefinition(null, xsdString, new Literal(index, xsdStringIri)));
        subsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        subsearch.addParameter(new ParameterDefinition(sachem + "searchMode", getIriClass("search_mode"),
                new IRI(sachem + "substructureSearch")));
        subsearch.addParameter(new ParameterDefinition(sachem + "chargeMode", getIriClass("charge_mode"),
                new IRI(sachem + "defaultChargeAsAny")));
        subsearch.addParameter(new ParameterDefinition(sachem + "isotopeMode", getIriClass("isotope_mode"),
                new IRI(sachem + "ignoreIsotopes")));
        subsearch.addParameter(new ParameterDefinition(sachem + "stereoMode", getIriClass("stereo_mode"),
                new IRI(sachem + "ignoreStereo")));
        subsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode", getIriClass("aromaticity_mode"),
                new IRI(sachem + "detectAromaticityIfNeeded")));
        subsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        subsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));

        subsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* sachem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(sachem + "similaritySearch",
                new Function(sachem, "similarity_search"));

        simsearch.addParameter(new ParameterDefinition(null, xsdString, new Literal(index, xsdStringIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", xsdFloat, new Literal("0.8", xsdFloatIri)));
        simsearch.addParameter(
                new ParameterDefinition(sachem + "similarityFingerprintDepth", xsdInt, new Literal("1", xsdIntIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode", getIriClass("aromaticity_mode"),
                new IRI(sachem + "detectAromaticityIfNeeded")));
        simsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        simsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));

        simsearch.addResult(new ResultDefinition(sachem + "compound", compound, "compound"));
        simsearch.addResult(new ResultDefinition(sachem + "score", xsdFloat, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* sachem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(sachem + "similarCompoundSearch",
                new Function(sachem, "similarity_search"));

        simcmpsearch.addParameter(new ParameterDefinition(null, xsdString, new Literal(index, xsdStringIri)));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simcmpsearch
                .addParameter(new ParameterDefinition(sachem + "cutoff", xsdFloat, new Literal("0.8", xsdFloatIri)));
        simcmpsearch.addParameter(
                new ParameterDefinition(sachem + "similarityFingerprintDepth", xsdInt, new Literal("1", xsdIntIri)));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode", getIriClass("aromaticity_mode"),
                new IRI(sachem + "detectAromaticityIfNeeded")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));

        simcmpsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);
    }
}
