package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdFloatIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;



public class PubChemConfiguration extends SparqlDatabaseConfiguration
{
    static final LangStringConstantTagClass rdfLangStringEn = LangStringConstantTagClass.get("en");
    static final DateConstantZoneClass xsdDateM4 = DateConstantZoneClass.get(-4 * 60 * 60);


    public PubChemConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

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


    private void loadClasses()
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

        HashSet<String> queryFormatValues = new HashSet<String>();
        queryFormatValues.add(sachem + "UnspecifiedFormat");
        queryFormatValues.add(sachem + "SMILES");
        queryFormatValues.add(sachem + "MolFile");
        queryFormatValues.add(sachem + "RGroup");
        addIriClass(new UserIriClass("queryFormat", Arrays.asList("integer"), queryFormatValues));

        HashSet<String> graphModeValues = new HashSet<String>();
        graphModeValues.add(sachem + "substructureSearch");
        graphModeValues.add(sachem + "exactSearch");
        addIriClass(new UserIriClass("graphMode", Arrays.asList("integer"), graphModeValues));

        HashSet<String> chargeModeValues = new HashSet<String>();
        chargeModeValues.add(sachem + "ignoreCharges");
        chargeModeValues.add(sachem + "defaultChargeAsZero");
        chargeModeValues.add(sachem + "defaultChargeAsAny");
        addIriClass(new UserIriClass("chargeMode", Arrays.asList("integer"), chargeModeValues));

        HashSet<String> isotopeModeValues = new HashSet<String>();
        isotopeModeValues.add(sachem + "ignoreIsotopes");
        isotopeModeValues.add(sachem + "defaultIsotopeAsStandard");
        isotopeModeValues.add(sachem + "defaultIsotopeAsAny");
        addIriClass(new UserIriClass("isotopeMode", Arrays.asList("integer"), isotopeModeValues));

        HashSet<String> stereoModeValues = new HashSet<String>();
        stereoModeValues.add(sachem + "ignoreStrereo");
        stereoModeValues.add(sachem + "strictStereo");
        addIriClass(new UserIriClass("stereoMode", Arrays.asList("integer"), stereoModeValues));

        HashSet<String> tautomerModeValues = new HashSet<String>();
        tautomerModeValues.add(sachem + "ignoreTautomers");
        tautomerModeValues.add(sachem + "inchiTautomers");
        addIriClass(new UserIriClass("tautomerMode", Arrays.asList("integer"), tautomerModeValues));
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
        String sachem = prefixes.get("sachem");
        String fulltext = prefixes.get("fulltext");
        UserIriClass compound = getIriClass("compound");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(sachem + "substructureSearch",
                "sachem_substructure_search");
        subsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        subsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
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
        subsearch.addResult(new ResultDefinition(compound));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(sachem + "similaritySearch",
                "sachem_similarity_search");
        simsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("queryFormat"),
                new IRI(sachem + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", xsdFloat, new Literal("0.8", xsdFloatIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simsearch.addResult(new ResultDefinition(sachem + "compound", compound, "compound"));
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
        simcmpsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);


        /* fulltext:bioassaySearch */
        ProcedureDefinition bioassay = new ProcedureDefinition(fulltext + "bioassaySearch", "bioassay");
        bioassay.addParameter(new ParameterDefinition(fulltext + "query", xsdString, null));
        bioassay.addResult(new ResultDefinition(getIriClass("bioassay")));
        procedures.put(bioassay.getProcedureName(), bioassay);
    }
}
