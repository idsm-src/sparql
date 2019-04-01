package cz.iocb.chemweb.server.db.postgresql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.db.schema.Column;
import cz.iocb.chemweb.server.db.schema.DatabaseSchema;
import cz.iocb.chemweb.server.db.schema.Table;
import cz.iocb.chemweb.server.db.schema.TableColumn;



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
                    String tableName = tables.getString("TABLE_NAME");
                    Table table = new Table(tableName);


                    try(ResultSet columns = metaData.getColumns(null, null, tableName, null))
                    {
                        while(columns.next())
                        {
                            TableColumn column = new TableColumn(columns.getString("COLUMN_NAME"));

                            if(columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls)
                                addNullableColumn(table, column);
                        }
                    }


                    try(ResultSet indexes = metaData.getIndexInfo(null, null, tableName, true, false))
                    {
                        List<Column> columns = null;

                        while(indexes.next())
                        {
                            short position = indexes.getShort("ORDINAL_POSITION");

                            if(position == 0)
                                continue;

                            if(position == 1)
                            {
                                if(columns != null)
                                    addPrimaryKeys(table, columns);

                                columns = new ArrayList<Column>();
                            }

                            columns.add(new TableColumn(indexes.getString("COLUMN_NAME")));
                        }

                        if(columns != null)
                            addPrimaryKeys(table, columns);
                    }


                    try(ResultSet indexes = metaData.getCrossReference(null, null, tableName, null, null, null))
                    {
                        Table foreignTable = null;
                        List<Column> parentColumns = new ArrayList<Column>();
                        List<Column> foreignColumns = new ArrayList<Column>();


                        while(indexes.next())
                        {
                            short seq = indexes.getShort("KEY_SEQ");

                            if(seq == 1)
                            {
                                if(foreignTable != null)
                                {
                                    addForeignKeys(table, parentColumns, foreignTable, foreignColumns);

                                    parentColumns = new ArrayList<Column>();
                                    foreignColumns = new ArrayList<Column>();
                                }

                                foreignTable = new Table(indexes.getString("FKTABLE_NAME"));
                            }

                            parentColumns.add(new TableColumn(indexes.getString("PKCOLUMN_NAME")));
                            foreignColumns.add(new TableColumn(indexes.getString("FKCOLUMN_NAME")));
                        }

                        if(foreignTable != null)
                            addForeignKeys(table, parentColumns, foreignTable, foreignColumns);
                    }
                }
            }
        }
    }
}
