package cz.iocb.chemweb.server.sparql.pubchem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
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



public class PubChemConfiguration extends SparqlDatabaseConfiguration
{
    private static PubChemConfiguration singleton;


    public PubChemConfiguration() throws FileNotFoundException, IOException, SQLException
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/datasource-postgresql.properties"));

        connectionPool = new ConnectionPool(properties);
        schema = new PostgresSchema(connectionPool);

        loadPrefixes();
        loadClasses();
        loadQuadMapping();
        loadProcedures();
    }


    public static PubChemConfiguration get() throws FileNotFoundException, IOException, SQLException
    {
        if(singleton != null)
            return singleton;

        synchronized(PubChemConfiguration.class)
        {
            if(singleton != null)
                return singleton;

            singleton = new PubChemConfiguration();
        }

        return singleton;
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

        prefixes.put("orchem", "http://bioinfo.iocb.cz/rdf/0.9/orchem#");
        prefixes.put("fulltext", "http://bioinfo.iocb.cz/rdf/0.9/fulltext#");
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
        String orchem = prefixes.get("orchem");
        String fulltext = prefixes.get("fulltext");

        IriClass compound = getIriClass("compound");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(orchem + "substructureSearch",
                "orchem_substructure_search");
        subsearch.addParameter(new ParameterDefinition(orchem + "query", LiteralClass.xsdString, null));
        subsearch.addParameter(
                new ParameterDefinition(orchem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        subsearch.addParameter(new ParameterDefinition(orchem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        subsearch.addParameter(new ParameterDefinition(orchem + "strictStereo", LiteralClass.xsdBoolean,
                new Literal("false", new IRI(Xsd.BOOLEAN))));
        subsearch.addParameter(new ParameterDefinition(orchem + "exact", LiteralClass.xsdBoolean,
                new Literal("false", new IRI(Xsd.BOOLEAN))));
        subsearch.addParameter(new ParameterDefinition(orchem + "tautomers", LiteralClass.xsdBoolean,
                new Literal("false", new IRI(Xsd.BOOLEAN))));
        subsearch.addResult(new ResultDefinition(compound));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(orchem + "similaritySearch",
                "orchem_similarity_search");
        simsearch.addParameter(new ParameterDefinition(orchem + "query", LiteralClass.xsdString, null));
        simsearch.addParameter(
                new ParameterDefinition(orchem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        simsearch.addParameter(new ParameterDefinition(orchem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simsearch.addParameter(new ParameterDefinition(orchem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simsearch.addResult(new ResultDefinition(orchem + "compound", compound, "compound"));
        simsearch.addResult(new ResultDefinition(orchem + "score", LiteralClass.xsdFloat, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(orchem + "similarCompoundSearch",
                "orchem_similarity_search");
        simcmpsearch.addParameter(new ParameterDefinition(orchem + "query", LiteralClass.xsdString, null));
        simcmpsearch.addParameter(
                new ParameterDefinition(orchem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        simcmpsearch.addParameter(new ParameterDefinition(orchem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simcmpsearch.addParameter(new ParameterDefinition(orchem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simcmpsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);


        /* orchem:smartsSearch */
        ProcedureDefinition smartssearch = new ProcedureDefinition(orchem + "smartsSearch", "smartsSearch");
        smartssearch.addParameter(new ParameterDefinition(orchem + "query", LiteralClass.xsdString, null));
        smartssearch.addParameter(new ParameterDefinition(orchem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        smartssearch.addResult(new ResultDefinition(compound));
        procedures.put(smartssearch.getProcedureName(), smartssearch);


        /* fulltext:bioassaySearch */
        ProcedureDefinition bioassay = new ProcedureDefinition(fulltext + "bioassaySearch", "bioassay");
        bioassay.addParameter(new ParameterDefinition(fulltext + "query", LiteralClass.xsdString, null));
        bioassay.addResult(new ResultDefinition(getIriClass("bioassay")));
        procedures.put(bioassay.getProcedureName(), bioassay);
    }
}
