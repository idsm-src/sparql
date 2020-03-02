package cz.iocb.chemweb.server.sparql.config.chembl;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;



public class ChemblConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "chembl";


    public ChemblConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

        loadPrefixes();
        loadClasses();
        loadQuadMapping();
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
}
