package cz.iocb.sparql.engine.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;



/**
 * Definition of a {@link VirtualTable}: the SQL query that computes its rows, the other virtual tables the query reads
 * (so that the {@code WITH} clause of a generated statement can list them first), and the facts about the table that
 * the optimiser cannot read from the catalog: the columns with their types, nullable columns, unique keys, foreign keys
 * and column pairs that never join. The facts are stated the same way as in {@link DatabaseSchema} and are merged into
 * the schema of the configuration when the table is registered; a mapping may use only the declared columns. A
 * statement declares the table as a common table expression that the database evaluates at every place reading it,
 * unless the definition says otherwise (see {@link #setInlined}).
 */
public class VirtualTableDefinition
{
    /**
     * Foreign key: {@code foreignColumns} of {@code foreignTable} reference {@code parentColumns} of
     * {@code parentTable} (matched by position). At least one of the tables is the virtual table being defined.
     *
     * @param parentTable the referenced table
     * @param parentColumns columns of the referenced table
     * @param foreignTable the referencing table
     * @param foreignColumns the referencing columns
     */
    public record ForeignKey(SourceTable parentTable, List<Column> parentColumns, SourceTable foreignTable,
            List<Column> foreignColumns)
    {
    }


    /**
     * Column lists of two tables (matched by position) that never share a value, so an equi-join on them is empty. At
     * least one of the tables is the virtual table being defined.
     *
     * @param leftTable the left table
     * @param leftColumns columns of the left table
     * @param rightTable the right table
     * @param rightColumns columns of the right table
     */
    public record UnjoinableColumns(SourceTable leftTable, List<Column> leftColumns, SourceTable rightTable,
            List<Column> rightColumns)
    {
    }


    /**
     * SQL query computing the rows of the table.
     */
    private final String query;

    /**
     * Virtual tables read by the query, in registration order.
     */
    private final Set<VirtualTable> dependencies = new LinkedHashSet<>();

    /**
     * Columns of the table with their types.
     */
    private final List<TableColumn> columns = new ArrayList<>();

    /**
     * Columns that may be NULL.
     */
    private final List<TableColumn> nullableColumns = new ArrayList<>();

    /**
     * Unique keys, each as its list of columns.
     */
    private final List<List<Column>> primaryKeys = new ArrayList<>();

    /**
     * Foreign keys involving the table.
     */
    private final List<ForeignKey> foreignKeys = new ArrayList<>();

    /**
     * Column lists declared never to join.
     */
    private final List<UnjoinableColumns> unjoinableColumns = new ArrayList<>();

    /**
     * Whether the statements evaluate the query at every place reading the table (see {@link #setInlined}).
     */
    private boolean inlined = true;


    /**
     * Creates the definition of a table computed by the query, which reads no other virtual table.
     *
     * @param query SQL query computing the rows of the table
     */
    public VirtualTableDefinition(String query)
    {
        this.query = query;
    }


    /**
     * Creates the definition of a table computed by the query, which reads the given virtual tables.
     *
     * @param query SQL query computing the rows of the table
     * @param dependencies virtual tables read by the query
     */
    public VirtualTableDefinition(String query, Collection<VirtualTable> dependencies)
    {
        this(query);
        this.dependencies.addAll(dependencies);
    }


    /**
     * Declares that the query reads the given virtual table, which then precedes this table in the {@code WITH} clause.
     *
     * @param table the virtual table read by the query
     */
    public void addDependency(VirtualTable table)
    {
        dependencies.add(table);
    }


    /**
     * Declares a column of the table with its type.
     *
     * @param column the column
     */
    public void addColumn(TableColumn column)
    {
        columns.add(column);
    }


    /**
     * Declares the column nullable.
     *
     * @param column the column
     */
    public void addNullableColumn(TableColumn column)
    {
        nullableColumns.add(column);
    }


    /**
     * Declares the columns to be a unique key of the table.
     *
     * @param columns the columns
     */
    public void addPrimaryKeys(List<Column> columns)
    {
        primaryKeys.add(new ArrayList<>(columns));
    }


    /**
     * Declares a foreign key: {@code foreignColumns} of {@code foreignTable} reference {@code parentColumns} of
     * {@code parentTable} (matched by position). Either table may be the virtual table being defined.
     *
     * @param parentTable the referenced table
     * @param parentColumns columns of the referenced table
     * @param foreignTable the referencing table
     * @param foreignColumns the referencing columns
     */
    public void addForeignKeys(SourceTable parentTable, List<Column> parentColumns, SourceTable foreignTable,
            List<Column> foreignColumns)
    {
        foreignKeys.add(new ForeignKey(parentTable, new ArrayList<>(parentColumns), foreignTable,
                new ArrayList<>(foreignColumns)));
    }


    /**
     * Declares that the column lists of the two tables (matched by position) never share a value, so an equi-join on
     * them is empty. Either table may be the virtual table being defined.
     *
     * @param leftTable the left table
     * @param leftColumns columns of the left table
     * @param rightTable the right table
     * @param rightColumns columns of the right table
     */
    public void addUnjoinableColumns(SourceTable leftTable, List<Column> leftColumns, SourceTable rightTable,
            List<Column> rightColumns)
    {
        unjoinableColumns.add(new UnjoinableColumns(leftTable, new ArrayList<>(leftColumns), rightTable,
                new ArrayList<>(rightColumns)));
    }


    /**
     * Sets whether the statements evaluate the query at every place reading the table, which they do by default: the
     * table is declared {@code AS NOT MATERIALIZED}, so the database evaluates the query in place, with the conditions
     * of the place, and looks the rows up by the indexes of the tables the query reads, also where a statement reads
     * the table at several places, whose results it would keep otherwise, reading each of them whole. A table whose
     * query is expensive and is read whole at several places, or that calls a function that is not deterministic
     * without being declared volatile, is better declared without it, so that the database keeps the result of the
     * query read at several places and evaluates in place only a query read at a single place. A query calling a
     * volatile function is never evaluated in place: the database keeps its result in either case.
     *
     * @param inlined whether the statements evaluate the query at every place reading the table
     */
    public void setInlined(boolean inlined)
    {
        this.inlined = inlined;
    }


    /**
     * SQL query computing the rows of the table.
     *
     * @return SQL query computing the rows of the table
     */
    public String getQuery()
    {
        return query;
    }


    /**
     * Virtual tables read by the query.
     *
     * @return virtual tables read by the query
     */
    public Set<VirtualTable> getDependencies()
    {
        return dependencies;
    }


    /**
     * Columns of the table with their types.
     *
     * @return columns of the table with their types
     */
    public List<TableColumn> getColumns()
    {
        return columns;
    }


    /**
     * Columns that may be NULL.
     *
     * @return columns that may be NULL
     */
    public List<TableColumn> getNullableColumns()
    {
        return nullableColumns;
    }


    /**
     * Unique keys, each as its list of columns.
     *
     * @return unique keys, each as its list of columns
     */
    public List<List<Column>> getPrimaryKeys()
    {
        return primaryKeys;
    }


    /**
     * Foreign keys involving the table.
     *
     * @return foreign keys involving the table
     */
    public List<ForeignKey> getForeignKeys()
    {
        return foreignKeys;
    }


    /**
     * Column lists declared never to join.
     *
     * @return column lists declared never to join
     */
    public List<UnjoinableColumns> getUnjoinableColumns()
    {
        return unjoinableColumns;
    }


    /**
     * Whether the statements evaluate the query at every place reading the table (see {@link #setInlined}).
     *
     * @return true if the statements evaluate the query at every place reading the table, false otherwise
     */
    public boolean isInlined()
    {
        return inlined;
    }


    @Override
    public int hashCode()
    {
        return query.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        VirtualTableDefinition other = (VirtualTableDefinition) object;

        return query.equals(other.query) && dependencies.equals(other.dependencies) && columns.equals(other.columns)
                && nullableColumns.equals(other.nullableColumns) && primaryKeys.equals(other.primaryKeys)
                && foreignKeys.equals(other.foreignKeys) && unjoinableColumns.equals(other.unjoinableColumns)
                && inlined == other.inlined;
    }
}
