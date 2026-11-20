package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.reference;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.graph.Node;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;
import cz.iocb.sparql.testing.Database;



/**
 * Test data of a query evaluation test stored as a triple store that numbers the resources only keeps them: a table
 * numbers the IRIs and the blank nodes, held in boxes, a table of the quads whose object is a resource holds the
 * numbers of the graph (NULL for the default graph), the subject, the predicate and the object of each quad, and a
 * table of the other quads holds their objects, which are literals or triple terms, in boxes next to the numbers of the
 * other terms. The quads are mapped by join mappings that join a table of quads to the table of the resources once for
 * every numbered term of a quad; the boxes of the resources and the boxes of the other objects are mapped by classes
 * that are disjoint, so that a pattern reads only the table that can hold its object.
 */
public final class MixedQuadTable
{
    /**
     * How the tables are queried.
     */
    public enum Variant
    {
        /**
         * The table of the resources is a table like any other: the queries read it for their constants too, and the
         * planner of the database chooses the joins.
         */
        PLAIN,

        /**
         * The table of the resources is a dictionary table, whose rows the translation reads for the constants of a
         * query, the tables of the quads are probed tables, which the translation asks whether they hold the constants
         * of a pattern, and the join optimizer of the engine chooses the joins.
         */
        DICTIONARY
    }


    /**
     * Table of the IRIs and the blank nodes of the current test with their numbers.
     */
    public static final DatabaseTable resourceTable = new DatabaseTable("sparql_test", "resources");

    /**
     * Table of the quads of the current test whose objects are resources, as the numbers of their terms.
     */
    public static final DatabaseTable resourceQuadTable = new DatabaseTable("sparql_test", "resource_quads");

    /**
     * Table of the other quads of the current test: the numbers of their terms and their objects.
     */
    public static final DatabaseTable valueQuadTable = new DatabaseTable("sparql_test", "value_quads");

    /**
     * The class of the boxes of the numbered terms: an IRI or a blank node.
     */
    public static final ResourceClass resource = unionize(Set.of(reference), box);

    /**
     * The class of the boxes of the objects that are not numbered: any term but an IRI and a blank node.
     */
    public static final ResourceClass value = subtract(box, reference);


    /**
     * Not instantiable.
     */
    private MixedQuadTable()
    {
    }


    /**
     * Creates the three tables unless they exist: the resources are unique and numbered by the primary key, which the
     * numbers of the quads reference, and the quads are indexed by the predicate with the subject and, where it is a
     * number, with the object.
     *
     * @param statement statement of the test database
     * @throws SQLException on database errors
     */
    public static void create(Statement statement) throws SQLException
    {
        String number = " references " + resourceTable + "(id)";

        statement.execute("create table if not exists " + resourceTable
                + " (id int8 primary key, term sparql.rdfbox not null unique)");

        statement.execute("create table if not exists " + resourceQuadTable + " (g int8" + number + ", s int8 not null"
                + number + ", p int8 not null" + number + ", o int8 not null" + number + ")");

        statement.execute("create table if not exists " + valueQuadTable + " (g int8" + number + ", s int8 not null"
                + number + ", p int8 not null" + number + ", o sparql.rdfbox not null)");

        statement.execute("create index if not exists resource_quads_pso on " + resourceQuadTable + " (p, s, o)");
        statement.execute("create index if not exists resource_quads_pos on " + resourceQuadTable + " (p, o, s)");
        statement.execute("create index if not exists value_quads_ps on " + valueQuadTable + " (p, s)");
    }


    /**
     * Registers the quad mappings of the tables in the configuration: for each of the two tables of quads, the quads of
     * the default graph (those with a NULL graph) by a mapping joining the table of the resources for the subject and
     * the predicate, and for the object where it is numbered, and the quads of the named graphs by a mapping joining it
     * for the graph as well.
     *
     * @param config the configuration, whose schema knows the tables
     * @param variant how the tables are queried
     */
    public static void map(SparqlDatabaseConfiguration config, Variant variant)
    {
        TermMapping term = config.createTermMapping(resource, "term");
        TableColumn id = new TableColumn("id", INT8);

        for(DatabaseTable quadTable : List.of(resourceQuadTable, valueQuadTable))
        {
            boolean numbered = quadTable == resourceQuadTable;

            List<JoinColumns> joins = new ArrayList<>();
            joins.add(new JoinColumns(0, new TableColumn("s", INT8), 1, id));
            joins.add(new JoinColumns(0, new TableColumn("p", INT8), 2, id));

            if(numbered)
                joins.add(new JoinColumns(0, new TableColumn("o", INT8), 3, id));

            // the table of the graphs of the named quads follows the tables of the other numbered terms
            int graphTable = joins.size() + 1;
            int objectTable = numbered ? 3 : 0;
            TermMapping object = numbered ? term : config.createTermMapping(value, "o");

            List<SourceTable> tables = new ArrayList<>(Collections.nCopies(graphTable, resourceTable));
            tables.set(0, quadTable);

            List<Conditions> conditions = new ArrayList<>(Collections.nCopies(graphTable, new Conditions(true)));
            conditions.set(0, config.createIsNullCondition(quadTable, "g"));

            config.addQuadMapping(List.copyOf(tables), List.copyOf(joins), 0, null, 1, term, 2, term, objectTable,
                    object, conditions, Collections.nCopies(graphTable, false));

            tables.add(resourceTable);
            joins.add(new JoinColumns(0, new TableColumn("g", INT8), graphTable, id));

            config.addQuadMapping(tables, joins, graphTable, term, 1, term, 2, term, objectTable, object);
        }

        if(variant == Variant.DICTIONARY)
        {
            config.addDictionaryTable(resourceTable);
            config.addProbedTable(resourceQuadTable);
            config.addProbedTable(valueQuadTable);
            config.setJoinOptimizer(true);
        }
    }


    /**
     * Replaces the content of the tables by the quads, a null graph standing for the default graph: the resources are
     * numbered in the order of their first occurrence, and every term kept in a box is boxed as the engine boxes a
     * constant of its class.
     *
     * @param request the current request
     * @param quads the quads
     * @throws SQLException on database errors
     */
    public static void store(Request request, List<Quad> quads) throws SQLException
    {
        Map<String, Integer> numbers = new LinkedHashMap<>();
        List<String> resourceRows = new ArrayList<>();
        List<String> valueRows = new ArrayList<>();

        for(Quad quad : quads)
        {
            boolean numbered = quad.object().isURI() || quad.object().isBlank();

            String object = numbered ? getNumber(request, numbers, quad.object()) :
                    SparqlTest.getBoxedTerm(request, quad.object());

            (numbered ? resourceRows : valueRows).add(
                    "(" + getNumber(request, numbers, quad.graph()) + ", " + getNumber(request, numbers, quad.subject())
                            + ", " + getNumber(request, numbers, quad.predicate()) + ", " + object + ")");
        }

        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement())
        {
            statement.execute("truncate " + resourceQuadTable + ", " + valueQuadTable + ", " + resourceTable);

            if(!numbers.isEmpty())
            {
                List<String> resources = new ArrayList<>(numbers.size());
                numbers.forEach((term, number) -> resources.add("(" + number + ", " + term + ")"));

                statement
                        .execute("insert into " + resourceTable + " (id, term) values " + String.join(", ", resources));
            }

            if(!resourceRows.isEmpty())
                statement.execute(
                        "insert into " + resourceQuadTable + " (g, s, p, o) values " + String.join(", ", resourceRows));

            if(!valueRows.isEmpty())
                statement.execute(
                        "insert into " + valueQuadTable + " (g, s, p, o) values " + String.join(", ", valueRows));

            // the join optimizer of the engine estimates the joins from the statistics of the tables
            statement.execute("analyze " + resourceTable);
            statement.execute("analyze " + resourceQuadTable);
            statement.execute("analyze " + valueQuadTable);
        }
    }


    /**
     * SQL code of the number of a resource of the data, which gets the next number when it occurs for the first time;
     * NULL for a null node.
     *
     * @param request the current request
     * @param numbers the numbers of the resources so far, by the SQL expressions of their boxes
     * @param node the resource
     * @return SQL code of the number of the resource
     */
    private static String getNumber(Request request, Map<String, Integer> numbers, Node node)
    {
        if(node == null)
            return "NULL";

        return numbers.computeIfAbsent(SparqlTest.getBoxedTerm(request, node), _ -> numbers.size() + 1).toString();
    }
}
