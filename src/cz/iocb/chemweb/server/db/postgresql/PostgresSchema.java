package cz.iocb.chemweb.server.db.postgresql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.db.DatabaseSchema;



public class PostgresSchema extends DatabaseSchema
{
    public PostgresSchema(DataSource connectionPool) throws SQLException
    {
        try(Connection connection = connectionPool.getConnection())
        {
            DatabaseMetaData metaData = connection.getMetaData();

            try(ResultSet tables = metaData.getTables(null, null, null, new String[] { "TABLE", "VIEW" }))
            {
                while(tables.next())
                {
                    String table = tables.getString("TABLE_NAME");


                    try(ResultSet indexes = metaData.getIndexInfo(null, null, table, true, false))
                    {
                        List<String> columns = null;

                        while(indexes.next())
                        {
                            short position = indexes.getShort("ORDINAL_POSITION");

                            if(position == 0)
                                continue;

                            if(position == 1)
                            {
                                if(columns != null)
                                    addPrimaryKeys(table, columns);

                                columns = new ArrayList<String>();
                            }

                            columns.add(indexes.getString("COLUMN_NAME"));
                        }

                        if(columns != null)
                            addPrimaryKeys(table, columns);
                    }


                    try(ResultSet indexes = metaData.getCrossReference(null, null, table, null, null, null))
                    {
                        String foreignTable = null;
                        List<String> parentColumns = new ArrayList<String>();
                        List<String> foreignColumns = new ArrayList<String>();


                        while(indexes.next())
                        {
                            short seq = indexes.getShort("KEY_SEQ");

                            if(seq == 1)
                            {
                                if(foreignTable != null)
                                {
                                    addForeignKeys(table, parentColumns, foreignTable, foreignColumns);

                                    parentColumns = new ArrayList<String>();
                                    foreignColumns = new ArrayList<String>();
                                }

                                foreignTable = indexes.getString("FKTABLE_NAME");
                            }

                            parentColumns.add(indexes.getString("PKCOLUMN_NAME"));
                            foreignColumns.add(indexes.getString("FKCOLUMN_NAME"));
                        }

                        if(foreignTable != null)
                            addForeignKeys(table, parentColumns, foreignTable, foreignColumns);
                    }
                }
            }
        }
    }
}
