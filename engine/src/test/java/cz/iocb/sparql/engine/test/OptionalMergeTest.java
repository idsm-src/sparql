package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
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
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Checks the merge of OPTIONALs of the same solutions that read one row of a table by its key into a single OPTIONAL,
 * with the join optimizer off and on, and that OPTIONALs not reading one row are left apart.
 *
 * The table {@code protein} holds the proteins 1 to 4; the table {@code info}, keyed by the protein, the rows (1, null,
 * 5, a), (2, 10, null, null) and (3, 7, 8, null) of the mass, the length and the note of a protein; the table
 * {@code name}, keyed by the protein, the full names f1 and f3 of the proteins 1 and 3; the table {@code tag}, which
 * has no key, the rows (1, x, null) and (1, y, 2) of the tag and the rank of the protein 1.
 */
public class OptionalMergeTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The configurations with the join optimizer off and on.
     */
    private static List<SparqlDatabaseConfiguration> configs = null;


    /**
     * Creates the {@code optional_merge_test} schema with its tables and the configurations mapping them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema optional_merge_test");
            statement.execute("create table optional_merge_test.protein (id int primary key, name varchar not null)");
            statement.execute("""
                    insert into optional_merge_test.protein \
                    values (1, 'p1'), (2, 'p2'), (3, 'p3'), (4, 'p4')""");
            statement.execute("""
                    create table optional_merge_test.info (protein int primary key \
                    references optional_merge_test.protein(id), mass int, len int, note varchar)""");
            statement.execute("""
                    insert into optional_merge_test.info values (1, null, 5, 'a'), (2, 10, null, null), \
                    (3, 7, 8, null)""");
            statement.execute("""
                    create table optional_merge_test.name (protein int primary key \
                    references optional_merge_test.protein(id), full_name varchar not null)""");
            statement.execute("insert into optional_merge_test.name values (1, 'f1'), (3, 'f3')");
            statement.execute("""
                    create table optional_merge_test.tag (protein int not null \
                    references optional_merge_test.protein(id), tag varchar, rank int)""");
            statement.execute("insert into optional_merge_test.tag values (1, 'x', null), (1, 'y', 2)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);
        configs = new ArrayList<>();

        for(boolean optimizer : new boolean[] { false, true })
        {
            SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
            config.setJoinOptimizer(optimizer);

            config.addPrefix("ex", "http://example.org/");
            config.addIriClass(new IntegerUserIriClass("protein", INT4, "http://example.org/protein/"));

            DatabaseTable protein = new DatabaseTable("optional_merge_test", "protein");
            DatabaseTable info = new DatabaseTable("optional_merge_test", "info");
            DatabaseTable name = new DatabaseTable("optional_merge_test", "name");
            DatabaseTable tag = new DatabaseTable("optional_merge_test", "tag");
            ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

            config.addQuadMapping(protein, graph, config.createIriMapping("protein", "id"),
                    config.createIriMapping("ex:name"), config.createLiteralMapping(xsdString, "name"));
            config.addQuadMapping(info, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:mass"), config.createLiteralMapping(xsdInt, "mass"));
            config.addQuadMapping(info, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:len"), config.createLiteralMapping(xsdInt, "len"));
            config.addQuadMapping(info, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:note"), config.createLiteralMapping(xsdString, "note"));
            config.addQuadMapping(name, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:fullName"), config.createLiteralMapping(xsdString, "full_name"));
            config.addQuadMapping(tag, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:tag"), config.createLiteralMapping(xsdString, "tag"));
            config.addQuadMapping(tag, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:rank"), config.createLiteralMapping(xsdInt, "rank"));

            configs.add(config);
        }
    }


    /**
     * Translates the query into SQL.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Runs the query and returns all rows.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * How many times the SQL reads the table.
     *
     * @param sql the SQL
     * @param table the name of a table of the test schema
     * @return number of the accesses to the table in the SQL
     */
    private static int reads(String sql, String table)
    {
        String name = "\"optional_merge_test\".\"" + table + "\"";

        return (sql.length() - sql.replace(name, "").length()) / name.length();
    }


    /**
     * Expected row of a protein and terms.
     *
     * @param protein the protein id
     * @param terms the terms, null for an unbound value
     * @return row of the protein and the terms
     */
    private static List<RdfTerm> row(int protein, RdfTerm... terms)
    {
        List<RdfTerm> row = new ArrayList<>();
        row.add(new Iri("http://example.org/protein/" + protein));
        row.addAll(Arrays.asList(terms));
        return row;
    }


    /**
     * An integer literal.
     *
     * @param value the value
     * @return the literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
    }


    /**
     * A string literal.
     *
     * @param value the value
     * @return the literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("two OPTIONALs reading one row of a table by its key are merged")
    void twoOptionals() throws Exception
    {
        String query = "SELECT ?p ?m ?l WHERE { ?p ex:name ?n OPTIONAL { ?p ex:mass ?m } OPTIONAL { ?p ex:len ?l } }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1, null, integer(5)), row(2, integer(10), null),
                    row(3, integer(7), integer(8)), row(4, null, null)));
            assertThat(reads(translate(config, query), "info"), is(1));
        }
    }


    @Test
    @DisplayName("three OPTIONALs reading one row of a table by its key are merged")
    void threeOptionals() throws Exception
    {
        String query = """
                SELECT ?p ?m ?l ?t WHERE { ?p ex:name ?n OPTIONAL { ?p ex:mass ?m } \
                OPTIONAL { ?p ex:len ?l } OPTIONAL { ?p ex:note ?t } }""";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1, null, integer(5), string("a")),
                    row(2, integer(10), null, null), row(3, integer(7), integer(8), null), row(4, null, null, null)));
            assertThat(reads(translate(config, query), "info"), is(1));
        }
    }


    @Test
    @DisplayName("OPTIONALs reading one row are merged across an OPTIONAL of another table")
    void acrossOtherOptional() throws Exception
    {
        String query = """
                SELECT ?p ?m ?f ?l WHERE { ?p ex:name ?n OPTIONAL { ?p ex:mass ?m } \
                OPTIONAL { ?p ex:fullName ?f } OPTIONAL { ?p ex:len ?l } }""";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, null, string("f1"), integer(5)), row(2, integer(10), null, null),
                            row(3, integer(7), string("f3"), integer(8)), row(4, null, null, null)));
            assertThat(reads(translate(config, query), "info"), is(1));
        }
    }


    @Test
    @DisplayName("an OPTIONAL sharing the variable of another OPTIONAL is not merged into it")
    void sharedOptionalVariable() throws Exception
    {
        String query = "SELECT ?p ?x WHERE { ?p ex:name ?n OPTIONAL { ?p ex:mass ?x } OPTIONAL { ?p ex:len ?x } }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query), containsInAnyOrder(row(1, integer(5)), row(2, integer(10)),
                    row(3, integer(7)), row(4, (RdfTerm) null)));
    }


    @Test
    @DisplayName("an OPTIONAL with a condition on the variable of another OPTIONAL is not merged into it")
    void conditionOnOtherOptional() throws Exception
    {
        String query = """
                SELECT ?p ?m ?l WHERE { ?p ex:name ?n OPTIONAL { ?p ex:mass ?m } \
                OPTIONAL { ?p ex:len ?l FILTER(?l > ?m) } }""";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query), containsInAnyOrder(row(1, null, null), row(2, integer(10), null),
                    row(3, integer(7), integer(8)), row(4, null, null)));
    }


    @Test
    @DisplayName("OPTIONALs of a table without a key are not merged")
    void tableWithoutKey() throws Exception
    {
        String query = "SELECT ?p ?t ?r WHERE { ?p ex:name ?n OPTIONAL { ?p ex:tag ?t } OPTIONAL { ?p ex:rank ?r } }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query), containsInAnyOrder(row(1, string("x"), integer(2)),
                    row(1, string("y"), integer(2)), row(2, null, null), row(3, null, null), row(4, null, null)));
    }
}
