package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Node;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;



/**
 * Test data of a query evaluation test stored as a dictionary-encoded triple store keeps them: a table numbers the
 * terms, held in boxes, and a table of the quads holds the numbers of the graph (NULL for the default graph), the
 * subject, the predicate and the object of each quad. The quads are mapped by join mappings that join the table of the
 * quads to the table of the terms once for every term of a quad, so the predicate and the graph are columns of joined
 * tables too.
 */
public final class NumberedQuadTable
{
    /**
     * How the tables are queried.
     */
    public enum Variant
    {
        /**
         * The table of the terms is a table like any other: the queries read it for their constants too, and the
         * planner of the database chooses the joins.
         */
        PLAIN,

        /**
         * The table of the terms is a dictionary table, whose rows the translation reads for the constants of a query,
         * and the join optimizer of the engine chooses the joins.
         */
        DICTIONARY
    }


    /**
     * Table of the terms of the current test with their numbers.
     */
    public static final DatabaseTable termTable = new DatabaseTable("sparql_test", "terms");

    /**
     * Table of the quads of the current test as the numbers of their terms.
     */
    public static final DatabaseTable quadTable = new DatabaseTable("sparql_test", "numbered_quads");


    /**
     * Not instantiable.
     */
    private NumberedQuadTable()
    {
    }


    /**
     * Creates the two tables unless they exist: the terms are unique and numbered by the primary key, which the columns
     * of the quads reference.
     *
     * @param statement statement of the test database
     * @throws SQLException on database errors
     */
    public static void create(Statement statement) throws SQLException
    {
        statement.execute("create table if not exists " + termTable
                + " (id int8 primary key, term sparql.rdfbox not null unique)");

        statement.execute("create table if not exists " + quadTable + " (g int8 references " + termTable + "(id), "
                + "s int8 not null references " + termTable + "(id), p int8 not null references " + termTable
                + "(id), o int8 not null references " + termTable + "(id))");
    }


    /**
     * Registers the quad mappings of the tables in the configuration: the quads of the default graph (those with a NULL
     * graph) by a mapping joining the table of the terms for the subject, the predicate and the object, the quads of
     * the named graphs by a mapping joining it for the graph as well.
     *
     * @param config the configuration, whose schema knows the tables
     * @param variant how the tables are queried
     */
    public static void map(SparqlDatabaseConfiguration config, Variant variant)
    {
        TermMapping term = config.createTermMapping(box, "term");
        TableColumn id = new TableColumn("id", INT8);

        List<JoinColumns> joins = new ArrayList<>();
        joins.add(new JoinColumns(0, new TableColumn("s", INT8), 1, id));
        joins.add(new JoinColumns(0, new TableColumn("p", INT8), 2, id));
        joins.add(new JoinColumns(0, new TableColumn("o", INT8), 3, id));

        List<SourceTable> tables = new ArrayList<>(Collections.nCopies(4, termTable));
        tables.set(0, quadTable);

        List<Conditions> conditions = new ArrayList<>(Collections.nCopies(4, new Conditions(true)));
        conditions.set(0, config.createIsNullCondition(quadTable, "g"));

        config.addQuadMapping(List.copyOf(tables), List.copyOf(joins), 0, null, 1, term, 2, term, 3, term, conditions,
                Collections.nCopies(4, false));

        tables.add(termTable);
        joins.add(new JoinColumns(0, new TableColumn("g", INT8), 4, id));

        config.addQuadMapping(tables, joins, 4, term, 1, term, 2, term, 3, term);

        if(variant == Variant.DICTIONARY)
        {
            config.addDictionaryTable(termTable);
            config.setJoinOptimizer(true);
        }
    }


    /**
     * Replaces the content of the tables by the quads, a null graph standing for the default graph: the terms are
     * numbered in the order of their first occurrence, each boxed as the engine boxes a constant of its class.
     *
     * @param request the current request
     * @param quads the quads
     * @throws SQLException on database errors
     */
    public static void store(Request request, List<Quad> quads) throws SQLException
    {
        Map<String, Integer> numbers = new LinkedHashMap<>();
        List<String> rows = new ArrayList<>(quads.size());

        for(Quad quad : quads)
            rows.add("(" + getNumber(request, numbers, quad.graph()) + ", "
                    + getNumber(request, numbers, quad.subject()) + ", " + getNumber(request, numbers, quad.predicate())
                    + ", " + getNumber(request, numbers, quad.object()) + ")");

        try(Connection connection = SparqlTest.connectionPool.getConnection();
                Statement statement = connection.createStatement())
        {
            statement.execute("truncate " + quadTable + ", " + termTable);

            if(!quads.isEmpty())
            {
                List<String> terms = new ArrayList<>(numbers.size());
                numbers.forEach((term, number) -> terms.add("(" + number + ", " + term + ")"));

                statement.execute("insert into " + termTable + " (id, term) values " + String.join(", ", terms));
                statement.execute("insert into " + quadTable + " (g, s, p, o) values " + String.join(", ", rows));
            }

            // the join optimizer of the engine estimates the joins from the statistics of the tables
            statement.execute("analyze " + termTable);
            statement.execute("analyze " + quadTable);
        }
    }


    /**
     * SQL code of the number of a term of the data, which gets the next number when it occurs for the first time; NULL
     * for a null node.
     *
     * @param request the current request
     * @param numbers the numbers of the terms so far, by the SQL expressions of their boxes
     * @param node the term
     * @return SQL code of the number of the term
     */
    private static String getNumber(Request request, Map<String, Integer> numbers, Node node)
    {
        if(node == null)
            return "NULL";

        return numbers.computeIfAbsent(SparqlTest.getBoxedTerm(request, node), _ -> numbers.size() + 1).toString();
    }
}
