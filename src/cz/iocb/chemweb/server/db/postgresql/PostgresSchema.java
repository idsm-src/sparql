package cz.iocb.chemweb.server.db.postgresql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema;



public class PostgresSchema extends DatabaseSchema
{
    public PostgresSchema(ConnectionPool connectionPool) throws SQLException
    {
        try(Connection connection = connectionPool.getConnection())
        {
            DatabaseMetaData metaData = connection.getMetaData();

            try(ResultSet tables = metaData.getTables(null, null, null, new String[] { "TABLE", "VIEW" }))
            {
                while(tables.next())
                {
                    String table = tables.getString("TABLE_NAME");
                    List<List<String>> pkeys = new ArrayList<List<String>>();

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
                                columns = new ArrayList<String>();
                                pkeys.add(columns);
                            }

                            columns.add(indexes.getString("COLUMN_NAME"));
                        }
                    }

                    primaryKeys.put(table, pkeys);


                    try(ResultSet indexes = metaData.getCrossReference(null, null, table, null, null, null))
                    {
                        List<List<KeyPair>> fkeys = null;
                        List<KeyPair> columns = null;
                        String lastFkTable = null;

                        while(indexes.next())
                        {
                            short seq = indexes.getShort("KEY_SEQ");

                            if(seq == 1)
                            {
                                String fktable = indexes.getString("FKTABLE_NAME");

                                if(!fktable.equals(lastFkTable))
                                {
                                    fkeys = new ArrayList<List<KeyPair>>();
                                    foreignKeys.put(new KeyPair(table, fktable), fkeys);
                                    lastFkTable = fktable;
                                }

                                columns = new ArrayList<KeyPair>();
                                fkeys.add(columns);
                            }


                            columns.add(new KeyPair(indexes.getString("PKCOLUMN_NAME"),
                                    indexes.getString("FKCOLUMN_NAME")));
                        }
                    }
                }
            }
        }
    }
}
