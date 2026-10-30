package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The ordering operators over terms read from a box column, compared with constants, with other boxes and with the
 * results of arithmetic: a box may hold a term of any kind, so the operands are compared by the operators of the box,
 * whose comparison of terms of different kinds is an error, which the filter rejects.
 */
public class BoxComparisonTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The terms as the text forms of the box: numerics, strings, dates, booleans, an IRI and a literal of an unknown
     * datatype.
     */
    private static final List<String> boxed = List.of("\"1\"^^<%sinteger>", "\"2.5\"^^<%sdecimal>",
            "\"5\"^^<%sinteger>", "\"7.0E0\"^^<%sdouble>", "\"10\"^^<%sinteger>", "\"a\"", "\"b\"",
            "\"2020-01-01\"^^<%sdate>", "\"2021-06-01\"^^<%sdate>", "\"true\"^^<%sboolean>", "\"false\"^^<%sboolean>",
            "<http://example.org/x>", "\"x\"^^<http://example.org/unknown>");

    /**
     * Engine over the configuration mapping the box column.
     */
    private static Engine engine = null;


    /**
     * Creates the table {@code item} holding the terms in a box column and maps it as {@code :value}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema box_comparison_test");
            statement.execute(
                    "create table box_comparison_test.item (id int primary key, value sparql.rdfbox not null)");

            for(int i = 0; i < boxed.size(); i++)
                statement.execute("insert into box_comparison_test.item values (" + i + ", '"
                        + boxed.get(i).replace("%s", "http://www.w3.org/2001/XMLSchema#") + "'::sparql.rdfbox)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("box_comparison_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping(":value"),
                config.createTermMapping(box, "value"));

        engine = new Engine(config);
    }


    /**
     * Runs the query and returns all rows.
     *
     * @param query the query, without the prefix declarations
     * @return the rows of the result
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
     * Expected row.
     *
     * @param terms the terms of the row
     * @return the row
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * Literal of the datatype in the given lexical form.
     *
     * @param value the lexical form
     * @param datatype the datatype IRI
     * @return the literal
     */
    private static TypedLiteral literal(String value, Iri datatype)
    {
        return new TypedLiteral(value, datatype);
    }


    @Test
    @DisplayName("a box is compared with a constant of each kind")
    void constant() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { ?s :value ?v FILTER(?v > 4) }"),
                containsInAnyOrder(row(literal("5", xsdIntegerIri)), row(literal("7.0E0", xsdDoubleIri)),
                        row(literal("10", xsdIntegerIri))));

        assertThat(execute("SELECT ?v WHERE { ?s :value ?v FILTER(?v >= \"b\") }"),
                containsInAnyOrder(row(literal("b", xsdStringIri))));

        assertThat(execute("SELECT ?v WHERE { ?s :value ?v FILTER(?v < \"2021-01-01\"^^xsd:date) }"),
                containsInAnyOrder(row(literal("2020-01-01", xsdDateIri))));

        assertThat(execute("SELECT ?v WHERE { ?s :value ?v FILTER(?v < true) }"),
                containsInAnyOrder(row(literal("false", xsdBooleanIri))));
    }


    @Test
    @DisplayName("two boxes are compared with each other")
    void boxes() throws Exception
    {
        assertThat(execute("SELECT ?a ?b WHERE { ?s :value ?a . ?t :value ?b FILTER(?a < ?b && ?a >= 5) }"),
                containsInAnyOrder(row(literal("5", xsdIntegerIri), literal("7.0E0", xsdDoubleIri)),
                        row(literal("5", xsdIntegerIri), literal("10", xsdIntegerIri)),
                        row(literal("7.0E0", xsdDoubleIri), literal("10", xsdIntegerIri))));
    }


    @Test
    @DisplayName("a box is compared with the result of arithmetic over a box")
    void arithmetic() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { ?s :value ?v FILTER(?v > 12 - ?v) }"),
                containsInAnyOrder(row(literal("7.0E0", xsdDoubleIri)), row(literal("10", xsdIntegerIri))));
    }
}
