package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;
import cz.iocb.sparql.testing.Database;



/**
 * The casts of xsd:dateTime and xsd:date literals, in canonical and other lexical forms, with and without a zone,
 * stored in the classes of every {@link TypedQuadTable} variant, to xsd:dateTime, xsd:date and xsd:string: the cast
 * value in the canonical form, whichever temporal class holds the literal, and no failure when the cast value is not
 * needed.
 */
public class TemporalCastTest
{
    /**
     * A literal with the lexical forms of its casts.
     *
     * @param literal the literal
     * @param dateTime the canonical form of the literal cast to xsd:dateTime
     * @param date the canonical form of the literal cast to xsd:date
     * @param string the literal cast to xsd:string
     */
    private record Cast(Node literal, String dateTime, String date, String string)
    {
    }


    /**
     * Predicate of the stored triples.
     */
    private static final String predicate = "http://example.org/p";

    /**
     * The literals, the item {@code s<i>} having the i-th one.
     */
    private static final List<Cast> casts = List.of(
            dateTime("2002-10-10T17:00:00Z", "2002-10-10T17:00:00Z", "2002-10-10Z"),
            dateTime("2002-10-10T12:00:00-05:00", "2002-10-10T12:00:00-05:00", "2002-10-10-05:00"),
            dateTime("2002-10-10T17:00:00", "2002-10-10T17:00:00", "2002-10-10"),
            dateTime("2002-10-10T17:00:00.000Z", "2002-10-10T17:00:00Z", "2002-10-10Z"),
            dateTime("2002-10-10T17:00:00+00:00", "2002-10-10T17:00:00Z", "2002-10-10Z"),
            dateTime("2002-10-10T17:00:00.0", "2002-10-10T17:00:00", "2002-10-10"),
            date("2002-10-10Z", "2002-10-10T00:00:00Z", "2002-10-10Z"),
            date("2002-10-10-05:00", "2002-10-10T00:00:00-05:00", "2002-10-10-05:00"),
            date("2002-10-10", "2002-10-10T00:00:00", "2002-10-10"),
            date("2002-10-10+00:00", "2002-10-10T00:00:00Z", "2002-10-10Z"));

    /**
     * Pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Catalog of the test database.
     */
    private static DatabaseSchema schema = null;


    /**
     * Reads the catalog of the test database.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();
        schema = new DatabaseSchema(connectionPool);
    }


    /**
     * An xsd:dateTime literal with its casts.
     *
     * @param form the lexical form of the literal
     * @param dateTime the canonical form of the literal, which is its cast to xsd:dateTime and to xsd:string
     * @param date the canonical form of the literal cast to xsd:date
     * @return the literal with its casts
     */
    private static Cast dateTime(String form, String dateTime, String date)
    {
        return new Cast(NodeFactory.createLiteralDT(form, XSDDatatype.XSDdateTime), dateTime, date, dateTime);
    }


    /**
     * An xsd:date literal with its casts.
     *
     * @param form the lexical form of the literal
     * @param dateTime the canonical form of the literal cast to xsd:dateTime
     * @param date the canonical form of the literal, which is its cast to xsd:date and to xsd:string
     * @return the literal with its casts
     */
    private static Cast date(String form, String dateTime, String date)
    {
        return new Cast(NodeFactory.createLiteralDT(form, XSDDatatype.XSDdate), dateTime, date, date);
    }


    /**
     * The variants of {@link TypedQuadTable}.
     *
     * @return the variants
     */
    static List<TypedQuadTable.Variant> getVariants()
    {
        return TypedQuadTable.getVariants();
    }


    /**
     * The item having the i-th literal.
     *
     * @param index index of the literal
     * @return the item
     */
    private static Iri item(int index)
    {
        return new Iri("http://example.org/s" + index);
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
     * Rows of the query over the literals stored in the classes of the variant; in the query, {@code $p} stands for the
     * predicate of the stored triples.
     *
     * @param variant the variant
     * @param query the query
     * @return rows of the query
     * @throws Exception if the data cannot be stored or the query fails
     */
    private static List<List<RdfTerm>> execute(TypedQuadTable.Variant variant, String query) throws Exception
    {
        List<Quad> quads = new ArrayList<>();

        for(int i = 0; i < casts.size(); i++)
            quads.add(new Quad(null, NodeFactory.createURI(item(i).getValue()), NodeFactory.createURI(predicate),
                    casts.get(i).literal()));

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        try(Request request = new Engine(config).getRequest())
        {
            TypedQuadTable.store(config, request, variant, quads);

            List<List<RdfTerm>> rows = new ArrayList<>();

            try(Result result = request.execute(
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + query.replace("$p", "<" + predicate + ">")))
            {
                while(result.next())
                    rows.add(Arrays.asList(result.getRow()));
            }

            return rows;
        }
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getVariants")
    @DisplayName("a cast gives the canonical form of the cast value")
    void cast(TypedQuadTable.Variant variant) throws Exception
    {
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int i = 0; i < casts.size(); i++)
            expected.add(row(item(i), new TypedLiteral(casts.get(i).dateTime(), xsdDateTimeIri),
                    new TypedLiteral(casts.get(i).date(), xsdDateIri),
                    new TypedLiteral(casts.get(i).string(), xsdStringIri)));

        assertThat(execute(variant, "SELECT ?x (xsd:dateTime(?o) AS ?t) (xsd:date(?o) AS ?d) (xsd:string(?o) AS ?s) "
                + "WHERE { ?x $p ?o }"), containsInAnyOrder(expected.toArray()));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getVariants")
    @DisplayName("a cast value is compared as a term of its own")
    void castInFilter(TypedQuadTable.Variant variant) throws Exception
    {
        assertThat(
                execute(variant,
                        "SELECT ?x WHERE { ?x $p ?o "
                                + "FILTER(sameTerm(xsd:dateTime(?o), \"2002-10-10T17:00:00Z\"^^xsd:dateTime)) }"),
                containsInAnyOrder(row(item(0)), row(item(3)), row(item(4))));

        assertThat(
                execute(variant,
                        "SELECT ?x WHERE { ?x $p ?o FILTER(sameTerm(xsd:date(?o), \"2002-10-10Z\"^^xsd:date)) }"),
                containsInAnyOrder(row(item(0)), row(item(3)), row(item(4)), row(item(6)), row(item(9))));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getVariants")
    @DisplayName("a cast whose value is not needed does not fail")
    void unusedCast(TypedQuadTable.Variant variant) throws Exception
    {
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int i = 0; i < casts.size(); i++)
            expected.add(row(item(i)));

        assertThat(
                execute(variant, "SELECT ?x WHERE { ?x $p ?o BIND(xsd:dateTime(?o) AS ?t) BIND(xsd:date(?o) AS ?d) }"),
                containsInAnyOrder(expected.toArray()));
    }
}
