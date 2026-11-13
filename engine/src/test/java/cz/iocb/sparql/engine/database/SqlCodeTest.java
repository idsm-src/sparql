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
 * {@link ExpressionColumn}): code keeps the declared tables its text reads and the outer tables its columns are
 * addressed through; a builder takes them over from the code and from the expression columns appended to it and records
 * a declared table written by its name, whereas plain text, the other tables and the other columns record nothing, and
 * neither does an outer table written by its name alone; code made of pieces by concatenation, by a format or by
 * joining has the tables of the pieces; code is compared and ordered by its text; and the string form of code is no
 * SQL.
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

    /**
     * The alias of the input of a node holding expressions.
     */
    private static final InputTable input = new InputTable("outer", 1);

    /**
     * The alias of the left side of a lateral join.
     */
    private static final LateralTable lateral = new LateralTable(2);


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
        assertTrue(builder.toCode().getOuterTables().isEmpty());
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
    void columnAddressedThroughOuterTableRefersToIt()
    {
        Column column = new TableColumn("id", INT4);

        assertEquals("\"outer1\".\"id\"", column.fromTable(input).toString());
        assertEquals(Set.of(input), column.fromTable(input).getCode().getOuterTables());
        assertEquals("\"lateral2\".\"id\"", column.fromTable(lateral).toString());
        assertEquals(Set.of(lateral), column.fromTable(lateral).getCode().getOuterTables());

        assertTrue(column.fromTable(virtual).getCode().getOuterTables().isEmpty());
        assertTrue(column.fromTable(new AliasTable("tab")).getCode().getOuterTables().isEmpty());
        assertTrue(column.fromTable(input).getCode().getDeclaredTables().isEmpty());

        // a constant is the same wherever it is read, so it is addressed through nothing
        Column constant = new ValueColumn("x", SqlType.VARCHAR);

        assertEquals(constant, constant.fromTable(input));
        assertTrue(constant.fromTable(input).getCode().getOuterTables().isEmpty());
    }


    @Test
    void outerTableWrittenAloneGivesTheAlias()
    {
        SqlCode given = new SqlBuilder().append("(SELECT 1) AS ").append(input).append(", ").append(lateral).toCode();

        assertEquals("(SELECT 1) AS \"outer1\", \"lateral2\"", given.getText());
        assertTrue(given.getOuterTables().isEmpty());

        // an alias of another kind with the same name is another table
        assertNotEquals(input, new AliasTable("outer1"));
        assertNotEquals(lateral, new AliasTable("lateral2"));
        assertEquals(input, new InputTable("outer", 1));
        assertEquals(lateral, new LateralTable(2));
        assertEquals(1, input.getDepth());
    }


    @Test
    void codeMadeOfPiecesRefersToWhatTheyReferTo()
    {
        Column first = new TableColumn("x", INT4).fromTable(input);
        Column second = new TableColumn("y", INT4).fromTable(lateral);

        SqlCode condition = SqlCode.of("(", first, " = ", second, ")");

        assertEquals("(\"outer1\".\"x\" = \"lateral2\".\"y\")", condition.getText());
        assertEquals(Set.of(input, lateral), condition.getOuterTables());

        Column expression = new ExpressionColumn(SqlCode.format("COALESCE(%s, %s)", first, exists), INT4);

        assertEquals(Set.of(input), expression.getCode().getOuterTables());
        assertEquals(Set.of(common), expression.getCode().getDeclaredTables());

        SqlCode query = new SqlBuilder().append("SELECT ").append(expression).append(" FROM (SELECT 1) AS ")
                .append(lateral).append(" WHERE ").append(Stream.of(condition).collect(SqlCode.joining(" AND ")))
                .toCode();

        assertEquals(Set.of(input, lateral), query.getOuterTables());
        assertEquals(Set.of(common), query.getDeclaredTables());
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
        assertNotEquals(new SqlCode("\"outer1\".\"id\""), new TableColumn("id", INT4).fromTable(input).getCode());
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
