package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.Database;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;



/**
 * Checks the deduplication of table accesses over tables that are not unique with respect to the mapped columns.
 *
 * The table {@code synonym} holds the pairs (1,a), (1,a), (1,b), (2,a), (2,a), so a plain multiset access sees five
 * rows, whereas the set of mapped triples has three members. The duplicated pairs differ in the {@code source} column,
 * on which the mappings {@code ex:synonym1} (source 1) and {@code ex:synonym2} (source 2) are restricted.
 */
public class DistinctTableAccessTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Catalog of the test database, read after the test tables were created.
     */
    private static DatabaseSchema schema = null;


    /**
     * Creates the {@code distinct_test} schema with the {@code compound} and {@code synonym} tables.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema distinct_test");

            statement.execute("create table distinct_test.compound (id int primary key, "
                    + "label varchar collate \"C\" not null)");
            statement.execute("insert into distinct_test.compound values (1, 'first'), (2, 'second'), (3, 'third')");

            statement.execute("""
                    create table distinct_test.synonym (compound int not null references \
                    distinct_test.compound(id), synonym varchar collate "C" not null, \
                    source int not null)""");
            statement.execute("insert into distinct_test.synonym values (1, 'a', 1), (1, 'a', 2), (1, 'b', 1), "
                    + "(2, 'a', 1), (2, 'a', 2)");
        }

        schema = new DatabaseSchema(connectionPool);
    }


    /**
     * Configuration mapping the two tables; {@code distinct} sets the distinct flag of the {@code synonym} mappings.
     */
    private static SparqlDatabaseConfiguration createConfiguration(boolean distinct) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("compound", "int4", "http://example.org/compound/"));

        DatabaseTable compound = new DatabaseTable("distinct_test", "compound");
        DatabaseTable synonym = new DatabaseTable("distinct_test", "synonym");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(compound, graph, config.createIriMapping("compound", "id"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(synonym, graph, config.createIriMapping("compound", "compound"),
                config.createIriMapping("ex:synonym"), config.createLiteralMapping(xsdString, "synonym"),
                new Conditions(true), distinct);

        config.addQuadMapping(synonym, graph, config.createIriMapping("compound", "compound"),
                config.createIriMapping("ex:synonym1"), config.createLiteralMapping(xsdString, "synonym"),
                config.createAreEqualCondition("source", "'1'::int4"), distinct);

        config.addQuadMapping(synonym, graph, config.createIriMapping("compound", "compound"),
                config.createIriMapping("ex:synonym2"), config.createLiteralMapping(xsdString, "synonym"),
                config.createAreEqualCondition("source", "'2'::int4"), distinct);

        config.addQuadMapping(List.of(compound, synonym),
                List.of(new JoinColumns(new TableColumn("id"), new TableColumn("compound"), "int4")), graph,
                config.createLiteralMapping(xsdString, "label"), config.createIriMapping("ex:labelSynonym"),
                config.createLiteralMapping(xsdString, "synonym"), List.of(new Conditions(true), new Conditions(true)),
                List.of(false, distinct));

        return config;
    }


    /**
     * Runs the query against the configuration with the given distinct flag and returns all rows.
     */
    private static List<List<RdfTerm>> execute(boolean distinct, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(createConfiguration(distinct));

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the compound with the given id.
     */
    private static Iri compound(int id)
    {
        return new Iri("http://example.org/compound/" + id);
    }


    /**
     * Expected xsd:string literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Expected xsd:integer literal.
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("a plain access keeps the duplicated rows")
    void multisetAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute(false, "SELECT ?s ?o WHERE { ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("a")),
                row(compound(1), string("b")), row(compound(2), string("a")), row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("a distinct access returns each triple once")
    void distinctAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?s ?o WHERE { ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("b")),
                row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("dropping a variable keeps the cardinality of the triples")
    void projectedAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?s WHERE { ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(compound(1)), row(compound(1)), row(compound(2))));
    }


    @Test
    @DisplayName("a distinct access under DISTINCT")
    void distinctOverAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT DISTINCT ?s WHERE { ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(compound(1)), row(compound(2))));
    }


    @Test
    @DisplayName("a distinct access under COUNT")
    void countOverAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT (COUNT(*) AS ?c) WHERE { ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(integer(3))));
    }


    @Test
    @DisplayName("a distinct access with a constant subject")
    void constantSubject() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?o WHERE { <http://example.org/compound/1> ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(string("a")), row(string("b"))));
    }


    @Test
    @DisplayName("a distinct access with constant terms only")
    void constantTerms() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT * WHERE { <http://example.org/compound/1> ex:synonym 'a' }");

        assertThat(result, containsInAnyOrder(row()));
    }


    @Test
    @DisplayName("a distinct access joined with a keyed access")
    void joinWithKeyedAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?l ?o WHERE { ?s ex:label ?l . ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(string("first"), string("a")), row(string("first"), string("b")),
                row(string("second"), string("a"))));
    }


    @Test
    @DisplayName("a join mapping with a distinct table")
    void joinMapping() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?l ?o WHERE { ?l ex:labelSynonym ?o }");

        assertThat(result, containsInAnyOrder(row(string("first"), string("a")), row(string("first"), string("b")),
                row(string("second"), string("a"))));
    }


    @Test
    @DisplayName("a join mapping without a distinct table keeps the duplicated rows")
    void joinMappingMultiset() throws Exception
    {
        List<List<RdfTerm>> result = execute(false, "SELECT ?l ?o WHERE { ?l ex:labelSynonym ?o }");

        assertThat(result,
                containsInAnyOrder(row(string("first"), string("a")), row(string("first"), string("a")),
                        row(string("first"), string("b")), row(string("second"), string("a")),
                        row(string("second"), string("a"))));
    }


    @Test
    @DisplayName("two distinct accesses in a union under DISTINCT")
    void distinctUnion() throws Exception
    {
        List<List<RdfTerm>> result = execute(true,
                "SELECT DISTINCT ?o WHERE { { <http://example.org/compound/1> ex:synonym ?o } "
                        + "UNION { <http://example.org/compound/2> ex:synonym ?o } }");

        assertThat(result, containsInAnyOrder(row(string("a")), row(string("b"))));
    }


    @Test
    @DisplayName("a distinct subselect over the same table filters the rows of a plain access")
    void distinctSubselectFilter() throws Exception
    {
        List<List<RdfTerm>> result = execute(false,
                "SELECT ?s ?o WHERE { ?s ex:synonym1 ?o . { SELECT DISTINCT ?s WHERE { ?s ex:synonym2 ?x } } }");

        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("b")),
                row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("a distinct subselect over the same table filters the rows of a distinct access")
    void distinctSubselectFilterOverDistinctAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute(true,
                "SELECT ?s ?o WHERE { ?s ex:synonym1 ?o . { SELECT DISTINCT ?s WHERE { ?s ex:synonym2 ?x } } }");

        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("b")),
                row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("a distinct access joined on all its columns filters the other access")
    void distinctAccessFilter() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?s ?o WHERE { ?s ex:synonym1 ?o . ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("b")),
                row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("a plain access joined on all its columns multiplies the other access")
    void plainAccessFilter() throws Exception
    {
        List<List<RdfTerm>> result = execute(false, "SELECT ?s ?o WHERE { ?s ex:synonym1 ?o . ?s ex:synonym ?o }");

        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("a")),
                row(compound(1), string("b")), row(compound(2), string("a")), row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("a distinct access not joined on all its columns keeps its multiplicities")
    void distinctAccessWithExtraVariable() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?s ?o ?o2 WHERE { ?s ex:synonym ?o . ?s ex:synonym1 ?o2 }");

        assertThat(result,
                containsInAnyOrder(row(compound(1), string("a"), string("a")),
                        row(compound(1), string("a"), string("b")), row(compound(1), string("b"), string("a")),
                        row(compound(1), string("b"), string("b")), row(compound(2), string("a"), string("a"))));
    }


    @Test
    @DisplayName("distinct accesses with conditions on unjoined columns are not merged")
    void distinctAccessesWithForeignConditions() throws Exception
    {
        List<List<RdfTerm>> result = execute(true, "SELECT ?s ?o WHERE { ?s ex:synonym1 ?o . ?s ex:synonym2 ?o }");

        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(2), string("a"))));
    }
}
