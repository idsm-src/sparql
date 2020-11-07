package cz.iocb.chemweb.server.sparql.config.chembl;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.common.Common;
import cz.iocb.chemweb.server.sparql.config.mesh.Mesh;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.config.pubchem.Bioassay;
import cz.iocb.chemweb.server.sparql.config.pubchem.Compound;
import cz.iocb.chemweb.server.sparql.config.pubchem.Reference;
import cz.iocb.chemweb.server.sparql.config.pubchem.Substance;



public class ChemblConfiguration extends SparqlDatabaseConfiguration
{
    static final String schema = "chembl";


    public ChemblConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

        addPrefixes(this);

        addResourceClasses(this);
        Common.addResourceClasses(this);
        Ontology.addResourceClasses(this);
        Mesh.addResourceClasses(this);
        Compound.addResourceClasses(this);
        Substance.addResourceClasses(this);
        Bioassay.addResourceClasses(this);
        Reference.addResourceClasses(this);

        addQuadMapping(this);
    }


    public static void addPrefixes(SparqlDatabaseConfiguration config)
    {
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        config.addPrefix("owl", "http://www.w3.org/2002/07/owl#");
        config.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");

        config.addPrefix("bao", "http://www.bioassayontology.org/bao#");
        config.addPrefix("bibo", "http://purl.org/ontology/bibo/");
        config.addPrefix("cco", "http://rdf.ebi.ac.uk/terms/chembl#");
        config.addPrefix("chembl", "http://rdf.ebi.ac.uk/resource/chembl/");
        config.addPrefix("chembl_activity", "http://rdf.ebi.ac.uk/resource/chembl/activity/");
        config.addPrefix("chembl_assay", "http://rdf.ebi.ac.uk/resource/chembl/assay/");
        config.addPrefix("chembl_binding_site", "http://rdf.ebi.ac.uk/resource/chembl/binding_site/");
        config.addPrefix("chembl_bio_cmpt", "http://rdf.ebi.ac.uk/resource/chembl/biocomponent/");
        config.addPrefix("chembl_cell_line", "http://rdf.ebi.ac.uk/resource/chembl/cell_line/");
        config.addPrefix("chembl_document", "http://rdf.ebi.ac.uk/resource/chembl/document/");
        config.addPrefix("chembl_indication", "http://rdf.ebi.ac.uk/resource/chembl/drug_indication/");
        config.addPrefix("chembl_journal", "http://rdf.ebi.ac.uk/resource/chembl/journal/");
        config.addPrefix("chembl_moa", "http://rdf.ebi.ac.uk/resource/chembl/drug_mechanism/");
        config.addPrefix("chembl_molecule", "http://rdf.ebi.ac.uk/resource/chembl/molecule/");
        config.addPrefix("chembl_protclass", "http://rdf.ebi.ac.uk/resource/chembl/protclass/");
        config.addPrefix("chembl_source", "http://rdf.ebi.ac.uk/resource/chembl/source/");
        config.addPrefix("chembl_target", "http://rdf.ebi.ac.uk/resource/chembl/target/");
        config.addPrefix("chembl_target_cmpt", "http://rdf.ebi.ac.uk/resource/chembl/targetcomponent/");
        config.addPrefix("cheminf", "http://semanticscience.org/resource/");
        config.addPrefix("cito", "http://purl.org/spar/cito/");
        config.addPrefix("dc", "http://purl.org/dc/elements/1.1/");
        config.addPrefix("dcterms", "http://purl.org/dc/terms/");
        config.addPrefix("doi", "http://dx.doi.org/");
        config.addPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        config.addPrefix("freq", "http://purl.org/cld/freq/");
        config.addPrefix("mio", "http://www.ebi.ac.uk/ontology-lookup/?termId=");
        config.addPrefix("oborel", "http://purl.obolibrary.org/obo#");
        config.addPrefix("ops", "http://www.openphacts.org/units/");
        config.addPrefix("pav", "http://purl.org/pav/");
        config.addPrefix("qudt", "http://qudt.org/vocab/unit#");
        config.addPrefix("sd", "http://www.w3.org/ns/sparql-service-description#");
        config.addPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        config.addPrefix("uniprot", "http://purl.uniprot.org/uniprot/");
        config.addPrefix("voag", "http://voag.linkedmodel.org/voag#");
        config.addPrefix("void", "http://rdfs.org/ns/void#");
        config.addPrefix("uo", "http://purl.obolibrary.org/obo/");

        // extension
        config.addPrefix("pdbo", "https://rdf.wwpdb.org/schema/pdbx-v50.owl#");
        config.addPrefix("sio", "http://semanticscience.org/resource/");
        config.addPrefix("template", "http://bioinfo.iocb.cz/0.9/template#");
    }


    public static void addResourceClasses(SparqlDatabaseConfiguration config) throws SQLException
    {
        Activity.addResourceClasses(config);
        Assay.addResourceClasses(config);
        BindingSite.addResourceClasses(config);
        BioComponent.addResourceClasses(config);
        CellLine.addResourceClasses(config);
        Document.addResourceClasses(config);
        DrugIndication.addResourceClasses(config);
        Journal.addResourceClasses(config);
        Mechanism.addResourceClasses(config);
        Molecule.addResourceClasses(config);
        MoleculeReference.addResourceClasses(config);
        ProteinClassification.addResourceClasses(config);
        Source.addResourceClasses(config);
        TargetComponent.addResourceClasses(config);
        TargetComponentReference.addResourceClasses(config);
        Target.addResourceClasses(config);
    }


    public static void addQuadMapping(SparqlDatabaseConfiguration config)
    {
        Activity.addQuadMapping(config);
        Assay.addQuadMapping(config);
        BindingSite.addQuadMapping(config);
        BioComponent.addQuadMapping(config);
        CellLine.addQuadMapping(config);
        Document.addQuadMapping(config);
        DrugIndication.addQuadMapping(config);
        Journal.addQuadMapping(config);
        Mechanism.addQuadMapping(config);
        Molecule.addQuadMapping(config);
        MoleculeReference.addQuadMapping(config);
        ProteinClassification.addQuadMapping(config);
        Source.addQuadMapping(config);
        TargetComponent.addQuadMapping(config);
        TargetComponentReference.addQuadMapping(config);
        Target.addQuadMapping(config);
    }
}
