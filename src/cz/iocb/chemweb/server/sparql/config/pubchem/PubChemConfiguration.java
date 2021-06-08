package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.chebi.Chebi;
import cz.iocb.chemweb.server.sparql.config.chembl.Assay;
import cz.iocb.chemweb.server.sparql.config.chembl.Mechanism;
import cz.iocb.chemweb.server.sparql.config.chembl.Molecule;
import cz.iocb.chemweb.server.sparql.config.common.Common;
import cz.iocb.chemweb.server.sparql.config.mesh.Mesh;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.config.sachem.Sachem;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
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

        addPrefixes();
        addResourceClasses();
        addQuadMappings();
        addFunctions();
        addProcedures();
    }


    private void addPrefixes()
    {
        Common.addPrefixes(this);

        addPrefix("compound", "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/");
        addPrefix("substance", "http://rdf.ncbi.nlm.nih.gov/pubchem/substance/");
        addPrefix("descriptor", "http://rdf.ncbi.nlm.nih.gov/pubchem/descriptor/");
        addPrefix("synonym", "http://rdf.ncbi.nlm.nih.gov/pubchem/synonym/");
        addPrefix("inchikey", "http://rdf.ncbi.nlm.nih.gov/pubchem/inchikey/");
        addPrefix("bioassay", "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/");
        addPrefix("measuregroup", "http://rdf.ncbi.nlm.nih.gov/pubchem/measuregroup/");
        addPrefix("endpoint", "http://rdf.ncbi.nlm.nih.gov/pubchem/endpoint/");
        addPrefix("reference", "http://rdf.ncbi.nlm.nih.gov/pubchem/reference/");
        addPrefix("protein", "http://rdf.ncbi.nlm.nih.gov/pubchem/protein/");
        addPrefix("conserveddomain", "http://rdf.ncbi.nlm.nih.gov/pubchem/conserveddomain/");
        addPrefix("gene", "http://rdf.ncbi.nlm.nih.gov/pubchem/gene/");
        addPrefix("pathway", "http://rdf.ncbi.nlm.nih.gov/pubchem/pathway/");
        addPrefix("source", "http://rdf.ncbi.nlm.nih.gov/pubchem/source/");
        addPrefix("concept", "http://rdf.ncbi.nlm.nih.gov/pubchem/concept/");
        addPrefix("vocab", "http://rdf.ncbi.nlm.nih.gov/pubchem/vocabulary#");
        addPrefix("obo", "http://purl.obolibrary.org/obo/");
        addPrefix("sio", "http://semanticscience.org/resource/");
        addPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        addPrefix("bao", "http://www.bioassayontology.org/bao#");
        addPrefix("bp", "http://www.biopax.org/release/biopax-level3.owl#");
        addPrefix("ndfrt", "http://evs.nci.nih.gov/ftp1/NDF-RT/NDF-RT.owl#");
        addPrefix("ncit", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#");
        addPrefix("wikidata", "http://www.wikidata.org/entity/");
        addPrefix("ops", "http://www.openphacts.org/units/");
        addPrefix("cito", "http://purl.org/spar/cito/");
        addPrefix("fabio", "http://purl.org/spar/fabio/");
        addPrefix("uniprot", "http://purl.uniprot.org/uniprot/");
        addPrefix("up", "http://purl.uniprot.org/core/");
        addPrefix("pdbo40", "http://rdf.wwpdb.org/schema/pdbx-v40.owl#");
        addPrefix("pdbo", "https://rdf.wwpdb.org/schema/pdbx-v50.owl#");
        addPrefix("pdbr", "http://rdf.wwpdb.org/pdb/");
        addPrefix("taxonomy", "http://identifiers.org/taxonomy/");
        addPrefix("reactome", "http://identifiers.org/reactome/");
        //addPrefix("chembl", "http://rdf.ebi.ac.uk/resource/chembl/molecule/");
        addPrefix("chemblchembl", "http://linkedchemistry.info/chembl/chemblid/");
        addPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        addPrefix("void", "http://rdfs.org/ns/void#");
        addPrefix("dcterms", "http://purl.org/dc/terms/");
        addPrefix("ensembl", "http://rdf.ebi.ac.uk/resource/ensembl/");
        addPrefix("cheminf", "http://semanticscience.org/resource/");
        addPrefix("mesh", "http://id.nlm.nih.gov/mesh/");

        // extension
        addPrefix("pubchem", "http://rdf.ncbi.nlm.nih.gov/pubchem/");
        addPrefix("sachem", "http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#");
        addPrefix("template", "http://bioinfo.iocb.cz/0.9/template#");
        addPrefix("meshv", "http://id.nlm.nih.gov/mesh/vocab#");
        addPrefix("fulltext", "http://bioinfo.uochb.cas.cz/rdf/v1.0/fulltext#");
    }


    private void addResourceClasses() throws SQLException
    {
        Common.addResourceClasses(this);
        Chebi.addResourceClasses(this);
        Molecule.addResourceClasses(this);
        Mesh.addResourceClasses(this);
        Ontology.addResourceClasses(this);
        Assay.addResourceClasses(this);
        Mechanism.addResourceClasses(this);

        Bioassay.addResourceClasses(this);
        Compound.addResourceClasses(this);
        Concept.addResourceClasses(this);
        ConservedDomain.addResourceClasses(this);
        CompoundDescriptor.addResourceClasses(this);
        SubstanceDescriptor.addResourceClasses(this);
        Endpoint.addResourceClasses(this);
        Gene.addResourceClasses(this);
        InchiKey.addResourceClasses(this);
        Measuregroup.addResourceClasses(this);
        Pathway.addResourceClasses(this);
        Protein.addResourceClasses(this);
        Reference.addResourceClasses(this);
        Source.addResourceClasses(this);
        Substance.addResourceClasses(this);
        Synonym.addResourceClasses(this);

        Sachem.addResourceClasses(this);
    }


    private void addQuadMappings()
    {
        Bioassay.addQuadMappings(this);
        Compound.addQuadMappings(this);
        Concept.addQuadMappings(this);
        ConservedDomain.addQuadMappings(this);
        CompoundDescriptor.addQuadMappings(this);
        SubstanceDescriptor.addQuadMappings(this);
        Endpoint.addQuadMappings(this);
        Gene.addQuadMappings(this);
        InchiKey.addQuadMappings(this);
        Measuregroup.addQuadMappings(this);
        Pathway.addQuadMappings(this);
        Protein.addQuadMappings(this);
        Reference.addQuadMappings(this);
        Source.addQuadMappings(this);
        Substance.addQuadMappings(this);
        Synonym.addQuadMappings(this);
    }


    private void addFunctions()
    {
        Common.addFunctions(this);
    }


    private void addProcedures()
    {
        String fulltext = getPrefixes().get("fulltext");
        UserIriClass compound = getIriClass("pubchem:compound");


        /* fulltext:bioassaySearch */
        ProcedureDefinition bioassay = new ProcedureDefinition(fulltext + "bioassaySearch",
                new Function("pubchem", "bioassay"));
        bioassay.addParameter(new ParameterDefinition(fulltext + "query", xsdString, null));
        bioassay.addResult(new ResultDefinition(getIriClass("pubchem:bioassay")));
        addProcedure(bioassay);


        /* fulltext:compoundSearch */
        ProcedureDefinition compoundSearch = new ProcedureDefinition(fulltext + "compoundSearch",
                new Function("pubchem", "compound"));
        compoundSearch.addParameter(new ParameterDefinition(fulltext + "query", xsdString, null));
        compoundSearch.addResult(new ResultDefinition(fulltext + "compound", compound, "compound"));
        compoundSearch.addResult(new ResultDefinition(fulltext + "name", rdfLangStringEn, "name"));
        addProcedure(compoundSearch);
    }
}
