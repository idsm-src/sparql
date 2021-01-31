package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.ontology.Ontology;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.mapping.classes.GeneralUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IntegerUserIriClass;



public class ChebiOntologySachemConfiguration extends SachemConfiguration
{
    public ChebiOntologySachemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        super(service, connectionPool, schema, "chebi", new Table("molecules", "chebi"),
                "http://purl.obolibrary.org/obo/CHEBI_",
                new GeneralUserIriClass("ontology:resource", "ontology", "ontology_resource",
                        Arrays.asList("smallint", "integer"), getOntologyPattern(connectionPool),
                        GeneralUserIriClass.SqlCheck.IF_NOT_MATCH),
                new IntegerUserIriClass("chebi:molfile", "integer", "http://purl.obolibrary.org/obo/CHEBI_",
                        "_Molfile"),
                getColumns(new String[] { Ontology.unitCHEBI, "compound" }),
                getColumns(new String[] { Ontology.unitCHEBI, "id" }));
    }


    private static String getOntologyPattern(DataSource connectionPool) throws SQLException
    {
        StringBuilder builder = new StringBuilder();

        try(Connection connection = connectionPool.getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement
                        .executeQuery("select pattern from ontology.resource_categories__reftable"))
                {
                    boolean hasResult = false;

                    while(result.next())
                    {
                        if(hasResult)
                            builder.append("|");

                        hasResult = true;

                        builder.append("(" + result.getString(1) + ")");
                    }
                }
            }
        }

        return builder.toString();
    }
}
