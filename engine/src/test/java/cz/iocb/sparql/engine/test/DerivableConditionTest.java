package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
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
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The conditions of a table access written without the ones the others imply: the equalities of an equivalence class as
 * those of its least member with the others, and no null test of a column that a strict comparison of the same disjunct
 * compares. The table {@code item} holds the items 1 to 4 with the numbers a, b and c (equal for the items 1 and 2) and
 * an optional kind (1, 2, none, 1), mapped by :a, :b and :c, and the kinds by :one (kind = 1, then a null test of the
 * kind), :other (kind != 1, then a null test) and :kind (a null test alone). A column compared strictly with a value
 * the rows are restricted to is not tested for null either.
 */
public class DerivableConditionTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code derivable_condition_test} schema with the table of the items and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema derivable_condition_test");
            statement.execute("create table derivable_condition_test.item (id int4 primary key, a int4 not null, "
                    + "b int4 not null, c int4 not null, kind int4)");
            statement.execute("insert into derivable_condition_test.item values (1, 5, 5, 5, 1), (2, 7, 7, 7, 2), "
                    + "(3, 1, 2, 3, null), (4, 1, 1, 2, 1)");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("derivable_condition_test", "item");
        TableColumn kind = new TableColumn("kind", INT4);

        for(String column : List.of("a", "b", "c"))
            config.addQuadMapping(table, null, config.createIriMapping("item", "id"),
                    config.createIriMapping(":" + column), config.createLiteralMapping(xsdInt, column));

        Condition one = new Condition();
        one.addAreEqual(kind, new ValueColumn("1", INT4));
        one.addIsNotNull(kind);

        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":one"),
                config.createLiteralMapping(xsdInt, "kind"), new Conditions(one));

        Condition other = new Condition();
        other.addAreNotEqual(kind, new ValueColumn("1", INT4));
        other.addIsNotNull(kind);

        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":other"),
                config.createLiteralMapping(xsdInt, "kind"), new Conditions(other));

        Condition any = new Condition();
        any.addIsNotNull(kind);

        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":kind"),
                config.createLiteralMapping(xsdInt, "kind"), new Conditions(any));

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declarations
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
     * @param query the query, without the prefix declarations
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
     * Row of the item and the xsd:int number.
     *
     * @param item the number of the item
     * @param value the number
     * @return row of the item and the number
     */
    private static List<RdfTerm> row(int item, int value)
    {
        return List.of(new Iri("http://example.org/item/" + item),
                new TypedLiteral(Integer.toString(value), xsdIntIri));
    }


    @Test
    @DisplayName("an equivalence class of columns is written by the equalities of its least member")
    void equivalenceClass() throws Exception
    {
        String query = "SELECT ?s ?v WHERE { ?s :a ?v . ?s :b ?v . ?s :c ?v }";

        assertThat(execute(query), containsInAnyOrder(row(1, 5), row(2, 7)));

        String sql = translate(query);

        assertThat(sql, containsString("\"a\" = \"b\""));
        assertThat(sql, containsString("\"a\" = \"c\""));
        assertThat(sql, not(containsString("\"b\" = \"c\"")));
        assertThat(sql, not(containsString("\"c\" = \"b\"")));
    }


    @Test
    @DisplayName("a column compared strictly is not tested for null")
    void comparedColumns() throws Exception
    {
        assertThat(execute("SELECT ?s ?k WHERE { ?s :one ?k }"), containsInAnyOrder(row(1, 1), row(4, 1)));
        assertThat(execute("SELECT ?s ?k WHERE { ?s :other ?k }"), containsInAnyOrder(row(2, 2)));

        assertThat(translate("SELECT ?s ?k WHERE { ?s :one ?k }"), not(containsString("IS NOT NULL")));
        assertThat(translate("SELECT ?s ?k WHERE { ?s :other ?k }"), not(containsString("IS NOT NULL")));
    }


    @Test
    @DisplayName("a column compared strictly with a value the rows are restricted to is not tested for null")
    void valueColumns() throws Exception
    {
        assertThat(execute("SELECT ?s ?k WHERE { ?s :kind ?k }"), containsInAnyOrder(row(1, 1), row(2, 2), row(4, 1)));
        assertThat(translate("SELECT ?s ?k WHERE { ?s :kind ?k }"), containsString("IS NOT NULL"));

        // the filter requires the term "1"^^xsd:int, which the access compares the kind with
        String query = "SELECT ?s ?k WHERE { ?s :kind ?k FILTER(?k = 1) }";

        assertThat(execute(query), containsInAnyOrder(row(1, 1), row(4, 1)));
        assertThat(translate(query), not(containsString("IS NOT NULL")));
    }
}
