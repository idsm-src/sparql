package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class PubChemConfiguration extends SparqlDatabaseConfiguration
{
    static final String sachem = "sachem";
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


        String sachem = "http://bioinfo\\.uochb\\.cas\\.cz/rdf/v1\\.0/sachem#";

        String queryFormatPattern = sachem + "(UnspecifiedFormat|SMILES|MolFile|RGroup)";
        addIriClass(new UserIriClass(schema, "query_format", Arrays.asList("integer"), queryFormatPattern));

        String searchModePattern = sachem + "(substructureSearch|exactSearch)";
        addIriClass(new UserIriClass(schema, "search_mode", Arrays.asList("integer"), searchModePattern));

        String chargeModePattern = sachem + "(ignoreCharges|defaultChargeAsZero|defaultChargeAsAny)";
        addIriClass(new UserIriClass(schema, "charge_mode", Arrays.asList("integer"), chargeModePattern));

        String isotopeModePattern = sachem + "(ignoreIsotopes|defaultIsotopeAsStandard|defaultIsotopeAsAny)";
        addIriClass(new UserIriClass(schema, "isotope_mode", Arrays.asList("integer"), isotopeModePattern));

        String stereoModePattern = sachem + "(ignoreStrereo|strictStereo)";
        addIriClass(new UserIriClass(schema, "stereo_mode", Arrays.asList("integer"), stereoModePattern));

        String tautomerModePattern = sachem + "(ignoreTautomers|inchiTautomers)";
        addIriClass(new UserIriClass(schema, "tautomer_mode", Arrays.asList("integer"), tautomerModePattern));
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
        String prefix = prefixes.get("sachem");
        String fulltext = prefixes.get("fulltext");
        UserIriClass compound = getIriClass("compound");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(prefix + "substructureSearch",
                new Function(sachem, "sachem_substructure_search"));
        subsearch.addParameter(new ParameterDefinition(prefix + "query", xsdString, null));
        subsearch.addParameter(new ParameterDefinition(prefix + "queryFormat", getIriClass("query_format"),
                new IRI(prefix + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(prefix + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        subsearch.addParameter(new ParameterDefinition(prefix + "searchMode", getIriClass("search_mode"),
                new IRI(prefix + "substructureSearch")));
        subsearch.addParameter(new ParameterDefinition(prefix + "chargeMode", getIriClass("charge_mode"),
                new IRI(prefix + "defaultChargeAsAny")));
        subsearch.addParameter(new ParameterDefinition(prefix + "isotopeMode", getIriClass("isotope_mode"),
                new IRI(prefix + "ignoreIsotopes")));
        subsearch.addParameter(new ParameterDefinition(prefix + "stereoMode", getIriClass("stereo_mode"),
                new IRI(prefix + "ignoreStrereo")));
        subsearch.addParameter(new ParameterDefinition(prefix + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(prefix + "ignoreTautomers")));
        subsearch.addResult(new ResultDefinition(compound));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(prefix + "similaritySearch",
                new Function(sachem, "sachem_similarity_search"));
        simsearch.addParameter(new ParameterDefinition(prefix + "query", xsdString, null));
        simsearch.addParameter(new ParameterDefinition(prefix + "queryFormat", getIriClass("query_format"),
                new IRI(prefix + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(prefix + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simsearch.addParameter(new ParameterDefinition(prefix + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simsearch.addResult(new ResultDefinition(prefix + "compound", compound, "compound"));
        simsearch.addResult(new ResultDefinition(prefix + "score", xsdDouble, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(prefix + "similarCompoundSearch",
                new Function(sachem, "sachem_similarity_search"));
        simcmpsearch.addParameter(new ParameterDefinition(prefix + "query", xsdString, null));
        simcmpsearch.addParameter(new ParameterDefinition(prefix + "queryFormat", getIriClass("query_format"),
                new IRI(prefix + "UnspecifiedFormat")));
        simcmpsearch
                .addParameter(new ParameterDefinition(prefix + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simcmpsearch.addParameter(new ParameterDefinition(prefix + "topn", xsdInt, new Literal("-1", xsdIntIri)));
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
