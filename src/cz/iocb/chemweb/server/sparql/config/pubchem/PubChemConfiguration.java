package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntegerIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.EnumUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class PubChemConfiguration extends SparqlDatabaseConfiguration
{
    static final String molecules = "molecules";
    static final String schema = "pubchem";

    static final LangStringConstantTagClass rdfLangStringEn = LangStringConstantTagClass.get("en");
    static final DateConstantZoneClass xsdDateM4 = DateConstantZoneClass.get(-4 * 60 * 60);


    public PubChemConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

        loadPrefixes();
        loadClasses();
        loadQuadMapping();
        loadProcedures();
        loadConstraints();
    }


    private void loadPrefixes()
    {
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");

        prefixes.put("compound", "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/");
        prefixes.put("substance", "http://rdf.ncbi.nlm.nih.gov/pubchem/substance/");
        prefixes.put("descriptor", "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/");
        prefixes.put("synonym", "http://rdf.ncbi.nlm.nih.gov/pubchem/synonym/");
        prefixes.put("inchikey", "http://rdf.ncbi.nlm.nih.gov/pubchem/inchikey/");
        prefixes.put("bioassay", "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/");
        prefixes.put("measuregroup", "http://rdf.ncbi.nlm.nih.gov/pubchem/measuregroup/");
        prefixes.put("endpoint", "http://rdf.ncbi.nlm.nih.gov/pubchem/endpoint/");
        prefixes.put("reference", "http://rdf.ncbi.nlm.nih.gov/pubchem/reference/");
        prefixes.put("protein", "http://rdf.ncbi.nlm.nih.gov/pubchem/protein/");
        prefixes.put("conserveddomain", "http://rdf.ncbi.nlm.nih.gov/pubchem/conserveddomain/");
        prefixes.put("gene", "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/");
        prefixes.put("biosystem", "http://rdf.ncbi.nlm.nih.gov/pubchem/biosystem/");
        prefixes.put("source", "http://rdf.ncbi.nlm.nih.gov/pubchem/source/");
        prefixes.put("concept", "http://rdf.ncbi.nlm.nih.gov/pubchem/concept/");
        prefixes.put("vocab", "http://rdf.ncbi.nlm.nih.gov/pubchem/vocabulary#");
        prefixes.put("obo", "http://purl.obolibrary.org/obo/");
        prefixes.put("sio", "http://semanticscience.org/resource/");
        prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
        prefixes.put("bao", "http://www.bioassayontology.org/bao#");
        prefixes.put("bp", "http://www.biopax.org/release/biopax-level3.owl#");
        prefixes.put("ndfrt", "http://evs.nci.nih.gov/ftp1/NDF-RT/NDF-RT.owl#");
        prefixes.put("ncit", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#");
        prefixes.put("wikidata", "http://www.wikidata.org/entity/");
        prefixes.put("ops", "http://www.openphacts.org/units/");
        prefixes.put("cito", "http://purl.org/spar/cito/");
        prefixes.put("fabio", "http://purl.org/spar/fabio/");
        prefixes.put("uniprot", "http://purl.uniprot.org/uniprot/");
        prefixes.put("pdbo", "http://rdf.wwpdb.org/schema/pdbx-v40.owl#");
        prefixes.put("pdbr", "http://rdf.wwpdb.org/pdb/");
        prefixes.put("taxonomy", "http://identifiers.org/taxonomy/");
        prefixes.put("reactome", "http://identifiers.org/reactome/");
        prefixes.put("chembl", "http://rdf.ebi.ac.uk/resource/chembl/molecule/");
        prefixes.put("chemblchembl", "http://linkedchemistry.info/chembl/chemblid/");
        prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
        prefixes.put("void", "http://rdfs.org/ns/void#");
        prefixes.put("dcterms", "http://purl.org/dc/terms/");
        prefixes.put("ensembl", "http://rdf.ebi.ac.uk/resource/ensembl/");
        prefixes.put("pubchem", "http://rdf.ncbi.nlm.nih.gov/pubchem/");
        prefixes.put("descriptor", "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/");
        prefixes.put("template", "http://bioinfo.iocb.cz/0.9/template#");

        prefixes.put("sachem", "http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#");
        prefixes.put("fulltext", "http://bioinfo.uochb.cas.cz/rdf/v1.0/fulltext#");
    }


    @SuppressWarnings("serial")
    private void loadClasses() throws SQLException
    {
        Bioassay.addIriClasses(this);
        Biosystem.addIriClasses(this);
        Compound.addIriClasses(this);
        Concept.addIriClasses(this);
        ConservedDomain.addIriClasses(this);
        CompoundDescriptor.addIriClasses(this);
        SubstanceDescriptor.addIriClasses(this);
        Endpoint.addIriClasses(this);
        Gene.addIriClasses(this);
        InchiKey.addIriClasses(this);
        Measuregroup.addIriClasses(this);
        Ontology.addIriClasses(this);
        Protein.addIriClasses(this);
        Reference.addIriClasses(this);
        Shared.addIriClasses(this);
        Source.addIriClasses(this);
        Substance.addIriClasses(this);
        Synonym.addIriClasses(this);


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
    }


    private void loadQuadMapping()
    {
        Bioassay.addQuadMapping(this);
        Biosystem.addQuadMapping(this);
        Compound.addQuadMapping(this);
        Concept.addQuadMapping(this);
        ConservedDomain.addQuadMapping(this);
        CompoundDescriptor.addQuadMapping(this);
        SubstanceDescriptor.addQuadMapping(this);
        Endpoint.addQuadMapping(this);
        Gene.addQuadMapping(this);
        InchiKey.addQuadMapping(this);
        Measuregroup.addQuadMapping(this);
        Ontology.addQuadMapping(this);
        Protein.addQuadMapping(this);
        Reference.addQuadMapping(this);
        Source.addQuadMapping(this);
        Substance.addQuadMapping(this);
        Synonym.addQuadMapping(this);
    }


    private void loadProcedures()
    {
        String index = "pubchem";
        String sachem = prefixes.get("sachem");
        String fulltext = prefixes.get("fulltext");
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
                new IRI(sachem + "aromaticityDetectIfMissing")));
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
                new IRI(sachem + "aromaticityDetectIfMissing")));
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
                new Function("sachem", "sparq_substructure_search_stub"));

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
                getIriClass("aromaticity_mode"), new IRI(sachem + "aromaticityDetectIfMissing")));
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
                new IRI(sachem + "aromaticityDetectIfMissing")));
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
                new IRI(sachem + "aromaticityDetectIfMissing")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simcmpsearch
                .addParameter(new ParameterDefinition(sachem + "topn", xsdInteger, new Literal("-1", xsdIntegerIri)));
        simcmpsearch.addParameter(new ParameterDefinition("#sort", xsdBoolean, new Literal("false", xsdBooleanIri)));

        simcmpsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);


        /* fulltext:bioassaySearch */
        ProcedureDefinition bioassay = new ProcedureDefinition(fulltext + "bioassaySearch",
                new Function(schema, "bioassay"));
        bioassay.addParameter(new ParameterDefinition(fulltext + "query", xsdString, null));
        bioassay.addResult(new ResultDefinition(getIriClass("bioassay")));
        procedures.put(bioassay.getProcedureName(), bioassay);


        /* fulltext:compoundSearch */
        ProcedureDefinition compoundSearch = new ProcedureDefinition(fulltext + "compoundSearch",
                new Function(schema, "compound"));
        compoundSearch.addParameter(new ParameterDefinition(fulltext + "query", xsdString, null));
        compoundSearch.addResult(new ResultDefinition(fulltext + "compound", compound, "compound"));
        compoundSearch.addResult(new ResultDefinition(fulltext + "name", rdfLangStringEn, "name"));
        procedures.put(compoundSearch.getProcedureName(), compoundSearch);
    }


    private void loadConstraints() throws SQLException
    {
        try(Connection connection = getConnectionPool().getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement
                        .executeQuery("select parent_table, parent_columns, foreign_table, foreign_columns from "
                                + schema + ".schema_foreign_keys"))
                {
                    while(result.next())
                    {
                        Table parentTable = new Table(schema, result.getString(1));
                        List<Column> parentColumns = getColumns((String[]) result.getArray(2).getArray());

                        Table foreignTable = new Table(schema, result.getString(3));
                        List<Column> foreignColumns = getColumns((String[]) result.getArray(4).getArray());

                        databaseSchema.addForeignKeys(parentTable, parentColumns, foreignTable, foreignColumns);
                    }
                }
            }


            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement
                        .executeQuery("select left_table, left_columns, right_table, right_columns from " + schema
                                + ".schema_unjoinable_columns"))
                {
                    while(result.next())
                    {
                        Table leftTable = new Table(schema, result.getString(1));
                        List<Column> leftColumns = getColumns((String[]) result.getArray(2).getArray());

                        Table rightTable = new Table(schema, result.getString(3));
                        List<Column> rightColumns = getColumns((String[]) result.getArray(4).getArray());

                        databaseSchema.addUnjoinableColumns(leftTable, leftColumns, rightTable, rightColumns);
                        databaseSchema.addUnjoinableColumns(rightTable, rightColumns, leftTable, leftColumns);
                    }
                }
            }
        }
    }
}
