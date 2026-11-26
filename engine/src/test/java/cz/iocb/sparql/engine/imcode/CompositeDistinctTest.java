package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.instanceOf;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.GenericUserIriClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The distinct values the join optimizer estimates for a variable held in two columns, an IRI made of a kind and a
 * serial number: every row has a value of its own where the columns hold a key of the table, the values are the
 * combinations the extended statistics of the table keep for the columns together where there are such statistics, in
 * either of their text forms, and otherwise those of the column that has the most of them, whichever of the columns the
 * order of their names puts first. A role that does not own the table reads the extended statistics through a function
 * declared {@code SECURITY DEFINER} only, as the database does not show them to it.
 */
public class CompositeDistinctTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The role that may read the tables but does not own them.
     */
    private static final String readerRole = "composite_distinct_reader";

    /**
     * Configuration of the owner of the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * Configuration of the role that does not own the tables, without a function reading the extended statistics.
     */
    private static SparqlDatabaseConfiguration reader = null;

    /**
     * Configuration of the role that does not own the tables, with the function reading the extended statistics.
     */
    private static SparqlDatabaseConfiguration readerWithFunction = null;

    /**
     * Configuration of the owner of the tables with a function giving fixed extended statistics in both text forms.
     */
    private static SparqlDatabaseConfiguration fixed = null;

    /**
     * Pool of the connections of the role that does not own the tables.
     */
    private static DataSource readerPool = null;


    /**
     * Creates the {@code composite_distinct_test} schema: the functions of the IRI class of the items; a thousand items
     * of five kinds and two hundred serial numbers, keyed by both; two thousand references to four hundred items, five
     * to each, whose serial numbers determine their kinds; three thousand scores of three hundred items of five kinds
     * and a hundred serial numbers, each serial number with three kinds, with extended statistics of the columns; all
     * analysed; the function reading the extended statistics for a role that does not own the tables, which is granted
     * to use the schemas of the extension and of the tables, to read the tables and to call the function, and a
     * function giving fixed extended statistics; and the configurations mapping {@code ex:label}, {@code ex:weight} and
     * {@code ex:score} over the tables.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource pool = Database.getPool();

        try(Connection connection = pool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema composite_distinct_test");
            statement.execute("""
                    create function composite_distinct_test.item(kind int4, serial int4) returns varchar \
                    language sql immutable \
                    as $$ select ('http://example.org/item/' || kind || '-' || serial)::varchar $$""");
            statement.execute("""
                    create function composite_distinct_test.item_inv1(iri varchar) returns int4 language sql \
                    immutable as $$ select split_part(substring(iri, 25), '-', 1)::int4 $$""");
            statement.execute("""
                    create function composite_distinct_test.item_inv2(iri varchar) returns int4 language sql \
                    immutable as $$ select split_part(substring(iri, 25), '-', 2)::int4 $$""");
            statement.execute("""
                    create table composite_distinct_test.items (kind int4 not null, serial int4 not null, \
                    label varchar not null, primary key (kind, serial))""");
            statement.execute("insert into composite_distinct_test.items select i % 5 + 1, i / 5, 'item ' || i "
                    + "from generate_series(0, 999) as i");
            statement.execute("""
                    create table composite_distinct_test.refs (kind int4 not null, serial int4 not null, \
                    weight int4 not null)""");
            statement.execute("insert into composite_distinct_test.refs select i / 5 % 5 + 1, i / 5, i "
                    + "from generate_series(0, 1999) as i");
            statement.execute("""
                    create table composite_distinct_test.scores (kind int4 not null, serial int4 not null, \
                    score int4 not null)""");
            statement.execute("""
                    insert into composite_distinct_test.scores select (i % 100 + i / 100 % 3) % 5 + 1, i % 100, i \
                    from generate_series(0, 2999) as i""");
            statement.execute("""
                    create statistics composite_distinct_test.scores_combinations (ndistinct) \
                    on kind, serial, score from composite_distinct_test.scores""");
            statement.execute("""
                    analyze composite_distinct_test.items, composite_distinct_test.refs, \
                    composite_distinct_test.scores""");
            statement.execute("""
                    create function composite_distinct_test.ndistinct(schema_name name, table_name name) \
                    returns setof text language sql stable security definer \
                    set search_path = pg_catalog, pg_temp \
                    as $$ select n_distinct::text from pg_stats_ext \
                    where schemaname = schema_name and tablename = table_name \
                    and has_table_privilege(session_user, format('%I.%I', schema_name, table_name), 'SELECT') $$""");
            statement.execute("""
                    create function composite_distinct_test.fixed_ndistinct(schema_name name, table_name name) \
                    returns setof text language sql stable \
                    as $$ select '{"1, 2": 800, "-1, 2": 999}' where table_name = 'refs' \
                    union all select '[{"attributes": [1, 2], "ndistinct": 450}]' where table_name = 'scores' $$""");
            statement.execute("do $$ begin create role " + readerRole + " nologin; "
                    + "exception when duplicate_object or unique_violation then null; end $$");
            statement.execute("grant usage on schema sparql, composite_distinct_test to " + readerRole);
            statement.execute("grant select on all tables in schema composite_distinct_test to " + readerRole);
            statement.execute("revoke execute on function composite_distinct_test.ndistinct(name, name) from public");
            statement.execute(
                    "grant execute on function composite_distinct_test.ndistinct(name, name) to " + readerRole);
        }

        PoolConfiguration shared = pool.getPoolProperties();
        PoolProperties properties = new PoolProperties();
        properties.setUrl(shared.getUrl());
        properties.setUsername(shared.getUsername());
        properties.setPassword(shared.getPassword());
        properties.setDriverClassName(shared.getDriverClassName());
        properties.setInitSQL("set role " + readerRole);

        readerPool = new DataSource(properties);

        DatabaseSchema schema = new DatabaseSchema(pool);

        config = configure(pool, schema);
        reader = configure(readerPool, schema);

        readerWithFunction = configure(readerPool, schema);
        readerWithFunction.setExtendedStatisticsFunction("composite_distinct_test", "ndistinct");

        fixed = configure(pool, schema);
        fixed.setExtendedStatisticsFunction("composite_distinct_test", "fixed_ndistinct");
    }


    /**
     * Closes the pool of the connections of the role that does not own the tables.
     */
    @AfterAll
    static void close()
    {
        if(readerPool != null)
            readerPool.close();
    }


    /**
     * Creates a configuration mapping the test tables, with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration configure(DataSource pool, DatabaseSchema schema) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, pool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new GenericUserIriClass("item", "composite_distinct_test", "item", List.of(INT4, INT4),
                "http://example\\.org/item/[0-9]+-[0-9]+"));

        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");
        DatabaseTable items = new DatabaseTable("composite_distinct_test", "items");
        DatabaseTable refs = new DatabaseTable("composite_distinct_test", "refs");
        DatabaseTable scores = new DatabaseTable("composite_distinct_test", "scores");

        config.addQuadMapping(items, graph, config.createIriMapping("item", "kind", "serial"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(refs, graph, config.createIriMapping("item", "kind", "serial"),
                config.createIriMapping("ex:weight"), config.createLiteralMapping(xsdInt, "weight"));
        config.addQuadMapping(scores, graph, config.createIriMapping("item", "kind", "serial"),
                config.createIriMapping("ex:score"), config.createLiteralMapping(xsdInt, "score"));

        config.setJoinOptimizer(true);

        return config;
    }


    /**
     * Returns the distinct values of {@code ?x} the join optimizer estimates for a query reading one table.
     */
    private static double estimate(SparqlDatabaseConfiguration config, String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            SqlIntercode node = imcode.optimize(request, false).optimize(request, true).getChild();

            assertThat(node, instanceOf(SqlTableAccess.class));

            return JoinOptimizer.estimateDistinct(request, node, new Variable("x"));
        }
    }


    @Test
    public void columnsHoldingAKey() throws Exception
    {
        // every item has a value of its own, although neither of its columns has more than 200 values
        assertThat(estimate(config, "SELECT ?x ?l WHERE { ?x ex:label ?l }"), closeTo(1000, 1));
    }


    @Test
    public void columnsWithoutAKey() throws Exception
    {
        // the 400 serial numbers of the items referred to, not the five kinds, whose column comes first by its name
        assertThat(estimate(config, "SELECT ?x ?w WHERE { ?x ex:weight ?w }"), closeTo(400, 1));
    }


    @Test
    public void extendedStatistics() throws Exception
    {
        // the 300 combinations the statistics keep for the columns together, of the three columns they are made for
        assertThat(estimate(config, "SELECT ?x ?s WHERE { ?x ex:score ?s }"), closeTo(300, 1));
    }


    @Test
    public void extendedStatisticsOfAReader() throws Exception
    {
        // the database does not show the statistics to a role that does not own the table, so the 100 serial numbers
        assertThat(estimate(reader, "SELECT ?x ?s WHERE { ?x ex:score ?s }"), closeTo(100, 1));

        // but the function declared by the owner gives them
        assertThat(estimate(readerWithFunction, "SELECT ?x ?s WHERE { ?x ex:score ?s }"), closeTo(300, 1));
    }


    @Test
    public void formsOfExtendedStatistics() throws Exception
    {
        // the form of PostgreSQL 18 and older, a combination holding an expression left out
        assertThat(estimate(fixed, "SELECT ?x ?w WHERE { ?x ex:weight ?w }"), closeTo(800, 1));

        // the form of PostgreSQL 19
        assertThat(estimate(fixed, "SELECT ?x ?s WHERE { ?x ex:score ?s }"), closeTo(450, 1));
    }
}
