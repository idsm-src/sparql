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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
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
    protected SachemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema, String index,
            Table table, String iriPrefix, UserIriClass compound, UserIriClass molfile, List<Column> resultFields,
            List<Column> molfileFields) throws SQLException
    {
        super(service, connectionPool, schema);

        loadPrefixes(iriPrefix);
        loadClasses(compound, molfile);
        loadQuadMapping(index, table, molfileFields, compound, molfile);
        loadProcedures(index, resultFields, compound);
    }


    protected SachemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema, String index,
            Table table, String iriPrefix, String idPrefix, int idNumberLength) throws SQLException
    {
        this(service, connectionPool, schema, index, table, iriPrefix,
                new IntegerUserIriClass(index + ":compound", "integer", iriPrefix + idPrefix, idNumberLength),
                new IntegerUserIriClass(index + ":molfile", "integer", iriPrefix + idPrefix, idNumberLength,
                        "_Molfile"),
                Arrays.asList(new TableColumn("compound")), Arrays.asList(new TableColumn("id")));
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
    private void loadClasses(UserIriClass compound, UserIriClass molfile)
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


        addIriClass(compound);
        addIriClass(molfile);
    }


    private void loadQuadMapping(String index, Table table, List<Column> molfileFields, UserIriClass compound,
            UserIriClass molfile) throws SQLException
    {
        NodeMapping subject = createIriMapping(molfile, "id");

        addQuadMapping(table, null, subject, createIriMapping("rdf:type"), createIriMapping("sio:SIO_011120"));
        addQuadMapping(table, null, subject, createIriMapping("sio:is-attribute-of"),
                createIriMapping(compound, molfileFields));
        addQuadMapping(table, null, subject, createIriMapping("sio:has-value"),
                createLiteralMapping(xsdString, "molfile"));
    }


    private void loadProcedures(String index, List<Column> resultFields, UserIriClass compound)
    {
        String sachem = prefixes.get("sachem");


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

        exactsearch.addResult(new ResultDefinition(null, compound, resultFields));
        addProcedure(exactsearch);


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

        subsearch.addResult(new ResultDefinition(null, compound, resultFields));
        addProcedure(subsearch);


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

        scoredsubsearch.addResult(new ResultDefinition(sachem + "compound", compound, resultFields));
        scoredsubsearch.addResult(new ResultDefinition(sachem + "score", xsdDouble, "score"));
        addProcedure(scoredsubsearch);


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

        simsearch.addResult(new ResultDefinition(sachem + "compound", compound, resultFields));
        simsearch.addResult(new ResultDefinition(sachem + "score", xsdDouble, "score"));
        addProcedure(simsearch);


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

        simcmpsearch.addResult(new ResultDefinition(null, compound, resultFields));
        addProcedure(simcmpsearch);
    }
}
