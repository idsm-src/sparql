package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration.getColumn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;



/**
 * Parsing of column specifications by {@link SparqlDatabaseConfiguration#getColumn}.
 */
public class ColumnSpecificationTest
{
    @Test
    void tableColumn()
    {
        Column column = getColumn("id");

        assertInstanceOf(TableColumn.class, column);
        assertEquals("\"id\"", column.toString());
        assertTrue(column.canBeNull());
    }


    @Test
    void expression()
    {
        Column column = getColumn("(\"a\" || \"b\")");

        assertInstanceOf(ExpressionColumn.class, column);
        assertEquals("(\"a\" || \"b\")", column.toString());
    }


    @Test
    void valueConstant()
    {
        Column column = getColumn("'it''s'::varchar");

        assertInstanceOf(ValueColumn.class, column);
        assertEquals("it's", ((ValueColumn) column).getValue());
        assertEquals(SqlType.VARCHAR, ((ValueColumn) column).getType());
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
            assertEquals(SqlType.INT4, ((NullColumn) column).getType());
            assertEquals("NULL::int4", column.toString());
            assertTrue(column.canBeNull());
        }

        assertEquals(new NullColumn(SqlType.INT4), getColumn("null::int4"));
    }
}
