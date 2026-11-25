package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * A term written in the place of a result of a procedure call has to match the result: a literal or an IRI has to be
 * the result, a triple term is matched component by component, its variables bound to the components or, where they are
 * bound already, agreeing with them. A literal and a triple term failed with a {@code NullPointerException}.
 *
 * The procedure {@code ex:words} returns the words of a tag ({@code a} and {@code b} of {@code red}, {@code c} of
 * {@code blue}), {@code ex:terms} the terms of a tag: the triple terms {@code <<( item:1 ex:p "a" )>>} and
 * {@code <<( item:2 ex:p "b" )>>} and the IRI {@code item:3} of {@code red}, the triple terms
 * {@code <<( item:4 ex:p item:4 )>>} and {@code <<( item:5 ex:q <<( item:1 ex:p "a" )>> )>>} of {@code blue}, and
 * {@code ex:pairs} every pair of a word and a term of a tag as the results {@code ex:word} and {@code ex:term}.
 */
public class ProcedureResultTermTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> ";

    /**
     * Configuration mapping the procedures.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code procedure_result_term_test} schema with the tables of the words and the terms and the
     * functions of the procedures.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema procedure_result_term_test");
            statement.execute("create table procedure_result_term_test.word (x varchar, tag varchar)");
            statement.execute(
                    "insert into procedure_result_term_test.word values ('a', 'red'), ('b', 'red'), ('c', 'blue')");
            statement.execute("create table procedure_result_term_test.term (t sparql.rdfbox, tag varchar)");
            statement.execute("""
                    insert into procedure_result_term_test.term values \
                    ('<<( <http://example.org/item/1> <http://example.org/p> "a" )>>', 'red'), \
                    ('<<( <http://example.org/item/2> <http://example.org/p> "b" )>>', 'red'), \
                    ('<http://example.org/item/3>', 'red'), \
                    ('<<( <http://example.org/item/4> <http://example.org/p> \
                    <http://example.org/item/4> )>>', 'blue'), \
                    ('<<( <http://example.org/item/5> <http://example.org/q> \
                    <<( <http://example.org/item/1> <http://example.org/p> "a" )>> )>>', 'blue')""");
            statement.execute("""
                    create function procedure_result_term_test.words(query varchar) returns setof varchar \
                    language sql as $$ select x from procedure_result_term_test.word where tag = query $$""");
            statement.execute("""
                    create function procedure_result_term_test.terms(query varchar) returns setof sparql.rdfbox \
                    language sql as $$ select t from procedure_result_term_test.term where tag = query $$""");
            statement.execute("""
                    create function procedure_result_term_test.pairs(query varchar) \
                    returns table (x varchar, t sparql.rdfbox) language sql as \
                    $$ select w.x, t.t from procedure_result_term_test.word w, procedure_result_term_test.term t \
                    where w.tag = query and t.tag = query $$""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema, false);
        optimized = configuration(connectionPool, schema, true);
    }


    /**
     * Configuration mapping the procedures {@code ex:words} with a string result, {@code ex:terms} with a result of any
     * term and {@code ex:pairs} with the results {@code ex:word} and {@code ex:term}, each with the parameter
     * {@code ex:query} of the tag.
     *
     * @param connectionPool pool of the test database
     * @param schema schema of the test database
     * @param joinOptimizer whether the join optimizer is enabled
     * @return configuration mapping the procedures
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema,
            boolean joinOptimizer) throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");

        ProcedureDefinition words = new ProcedureDefinition("http://example.org/words",
                new Function("procedure_result_term_test", "words"));
        words.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        words.addResult(new ResultDefinition(xsdString));
        configuration.addProcedure(words);

        ProcedureDefinition terms = new ProcedureDefinition("http://example.org/terms",
                new Function("procedure_result_term_test", "terms"));
        terms.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        terms.addResult(new ResultDefinition(box));
        configuration.addProcedure(terms);

        ProcedureDefinition pairs = new ProcedureDefinition("http://example.org/pairs",
                new Function("procedure_result_term_test", "pairs"));
        pairs.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        pairs.addResult(new ResultDefinition("http://example.org/word", xsdString, "x"));
        pairs.addResult(new ResultDefinition("http://example.org/term", box, "t"));
        configuration.addProcedure(pairs);

        configuration.setJoinOptimizer(joinOptimizer);

        return configuration;
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled.
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
        List<List<RdfTerm>> rows = execute(config, query);

        assertThat(execute(optimized, query), containsInAnyOrder(rows.toArray()));

        return rows;
    }


    /**
     * Rows of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = new Engine(configuration).getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Row of the terms.
     *
     * @param terms the terms
     * @return row of the terms
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of an item.
     *
     * @param id identifier of the item
     * @return IRI of the item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    /**
     * Literal of xsd:string.
     *
     * @param value the value of the literal
     * @return literal of xsd:string
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a literal has to be the result")
    void literals() throws Exception
    {
        assertThat(execute("SELECT ?t WHERE { VALUES ?t { 'red' 'blue' } 'a' ex:words [ ex:query ?t ] }"),
                containsInAnyOrder(row(string("red"))));

        assertThat(execute("SELECT ?t WHERE { VALUES ?t { 'red' 'blue' } 'c' ex:words [ ex:query ?t ] }"),
                containsInAnyOrder(row(string("blue"))));

        assertThat(execute("SELECT ?t WHERE { VALUES ?t { 'red' 'blue' } 'a'@en ex:words [ ex:query ?t ] }"), empty());
    }


    @Test
    @DisplayName("an IRI has to be the result")
    void iris() throws Exception
    {
        assertThat(execute("SELECT ?t WHERE { VALUES ?t { 'red' 'blue' } item:3 ex:terms [ ex:query ?t ] }"),
                containsInAnyOrder(row(string("red"))));
    }


    @Test
    @DisplayName("a triple term is matched component by component")
    void tripleTerms() throws Exception
    {
        assertThat(
                execute("SELECT ?t WHERE { VALUES ?t { 'red' 'blue' } "
                        + "<<( item:1 ex:p 'a' )>> ex:terms [ ex:query ?t ] }"),
                containsInAnyOrder(row(string("red"))));

        assertThat(execute("SELECT ?t WHERE { VALUES ?t { 'red' 'blue' } "
                + "<<( item:1 ex:p 'a'@en )>> ex:terms [ ex:query ?t ] }"), empty());

        // the IRI item:3 is no triple term
        assertThat(execute("SELECT ?s ?o WHERE { <<( ?s ex:p ?o )>> ex:terms [ ex:query 'red' ] }"),
                containsInAnyOrder(row(item(1), string("a")), row(item(2), string("b"))));

        assertThat(
                execute("SELECT ?x WHERE { VALUES ?t { 'red' 'blue' } <<( ?x ex:p ?x )>> ex:terms [ ex:query ?t ] }"),
                containsInAnyOrder(row(item(4))));

        assertThat(execute("SELECT ?s ?o WHERE { <<( ?s ex:q <<( item:1 ?p ?o )>> )>> ex:terms [ ex:query 'blue' ] }"),
                containsInAnyOrder(row(item(5), string("a"))));
    }


    @Test
    @DisplayName("a variable of a triple term bound already has to agree with the component where it is bound")
    void boundVariables() throws Exception
    {
        assertThat(
                execute("SELECT ?s ?o WHERE { VALUES ?s { item:1 UNDEF } "
                        + "<<( ?s ex:p ?o )>> ex:terms [ ex:query 'red' ] }"),
                containsInAnyOrder(row(item(1), string("a")), row(item(1), string("a")), row(item(2), string("b"))));

        assertThat(
                execute("SELECT ?s ?o WHERE { VALUES ?o { 'a' UNDEF 'z' } "
                        + "<<( ?s ex:p ?o )>> ex:terms [ ex:query 'red' ] }"),
                containsInAnyOrder(row(item(1), string("a")), row(item(1), string("a")), row(item(2), string("b"))));
    }


    @Test
    @DisplayName("the terms written as the results of a call with several results")
    void severalResults() throws Exception
    {
        assertThat(
                execute("SELECT ?s ?w WHERE { [ ex:word ?w ; ex:term <<( ?s ex:p 'a' )>> ] "
                        + "ex:pairs [ ex:query 'red' ] }"),
                containsInAnyOrder(row(item(1), string("a")), row(item(1), string("b"))));

        assertThat(
                execute("SELECT ?t WHERE { VALUES ?t { 'red' 'blue' } "
                        + "[ ex:word 'c' ; ex:term ?u ] ex:pairs [ ex:query ?t ] }"),
                containsInAnyOrder(row(string("blue")), row(string("blue"))));

        // the word is bound by the call and the object of the term has to agree with it
        assertThat(
                execute("SELECT ?x WHERE { [ ex:word ?x ; ex:term <<( ?s ex:p ?x )>> ] ex:pairs [ ex:query 'red' ] }"),
                containsInAnyOrder(row(string("a")), row(string("b"))));
    }
}
