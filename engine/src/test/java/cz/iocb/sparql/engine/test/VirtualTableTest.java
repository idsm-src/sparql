package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.Database;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Tests of quad mappings over virtual tables: the tables are attached to the generated statements as common table
 * expressions (in dependency order), their declared keys and foreign keys drive the optimiser, and their definitions
 * are validated when the configuration is built.
 */
public class VirtualTableTest
{
    /**
     * Prefix declaration shared by the queries.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Table of compounds.
     */
    private static final DatabaseTable compound = new DatabaseTable("virtual_test", "compound");

    /**
     * Distinct compound-synonym pairs computed from the synonym table.
     */
    private static final VirtualTable synonyms = new VirtualTable("compound_synonyms");

    /**
     * Synonyms joined with the label of their compound, computed from {@link #synonyms}.
     */
    private static final VirtualTable labeled = new VirtualTable("labeled_synonyms");

    /**
     * Connection pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Schema of the test database, shared by the configurations.
     */
    private static DatabaseSchema schema = null;


    /**
     * Creates the test tables and reads the database schema.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema virtual_test");
            statement.execute("create table virtual_test.compound (id int primary key, "
                    + "label varchar collate \"C\" not null)");
            statement.execute("insert into virtual_test.compound values (1, 'first'), (2, 'second'), (3, 'third')");
            statement.execute("""
                    create table virtual_test.synonym (compound int not null references \
                    virtual_test.compound(id), synonym varchar collate "C" not null, \
                    source int not null)""");
            statement.execute("insert into virtual_test.synonym values (1, 'a', 1), (1, 'a', 2), (1, 'b', 1), "
                    + "(2, 'a', 1), (2, 'a', 2)");
        }

        schema = new DatabaseSchema(connectionPool);
    }


    /**
     * Definition of {@link #synonyms}: distinct pairs, keyed by both columns, referencing the compound table.
     *
     * @return definition of {@link #synonyms}
     */
    private static VirtualTableDefinition synonymsDefinition()
    {
        VirtualTableDefinition definition = new VirtualTableDefinition(
                "SELECT DISTINCT compound, synonym FROM virtual_test.synonym");
        definition.addPrimaryKeys(List.of(new TableColumn("compound"), new TableColumn("synonym")));
        definition.addForeignKeys(compound, List.of(new TableColumn("id")), synonyms,
                List.of(new TableColumn("compound")));
        return definition;
    }


    /**
     * Definition of {@link #labeled}, which reads {@link #synonyms}.
     *
     * @return definition of {@link #labeled}
     */
    private static VirtualTableDefinition labeledDefinition()
    {
        VirtualTableDefinition definition = new VirtualTableDefinition("SELECT c.id, c.label, s.synonym "
                + "FROM virtual_test.compound c JOIN " + synonyms + " s ON s.compound = c.id", List.of(synonyms));
        definition.addPrimaryKeys(List.of(new TableColumn("id"), new TableColumn("synonym")));
        return definition;
    }


    /**
     * Configuration mapping the compound table and both virtual tables.
     *
     * @return the configuration
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration createConfiguration() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("compound", "int4", "http://example.org/compound/"));

        config.addVirtualTable(synonyms, synonymsDefinition());
        config.addVirtualTable(labeled, labeledDefinition());

        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(compound, graph, config.createIriMapping("compound", "id"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(synonyms, graph, config.createIriMapping("compound", "compound"),
                config.createIriMapping("ex:synonym"), config.createLiteralMapping(xsdString, "synonym"));
        config.addQuadMapping(labeled, graph, config.createIriMapping("compound", "id"),
                config.createIriMapping("ex:labeledSynonym"), config.createLiteralMapping(xsdString, "synonym"));
        config.addQuadMapping(labeled, graph, config.createLiteralMapping(xsdString, "label"),
                config.createIriMapping("ex:synonymOfLabel"), config.createLiteralMapping(xsdString, "synonym"));

        return config;
    }


    /**
     * Runs the query against the configuration and collects the rows.
     *
     * @param query the query without the prefix declaration
     * @return the result rows
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if a configured limit is exceeded
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(createConfiguration());

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
     *
     * @param terms the terms
     * @return the row
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the compound.
     *
     * @param id the compound id
     * @return IRI of the compound
     */
    private static Iri compound(int id)
    {
        return new Iri("http://example.org/compound/" + id);
    }


    /**
     * String literal.
     *
     * @param value the value
     * @return string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Integer literal.
     *
     * @param value the value
     * @return integer literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("an access to a virtual table")
    void virtualAccess() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s ?o WHERE { ?s ex:synonym ?o }");
        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("b")),
                row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("a virtual table joined with a database table by its foreign key")
    void joinWithDatabaseTable() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?l ?o WHERE { ?s ex:label ?l . ?s ex:synonym ?o }");
        assertThat(result, containsInAnyOrder(row(string("first"), string("a")), row(string("first"), string("b")),
                row(string("second"), string("a"))));
    }


    @Test
    @DisplayName("a virtual table depending on another virtual table")
    void dependentVirtualTable() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT ?s ?o WHERE { ?s ex:labeledSynonym ?o }");
        assertThat(result, containsInAnyOrder(row(compound(1), string("a")), row(compound(1), string("b")),
                row(compound(2), string("a"))));
    }


    @Test
    @DisplayName("two virtual tables in one query")
    void twoVirtualTables() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT ?l ?o WHERE { ?s ex:synonym ?o . ?l ex:synonymOfLabel ?o . FILTER(?o = 'b') }");
        assertThat(result, containsInAnyOrder(row(string("first"), string("b"))));
    }


    @Test
    @DisplayName("a virtual table under COUNT and DISTINCT")
    void aggregatedVirtualTable() throws Exception
    {
        List<List<RdfTerm>> result = execute("SELECT (COUNT(DISTINCT ?s) AS ?c) WHERE { ?s ex:labeledSynonym ?o }");
        assertThat(result, containsInAnyOrder(row(integer(2))));
    }


    @Test
    @DisplayName("a virtual table in FILTER EXISTS")
    void existsOverVirtualTable() throws Exception
    {
        List<List<RdfTerm>> result = execute(
                "SELECT ?l WHERE { ?s ex:label ?l . FILTER NOT EXISTS { ?s ex:synonym ?o } }");
        assertThat(result, containsInAnyOrder(row(string("third"))));
    }


    @Test
    @DisplayName("the facts of a virtual table are known to the configuration's schema only")
    void schemaFacts() throws Exception
    {
        SparqlDatabaseConfiguration config = createConfiguration();
        Set<Column> key = Set.of(new TableColumn("compound"), new TableColumn("synonym"));

        assertTrue(config.getDatabaseSchema().isKey(synonyms, key));
        assertFalse(config.getDatabaseSchema().getForeignKeys(compound, synonyms).isEmpty());
        assertFalse(schema.isKey(synonyms, key));
        assertTrue(schema.getForeignKeys(compound, synonyms).isEmpty());
    }


    @Test
    @DisplayName("a mapping over an undefined virtual table is rejected")
    void undefinedVirtualTable() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addIriClass(new IntegerUserIriClass("compound", "int4", "http://example.org/compound/"));
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(synonyms, graph, config.createIriMapping("compound", "compound"),
                        config.createIriMapping("<http://example.org/synonym>"),
                        config.createLiteralMapping(xsdString, "synonym")));
    }


    @Test
    @DisplayName("conflicting definitions of a virtual table are rejected")
    void conflictingDefinitions() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addVirtualTable(synonyms, synonymsDefinition());
        config.addVirtualTable(synonyms, synonymsDefinition());

        assertThrows(IllegalArgumentException.class,
                () -> config.addVirtualTable(synonyms, new VirtualTableDefinition("SELECT 1")));
    }
}
