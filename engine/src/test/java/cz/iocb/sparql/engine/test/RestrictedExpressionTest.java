package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.testing.Database;



/**
 * Expressions whose value is needed only in some of its classes. A function takes its argument in the classes it has a
 * value for, so an IF in the argument passes that restriction to its branches, which then leave the other classes of
 * their values without columns. Whatever a branch is and whichever classes it takes, the function of the IF has to be
 * the IF of the function applied to the branches, in which nothing is restricted: every function (the wrapper) is
 * applied to an IF of every expression and of a constant, in both orders of the branches, the constant being of a kind
 * the function takes or of another one, and compared with the rewritten form. The expressions are evaluated over
 * constant solutions, over a union of them, whose bindings change when the query is optimised, so that the expressions
 * are built anew under the restriction, and over a table with a box column, whose class overlaps every class.
 */
public class RestrictedExpressionTest
{
    /**
     * A function applied to an IF, with the constant taken as the other branch of the IF.
     *
     * @param function the function, with {@code %s} in place of its argument
     * @param constant the constant
     */
    private record Wrapper(String function, String constant)
    {
        /**
         * The function applied to the argument.
         *
         * @param argument the argument
         * @return the function applied to the argument
         */
        private String apply(String argument)
        {
            return function.formatted(argument);
        }


        @Override
        public String toString()
        {
            return apply("IF(c, e, " + constant + ")");
        }
    }


    /**
     * Solutions the expressions are evaluated over.
     *
     * @param name the name of the shape, which names the tests
     * @param pattern the graph pattern giving the solutions
     * @param key the variable identifying a solution
     * @param condition the condition of the IF
     * @param expressions the expressions taken as a branch of the IF
     */
    private record Shape(String name, String pattern, String key, String condition, List<String> expressions)
    {
        @Override
        public String toString()
        {
            return name;
        }
    }


    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefixes = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX : <http://example.org/> ";

    /**
     * The functions, each with a constant of a kind it takes and with a constant of another kind.
     */
    private static final List<Wrapper> wrappers = List.of(new Wrapper("STRLEN(%s)", "'zz'"),
            new Wrapper("STRLEN(%s)", "1"), new Wrapper("UCASE(%s)", "'zz'"), new Wrapper("UCASE(%s)", "1"),
            new Wrapper("-(%s)", "1"), new Wrapper("-(%s)", "'zz'"), new Wrapper("(%s) + 1", "1"),
            new Wrapper("(%s) + 1", "'zz'"), new Wrapper("YEAR(%s)", "'2020-01-01T00:00:00Z'^^xsd:dateTime"),
            new Wrapper("YEAR(%s)", "1"), new Wrapper("!(%s)", "true"), new Wrapper("!(%s)", ":k"),
            new Wrapper("((%s) && true)", "true"), new Wrapper("((%s) && true)", ":k"),
            new Wrapper("DATATYPE(%s)", "1"), new Wrapper("DATATYPE(%s)", ":k"), new Wrapper("STR(%s)", "'zz'"),
            new Wrapper("isIRI(%s)", ":k"), new Wrapper("isIRI(%s)", "1"), new Wrapper("LANG(%s)", "'zz'@en"),
            new Wrapper("LANG(%s)", "1"), new Wrapper("((%s) = 'abc')", "'abc'"), new Wrapper("((%s) = 'abc')", "1"),
            new Wrapper("sameTerm(%s, 3)", "3"), new Wrapper("sameTerm(%s, 3)", "'zz'"),
            new Wrapper("xsd:integer(%s)", "'7'"), new Wrapper("xsd:integer(%s)", ":k"),
            new Wrapper("CONCAT(%s, 'x')", "'zz'"), new Wrapper("CONCAT(%s, 'x')", "1"),
            new Wrapper("REGEX(%s, 'a')", "'zz'"), new Wrapper("REGEX(%s, 'a')", "1"), new Wrapper("((%s) < 5)", "1"),
            new Wrapper("((%s) < 5)", "'zz'"));

    /**
     * The first rows of the constant solutions.
     */
    private static final String firstRows = """
            (1 true 'abc' 3 '2020-01-01T10:00:00Z'^^xsd:dateTime :x true 'x'@en 'q') \
            (2 false 'abc' 3 '2020-01-01T10:00:00Z'^^xsd:dateTime :x true 'x'@en 2) \
            (3 true '' 0 '2021-06-01'^^xsd:date :y false 'y'@en--ltr :z)""";

    /**
     * The other rows of the constant solutions.
     */
    private static final String otherRows = """
            (4 true 'b' 2.5 '2020-01-01T10:00:00.0Z'^^xsd:dateTime 'lit' 1 'z' 1.5e0) \
            (5 true UNDEF UNDEF UNDEF UNDEF UNDEF UNDEF UNDEF) \
            (6 UNDEF 'abc' 3 '2020-01-01T10:00:00Z'^^xsd:dateTime :x true 'x'@en 'q')""";

    /**
     * The expressions over the constant solutions: the variables, tests, logical operations, functions on strings, on
     * numerics and on dates, casts and functional forms.
     */
    private static final List<String> expressions = List.of("?s", "?n", "?d", "?i", "?b", "?l", "?m", "REGEX(?s, 'a')",
            "REGEX(?m, 'q')", "CONTAINS(?s, 'b')", "sameTerm(?s, 'abc')", "isIRI(?i)", "BOUND(?s)", "?m IN (2, 'q')",
            "?n = 3", "!?b", "!?m", "?b && true", "?m && true", "EXISTS { VALUES ?q { 1 } }", "STR(?m)", "LANG(?l)",
            "UCASE(?s)", "CONCAT(?s, ?l)", "STRDT('5', xsd:integer)", "STRLEN(?s)", "YEAR(?d)", "?n + 1", "-?m",
            "xsd:integer(?s)", "xsd:dateTime(?d)", "IRI(?s)", "DATATYPE(?m)", "COALESCE(?m, 1)", "COALESCE(?n, 'z')",
            "IF(?b, ?m, 1)", "TRIPLE(?i, :p, ?n)");

    /**
     * The expressions over the table: the box, the optional integer, the item and functions of them.
     */
    private static final List<String> boxExpressions = List.of("?v", "?i", "?s", "STR(?v)", "LANG(?v)", "DATATYPE(?v)",
            "-?v", "!?v", "?v && true", "?v + ?i", "?v = ?i", "COALESCE(?i, ?v)", "COALESCE(?v, ?i)",
            "IF(isLITERAL(?v), ?v, ?i)", "REGEX(?v, 'a')", "UCASE(?v)", "STRLEN(?v)", "isIRI(?v)", "sameTerm(?v, ?i)",
            "xsd:integer(?v)", "CONCAT(?v, 'x')", "?v IN (1, 'abc')");

    /**
     * The solutions the expressions are evaluated over.
     */
    private static final List<Shape> shapes = List.of(
            new Shape("values", "VALUES (?id ?c ?s ?n ?d ?i ?b ?l ?m) { " + firstRows + " " + otherRows + " }", "?id",
                    "?c", expressions),
            new Shape("union",
                    "{ VALUES (?id ?c ?s ?n ?d ?i ?b ?l ?m) { " + firstRows + " } } UNION "
                            + "{ VALUES (?id ?c ?s ?n ?d ?i ?b ?l ?m) { " + otherRows + " } }",
                    "?id", "?c", expressions),
            new Shape("box", "?s :box ?v OPTIONAL { ?s :int ?i }", "?s", "BOUND(?i)", boxExpressions));

    /**
     * The greatest number of expressions evaluated by one query, as the time a query takes grows faster than the number
     * of the expressions it projects.
     */
    private static final int batch = 30;

    /**
     * Engine of the configuration mapping the table.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code restricted_expression_test} schema with the table {@code item} of the items 1 to 7, their
     * boxes (an integer, strings, an IRI, a language-tagged string, a boolean, a double) and their optional integers,
     * and the configuration mapping the boxes by {@code :box} and the integers by {@code :int}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema restricted_expression_test");
            statement.execute("create table restricted_expression_test.item (id int primary key, "
                    + "value sparql.rdfbox not null, number int)");
            statement.execute("""
                    insert into restricted_expression_test.item values \
                    (1, '"1"^^<http://www.w3.org/2001/XMLSchema#integer>', 10), (2, '"abc"', null), \
                    (3, '<http://example.org/x>', 30), (4, '"abc"@en', null), \
                    (5, '"true"^^<http://www.w3.org/2001/XMLSchema#boolean>', 50), (6, '""', 60), \
                    (7, '"2.5E0"^^<http://www.w3.org/2001/XMLSchema#double>', null)""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("restricted_expression_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping(":box"),
                config.createTermMapping(box, "value"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping(":int"),
                config.createLiteralMapping(xsdInt, "number"));

        engine = new Engine(config);
    }


    /**
     * Every function combined with every shape of the solutions.
     *
     * @return the functions combined with the shapes
     */
    static Stream<Arguments> getCases()
    {
        return wrappers.stream().flatMap(w -> shapes.stream().map(s -> Arguments.of(w, s)));
    }


    /**
     * Rows of the values of the expressions over the solutions of the shape, ordered by the key of the solutions.
     *
     * @param shape the solutions
     * @param expressions the expressions
     * @return rows of the values of the expressions
     * @throws Exception if the query fails
     */
    private static List<List<RdfTerm>> evaluate(Shape shape, List<String> expressions) throws Exception
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        try(Request request = engine.getRequest())
        {
            for(int begin = 0; begin < expressions.size(); begin += batch)
            {
                StringBuilder query = new StringBuilder(prefixes).append("SELECT");

                for(int i = begin; i < Math.min(begin + batch, expressions.size()); i++)
                    query.append(" (").append(expressions.get(i)).append(" AS ?e").append(i).append(")");

                query.append(" WHERE { ").append(shape.pattern()).append(" } ORDER BY ").append(shape.key());

                try(Result result = request.execute(query.toString()))
                {
                    for(int row = 0; result.next(); row++)
                    {
                        if(begin == 0)
                            rows.add(new ArrayList<>());

                        rows.get(row).addAll(Arrays.asList(result.getRow()));
                    }
                }
            }
        }

        return rows;
    }


    @ParameterizedTest(name = "{0} over {1}")
    @MethodSource("getCases")
    @DisplayName("a function of an IF is the IF of the function of its branches")
    void restricted(Wrapper wrapper, Shape shape) throws Exception
    {
        List<String> restricted = new ArrayList<>();
        List<String> rewritten = new ArrayList<>();

        for(String expression : shape.expressions())
        {
            String function = wrapper.apply(expression);
            String constant = wrapper.apply(wrapper.constant());
            String condition = "IF(" + shape.condition() + ", ";

            restricted.add(wrapper.apply(condition + expression + ", " + wrapper.constant() + ")"));
            rewritten.add(condition + function + ", " + constant + ")");

            restricted.add(wrapper.apply(condition + wrapper.constant() + ", " + expression + ")"));
            rewritten.add(condition + constant + ", " + function + ")");
        }

        List<List<RdfTerm>> expected = evaluate(shape, rewritten);
        List<List<RdfTerm>> rows = evaluate(shape, restricted);

        assertEquals(expected.size(), rows.size());

        for(int i = 0; i < restricted.size(); i++)
        {
            int column = i;

            assertEquals(expected.stream().map(r -> r.get(column)).toList(),
                    rows.stream().map(r -> r.get(column)).toList(), restricted.get(i));
        }
    }
}
