package cz.iocb.chemweb.server.sparql.config.sachem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntegerIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.EnumUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public abstract class SachemConfiguration extends SparqlDatabaseConfiguration
{
    private final String index;
    private final String schema;
    private final String table;

    private final String iriPrefix;
    private final String idPrefix;
    private final int idNumberLength;


    protected SachemConfiguration(DataSource connectionPool, String index, String schema, String table,
            String iriPrefix, String idPrefix, int idNumberLength) throws SQLException
    {
        super(connectionPool);

        this.index = index;
        this.schema = schema;
        this.table = table;

        this.iriPrefix = iriPrefix;
        this.idPrefix = idPrefix;
        this.idNumberLength = idNumberLength;

        loadPrefixes();
        loadClasses();
        loadQuadMapping();
        loadProcedures();
    }


    private void loadPrefixes()
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
    private void loadClasses()
    {
        String sachem = prefixes.get("sachem");

        addIriClass(new EnumUserIriClass("query_format", "sachem.query_format", new HashMap<String, String>()
        {
            {
                put("UNSPECIFIED", sachem + "UnspecifiedFormat");
                put("SMILES", sachem + "SMILES");
                put("MOLFILE", sachem + "MolFile");
                put("RGROUP", sachem + "RGroup");
            }
        }));

        addIriClass(new EnumUserIriClass("search_mode", "sachem.search_mode", new HashMap<String, String>()
        {
            {
                put("SUBSTRUCTURE", sachem + "substructureSearch");
                put("EXACT", sachem + "exactSearch");
            }
        }));

        addIriClass(new EnumUserIriClass("charge_mode", "sachem.charge_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreCharges");
                put("DEFAULT_AS_UNCHARGED", sachem + "defaultChargeAsZero");
                put("DEFAULT_AS_ANY", sachem + "defaultChargeAsAny");
            }
        }));

        addIriClass(new EnumUserIriClass("isotope_mode", "sachem.isotope_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreIsotopes");
                put("DEFAULT_AS_STANDARD", sachem + "defaultIsotopeAsStandard");
                put("DEFAULT_AS_ANY", sachem + "defaultIsotopeAsAny");
            }
        }));

        addIriClass(new EnumUserIriClass("radical_mode", "sachem.radical_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreSpinMultiplicity");
                put("DEFAULT_AS_STANDARD", sachem + "defaultSpinMultiplicityAsZero");
                put("DEFAULT_AS_ANY", sachem + "defaultSpinMultiplicityAsAny");
            }
        }));

        addIriClass(new EnumUserIriClass("stereo_mode", "sachem.stereo_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreStereo");
                put("STRICT", sachem + "strictStereo");
            }
        }));

        addIriClass(new EnumUserIriClass("aromaticity_mode", "sachem.aromaticity_mode", new HashMap<String, String>()
        {
            {
                put("PRESERVE", sachem + "aromaticityFromQuery");
                put("DETECT", sachem + "aromaticityDetect");
                put("AUTO", sachem + "aromaticityDetectIfMissing");
            }
        }));

        addIriClass(new EnumUserIriClass("tautomer_mode", "sachem.tautomer_mode", new HashMap<String, String>()
        {
            {
                put("IGNORE", sachem + "ignoreTautomers");
                put("INCHI", sachem + "inchiTautomers");
            }
        }));


        addIriClass(new IntegerUserIriClass("compound", "integer", iriPrefix + idPrefix, idNumberLength));
        addIriClass(new IntegerUserIriClass("compound_molfile", "integer", iriPrefix + idPrefix, idNumberLength,
                "_Molfile"));
    }


    private void loadQuadMapping() throws SQLException
    {
        NodeMapping subject = createIriMapping("compound_molfile", "id");

        addQuadMapping(new Table(schema, table), null, subject, createIriMapping("rdf:type"),
                createIriMapping("sio:SIO_011120"));
        addQuadMapping(new Table(schema, table), null, subject, createIriMapping("sio:is-attribute-of"),
                createIriMapping("compound", "id"));
        addQuadMapping(new Table(schema, table), null, subject, createIriMapping("sio:has-value"),
                createLiteralMapping(xsdString, "molfile"));
    }


    private void loadProcedures()
    {
        String sachem = prefixes.get("sachem");
        UserIriClass compound = getIriClass("compound");


        /* sachem:exactSearch */
        ProcedureDefinition exactsearch = new ProcedureDefinition(sachem + "exactSearch",
                new Function("sachem", "substructure_search_stub"));

        exactsearch.addParameter(new ParameterDefinition("#index", xsdString, new Literal(index, xsdStringIri)));
        exactsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        exactsearch.addParameter(new ParameterDefinition(sachem + "searchMode", getIriClass("search_mode"),
                new IRI(sachem + "exactSearch")));
        exactsearch.addParameter(new ParameterDefinition(sachem + "chargeMode", getIriClass("charge_mode"),
                new IRI(sachem + "defaultChargeAsZero")));
        exactsearch.addParameter(new ParameterDefinition(sachem + "isotopeMode", getIriClass("isotope_mode"),
                new IRI(sachem + "defaultIsotopeAsStandard")));
        exactsearch.addParameter(new ParameterDefinition(sachem + "radicalMode", getIriClass("radical_mode"),
                new IRI(sachem + "defaultSpinMultiplicityAsZero")));
        exactsearch.addParameter(new ParameterDefinition(sachem + "stereoMode", getIriClass("stereo_mode"),
                new IRI(sachem + "strictStereo")));
        exactsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode", getIriClass("aromaticity_mode"),
                new IRI(sachem + "aromaticityDetect")));
        exactsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        exactsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        exactsearch
                .addParameter(new ParameterDefinition(sachem + "topn", xsdInteger, new Literal("-1", xsdIntegerIri)));
        exactsearch.addParameter(new ParameterDefinition("#sort", xsdBoolean, new Literal("false", xsdBooleanIri)));
        exactsearch.addParameter(
                new ParameterDefinition(sachem + "internalMatchingLimit", xsdInteger, new Literal("0", xsdIntegerIri)));

        exactsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(exactsearch.getProcedureName(), exactsearch);


        /* sachem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(sachem + "substructureSearch",
                new Function("sachem", "substructure_search_stub"));

        subsearch.addParameter(new ParameterDefinition("#index", xsdString, new Literal(index, xsdStringIri)));
        subsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        subsearch.addParameter(new ParameterDefinition(sachem + "searchMode", getIriClass("search_mode"),
                new IRI(sachem + "substructureSearch")));
        subsearch.addParameter(new ParameterDefinition(sachem + "chargeMode", getIriClass("charge_mode"),
                new IRI(sachem + "defaultChargeAsAny")));
        subsearch.addParameter(new ParameterDefinition(sachem + "isotopeMode", getIriClass("isotope_mode"),
                new IRI(sachem + "ignoreIsotopes")));
        subsearch.addParameter(new ParameterDefinition(sachem + "radicalMode", getIriClass("radical_mode"),
                new IRI(sachem + "ignoreSpinMultiplicity")));
        subsearch.addParameter(new ParameterDefinition(sachem + "stereoMode", getIriClass("stereo_mode"),
                new IRI(sachem + "ignoreStereo")));
        subsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode", getIriClass("aromaticity_mode"),
                new IRI(sachem + "aromaticityDetect")));
        subsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        subsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInteger, new Literal("-1", xsdIntegerIri)));
        subsearch.addParameter(new ParameterDefinition("#sort", xsdBoolean, new Literal("false", xsdBooleanIri)));
        subsearch.addParameter(
                new ParameterDefinition(sachem + "internalMatchingLimit", xsdInteger, new Literal("0", xsdIntegerIri)));

        subsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* sachem:scoredSubstructureSearch */
        ProcedureDefinition scoredsubsearch = new ProcedureDefinition(sachem + "scoredSubstructureSearch",
                new Function("sachem", "substructure_search_stub"));

        scoredsubsearch.addParameter(new ParameterDefinition("#index", xsdString, new Literal(index, xsdStringIri)));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "searchMode", getIriClass("search_mode"),
                new IRI(sachem + "substructureSearch")));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "chargeMode", getIriClass("charge_mode"),
                new IRI(sachem + "defaultChargeAsAny")));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "isotopeMode", getIriClass("isotope_mode"),
                new IRI(sachem + "ignoreIsotopes")));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "radicalMode", getIriClass("radical_mode"),
                new IRI(sachem + "ignoreSpinMultiplicity")));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "stereoMode", getIriClass("stereo_mode"),
                new IRI(sachem + "ignoreStereo")));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode",
                getIriClass("aromaticity_mode"), new IRI(sachem + "aromaticityDetect")));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        scoredsubsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        scoredsubsearch
                .addParameter(new ParameterDefinition(sachem + "topn", xsdInteger, new Literal("-1", xsdIntegerIri)));
        scoredsubsearch.addParameter(new ParameterDefinition("#sort", xsdBoolean, new Literal("false", xsdBooleanIri)));
        scoredsubsearch.addParameter(
                new ParameterDefinition(sachem + "internalMatchingLimit", xsdInteger, new Literal("0", xsdIntegerIri)));

        scoredsubsearch.addResult(new ResultDefinition(sachem + "compound", compound, "compound"));
        scoredsubsearch.addResult(new ResultDefinition(sachem + "score", xsdDouble, "score"));
        procedures.put(scoredsubsearch.getProcedureName(), scoredsubsearch);


        /* sachem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(sachem + "similaritySearch",
                new Function("sachem", "similarity_search_stub"));

        simsearch.addParameter(new ParameterDefinition("#index", xsdString, new Literal(index, xsdStringIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simsearch.addParameter(
                new ParameterDefinition(sachem + "similarityRadius", xsdInteger, new Literal("1", xsdIntegerIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode", getIriClass("aromaticity_mode"),
                new IRI(sachem + "aromaticityDetect")));
        simsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        simsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInteger, new Literal("-1", xsdIntegerIri)));
        simsearch.addParameter(new ParameterDefinition("#sort", xsdBoolean, new Literal("false", xsdBooleanIri)));

        simsearch.addResult(new ResultDefinition(sachem + "compound", compound, "compound"));
        simsearch.addResult(new ResultDefinition(sachem + "score", xsdDouble, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* sachem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(sachem + "similarCompoundSearch",
                new Function("sachem", "similarity_search_stub"));

        simcmpsearch.addParameter(new ParameterDefinition("#index", xsdString, new Literal(index, xsdStringIri)));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simcmpsearch
                .addParameter(new ParameterDefinition(sachem + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simcmpsearch.addParameter(
                new ParameterDefinition(sachem + "similarityRadius", xsdInteger, new Literal("1", xsdIntegerIri)));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "aromaticityMode", getIriClass("aromaticity_mode"),
                new IRI(sachem + "aromaticityDetect")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simcmpsearch
                .addParameter(new ParameterDefinition(sachem + "topn", xsdInteger, new Literal("-1", xsdIntegerIri)));
        simcmpsearch.addParameter(new ParameterDefinition("#sort", xsdBoolean, new Literal("false", xsdBooleanIri)));

        simcmpsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);
    }
}
