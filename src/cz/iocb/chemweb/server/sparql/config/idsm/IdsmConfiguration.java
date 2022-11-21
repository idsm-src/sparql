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
        super(service != null ? service : "https://idsm.elixir-czech.cz/sparql/endpoint/idsm", connectionPool, schema);

        addPrefixes();
        addServices();

        setConstraints();

        addServiceDescription();
    }


    private void addPrefixes()
    {
        // rhea
        addPrefix("rh", "http://rdf.rhea-db.org/");
        addPrefix("taxon", "http://purl.uniprot.org/taxonomy/");

        // nextprot
        addPrefix("nextprot", "http://nextprot.org/rdf#");
        addPrefix("cv", "http://nextprot.org/rdf/terminology/");

        // wikidata
        addPrefix("wikibase", "http://wikiba.se/ontology#");
        addPrefix("wd", "http://www.wikidata.org/entity/");
        addPrefix("wdt", "http://www.wikidata.org/prop/direct/");
        addPrefix("wdtn", "http://www.wikidata.org/prop/direct-normalized/");
        addPrefix("wds", "http://www.wikidata.org/entity/statement/");
        addPrefix("p", "http://www.wikidata.org/prop/");
        addPrefix("wdref", "http://www.wikidata.org/reference/");
        addPrefix("wdv", "http://www.wikidata.org/value/");
        addPrefix("ps", "http://www.wikidata.org/prop/statement/");
        addPrefix("psv", "http://www.wikidata.org/prop/statement/value/");
        addPrefix("psn", "http://www.wikidata.org/prop/statement/value-normalized/");
        addPrefix("pq", "http://www.wikidata.org/prop/qualifier/");
        addPrefix("pqv", "http://www.wikidata.org/prop/qualifier/value/");
        addPrefix("pqn", "http://www.wikidata.org/prop/qualifier/value-normalized/");
        addPrefix("pr", "http://www.wikidata.org/prop/reference/");
        addPrefix("prv", "http://www.wikidata.org/prop/reference/value/");
        addPrefix("prn", "http://www.wikidata.org/prop/reference/value-normalized/");
        addPrefix("wdno", "http://www.wikidata.org/prop/novalue/");
        addPrefix("wdata", "http://www.wikidata.org/wiki/Special:EntityData/");
    }


    private void addServices() throws SQLException
    {
        addService(new ChebiConfiguration(null, connectionPool, getDatabaseSchema()), true);
        addService(new ChemblConfiguration(null, connectionPool, getDatabaseSchema()), true);
        addService(new MeshConfiguration(null, connectionPool, getDatabaseSchema()), true);
        addService(new OntologyConfiguration(null, connectionPool, getDatabaseSchema()), true);
        addService(new PubChemConfiguration(null, connectionPool, getDatabaseSchema()), true);
        addService(new PubChemSachemConfiguration(null, connectionPool, getDatabaseSchema()), true);


        addService(new WikidataSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/wikidata",
                connectionPool, getDatabaseSchema()), false);
        addService(new WikidataSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/wikidata",
                connectionPool, getDatabaseSchema()), false);

        addService(new DrugbankSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/drugbank",
                connectionPool, getDatabaseSchema()), false);
        addService(new DrugbankSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/drugbank",
                connectionPool, getDatabaseSchema()), false);

        addService(new ChebiOntologySachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/chebi",
                connectionPool, getDatabaseSchema()), false);
        addService(new ChebiOntologySachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/chebi",
                connectionPool, getDatabaseSchema()), false);

        addService(new ChemblSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/chembl", connectionPool,
                getDatabaseSchema()), false);
        addService(new ChemblSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/chembl", connectionPool,
                getDatabaseSchema()), false);

        addService(new PubChemSachemConfiguration("https://idsm.elixir-czech.cz/sparql/endpoint/pubchem",
                connectionPool, getDatabaseSchema()), false);
        addService(new PubChemSachemConfiguration("https://idsm.elixir-czech.cz/sachem/endpoint/pubchem",
                connectionPool, getDatabaseSchema()), false);
    }
}
