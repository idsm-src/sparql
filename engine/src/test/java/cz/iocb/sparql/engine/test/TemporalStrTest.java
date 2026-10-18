package cz.iocb.sparql.engine.test;

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
 * STR of xsd:dateTime and xsd:date literals, in canonical and other lexical forms, with and without a zone, stored in
 * the classes of every {@link TypedQuadTable} variant: the lexical form of the literal, whichever temporal class holds
 * it.
 */
public class TemporalStrTest
{
    private static final String predicate = "http://example.org/p";

    private static final List<Node> literals = List.of(
            NodeFactory.createLiteralDT("2002-10-10T17:00:00Z", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10T12:00:00-05:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10T17:00:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10T17:00:00.000Z", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10T17:00:00+00:00", XSDDatatype.XSDdateTime),
            NodeFactory.createLiteralDT("2002-10-10Z", XSDDatatype.XSDdate),
            NodeFactory.createLiteralDT("2002-10-10-05:00", XSDDatatype.XSDdate),
            NodeFactory.createLiteralDT("2002-10-10", XSDDatatype.XSDdate),
            NodeFactory.createLiteralDT("2002-10-10+00:00", XSDDatatype.XSDdate));

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


    @ParameterizedTest(name = "{0}")
    @MethodSource("getVariants")
    void str(TypedQuadTable.Variant variant) throws Exception
    {
        List<Quad> quads = new ArrayList<>();
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int i = 0; i < literals.size(); i++)
        {
            Node subject = NodeFactory.createURI("http://example.org/s" + i);
            quads.add(new Quad(null, subject, NodeFactory.createURI(predicate), literals.get(i)));
            expected.add(List.of(new Iri(subject.getURI()),
                    new TypedLiteral(literals.get(i).getLiteralLexicalForm(), xsdStringIri)));
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        try(Request request = new Engine(config).getRequest())
        {
            TypedQuadTable.store(config, request, variant, quads);

            List<List<RdfTerm>> rows = new ArrayList<>();

            try(Result result = request.execute("SELECT ?x (STR(?o) AS ?s) WHERE { ?x <" + predicate + "> ?o }"))
            {
                while(result.next())
                    rows.add(Arrays.asList(result.getRow()));
            }

            assertThat(rows, containsInAnyOrder(expected.toArray()));
        }
    }
}
