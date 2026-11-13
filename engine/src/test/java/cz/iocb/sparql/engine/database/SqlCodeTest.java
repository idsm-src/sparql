package cz.iocb.sparql.engine.database;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;



/**
 * SQL code, its builder and the columns made of it (see {@link SqlCode}, {@link SqlBuilder} and
 * {@link ExpressionColumn}): code keeps the declared tables its text reads; a builder takes them over from the code and
 * from the expression columns appended to it and records a declared table written by its name, whereas plain text, the
 * other tables and the other columns record nothing; code made of pieces by concatenation, by a format or by joining
 * reads what the pieces read; code is compared and ordered by its text; and the string form of code is no SQL.
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

    /**
     * An expression column whose expression reads the common table expression.
     */
    private static final Column exists = new ExpressionColumn(SqlCode.of("EXISTS (SELECT 1 FROM ", common, ")"), BOOL,
            false);


    @Test
    void plainTextAndOtherPiecesRecordNothing()
    {
        SqlBuilder builder = new SqlBuilder();

        assertTrue(builder.isEmpty());

        builder.append("SELECT ").append(1).append(", ").append(new TableColumn("id", INT4)).append(", ");
        builder.append(new ValueColumn("x", SqlType.VARCHAR)).append(" FROM ");
        builder.append(new DatabaseTable("schema", "table")).append(" AS ").append(new AliasTable("tab"));

        assertFalse(builder.isEmpty());
        assertEquals("SELECT 1, \"id\", 'x'::varchar FROM \"schema\".\"table\" AS \"tab\"", builder.toCode().getText());
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
    void expressionColumnBringsTheTablesOfItsExpression()
    {
        assertEquals("EXISTS (SELECT 1 FROM \"cte3\")", exists.toString());
        assertEquals(Set.of(common), exists.getCode().getDeclaredTables());

        SqlCode code = new SqlBuilder().append("SELECT ").append(exists).append(" AS \"x\"").toCode();

        assertEquals("SELECT EXISTS (SELECT 1 FROM \"cte3\") AS \"x\"", code.getText());
        assertEquals(Set.of(common), code.getDeclaredTables());

        // an expression made of the column reads what the column reads
        Column negation = new ExpressionColumn(SqlCode.of("(NOT ", exists, ")"), BOOL);

        assertEquals("(NOT EXISTS (SELECT 1 FROM \"cte3\"))", negation.toString());
        assertEquals(Set.of(common), negation.getCode().getDeclaredTables());
    }


    @Test
    void codeMadeOfPiecesReadsWhatTheyRead()
    {
        Column other = new TableColumn("y", BOOL);

        SqlCode formatted = SqlCode.format("CASE WHEN %s THEN '%d'::int4 END", exists, 5);

        assertEquals("CASE WHEN EXISTS (SELECT 1 FROM \"cte3\") THEN '5'::int4 END", formatted.getText());
        assertEquals(Set.of(common), formatted.getDeclaredTables());

        SqlCode joined = Stream.of(other, exists).collect(SqlCode.joining(" AND ", "(", ")"));

        assertEquals("(\"y\" AND EXISTS (SELECT 1 FROM \"cte3\"))", joined.getText());
        assertEquals(Set.of(common), joined.getDeclaredTables());

        assertEquals("()", Stream.empty().collect(SqlCode.joining(" AND ", "(", ")")).getText());
        assertEquals("\"y\"", Stream.of(other).collect(SqlCode.joining(", ")).getText());
        assertTrue(Stream.of(other).collect(SqlCode.joining(", ")).getDeclaredTables().isEmpty());
    }


    @Test
    void columnQualifiedByDeclaredTableReadsIt()
    {
        Column column = new TableColumn("id", INT4);

        assertEquals("\"synonyms\".\"id\"", column.fromTable(virtual).toString());
        assertEquals(Set.of(virtual), column.fromTable(virtual).getCode().getDeclaredTables());

        assertEquals("\"tab\".\"id\"", column.fromTable(new AliasTable("tab")).toString());
        assertTrue(column.fromTable(new AliasTable("tab")).getCode().getDeclaredTables().isEmpty());
        assertTrue(column.getCode().getDeclaredTables().isEmpty());
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
    void codeIsComparedAndOrderedByItsText()
    {
        assertEquals(SqlCode.TRUE, new SqlCode("true"));
        assertEquals(SqlCode.TRUE.hashCode(), "true".hashCode());
        assertNotEquals(SqlCode.TRUE, SqlCode.FALSE);
        assertNotEquals(new SqlCode("\"cte3\""), SqlCode.of(common));
        assertTrue(new SqlCode("").isEmpty());
        assertFalse(SqlCode.FALSE.isEmpty());

        assertEquals(List.of("(a)", "(b)", "false", "true"),
                Stream.of(SqlCode.TRUE, new SqlCode("(b)"), SqlCode.FALSE, new SqlCode("(a)")).sorted()
                        .map(SqlCode::getText).toList());
    }


    @Test
    void stringFormIsNoSql()
    {
        SqlCode code = new SqlCode("SELECT 1");

        assertEquals("SELECT 1", code.getText());
        assertFalse(code.toString().startsWith("SELECT"));
    }
}
