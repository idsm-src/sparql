package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.chebi.Chebi;
import cz.iocb.chemweb.server.sparql.config.chembl.Assay;
import cz.iocb.chemweb.server.sparql.config.chembl.Mechanism;
import cz.iocb.chemweb.server.sparql.config.chembl.Molecule;
import cz.iocb.chemweb.server.sparql.config.common.Common;
import cz.iocb.chemweb.server.sparql.config.mesh.Mesh;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.EnumUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ResultDefinition;



public class PubChemConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "pubchem";

    public static final LangStringConstantTagClass rdfLangStringEn = LangStringConstantTagClass.get("en");
    public static final DateConstantZoneClass xsdDateM4 = DateConstantZoneClass.get(-4 * 60 * 60);


    public PubChemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema) throws SQLException
    {
        super(service, connectionPool, schema);

        addPrefixes(this);

        addResourceClasses(this);
        Common.addResourceClasses(this);
        Chebi.addResourceClasses(this);
        Molecule.addResourceClasses(this);
        Mesh.addResourceClasses(this);
        Ontology.addResourceClasses(this);
        Assay.addResourceClasses(this);
        Mechanism.addResourceClasses(this);

        addQuadMapping(this);

        addProcedures(this);

        Common.addFunctions(this);
    }


    public static void addPrefixes(SparqlDatabaseConfiguration config)
    {
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        config.addPrefix("owl", "http://www.w3.org/2002/07/owl#");
        config.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");

        config.addPrefix("compound", "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/");
        config.addPrefix("substance", "http://rdf.ncbi.nlm.nih.gov/pubchem/substance/");
        config.addPrefix("descriptor", "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/");
        config.addPrefix("synonym", "http://rdf.ncbi.nlm.nih.gov/pubchem/synonym/");
        config.addPrefix("inchikey", "http://rdf.ncbi.nlm.nih.gov/pubchem/inchikey/");
        config.addPrefix("bioassay", "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/");
        config.addPrefix("measuregroup", "http://rdf.ncbi.nlm.nih.gov/pubchem/measuregroup/");
        config.addPrefix("endpoint", "http://rdf.ncbi.nlm.nih.gov/pubchem/endpoint/");
        config.addPrefix("reference", "http://rdf.ncbi.nlm.nih.gov/pubchem/reference/");
        config.addPrefix("protein", "http://rdf.ncbi.nlm.nih.gov/pubchem/protein/");
        config.addPrefix("conserveddomain", "http://rdf.ncbi.nlm.nih.gov/pubchem/conserveddomain/");
        config.addPrefix("gene", "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/");
        config.addPrefix("pathway", "http://rdf.ncbi.nlm.nih.gov/pubchem/pathway/");
        config.addPrefix("source", "http://rdf.ncbi.nlm.nih.gov/pubchem/source/");
        config.addPrefix("concept", "http://rdf.ncbi.nlm.nih.gov/pubchem/concept/");
        config.addPrefix("vocab", "http://rdf.ncbi.nlm.nih.gov/pubchem/vocabulary#");
        config.addPrefix("obo", "http://purl.obolibrary.org/obo/");
        config.addPrefix("sio", "http://semanticscience.org/resource/");
        config.addPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        config.addPrefix("bao", "http://www.bioassayontology.org/bao#");
        config.addPrefix("bp", "http://www.biopax.org/release/biopax-level3.owl#");
        config.addPrefix("ndfrt", "http://evs.nci.nih.gov/ftp1/NDF-RT/NDF-RT.owl#");
        config.addPrefix("ncit", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#");
        config.addPrefix("wikidata", "http://www.wikidata.org/entity/");
        config.addPrefix("ops", "http://www.openphacts.org/units/");
        config.addPrefix("cito", "http://purl.org/spar/cito/");
        config.addPrefix("fabio", "http://purl.org/spar/fabio/");
        config.addPrefix("uniprot", "http://purl.uniprot.org/uniprot/");
        config.addPrefix("up", "http://purl.uniprot.org/core/");
        config.addPrefix("pdbo40", "http://rdf.wwpdb.org/schema/pdbx-v40.owl#");
        config.addPrefix("pdbo", "https://rdf.wwpdb.org/schema/pdbx-v50.owl#");
        config.addPrefix("pdbr", "http://rdf.wwpdb.org/pdb/");
        config.addPrefix("taxonomy", "http://identifiers.org/taxonomy/");
        config.addPrefix("reactome", "http://identifiers.org/reactome/");
        //config.addPrefix("chembl", "http://rdf.ebi.ac.uk/resource/chembl/molecule/");
        config.addPrefix("chemblchembl", "http://linkedchemistry.info/chembl/chemblid/");
        config.addPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        config.addPrefix("void", "http://rdfs.org/ns/void#");
        config.addPrefix("dcterms", "http://purl.org/dc/terms/");
        config.addPrefix("ensembl", "http://rdf.ebi.ac.uk/resource/ensembl/");
        config.addPrefix("cheminf", "http://semanticscience.org/resource/");
        config.addPrefix("mesh", "http://id.nlm.nih.gov/mesh/");

        // extension
        config.addPrefix("pubchem", "http://rdf.ncbi.nlm.nih.gov/pubchem/");
        config.addPrefix("sachem", "http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#");
        config.addPrefix("template", "http://bioinfo.iocb.cz/0.9/template#");
        config.addPrefix("meshv", "http://id.nlm.nih.gov/mesh/vocab#");
        config.addPrefix("fulltext", "http://bioinfo.uochb.cas.cz/rdf/v1.0/fulltext#");
    }


    @SuppressWarnings("serial")
    public static void addResourceClasses(SparqlDatabaseConfiguration config) throws SQLException
    {
        Bioassay.addResourceClasses(config);
        Compound.addResourceClasses(config);
        Concept.addResourceClasses(config);
        ConservedDomain.addResourceClasses(config);
        CompoundDescriptor.addResourceClasses(config);
        SubstanceDescriptor.addResourceClasses(config);
        Endpoint.addResourceClasses(config);
        Gene.addResourceClasses(config);
        InchiKey.addResourceClasses(config);
        Measuregroup.addResourceClasses(config);
        Pathway.addResourceClasses(config);
        Protein.addResourceClasses(config);
        Reference.addResourceClasses(config);
        Source.addResourceClasses(config);
        Substance.addResourceClasses(config);
        Synonym.addResourceClasses(config);

        String sachem = config.getPrefixes().get("sachem");

        config.addIriClass(
                new EnumUserIriClass("pubchem:query_format", "sachem.query_format", new HashMap<String, String>()
                {
                    {
                        put("UNSPECIFIED", sachem + "UnspecifiedFormat");
                        put("SMILES", sachem + "SMILES");
                        put("MOLFILE", sachem + "MolFile");
                        put("RGROUP", sachem + "RGroup");
                    }
                }));

        config.addIriClass(
                new EnumUserIriClass("pubchem:search_mode", "sachem.search_mode", new HashMap<String, String>()
                {
                    {
                        put("SUBSTRUCTURE", sachem + "substructureSearch");
                        put("EXACT", sachem + "exactSearch");
                    }
                }));

        config.addIriClass(
                new EnumUserIriClass("pubchem:charge_mode", "sachem.charge_mode", new HashMap<String, String>()
                {
                    {
                        put("IGNORE", sachem + "ignoreCharges");
                        put("DEFAULT_AS_UNCHARGED", sachem + "defaultChargeAsZero");
                        put("DEFAULT_AS_ANY", sachem + "defaultChargeAsAny");
                    }
                }));

        config.addIriClass(
                new EnumUserIriClass("pubchem:isotope_mode", "sachem.isotope_mode", new HashMap<String, String>()
                {
                    {
                        put("IGNORE", sachem + "ignoreIsotopes");
                        put("DEFAULT_AS_STANDARD", sachem + "defaultIsotopeAsStandard");
                        put("DEFAULT_AS_ANY", sachem + "defaultIsotopeAsAny");
                    }
                }));

        config.addIriClass(
                new EnumUserIriClass("pubchem:radical_mode", "sachem.radical_mode", new HashMap<String, String>()
                {
                    {
                        put("IGNORE", sachem + "ignoreSpinMultiplicity");
                        put("DEFAULT_AS_STANDARD", sachem + "defaultSpinMultiplicityAsZero");
                        put("DEFAULT_AS_ANY", sachem + "defaultSpinMultiplicityAsAny");
                    }
                }));

        config.addIriClass(
                new EnumUserIriClass("pubchem:stereo_mode", "sachem.stereo_mode", new HashMap<String, String>()
                {
                    {
                        put("IGNORE", sachem + "ignoreStereo");
                        put("STRICT", sachem + "strictStereo");
                    }
                }));

        config.addIriClass(new EnumUserIriClass("pubchem:aromaticity_mode", "sachem.aromaticity_mode",
                new HashMap<String, String>()
                {
                    {
                        put("PRESERVE", sachem + "aromaticityFromQuery");
                        put("DETECT", sachem + "aromaticityDetect");
                        put("AUTO", sachem + "aromaticityDetectIfMissing");
                    }
                }));

        config.addIriClass(
                new EnumUserIriClass("pubchem:tautomer_mode", "sachem.tautomer_mode", new HashMap<String, String>()
                {
                    {
                        put("IGNORE", sachem + "ignoreTautomers");
                        put("INCHI", sachem + "inchiTautomers");
                    }
                }));
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        Bioassay.addQuadMapping(config);
        Compound.addQuadMapping(config);
        Concept.addQuadMapping(config);
        ConservedDomain.addQuadMapping(config);
        CompoundDescriptor.addQuadMapping(config);
        SubstanceDescriptor.addQuadMapping(config);
        Endpoint.addQuadMapping(config);
        Gene.addQuadMapping(config);
        InchiKey.addQuadMapping(config);
        Measuregroup.addQuadMapping(config);
        Pathway.addQuadMapping(config);
        Protein.addQuadMapping(config);
        Reference.addQuadMapping(config);
        Source.addQuadMapping(config);
        Substance.addQuadMapping(config);
        Synonym.addQuadMapping(config);
    }


    public static void addProcedures(SparqlDatabaseConfiguration config)
    {
        String fulltext = config.getPrefixes().get("fulltext");
        UserIriClass compound = config.getIriClass("pubchem:compound");


        /* fulltext:bioassaySearch */
        ProcedureDefinition bioassay = new ProcedureDefinition(fulltext + "bioassaySearch",
                new Function("pubchem", "bioassay"));
        bioassay.addParameter(new ParameterDefinition(fulltext + "query", xsdString, null));
        bioassay.addResult(new ResultDefinition(config.getIriClass("pubchem:bioassay")));
        config.addProcedure(bioassay);


        /* fulltext:compoundSearch */
        ProcedureDefinition compoundSearch = new ProcedureDefinition(fulltext + "compoundSearch",
                new Function("pubchem", "compound"));
        compoundSearch.addParameter(new ParameterDefinition(fulltext + "query", xsdString, null));
        compoundSearch.addResult(new ResultDefinition(fulltext + "compound", compound, "compound"));
        compoundSearch.addResult(new ResultDefinition(fulltext + "name", rdfLangStringEn, "name"));
        config.addProcedure(compoundSearch);
    }
}
