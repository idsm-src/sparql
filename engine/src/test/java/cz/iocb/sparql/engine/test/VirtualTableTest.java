package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.CommonTable;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



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
        definition.addColumn(new TableColumn("compound", INT4));
        definition.addColumn(new TableColumn("synonym", VARCHAR));
        definition.addPrimaryKeys(List.of(new TableColumn("compound", INT4), new TableColumn("synonym", VARCHAR)));
        definition.addForeignKeys(compound, List.of(new TableColumn("id", INT4)), synonyms,
                List.of(new TableColumn("compound", INT4)));
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
        definition.addColumn(new TableColumn("id", INT4));
        definition.addColumn(new TableColumn("label", VARCHAR));
        definition.addColumn(new TableColumn("synonym", VARCHAR));
        definition.addPrimaryKeys(List.of(new TableColumn("id", INT4), new TableColumn("synonym", VARCHAR)));
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
        config.addIriClass(new IntegerUserIriClass("compound", INT4, "http://example.org/compound/"));

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
     * The SQL statement the query is translated to.
     *
     * @param query the query without the prefix declaration
     * @return the SQL statement
     * @throws Exception if the query cannot be translated
     */
    private static String translate(String query) throws Exception
    {
        Engine engine = new Engine(createConfiguration());

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
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


    /**
     * Boolean literal.
     *
     * @param value the value
     * @return boolean literal
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
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
    @DisplayName("a string spelling an alias of a common table expression reading a virtual table")
    void aliasInString() throws Exception
    {
        List<List<RdfTerm>> result = execute("""
                SELECT ?l WHERE { { ?s ex:label ?l \
                FILTER(?l NOT IN ('"cte0"', '"cte1"', '"cte2"')) } UNION { ?q ex:synonym ?w \
                FILTER EXISTS { GRAPH ?g { <http://example.org/compound/1> ex:synonym* ?w } } \
                FILTER(?q = <http://example.org/other/1>) } }""");
        assertThat(result, containsInAnyOrder(row(string("first")), row(string("second")), row(string("third"))));
    }


    @Test
    @DisplayName("a virtual table in the EXISTS pattern of a BIND")
    void existsInBind() throws Exception
    {
        String query = "SELECT ?l ?b WHERE { ?s ex:label ?l BIND(EXISTS { ?s ex:synonym ?o } AS ?b) }";

        assertThat(execute(query), containsInAnyOrder(row(string("first"), bool(true)),
                row(string("second"), bool(true)), row(string("third"), bool(false))));
        assertThat(translate(query), startsWith("WITH \"compound_synonyms\" AS ("));
    }


    @Test
    @DisplayName("a virtual table in the EXISTS pattern of a filter of an OPTIONAL")
    void existsInOptionalFilter() throws Exception
    {
        String query = "SELECT ?l ?t WHERE { ?s ex:label ?l OPTIONAL { ?s ex:label ?t "
                + "FILTER EXISTS { ?s ex:synonym 'b' } } }";

        assertThat(execute(query), containsInAnyOrder(row(string("first"), string("first")),
                row(string("second"), null), row(string("third"), null)));
        assertThat(translate(query), startsWith("WITH \"compound_synonyms\" AS ("));
    }


    @Test
    @DisplayName("a virtual table in the EXISTS pattern of an aggregated expression")
    void existsInAggregate() throws Exception
    {
        String query = "SELECT (SUM(IF(EXISTS { ?s ex:synonym ?o }, 1, 0)) AS ?c) WHERE { ?s ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(integer(2))));
        assertThat(translate(query), startsWith("WITH \"compound_synonyms\" AS ("));
    }


    @Test
    @DisplayName("an inline table and a virtual table in nested EXISTS patterns")
    void nestedExists() throws Exception
    {
        String query = "SELECT ?l WHERE { ?s ex:label ?l FILTER EXISTS { VALUES ?o { 'b' 'c' } "
                + "FILTER EXISTS { ?s ex:synonym ?o } } }";

        assertThat(execute(query), containsInAnyOrder(row(string("first"))));

        // the statement declares the virtual table and the inline table, each once, which the patterns only read
        String sql = translate(query);
        assertThat(sql, startsWith("WITH \"compound_synonyms\" AS ("));
        assertThat(sql, containsString(", \"cte0\" AS (SELECT * FROM (VALUES"));
        assertThat(sql.indexOf(" AS (SELECT * FROM (VALUES"), is(sql.lastIndexOf(" AS (SELECT * FROM (VALUES")));
        assertThat(sql.indexOf("WITH "), is(sql.lastIndexOf("WITH ")));
    }


    @Test
    @DisplayName("a statement declares the tables its code reads, also through its common table expressions")
    void withClause() throws Exception
    {
        Engine engine = new Engine(createConfiguration());

        try(Request request = engine.getRequest())
        {
            CommonTable first = request.getCommonTableExpression(new SqlCode("SELECT 1 AS x"));
            CommonTable second = request.getCommonTableExpression(
                    new SqlBuilder().append("SELECT x FROM ").append(first).append(", ").append(labeled).toCode());
            CommonTable unused = request.getCommonTableExpression(new SqlCode("SELECT 2 AS y"));

            // a query asked for again has the common table expression it had
            assertSame(first, request.getCommonTableExpression(new SqlCode("SELECT 1 AS x")));
            assertEquals(List.of("cte0", "cte1", "cte2"), List.of(first.getName(), second.getName(), unused.getName()));

            // the code reads only the second one, whose query reads the first one and a virtual table, which reads
            // another virtual table; each table follows the tables its query reads
            SqlCode code = new SqlBuilder().append("SELECT x FROM ").append(second).toCode();

            assertEquals("WITH \"compound_synonyms\" AS (" + synonymsDefinition().getQuery()
                    + "), \"labeled_synonyms\" AS (" + labeledDefinition().getQuery()
                    + "), \"cte0\" AS (SELECT 1 AS x), \"cte1\" AS (SELECT x FROM \"cte0\", \"labeled_synonyms\") ",
                    SqlSelect.translateWithClause(request, code));

            // a table the code does not read is not declared, even when the text spells its name
            assertEquals("", SqlSelect.translateWithClause(request, new SqlCode("SELECT '\"cte2\"'")));
        }
    }


    @Test
    @DisplayName("the facts of a virtual table are known to the configuration's schema only")
    void schemaFacts() throws Exception
    {
        SparqlDatabaseConfiguration config = createConfiguration();
        Set<Column> key = Set.of(new TableColumn("compound", INT4), new TableColumn("synonym", VARCHAR));

        assertTrue(config.getDatabaseSchema().isKey(synonyms, key));
        assertFalse(config.getDatabaseSchema().getForeignKeys(compound, synonyms).isEmpty());
        assertFalse(schema.isKey(synonyms, key));
        assertTrue(schema.getForeignKeys(compound, synonyms).isEmpty());

        assertEquals(INT4, config.getDatabaseSchema().getColumnType(synonyms, new TableColumn("compound", INT4)));
        assertEquals(VARCHAR, config.getDatabaseSchema().getColumnType(labeled, new TableColumn("label", VARCHAR)));
        assertNull(schema.getColumnType(synonyms, new TableColumn("compound", INT4)));
    }


    @Test
    @DisplayName("column references are typed from the schema when the mapping is registered")
    void columnTypes() throws Exception
    {
        SparqlDatabaseConfiguration config = createConfiguration();

        assertEquals(new TableColumn("id", INT4), schema.getColumn(compound, "id"));
        assertEquals(INT4, schema.getColumn(compound, "id").getType());
        assertEquals(VARCHAR, schema.getColumn(compound, "label").getType());
        assertNull(schema.getColumn(compound, "missing"));

        assertEquals(INT4, config.getColumn(compound, "id").getType());
        assertEquals(VARCHAR, config.getColumn(labeled, "synonym").getType());
        assertEquals(VARCHAR, config.getColumn(compound, "(label || label)::varchar").getType());
        assertEquals(List.of(new TableColumn("id", INT4)), config.getColumns(compound, "id"));
        assertThrows(IllegalArgumentException.class, () -> config.getColumn(compound, "missing"));

        assertTrue(config.createIsNotNullCondition(compound, "label").getIsNotNull()
                .contains(new TableColumn("label", VARCHAR)));
        assertThrows(IllegalArgumentException.class, () -> config.createIsNotNullCondition("label"));
        assertThrows(IllegalArgumentException.class, () -> config.createIsNotNullCondition(compound, "missing"));

        for(QuadMapping mapping : config.getMappings(null))
        {
            for(TermMapping term : List.of(mapping.getSubject(), mapping.getObject()))
            {
                if(!(term instanceof ParametrisedMapping))
                    continue;

                List<Column> columns = term.getColumns(null);

                for(int i = 0; i < columns.size(); i++)
                    assertEquals(term.getResourceClass(null).getSqlTypes().get(i), columns.get(i).getType());
            }
        }

        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(compound, null, config.createIriMapping("compound", "missing"),
                        config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label")));

        // the compound class declares an int4 column, the label column is a varchar
        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(compound, null, config.createIriMapping("compound", "label"),
                        config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label")));

        // the same mismatch in a condition
        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(compound, null, config.createIriMapping("compound", "id"),
                        config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"),
                        config.createAreEqualCondition("label", "'1'::int4")));
    }


    @Test
    @DisplayName("a mapping over an undefined virtual table is rejected")
    void undefinedVirtualTable() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addIriClass(new IntegerUserIriClass("compound", INT4, "http://example.org/compound/"));
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
