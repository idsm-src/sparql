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
import javax.sql.DataSource;
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
 * serial number: every row has a value of its own where the columns hold a key of the table, and otherwise the values
 * are those of the column that has the most of them, whichever of the columns the order of their names puts first.
 */
public class CompositeDistinctTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code composite_distinct_test} schema: the functions of the IRI class of the items, a thousand items
     * of five kinds and two hundred serial numbers, keyed by both, and two thousand references to four hundred items,
     * five to each, whose serial numbers determine their kinds, analysed; and the configuration mapping
     * {@code ex:label} and {@code ex:weight} over them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
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
            statement.execute("analyze composite_distinct_test.items, composite_distinct_test.refs");
        }

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new GenericUserIriClass("item", "composite_distinct_test", "item", List.of(INT4, INT4),
                "http://example\\.org/item/[0-9]+-[0-9]+"));

        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");
        DatabaseTable items = new DatabaseTable("composite_distinct_test", "items");
        DatabaseTable refs = new DatabaseTable("composite_distinct_test", "refs");

        config.addQuadMapping(items, graph, config.createIriMapping("item", "kind", "serial"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(refs, graph, config.createIriMapping("item", "kind", "serial"),
                config.createIriMapping("ex:weight"), config.createLiteralMapping(xsdInt, "weight"));

        config.setJoinOptimizer(true);
    }


    /**
     * Returns the distinct values of {@code ?x} the join optimizer estimates for a query reading one table.
     */
    private static double estimate(String query) throws Exception
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
        assertThat(estimate("SELECT ?x ?l WHERE { ?x ex:label ?l }"), closeTo(1000, 1));
    }


    @Test
    public void columnsWithoutAKey() throws Exception
    {
        // the 400 serial numbers of the items referred to, not the five kinds, whose column comes first by its name
        assertThat(estimate("SELECT ?x ?w WHERE { ?x ex:weight ?w }"), closeTo(400, 1));
    }
}
