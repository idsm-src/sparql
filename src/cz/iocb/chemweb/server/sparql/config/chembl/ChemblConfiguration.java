package cz.iocb.chemweb.server.sparql.config.chembl;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import java.sql.SQLException;
import java.util.Arrays;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Function;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;



public class ChemblConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "chembl";


    public ChemblConfiguration(DataSource connectionPool) throws SQLException
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

        prefixes.put("bao", "http://www.bioassayontology.org/bao#");
        prefixes.put("bibo", "http://purl.org/ontology/bibo/");
        prefixes.put("cco", "http://rdf.ebi.ac.uk/terms/chembl#");
        prefixes.put("chembl", "http://rdf.ebi.ac.uk/resource/chembl/");
        prefixes.put("chembl_activity", "http://rdf.ebi.ac.uk/resource/chembl/activity/");
        prefixes.put("chembl_assay", "http://rdf.ebi.ac.uk/resource/chembl/assay/");
        prefixes.put("chembl_binding_site", "http://rdf.ebi.ac.uk/resource/chembl/binding_site/");
        prefixes.put("chembl_bio_cmpt", "http://rdf.ebi.ac.uk/resource/chembl/biocomponent/");
        prefixes.put("chembl_cell_line", "http://rdf.ebi.ac.uk/resource/chembl/cell_line/");
        prefixes.put("chembl_document", "http://rdf.ebi.ac.uk/resource/chembl/document/");
        prefixes.put("chembl_indication", "http://rdf.ebi.ac.uk/resource/chembl/drug_indication/");
        prefixes.put("chembl_journal", "http://rdf.ebi.ac.uk/resource/chembl/journal/");
        prefixes.put("chembl_moa", "http://rdf.ebi.ac.uk/resource/chembl/drug_mechanism/");
        prefixes.put("chembl_molecule", "http://rdf.ebi.ac.uk/resource/chembl/molecule/");
        prefixes.put("chembl_protclass", "http://rdf.ebi.ac.uk/resource/chembl/protclass/");
        prefixes.put("chembl_source", "http://rdf.ebi.ac.uk/resource/chembl/source/");
        prefixes.put("chembl_target", "http://rdf.ebi.ac.uk/resource/chembl/target/");
        prefixes.put("chembl_target_cmpt", "http://rdf.ebi.ac.uk/resource/chembl/targetcomponent/");
        prefixes.put("cheminf", "http://semanticscience.org/resource/");
        prefixes.put("cito", "http://purl.org/spar/cito/");
        prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
        prefixes.put("dcterms", "http://purl.org/dc/terms/");
        prefixes.put("doi", "http://dx.doi.org/");
        prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
        prefixes.put("freq", "http://purl.org/cld/freq/");
        prefixes.put("mio", "http://www.ebi.ac.uk/ontology-lookup/?termId=");
        prefixes.put("oborel", "http://purl.obolibrary.org/obo#");
        prefixes.put("ops", "http://www.openphacts.org/units/");
        prefixes.put("pav", "http://purl.org/pav/");
        prefixes.put("qudt", "http://qudt.org/vocab/unit#");
        prefixes.put("sd", "http://www.w3.org/ns/sparql-service-description#");
        prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
        prefixes.put("uniprot", "http://purl.uniprot.org/uniprot/");
        prefixes.put("voag", "http://voag.linkedmodel.org/voag#");
        prefixes.put("void", "http://rdfs.org/ns/void#");
        prefixes.put("uo", "http://purl.obolibrary.org/obo/");

        prefixes.put("sachem", "http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#");
    }


    private void loadClasses() throws SQLException
    {
        Common.addIriClasses(this);
        Activity.addIriClasses(this);
        Assay.addIriClasses(this);
        BindingSite.addIriClasses(this);
        BioComponent.addIriClasses(this);
        CellLine.addIriClasses(this);
        Document.addIriClasses(this);
        DrugIndication.addIriClasses(this);
        Journal.addIriClasses(this);
        Mechanism.addIriClasses(this);
        Molecule.addIriClasses(this);
        MoleculeReference.addIriClasses(this);
        ProteinClassification.addIriClasses(this);
        Source.addIriClasses(this);
        TargetComponent.addIriClasses(this);
        TargetComponentReference.addIriClasses(this);
        Target.addIriClasses(this);
        Ontology.addIriClasses(this);


        String sachem = "http://bioinfo\\.uochb\\.cas\\.cz/rdf/v1\\.0/sachem#";

        String queryFormatPattern = sachem + "(UnspecifiedFormat|SMILES|MolFile|RGroup)";
        addIriClass(new GeneralUserIriClass(schema, "query_format", Arrays.asList("integer"), queryFormatPattern));

        String searchModePattern = sachem + "(substructureSearch|exactSearch)";
        addIriClass(new GeneralUserIriClass(schema, "search_mode", Arrays.asList("integer"), searchModePattern));

        String chargeModePattern = sachem + "(ignoreCharges|defaultChargeAsZero|defaultChargeAsAny)";
        addIriClass(new GeneralUserIriClass(schema, "charge_mode", Arrays.asList("integer"), chargeModePattern));

        String isotopeModePattern = sachem + "(ignoreIsotopes|defaultIsotopeAsStandard|defaultIsotopeAsAny)";
        addIriClass(new GeneralUserIriClass(schema, "isotope_mode", Arrays.asList("integer"), isotopeModePattern));

        String stereoModePattern = sachem + "(ignoreStrereo|strictStereo)";
        addIriClass(new GeneralUserIriClass(schema, "stereo_mode", Arrays.asList("integer"), stereoModePattern));

        String tautomerModePattern = sachem + "(ignoreTautomers|inchiTautomers)";
        addIriClass(new GeneralUserIriClass(schema, "tautomer_mode", Arrays.asList("integer"), tautomerModePattern));
    }


    private void loadQuadMapping()
    {
        Activity.addQuadMapping(this);
        Assay.addQuadMapping(this);
        BindingSite.addQuadMapping(this);
        BioComponent.addQuadMapping(this);
        CellLine.addQuadMapping(this);
        Document.addQuadMapping(this);
        DrugIndication.addQuadMapping(this);
        Journal.addQuadMapping(this);
        Mechanism.addQuadMapping(this);
        Molecule.addQuadMapping(this);
        MoleculeReference.addQuadMapping(this);
        ProteinClassification.addQuadMapping(this);
        Source.addQuadMapping(this);
        TargetComponent.addQuadMapping(this);
        TargetComponentReference.addQuadMapping(this);
        Target.addQuadMapping(this);
        Ontology.addQuadMapping(this);
    }


    private void loadProcedures()
    {
        String sachem = prefixes.get("sachem");
        UserIriClass compound = getIriClass("molecule");


        /* orchem:substructureSearch */
        ProcedureDefinition subsearch = new ProcedureDefinition(sachem + "substructureSearch",
                new Function(schema, "sachem_substructure_search"));
        subsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        subsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        subsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        subsearch.addParameter(new ParameterDefinition(sachem + "searchMode", getIriClass("search_mode"),
                new IRI(sachem + "substructureSearch")));
        subsearch.addParameter(new ParameterDefinition(sachem + "chargeMode", getIriClass("charge_mode"),
                new IRI(sachem + "defaultChargeAsAny")));
        subsearch.addParameter(new ParameterDefinition(sachem + "isotopeMode", getIriClass("isotope_mode"),
                new IRI(sachem + "ignoreIsotopes")));
        subsearch.addParameter(new ParameterDefinition(sachem + "stereoMode", getIriClass("stereo_mode"),
                new IRI(sachem + "ignoreStrereo")));
        subsearch.addParameter(new ParameterDefinition(sachem + "tautomerMode", getIriClass("tautomer_mode"),
                new IRI(sachem + "ignoreTautomers")));
        subsearch.addResult(new ResultDefinition(compound));
        procedures.put(subsearch.getProcedureName(), subsearch);


        /* orchem:similaritySearch */
        ProcedureDefinition simsearch = new ProcedureDefinition(sachem + "similaritySearch",
                new Function(schema, "sachem_similarity_search"));
        simsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simsearch.addParameter(new ParameterDefinition(sachem + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simsearch.addResult(new ResultDefinition(sachem + "compound", compound, "compound"));
        simsearch.addResult(new ResultDefinition(sachem + "score", xsdDouble, "score"));
        procedures.put(simsearch.getProcedureName(), simsearch);


        /* orchem:similarCompoundSearch */
        ProcedureDefinition simcmpsearch = new ProcedureDefinition(sachem + "similarCompoundSearch",
                new Function(schema, "sachem_similarity_search"));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "query", xsdString, null));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "queryFormat", getIriClass("query_format"),
                new IRI(sachem + "UnspecifiedFormat")));
        simcmpsearch
                .addParameter(new ParameterDefinition(sachem + "cutoff", xsdDouble, new Literal("0.8", xsdDoubleIri)));
        simcmpsearch.addParameter(new ParameterDefinition(sachem + "topn", xsdInt, new Literal("-1", xsdIntIri)));
        simcmpsearch.addResult(new ResultDefinition(null, compound, "compound"));
        procedures.put(simcmpsearch.getProcedureName(), simcmpsearch);
    }
}
