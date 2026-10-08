package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
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
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Literals mapped from SQL expressions ({@code (expression)::type} specifications): a table access evaluates such an
 * expression inside its own subquery and exposes it under an alias, so the conditions merged into the access, such as
 * the rows of a VALUES clause, have to refer to the expression rather than to the alias.
 */
public class ExpressionColumnTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the test table.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code expression_test} schema with the {@code compound} table and the configuration mapping its
     * label and identifier as expressions.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema expression_test");
            statement.execute("create table expression_test.compound (id int primary key, "
                    + "name varchar collate \"C\" not null)");
            statement.execute("insert into expression_test.compound values (1, 'one'), (2, 'two'), (3, 'three')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("compound", INT4, "http://example.org/compound/"));

        DatabaseTable compound = new DatabaseTable("expression_test", "compound");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(compound, graph, config.createIriMapping("compound", "id"),
                config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "('compound ' || name)::varchar"));

        config.addQuadMapping(compound, graph, config.createIriMapping("compound", "id"),
                config.createIriMapping("ex:identifier"), config.createLiteralMapping(xsdString, "(id)::varchar"));
    }


    /**
     * Runs the query and returns all rows.
     */
    private static List<List<RdfTerm>> execute(String query)
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
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the compound with the given id.
     */
    private static Iri compound(int id)
    {
        return new Iri("http://example.org/compound/" + id);
    }


    @Test
    @DisplayName("VALUES on a variable bound to an expression")
    void valuesOnExpression() throws Exception
    {
        List<List<RdfTerm>> result = execute("""
                SELECT ?c WHERE { VALUES ?l { "compound one" "compound three" "compound four" } ?c ex:label ?l }""");

        assertThat(result, containsInAnyOrder(row(compound(1)), row(compound(3))));
    }


    @Test
    @DisplayName("VALUES on a variable bound to an expression over the key")
    void valuesOnKeyExpression() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?c WHERE { VALUES ?i { \"1\" \"2\" \"7\" } ?c ex:identifier ?i }");

        assertThat(result, containsInAnyOrder(row(compound(1)), row(compound(2))));
    }


    @Test
    @DisplayName("a constant compared with an expression")
    void constantExpression() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?c WHERE { ?c ex:label \"compound two\" }");

        assertThat(result, containsInAnyOrder(row(compound(2))));
    }
}
