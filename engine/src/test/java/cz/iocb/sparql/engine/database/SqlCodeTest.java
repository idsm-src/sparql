package cz.iocb.sparql.engine.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Test;



/**
 * SQL code and its builder (see {@link SqlCode} and {@link SqlBuilder}): code keeps the declared tables its text reads;
 * a builder takes them over from the code appended to it and records a declared table written by its name, whereas
 * plain text and the other tables record nothing; the tables read by SQL rendered beforehand are added by hand; and the
 * string form of code is no SQL.
 */
public class SqlCodeTest
{
    /**
     * A virtual table.
     */
    private static final VirtualTable virtual = new VirtualTable("synonyms");

    /**
     * A common table expression whose query reads the virtual table.
     */
    private static final CommonTable common = new CommonTable(3,
            new SqlBuilder().append("SELECT 1 FROM ").append(virtual).toCode());


    @Test
    void plainTextAndOtherTablesRecordNothing()
    {
        SqlBuilder builder = new SqlBuilder();

        assertTrue(builder.isEmpty());

        builder.append("SELECT ").append(1).append(" FROM ").append(new DatabaseTable("schema", "table"));
        builder.append(" AS ").append(new AliasTable("tab"));

        assertFalse(builder.isEmpty());
        assertEquals("SELECT 1 FROM \"schema\".\"table\" AS \"tab\"", builder.toCode().getText());
        assertTrue(builder.toCode().getDeclaredTables().isEmpty());
    }


    @Test
    void declaredTableWrittenByNameIsRead()
    {
        SqlCode code = new SqlBuilder().append("SELECT 1 FROM ").append(virtual).append(", ").append(common).toCode();

        assertEquals("SELECT 1 FROM \"synonyms\", \"cte3\"", code.getText());
        assertEquals(Set.of(virtual, common), code.getDeclaredTables());
    }


    @Test
    void appendedCodeBringsItsTables()
    {
        SqlCode inner = new SqlBuilder().append("SELECT 1 FROM ").append(common).toCode();
        SqlCode outer = new SqlBuilder().append("SELECT 1 FROM (").append(inner).append(") AS tab").toCode();

        assertEquals("SELECT 1 FROM (SELECT 1 FROM \"cte3\") AS tab", outer.getText());
        assertEquals(Set.of(common), outer.getDeclaredTables());
    }


    @Test
    void tablesOfSqlRenderedBeforehandAreAdded()
    {
        SqlBuilder builder = new SqlBuilder().append("SELECT EXISTS (SELECT 1 FROM \"synonyms\")");

        assertTrue(builder.toCode().getDeclaredTables().isEmpty());

        builder.addDeclaredTables(Set.of(virtual));

        assertEquals(Set.of(virtual), builder.toCode().getDeclaredTables());
    }


    @Test
    void commonTableIsNamedByItsNumberAndKeepsItsQuery()
    {
        assertEquals("cte3", common.getName());
        assertEquals(3, common.getIndex());
        assertEquals("SELECT 1 FROM \"synonyms\"", common.getQuery().getText());
        assertEquals(Set.of(virtual), common.getQuery().getDeclaredTables());
    }


    @Test
    void stringFormIsNoSql()
    {
        SqlCode code = new SqlCode("SELECT 1");

        assertEquals("SELECT 1", code.getText());
        assertFalse(code.toString().startsWith("SELECT"));
        assertEquals(code, new SqlCode("SELECT 1"));
    }
}
