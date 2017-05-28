package cz.iocb.chemweb.server.sparql.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass.xsdString;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import cz.iocb.chemweb.server.db.postgresql.ConnectionPool;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ConstantLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.Xsd;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;



public class PubChemMapping
{
    private static HashMap<String, String> prefixes = new HashMap<String, String>();
    private static ArrayList<ResourceClass> classes = new ArrayList<ResourceClass>();
    private static List<QuadMapping> mappings = new ArrayList<QuadMapping>();
    private static LinkedHashMap<String, ProcedureDefinition> procedures = new LinkedHashMap<String, ProcedureDefinition>();


    static
    {
        loadPrefixes();
        loadClasses();
        loadMapping();
        loadProcedures();
    }


    private static void loadPrefixes()
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


    private static void loadClasses()
    {
        classes.addAll(LiteralClass.getClasses());

        Bioassay.loadClasses();
        Biosystem.loadClasses();
        Compound.loadClasses();
        Concept.loadClasses();
        ConservedDomain.loadClasses();
        CompoundDescriptor.loadClasses();
        SubstanceDescriptor.loadClasses();
        Endpoint.loadClasses();
        Gene.loadClasses();
        InchiKey.loadClasses();
        Measuregroup.loadClasses();
        Ontology.loadClasses();
        Protein.loadClasses();
        Reference.loadClasses();
        Shared.loadClasses();
        Source.loadClasses();
        Substance.loadClasses();
        Synonym.loadClasses();
    }


    private static void loadMapping()
    {
        Bioassay.loadMapping();
        Biosystem.loadMapping();
        Compound.loadMapping();
        Concept.loadMapping();
        ConservedDomain.loadMapping();
        CompoundDescriptor.loadMapping();
        SubstanceDescriptor.loadMapping();
        Endpoint.loadMapping();
        Gene.loadMapping();
        InchiKey.loadMapping();
        Measuregroup.loadMapping();
        Ontology.loadMapping();
        Protein.loadMapping();
        Reference.loadMapping();
        Source.loadMapping();
        Substance.loadMapping();
        Synonym.loadMapping();
    }


    private static void loadProcedures()
    {
        String orchem = prefixes.get("orchem");
        String fulltext = prefixes.get("fulltext");

        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(orchem + "substructureSearch", "substructureSearch",
                null);
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
        subsearch.addResult(new ResultDefinition(Compound.compound));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(orchem + "similaritySearch", "similaritySearch",
                "OrChemCompound");
        simsearch.addParameter(new ParameterDefinition(orchem + "query", LiteralClass.xsdString, null));
        simsearch.addParameter(
                new ParameterDefinition(orchem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        simsearch.addParameter(new ParameterDefinition(orchem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simsearch.addParameter(new ParameterDefinition(orchem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simsearch.addResult(new ResultDefinition(orchem + "compound", Compound.compound, "@f1"));
        simsearch.addResult(new ResultDefinition(orchem + "score", LiteralClass.xsdFloat, "@f2"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(orchem + "similarCompoundSearch",
                "similarCompoundSearch", null);
        simcmpsearch.addParameter(new ParameterDefinition(orchem + "query", LiteralClass.xsdString, null));
        simcmpsearch.addParameter(
                new ParameterDefinition(orchem + "queryType", LiteralClass.xsdString, new Literal("SMILES")));
        simcmpsearch.addParameter(new ParameterDefinition(orchem + "cutoff", LiteralClass.xsdFloat,
                new Literal("0.8", new IRI(Xsd.FLOAT))));
        simcmpsearch.addParameter(new ParameterDefinition(orchem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        simcmpsearch.addResult(new ResultDefinition(Compound.compound));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);


        /* orchem:smartsSearch */
        ProcedureDefinition smartssearch = new ProcedureDefinition(orchem + "smartsSearch", "smartsSearch", null);
        smartssearch.addParameter(new ParameterDefinition(orchem + "query", LiteralClass.xsdString, null));
        smartssearch.addParameter(new ParameterDefinition(orchem + "topn", LiteralClass.xsdInteger,
                new Literal("-1", new IRI(Xsd.INTEGER))));
        smartssearch.addResult(new ResultDefinition(Compound.compound));
        procedures.put(smartssearch.getProcedureName(), smartssearch);


        /* fulltext:bioassaySearch */
        ProcedureDefinition bioassay = new ProcedureDefinition(fulltext + "bioassaySearch", "bioassay", null);
        bioassay.addParameter(new ParameterDefinition(fulltext + "query", LiteralClass.xsdString, null));
        bioassay.addResult(new ResultDefinition(Bioassay.bioassay));
        procedures.put(bioassay.getProcedureName(), bioassay);
    }


    public static synchronized HashMap<String, String> getPrefixes()
    {
        return PubChemMapping.prefixes;
    }


    public static synchronized List<ResourceClass> getClasses()
    {
        return PubChemMapping.classes;
    }


    public static synchronized List<QuadMapping> getMappings()
    {
        return PubChemMapping.mappings;
    }


    public static synchronized LinkedHashMap<String, ProcedureDefinition> getProcedures()
    {
        return PubChemMapping.procedures;
    }


    protected static void classmap(IriClass iriClass)
    {
        classes.add(iriClass);
    }


    protected static NodeMapping iri(IriClass iriClass, String... columns)
    {
        return new ParametrisedIriMapping(iriClass, Arrays.asList(columns));
    }


    protected static ConstantIriMapping iri(String value)
    {
        String iri = null;

        if(value.startsWith("<"))
        {
            iri = value.substring(1, value.length() - 1);
        }
        else
        {
            String[] parts = value.split(":");
            iri = prefixes.get(parts[0]) + parts[1];
        }

        IriClass iriClass = null;

        for(ResourceClass c : classes)
        {
            if(c instanceof IriClass && ((IriClass) c).match(iri))
            {
                if(iriClass != null)
                    throw new RuntimeException("ambigous iri class for " + value);

                iriClass = (IriClass) c;
            }
        }

        assert iriClass != null;

        if(iriClass == null)
            throw new RuntimeException("no iri class for " + value);

        return new ConstantIriMapping(iriClass, iri);
    }


    protected static NodeMapping literal(LiteralClass literalClass, String column)
    {
        return new ParametrisedLiteralMapping(literalClass, column);
    }


    protected static NodeMapping literal(String value)
    {
        return new ConstantLiteralMapping(xsdString, new Literal(value));
    }


    protected static void quad(String table, NodeMapping graph, NodeMapping subject, NodeMapping predicate,
            NodeMapping object)
    {
        QuadMapping map = new QuadMapping(table, graph, subject, predicate, object);
        mappings.add(map);
    }


    protected static void quad(String table, NodeMapping graph, NodeMapping subject, NodeMapping predicate,
            NodeMapping object, String condition)
    {
        QuadMapping map = new QuadMapping(table, graph, subject, predicate, object, condition);
        mappings.add(map);
    }


    protected static HashSet<String> getIriValues(String table)
    {
        HashSet<String> set = new HashSet<String>();

        try (Connection connection = ConnectionPool.getConnection())
        {
            try (PreparedStatement statement = connection.prepareStatement("select iri from " + table))
            {
                try (java.sql.ResultSet result = statement.executeQuery())
                {
                    while(result.next())
                        set.add(result.getString(1));
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return set;
    }
}
