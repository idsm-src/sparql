package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
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
 * stored in the box, the triple term shorthand of expressions, nested triple terms, the comparisons {@code =},
 * {@code !=}, {@code <} and IN, sameTerm, ORDER BY, and the triple terms, reified triples and annotations of CONSTRUCT
 * templates.
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
    private static final Iri o = iri("o");
    private static final Iri q = iri("q");
    private static final Iri r = iri("r");
    private static final Iri s = iri("s");
    private static final Iri reifies = new Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies");
    private static final StrBlankNode bnode = new StrBlankNode("", 0);
    private static final TypedLiteral lit = new TypedLiteral("lit", xsdStringIri);
    private static final TypedLiteral one = new TypedLiteral("1", xsdIntegerIri);
    private static final TypedLiteral two = new TypedLiteral("2", xsdIntegerIri);
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


    /**
     * The rows with the labels of their blank nodes erased, inside triple terms too.
     *
     * @param rows the rows
     * @return the rows with the labels of their blank nodes erased
     */
    private static List<List<RdfTerm>> erase(List<List<RdfTerm>> rows)
    {
        return rows.stream().map(row -> row.stream().map(TripleTermFunctionTest::erase).toList()).toList();
    }


    /**
     * The term with the labels of its blank nodes erased, inside triple terms too.
     *
     * @param term the term
     * @return the term with the labels of its blank nodes erased
     */
    private static RdfTerm erase(RdfTerm term)
    {
        return switch(term)
        {
            case BlankNode _ -> bnode;
            case TripleTerm triple -> triple(erase(triple.getSubject()), erase(triple.getPredicate()),
                    erase(triple.getObject()));
            case null, default -> term;
        };
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


    @Test
    @DisplayName("= and != compare triple terms by the values of their components, sameTerm by their identity")
    void equality() throws Exception
    {
        assertThat(execute("""
                SELECT ?e ?n ?s WHERE {
                  VALUES (?l ?r) {
                    (<<( :a :b 123 )>> <<( :a :b 123.0 )>>)
                    (<<( :a :b 123 )>> <<( :a :b 123 )>>)
                    (<<( :a :q <<( :a :b 123 )>> )>> <<( :a :q <<( :a :b 123.0 )>> )>>)
                    (<<( :a :b 123 )>> <<( :c :d 123 )>>)
                    (<<( :a :b 9 )>> <<( :a :b 123 )>>)
                    (<<( :a :b "x"^^:t )>> <<( :a :b "y"^^:t )>>)
                    (<<( :a :b :c )>> :x)
                  }
                  BIND(?l = ?r AS ?e)
                  BIND(?l != ?r AS ?n)
                  BIND(sameTerm(?l, ?r) AS ?s)
                }"""), containsInAnyOrder(row(yes, no, no), row(yes, no, yes), row(yes, no, no), row(no, yes, no),
                row(no, yes, no), row(null, null, no), row(no, yes, no)));
    }


    @Test
    @DisplayName("boxed triple terms compare by value with = and by identity with sameTerm")
    void boxedEquality() throws Exception
    {
        assertThat(execute("""
                SELECT ?e ?s ?i WHERE {
                  VALUES ?c { true false }
                  BIND(IF(?c, <<( :a :b 1 )>>, :x) AS ?b)
                  BIND(?b = <<( :a :b 1.0 )>> AS ?e)
                  BIND(sameTerm(?b, <<( :a :b 1.0 )>>) AS ?s)
                  BIND(sameTerm(?b, <<( :a :b 1 )>>) AS ?i)
                }"""), containsInAnyOrder(row(yes, no, yes), row(no, no, no)));
    }


    @Test
    @DisplayName("ordering comparisons of triple terms are errors")
    void lessThan() throws Exception
    {
        assertThat(execute("""
                SELECT ?c WHERE {
                  VALUES (?l ?r) { (<<( :a :b 1 )>> <<( :a :b 2 )>>) }
                  BIND(?l < ?r AS ?c)
                }"""), containsInAnyOrder(row((RdfTerm) null)));
    }


    @Test
    @DisplayName("IN tests the membership of a triple term by value")
    void in() throws Exception
    {
        assertThat(execute("""
                SELECT ?t WHERE {
                  VALUES ?t { <<( :a :b 1 )>> <<( :a :b 2 )>> <<( :a :b 3 )>> :x }
                  FILTER(?t IN (<<( :a :b 1 )>>, <<( :a :b 2.0 )>>))
                }"""), containsInAnyOrder(row(triple(a, b, one)), row(triple(a, b, two))));
    }


    @Test
    @DisplayName("triple terms are ordered after the other terms and among themselves by their components")
    void ordering() throws Exception
    {
        String query = """
                SELECT ?v WHERE {
                  VALUES ?v { <<( :b :p 1 )>> "lit" <<( :a :p 2 )>> :x <<( :a :p 1 )>> }
                } ORDER BY %s(?v)""";

        assertThat(execute(query.formatted("ASC")),
                contains(row(x), row(lit), row(triple(a, p, one)), row(triple(a, p, two)), row(triple(b, p, one))));

        assertThat(execute(query.formatted("DESC")),
                contains(row(triple(b, p, one)), row(triple(a, p, two)), row(triple(a, p, one)), row(lit), row(x)));
    }


    @Test
    @DisplayName("a triple term of a CONSTRUCT template is built from its components, nested ones included")
    void constructTripleTerm() throws Exception
    {
        assertThat(execute("""
                CONSTRUCT { ?s :p <<( ?s :q ?o )>> } WHERE {
                  VALUES (?s ?o) { (:a 1) (:b "lit") }
                }"""), containsInAnyOrder(row(a, p, triple(a, q, one)), row(b, p, triple(b, q, lit))));

        assertThat(execute("""
                CONSTRUCT { :s :p <<( :a :q <<( ?s :r ?o )>> )>> } WHERE {
                  VALUES (?s ?o) { (:a 1) }
                }"""), containsInAnyOrder(row(s, p, triple(a, q, triple(a, r, one)))));
    }


    @Test
    @DisplayName("a reified triple or an annotation of a CONSTRUCT template produces the reifying triples")
    void constructReifiedTriple() throws Exception
    {
        List<List<RdfTerm>> annotated = execute("""
                CONSTRUCT { ?s :p ?o {| :by :me |} } WHERE {
                  VALUES (?s ?o) { (:a 1) }
                }""");

        assertThat(erase(annotated), containsInAnyOrder(row(a, p, one), row(bnode, reifies, triple(a, p, one)),
                row(bnode, iri("by"), iri("me"))));

        assertEquals(1,
                annotated.stream().map(row -> row.get(0)).filter(t -> t instanceof BlankNode).distinct().count());

        List<List<RdfTerm>> reified = execute("""
                CONSTRUCT { << :a :b ?o >> :src :x } WHERE {
                  VALUES ?o { 1 }
                }""");

        assertThat(erase(reified),
                containsInAnyOrder(row(bnode, reifies, triple(a, b, one)), row(bnode, iri("src"), x)));

        assertInstanceOf(BlankNode.class, reified.get(0).get(0));
        assertEquals(reified.get(0).get(0), reified.get(1).get(0));
    }


    @Test
    @DisplayName("a template produces no triple for a triple term with an unbound or invalid component or in the "
            + "subject position")
    void constructInvalidTripleTerms() throws Exception
    {
        assertThat(execute("""
                CONSTRUCT { :s :p <<( ?x :q :o )>> } WHERE {
                  VALUES ?x { :a "lit" UNDEF }
                }"""), containsInAnyOrder(row(s, p, triple(a, q, o))));

        assertThat(execute("""
                CONSTRUCT { <<( :a :b :c )>> :p :o . :s :p <<( <<( :a :b :c )>> :q :o )>> . :s :p :o } WHERE {
                }"""), containsInAnyOrder(row(s, p, o)));

        assertThat(execute("""
                CONSTRUCT { ?t :p :o . :s :q ?t } WHERE {
                  VALUES ?t { <<( :a :b 1 )>> :x }
                }"""), containsInAnyOrder(row(s, q, triple(a, b, one)), row(s, q, x), row(x, p, o)));

        assertThat(execute("""
                CONSTRUCT { ?b :p :o } WHERE {
                  VALUES ?c { true false }
                  BIND(IF(?c, <<( :a :b 1 )>>, :x) AS ?b)
                }"""), containsInAnyOrder(row(x, p, o)));
    }
}
