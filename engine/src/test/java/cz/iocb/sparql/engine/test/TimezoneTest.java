package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationIri;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * TIMEZONE of xsd:dateTime and xsd:date literals with and without a zone, stored in the classes of every
 * {@link TypedQuadTable} variant: the zone as a duration, and unbound for a value without a zone, which BOUND sees too.
 */
public class TimezoneTest
{
    private static final String predicate = "http://example.org/p";

    private static final List<Node> literals = List.of(
            NodeFactory.createLiteralDT("2002-10-10T17:00:00Z", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10T12:00:00-05:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10T17:00:00.000+00:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10T17:00:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10Z", XSDDatatype.XSDdate),
            NodeFactory.createLiteralDT("2002-10-10-05:00", XSDDatatype.XSDdate),
            NodeFactory.createLiteralDT("2002-10-10", XSDDatatype.XSDdate));

    private static final List<String> zones = Arrays.asList("PT0S", "-PT5H", "PT0S", null, "PT0S", "-PT5H", null);

    private static DataSource connectionPool = null;
    private static DatabaseSchema schema = null;


    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();
        schema = new DatabaseSchema(connectionPool);
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
     * Rows of the query run over the literals stored in the variant.
     *
     * @param variant the variant
     * @param query the query
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(TypedQuadTable.Variant variant, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        List<Quad> quads = new ArrayList<>();

        for(int i = 0; i < literals.size(); i++)
            quads.add(new Quad(null, NodeFactory.createURI(subject(i).getValue()), NodeFactory.createURI(predicate),
                    literals.get(i)));

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        try(Request request = new Engine(config).getRequest())
        {
            TypedQuadTable.store(config, request, variant, quads);

            List<List<RdfTerm>> rows = new ArrayList<>();

            try(Result result = request.execute(query))
            {
                while(result.next())
                    rows.add(Arrays.asList(result.getRow()));
            }

            return rows;
        }
    }


    /**
     * Subject of the quad of the literal.
     *
     * @param index index of the literal
     * @return subject of the quad of the literal
     */
    private static Iri subject(int index)
    {
        return new Iri("http://example.org/s" + index);
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getVariants")
    void timezone(TypedQuadTable.Variant variant) throws Exception
    {
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int i = 0; i < literals.size(); i++)
            expected.add(Arrays.asList(subject(i),
                    zones.get(i) == null ? null : new TypedLiteral(zones.get(i), xsdDayTimeDurationIri)));

        assertThat(execute(variant, "SELECT ?x ?tz WHERE { ?x <" + predicate + "> ?o BIND(TIMEZONE(?o) AS ?tz) }"),
                containsInAnyOrder(expected.toArray()));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getVariants")
    void unbound(TypedQuadTable.Variant variant) throws Exception
    {
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int i = 0; i < literals.size(); i++)
            if(zones.get(i) == null)
                expected.add(List.of(subject(i)));

        assertThat(execute(variant,
                "SELECT ?x WHERE { ?x <" + predicate + "> ?o BIND(TIMEZONE(?o) AS ?tz) " + "FILTER(!BOUND(?tz)) }"),
                containsInAnyOrder(expected.toArray()));
    }
}
