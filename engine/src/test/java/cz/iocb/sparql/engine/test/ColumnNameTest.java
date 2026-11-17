package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The names of the generated columns, made of the name of a variable and the label of a class: a name PostgreSQL would
 * truncate is shortened by the engine, reckoning the characters beyond ASCII by the bytes they may take, so that the
 * columns of two classes of one variable keep different names; the columns of the result do not take the names of the
 * columns the keys of the ordering are made of, as the keys would then order by the result; and two classes of one name
 * label their columns differently.
 *
 * The date {@code 2020-01-02+14:00}, held as a zoned date, starts before the date {@code 2020-01-01-14:00}, held as a
 * date with its offset, while it follows it by the text; the table {@code text} holds the strings {@code str} and
 * {@code zz}, mapped by ex:v as literals and by ex:w as IRIs of a user class named {@code string}, the name of the
 * class of the literals.
 */
public class ColumnNameTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code column_name_test} schema with the tables of the dates and of the strings and maps them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema column_name_test");
            statement.execute("create table column_name_test.zoned (s varchar not null, d sparql.zoneddate not null, "
                    + "l varchar not null)");
            statement.execute("insert into column_name_test.zoned values ('http://example.org/a', "
                    + "'2020-01-02+14:00'::sparql.zoneddate, '')");
            statement.execute("create table column_name_test.offset (s varchar not null, d date not null, "
                    + "z int not null, l varchar not null)");
            statement.execute("insert into column_name_test.offset values ('http://example.org/b', '2020-01-01', "
                    + "-50400, '')");
            statement.execute("create table column_name_test.text (s varchar not null, v varchar not null)");
            statement.execute("insert into column_name_test.text values ('http://example.org/c', 'str'), "
                    + "('http://example.org/d', 'zz')");
        }

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), true);
        config.addPrefix("ex", "http://example.org/");

        StringUserIriClass string = new StringUserIriClass(xsdString.getResourceName(), "http://example.org/s/");
        config.addIriClass(string);

        config.addQuadMapping(new DatabaseTable("column_name_test", "zoned"), null, config.createIriMapping(iri, "s"),
                config.createIriMapping("ex:d"), config.createLiteralMapping(genScalarDate, "d", "l"));
        config.addQuadMapping(new DatabaseTable("column_name_test", "offset"), null, config.createIriMapping(iri, "s"),
                config.createIriMapping("ex:d"), config.createLiteralMapping(genDate, "d", "z", "l"));
        config.addQuadMapping(new DatabaseTable("column_name_test", "text"), null, config.createIriMapping(iri, "s"),
                config.createIriMapping("ex:v"), config.createLiteralMapping(xsdString, "v"));
        config.addQuadMapping(new DatabaseTable("column_name_test", "text"), null, config.createIriMapping(iri, "s"),
                config.createIriMapping("ex:w"), config.createIriMapping(string, "v"));
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
        try(Request request = new Engine(config).getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * A row of a single term.
     *
     * @param term the term
     * @return the row
     */
    private static List<RdfTerm> row(RdfTerm term)
    {
        return List.of(term);
    }


    @Test
    @DisplayName("variables of characters beyond ASCII bound in two classes")
    void otherCharacters() throws Exception
    {
        for(String name : List.of(Character.toString(0x4E00).repeat(20), Character.toString(0x10D).repeat(40),
                Character.toString(0x1D400).repeat(20)))
            assertThat(execute("SELECT * WHERE { VALUES ?" + name + " { 'a'@en 'b'@cs } }"),
                    containsInAnyOrder(row(new LangStringLiteral("a", "en")), row(new LangStringLiteral("b", "cs"))));
    }


    @Test
    @DisplayName("the keys of the ordering are not the columns of the result")
    void ordering() throws Exception
    {
        for(String name : List.of("d", "v" + "x".repeat(59)))
            assertThat(execute("SELECT ?" + name + " WHERE { ?s ex:d ?" + name + " } ORDER BY ?" + name),
                    contains(row(new TypedLiteral("2020-01-02+14:00", xsdDateIri)),
                            row(new TypedLiteral("2020-01-01-14:00", xsdDateIri))));
    }


    @Test
    @DisplayName("two classes of one name")
    void classesOfOneName() throws Exception
    {
        List<RdfTerm> first = row(new Iri("http://example.org/s/str"));
        List<RdfTerm> second = row(new Iri("http://example.org/s/zz"));
        List<RdfTerm> third = row(new TypedLiteral("str", xsdStringIri));
        List<RdfTerm> fourth = row(new TypedLiteral("zz", xsdStringIri));

        assertThat(execute("SELECT ?o WHERE { { ?s ex:v ?o } UNION { ?s ex:w ?o } }"),
                containsInAnyOrder(first, second, third, fourth));

        assertThat(execute("SELECT ?o WHERE { { ?s ex:v ?o } UNION { ?s ex:w ?o } } ORDER BY ?o"),
                contains(first, second, third, fourth));

        assertThat(execute("SELECT DISTINCT ?o WHERE { { ?s ex:v ?o } UNION { ?s ex:w ?o } UNION { ?s ex:v ?o } }"),
                containsInAnyOrder(first, second, third, fourth));
    }
}
