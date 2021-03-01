package cz.iocb.chemweb.server.sparql.config.idsm;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.chebi.ChebiConfiguration;
import cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration;
import cz.iocb.chemweb.server.sparql.config.mesh.MeshConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.OntologyConfiguration;
import cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration;
import cz.iocb.chemweb.server.sparql.config.sachem.ChebiOntologySachemConfiguration;
import cz.iocb.chemweb.server.sparql.config.sachem.ChemblSachemConfiguration;
import cz.iocb.chemweb.server.sparql.config.sachem.DrugbankSachemConfiguration;
import cz.iocb.chemweb.server.sparql.config.sachem.PubChemSachemConfiguration;
import cz.iocb.chemweb.server.sparql.config.sachem.WikidataSachemConfiguration;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;



public class IdsmConfiguration extends SparqlDatabaseConfiguration
{
    public IdsmConfiguration(String service, DataSource connectionPool, DatabaseSchema schema) throws SQLException
    {
        super(service, connectionPool, schema);
        addExtraPrefixes(this);

        addService(new ChebiConfiguration(null, connectionPool, schema), true);
        addService(new ChemblConfiguration(null, connectionPool, schema), true);
        addService(new MeshConfiguration(null, connectionPool, schema), true);
        addService(new OntologyConfiguration(null, connectionPool, schema), true);
        addService(new PubChemConfiguration(null, connectionPool, schema), true);
        addService(new PubChemSachemConfiguration(null, connectionPool, schema), true);


        addService(new WikidataSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/wikidata",
                connectionPool, schema), false);
        addService(new WikidataSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/wikidata",
                connectionPool, schema), false);

        addService(new DrugbankSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/drugbank",
                connectionPool, schema), false);
        addService(new DrugbankSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/drugbank",
                connectionPool, schema), false);

        addService(new ChebiOntologySachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/chebi",
                connectionPool, schema), false);
        addService(new ChebiOntologySachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/chebi",
                connectionPool, schema), false);

        addService(new ChemblSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/chembl", connectionPool,
                schema), false);
        addService(new ChemblSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/chembl", connectionPool,
                schema), false);

        addService(new PubChemSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/pubchem",
                connectionPool, schema), false);
        addService(new PubChemSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/pubchem",
                connectionPool, schema), false);

        setConstraints();
    }


    public static void addExtraPrefixes(SparqlDatabaseConfiguration config)
    {
        // rhea
        config.addPrefix("rh", "http://rdf.rhea-db.org/");
        config.addPrefix("taxon", "http://purl.uniprot.org/taxonomy/");

        // nextprot
        config.addPrefix("nextprot", "http://nextprot.org/rdf#");
        config.addPrefix("cv", "http://nextprot.org/rdf/terminology/");
    }
}
