package cz.iocb.sparql.engine.database;

import java.sql.Connection;
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
 * Catalog facts about the source tables used by the optimiser and the translator: the columns with their SQL types,
 * nullable columns, unique keys, foreign keys, column pairs known never to join, the collations of the character
 * columns that do not use the default one, and character columns with a collation that does not order by code points.
 * The facts about database tables are read from the PostgreSQL catalog (or filled in by hand); the facts about virtual
 * tables are merged in from their definitions by {@link #addVirtualTable}.
 */
public class DatabaseSchema
{
    /**
     * Catalog query listing every column of a user table, view, materialized view, partitioned or foreign table with
     * the schema and the name of its type, and whether it is declared NOT NULL (directly or by its domain). The type is
     * named the way a configuration names it (see {@link SqlType#of}): by the bare name of a type of the
     * {@code pg_catalog} or {@code public} schema, which is the canonical name of a built-in type, and schema-qualified
     * otherwise.
     */
    private static final String columnTypeQuery = """
            SELECT n.nspname, c.relname, a.attname, tn.nspname, t.typname,
                a.attnotnull OR (t.typtype = 'd' AND t.typnotnull)
            FROM pg_attribute a
            JOIN pg_class c ON c.oid = a.attrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_type t ON t.oid = a.atttypid
            JOIN pg_namespace tn ON tn.oid = t.typnamespace
            WHERE a.attnum > 0 AND NOT a.attisdropped
                AND c.relkind IN ('r', 'v', 'm', 'p', 'f')
                AND n.nspname NOT IN ('pg_catalog', 'information_schema')""";

    /**
     * Catalog query listing every column of a user table or view that has a collation, named as an SQL identifier
     * ({@code "default"} for the default collation of the database). The translator needs the collations of the columns
     * to keep the collations of a recursive query consistent. Besides, SPARQL orders and compares strings by unicode
     * code points, whereas PostgreSQL orders a character column by its collation, so only the collations that happen to
     * order by code points give the results the specification asks for. The name of a collation does not say which ones
     * those are (musl, for instance, orders bytewise under every locale name), so the server is asked directly.
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
     * Catalog query listing the key columns of every unique index of a user relation that makes them unique across all
     * rows, ordered by the index and the position: the index has to be valid (an index created with {@code ON ONLY} on
     * a partitioned table, or one whose concurrent build failed, does not guarantee anything), not partial and not over
     * an expression; the included non-key columns are left out.
     */
    private static final String keyQuery = """
            SELECT i.indexrelid, n.nspname, c.relname, a.attname
            FROM pg_index i
            JOIN pg_class c ON c.oid = i.indrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            CROSS JOIN LATERAL unnest(i.indkey::int2[]) WITH ORDINALITY AS k(attnum, position)
            JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = k.attnum
            WHERE i.indisunique AND i.indisvalid AND i.indpred IS NULL AND i.indexprs IS NULL
                AND k.position <= i.indnkeyatts
                AND c.relkind IN ('r', 'v', 'm', 'p', 'f')
                AND n.nspname NOT IN ('pg_catalog', 'information_schema')
            ORDER BY i.indexrelid, k.position""";

    /**
     * Catalog query listing the column pairs of every validated foreign key of a user table, ordered by the constraint
     * and the position, each pair as the referenced (parent) column and the referencing (foreign) column. A constraint
     * added {@code NOT VALID} is left out, because the rows that existed before it need not satisfy it, and so are the
     * copies of a constraint that PostgreSQL attaches to the partitions of a referenced partitioned table: they have
     * the same referencing table as their parent constraint, and its rows reference just one of the partitions.
     */
    private static final String foreignKeyQuery = """
            SELECT k.oid, pn.nspname, pc.relname, pa.attname, fn.nspname, fc.relname, fa.attname
            FROM pg_constraint k
            JOIN pg_class pc ON pc.oid = k.confrelid
            JOIN pg_namespace pn ON pn.oid = pc.relnamespace
            JOIN pg_class fc ON fc.oid = k.conrelid
            JOIN pg_namespace fn ON fn.oid = fc.relnamespace
            CROSS JOIN LATERAL unnest(k.confkey, k.conkey) WITH ORDINALITY AS c(pattnum, fattnum, position)
            JOIN pg_attribute pa ON pa.attrelid = k.confrelid AND pa.attnum = c.pattnum
            JOIN pg_attribute fa ON fa.attrelid = k.conrelid AND fa.attnum = c.fattnum
            WHERE k.contype = 'f' AND k.convalidated
                AND NOT EXISTS (SELECT 1 FROM pg_constraint p WHERE p.oid = k.conparentid AND p.conrelid = k.conrelid)
                AND pn.nspname NOT IN ('pg_catalog', 'information_schema')
                AND fn.nspname NOT IN ('pg_catalog', 'information_schema')
            ORDER BY k.oid, c.position""";

    /**
     * Query template testing whether a collation orders a sample of strings the same way as the "C" collation.
     */
    private static final String collationProbeQuery = """
            SELECT array_agg(s ORDER BY s COLLATE %s) = array_agg(s ORDER BY s COLLATE "C")
            FROM (VALUES ('A'), ('a'), ('B'), ('b'), ('Z'), ('z'), ('0'), ('_'), ('-'), (' '), ('E'), ('\u00e9'))
                AS probe(s)""";


    /**
     * SQL types of the columns per table.
     */
    protected final Map<SourceTable, Map<Column, SqlType>> columnTypes = new HashMap<>();

    /**
     * Nullable columns per table.
     */
    protected final Map<SourceTable, List<Column>> nullableColumns = new HashMap<>();

    /**
     * Per table, the character columns whose collation does not order by code points, with the collation name.
     */
    protected final Map<SourceTable, Map<Column, String>> foreignCollations = new HashMap<>();

    /**
     * Per table, the character columns with another collation than the default one of the database, with the collation
     * name as an SQL identifier.
     */
    protected final Map<SourceTable, Map<Column, String>> columnCollations = new HashMap<>();

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
     * Reads tables, views, materialized views, partitioned and foreign tables, their columns with the types, nullable
     * columns, keys, foreign keys and column collations from the catalog of the database.
     *
     * @param connectionPool the connection pool of the database
     * @throws SQLException on database errors
     */
    public DatabaseSchema(DataSource connectionPool) throws SQLException
    {
        try(Connection connection = connectionPool.getConnection())
        {
            connection.setAutoCommit(true);

            try(Statement statement = connection.createStatement())
            {
                try(ResultSet columns = statement.executeQuery(columnTypeQuery))
                {
                    while(columns.next())
                    {
                        String typeSchema = columns.getString(4);
                        String typeName = columns.getString(5);

                        if(!typeSchema.equals("pg_catalog") && !typeSchema.equals("public"))
                            typeName = typeSchema + "." + typeName;

                        SourceTable table = new DatabaseTable(columns.getString(1), columns.getString(2));
                        TableColumn column = new TableColumn(columns.getString(3), SqlType.of(typeName));

                        addColumn(table, column);

                        if(!columns.getBoolean(6))
                            addNullableColumn(table, column);
                    }
                }


                try(ResultSet keys = statement.executeQuery(keyQuery))
                {
                    long index = 0;
                    SourceTable table = null;
                    List<Column> columns = new ArrayList<>();

                    while(keys.next())
                    {
                        if(keys.getLong(1) != index)
                        {
                            if(table != null)
                                addPrimaryKeys(table, columns);

                            index = keys.getLong(1);
                            table = new DatabaseTable(keys.getString(2), keys.getString(3));
                            columns = new ArrayList<>();
                        }

                        columns.add(requireColumn(table, keys.getString(4)));
                    }

                    if(table != null)
                        addPrimaryKeys(table, columns);
                }


                try(ResultSet keys = statement.executeQuery(foreignKeyQuery))
                {
                    long constraint = 0;
                    SourceTable parentTable = null;
                    SourceTable foreignTable = null;
                    List<Column> parentColumns = new ArrayList<>();
                    List<Column> foreignColumns = new ArrayList<>();

                    while(keys.next())
                    {
                        if(keys.getLong(1) != constraint)
                        {
                            if(parentTable != null)
                                addForeignKeys(parentTable, parentColumns, foreignTable, foreignColumns);

                            constraint = keys.getLong(1);
                            parentTable = new DatabaseTable(keys.getString(2), keys.getString(3));
                            foreignTable = new DatabaseTable(keys.getString(5), keys.getString(6));
                            parentColumns = new ArrayList<>();
                            foreignColumns = new ArrayList<>();
                        }

                        parentColumns.add(requireColumn(parentTable, keys.getString(4)));
                        foreignColumns.add(requireColumn(foreignTable, keys.getString(7)));
                    }

                    if(parentTable != null)
                        addForeignKeys(parentTable, parentColumns, foreignTable, foreignColumns);
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
                SourceTable table = new DatabaseTable(collatedColumn[0], collatedColumn[1]);
                TableColumn column = requireColumn(table, collatedColumn[2]);

                if(!collation.equals("\"default\""))
                    addCollation(table, column, collation);

                if(!checkedCollations.computeIfAbsent(collation, c -> isCodepointCollation(connection, c)))
                    addForeignCollation(table, column, collation);
            }
        }
    }


    /**
     * Reference to the named column of the table typed by the catalog, which has to list the column.
     *
     * @param table the table
     * @param name the column name
     * @return reference to the named column of the table typed by the catalog
     * @throws SQLException if the catalog does not list the column
     */
    private TableColumn requireColumn(SourceTable table, String name) throws SQLException
    {
        TableColumn column = getColumn(table, name);

        if(column == null)
            throw new SQLException("type of column \"" + name + "\" of table " + table + " is not known");

        return column;
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
        for(Entry<SourceTable, Map<Column, SqlType>> e : other.columnTypes.entrySet())
            columnTypes.put(e.getKey(), new HashMap<>(e.getValue()));

        for(Entry<SourceTable, List<Column>> e : other.nullableColumns.entrySet())
            nullableColumns.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<SourceTable, Map<Column, String>> e : other.foreignCollations.entrySet())
            foreignCollations.put(e.getKey(), new HashMap<>(e.getValue()));

        for(Entry<SourceTable, Map<Column, String>> e : other.columnCollations.entrySet())
            columnCollations.put(e.getKey(), new HashMap<>(e.getValue()));

        for(Entry<SourceTable, List<List<Column>>> e : other.primaryKeys.entrySet())
            primaryKeys.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<TablePair, List<Set<ColumnPair>>> e : other.foreignKeys.entrySet())
            foreignKeys.put(e.getKey(), new ArrayList<>(e.getValue()));

        for(Entry<TablePair, List<List<ColumnPair>>> e : other.unjoinableColumns.entrySet())
            unjoinableColumns.put(e.getKey(), new ArrayList<>(e.getValue()));
    }


    /**
     * Merges the facts stated by the definition of a virtual table: its columns with their types, nullable columns,
     * unique keys, foreign keys and unjoinable column lists. The query of the definition is not needed here; it is
     * attached to the generated statements by the translator.
     *
     * @param table the virtual table
     * @param definition the definition of the virtual table
     */
    public void addVirtualTable(VirtualTable table, VirtualTableDefinition definition)
    {
        for(TableColumn column : definition.getColumns())
            addColumn(table, column);

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
     * Declares a column of the table with its type.
     *
     * @param table the table
     * @param column the column
     */
    public void addColumn(SourceTable table, TableColumn column)
    {
        columnTypes.computeIfAbsent(table, _ -> new HashMap<>()).put(column, column.getType());
    }


    /**
     * SQL type of the column of the table, or null when the schema does not know the column.
     *
     * @param table the table
     * @param column the column
     * @return SQL type of the column of the table, or null when the schema does not know the column
     */
    public SqlType getColumnType(SourceTable table, Column column)
    {
        return columnTypes.getOrDefault(table, Map.of()).get(column);
    }


    /**
     * Reference to the named column of the table typed by the schema, or null when the schema does not know the column.
     *
     * @param table the table
     * @param name the column name
     * @return reference to the named column of the table typed by the schema, or null when the schema does not know the
     *         column
     */
    public TableColumn getColumn(SourceTable table, String name)
    {
        SqlType type = columnTypes.getOrDefault(table, Map.of()).entrySet().stream()
                .filter(e -> e.getKey().getName().equals(name)).map(Entry::getValue).findFirst().orElse(null);

        return type != null ? new TableColumn(name, type) : null;
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
     * Records that the character column uses another collation than the default one of the database.
     *
     * @param table the table
     * @param column the column
     * @param collation name of the collation as an SQL identifier, such as {@code "C"}
     */
    public void addCollation(SourceTable table, TableColumn column, String collation)
    {
        columnCollations.computeIfAbsent(table, _ -> new HashMap<>()).put(column, collation);
    }


    /**
     * Returns the collation of the given character column as an SQL identifier when it is not the default one of the
     * database, and null when the column uses the default collation, is not a character column, or is not known.
     *
     * @param table the table
     * @param column the column
     * @return the collation of the given character column as an SQL identifier when it is not the default one of the
     *         database, and null otherwise
     */
    public String getCollation(SourceTable table, Column column)
    {
        return columnCollations.getOrDefault(table, Map.of()).get(column);
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
            case TableColumn col -> col.canBeNull() && nullableColumns.getOrDefault(table, List.of()).contains(col);
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
