package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.testing.Database;



/**
 * REGEX of a text bound in several classes (an xsd:string, language-tagged strings with a base direction and without,
 * an integer): the classes that can hold a string literal and store it as a string are tested by one call, which reads
 * the string literal of every class, the classes stored in a box by another one, which reads the box, and a text of
 * another kind is an error. A computed pattern, which the second call would evaluate again, makes one call read the
 * string literals of all the classes.
 */
public class RegexFunctionTest
{
    /**
     * The texts of the queries.
     */
    private static final String texts = "VALUES ?v { \"abc\" \"abc\"@en \"abc\"@en--ltr \"xyz\"@de--rtl 1 } ";

    /**
     * Engine over a configuration mapping nothing.
     */
    private static Engine engine = null;


    /**
     * Creates the engine.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Rows of the query.
     *
     * @param query the query
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * The number of the calls of the regular expression functions of the extension in the SQL of the query.
     *
     * @param query the query
     * @return the number of the calls
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static int calls(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            String sql = imcode.optimize(request, false).optimize(request, true).translate(request);

            Matcher matcher = Pattern.compile("sparql\\.regex_").matcher(sql);
            int count = 0;

            while(matcher.find())
                count++;

            return count;
        }
    }


    @Test
    @DisplayName("the texts of several classes are tested by one call")
    void severalClasses() throws Exception
    {
        List<String> queries = List.of("SELECT ?v WHERE { " + texts + "FILTER(REGEX(?v, \"b\")) }",
                "SELECT ?v WHERE { " + texts + "BIND(\"B\" AS ?p) FILTER(REGEX(?v, ?p, \"i\")) }",
                "SELECT ?v WHERE { " + texts + "FILTER(REGEX(LCASE(?v), \"^a\")) }");

        for(String query : queries)
        {
            assertThat(query, execute(query),
                    containsInAnyOrder(List.of(new TypedLiteral("abc", xsdStringIri)),
                            List.of(new LangStringLiteral("abc", "en")),
                            List.of(new DirLangStringLiteral("abc", "en", Direction.LTR))));

            assertThat(query, calls(query), is(1));
        }
    }


    @Test
    @DisplayName("the texts stored in a box and those stored as strings are tested by a call each")
    void boxedClasses() throws Exception
    {
        // the queries merge a language-tagged string stored in a box with the strings into one class, so the text is
        // a variable of the column b of such strings and the column s of the xsd:strings, and the pattern a variable
        // of the column p
        ResourceClass boxedLangString = unionize(Set.of(rdfLangString), box);

        VariableBinding text = new VariableBinding(new Variable("v"), Map.of(boxedLangString,
                List.of(new TableColumn("b", RDFBOX)), xsdString, List.of(new TableColumn("s", VARCHAR))), true);
        VariableBinding pattern = new VariableBinding(new Variable("p"), xsdString,
                List.of(new TableColumn("p", VARCHAR)), false);

        try(Request request = engine.getRequest())
        {
            ClassRelations relations = request.getConfiguration();

            String plain = regex(request, SqlVariable.create(relations, text), SqlVariable.create(relations, pattern));

            assertThat(plain, containsString("sparql.regex_rdfbox(\"b\", \"p\")"));
            assertThat(plain, containsString("sparql.regex_string(\"s\", \"p\")"));
            assertThat(evaluate(plain), is(Arrays.asList(true, true, false, null)));

            // a computed pattern, which the second call would evaluate again, is evaluated by one call
            String computed = regex(request, SqlVariable.create(relations, text),
                    SqlBuiltinCall.create(request, "lcase", false, List.of(SqlVariable.create(relations, pattern))));

            assertThat(computed, not(containsString("sparql.regex_rdfbox")));
            assertThat(computed.split("sparql\\.regex_string", -1).length, is(2));
            assertThat(evaluate(computed), is(Arrays.asList(true, true, false, null)));
        }
    }


    /**
     * SQL code of REGEX of the text and the pattern.
     *
     * @param request the current request
     * @param text the text
     * @param pattern the pattern
     * @return SQL code of REGEX of the text and the pattern
     */
    private static String regex(Request request, SqlExpressionIntercode text, SqlExpressionIntercode pattern)
    {
        SqlExpressionIntercode regex = SqlBuiltinCall.create(request, "regex", false, List.of(text, pattern));

        return regex.get(request.getConfiguration(), xsdBoolean).get(0).getCode().getText();
    }


    /**
     * Values of the SQL code of a boolean over the rows of the columns {@code b}, {@code s} and {@code p}: the
     * language-tagged string {@code "abc"@en} in a box, the xsd:strings {@code "abd"} and {@code "xyz"}, and no text,
     * each with the pattern {@code b}.
     *
     * @param code the SQL code
     * @return values of the SQL code over the rows, null for NULL
     * @throws SQLException on database errors
     */
    private static List<Boolean> evaluate(String code) throws SQLException
    {
        String rows = """
                (VALUES ('"abc"@en'::sparql.rdfbox, NULL::varchar, 'b'::varchar), (NULL, 'abd', 'b'), \
                (NULL, 'xyz', 'b'), (NULL, NULL, 'b')) AS tab ("b", "s", "p")""";

        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT " + code + " FROM " + rows))
        {
            List<Boolean> values = new ArrayList<>();

            while(result.next())
                values.add(result.getObject(1) == null ? null : result.getBoolean(1));

            return values;
        }
    }
}
