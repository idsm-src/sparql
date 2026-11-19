package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * VALUES merged into a table access as conditions also when the access binds a variable in another class than the
 * VALUES: the terms of the rows are converted to the class of the access, a row whose term the class cannot hold is
 * left out, and a list too long for the conditions of the access stays a joined table. The table {@code item} holds the
 * items 1 to 5, each with its IRI in a column of all IRIs ({@code :self}) and its number in a box ({@code :value}), the
 * items being mapped by their ids in the IRI class {@code item}.
 */
public class ValuesClassMergeTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code values_class_merge_test} schema with the table of the items and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema values_class_merge_test");
            statement.execute("create table values_class_merge_test.item (id int4 primary key, "
                    + "iri varchar collate \"C\" not null, value sparql.rdfbox not null)");
            statement.execute("insert into values_class_merge_test.item select i, 'http://example.org/item/' || i, "
                    + "sparql.rdfbox_create_from_integer(i) from generate_series(1, 5) as i");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("values_class_merge_test", "item");

        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":self"),
                config.createIriMapping(iri, "iri"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":value"),
                config.createTermMapping(box, "value"));

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Translates the query into SQL.
     *
     * @param query the query, without the prefix declaration
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Row of the item.
     *
     * @param item the number of the item
     * @return row of the item
     */
    private static List<RdfTerm> row(int item)
    {
        return List.of(new Iri("http://example.org/item/" + item));
    }


    @Test
    @DisplayName("literals are converted to the boxes of the access")
    void literalsToBoxes() throws Exception
    {
        String query = "SELECT ?s WHERE { VALUES ?v { 1 2 7 } ?s :value ?v }";

        assertThat(execute(query), containsInAnyOrder(row(1), row(2)));
        assertThat(translate(query), not(containsString("cte")));
    }


    @Test
    @DisplayName("IRIs of several classes are converted to the class of all IRIs of the access")
    void irisOfSeveralClasses() throws Exception
    {
        String query = "SELECT ?s WHERE { VALUES ?i { <http://example.org/item/1> <http://example.org/item/3> "
                + "<http://example.com/x> } ?s :self ?i }";

        assertThat(execute(query), containsInAnyOrder(row(1), row(3)));
        assertThat(translate(query), not(containsString("cte")));
    }


    @Test
    @DisplayName("a row the class of the access cannot hold is left out")
    void unrepresentableRows() throws Exception
    {
        String query = "SELECT ?s WHERE { VALUES (?s ?i) { (<http://example.com/x> <http://example.org/item/2>) "
                + "(<http://example.org/item/4> <http://example.org/item/4>) } ?s :self ?i }";

        assertThat(execute(query), containsInAnyOrder(row(4)));
        assertThat(translate(query), not(containsString("example.com")));
    }


    @Test
    @DisplayName("a list too long for the conditions stays a joined table")
    void longList() throws Exception
    {
        String values = IntStream.rangeClosed(1, 40).mapToObj(Integer::toString).collect(Collectors.joining(" "));
        String query = "SELECT ?s WHERE { VALUES ?v { " + values + " } ?s :value ?v }";

        assertThat(execute(query), containsInAnyOrder(row(1), row(2), row(3), row(4), row(5)));
        assertThat(translate(query), containsString("cte"));
    }
}
