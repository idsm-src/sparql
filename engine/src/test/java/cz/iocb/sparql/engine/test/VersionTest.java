package cz.iocb.sparql.engine.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.testing.Database;



/**
 * The version label of a prepared query: the VERSION declaration, the version given by the protocol (which takes
 * precedence, a difference being a warning) and the checks of the labels.
 */
public class VersionTest
{
    private static final String query = "SELECT * WHERE {}";

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Texts of the messages of the category.
     *
     * @param messages the messages
     * @param category the category
     * @return texts of the messages of the category
     */
    private static List<String> texts(List<TranslateMessage> messages, MessageCategory category)
    {
        return messages.stream().filter(m -> m.getCategory() == category).map(TranslateMessage::getMessage).toList();
    }


    @Test
    @DisplayName("the version of a query is the declared one unless the protocol gives one")
    void effectiveVersion() throws Exception
    {
        try(Request request = engine.getRequest())
        {
            assertNull(request.prepareQuery(query, null).getVersion());
            assertEquals("1.2", request.prepareQuery("VERSION \"1.2\" " + query, null).getVersion());
            assertEquals("1.2-basic", request.prepareQuery(query, null, "1.2-basic").getVersion());
            assertEquals("1.1", request.prepareQuery("VERSION \"1.1\" " + query, null, "1.1").getVersion());

            PreparedQuery prepared = request.prepareQuery("VERSION \"1.2\" " + query, null, "1.1");
            assertEquals("1.1", prepared.getVersion());
            assertEquals(List.of("version '1.1' of the protocol differs from the declared version '1.2'"),
                    texts(prepared.getMessages(), MessageCategory.WARNING));
        }
    }


    @Test
    @DisplayName("a version that is not a SPARQL version label is an error from the protocol and a warning in a query")
    void unknownLabels() throws Exception
    {
        try(Request request = engine.getRequest())
        {
            TranslateExceptions exception = assertThrows(TranslateExceptions.class,
                    () -> request.prepareQuery(query, null, "2.0"));
            assertEquals(List.of("version '2.0' is not supported"),
                    texts(exception.getMessages(), MessageCategory.ERROR));

            PreparedQuery prepared = request.prepareQuery("VERSION \"2.0\" " + query, null);
            assertEquals("2.0", prepared.getVersion());
            assertEquals(List.of("version label '2.0' is not recognized"),
                    texts(prepared.getMessages(), MessageCategory.WARNING));
        }
    }
}
