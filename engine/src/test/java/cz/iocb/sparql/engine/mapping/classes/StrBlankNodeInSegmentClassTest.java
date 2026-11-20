package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strScalarBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.testing.Database;



/**
 * The conversions of {@link StrBlankNodeInSegmentClass} from every general class of string blank nodes: a blank node of
 * the segment converted to the general class and back is the same node, and one of another segment is no node of the
 * class.
 */
public class StrBlankNodeInSegmentClassTest
{
    /**
     * The class of the test.
     */
    private static final StrBlankNodeInSegmentClass nodes = new StrBlankNodeInSegmentClass(54321);

    /**
     * A class of another segment.
     */
    private static final StrBlankNodeInSegmentClass others = new StrBlankNodeInSegmentClass(54322);


    /**
     * Value of the column evaluated by the database, null for NULL.
     *
     * @param column the column
     * @return value of the column evaluated by the database, null for NULL
     * @throws SQLException on database errors
     */
    private static String evaluate(Column column) throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT (" + column.getCode().getText() + ")::text"))
        {
            result.next();
            return result.getString(1);
        }
    }


    @Test
    @DisplayName("a node converted to a general class and back is the same node")
    void roundTrip() throws SQLException
    {
        List<Column> node = List.of(constant("a b", VARCHAR));

        for(ResourceClass general : List.of(box, strScalarBlankNode, strBlankNode))
        {
            List<Column> converted = nodes.toGeneralClass(general, node, false);
            assertEquals("a b", evaluate(nodes.fromGeneralClass(general, converted, false).get(0)), general.toString());

            // a node of another segment is no node of the class
            List<Column> other = others.toGeneralClass(general, node, false);
            assertNull(evaluate(nodes.fromGeneralClass(general, other, false).get(0)), general.toString());
        }
    }
}
