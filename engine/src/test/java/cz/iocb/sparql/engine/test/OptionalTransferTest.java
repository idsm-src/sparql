package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Checks that an OPTIONAL, or a part of it, reading the row of a member of the left side by a key is moved to the left
 * side, with the join optimizer off and on, and that it is not where the move would bind a variable the parent needs or
 * where the part requires more of the row.
 *
 * The table {@code gene} holds the genes 1 and 2 with the labels g1 and g2 and the note n2 of the gene 2; the table
 * {@code protein} holds the proteins 1 to 3 of the genes 1, 2 and 1, referenced by the not null column {@code gene},
 * the nullable column {@code other_gene}, set to the gene 1 for the protein 2 only, and the description d1 of the
 * protein 1; the table {@code protein_name}, keyed by the protein, the full names f1, f2 and f3.
 */
public class OptionalTransferTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The configurations with the join optimizer off and on.
     */
    private static List<SparqlDatabaseConfiguration> configs = null;


    /**
     * Creates the {@code optional_transfer_test} schema with its tables and the configurations mapping them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema optional_transfer_test");
            statement.execute("""
                    create table optional_transfer_test.gene (id int primary key, label varchar not null, \
                    note varchar)""");
            statement.execute("insert into optional_transfer_test.gene values (1, 'g1', null), (2, 'g2', 'n2')");
            statement.execute("""
                    create table optional_transfer_test.protein (id int primary key, \
                    gene int not null references optional_transfer_test.gene(id), \
                    other_gene int references optional_transfer_test.gene(id), descr varchar)""");
            statement.execute("""
                    insert into optional_transfer_test.protein values (1, 1, null, 'd1'), (2, 2, 1, null), \
                    (3, 1, null, null)""");
            statement.execute("""
                    create table optional_transfer_test.protein_name (protein int primary key \
                    references optional_transfer_test.protein(id), full_name varchar not null)""");
            statement.execute("insert into optional_transfer_test.protein_name values (1, 'f1'), (2, 'f2'), (3, 'f3')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);
        configs = new ArrayList<>();

        for(boolean optimizer : new boolean[] { false, true })
        {
            SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
            config.setJoinOptimizer(optimizer);

            config.addPrefix("ex", "http://example.org/");
            config.addIriClass(new IntegerUserIriClass("protein", INT4, "http://example.org/protein/"));
            config.addIriClass(new IntegerUserIriClass("gene", INT4, "http://example.org/gene/"));

            DatabaseTable gene = new DatabaseTable("optional_transfer_test", "gene");
            DatabaseTable protein = new DatabaseTable("optional_transfer_test", "protein");
            DatabaseTable name = new DatabaseTable("optional_transfer_test", "protein_name");
            ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

            config.addQuadMapping(gene, graph, config.createIriMapping("gene", "id"),
                    config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));
            config.addQuadMapping(gene, graph, config.createIriMapping("gene", "id"),
                    config.createIriMapping("ex:note"), config.createLiteralMapping(xsdString, "note"));
            config.addQuadMapping(protein, graph, config.createIriMapping("protein", "id"),
                    config.createIriMapping("ex:gene"), config.createIriMapping("gene", "gene"));
            config.addQuadMapping(protein, graph, config.createIriMapping("protein", "id"),
                    config.createIriMapping("ex:otherGene"), config.createIriMapping("gene", "other_gene"));
            config.addQuadMapping(protein, graph, config.createIriMapping("protein", "id"),
                    config.createIriMapping("ex:descr"), config.createLiteralMapping(xsdString, "descr"));
            config.addQuadMapping(name, graph, config.createIriMapping("protein", "protein"),
                    config.createIriMapping("ex:fullName"), config.createLiteralMapping(xsdString, "full_name"));

            configs.add(config);
        }
    }


    /**
     * Translates the query into SQL.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Runs the query and returns all rows.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * How many times the SQL reads the table.
     *
     * @param sql the SQL
     * @param table the name of a table of the test schema
     * @return number of the accesses to the table in the SQL
     */
    private static int reads(String sql, String table)
    {
        String name = "\"optional_transfer_test\".\"" + table + "\"";

        return (sql.length() - sql.replace(name, "").length()) / name.length();
    }


    /**
     * Expected row of a protein and terms.
     *
     * @param protein the protein id
     * @param terms the terms, null for an unbound value
     * @return row of the protein and the terms
     */
    private static List<RdfTerm> row(int protein, RdfTerm... terms)
    {
        List<RdfTerm> row = new ArrayList<>();
        row.add(new Iri("http://example.org/protein/" + protein));
        row.addAll(Arrays.asList(terms));
        return row;
    }


    /**
     * A string literal.
     *
     * @param value the value
     * @return the literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("an OPTIONAL reading the row of a member of a join by its key becomes a part of the member")
    void optionalOfJoinMember() throws Exception
    {
        String query = "SELECT ?p ?l ?d WHERE { ?p ex:gene ?g . ?g ex:label ?l . OPTIONAL { ?p ex:descr ?d } }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1, string("g1"), string("d1")),
                    row(2, string("g2"), null), row(3, string("g1"), null)));

            String sql = translate(config, query);
            assertThat(sql, not(containsString("LEFT JOIN")));
            assertThat(reads(sql, "protein"), is(1));
        }
    }


    @Test
    @DisplayName("a part of an OPTIONAL reading the row of the left side by its key is moved to the left side")
    void partReadingLeftRow() throws Exception
    {
        String query = "SELECT ?p ?l WHERE { ?p ex:descr ?d OPTIONAL { ?p ex:gene ?g . ?g ex:label ?l } }";

        for(SparqlDatabaseConfiguration config : configs)
        {
            assertThat(execute(config, query), containsInAnyOrder(row(1, string("g1"))));

            String sql = translate(config, query);
            assertThat(sql, not(containsString("LEFT JOIN")));
            assertThat(reads(sql, "protein"), is(1));
        }
    }


    @Test
    @DisplayName("a part of an OPTIONAL is not moved when the parent needs its variable")
    void partVariableNeeded() throws Exception
    {
        String query = "SELECT ?p ?g ?t WHERE { ?p ex:descr ?d OPTIONAL { ?p ex:gene ?g . ?g ex:note ?t } }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query), containsInAnyOrder(row(1, null, null)));
    }


    @Test
    @DisplayName("a part of an OPTIONAL requiring a nullable column of the row is not moved")
    void partRequiringNullableColumn() throws Exception
    {
        String query = "SELECT ?p ?l WHERE { ?p ex:fullName ?f OPTIONAL { ?p ex:otherGene ?g . ?g ex:label ?l } }";

        for(SparqlDatabaseConfiguration config : configs)
            assertThat(execute(config, query),
                    containsInAnyOrder(row(1, (RdfTerm) null), row(2, string("g1")), row(3, (RdfTerm) null)));
    }
}
