package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
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
 * ORDER BY over xsd:dateTime literals of several timezone offsets, in the canonical forms and in others: the classes of
 * such literals hold the instant in a column of their own, so the literals are ordered by that column, without
 * converting them to scalar values row by row, and they come in the order of the box, which orders them by their
 * instants as well. The instants differ, as SPARQL leaves the order of equal values open.
 */
public class DateTimeOrderTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * The lexical forms in the order of their instants (the values without an offset taken as UTC).
     */
    private static final List<String> ordered = List.of("2019-06-01T00:00:00", "2020-01-01T10:00:00Z",
            "2020-01-01T10:30:00.0Z", "2020-01-01T12:00:00+01:00", "2020-01-01T13:30:00.50+02:00",
            "2020-01-01T05:00:00-07:00", " 2020-01-01T12:30:00+00:00 ", "2021-06-01T00:00:00.000");

    /**
     * The VALUES clause of the literals, in another order than theirs.
     */
    private static final String values = ordered.reversed().stream()
            .map(f -> "\"" + f + "\"^^<" + xsdDateTimeIri.getValue() + ">")
            .collect(Collectors.joining(" ", "VALUES ?v { ", " } "));

    /**
     * Engine over the configuration mapping the box column.
     */
    private static Engine engine = null;


    /**
     * Creates the table {@code item} holding the literals in a box column and maps it as {@code :term}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema date_time_order_test");
            statement.execute("create table date_time_order_test.item (term sparql.rdfbox not null)");

            for(String form : ordered)
                statement.execute("insert into date_time_order_test.item values ('\"" + form + "\"^^<"
                        + xsdDateTimeIri.getValue() + ">'::sparql.rdfbox)");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), true);

        config.addQuadMapping(new DatabaseTable("date_time_order_test", "item"), null,
                config.createIriMapping("<http://example.org/item>"),
                config.createIriMapping("<http://example.org/term>"), config.createTermMapping(box, "term"));

        engine = new Engine(config);
    }


    /**
     * Values of {@code ?v} in the order the query returns them.
     *
     * @param query the query, without the prefix declarations
     * @return values of {@code ?v} in the order the query returns them
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<RdfTerm> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<RdfTerm> terms = new ArrayList<>();

            while(result.next())
                terms.add(result.getRow()[0]);

            return terms;
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
     * The literals in the given order.
     *
     * @param forms the lexical forms
     * @return the literals in the given order
     */
    private static Object[] literals(List<String> forms)
    {
        return forms.stream().map(f -> new TypedLiteral(f, xsdDateTimeIri)).toArray();
    }


    @Test
    @DisplayName("date-times of several offsets are ordered by their instants as the box orders them")
    void order() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { " + values + "} ORDER BY ?v"), contains(literals(ordered)));
        assertThat(execute("SELECT ?v WHERE { " + values + "} ORDER BY DESC(?v)"),
                contains(literals(ordered.reversed())));

        assertThat(execute("SELECT ?v WHERE { ?s :term ?v } ORDER BY ?v"), contains(literals(ordered)));
        assertThat(execute("SELECT ?v WHERE { ?s :term ?v } ORDER BY DESC(?v)"),
                contains(literals(ordered.reversed())));
    }


    @Test
    @DisplayName("the instants are ordered as they are stored")
    void storedInstants() throws Exception
    {
        assertThat(translate("SELECT ?v WHERE { " + values + "} ORDER BY ?v"), not(containsString("zoneddatetime")));
    }
}
