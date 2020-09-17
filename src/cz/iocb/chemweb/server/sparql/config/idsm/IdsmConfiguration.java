package cz.iocb.chemweb.server.sparql.config.idsm;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.config.chebi.ChebiConfiguration;
import cz.iocb.chemweb.server.sparql.config.chembl.ChemblConfiguration;
import cz.iocb.chemweb.server.sparql.config.common.Common;
import cz.iocb.chemweb.server.sparql.config.mesh.MeshConfiguration;
import cz.iocb.chemweb.server.sparql.config.nextprot.NeXtProtConfiguration;
import cz.iocb.chemweb.server.sparql.config.ontology.OntologyConfiguration;
import cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration;



public class IdsmConfiguration extends SparqlDatabaseConfiguration
{
    public IdsmConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);

        Common.addPrefixes(this);
        ChebiConfiguration.addPrefixes(this);
        ChemblConfiguration.addPrefixes(this);
        MeshConfiguration.addPrefixes(this);
        NeXtProtConfiguration.addPrefixes(this);
        OntologyConfiguration.addPrefixes(this);
        PubChemConfiguration.addPrefixes(this);

        Common.addResourceClasses(this);
        ChebiConfiguration.addResourceClasses(this);
        ChemblConfiguration.addResourceClasses(this);
        MeshConfiguration.addResourceClasses(this);
        NeXtProtConfiguration.addResourceClasses(this);
        OntologyConfiguration.addResourceClasses(this);
        PubChemConfiguration.addResourceClasses(this);

        ChebiConfiguration.addQuadMapping(this);
        ChemblConfiguration.addQuadMapping(this);
        MeshConfiguration.addQuadMapping(this);
        NeXtProtConfiguration.addQuadMapping(this);
        OntologyConfiguration.addQuadMapping(this);
        PubChemConfiguration.addQuadMapping(this);

        PubChemConfiguration.addProcedures(this);

        Common.addFunctions(this);

        setConstraints();
    }
}
