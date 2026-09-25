package cz.iocb.sparql.engine.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.database.VirtualTableDefinition.ForeignKey;
import cz.iocb.sparql.engine.database.VirtualTableDefinition.UnjoinableColumns;



/**
 * Catalog facts about the source tables used by the optimiser: nullable columns, unique keys, foreign keys, column
 * pairs known never to join, and character columns with a collation that does not order by code points. The facts about
 * database tables are read from the JDBC metadata and the PostgreSQL catalog (or filled in by hand); the facts about
 * virtual tables are merged in from their definitions by {@link #addVirtualTable}.
 */
public class DatabaseSchema
{
    /**
     * Catalog query listing every column of a user table or view that has an explicit collation. SPARQL orders and
     * compares strings by unicode code points, whereas PostgreSQL orders a character column by its collation, so only
     * the collations that happen to order by code points give the results the specification asks for. The name of a
     * collation does not say which ones those are (musl, for instance, orders bytewise under every locale name), so the
     * server is asked directly.
     */
    private static final String collationQuery = """
            SELECT n.nspname, c.relname, a.attname, a.attcollation::regcollation::text
            FROM pg_attribute a
            JOIN pg_class c ON c.oid = a.attrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE a.attcollation <> 0 AND a.attnum > 0 AND NOT a.attisdropped
                AND c.relkind IN ('r', 'v', 'm', 'p', 'f')
                AND n.nspname NOT IN ('pg_catalog', 'information_schema')""";

    /**
     * Query template testing whether a collation orders a sample of strings the same way as the "C" collation.
     */
    private static final String collationProbeQuery = """
            SELECT array_agg(s ORDER BY s COLLATE %s) = array_agg(s ORDER BY s COLLATE "C")
            FROM (VALUES ('A'), ('a'), ('B'), ('b'), ('Z'), ('z'), ('0'), ('_'), ('-'), (' '), ('E'), ('\u00e9'))
                AS probe(s)""";


    /**
     * Nullable columns per table.
     */
    protected final Map<SourceTable, List<Column>> nullableColumns = new HashMap<>();

    /**
     * Per table, the character columns whose collation does not order by code points, with the collation name.
     */
    protected final Map<SourceTable, Map<Column, String>> foreignCollations = new HashMap<>();

    /**
     * Unique keys per table, each as its list of columns.
     */
    protected final Map<SourceTable, List<List<Column>>> primaryKeys = new HashMap<>();

    /**
     * Foreign keys per (parent table, foreign table) pair, each as its set of column pairs.
     */
    protected final Map<TablePair, List<Set<ColumnPair>>> foreignKeys = new HashMap<>();

    /**
     * Column lists declared never to join, per (left table, right table) pair.
     */
    protected final Map<TablePair, List<List<ColumnPair>>> unjoinableColumns = new HashMap<>();


    /**
     * Reads tables and views, their nullable columns, unique indexes, foreign keys and column collations from the
     * database.
     *
     * @param connectionPool the connection pool of the database
     * @throws SQLException on database errors
     */
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
                    SourceTable table = new DatabaseTable(tableSchema, tableName);


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
                        SourceTable foreignTable = null;
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

                                foreignTable = new DatabaseTable(indexes.getString("FKTABLE_SCHEM"),
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


            List<String[]> collatedColumns = new ArrayList<>();

            try(Statement statement = connection.createStatement())
            {
                try(ResultSet collations = statement.executeQuery(collationQuery))
                {
                    while(collations.next())
                        collatedColumns.add(new String[] { collations.getString(1), collations.getString(2),
                                collations.getString(3), collations.getString(4) });
                }
            }

            Map<String, Boolean> checkedCollations = new HashMap<>();

            for(String[] collatedColumn : collatedColumns)
            {
                String collation = collatedColumn[3];

                if(checkedCollations.computeIfAbsent(collation, c -> isCodepointCollation(connection, c)))
                    continue;

                addForeignCollation(new DatabaseTable(collatedColumn[0], collatedColumn[1]),
                        new TableColumn(collatedColumn[2]), collation);
            }
        }
    }


    /**
     * Returns whether the given PostgreSQL collation orders strings by unicode code points, which is the ordering that
     * SPARQL prescribes. The collation is compared against the "C" collation on a sample of strings, because its name
     * alone does not tell: musl, for example, orders bytewise under every locale name, whereas the same name means a
     * locale ordering under glibc.
     *
     * @param connection the database connection
     * @param collation name of the collation
     * @return true if the collation orders by code points, false otherwise
     */
    protected boolean isCodepointCollation(Connection connection, String collation)
    {
        try(Statement statement = connection.createStatement())
        {
            try(ResultSet result = statement.executeQuery(collationProbeQuery.formatted(collation)))
            {
                return result.next() && result.getBoolean(1);
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    /**
     * Copy constructor.
     *
     * @param other the schema to copy
     */
    public DatabaseSchema(DatabaseSchema other)
    {
        for(Entry<SourceTable, List<Column>> e : other.nullableColumns.entrySet())
            nullableColumns.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<SourceTable, Map<Column, String>> e : other.foreignCollations.entrySet())
            foreignCollations.put(e.getKey(), new HashMap<>(e.getValue()));

        for(Entry<SourceTable, List<List<Column>>> e : other.primaryKeys.entrySet())
            primaryKeys.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<TablePair, List<Set<ColumnPair>>> e : other.foreignKeys.entrySet())
            foreignKeys.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<TablePair, List<List<ColumnPair>>> e : other.unjoinableColumns.entrySet())
            unjoinableColumns.put(e.getKey(), new ArrayList<>(e.getValue()));
    }


    /**
     * Merges the facts stated by the definition of a virtual table: its nullable columns, unique keys, foreign keys and
     * unjoinable column lists. The query of the definition is not needed here; it is attached to the generated
     * statements by the translator.
     *
     * @param table the virtual table
     * @param definition the definition of the virtual table
     */
    public void addVirtualTable(VirtualTable table, VirtualTableDefinition definition)
    {
        for(TableColumn column : definition.getNullableColumns())
            addNullableColumn(table, column);

        for(List<Column> key : definition.getPrimaryKeys())
            addPrimaryKeys(table, key);

        for(ForeignKey key : definition.getForeignKeys())
            addForeignKeys(key.parentTable(), key.parentColumns(), key.foreignTable(), key.foreignColumns());

        for(UnjoinableColumns unjoinable : definition.getUnjoinableColumns())
            addUnjoinableColumns(unjoinable.leftTable(), unjoinable.leftColumns(), unjoinable.rightTable(),
                    unjoinable.rightColumns());
    }


    /**
     * Declares the column nullable.
     *
     * @param table the table
     * @param column the column
     */
    public void addNullableColumn(SourceTable table, TableColumn column)
    {
        List<Column> columnList = nullableColumns.get(table);

        if(columnList == null)
        {
            columnList = new ArrayList<>();
            nullableColumns.put(table, columnList);
        }

        columnList.add(column);
    }


    /**
     * Declares the columns to be a unique key of the table.
     *
     * @param table the table
     * @param columns the columns
     */
    public void addPrimaryKeys(SourceTable table, List<Column> columns)
    {
        List<List<Column>> primaryKeyList = primaryKeys.get(table);

        if(primaryKeyList == null)
        {
            primaryKeyList = new ArrayList<>();
            primaryKeys.put(table, primaryKeyList);
        }

        primaryKeyList.add(new ArrayList<>(columns));
    }


    /**
     * Declares a foreign key: {@code foreignColumns} of {@code foreignTable} reference {@code parentColumns} of
     * {@code parentTable} (matched by position).
     *
     * @param parentTable the referenced table
     * @param parentColumns columns of the referenced table
     * @param foreignTable the referencing table
     * @param foreignColumns the referencing columns
     */
    public void addForeignKeys(SourceTable parentTable, List<Column> parentColumns, SourceTable foreignTable,
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


    /**
     * Declares that the column lists of the two tables (matched by position) never share a value, so an equi-join on
     * them is empty.
     *
     * @param leftTable the left table
     * @param leftColumns columns of the left table
     * @param rightTable the right table
     * @param rightColumns columns of the right table
     */
    public void addUnjoinableColumns(SourceTable leftTable, List<Column> leftColumns, SourceTable rightTable,
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


    /**
     * Records that the character column uses a collation that does not order by code points.
     *
     * @param table the table
     * @param column the column
     * @param collation name of the collation
     */
    public void addForeignCollation(SourceTable table, TableColumn column, String collation)
    {
        Map<Column, String> columnMap = foreignCollations.get(table);

        if(columnMap == null)
        {
            columnMap = new HashMap<>();
            foreignCollations.put(table, columnMap);
        }

        columnMap.put(column, collation);
    }


    /**
     * Returns the collation of the given character column when it does not order by unicode code points, and null when
     * the column orders the way SPARQL prescribes or is not a character column at all.
     *
     * @param table the table
     * @param column the column
     * @return the collation of the given character column when it does not order by unicode code points, and null when
     *         the column orders the way SPARQL prescribes or is not a character column at all
     */
    public String getForeignCollation(SourceTable table, Column column)
    {
        return foreignCollations.getOrDefault(table, Map.of()).get(column);
    }


    /**
     * True if the column may be NULL: never for constants, as declared for expressions, per catalog for table columns.
     *
     * @param table the table
     * @param column the column
     * @return true if the column may be NULL, false otherwise
     */
    public boolean isNullableColumn(SourceTable table, Column column)
    {
        return switch(column)
        {
            case ConstantColumn col -> col.canBeNull();
            case ExpressionColumn col -> col.canBeNull();
            default -> column.canBeNull() && nullableColumns.getOrDefault(table, List.of()).contains(column);
        };
    }


    /**
     * A unique key of the table all of whose columns are among {@code columns}, or null.
     *
     * @param table the table
     * @param columns the columns
     * @return A unique key of the table all of whose columns are among {@code columns}, or null
     */
    public List<Column> getCompatibleKey(SourceTable table, Set<Column> columns)
    {
        List<List<Column>> keys = primaryKeys.get(table);

        if(keys == null)
            return null;

        for(List<Column> key : keys)
            if(columns.containsAll(key))
                return key;

        return null;
    }


    /**
     * A foreign key from {@code childTable} to {@code parentTable} that contains all the given join pairs and whose
     * child columns cover {@code extra}, or null. Every child row then has exactly one matching parent row.
     *
     * @param parentTable the referenced table
     * @param childTable the referencing table
     * @param columns the join column pairs
     * @param extra child columns that must be covered by the key
     * @return A foreign key from {@code childTable} to {@code parentTable} that contains all the given join pairs and
     *         whose child columns cover {@code extra}, or null
     */
    public Set<ColumnPair> isPartOfForeignKey(SourceTable parentTable, SourceTable childTable, Set<ColumnPair> columns,
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


    /**
     * A foreign key from {@code childTable} to {@code parentTable} all of whose pairs are among the given join pairs
     * and whose child columns cover, through the pairs, all of {@code parentColumns}, or null. The parent side of the
     * join can then be dropped.
     *
     * @param parentTable the referenced table
     * @param childTable the referencing table
     * @param columns the join column pairs
     * @param parentColumns columns of the referenced table
     * @return A foreign key from {@code childTable} to {@code parentTable} all of whose pairs are among the given join
     *         pairs and whose child columns cover, through the pairs, all of {@code parentColumns}, or null
     */
    public Set<ColumnPair> getCompatibleForeignKey(SourceTable parentTable, SourceTable childTable,
            Set<ColumnPair> columns, Set<Column> parentColumns)
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


    /**
     * A declared unjoinable column list fully contained in the given join pairs, or null.
     *
     * @param leftTable the left table
     * @param rightTable the right table
     * @param columns the join column pairs
     * @return A declared unjoinable column list fully contained in the given join pairs, or null
     */
    public List<ColumnPair> getUnjoinableColumns(SourceTable leftTable, SourceTable rightTable,
            List<ColumnPair> columns)
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


    /**
     * True if the columns contain a unique key of the table.
     *
     * @param table the table
     * @param columns the columns
     * @return true if the columns contain a unique key of the table, false otherwise
     */
    public boolean isKey(SourceTable table, Set<Column> columns)
    {
        return getCompatibleKey(table, columns) != null;
    }


    /**
     * Foreign keys from {@code foreignTable} to {@code parentTable}, each as its set of column pairs.
     *
     * @param parentTable the referenced table
     * @param foreignTable the referencing table
     * @return foreign keys from {@code foreignTable} to {@code parentTable}, each as its set of column pairs
     */
    public List<Set<ColumnPair>> getForeignKeys(SourceTable parentTable, SourceTable foreignTable)
    {
        if(parentTable == null || foreignTable == null)
            return List.of();

        return foreignKeys.getOrDefault(new TablePair(parentTable, foreignTable), List.of());
    }


    /**
     * Declared unjoinable column lists between the two tables, or null.
     *
     * @param leftTable the left table
     * @param rightTable the right table
     * @return declared unjoinable column lists between the two tables, or null
     */
    public List<List<ColumnPair>> getUnjoinableColumns(SourceTable leftTable, SourceTable rightTable)
    {
        if(leftTable == null || rightTable == null)
            return null;

        return unjoinableColumns.get(new TablePair(leftTable, rightTable));
    }
}
