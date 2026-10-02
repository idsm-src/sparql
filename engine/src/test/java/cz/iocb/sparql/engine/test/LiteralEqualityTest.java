package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The operators {@code =}, {@code !=} and IN on literals, run against an empty configuration, following the function
 * sameValue() of SPARQL 1.2: literals of two different handled datatypes are different values, literals of the same
 * handled datatype compare by value, and a literal of an unknown datatype or an ill-typed one equals the same term only
 * and is an error otherwise; the literals are compared in their own classes, through the box and inside triple terms.
 */
public class LiteralEqualityTest
{
    private static final String prefix = "PREFIX : <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
    private static final TypedLiteral yes = new TypedLiteral("true", xsdBooleanIri);
    private static final TypedLiteral no = new TypedLiteral("false", xsdBooleanIri);
    private static final List<RdfTerm> equal = row(yes, no);
    private static final List<RdfTerm> different = row(no, yes);
    private static final List<RdfTerm> error = row(null, null);

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Rows of the query run against the empty configuration.
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
     * Results of {@code ?l = ?r} and {@code ?l != ?r} for the given pairs of terms.
     *
     * @param pairs the pairs of terms, as rows of a VALUES clause
     * @return results of {@code ?l = ?r} and {@code ?l != ?r} for the pairs
     * @throws Exception on failures
     */
    private static List<List<RdfTerm>> compare(String pairs) throws Exception
    {
        return execute("SELECT ?e ?n WHERE { VALUES (?l ?r) { " + pairs + " } BIND(?l = ?r AS ?e) "
                + "BIND(?l != ?r AS ?n) }");
    }


    @Test
    @DisplayName("literals of two different handled datatypes are different values")
    void differentDatatypes() throws Exception
    {
        assertThat(compare("""
                (1 "1") (1 "1"^^xsd:string) (true 1) ("1" "1"@en) ("a"@en "a"@en--ltr) ("a"@en--ltr "a"@en--rtl)
                ("2024-01-01"^^xsd:date "2024-01-01T00:00:00Z"^^xsd:dateTime) ("PT1H"^^xsd:dayTimeDuration 1)"""),
                containsInAnyOrder(different, different, different, different, different, different, different,
                        different));
    }


    @Test
    @DisplayName("literals of the same handled datatype compare by value")
    void sameDatatype() throws Exception
    {
        assertThat(compare("""
                (1 1.0) ("a" "a") ("a" "b") ("a"@en "a"@en) ("a"@en "b"@en) ("a"@en--ltr "a"@en--ltr)
                ("a"@en--ltr "b"@en--ltr) ("a"@en--rtl "a"@fr--rtl)"""),
                containsInAnyOrder(equal, equal, different, equal, different, equal, different, different));
    }


    @Test
    @DisplayName("a literal of an unknown datatype equals the same term only and is an error otherwise")
    void unknownDatatypes() throws Exception
    {
        assertThat(compare("""
                ("x"^^:t "x"^^:t) ("x"^^:t "y"^^:t) ("x"^^:t "x"^^:u) ("x"^^:t "x") ("x"^^:t 1) ("x"^^:t "x"@en)
                ("x"^^:t :x) ("x"^^:t <<( :s :p "x"^^:t )>>)"""),
                containsInAnyOrder(equal, error, error, error, error, error, different, different));
    }


    @Test
    @DisplayName("an ill-typed literal equals the same term only and is an error otherwise")
    void illTyped() throws Exception
    {
        assertThat(compare("""
                ("abc"^^xsd:integer "abc"^^xsd:integer) ("abc"^^xsd:integer 1) ("abc"^^xsd:integer "abc")
                ("abc"^^xsd:integer "abc"^^xsd:decimal) ("1"^^xsd:boolean 1)"""),
                containsInAnyOrder(equal, error, error, error, different));
    }


    @Test
    @DisplayName("literals stored in the box follow the same rules")
    void boxed() throws Exception
    {
        assertThat(execute("""
                SELECT ?e ?n WHERE {
                  VALUES (?c ?d) { (true true) (true false) (false true) (false false) }
                  BIND(IF(?c, 1, "a"@en) AS ?l)
                  BIND(IF(?d, "1", "x"^^:t) AS ?r)
                  BIND(?l = ?r AS ?e)
                  BIND(?l != ?r AS ?n)
                }"""), containsInAnyOrder(different, error, different, error));

        assertThat(execute("""
                SELECT ?e ?n WHERE {
                  VALUES (?c ?r) { (true "1") (true 1) (false "a"@en) (false "b"@en) (false 1) }
                  BIND(IF(?c, 1, "a"@en) AS ?l)
                  BIND(?l = ?r AS ?e)
                  BIND(?l != ?r AS ?n)
                }"""), containsInAnyOrder(different, equal, equal, different, different));
    }


    @Test
    @DisplayName("IN and NOT IN are built from the equality, an error of a member being absorbed by a true one")
    void in() throws Exception
    {
        assertThat(execute("""
                SELECT ?a ?b ?c ?d ?e WHERE {
                  BIND(1 IN ("1", 1.0) AS ?a)
                  BIND(1 NOT IN ("1", "a"@en) AS ?b)
                  BIND(1 IN ("1", "x"^^:t) AS ?c)
                  BIND(1 NOT IN ("x"^^:t, 1) AS ?d)
                  BIND(1 NOT IN ("x"^^:t, "1") AS ?e)
                }"""), containsInAnyOrder(row(yes, yes, null, no, null)));
    }


    @Test
    @DisplayName("literals inside triple terms compare by the same =, a differing component overriding an error")
    void tripleTerms() throws Exception
    {
        assertThat(compare("""
                (<<( :s :p 1 )>> <<( :s :p "1" )>>) (<<( :s :p 1 )>> <<( :s :p 1.0 )>>)
                (<<( :s :p "a"@en )>> <<( :s :p "b"@en )>>) (<<( :s :p 1 )>> <<( :s :p "x"^^:t )>>)
                (<<( :s :p "x"^^:t )>> <<( :s :p "x"^^:t )>>)
                (<<( :s :p "NaN"^^xsd:double )>> <<( :s :p "NaN"^^xsd:double )>>)
                (<<( :a :p "x"^^:t )>> <<( :b :p "y"^^:t )>>) (<<( :s :p "x"^^:t )>> <<( :s :q "y"^^:t )>>)"""),
                containsInAnyOrder(different, equal, different, error, equal, different, different, different));

        assertThat(execute("""
                SELECT ?e ?n WHERE {
                  VALUES (?c ?r) {
                    (true <<( :s :p "NaN"^^xsd:double )>>) (true <<( :t :p "x"^^:t )>>) (true <<( :s :p "x"^^:t )>>)
                    (true <<( :s :p "NaN"^^xsd:float )>>) (false :x)
                  }
                  BIND(IF(?c, <<( :s :p "NaN"^^xsd:double )>>, :x) AS ?l)
                  BIND(?l = ?r AS ?e)
                  BIND(?l != ?r AS ?n)
                }"""), containsInAnyOrder(different, different, error, different, equal));
    }
}
