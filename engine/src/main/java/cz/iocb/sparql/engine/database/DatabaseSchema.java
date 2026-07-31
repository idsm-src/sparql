package cz.iocb.sparql.engine.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.sql.DataSource;



public class DatabaseSchema
{
    public static class Pair<T>
    {
        protected final T left;
        protected final T right;

        public Pair(T left, T right)
        {
            this.left = left;
            this.right = right;
        }

        public final T getLeft()
        {
            return left;
        }

        public final T getRight()
        {
            return right;
        }

        @Override
        public int hashCode()
        {
            return left.hashCode() ^ right.hashCode();
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            Pair<?> other = (Pair<?>) object;

            return left.equals(other.left) && right.equals(other.right);
        }
    }


    public static class TablePair extends Pair<Table>
    {
        public TablePair(Table parent, Table foreign)
        {
            super(parent, foreign);
        }
    }


    public static class ColumnPair extends Pair<Column>
    {
        public ColumnPair(Column parent, Column foreign)
        {
            super(parent, foreign);
        }
    }


    protected final Map<Table, List<Column>> nullableColumns = new HashMap<>();
    protected final Map<Table, List<List<Column>>> primaryKeys = new HashMap<>();
    protected final Map<TablePair, List<Set<ColumnPair>>> foreignKeys = new HashMap<>();
    protected final Map<TablePair, List<List<ColumnPair>>> unjoinableColumns = new HashMap<>();


    public DatabaseSchema(DataSource connectionPool) throws SQLException
    {
        try(Connection connection = connectionPool.getConnection())
        {
            connection.setAutoCommit(true);

            DatabaseMetaData metaData = connection.getMetaData();

            try(ResultSet tables = metaData.getTables(null, null, null, new String[] { "TABLE", "VIEW" }))
            {
                while(tables.next())
                {
                    String tableSchema = tables.getString("TABLE_SCHEM");
                    String tableName = tables.getString("TABLE_NAME");
                    Table table = new Table(tableSchema, tableName);


                    try(ResultSet columns = metaData.getColumns(null, tableSchema, tableName, null))
                    {
                        while(columns.next())
                        {
                            TableColumn column = new TableColumn(columns.getString("COLUMN_NAME"));

                            if(columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls)
                                addNullableColumn(table, column);
                        }
                    }


                    try(ResultSet indexes = metaData.getIndexInfo(null, tableSchema, tableName, true, false))
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

                                columns = new ArrayList<>();
                            }

                            columns.add(new TableColumn(indexes.getString("COLUMN_NAME")));
                        }

                        if(columns != null)
                            addPrimaryKeys(table, columns);
                    }


                    try(ResultSet indexes = metaData.getCrossReference(null, tableSchema, tableName, null, null, null))
                    {
                        Table foreignTable = null;
                        List<Column> parentColumns = new ArrayList<>();
                        List<Column> foreignColumns = new ArrayList<>();


                        while(indexes.next())
                        {
                            short seq = indexes.getShort("KEY_SEQ");

                            if(seq == 1)
                            {
                                if(foreignTable != null)
                                {
                                    addForeignKeys(table, parentColumns, foreignTable, foreignColumns);

                                    parentColumns = new ArrayList<>();
                                    foreignColumns = new ArrayList<>();
                                }

                                foreignTable = new Table(indexes.getString("FKTABLE_SCHEM"),
                                        indexes.getString("FKTABLE_NAME"));
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


    public DatabaseSchema(DatabaseSchema other)
    {
        for(Entry<Table, List<Column>> e : other.nullableColumns.entrySet())
            nullableColumns.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<Table, List<List<Column>>> e : other.primaryKeys.entrySet())
            primaryKeys.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<TablePair, List<Set<ColumnPair>>> e : other.foreignKeys.entrySet())
            foreignKeys.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<TablePair, List<List<ColumnPair>>> e : other.unjoinableColumns.entrySet())
            unjoinableColumns.put(e.getKey(), new ArrayList<>(e.getValue()));
    }


    public void addNullableColumn(Table table, TableColumn column)
    {
        List<Column> columnList = nullableColumns.get(table);

        if(columnList == null)
        {
            columnList = new ArrayList<>();
            nullableColumns.put(table, columnList);
        }

        columnList.add(column);
    }


    public void addPrimaryKeys(Table table, List<Column> columns)
    {
        List<List<Column>> primaryKeyList = primaryKeys.get(table);

        if(primaryKeyList == null)
        {
            primaryKeyList = new ArrayList<>();
            primaryKeys.put(table, primaryKeyList);
        }

        primaryKeyList.add(new ArrayList<>(columns));
    }


    public void addForeignKeys(Table parentTable, List<Column> parentColumns, Table foreignTable,
            List<Column> foreignColumns)
    {
        TablePair tablePair = new TablePair(parentTable, foreignTable);

        List<Set<ColumnPair>> foreignKeyList = foreignKeys.get(tablePair);

        if(foreignKeyList == null)
        {
            foreignKeyList = new ArrayList<>();
            foreignKeys.put(tablePair, foreignKeyList);
        }

        Set<ColumnPair> keyPairs = new HashSet<>();

        for(int i = 0; i < parentColumns.size(); i++)
            keyPairs.add(new ColumnPair(parentColumns.get(i), foreignColumns.get(i)));

        foreignKeyList.add(keyPairs);
    }


    public void addUnjoinableColumns(Table leftTable, List<Column> leftColumns, Table rightTable,
            List<Column> rightColumns)
    {
        TablePair tablePair = new TablePair(leftTable, rightTable);

        List<List<ColumnPair>> unjoinableList = unjoinableColumns.get(tablePair);

        if(unjoinableList == null)
        {
            unjoinableList = new ArrayList<>();
            unjoinableColumns.put(tablePair, unjoinableList);
        }

        List<ColumnPair> columnPairs = new ArrayList<>();

        for(int i = 0; i < leftColumns.size(); i++)
            columnPairs.add(new ColumnPair(leftColumns.get(i), rightColumns.get(i)));

        unjoinableList.add(columnPairs);
    }


    public boolean isNullableColumn(Table table, Column column)
    {
        return switch(column)
        {
            case ConstantColumn _ -> false;
            case ExpressionColumn col -> col.canBeNull();
            default -> nullableColumns.getOrDefault(table, List.of()).contains(column);
        };
    }


    public List<Column> getCompatibleKey(Table table, Set<Column> columns)
    {
        List<List<Column>> keys = primaryKeys.get(table);

        if(keys == null)
            return null;

        for(List<Column> key : keys)
            if(columns.containsAll(key))
                return key;

        return null;
    }


    public Set<ColumnPair> isPartOfForeignKey(Table parentTable, Table childTable, Set<ColumnPair> columns,
            Set<Column> extra)
    {
        List<Set<ColumnPair>> keys = getForeignKeys(parentTable, childTable);

        for(Set<ColumnPair> key : keys)
        {
            if(columns.stream().allMatch(k -> key.contains(k)))
            {
                Set<Column> covered = new HashSet<>();

                for(ColumnPair pair : key)
                    covered.add(pair.getRight());

                if(extra.stream().allMatch(c -> covered.contains(c)))
                    return key;
            }
        }

        return null;
    }


    public Set<ColumnPair> getCompatibleForeignKey(Table parentTable, Table childTable, Set<ColumnPair> columns,
            Set<Column> parentColumns)
    {
        List<Set<ColumnPair>> keys = getForeignKeys(parentTable, childTable);

        for(Set<ColumnPair> key : keys)
        {
            if(key.stream().allMatch(k -> columns.contains(k)))
            {
                Set<Column> covered = new HashSet<>();

                for(ColumnPair keyPair : key)
                    for(ColumnPair pair : columns)
                        if(keyPair.getRight().equals(pair.getRight()))
                            covered.add(pair.getLeft());

                if(parentColumns.stream().allMatch(c -> covered.contains(c)))
                    return key;
            }
        }

        return null;
    }


    public List<ColumnPair> getUnjoinableColumns(Table leftTable, Table rightTable, List<ColumnPair> columns)
    {
        List<List<ColumnPair>> list = getUnjoinableColumns(leftTable, rightTable);

        if(list == null)
            return null;

        loop:
        for(List<ColumnPair> key : list)
        {
            for(ColumnPair keyPair : key)
                if(!columns.contains(keyPair))
                    continue loop;

            return key;
        }

        return null;
    }


    public boolean isKey(Table table, Set<Column> columns)
    {
        return getCompatibleKey(table, columns) != null;
    }


    public List<Set<ColumnPair>> getForeignKeys(Table parentTable, Table foreignTable)
    {
        if(parentTable == null || foreignTable == null)
            return List.of();

        return foreignKeys.getOrDefault(new TablePair(parentTable, foreignTable), List.of());
    }


    public List<List<ColumnPair>> getUnjoinableColumns(Table leftTable, Table rightTable)
    {
        if(leftTable == null || rightTable == null)
            return null;

        return unjoinableColumns.get(new TablePair(leftTable, rightTable));
    }
}
