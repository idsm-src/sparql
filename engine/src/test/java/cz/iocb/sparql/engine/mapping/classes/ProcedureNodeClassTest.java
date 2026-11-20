package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.testing.Database;



/**
 * The label of a blank node held by columns ({@link ProcedureNodeClass}), which the node gets when it is converted to a
 * general class of string blank nodes or to the box, and the columns read back from it, in SQL and in Java. The class
 * holds a node by a text, a number and an optional text; the node of the test has a text with a comma and a percent
 * sign, the number 42 and no optional text.
 */
public class ProcedureNodeClassTest
{
    /**
     * The segment of the class.
     */
    private static final int segment = 12345;

    /**
     * The class of the test.
     */
    private static final ProcedureNodeClass nodes = new ProcedureNodeClass(segment, List.of(VARCHAR, INT4, VARCHAR),
            List.of(false, false, true));

    /**
     * Columns holding the node of the test.
     */
    private static final List<Column> node = List.of(constant("a,b%c", VARCHAR), constant("42", INT4),
            new NullColumn(VARCHAR));

    /**
     * The label of the node of the test.
     */
    private static final String label = "a%2Cb%25c,42,%";


    /**
     * Values of the columns evaluated by the database, null for NULL.
     *
     * @param columns the columns
     * @return values of the columns evaluated by the database, null for NULL
     * @throws SQLException on database errors
     */
    private static List<String> evaluate(List<Column> columns) throws SQLException
    {
        StringBuilder query = new StringBuilder("SELECT ");

        for(int i = 0; i < columns.size(); i++)
            query.append(i > 0 ? ", " : "").append("(").append(columns.get(i).getCode().getText()).append(")::text");

        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query.toString()))
        {
            result.next();

            List<String> values = new ArrayList<>();

            for(int i = 1; i <= columns.size(); i++)
                values.add(result.getString(i));

            return values;
        }
    }


    @Test
    @DisplayName("the label lists the escaped texts of the columns")
    void label() throws SQLException
    {
        assertEquals(List.of(label, Integer.toString(segment)),
                evaluate(nodes.toGeneralClass(strBlankNode, node, false)));

        // a node that is not bound has no label
        List<Column> unbound = List.of(new NullColumn(VARCHAR), new NullColumn(INT4), new NullColumn(VARCHAR));
        assertEquals(Arrays.asList(null, null), evaluate(nodes.toGeneralClass(strBlankNode, unbound, true)));
    }


    @Test
    @DisplayName("the columns are read back from every general class")
    void columns() throws SQLException
    {
        List<String> values = Arrays.asList("a,b%c", "42", null);

        for(ResourceClass general : List.of(box, strScalarBlankNode, strBlankNode))
        {
            List<Column> converted = nodes.toGeneralClass(general, node, false);
            assertEquals(values, evaluate(nodes.fromGeneralClass(general, converted, false)), general.toString());
        }

        // a blank node of another segment is no node of the class
        ProcedureNodeClass others = new ProcedureNodeClass(segment + 1, List.of(VARCHAR, INT4, VARCHAR),
                List.of(false, false, true));

        assertEquals(Arrays.asList(null, null, null),
                evaluate(nodes.fromGeneralClass(box, others.toGeneralClass(box, node, false), false)));
    }


    @Test
    @DisplayName("a blank node of the segment with a label of the columns is a node of the class")
    void constants()
    {
        assertTrue(nodes.match(null, new StrBlankNode(label, segment)));
        assertEquals(node, nodes.toColumns(new StrBlankNode(label, segment)));

        assertFalse(nodes.match(null, new StrBlankNode(label, segment + 1)));
        assertFalse(nodes.match(null, new StrBlankNode("a,42", segment)));
        assertFalse(nodes.match(null, new StrBlankNode("%,42,%", segment)));
    }
}
