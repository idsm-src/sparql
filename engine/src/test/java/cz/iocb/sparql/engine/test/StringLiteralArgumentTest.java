package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.stringLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
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
 * An argument of an extension function and a parameter of a procedure declared in the class of the string literals, the
 * argument kind "string literal" of SPARQL: the SQL function takes the string of a plain or language-tagged literal
 * (with or without a base direction) as a single {@code varchar}, and any other term supplies no value. The table
 * {@code item} holds the items 1 to 7 with a box ({@code :boxed}: {@code "a b"}, {@code "c"@en}, {@code "d e"@en--ltr},
 * {@code "f"@ar--rtl}, 42, an IRI and a double), and the items 1 and 2 also a string ({@code :plain}) and a
 * language-tagged string ({@code :tagged}); the function {@code ex:length} gives the length of the string, the
 * procedure {@code ex:words} its words separated by spaces.
 */
public class StringLiteralArgumentTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> PREFIX ex: <http://example.org/ex/> ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code string_literal_argument_test} schema with the table of the items, the function and the
     * procedure, and maps them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema string_literal_argument_test");
            statement.execute("""
                    create table string_literal_argument_test.item (id int4 primary key, \
                    boxed sparql.rdfbox not null, plain varchar collate "C", tagged varchar collate "C", \
                    lang varchar collate "C")""");
            statement.execute("""
                    insert into string_literal_argument_test.item values \
                    (1, '"a b"'::sparql.rdfbox, 'a b', 'a b', 'en'), \
                    (2, '"c"@en'::sparql.rdfbox, 'c', 'c', 'cs'), \
                    (3, '"d e"@en--ltr'::sparql.rdfbox, null, null, null), \
                    (4, '"f"@ar--rtl'::sparql.rdfbox, null, null, null), \
                    (5, '"42"^^<http://www.w3.org/2001/XMLSchema#integer>'::sparql.rdfbox, null, null, null), \
                    (6, '<http://example.org/x>'::sparql.rdfbox, null, null, null), \
                    (7, '"1.5E0"^^<http://www.w3.org/2001/XMLSchema#double>'::sparql.rdfbox, null, null, null)""");
            statement.execute("""
                    create function string_literal_argument_test.length_of(value varchar) returns int4 \
                    language sql immutable strict parallel safe as $$ select length(value) $$""");
            statement.execute("""
                    create function string_literal_argument_test.words(value varchar) returns setof varchar \
                    language sql immutable strict parallel safe as \
                    $$ select unnest(string_to_array(value, ' ')) $$""");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("string_literal_argument_test", "item");

        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":boxed"),
                config.createTermMapping(box, "boxed"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":plain"),
                config.createLiteralMapping(xsdString, "plain"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "id"), config.createIriMapping(":tagged"),
                config.createLiteralMapping(rdfLangString, "tagged", "lang"));

        config.addFunction(new FunctionDefinition("http://example.org/ex/length",
                new Function("string_literal_argument_test", "length_of"), xsdInt, List.of(stringLiteral), false,
                true));

        ProcedureDefinition words = new ProcedureDefinition("http://example.org/ex/words",
                new Function("string_literal_argument_test", "words"));
        words.addParameter(new ParameterDefinition("http://example.org/ex/text", stringLiteral, null));
        words.addResult(new ResultDefinition(xsdString));
        config.addProcedure(words);

        engine = new Engine(config);
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
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Row of the item and the term.
     *
     * @param item the number of the item
     * @param term the term, or null
     * @return row of the item and the term
     */
    private static List<RdfTerm> row(int item, RdfTerm term)
    {
        return Arrays.asList(new Iri("http://example.org/item/" + item), term);
    }


    /**
     * The xsd:int literal.
     *
     * @param value the value
     * @return the xsd:int literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
    }


    /**
     * The xsd:string literal.
     *
     * @param value the value
     * @return the xsd:string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a function takes the string of a string literal of any kind and nothing of the other terms")
    void functionArguments() throws Exception
    {
        assertThat(execute("SELECT ?s ?l WHERE { ?s :boxed ?v BIND(ex:length(?v) AS ?l) }"),
                containsInAnyOrder(row(1, integer(3)), row(2, integer(1)), row(3, integer(3)), row(4, integer(1)),
                        row(5, null), row(6, null), row(7, null)));

        // a column of strings or of language-tagged strings is passed as it is
        assertThat(execute("SELECT ?s ?l WHERE { ?s :plain ?v BIND(ex:length(?v) AS ?l) }"),
                containsInAnyOrder(row(1, integer(3)), row(2, integer(1))));
        assertThat(execute("SELECT ?s ?l WHERE { ?s :tagged ?v BIND(ex:length(?v) AS ?l) }"),
                containsInAnyOrder(row(1, integer(3)), row(2, integer(1))));

        assertThat(translate("SELECT ?l WHERE { ?s :plain ?v BIND(ex:length(?v) AS ?l) }"),
                containsString("\"length_of\"(\"plain\")"));
        assertThat(translate("SELECT ?l WHERE { ?s :tagged ?v BIND(ex:length(?v) AS ?l) }"),
                containsString("\"length_of\"(\"tagged\")"));

        assertThat(execute("SELECT ?l WHERE { BIND(ex:length(42) AS ?l) FILTER(BOUND(?l)) }"), empty());
    }


    @Test
    @DisplayName("a procedure takes the string of a string literal of any kind and gives nothing for the other terms")
    void procedureParameters() throws Exception
    {
        assertThat(execute("SELECT ?s ?w WHERE { ?s :boxed ?v . ?w ex:words [ ex:text ?v ] }"),
                containsInAnyOrder(row(1, string("a")), row(1, string("b")), row(2, string("c")), row(3, string("d")),
                        row(3, string("e")), row(4, string("f"))));
    }
}
