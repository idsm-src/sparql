package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration.getColumn;
import static cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration.getColumns;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Parsing of column specifications by {@link SparqlDatabaseConfiguration#getColumn(String)} and
 * {@link SparqlDatabaseConfiguration#getColumns(ResourceClass, String...)}.
 */
public class ColumnSpecificationTest
{
    @Test
    void tableColumn()
    {
        assertThrows(IllegalArgumentException.class, () -> getColumn("id"));

        List<Column> columns = getColumns(xsdDate, "d", "'0'::integer");

        assertInstanceOf(TableColumn.class, columns.get(0));
        assertEquals("\"d\"", columns.get(0).toString());
        assertEquals(SqlType.DATE, columns.get(0).getType());
        assertTrue(columns.get(0).canBeNull());
        assertEquals(new ValueColumn("0", SqlType.INT4), columns.get(1));

        assertEquals(List.of(new TableColumn("s", SqlType.VARCHAR)), getColumns(xsdString, "s"));
        assertEquals(SqlType.VARCHAR, getColumns(xsdString, "s").get(0).getType());

        assertThrows(IllegalArgumentException.class, () -> getColumns(xsdString, "s", "t"));
        assertThrows(IllegalArgumentException.class, () -> getColumns(xsdDate, "d"));
    }


    @Test
    void expression()
    {
        Column column = getColumn("(\"a\" || \"b\")::varchar");

        assertInstanceOf(ExpressionColumn.class, column);
        assertEquals("(\"a\" || \"b\")::varchar", column.toString());
        assertEquals(SqlType.VARCHAR, column.getType());
        assertTrue(column.canBeNull());

        assertEquals(SqlType.INT4, getColumn("(\"a\" + 1)::integer").getType());
        assertEquals(SqlType.RDFBOX, getColumn("(sparql.rdfbox_create_from_iri(\"a\"))::sparql.rdfbox").getType());
        assertEquals(SqlType.of("my_type"), getColumn("((\"a\"))::my_type").getType());

        assertThrows(IllegalArgumentException.class, () -> getColumn("(\"a\" || \"b\")"));
    }


    @Test
    void valueConstant()
    {
        Column column = getColumn("'it''s'::varchar");

        assertInstanceOf(ValueColumn.class, column);
        assertEquals("it's", ((ValueColumn) column).getValue());
        assertEquals(SqlType.VARCHAR, column.getType());
        assertEquals("'it''s'::varchar", column.toString());
        assertFalse(column.canBeNull());

        assertEquals("'1'::int4", getColumn("'1'::integer").toString());
    }


    @Test
    void nullConstant()
    {
        for(String specification : new String[] { "null::int4", "NULL::int4", "Null::integer" })
        {
            Column column = getColumn(specification);

            assertInstanceOf(NullColumn.class, column);
            assertEquals(SqlType.INT4, column.getType());
            assertEquals("NULL::int4", column.toString());
            assertTrue(column.canBeNull());
        }

        assertEquals(new NullColumn(SqlType.INT4), getColumn("null::int4"));
    }
}
