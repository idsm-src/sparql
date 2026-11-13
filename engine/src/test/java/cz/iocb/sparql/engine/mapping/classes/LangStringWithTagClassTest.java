package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.ColumnMap;



/**
 * Tests of {@link LangStringWithTagClass}: the tag of the class comes with a stored term and may hold any characters,
 * so the names of the columns of the class carry it as a part of a quoted identifier and the SQL converting the values
 * of the class as a quoted constant.
 */
public class LangStringWithTagClassTest
{
    private static final Column value = new TableColumn("v", VARCHAR);
    private static final Column tag = new TableColumn("t", VARCHAR);
    private static final Column boxed = new TableColumn("b", RDFBOX);


    @Test
    void createColumnsTest()
    {
        LangStringWithTagClass tagClass = LangStringWithTagClass.get("a\"b");

        assertEquals("\"x#lang-a\"\"b_par0\"",
                tagClass.createColumns(new ColumnMap(), new Variable("x")).get(0).toString());
    }


    @Test
    void toGeneralClassTest()
    {
        LangStringWithTagClass tagClass = LangStringWithTagClass.get("a'b");

        assertEquals("sparql.rdfbox_create_from_langstring(\"v\", 'a''b'::varchar)",
                tagClass.toGeneralClass(box, List.of(value), false).get(0).toString());

        assertEquals(List.of(value, constant("a'b", VARCHAR)),
                tagClass.toGeneralClass(rdfLangString, List.of(value), false));

        assertEquals("CASE WHEN \"v\" IS NOT NULL THEN 'a''b'::varchar END",
                tagClass.toGeneralClass(rdfLangString, List.of(value), true).get(1).toString());
    }


    @Test
    void fromGeneralClassTest()
    {
        LangStringWithTagClass tagClass = LangStringWithTagClass.get("a'b");

        assertEquals("sparql.rdfbox_get_langstring_value_of_lang(\"b\", 'a''b'::varchar)",
                tagClass.fromGeneralClass(box, List.of(boxed), false).get(0).toString());

        assertEquals("CASE WHEN \"t\" = 'a''b'::varchar THEN \"v\" END",
                tagClass.fromGeneralClass(rdfLangString, List.of(value, tag), false).get(0).toString());
    }
}
