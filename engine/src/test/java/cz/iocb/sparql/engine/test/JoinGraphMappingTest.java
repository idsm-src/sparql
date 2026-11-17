package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.ParametrisedIriMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Join quad mappings whose graph is a column: the table {@code item} holds the things {@code :thing/1} and
 * {@code :thing/2} with their graphs {@code :g1} and {@code :g2}, the table {@code label} their labels ({@code one} of
 * the first, {@code two} and {@code deux} of the second), mapped by :label with the graph of the item; and a mapping by
 * :tag whose graph comes from the second table, which has a column of the same name as the first one, so that only the
 * column of the right table gives the graphs {@code :t1} and {@code :t2}.
 */
public class JoinGraphMappingTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code join_graph_mapping_test} schema with the tables of the items and of the labels and maps them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema join_graph_mapping_test");
            statement.execute("create table join_graph_mapping_test.item (id int4 not null, "
                    + "g varchar collate \"C\" not null)");
            statement.execute("insert into join_graph_mapping_test.item values (1, 'http://example.org/g1'), "
                    + "(2, 'http://example.org/g2')");
            statement.execute("create table join_graph_mapping_test.label (id int4 not null, "
                    + "label varchar collate \"C\" not null, g varchar collate \"C\" not null)");
            statement.execute("insert into join_graph_mapping_test.label values (1, 'one', 'http://example.org/t1'), "
                    + "(2, 'two', 'http://example.org/t2'), (2, 'deux', 'http://example.org/t2')");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("thing", INT4, "http://example.org/thing/"));

        List<SourceTable> tables = List.of(new DatabaseTable("join_graph_mapping_test", "item"),
                new DatabaseTable("join_graph_mapping_test", "label"));
        List<JoinColumns> joins = List.of(new JoinColumns(new TableColumn("id", INT4), new TableColumn("id", INT4)));

        config.addQuadMapping(tables, joins, config.createIriMapping(iri, "g"), config.createIriMapping("thing", "id"),
                config.createIriMapping(":label"), config.createLiteralMapping(xsdString, "label"));

        config.getMappings(null)
                .add(new JoinTableQuadMapping(tables, joins, 1, config.createIriMapping(iri, "g"), 0,
                        config.createIriMapping("thing", "id"), 0, config.createIriMapping(":tag"), 1,
                        config.createLiteralMapping(xsdString, "label"), Collections.nCopies(2, new Conditions(true))));

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
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
     * IRI of the example namespace.
     *
     * @param name the local name
     * @return IRI of the example namespace
     */
    private static RdfTerm node(String name)
    {
        return new Iri("http://example.org/" + name);
    }


    /**
     * The xsd:string literal.
     *
     * @param value the value
     * @return the xsd:string literal
     */
    private static RdfTerm string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
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


    @Test
    @DisplayName("the graph of a join mapping is a column of the first table")
    void graphColumn() throws Exception
    {
        assertThat(execute("SELECT ?g ?s ?l WHERE { GRAPH ?g { ?s :label ?l } }"),
                containsInAnyOrder(row(node("g1"), node("thing/1"), string("one")),
                        row(node("g2"), node("thing/2"), string("two")),
                        row(node("g2"), node("thing/2"), string("deux"))));

        assertThat(execute("SELECT ?s ?l WHERE { GRAPH :g1 { ?s :label ?l } }"),
                containsInAnyOrder(row(node("thing/1"), string("one"))));

        assertThat(execute("SELECT ?s ?l FROM :g2 WHERE { ?s :label ?l }"),
                containsInAnyOrder(row(node("thing/2"), string("two")), row(node("thing/2"), string("deux"))));

        assertThat(execute("SELECT ?g ?s ?l FROM NAMED :g1 WHERE { GRAPH ?g { ?s :label ?l } }"),
                containsInAnyOrder(row(node("g1"), node("thing/1"), string("one"))));
    }


    @Test
    @DisplayName("the graph of a join mapping comes from the table the mapping names")
    void graphTable() throws Exception
    {
        assertThat(execute("SELECT ?g ?s ?l WHERE { GRAPH ?g { ?s :tag ?l } }"),
                containsInAnyOrder(row(node("t1"), node("thing/1"), string("one")),
                        row(node("t2"), node("thing/2"), string("two")),
                        row(node("t2"), node("thing/2"), string("deux"))));

        assertThat(execute("SELECT ?s ?l WHERE { GRAPH :t2 { ?s :tag ?l } }"),
                containsInAnyOrder(row(node("thing/2"), string("two")), row(node("thing/2"), string("deux"))));

        assertThat(execute("SELECT ?g WHERE { GRAPH ?g { ?s :tag ?l } } GROUP BY ?g"),
                containsInAnyOrder(row(node("t1")), row(node("t2"))));
    }


    @Test
    @DisplayName("join mappings differing in the tables of their terms are different mappings")
    void tableIndexes()
    {
        List<SourceTable> tables = List.of(new DatabaseTable("join_graph_mapping_test", "item"),
                new DatabaseTable("join_graph_mapping_test", "label"));
        List<JoinColumns> joins = List.of(new JoinColumns(new TableColumn("id", INT4), new TableColumn("id", INT4)));
        List<Conditions> conditions = Collections.nCopies(2, new Conditions(true));
        TermMapping term = new ParametrisedIriMapping(iri, List.of(new TableColumn("g", VARCHAR)));

        assertThat(new JoinTableQuadMapping(tables, joins, 0, term, 0, term, 0, term, 1, term, conditions),
                is(new JoinTableQuadMapping(tables, joins, 0, term, 0, term, 0, term, 1, term, conditions)));
        assertThat(new JoinTableQuadMapping(tables, joins, 0, term, 0, term, 0, term, 1, term, conditions),
                not(new JoinTableQuadMapping(tables, joins, 1, term, 0, term, 0, term, 1, term, conditions)));
    }
}
