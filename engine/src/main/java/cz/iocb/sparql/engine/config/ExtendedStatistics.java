package cz.iocb.sparql.engine.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import cz.iocb.sparql.engine.database.CatalogStatistics;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.ParametrisedMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * The extended statistics of the database ({@code CREATE STATISTICS}) that the engine needs for the tables a
 * configuration maps, with the statements making them. The join optimizer takes the distinct values of a variable held
 * in several columns for the distinct combinations of the values of those columns, which only extended statistics keep
 * (see {@link CatalogStatistics}), whereas the statistics the database keeps of every column by itself give the values
 * of each column alone. They are needed for the columns of a table that hold one term of a quad mapping together, like
 * the parts of an IRI, and for the columns by which a join mapping joins two of its tables: at least two columns of the
 * table, no expression among them, no more than a statistics object takes (eight), and no unique key of the table among
 * them, as the engine knows that the rows of a key have a value of their own each. The statistics of a set of columns
 * are kept by the statistics of a larger set of the same table, which hold all the combinations of their columns. Every
 * statistics object keeps the numbers of the distinct combinations ({@code ndistinct}), which the engine and the
 * planner of the database read for the joins and the groups, and the functional dependencies among the columns
 * ({@code dependencies}), by which the planner estimates the rows of a constant term held in the columns.
 * <p>
 * The statements are idempotent (see {@link #getStatements}, {@link #getScript} and {@link #apply}), so that they may
 * be run again whenever the mapping changes: in one transaction they drop the statistics objects that an earlier run
 * has made (which a comment marks, see {@link #MARKER}) on the mapped tables and that are no longer needed or are
 * needed otherwise, make the missing ones in the schema of their table (named after the table and the columns, as the
 * database names a statistics object made without a name), analyse the tables whose needed statistics have not been
 * computed yet, and, when the configuration names the function through which the engine reads the statistics (see
 * {@link SparqlDatabaseConfiguration#setExtendedStatisticsFunction}), create or replace that function, in an existing
 * schema, and grant it to the given roles. They are to be run by the owner of the tables: only the owner may create
 * their statistics, and the function, declared {@code SECURITY DEFINER}, sees the statistics of the tables its owner
 * owns. The database computes the statistics anew whenever it analyses a table, so the statements need not be run when
 * only the data change.
 */
public final class ExtendedStatistics
{
    /**
     * Comment marking the statistics objects the statements make.
     */
    public static final String MARKER = "extended statistics of the columns of the terms mapped for the SPARQL engine";

    /**
     * Most columns a statistics object of the database takes.
     */
    private static final int MAX_COLUMNS = 8;

    /**
     * Most bytes of an identifier of the database.
     */
    private static final int MAX_NAME_LENGTH = 63;

    /**
     * Statement creating the temporary table of the mapped tables and of their needed statistics, which the
     * synchronization reads.
     */
    private static final String TABLE_STATEMENT = """
            CREATE TEMPORARY TABLE sparql_extended_statistics (table_id regclass NOT NULL, statistics_name name, \
            column_names name[]) ON COMMIT DROP""";

    /**
     * Statement creating the temporary function telling whether a statistics object is the needed one: of the table, of
     * the columns and of the kinds the statistics are made of.
     */
    private static final String DEFINITION_STATEMENT = """
            CREATE OR REPLACE FUNCTION pg_temp.sparql_is_defined(object_id oid, table_id regclass, column_names name[])
            RETURNS boolean LANGUAGE sql STABLE AS $$
                SELECT x.stxrelid = table_id AND x.stxexprs IS NULL
                    AND x.stxkind @> '{d,f}'::"char"[] AND x.stxkind <@ '{d,f}'::"char"[]
                    AND cardinality(x.stxkeys::int2[]) = cardinality(column_names)
                    AND ARRAY(SELECT k FROM unnest(x.stxkeys::int2[]) AS k ORDER BY k)
                        = ARRAY(SELECT a.attnum FROM pg_attribute a WHERE a.attrelid = table_id
                            AND a.attname = ANY(column_names) AND NOT a.attisdropped ORDER BY a.attnum)
                FROM pg_statistic_ext x WHERE x.oid = object_id
            $$""";

    /**
     * Statement dropping the statistics objects made before that are not needed, making the missing ones and analysing
     * the tables whose needed statistics have not been computed yet; an object of a needed name that is no such object
     * and has not been made by the statements is an error.
     */
    private static final String SYNCHRONIZATION_STATEMENT = """
            DO $$
            DECLARE
                marker CONSTANT text := @marker@;
                item record;
            BEGIN
                FOR item IN
                    SELECT x.stxnamespace::regnamespace AS namespace, x.stxname AS name
                    FROM pg_statistic_ext x
                    WHERE x.stxrelid IN (SELECT s.table_id FROM pg_temp.sparql_extended_statistics s)
                        AND obj_description(x.oid, 'pg_statistic_ext') = marker
                        AND NOT EXISTS (SELECT FROM pg_temp.sparql_extended_statistics s
                            WHERE s.table_id = x.stxrelid AND s.statistics_name = x.stxname
                                AND pg_temp.sparql_is_defined(x.oid, s.table_id, s.column_names))
                LOOP
                    EXECUTE format('DROP STATISTICS %s.%I', item.namespace, item.name);
                END LOOP;

                FOR item IN
                    SELECT s.table_id, s.statistics_name AS name, s.column_names,
                        c.relnamespace::regnamespace AS namespace,
                        (SELECT x.oid FROM pg_statistic_ext x
                            WHERE x.stxnamespace = c.relnamespace AND x.stxname = s.statistics_name) AS existing
                    FROM pg_temp.sparql_extended_statistics s JOIN pg_class c ON c.oid = s.table_id
                    WHERE s.statistics_name IS NOT NULL
                LOOP
                    IF item.existing IS NULL THEN
                        EXECUTE format('CREATE STATISTICS %s.%I (ndistinct, dependencies) ON %s FROM %s',
                            item.namespace, item.name,
                            (SELECT string_agg(quote_ident(n), ', ') FROM unnest(item.column_names) AS n),
                            item.table_id);
                    ELSIF NOT pg_temp.sparql_is_defined(item.existing, item.table_id, item.column_names) THEN
                        RAISE EXCEPTION 'statistics object %.% exists with another definition',
                            item.namespace, item.name;
                    END IF;

                    EXECUTE format('COMMENT ON STATISTICS %s.%I IS %L', item.namespace, item.name, marker);
                END LOOP;

                FOR item IN
                    SELECT DISTINCT s.table_id
                    FROM pg_temp.sparql_extended_statistics s JOIN pg_class c ON c.oid = s.table_id
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE s.statistics_name IS NOT NULL AND NOT EXISTS (SELECT FROM pg_stats_ext e
                        WHERE e.statistics_schemaname = n.nspname AND e.statistics_name = s.statistics_name)
                LOOP
                    EXECUTE format('ANALYZE %s', item.table_id);
                END LOOP;
            END $$""".replace("@marker@", ValueColumn.quote(MARKER));

    /**
     * Body of the function through which the engine reads the statistics: the text forms of the {@code n_distinct} of
     * the extended statistics of a table that the role logged in to the database may read.
     */
    private static final String FUNCTION_BODY = """
            (schema_name name, table_name name) RETURNS SETOF text
            LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp AS $$
                SELECT n_distinct::text FROM pg_stats_ext
                WHERE schemaname = schema_name AND tablename = table_name AND n_distinct IS NOT NULL
                    AND has_table_privilege(session_user, format('%I.%I', schema_name, table_name), 'SELECT')
            $$""";

    /**
     * How to run the tool from the command line.
     */
    private static final String USAGE = """
            usage: java cz.iocb.sparql.engine.config.ExtendedStatistics [options] <configuration class> <JDBC URL>
              prints the psql script making the extended statistics the configuration needs, or makes them
              the configuration class has a public (String service, DataSource, DatabaseSchema) constructor
              the JDBC URL may carry the user and the password (?user=...&password=...), or ~/.pgpass gives it
            options:
              --apply                    make the statistics in the database instead of printing the script
              --service <IRI>            the IRI of the service given to the configuration
              --function <schema.name>   the function the engine reads the statistics through
              --reader <role>            a role the function is granted to (repeatable)""";

    /**
     * Extended statistics of a set of columns of a table.
     *
     * @param table the table
     * @param name the name of the statistics object, in the schema of the table
     * @param columns the names of the columns, in the order of their names
     */
    public record Definition(DatabaseTable table, String name, List<String> columns)
    {
    }


    /**
     * Name of the class of the configuration.
     */
    private final String configuration;

    /**
     * The mapped tables of the database, in the order of their schemas and names.
     */
    private final List<DatabaseTable> tables;

    /**
     * The needed statistics, in the order of their tables and names.
     */
    private final List<Definition> definitions;

    /**
     * The function through which the engine reads the statistics, or null.
     */
    private final Function function;

    /**
     * The roles the function is granted to.
     */
    private final List<String> readers;


    /**
     * Finds the extended statistics the configuration needs for the tables it maps.
     *
     * @param config the configuration
     * @param readers the roles the function through which the engine reads the statistics is to be granted to, if the
     *            configuration names one (see {@link SparqlDatabaseConfiguration#setExtendedStatisticsFunction})
     */
    public ExtendedStatistics(SparqlDatabaseConfiguration config, List<String> readers)
    {
        DatabaseSchema schema = config.getDatabaseSchema();
        Set<DatabaseTable> mapped = new TreeSet<>(
                Comparator.comparing(DatabaseTable::getSchema).thenComparing(DatabaseTable::getName));
        Map<DatabaseTable, Set<Set<String>>> needed = new HashMap<>();

        for(Iri service : config.getServices())
        {
            for(QuadMapping mapping : config.getMappings(service))
            {
                if(mapping instanceof SingleTableQuadMapping single)
                {
                    addTable(mapped, single.getTable());

                    for(TermMapping term : getTerms(mapping))
                        addTerm(schema, needed, single.getTable(), term);
                }
                else if(mapping instanceof JoinTableQuadMapping join)
                {
                    List<SourceTable> joined = join.getTables();

                    for(SourceTable table : joined)
                        addTable(mapped, table);

                    if(mapping.getGraph() != null)
                        addTerm(schema, needed, joined.get(join.getGraphTableIdx()), mapping.getGraph());

                    addTerm(schema, needed, joined.get(join.getSubjectTableIdx()), mapping.getSubject());
                    addTerm(schema, needed, joined.get(join.getPredicateTableIdx()), mapping.getPredicate());
                    addTerm(schema, needed, joined.get(join.getObjectTableIdx()), mapping.getObject());

                    for(JoinColumns pair : join.getJoinColumnsPairs())
                    {
                        addColumns(schema, needed, joined.get(pair.getLeftTable()), pair.getLeftColumns());
                        addColumns(schema, needed, joined.get(pair.getRightTable()), pair.getRightColumns());
                    }
                }
            }
        }

        List<Definition> definitions = new ArrayList<>();

        for(DatabaseTable table : mapped)
        {
            Set<Set<String>> sets = needed.getOrDefault(table, Set.of());
            List<Definition> kept = new ArrayList<>();

            // a set of columns is kept by the statistics of a larger set holding it
            for(Set<String> set : sets)
            {
                if(sets.stream().noneMatch(other -> other.size() > set.size() && other.containsAll(set)))
                {
                    List<String> columns = set.stream().sorted().toList();
                    kept.add(new Definition(table, getName(table, columns), columns));
                }
            }

            kept.sort(Comparator.comparing(Definition::name));
            definitions.addAll(kept);
        }

        this.configuration = config.getClass().getName();
        this.tables = List.copyOf(mapped);
        this.definitions = List.copyOf(definitions);
        this.function = config.getExtendedStatisticsFunction();
        this.readers = List.copyOf(readers);
    }


    /**
     * The mapped tables of the database, in the order of their schemas and names; the statements drop the statistics
     * objects made before on them that are no longer needed.
     *
     * @return the mapped tables of the database
     */
    public List<DatabaseTable> getTables()
    {
        return tables;
    }


    /**
     * The needed statistics, in the order of their tables and names.
     *
     * @return the needed statistics
     */
    public List<Definition> getDefinitions()
    {
        return definitions;
    }


    /**
     * The statements making the needed statistics and the function, to be run in one transaction by the owner of the
     * tables (see {@link #apply}); they may be run again.
     *
     * @return the statements making the needed statistics and the function
     */
    public List<String> getStatements()
    {
        List<String> statements = new ArrayList<>();

        statements.add(TABLE_STATEMENT);

        if(!tables.isEmpty())
        {
            List<String> rows = new ArrayList<>();

            for(DatabaseTable table : tables)
            {
                List<Definition> statistics = definitions.stream().filter(d -> d.table().equals(table)).toList();

                // a mapped table needing no statistics, whose statistics made before are dropped
                if(statistics.isEmpty())
                    rows.add("(" + literal(table.toString()) + ", NULL, NULL)");

                for(Definition definition : statistics)
                    rows.add("(" + literal(table.toString()) + ", " + literal(definition.name()) + ", ARRAY["
                            + String.join(", ", definition.columns().stream().map(ExtendedStatistics::literal).toList())
                            + "]::name[])");
            }

            String values = String.join(",\n    ", rows);

            statements.add("INSERT INTO pg_temp.sparql_extended_statistics VALUES\n    " + values);
        }

        statements.add(DEFINITION_STATEMENT);
        statements.add(SYNCHRONIZATION_STATEMENT);

        if(function != null)
        {
            String signature = function + "(name, name)";

            statements.add("CREATE OR REPLACE FUNCTION " + function + FUNCTION_BODY);
            statements.add("COMMENT ON FUNCTION " + signature + " IS "
                    + literal("n_distinct of the extended statistics of a table, read by the SPARQL engine"));
            statements.add("REVOKE ALL ON FUNCTION " + signature + " FROM PUBLIC");

            for(String reader : readers)
            {
                statements
                        .add("GRANT USAGE ON SCHEMA " + identifier(function.getSchema()) + " TO " + identifier(reader));
                statements.add("GRANT EXECUTE ON FUNCTION " + signature + " TO " + identifier(reader));
            }
        }

        return statements;
    }


    /**
     * The psql script making the needed statistics and the function in one transaction (see {@link #getStatements}),
     * stopping at the first error; it may be run again.
     *
     * @return the psql script making the needed statistics and the function
     */
    public String getScript()
    {
        StringBuilder script = new StringBuilder();

        script.append("""
                -- The extended statistics of the columns holding the terms of the SPARQL mapping together, made for
                -- %s by %s.
                -- Run it as the owner of the tables (psql -f <file>), and again whenever the mapping changes: it makes
                -- the missing statistics, drops those it has made before that are no longer needed and analyses the
                -- tables whose statistics have not been computed yet.
                --
                -- needed statistics:
                """.formatted(configuration, ExtendedStatistics.class.getName()));

        if(definitions.isEmpty())
            script.append("--     none\n");

        for(Definition definition : definitions)
            script.append("--     " + definition.table() + " (" + String.join(", ", definition.columns()) + ")\n");

        if(function != null)
            script.append("--\n-- The engine reads the statistics through " + function + ", which the configuration "
                    + "names\n-- (statistics-function=" + function.getSchema() + "." + function.getName() + ").\n");

        script.append("\n\\set ON_ERROR_STOP on\n\nBEGIN;\n\n");

        for(String statement : getStatements())
            script.append(statement).append(";\n\n");

        script.append("COMMIT;\n");

        return script.toString();
    }


    /**
     * Runs the statements making the needed statistics and the function in one transaction on the connection, as the
     * owner of the tables; the connection keeps its setting of the automatic commits.
     *
     * @param connection the connection
     * @throws SQLException if a statement fails, which rolls the transaction back
     */
    public void apply(Connection connection) throws SQLException
    {
        boolean autoCommit = connection.getAutoCommit();

        try
        {
            connection.setAutoCommit(false);

            try(Statement statement = connection.createStatement())
            {
                for(String sql : getStatements())
                    statement.execute(sql);

                connection.commit();
            }
            catch(SQLException e)
            {
                connection.rollback();
                throw e;
            }
        }
        finally
        {
            connection.setAutoCommit(autoCommit);
        }
    }


    /**
     * Prints the psql script making the extended statistics a configuration needs (see {@link #getScript}), or makes
     * them in the database (see {@link #apply}); the configuration is created as the endpoint creates it, through its
     * {@code (String service, DataSource, DatabaseSchema)} constructor, over the database of a JDBC URL.
     *
     * @param args the options, the name of the class of the configuration and the JDBC URL of the database
     * @throws Exception if the configuration cannot be created or the database fails
     */
    public static void main(String[] args) throws Exception
    {
        boolean apply = false;
        String service = null;
        String function = null;
        List<String> readers = new ArrayList<>();
        List<String> arguments = new ArrayList<>();

        for(int i = 0; i < args.length; i++)
        {
            switch(args[i])
            {
                case "--apply" -> apply = true;
                case "--service" -> service = getValue(args, ++i);
                case "--function" -> function = getValue(args, ++i);
                case "--reader" -> readers.add(getValue(args, ++i));
                default ->
                {
                    if(args[i].startsWith("--"))
                        usage();

                    arguments.add(args[i]);
                }
            }
        }

        int dot = function != null ? function.indexOf('.') : -1;

        if(arguments.size() != 2 || function != null && (dot <= 0 || dot == function.length() - 1))
            usage();

        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(arguments.get(1));

        SparqlDatabaseConfiguration config = (SparqlDatabaseConfiguration) Class.forName(arguments.get(0))
                .getConstructor(String.class, DataSource.class, DatabaseSchema.class)
                .newInstance(service, source, new DatabaseSchema(source));

        if(function != null)
            config.setExtendedStatisticsFunction(function.substring(0, dot), function.substring(dot + 1));

        ExtendedStatistics statistics = new ExtendedStatistics(config, readers);

        if(apply)
        {
            try(Connection connection = source.getConnection())
            {
                statistics.apply(connection);
            }
        }
        else
        {
            System.out.print(statistics.getScript());
        }
    }


    /**
     * The value of an option, the argument following it; a missing one ends the program with the usage.
     *
     * @param args the arguments
     * @param index the index of the value among the arguments
     * @return the value of the option
     */
    private static String getValue(String[] args, int index)
    {
        if(index >= args.length)
            usage();

        return args[index];
    }


    /**
     * Prints how to run the tool and ends the program.
     */
    private static void usage()
    {
        System.err.println(USAGE);
        System.exit(2);
    }


    /**
     * Adds the table to the mapped tables when it is a table of the database.
     *
     * @param mapped the mapped tables of the database
     * @param table the table
     */
    private static void addTable(Set<DatabaseTable> mapped, SourceTable table)
    {
        if(table instanceof DatabaseTable databaseTable)
            mapped.add(databaseTable);
    }


    /**
     * The terms of the quad mapping, its graph left out when it maps the default graph.
     *
     * @param mapping the quad mapping
     * @return the terms of the quad mapping
     */
    private static List<TermMapping> getTerms(QuadMapping mapping)
    {
        List<TermMapping> terms = new ArrayList<>();

        if(mapping.getGraph() != null)
            terms.add(mapping.getGraph());

        terms.add(mapping.getSubject());
        terms.add(mapping.getPredicate());
        terms.add(mapping.getObject());

        return terms;
    }


    /**
     * Adds the columns of the term to the sets of columns needing statistics when the term is held in columns of the
     * table (a constant term is held in none).
     *
     * @param schema the schema of the database
     * @param needed the sets of columns needing statistics, by table
     * @param table the table holding the term
     * @param term the term
     */
    private static void addTerm(DatabaseSchema schema, Map<DatabaseTable, Set<Set<String>>> needed, SourceTable table,
            TermMapping term)
    {
        if(term instanceof ParametrisedMapping)
            addColumns(schema, needed, table, term.getColumns(null));
    }


    /**
     * Adds the columns to the sets of columns needing statistics when they are at least two columns of a table of the
     * database, no more than a statistics object takes, with no expression among them, which the engine knows nothing
     * of, and without a unique key of the table; constants are left out.
     *
     * @param schema the schema of the database
     * @param needed the sets of columns needing statistics, by table
     * @param table the table
     * @param columns the columns
     */
    private static void addColumns(DatabaseSchema schema, Map<DatabaseTable, Set<Set<String>>> needed,
            SourceTable table, List<Column> columns)
    {
        if(!(table instanceof DatabaseTable databaseTable))
            return;

        Set<Column> held = new HashSet<>();

        for(Column column : columns)
        {
            if(column instanceof ExpressionColumn)
                return;

            if(column instanceof TableColumn)
                held.add(column);
        }

        if(held.size() < 2 || held.size() > MAX_COLUMNS || schema.getCompatibleKey(table, held) != null)
            return;

        Set<String> names = new HashSet<>();

        for(Column column : held)
            names.add(((TableColumn) column).getName());

        needed.computeIfAbsent(databaseTable, _ -> new HashSet<>()).add(names);
    }


    /**
     * The name of the statistics object of the columns of the table: the names of the table and of the columns joined
     * by underscores and ended by {@code _stat}, as the database names a statistics object made without a name, cut to
     * the length of an identifier and ended by a hash of the whole name when it is longer.
     *
     * @param table the table
     * @param columns the names of the columns
     * @return the name of the statistics object of the columns of the table
     */
    private static String getName(DatabaseTable table, List<String> columns)
    {
        String name = table.getName() + "_" + String.join("_", columns) + "_stat";

        if(name.getBytes(UTF_8).length <= MAX_NAME_LENGTH)
            return name;

        String hash = "_" + HexFormat.of().toHexDigits(name.hashCode());
        StringBuilder cut = new StringBuilder();
        int length = hash.length();

        for(int codePoint : name.codePoints().toArray())
        {
            String character = Character.toString(codePoint);
            length += character.getBytes(UTF_8).length;

            if(length > MAX_NAME_LENGTH)
                break;

            cut.append(character);
        }

        return cut + hash;
    }


    /**
     * SQL string literal of the text.
     *
     * @param text the text
     * @return SQL string literal of the text
     */
    private static String literal(String text)
    {
        return ValueColumn.quote(text);
    }


    /**
     * SQL identifier of the name, double-quoted.
     *
     * @param name the name
     * @return SQL identifier of the name
     */
    private static String identifier(String name)
    {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
