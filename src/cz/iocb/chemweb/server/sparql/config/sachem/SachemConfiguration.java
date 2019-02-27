package cz.iocb.chemweb.server.sparql.config.sachem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdFloatIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import java.sql.SQLException;
import java.util.Arrays;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public abstract class SachemConfiguration extends SparqlDatabaseConfiguration
{
    protected SachemConfiguration(DataSource connectionPool, String iriPrefix, String idPattern) throws SQLException
    {
        super(connectionPool);

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

        String queryFormatPattern = sachem + "(UnspecifiedFormat|SMILES|MolFile|RGroup)";
        addIriClass(new UserIriClass("queryFormat", Arrays.asList("integer"), queryFormatPattern));

        String searchModePattern = sachem + "(substructureSearch|exactSearch)";
        addIriClass(new UserIriClass("searchMode", Arrays.asList("integer"), searchModePattern));

        String chargeModePattern = sachem + "(ignoreCharges|defaultChargeAsZero|defaultChargeAsAny)";
        addIriClass(new UserIriClass("chargeMode", Arrays.asList("integer"), chargeModePattern));

        String isotopeModePattern = sachem + "(ignoreIsotopes|defaultIsotopeAsStandard|defaultIsotopeAsAny)";
        addIriClass(new UserIriClass("isotopeMode", Arrays.asList("integer"), isotopeModePattern));

        String stereoModePattern = sachem + "(ignoreStrereo|strictStereo)";
        addIriClass(new UserIriClass("stereoMode", Arrays.asList("integer"), stereoModePattern));

        String tautomerModePattern = sachem + "(ignoreTautomers|inchiTautomers)";
        addIriClass(new UserIriClass("tautomerMode", Arrays.asList("integer"), tautomerModePattern));

        addIriClass(new UserIriClass("compound", Arrays.asList("integer"), iriPrefix + idPattern));
        addIriClass(new UserIriClass("compound_molfile", Arrays.asList("integer"), iriPrefix + idPattern + "_Molfile"));

        String classPattern = "http://semanticscience.org/resource/SIO_011120";
        addIriClass(new UserIriClass("class", Arrays.asList("integer"), classPattern));

        String propertyPattern = "(http://www.w3.org/1999/02/22-rdf-syntax-ns#type|http://semanticscience.org/resource/(is-attribute-of|has-value))";
        addIriClass(new UserIriClass("property", Arrays.asList("integer"), propertyPattern));
    }


    private void loadQuadMapping() throws SQLException
    {
        UserIriClass compound = getIriClass("compound");

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
        UserIriClass molClass = getIriClass("compound");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition(
                sachem + "substructureSearch", "sachem_substructure_search");
        subsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        subsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        subsearch.addParameter(new ParameterDefinition(sachem + "searchMode", getIriClass("searchMode"),
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
        simsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", xsdFloat, new Literal("0.8", xsdFloatIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simsearch.addResult(new ResultDefinition(sachem + "compound", molClass, "compound"));
        simsearch.addResult(new ResultDefinition(sachem + "score", xsdFloat, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(sachem + "similarCompoundSearch",
                "sachem_similarity_search");
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        simcmpsearch
                .addParameter(new ParameterDefinition(sachem + "cutoff", xsdFloat, new Literal("0.8", xsdFloatIri)));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simcmpsearch.addResult(new ResultDefinition(null, molClass, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);
    }
}
