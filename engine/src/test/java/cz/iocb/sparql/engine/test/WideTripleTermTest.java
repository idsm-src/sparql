package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The function TRIPLE over arguments of many classes: the combinations of the component classes must not each get a
 * triple term class of their own, neither in the first optimisation pass over the variables of an unevaluated SERVICE
 * call, bound in every class of a configuration of hundreds of IRI classes, nor in a query over VALUES of many IRI
 * classes, whose triple terms are held in the general classes of their components instead.
 */
public class WideTripleTermTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The SERVICE call binding {@code ?x}, {@code ?o} and {@code ?n}, never evaluated.
     */
    private static final String service = "SERVICE <http://example.org/remote/sparql> { ?x ex:p ?o ; ex:q ?n } ";

    /**
     * The predicate of the triple terms.
     */
    private static final Iri p = new Iri("http://example.org/p");

    /**
     * Engine over a configuration of two hundred IRI classes.
     */
    private static Engine engine = null;


    /**
     * Registers two hundred IRI classes of distinct prefixes.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, Database.getPool(), null, false);
        config.addPrefix("ex", "http://example.org/");

        for(int i = 0; i < 200; i++)
            config.addIriClass(new IntegerUserIriClass("ex:c" + i, INT4, "http://example.org/c" + i + "/"));

        engine = new Engine(config);
    }


    /**
     * Runs the first optimisation pass of a query binding the expression over the SERVICE variables, within thirty
     * seconds.
     *
     * @param expression the expression over {@code ?x}, {@code ?o} and {@code ?n}
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static void optimize(String expression) throws SQLException, TranslateExceptions, ServiceException
    {
        String query = prefix + "SELECT ?x ?r WHERE { " + service + "BIND(" + expression + " AS ?r) }";

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            try(Request request = engine.getRequest())
            {
                PreparedQuery prepared = request.prepareQuery(query, null);

                SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null,
                        List.of());

                SqlIntercode optimized = imcode.optimize(request, false);

                assertNotNull(optimized);
            }
        });
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declaration
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
     * The IRI of the given class, {@code http://example.org/cN/N}.
     *
     * @param index the index of the class
     * @return the IRI of the given class
     */
    private static Iri iri(int index)
    {
        return new Iri("http://example.org/c" + index + "/" + index);
    }


    /**
     * The subject of the row: the IRI of the class {@code index % subjects}, so that the rows cover {@code subjects}
     * subject classes.
     *
     * @param index the index of the row
     * @param subjects the number of subject classes
     * @return the subject of the row
     */
    private static Iri subject(int index, int subjects)
    {
        return iri(index % subjects);
    }


    /**
     * The object of the row: the IRI of the class {@code index}, so that the rows cover as many object classes as there
     * are rows.
     *
     * @param index the index of the row
     * @return the object of the row
     */
    private static Iri object(int index)
    {
        return iri(index);
    }


    /**
     * The clause {@code VALUES (?s ?o) { ... }} of the given number of rows over the given number of subject classes.
     *
     * @param rows the number of rows
     * @param subjects the number of subject classes
     * @return the VALUES clause
     */
    private static String values(int rows, int subjects)
    {
        return IntStream.range(0, rows)
                .mapToObj(i -> "(<" + subject(i, subjects).getValue() + "> <" + object(i).getValue() + ">)")
                .collect(Collectors.joining(" ", "VALUES (?s ?o) { ", " } "));
    }


    /**
     * Checks the triple terms built from the VALUES rows of the given number over the given number of subject classes:
     * their values, their components, their equality and identity with a constant triple term.
     *
     * @param rows the number of rows
     * @param subjects the number of subject classes
     * @throws Exception on failures
     */
    private static void check(int rows, int subjects) throws Exception
    {
        String pattern = values(rows, subjects) + "BIND(TRIPLE(?s, ex:p, ?o) AS ?t) ";

        List<List<RdfTerm>> terms = IntStream.range(0, rows)
                .mapToObj(i -> row(new TripleTerm(subject(i, subjects), p, object(i)))).toList();

        assertThat(execute("SELECT ?t WHERE { " + pattern + "}"), containsInAnyOrder(terms.toArray()));

        List<List<RdfTerm>> components = IntStream.range(0, rows).mapToObj(i -> row(subject(i, subjects), p, object(i)))
                .toList();

        assertThat(
                execute("SELECT ?a ?b ?c WHERE { " + pattern + "BIND(SUBJECT(?t) AS ?a) "
                        + "BIND(PREDICATE(?t) AS ?b) BIND(OBJECT(?t) AS ?c) }"),
                containsInAnyOrder(components.toArray()));

        String constant = "<<( <" + subject(5, subjects).getValue() + "> ex:p <" + object(5).getValue() + "> )>>";

        assertThat(execute("SELECT ?s WHERE { " + pattern + "FILTER(?t = " + constant + ") }"),
                contains(row(subject(5, subjects))));
        assertThat(execute("SELECT ?s WHERE { " + pattern + "FILTER(sameTerm(?t, " + constant + ")) }"),
                contains(row(subject(5, subjects))));
    }


    @Test
    @DisplayName("TRIPLE over SERVICE variables")
    void optimisation() throws Exception
    {
        optimize("TRIPLE(?x, ex:p, ?o)");
        optimize("TRIPLE(?x, ?n, ?o)");
        optimize("OBJECT(TRIPLE(?x, ex:p, ?o))");
    }


    @Test
    @DisplayName("triple terms with the subject taken in its general class")
    void generalSubject() throws Exception
    {
        // seventeen subject classes times seventeen object classes, the subjects generalised first
        check(17, 17);
    }


    @Test
    @DisplayName("triple terms with the object taken in its general class")
    void generalObject() throws Exception
    {
        // sixteen subject classes times seventeen object classes, the objects generalised
        check(17, 16);
    }
}
