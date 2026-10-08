package cz.iocb.sparql.engine.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.testing.Database;



/**
 * Catalog facts read by {@link DatabaseSchema}: every kind of relation the configuration may map has to report its
 * nullable columns, only a valid full unique index over columns is a key, only a validated foreign key of the
 * referencing table itself (not one of its copies on the partitions of a referenced partitioned table) is a foreign
 * key.
 */
public class DatabaseSchemaTest
{
    /**
     * Pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Catalog read after the test relations were created.
     */
    private static DatabaseSchema schema = null;


    /**
     * Creates the {@code schema_test} schema with a table carrying a partial and an expression unique index, a view, a
     * materialized view, partitioned tables with an invalid and a valid key, a foreign table, foreign keys to and from
     * partitioned tables and a NOT VALID one, two tables whose names match as patterns, and reads the catalog.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema schema_test");

            statement.execute(
                    "create table schema_test.plain (id int primary key, name varchar not null, " + "note varchar)");
            statement.execute("create unique index plain_note on schema_test.plain (note) where id > 0");
            statement.execute("create unique index plain_name on schema_test.plain (lower(name))");

            statement.execute("create view schema_test.plain_view as select id, name from schema_test.plain");

            statement.execute("create materialized view schema_test.plain_matview as select id, note "
                    + "from schema_test.plain");
            statement.execute("create unique index plain_matview_id on schema_test.plain_matview (id)");

            statement.execute("create table schema_test.parted (id int not null, kind int not null, label varchar) "
                    + "partition by list (kind)");
            statement.execute("create table schema_test.parted_one partition of schema_test.parted for values in (1)");

            statement.execute("create foreign data wrapper schema_test_wrapper");
            statement.execute("create server schema_test_server foreign data wrapper schema_test_wrapper");
            statement.execute("create foreign table schema_test.remote (id int not null, label varchar) "
                    + "server schema_test_server");

            // a unique index created only on the parent stays invalid until every partition has one attached
            statement.execute("create unique index parted_key on only schema_test.parted (id, kind)");

            statement.execute("create table schema_test.keyed (id int not null, kind int not null, "
                    + "primary key (id, kind)) partition by list (kind)");
            statement.execute("create table schema_test.keyed_one partition of schema_test.keyed for values in (1)");
            statement.execute("create table schema_test.keyed_two partition of schema_test.keyed for values in (2)");

            // the foreign key of the referencing table is copied to every partition of the referenced one
            statement.execute("create table schema_test.referencing (id int primary key, keyed int not null, "
                    + "kind int not null, foreign key (keyed, kind) references schema_test.keyed (id, kind))");

            // the foreign key of a partitioned referencing table is copied to its partitions, which do reference
            statement.execute("create table schema_test.parted_referencing (id int not null, plain int not null "
                    + "references schema_test.plain (id)) partition by list (id)");
            statement.execute("create table schema_test.parted_referencing_one partition of "
                    + "schema_test.parted_referencing for values in (1)");

            statement.execute("create table schema_test.unchecked (id int primary key, plain int not null)");
            statement.execute("alter table schema_test.unchecked add foreign key (plain) "
                    + "references schema_test.plain (id) not valid");

            // the names are not patterns: like_a does not stand for likexa
            statement.execute("create table schema_test.like_a (a int not null)");
            statement.execute("create table schema_test.likexa (b int)");
        }

        schema = new DatabaseSchema(connectionPool);
    }


    /**
     * Drops the test relations, so that the catalogs read by the other tests do not see them.
     */
    @AfterAll
    static void cleanup() throws SQLException
    {
        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("drop schema schema_test cascade");
            statement.execute("drop server schema_test_server");
            statement.execute("drop foreign data wrapper schema_test_wrapper");
        }
    }


    /**
     * True if the catalog says that the column of the relation may be NULL; the column has to be known.
     */
    private static boolean isNullable(String relation, String column)
    {
        DatabaseTable table = new DatabaseTable("schema_test", relation);
        TableColumn tableColumn = schema.getColumn(table, column);

        assertNotNull(tableColumn, () -> "column " + column + " of " + relation);

        return schema.isNullableColumn(table, tableColumn);
    }


    /**
     * A unique key of the relation within the given columns, which have to be known, or null.
     */
    private static List<Column> getKey(String relation, String... columns)
    {
        DatabaseTable table = new DatabaseTable("schema_test", relation);
        Set<Column> keyColumns = new HashSet<>();

        for(String column : columns)
            keyColumns.add(Objects.requireNonNull(schema.getColumn(table, column), column));

        return schema.getCompatibleKey(table, keyColumns);
    }


    /**
     * The foreign keys by which the rows of the foreign relation reference the parent relation.
     */
    private static List<Set<ColumnPair>> getForeignKeys(String parent, String foreign)
    {
        return schema.getForeignKeys(new DatabaseTable("schema_test", parent),
                new DatabaseTable("schema_test", foreign));
    }


    @Test
    @DisplayName("nullable columns of a table and a view")
    void tableAndView()
    {
        assertFalse(isNullable("plain", "name"));
        assertTrue(isNullable("plain", "note"));
        assertTrue(isNullable("plain_view", "name"));
    }


    @Test
    @DisplayName("columns of a materialized view may be NULL")
    void materializedView()
    {
        assertTrue(isNullable("plain_matview", "id"));
        assertTrue(isNullable("plain_matview", "note"));
    }


    @Test
    @DisplayName("a unique index of a materialized view is a key")
    void materializedViewKey()
    {
        assertNotNull(getKey("plain_matview", "id"));
    }


    @Test
    @DisplayName("nullable columns of a partitioned table and its partition")
    void partitionedTable()
    {
        assertFalse(isNullable("parted", "id"));
        assertTrue(isNullable("parted", "label"));
        assertFalse(isNullable("parted_one", "id"));
        assertTrue(isNullable("parted_one", "label"));
    }


    @Test
    @DisplayName("nullable columns of a foreign table")
    void foreignTable()
    {
        assertFalse(isNullable("remote", "id"));
        assertTrue(isNullable("remote", "label"));
    }


    @Test
    @DisplayName("a primary key is a key, a partial or an expression unique index is not")
    void uniqueIndexes()
    {
        assertNotNull(getKey("plain", "id"));
        assertNull(getKey("plain", "note"));
        assertNull(getKey("plain", "name"));
        assertNull(getKey("plain", "name", "note"));
    }


    @Test
    @DisplayName("the key of a partitioned table is a key, an invalid unique index is not")
    void partitionedKeys()
    {
        assertNotNull(getKey("keyed", "id", "kind"));
        assertNotNull(getKey("keyed_one", "id", "kind"));
        assertNull(getKey("parted", "id", "kind"));
    }


    @Test
    @DisplayName("a foreign key to a partitioned table does not reference each partition")
    void foreignKeyToPartitions()
    {
        assertFalse(getForeignKeys("keyed", "referencing").isEmpty());
        assertTrue(getForeignKeys("keyed_one", "referencing").isEmpty());
        assertTrue(getForeignKeys("keyed_two", "referencing").isEmpty());
    }


    @Test
    @DisplayName("a foreign key of a partitioned table holds for its partitions")
    void foreignKeyOfPartitions()
    {
        assertFalse(getForeignKeys("plain", "parted_referencing").isEmpty());
        assertFalse(getForeignKeys("plain", "parted_referencing_one").isEmpty());
    }


    @Test
    @DisplayName("a foreign key added as NOT VALID is not a fact")
    void unvalidatedForeignKey()
    {
        assertTrue(getForeignKeys("plain", "unchecked").isEmpty());
    }


    @Test
    @DisplayName("relation names are not patterns")
    void relationNames()
    {
        assertFalse(isNullable("like_a", "a"));
        assertNull(schema.getColumn(new DatabaseTable("schema_test", "like_a"), "b"));
        assertTrue(isNullable("likexa", "b"));
    }
}
