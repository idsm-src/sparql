package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
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
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The rows the join optimizer estimates a filter to keep of a table access whose column holds nulls: the access has
 * only the rows in which the column is not null, so what a comparison of the column keeps is a part of those rows,
 * however many comparisons of the column there are and whatever they are made into, whereas the planner of the
 * database, asked about them together, takes the nulls away with every comparison again; the comparisons of a column
 * without nulls are estimated as the planner estimates them.
 */
public class FilterSelectivityTest
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
     * Creates the {@code filter_selectivity_test} schema: a thousand nodes, 400 of which have a grade, each of the
     * grades 1 to 100 held by four of them, and all of which have a level, each of the levels 1 to 10 held by a hundred
     * of them, analysed; and the configuration mapping {@code ex:grade} and {@code ex:level} over them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema filter_selectivity_test");
            statement.execute("create table filter_selectivity_test.nodes (id integer primary key, grade integer, "
                    + "level integer not null)");
            statement.execute("""
                    insert into filter_selectivity_test.nodes select i, \
                    case when i % 5 < 2 then (i / 5) % 100 + 1 end, i % 10 + 1 \
                    from generate_series(1, 1000) as i""");
            statement.execute("analyze filter_selectivity_test.nodes");
        }

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("node", INT4, "http://example.org/n/"));

        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");
        DatabaseTable nodes = new DatabaseTable("filter_selectivity_test", "nodes");

        config.addQuadMapping(nodes, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:grade"),
                config.createLiteralMapping(xsdInt, "grade"));
        config.addQuadMapping(nodes, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:level"),
                config.createLiteralMapping(xsdInt, "level"));

        config.setEngineSettings(config.getEngineSettings().withJoinOptimizer(JoinOptimizerMode.SETTINGS));
    }


    /**
     * Returns the rows the join optimizer estimates for the filter of a query, which filters an access to the table.
     */
    private static double estimate(String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            SqlIntercode node = imcode.optimize(request, false).optimize(request, true).getChild();

            assertThat(node, instanceOf(SqlFilter.class));
            assertThat(((SqlFilter) node).getChild(), instanceOf(SqlTableAccess.class));

            return JoinOptimizer.estimateRows(request, node);
        }
    }


    @Test
    public void inequalities() throws Exception
    {
        // each inequality drops the four nodes of one grade out of the 400 that have a grade
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g != 1) }"), closeTo(396, 4));
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g != 1 && ?g != 2 && ?g != 3 && ?g != 4) }"),
                closeTo(384, 10));

        // as a variable that is none of several constants does
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g NOT IN (1, 2, 3, 4)) }"), closeTo(384, 10));
    }


    @Test
    public void bounds() throws Exception
    {
        // bounds that every grade meets keep all the nodes that have a grade
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g > 0) }"), closeTo(400, 4));
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g >= 1 && ?g <= 100) }"), closeTo(400, 4));

        // a half of the grades, and a narrow range of them
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g > 50) }"), closeTo(200, 4));
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g >= 2 && ?g < 4) }"), closeTo(8, 2));
    }


    @Test
    public void combinations() throws Exception
    {
        // the nodes of one grade or of two others
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(?g = 1 || ?g > 98) }"), closeTo(12, 3));

        // a negation keeps what its operand drops of the nodes that have a grade
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g FILTER(!(?g > 50 || ?g = 1)) }"), closeTo(196, 6));
    }


    @Test
    public void columnsWithoutNulls() throws Exception
    {
        // the planner counts no null of the levels, so it is asked about their comparisons together
        assertThat(estimate("SELECT ?x WHERE { ?x ex:level ?l FILTER(?l > 5 && ?l <= 8) }"), closeTo(300, 4));

        // a half of the levels of the nodes that have a grade other than one
        assertThat(estimate("SELECT ?x WHERE { ?x ex:grade ?g . ?x ex:level ?l FILTER(?l > 5 && ?g != 1) }"),
                closeTo(198, 6));
    }
}
