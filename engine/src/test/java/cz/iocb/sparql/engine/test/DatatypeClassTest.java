package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
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
 * The class of the result of DATATYPE for a value that may be unbound or is bound in several classes of literals of one
 * datatype each: when the configuration detects the IRIs of all the datatypes as IRIs of one class, the result has that
 * class, so that a join with the IRIs of the other classes is no comparison. The table {@code item} holds the items 1
 * to 3 with an xsd:int {@code :n}, an optional xsd:int {@code :m} (items 1 and 3), an optional xsd:long {@code :k}
 * (item 1) and an optional xsd:string {@code :l} (item 3); the first configuration has only the IRI class of the items,
 * the second one also the classes {@code numbers} of the IRIs of xsd:int and xsd:long and {@code strings} of the IRI of
 * xsd:string.
 */
public class DatatypeClassTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Namespace of the XSD datatypes.
     */
    private static final String xsd = "http://www.w3.org/2001/XMLSchema#";

    /**
     * Engine over the configuration with the IRI class of the items only.
     */
    private static Engine engine = null;

    /**
     * Engine over the configuration with the IRI classes of the datatypes too.
     */
    private static Engine datatypeEngine = null;


    /**
     * Creates the {@code datatype_class_test} schema with the table of the items, and the engines over the two
     * configurations mapping it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema datatype_class_test");
            statement.execute("create table datatype_class_test.item (id int4 primary key, n int4 not null, m int4, "
                    + "k int8, l varchar collate \"C\")");
            statement.execute("insert into datatype_class_test.item values (1, 1, 10, 100, null), "
                    + "(2, 2, null, null, null), (3, 3, 30, null, 'c')");
        }

        engine = new Engine(configuration(connectionPool, false));
        datatypeEngine = new Engine(configuration(connectionPool, true));
    }


    /**
     * Configuration mapping the table.
     *
     * @param connectionPool the connection pool
     * @param datatypes whether the configuration has the IRI classes of the datatypes
     * @return configuration mapping the table
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, boolean datatypes)
            throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        if(datatypes)
        {
            config.addIriClass(new StringUserIriClass("numbers", xsd, "int|long"));
            config.addIriClass(new StringUserIriClass("strings", xsd, "string"));
        }

        DatabaseTable table = new DatabaseTable("datatype_class_test", "item");

        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":n"),
                config.createLiteralMapping(xsdInt, "n"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":m"),
                config.createLiteralMapping(xsdInt, "m"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":k"),
                config.createLiteralMapping(xsdLong, "k"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":l"),
                config.createLiteralMapping(xsdString, "l"));

        return config;
    }


    /**
     * Rows of the query.
     *
     * @param engine the engine
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(Engine engine, String query)
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
     * @param engine the engine
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(Engine engine, String query)
            throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Row of the item and the datatype.
     *
     * @param item the number of the item
     * @param datatype the local name of the XSD datatype, or null
     * @return row of the item and the datatype
     */
    private static List<RdfTerm> row(int item, String datatype)
    {
        return Arrays.asList(new Iri("http://example.org/item/" + item),
                datatype == null ? null : new Iri(xsd + datatype));
    }


    @Test
    @DisplayName("the datatype of an unbound value is in the class of the unsupported IRIs")
    void unsupportedIri() throws Exception
    {
        String query = "SELECT ?s ?d WHERE { ?s :n ?n OPTIONAL { ?s :m ?m } BIND(DATATYPE(?m) AS ?d) }";

        assertThat(execute(engine, query), containsInAnyOrder(row(1, "int"), row(2, null), row(3, "int")));

        // the join with the items compares no item with the datatype
        String join = "SELECT ?s ?d ?p WHERE { ?s :n ?n OPTIONAL { ?s :m ?m } BIND(DATATYPE(?m) AS ?d) ?d ?p ?o }";

        assertThat(execute(engine, join).size(), is(7));
        assertThat(translate(engine, join), containsString("\"d#unsupported_par0\" IS NULL"));
        assertThat(translate(engine, join), not(containsString(") OR (")));
    }


    @Test
    @DisplayName("the datatypes of the values bound in several classes are in the class detected for them all")
    void detectedClass() throws Exception
    {
        String query = "SELECT ?s ?d WHERE { ?s :n ?n OPTIONAL { { ?s :m ?v } UNION { ?s :k ?v } } "
                + "BIND(DATATYPE(?v) AS ?d) }";

        assertThat(translate(datatypeEngine, query), containsString("\"d#numbers_par0\""));
        assertThat(execute(datatypeEngine, query),
                containsInAnyOrder(row(1, "int"), row(1, "long"), row(2, null), row(3, "int")));

        // the datatypes in two classes keep the class of all IRIs
        String classes = "SELECT ?s ?d WHERE { ?s :n ?n OPTIONAL { { ?s :m ?v } UNION { ?s :l ?v } } "
                + "BIND(DATATYPE(?v) AS ?d) FILTER(?d != xsd:int) }";

        assertThat(translate(datatypeEngine, classes), containsString("\"d#iri_par0\""));
        assertThat(execute(datatypeEngine, classes), containsInAnyOrder(row(3, "string")));
    }
}
