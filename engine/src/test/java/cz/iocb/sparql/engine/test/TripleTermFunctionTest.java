package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.Database;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;



/**
 * Queries over triple terms given by VALUES or built by expressions, run against an empty configuration: the functions
 * TRIPLE, SUBJECT, PREDICATE, OBJECT and isTRIPLE applied to triple terms kept in their own classes and to triple terms
 * stored in the box, the triple term shorthand of expressions, nested triple terms and sameTerm.
 */
public class TripleTermFunctionTest
{
    private static final String prefix = "PREFIX : <http://example.org/> ";
    private static final Iri a = iri("a");
    private static final Iri b = iri("b");
    private static final Iri c = iri("c");
    private static final Iri d = iri("d");
    private static final Iri e = iri("e");
    private static final Iri p = iri("p");
    private static final Iri x = iri("x");
    private static final TypedLiteral lit = new TypedLiteral("lit", xsdStringIri);
    private static final TypedLiteral one = new TypedLiteral("1", xsdIntegerIri);
    private static final TypedLiteral yes = new TypedLiteral("true", xsdBooleanIri);
    private static final TypedLiteral no = new TypedLiteral("false", xsdBooleanIri);

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Rows of the query run against the empty configuration.
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
     * IRI of the example namespace.
     *
     * @param name the local name
     * @return IRI of the example namespace
     */
    private static Iri iri(String name)
    {
        return new Iri("http://example.org/" + name);
    }


    /**
     * Triple term of the components.
     *
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     * @return triple term of the components
     */
    private static TripleTerm triple(RdfTerm subject, RdfTerm predicate, RdfTerm object)
    {
        return new TripleTerm(subject, predicate, object);
    }


    @Test
    @DisplayName("TRIPLE builds a triple term from an IRI subject, an IRI predicate and any object")
    void tripleFunction() throws Exception
    {
        assertThat(execute("""
                SELECT ?t WHERE {
                  VALUES (?s ?p ?o) { (:a :b :c) (:a :b 1) (:a :b "x"@en) (:a :b <<( :c :d :e )>>) }
                  BIND(TRIPLE(?s, ?p, ?o) AS ?t)
                }"""), containsInAnyOrder(row(triple(a, b, c)), row(triple(a, b, one)),
                row(triple(a, b, new LangStringLiteral("x", "en"))), row(triple(a, b, triple(c, d, e)))));
    }


    @Test
    @DisplayName("TRIPLE accepts a blank node subject")
    void blankNodeSubject() throws Exception
    {
        assertThat(execute("""
                SELECT ?i ?b WHERE {
                  BIND(TRIPLE(BNODE(), :p, :o) AS ?t)
                  BIND(isTRIPLE(?t) AS ?i)
                  BIND(isBLANK(SUBJECT(?t)) AS ?b)
                }"""), containsInAnyOrder(row(yes, yes)));
    }


    @Test
    @DisplayName("TRIPLE is an error for a literal or triple term subject, a non-IRI predicate or an unbound component")
    void tripleErrors() throws Exception
    {
        assertThat(execute("""
                SELECT ?t WHERE {
                  VALUES (?s ?p ?o) {
                    ("a" :b :c) (1 :b :c) (<<( :a :b :c )>> :b :c)
                    (:a "b" :c) (:a 1 :c) (:a <<( :a :b :c )>> :c)
                    (UNDEF :b :c) (:a UNDEF :c) (:a :b UNDEF)
                  }
                  BIND(TRIPLE(?s, ?p, ?o) AS ?t)
                }"""),
                containsInAnyOrder(row((RdfTerm) null), row((RdfTerm) null), row((RdfTerm) null), row((RdfTerm) null),
                        row((RdfTerm) null), row((RdfTerm) null), row((RdfTerm) null), row((RdfTerm) null),
                        row((RdfTerm) null)));
    }


    @Test
    @DisplayName("a triple term written in an expression is the TRIPLE call on its components")
    void shorthand() throws Exception
    {
        assertThat(execute("""
                SELECT ?t ?u WHERE {
                  VALUES (?s ?o) { (:a :c) (:a 1) ("lit" :c) }
                  BIND(<<( ?s :p ?o )>> AS ?t)
                  BIND(<<( :a :p <<( ?s :p ?o )>> )>> AS ?u)
                }"""), containsInAnyOrder(row(triple(a, p, c), triple(a, p, triple(a, p, c))),
                row(triple(a, p, one), triple(a, p, triple(a, p, one))), row(null, null)));
    }


    @Test
    @DisplayName("SUBJECT, PREDICATE, OBJECT and isTRIPLE on triple terms of their own classes and on other terms")
    void components() throws Exception
    {
        assertThat(execute("""
                SELECT ?t ?s ?p ?o ?i WHERE {
                  VALUES ?t { <<( :a :b :c )>> <<( :a :b 1 )>> :x "lit" }
                  BIND(SUBJECT(?t) AS ?s)
                  BIND(PREDICATE(?t) AS ?p)
                  BIND(OBJECT(?t) AS ?o)
                  BIND(isTRIPLE(?t) AS ?i)
                }"""), containsInAnyOrder(row(triple(a, b, c), a, b, c, yes), row(triple(a, b, one), a, b, one, yes),
                row(x, null, null, null, no), row(lit, null, null, null, no)));
    }


    @Test
    @DisplayName("the functions on triple terms stored in the box, and TRIPLE on boxed subjects and predicates")
    void boxedTerms() throws Exception
    {
        assertThat(execute("""
                SELECT ?b ?s ?p ?o ?i ?u ?v WHERE {
                  VALUES ?c { true false }
                  BIND(IF(?c, <<( :a :b 1 )>>, :x) AS ?b)
                  BIND(SUBJECT(?b) AS ?s)
                  BIND(PREDICATE(?b) AS ?p)
                  BIND(OBJECT(?b) AS ?o)
                  BIND(isTRIPLE(?b) AS ?i)
                  BIND(TRIPLE(IF(?c, :a, "lit"), :p, :c) AS ?u)
                  BIND(TRIPLE(:a, IF(?c, :p, 1), :c) AS ?v)
                }"""), containsInAnyOrder(row(triple(a, b, one), a, b, one, yes, triple(a, p, c), triple(a, p, c)),
                row(x, null, null, null, no, null, null)));
    }


    @Test
    @DisplayName("nested triple terms")
    void nested() throws Exception
    {
        assertThat(execute("""
                SELECT ?o ?s WHERE {
                  VALUES ?t { <<( :a :b <<( :c :d :e )>> )>> }
                  BIND(OBJECT(?t) AS ?o)
                  BIND(SUBJECT(OBJECT(?t)) AS ?s)
                }"""), containsInAnyOrder(row(triple(c, d, e), c)));
    }


    @Test
    @DisplayName("sameTerm compares triple terms by their components")
    void sameTerm() throws Exception
    {
        assertThat(execute("""
                SELECT ?t WHERE {
                  VALUES ?t { <<( :a :b :c )>> <<( :a :b 1 )>> :x }
                  FILTER(sameTerm(?t, <<( :a :b 1 )>>))
                }"""), containsInAnyOrder(row(triple(a, b, one))));
    }
}
