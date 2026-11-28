package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.config.SettingValues.checkDuration;
import static cz.iocb.sparql.engine.config.SettingValues.checkFraction;
import static cz.iocb.sparql.engine.config.SettingValues.checkPositive;
import static cz.iocb.sparql.engine.config.SettingValues.checkRange;
import static cz.iocb.sparql.engine.config.SettingValues.parseBoolean;
import static cz.iocb.sparql.engine.config.SettingValues.parseDouble;
import static cz.iocb.sparql.engine.config.SettingValues.parseDuration;
import static cz.iocb.sparql.engine.config.SettingValues.parseEnum;
import static cz.iocb.sparql.engine.config.SettingValues.parseFunction;
import static cz.iocb.sparql.engine.config.SettingValues.parseInt;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Function;



/**
 * Settings of the engine for the requests of a configuration (see
 * {@link SparqlDatabaseConfiguration#getEngineSettings}): whether the engine chooses the joins and how the planner
 * keeps them, the costs and the margins by which the join optimizer chooses the joins, the estimates it takes where
 * nothing is known, the limits beyond which the translation leaves a transformation of the intermediate code out, how
 * the statistics of the tables are read, kept and renewed, how the SERVICE calls are ordered, how their answers are
 * received and their solutions stored, and how the rows of a result are read. A request keeps the settings it has begun
 * with (see {@link cz.iocb.sparql.engine.request.Request#getEngineSettings}). The settings are immutable: the
 * {@code with} methods derive changed copies, checking the values given. The default of a setting, which
 * {@link #DEFAULT} holds, is the initial value of its field, whose description tells why the value is what it is where
 * there is a reason for it. Each setting has a name, the name of its accessor written in lower case with its words
 * joined by hyphens, like {@code lookup-cost}, by which an attribute of the resource of the configuration in
 * {@code context.xml} sets it to the value its text gives (see {@link #with(String, String)}).
 */
public final class EngineSettings implements Cloneable
{
    /**
     * Who chooses the order and the methods of the joins the engine generates, and how the planner of the database is
     * made to keep them.
     */
    public enum JoinOptimizerMode
    {
        /**
         * The planner chooses the joins.
         */
        OFF,

        /**
         * The engine chooses the joins (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer}), and the statement runs
         * under settings that make the planner keep the order of the joins as written and hash every join it can; an
         * index lookup is written as a lateral subquery fenced by {@code OFFSET 0}, which the database evaluates in the
         * process reading the outer rows, or, where the database may evaluate the statement by parallel workers, merged
         * into the join and compared by conditions it cannot hash (see
         * {@link cz.iocb.sparql.engine.request.Request#isParallel}).
         */
        SETTINGS,

        /**
         * The engine chooses the joins, and plan advice makes the planner keep them: the lookups are plain joins of the
         * statement, which the database can hand to the workers and cache by the values they are looked up by
         * (Memoize), and the advice of the module {@code pg_plan_advice} makes the planner look them up by nested loops
         * (see {@link cz.iocb.sparql.engine.database.PlanAdvice}); a statement with a lookup is planned once more for
         * it, for the planner to tell the identifiers of its relations, and once again when a lookup is memoized. The
         * module comes with PostgreSQL 19 and later and has to be loaded in the sessions of the data source, by
         * {@code session_preload_libraries} or {@code shared_preload_libraries}, as only a superuser may load it in a
         * session. The tables looked up are given aliases of their own, so the expressions of the mappings must not
         * qualify the columns by the names of their tables.
         */
        ADVICE
    }


    /**
     * The default settings.
     */
    public static final EngineSettings DEFAULT = new EngineSettings();

    /**
     * The names of the settings, in the order of their accessors.
     */
    public static final List<String> NAMES = List.of("join-optimizer", "lookup-cost", "lookup-row-cost", "hash-cost",
            "comparison-cost", "pairwise-margin", "second-join-margin", "memoize-ratio", "exhaustive-join-limit",
            "counted-join-ratio", "default-rows", "equality-selectivity", "inequality-selectivity",
            "default-selectivity", "disjunct-limit", "box-comparison-limit", "triple-term-class-limit",
            "exists-round-limit", "statistics-function", "statistics-check-interval", "statistics-cache-limit",
            "service-reorder", "service-table-rows", "service-batch-size", "service-check-interval",
            "service-part-size", "service-read-ahead", "copy-drain-time", "copy-cancel-interval", "result-check-rows");

    /**
     * Who chooses the order and the methods of the joins the engine generates: when the engine does, it chooses them
     * from the statistics of the tables (see {@link SparqlDatabaseConfiguration#getTableStatistics}) and makes the
     * planner keep them, as the planner estimates every join of a table holding many kinds of rows, such as a table of
     * all the triples of a dataset, at one row and nests the joins in a wrong order then; the planner chooses them by
     * default.
     */
    private JoinOptimizerMode joinOptimizer = JoinOptimizerMode.OFF;

    /**
     * Cost of the lookup of the rows matching one outer row by an index, in units of one row scanned or hashed, like
     * the other costs of the join optimizer, which are calibrated on the NeXtProt queries over a single table of
     * triples (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer}).
     */
    private double lookupCost = 8;

    /**
     * Cost of a row that a lookup by an index finds, whereas a row that a hash join gives costs 1.
     */
    private double lookupRowCost = 2;

    /**
     * Cost of hashing one row of the smaller side of a hash join; the probe of one row of the other side costs 1.
     */
    private double hashCost = 2.5;

    /**
     * Cost of comparing one pair of rows when the sides of a join have to be compared pairwise.
     */
    private double comparisonCost = 0.3;

    /**
     * How many times cheaper the lookups of the solutions binding a variable have to be than the pairwise comparison of
     * the sides for the solutions to be told apart (see
     * {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#planLeftJoin}): the database scans a table of a few pages whole
     * instead of using its index, so a lookup in a small table costs what the comparison with its rows does, and the
     * margin leaves such right sides compared pairwise. The same margin applies to the choice of how a pattern is
     * tested (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#planTest}).
     */
    private double pairwiseMargin = 16;

    /**
     * How many times more the comparisons spared by hashing the solutions of the left side of an OPTIONAL that bind a
     * variable apart from the others have to cost than the second join it takes (see
     * {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#planLeftJoin}). The costs of the two are rough estimates, and
     * the right side is then kept for the joins to read it twice, which they do not count.
     */
    private double secondJoinMargin = 2;

    /**
     * How many times a table access has to be looked up for every combination of the values it is looked up by, on
     * average, for the database to keep the rows found for the values (Memoize) where plan advice makes it look the
     * access up (see {@link cz.iocb.sparql.engine.database.PlanAdvice#MEMOIZED_LOOKUP}): a lookup found in the cache
     * costs a part of an index lookup, one that is not costs about half a lookup more, so the cache pays when about a
     * third of the lookups find their rows there. The step of a walk departing from several starts is memoized when as
     * many starts are estimated, as so many may reach a node (see
     * {@link cz.iocb.sparql.engine.imcode.SqlRecursive#isStepMemoized}).
     */
    private double memoizeRatio = 2;

    /**
     * Largest number of children of a join whose join trees are all tried (see
     * {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#planExhaustively}); the tree of a join of more children is
     * built greedily (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#planGreedily}), once the children that the
     * others determine have been left out (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#getDetermined}).
     * Trying the trees of n children takes time growing as 3 to the power of n and keeps the sizes and the best trees
     * of the joins of all the 2 to the power of n subsets of the children in arrays, which an {@code int} indexes, so
     * the limit is at most 30.
     */
    private int exhaustiveJoinLimit = 15;

    /**
     * How many times more solutions a join has to be estimated to have than its counted children together with the join
     * of their counts for its solutions to be counted by the counts of its children (see
     * {@link cz.iocb.sparql.engine.imcode.SqlAggregation#countJoin}). The counted children are then each read and
     * grouped once and their counts are joined with the other children, whereas the join hands every solution it makes
     * to the count: with five workers, the join took 46 ns for a solution it made and the counted children 89 ns for a
     * solution they grouped, so the counts pay from about twice as many solutions, and the margin covers an estimate
     * that is off by half.
     */
    private double countedJoinRatio = 4;

    /**
     * Rows of a table, or of a node of the intermediate code, nothing is known about: of a table that has not been
     * analysed (see {@link cz.iocb.sparql.engine.database.CatalogStatistics}), of a group of rows the statistics of a
     * table have nothing about (see {@link cz.iocb.sparql.engine.database.TableStatisticsBase}), and of a node the join
     * optimizer cannot estimate (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#estimateRows}).
     */
    private double defaultRows = 1000;

    /**
     * Selectivity of an equality nothing is known about (the default of the planner): of a column bound to a value when
     * the statistics of the table have nothing about the column (see
     * {@link cz.iocb.sparql.engine.database.TableStatisticsBase}), and of an equality in a filter condition the planner
     * is not asked about (see {@link cz.iocb.sparql.engine.imcode.JoinOptimizer#selectivity}), which a test of the
     * characters of a string and the equality with a member of a membership test count as too.
     */
    private double equalitySelectivity = 0.005;

    /**
     * Selectivity of an inequality in a filter condition the planner is not asked about (the default of the planner).
     */
    private double inequalitySelectivity = 1.0 / 3;

    /**
     * Selectivity of a filter condition nothing is known about, and the part of the solutions of its left side that a
     * difference is taken to keep, as nothing tells how many of them the right side removes (see
     * {@link cz.iocb.sparql.engine.imcode.SqlMinus}).
     */
    private double defaultSelectivity = 0.5;

    /**
     * The greatest number of disjuncts the conditions of a table access may get by a merge that multiplies them: by
     * taking in the rows of VALUES, or by the merge with another access to its table or to a table referencing it (see
     * {@link cz.iocb.sparql.engine.imcode.SqlTableAccess}). The conditions are kept in disjunctive normal form, so they
     * get a disjunct for every row of the VALUES and every disjunct they have, or for every pair of the disjuncts of
     * the two accesses that do not contradict each other, and building them, like every later operation on them,
     * compares every two disjuncts: the time grows with the square of their number, whereas the database joins the
     * access with a list left in VALUES, or the two accesses left apart, as fast as it evaluates the disjunction.
     */
    private int disjunctLimit = 30;

    /**
     * Number of argument class combinations above which the operands of a comparison are compared as boxes by a single
     * expression instead of combination by combination (see
     * {@link cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison}).
     */
    private int boxComparisonLimit = 256;

    /**
     * Number of triple term classes above which the function TRIPLE takes the components of the most classes in their
     * general classes instead of building a class for every combination of the component classes (see
     * {@link cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall}).
     */
    private int tripleTermClassLimit = 256;

    /**
     * Upper bound of the rounds in which the pattern of an EXISTS is optimised for a current solution (see
     * {@link cz.iocb.sparql.engine.imcode.expression.SqlExists}).
     */
    private int existsRoundLimit = 8;

    /**
     * The function through which the engine reads the numbers of the distinct combinations of the values of several
     * columns that the extended statistics of the database keep for a table ({@code CREATE STATISTICS} of the kind
     * {@code ndistinct}), like those of the columns of an IRI made of several parts (see
     * {@link cz.iocb.sparql.engine.database.CatalogStatistics}), or null, the default, when the engine reads the view
     * {@code pg_stats_ext} itself, which serves a role owning the tables. The database shows the view to the owner of
     * the table only, so a role that may only read the tables needs a function declared {@code SECURITY DEFINER} by
     * their owner: it takes the schema and the name of a table (both of the type {@code name}) and returns the text
     * forms of the {@code n_distinct} of the extended statistics of the table ({@code SETOF text}), like
     *
     * <pre>
     * CREATE FUNCTION stats.ndistinct(schema_name name, table_name name) RETURNS SETOF text
     * LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, pg_temp
     * AS $$ SELECT n_distinct::text FROM pg_stats_ext
     *     WHERE schemaname = schema_name AND tablename = table_name AND n_distinct IS NOT NULL
     *     AND has_table_privilege(session_user, format('%I.%I', schema_name, table_name), 'SELECT') $$;
     * REVOKE EXECUTE ON FUNCTION stats.ndistinct(name, name) FROM PUBLIC;
     * GRANT EXECUTE ON FUNCTION stats.ndistinct(name, name) TO reader;
     * </pre>
     *
     * which gives no more than the numbers, of the tables the role logged in to the database may read; the statements
     * of {@link ExtendedStatistics} make the function together with the statistics the configuration needs. The
     * statistics of a table are kept by the configuration once they have been read, so the function is to be set before
     * the configuration serves requests.
     */
    private Function statisticsFunction = null;

    /**
     * The least time between two checks of whether the statistics of the tables are to be renewed (see
     * {@link SparqlDatabaseConfiguration#checkTableStatistics}); zero has every request that asks for statistics check
     * them.
     */
    private Duration statisticsCheckInterval = Duration.ofMinutes(1);

    /**
     * Most values a cache of the statistics of a table keeps (see
     * {@link cz.iocb.sparql.engine.database.TableStatisticsBase}): far more than the values the queries of a deployment
     * keep asking about, so that only a stream of ever new constants fills a cache, which is then emptied instead of
     * growing for the life of the statistics; a frequency takes up some 140 to 280 bytes of a cache, the statistics of
     * a group more.
     */
    private int statisticsCacheLimit = 100_000;

    /**
     * Whether a request evaluates the SERVICE calls independently of their context and joins them afterwards, each
     * asked about as many of its shared variables as the context limit of its endpoint takes by the estimates (see
     * {@link cz.iocb.sparql.engine.request.Request#isServiceReorderEnabled}), when the request is not told otherwise
     * (see {@link cz.iocb.sparql.engine.request.Engine#getRequest(boolean)}), like by the parameter
     * {@code service-reorder} of a request of the endpoint; not by default.
     */
    private boolean serviceReorder = false;

    /**
     * Number of the solutions of a SERVICE call from which they are stored in a temporary table, which the query reads
     * as an ordinary table; fewer solutions are kept as inline data (see
     * {@link cz.iocb.sparql.engine.translator.StoredResultHandler}).
     */
    private int serviceTableRows = 1000;

    /**
     * Number of the solutions of a SERVICE call inserted into their temporary table by one statement (see
     * {@link cz.iocb.sparql.engine.translator.StoredResultHandler}); the first statement inserts at least
     * {@link #serviceTableRows} of them, as the solutions are kept until it is known whether they are stored.
     */
    private int serviceBatchSize = 1000;

    /**
     * Longest time a request waits for a part of the answer of an endpoint before it checks again that its time limit
     * has not elapsed and that it has not been cancelled (see {@link cz.iocb.sparql.engine.imcode.SqlServiceStub}).
     */
    private Duration serviceCheckInterval = Duration.ofMillis(100);

    /**
     * Largest part of the answer of an endpoint, in bytes, that the thread receiving the answer hands over to the
     * request at once (see {@link cz.iocb.sparql.engine.imcode.SqlServiceStub}).
     */
    private int servicePartSize = 65536;

    /**
     * Number of the parts of the answer of an endpoint that the thread receiving the answer reads ahead of the request
     * at most: it hands them over through a bounded queue, so that it reads ahead only a few parts (see
     * {@link cz.iocb.sparql.engine.imcode.SqlServiceStub}).
     */
    private int serviceReadAhead = 16;

    /**
     * Time for which the rows of a copy closed before their end read the rows the database has sent already, about what
     * a cancellation of the copy takes, as it opens a connection of its own (see
     * {@link cz.iocb.sparql.engine.request.CopyRows#close}).
     */
    private Duration copyDrainTime = Duration.ofMillis(2);

    /**
     * Time after which the cancellation of a copy is sent again until the database has started the copy: a cancellation
     * that comes before the statement of the copy reaches the database has no effect, and the driver tells that the
     * database has started the copy only when the database sends the first rows (see
     * {@link cz.iocb.sparql.engine.request.Request#cancel}).
     */
    private Duration copyCancelInterval = Duration.ofMillis(100);

    /**
     * Number of the rows a result reads between two checks of the time limit of its request; a result read in parts
     * checks the time limit once for a part at least (see {@link cz.iocb.sparql.engine.request.Result#next}).
     */
    private int resultCheckRows = 100;


    /**
     * Creates the default settings.
     */
    private EngineSettings()
    {
    }


    /**
     * Who chooses the order and the methods of the joins the engine generates (the setting {@code join-optimizer}).
     *
     * @return who chooses the joins and how the planner keeps them
     */
    public JoinOptimizerMode joinOptimizer()
    {
        return joinOptimizer;
    }


    /**
     * Copy with the given chooser of the order and the methods of the joins the engine generates.
     *
     * @param joinOptimizer who chooses the joins and how the planner keeps them
     * @return copy with the given chooser of the joins
     * @throws NullPointerException if the chooser is null
     */
    public EngineSettings withJoinOptimizer(JoinOptimizerMode joinOptimizer)
    {
        EngineSettings copy = copy();
        copy.joinOptimizer = Objects.requireNonNull(joinOptimizer, "join-optimizer");
        return copy;
    }


    /**
     * True if the engine chooses the order and the methods of the joins it generates (see {@link #joinOptimizer}).
     *
     * @return true if the engine chooses the joins, false if the planner does
     */
    public boolean isJoinOptimizerEnabled()
    {
        return joinOptimizer != JoinOptimizerMode.OFF;
    }


    /**
     * True if plan advice makes the planner keep the joins the engine chooses (see {@link JoinOptimizerMode#ADVICE}).
     *
     * @return true if plan advice makes the planner keep the joins, false otherwise
     */
    public boolean isPlanAdviceEnabled()
    {
        return joinOptimizer == JoinOptimizerMode.ADVICE;
    }


    /**
     * Cost of the lookup of the rows matching one outer row by an index (the setting {@code lookup-cost}).
     *
     * @return cost of the lookup of the rows matching one outer row by an index
     */
    public double lookupCost()
    {
        return lookupCost;
    }


    /**
     * Copy with the given cost of the lookup of the rows matching one outer row by an index.
     *
     * @param lookupCost cost of the lookup of the rows matching one outer row by an index
     * @return copy with the given cost
     * @throws IllegalArgumentException if the cost is not a positive finite number
     */
    public EngineSettings withLookupCost(double lookupCost)
    {
        EngineSettings copy = copy();
        copy.lookupCost = checkPositive("lookup-cost", lookupCost);
        return copy;
    }


    /**
     * Cost of a row that a lookup by an index finds (the setting {@code lookup-row-cost}).
     *
     * @return cost of a row that a lookup by an index finds
     */
    public double lookupRowCost()
    {
        return lookupRowCost;
    }


    /**
     * Copy with the given cost of a row that a lookup by an index finds.
     *
     * @param lookupRowCost cost of a row that a lookup by an index finds
     * @return copy with the given cost
     * @throws IllegalArgumentException if the cost is not a positive finite number
     */
    public EngineSettings withLookupRowCost(double lookupRowCost)
    {
        EngineSettings copy = copy();
        copy.lookupRowCost = checkPositive("lookup-row-cost", lookupRowCost);
        return copy;
    }


    /**
     * Cost of hashing one row of the smaller side of a hash join (the setting {@code hash-cost}).
     *
     * @return cost of hashing one row of the smaller side of a hash join
     */
    public double hashCost()
    {
        return hashCost;
    }


    /**
     * Copy with the given cost of hashing one row of the smaller side of a hash join.
     *
     * @param hashCost cost of hashing one row of the smaller side of a hash join
     * @return copy with the given cost
     * @throws IllegalArgumentException if the cost is not a positive finite number
     */
    public EngineSettings withHashCost(double hashCost)
    {
        EngineSettings copy = copy();
        copy.hashCost = checkPositive("hash-cost", hashCost);
        return copy;
    }


    /**
     * Cost of comparing one pair of rows when the sides of a join have to be compared pairwise (the setting
     * {@code comparison-cost}).
     *
     * @return cost of comparing one pair of rows
     */
    public double comparisonCost()
    {
        return comparisonCost;
    }


    /**
     * Copy with the given cost of comparing one pair of rows when the sides of a join have to be compared pairwise.
     *
     * @param comparisonCost cost of comparing one pair of rows
     * @return copy with the given cost
     * @throws IllegalArgumentException if the cost is not a positive finite number
     */
    public EngineSettings withComparisonCost(double comparisonCost)
    {
        EngineSettings copy = copy();
        copy.comparisonCost = checkPositive("comparison-cost", comparisonCost);
        return copy;
    }


    /**
     * How many times cheaper lookups have to be than a pairwise comparison for the solutions binding a variable to be
     * told apart (the setting {@code pairwise-margin}).
     *
     * @return how many times cheaper lookups have to be than a pairwise comparison
     */
    public double pairwiseMargin()
    {
        return pairwiseMargin;
    }


    /**
     * Copy with the given margin of lookups against a pairwise comparison.
     *
     * @param pairwiseMargin how many times cheaper lookups have to be than a pairwise comparison
     * @return copy with the given margin
     * @throws IllegalArgumentException if the margin is not a positive finite number
     */
    public EngineSettings withPairwiseMargin(double pairwiseMargin)
    {
        EngineSettings copy = copy();
        copy.pairwiseMargin = checkPositive("pairwise-margin", pairwiseMargin);
        return copy;
    }


    /**
     * How many times more the comparisons spared by hashing the solutions binding a variable apart have to cost than
     * the second join it takes (the setting {@code second-join-margin}).
     *
     * @return how many times more the comparisons spared have to cost than the second join
     */
    public double secondJoinMargin()
    {
        return secondJoinMargin;
    }


    /**
     * Copy with the given margin of the comparisons spared against the second join.
     *
     * @param secondJoinMargin how many times more the comparisons spared have to cost than the second join
     * @return copy with the given margin
     * @throws IllegalArgumentException if the margin is not a positive finite number
     */
    public EngineSettings withSecondJoinMargin(double secondJoinMargin)
    {
        EngineSettings copy = copy();
        copy.secondJoinMargin = checkPositive("second-join-margin", secondJoinMargin);
        return copy;
    }


    /**
     * How many times a table access has to be looked up for every combination of the values it is looked up by for the
     * rows found to be kept (the setting {@code memoize-ratio}).
     *
     * @return how many times an access has to be looked up for a combination of values for the rows to be kept
     */
    public double memoizeRatio()
    {
        return memoizeRatio;
    }


    /**
     * Copy with the given number of the lookups of a combination of values from which the rows found are kept.
     *
     * @param memoizeRatio how many times an access has to be looked up for a combination of values for the rows to be
     *            kept
     * @return copy with the given ratio
     * @throws IllegalArgumentException if the ratio is not a positive finite number
     */
    public EngineSettings withMemoizeRatio(double memoizeRatio)
    {
        EngineSettings copy = copy();
        copy.memoizeRatio = checkPositive("memoize-ratio", memoizeRatio);
        return copy;
    }


    /**
     * Largest number of children of a join whose join trees are all tried (the setting {@code exhaustive-join-limit}).
     *
     * @return largest number of children of a join whose join trees are all tried
     */
    public int exhaustiveJoinLimit()
    {
        return exhaustiveJoinLimit;
    }


    /**
     * Copy with the given largest number of children of a join whose join trees are all tried.
     *
     * @param exhaustiveJoinLimit largest number of children of a join whose join trees are all tried
     * @return copy with the given limit
     * @throws IllegalArgumentException if the limit is not between 1 and 30
     */
    public EngineSettings withExhaustiveJoinLimit(int exhaustiveJoinLimit)
    {
        EngineSettings copy = copy();
        copy.exhaustiveJoinLimit = checkRange("exhaustive-join-limit", exhaustiveJoinLimit, 1, 30);
        return copy;
    }


    /**
     * How many times more solutions a join has to be estimated to have than its counted children together with the join
     * of their counts for them to be counted so (the setting {@code counted-join-ratio}).
     *
     * @return how many times more solutions a join has to have for them to be counted by the counts of its children
     */
    public double countedJoinRatio()
    {
        return countedJoinRatio;
    }


    /**
     * Copy with the given ratio from which the solutions of a join are counted by the counts of its children.
     *
     * @param countedJoinRatio how many times more solutions a join has to have for them to be counted by the counts of
     *            its children
     * @return copy with the given ratio
     * @throws IllegalArgumentException if the ratio is not a positive finite number
     */
    public EngineSettings withCountedJoinRatio(double countedJoinRatio)
    {
        EngineSettings copy = copy();
        copy.countedJoinRatio = checkPositive("counted-join-ratio", countedJoinRatio);
        return copy;
    }


    /**
     * Rows of a table, or of a node of the intermediate code, nothing is known about (the setting
     * {@code default-rows}).
     *
     * @return rows of a table, or of a node, nothing is known about
     */
    public double defaultRows()
    {
        return defaultRows;
    }


    /**
     * Copy with the given rows of a table, or of a node of the intermediate code, nothing is known about.
     *
     * @param defaultRows rows of a table, or of a node, nothing is known about
     * @return copy with the given rows
     * @throws IllegalArgumentException if the rows are not a positive finite number
     */
    public EngineSettings withDefaultRows(double defaultRows)
    {
        EngineSettings copy = copy();
        copy.defaultRows = checkPositive("default-rows", defaultRows);
        return copy;
    }


    /**
     * Selectivity of an equality nothing is known about (the setting {@code equality-selectivity}).
     *
     * @return selectivity of an equality nothing is known about
     */
    public double equalitySelectivity()
    {
        return equalitySelectivity;
    }


    /**
     * Copy with the given selectivity of an equality nothing is known about.
     *
     * @param equalitySelectivity selectivity of an equality nothing is known about
     * @return copy with the given selectivity
     * @throws IllegalArgumentException if the selectivity is not a number greater than 0 and at most 1
     */
    public EngineSettings withEqualitySelectivity(double equalitySelectivity)
    {
        EngineSettings copy = copy();
        copy.equalitySelectivity = checkFraction("equality-selectivity", equalitySelectivity);
        return copy;
    }


    /**
     * Selectivity of an inequality in a filter condition the planner is not asked about (the setting
     * {@code inequality-selectivity}).
     *
     * @return selectivity of an inequality in a filter condition
     */
    public double inequalitySelectivity()
    {
        return inequalitySelectivity;
    }


    /**
     * Copy with the given selectivity of an inequality in a filter condition the planner is not asked about.
     *
     * @param inequalitySelectivity selectivity of an inequality in a filter condition
     * @return copy with the given selectivity
     * @throws IllegalArgumentException if the selectivity is not a number greater than 0 and at most 1
     */
    public EngineSettings withInequalitySelectivity(double inequalitySelectivity)
    {
        EngineSettings copy = copy();
        copy.inequalitySelectivity = checkFraction("inequality-selectivity", inequalitySelectivity);
        return copy;
    }


    /**
     * Selectivity of a filter condition nothing is known about (the setting {@code default-selectivity}).
     *
     * @return selectivity of a filter condition nothing is known about
     */
    public double defaultSelectivity()
    {
        return defaultSelectivity;
    }


    /**
     * Copy with the given selectivity of a filter condition nothing is known about.
     *
     * @param defaultSelectivity selectivity of a filter condition nothing is known about
     * @return copy with the given selectivity
     * @throws IllegalArgumentException if the selectivity is not a number greater than 0 and at most 1
     */
    public EngineSettings withDefaultSelectivity(double defaultSelectivity)
    {
        EngineSettings copy = copy();
        copy.defaultSelectivity = checkFraction("default-selectivity", defaultSelectivity);
        return copy;
    }


    /**
     * The greatest number of disjuncts the conditions of a table access may get by a merge that multiplies them (the
     * setting {@code disjunct-limit}).
     *
     * @return the greatest number of disjuncts the conditions of an access may get by a merge
     */
    public int disjunctLimit()
    {
        return disjunctLimit;
    }


    /**
     * Copy with the given greatest number of disjuncts the conditions of a table access may get by a merge.
     *
     * @param disjunctLimit the greatest number of disjuncts the conditions of an access may get by a merge
     * @return copy with the given limit
     * @throws IllegalArgumentException if the limit is not positive
     */
    public EngineSettings withDisjunctLimit(int disjunctLimit)
    {
        EngineSettings copy = copy();
        copy.disjunctLimit = checkRange("disjunct-limit", disjunctLimit, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Number of argument class combinations above which the operands of a comparison are compared as boxes (the setting
     * {@code box-comparison-limit}).
     *
     * @return number of combinations above which the operands of a comparison are compared as boxes
     */
    public int boxComparisonLimit()
    {
        return boxComparisonLimit;
    }


    /**
     * Copy with the given number of argument class combinations above which the operands of a comparison are compared
     * as boxes.
     *
     * @param boxComparisonLimit number of combinations above which the operands of a comparison are compared as boxes
     * @return copy with the given limit
     * @throws IllegalArgumentException if the limit is not positive
     */
    public EngineSettings withBoxComparisonLimit(int boxComparisonLimit)
    {
        EngineSettings copy = copy();
        copy.boxComparisonLimit = checkRange("box-comparison-limit", boxComparisonLimit, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Number of triple term classes above which the function TRIPLE takes the components of the most classes in their
     * general classes (the setting {@code triple-term-class-limit}).
     *
     * @return number of triple term classes above which TRIPLE takes components in their general classes
     */
    public int tripleTermClassLimit()
    {
        return tripleTermClassLimit;
    }


    /**
     * Copy with the given number of triple term classes above which the function TRIPLE takes the components of the
     * most classes in their general classes.
     *
     * @param tripleTermClassLimit number of triple term classes above which TRIPLE takes components in their general
     *            classes
     * @return copy with the given limit
     * @throws IllegalArgumentException if the limit is not positive
     */
    public EngineSettings withTripleTermClassLimit(int tripleTermClassLimit)
    {
        EngineSettings copy = copy();
        copy.tripleTermClassLimit = checkRange("triple-term-class-limit", tripleTermClassLimit, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Upper bound of the rounds in which the pattern of an EXISTS is optimised for a current solution (the setting
     * {@code exists-round-limit}).
     *
     * @return upper bound of the rounds in which the pattern of an EXISTS is optimised
     */
    public int existsRoundLimit()
    {
        return existsRoundLimit;
    }


    /**
     * Copy with the given upper bound of the rounds in which the pattern of an EXISTS is optimised for a current
     * solution.
     *
     * @param existsRoundLimit upper bound of the rounds in which the pattern of an EXISTS is optimised
     * @return copy with the given bound
     * @throws IllegalArgumentException if the bound is not positive
     */
    public EngineSettings withExistsRoundLimit(int existsRoundLimit)
    {
        EngineSettings copy = copy();
        copy.existsRoundLimit = checkRange("exists-round-limit", existsRoundLimit, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * The function through which the engine reads the extended statistics of the tables (the setting
     * {@code statistics-function}), or null when it reads the view {@code pg_stats_ext} itself.
     *
     * @return the function through which the engine reads the extended statistics of the tables, or null
     */
    public Function statisticsFunction()
    {
        return statisticsFunction;
    }


    /**
     * Copy with the given function through which the engine reads the extended statistics of the tables.
     *
     * @param statisticsFunction the function, or null when the engine reads the view {@code pg_stats_ext} itself
     * @return copy with the given function
     */
    public EngineSettings withStatisticsFunction(Function statisticsFunction)
    {
        EngineSettings copy = copy();
        copy.statisticsFunction = statisticsFunction;
        return copy;
    }


    /**
     * The least time between two checks of whether the statistics of the tables are to be renewed (the setting
     * {@code statistics-check-interval}).
     *
     * @return the least time between two checks of the statistics
     */
    public Duration statisticsCheckInterval()
    {
        return statisticsCheckInterval;
    }


    /**
     * Copy with the given least time between two checks of whether the statistics of the tables are to be renewed.
     *
     * @param statisticsCheckInterval the least time between two checks of the statistics
     * @return copy with the given time
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is negative or longer than a {@code long} number of nanoseconds
     */
    public EngineSettings withStatisticsCheckInterval(Duration statisticsCheckInterval)
    {
        EngineSettings copy = copy();
        copy.statisticsCheckInterval = checkDuration("statistics-check-interval", statisticsCheckInterval,
                Duration.ZERO);
        return copy;
    }


    /**
     * Most values a cache of the statistics of a table keeps (the setting {@code statistics-cache-limit}).
     *
     * @return most values a cache of the statistics of a table keeps
     */
    public int statisticsCacheLimit()
    {
        return statisticsCacheLimit;
    }


    /**
     * Copy with the given most values a cache of the statistics of a table keeps.
     *
     * @param statisticsCacheLimit most values a cache of the statistics of a table keeps
     * @return copy with the given limit
     * @throws IllegalArgumentException if the limit is not positive
     */
    public EngineSettings withStatisticsCacheLimit(int statisticsCacheLimit)
    {
        EngineSettings copy = copy();
        copy.statisticsCacheLimit = checkRange("statistics-cache-limit", statisticsCacheLimit, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Whether a request evaluates the SERVICE calls independently of their context when it is not told otherwise (the
     * setting {@code service-reorder}).
     *
     * @return whether a request evaluates the SERVICE calls independently of their context by default
     */
    public boolean serviceReorder()
    {
        return serviceReorder;
    }


    /**
     * Copy with the given default of the evaluation of the SERVICE calls independently of their context.
     *
     * @param serviceReorder whether a request evaluates the SERVICE calls independently of their context by default
     * @return copy with the given default
     */
    public EngineSettings withServiceReorder(boolean serviceReorder)
    {
        EngineSettings copy = copy();
        copy.serviceReorder = serviceReorder;
        return copy;
    }


    /**
     * Number of the solutions of a SERVICE call from which they are stored in a temporary table (the setting
     * {@code service-table-rows}).
     *
     * @return number of the solutions of a SERVICE call from which they are stored in a temporary table
     */
    public int serviceTableRows()
    {
        return serviceTableRows;
    }


    /**
     * Copy with the given number of the solutions of a SERVICE call from which they are stored in a temporary table;
     * zero stores them all there.
     *
     * @param serviceTableRows number of the solutions of a SERVICE call from which they are stored in a temporary table
     * @return copy with the given number
     * @throws IllegalArgumentException if the number is negative
     */
    public EngineSettings withServiceTableRows(int serviceTableRows)
    {
        EngineSettings copy = copy();
        copy.serviceTableRows = checkRange("service-table-rows", serviceTableRows, 0, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Number of the solutions of a SERVICE call inserted into their temporary table by one statement (the setting
     * {@code service-batch-size}).
     *
     * @return number of the solutions of a SERVICE call inserted by one statement
     */
    public int serviceBatchSize()
    {
        return serviceBatchSize;
    }


    /**
     * Copy with the given number of the solutions of a SERVICE call inserted into their temporary table by one
     * statement.
     *
     * @param serviceBatchSize number of the solutions of a SERVICE call inserted by one statement
     * @return copy with the given number
     * @throws IllegalArgumentException if the number is not positive
     */
    public EngineSettings withServiceBatchSize(int serviceBatchSize)
    {
        EngineSettings copy = copy();
        copy.serviceBatchSize = checkRange("service-batch-size", serviceBatchSize, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Longest time a request waits for a part of the answer of an endpoint before it checks its time limit and its
     * cancellation again (the setting {@code service-check-interval}).
     *
     * @return longest time a request waits for a part of the answer of an endpoint between two checks
     */
    public Duration serviceCheckInterval()
    {
        return serviceCheckInterval;
    }


    /**
     * Copy with the given longest time a request waits for a part of the answer of an endpoint between two checks.
     *
     * @param serviceCheckInterval longest time a request waits for a part of the answer of an endpoint between two
     *            checks
     * @return copy with the given time
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is not positive or longer than a {@code long} number of nanoseconds
     */
    public EngineSettings withServiceCheckInterval(Duration serviceCheckInterval)
    {
        EngineSettings copy = copy();
        copy.serviceCheckInterval = checkDuration("service-check-interval", serviceCheckInterval, Duration.ofNanos(1));
        return copy;
    }


    /**
     * Largest part of the answer of an endpoint, in bytes, handed over to the request at once (the setting
     * {@code service-part-size}).
     *
     * @return largest part of the answer of an endpoint handed over at once
     */
    public int servicePartSize()
    {
        return servicePartSize;
    }


    /**
     * Copy with the given largest part of the answer of an endpoint, in bytes, handed over to the request at once.
     *
     * @param servicePartSize largest part of the answer of an endpoint handed over at once
     * @return copy with the given size
     * @throws IllegalArgumentException if the size is not positive
     */
    public EngineSettings withServicePartSize(int servicePartSize)
    {
        EngineSettings copy = copy();
        copy.servicePartSize = checkRange("service-part-size", servicePartSize, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Number of the parts of the answer of an endpoint read ahead of the request at most (the setting
     * {@code service-read-ahead}).
     *
     * @return number of the parts of the answer of an endpoint read ahead at most
     */
    public int serviceReadAhead()
    {
        return serviceReadAhead;
    }


    /**
     * Copy with the given number of the parts of the answer of an endpoint read ahead of the request at most.
     *
     * @param serviceReadAhead number of the parts of the answer of an endpoint read ahead at most
     * @return copy with the given number
     * @throws IllegalArgumentException if the number is not positive
     */
    public EngineSettings withServiceReadAhead(int serviceReadAhead)
    {
        EngineSettings copy = copy();
        copy.serviceReadAhead = checkRange("service-read-ahead", serviceReadAhead, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Time for which the rows of a copy closed before their end read the rows the database has sent already (the
     * setting {@code copy-drain-time}).
     *
     * @return time for which the rows of a copy closed before their end are read
     */
    public Duration copyDrainTime()
    {
        return copyDrainTime;
    }


    /**
     * Copy with the given time for which the rows of a copy closed before their end read the rows sent already.
     *
     * @param copyDrainTime time for which the rows of a copy closed before their end are read
     * @return copy with the given time
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is negative or longer than a {@code long} number of nanoseconds
     */
    public EngineSettings withCopyDrainTime(Duration copyDrainTime)
    {
        EngineSettings copy = copy();
        copy.copyDrainTime = checkDuration("copy-drain-time", copyDrainTime, Duration.ZERO);
        return copy;
    }


    /**
     * Time after which the cancellation of a copy is sent again until the database has started the copy (the setting
     * {@code copy-cancel-interval}).
     *
     * @return time after which the cancellation of a copy is sent again
     */
    public Duration copyCancelInterval()
    {
        return copyCancelInterval;
    }


    /**
     * Copy with the given time after which the cancellation of a copy is sent again until the copy has started.
     *
     * @param copyCancelInterval time after which the cancellation of a copy is sent again
     * @return copy with the given time
     * @throws NullPointerException if the time is null
     * @throws IllegalArgumentException if the time is not positive or longer than a {@code long} number of nanoseconds
     */
    public EngineSettings withCopyCancelInterval(Duration copyCancelInterval)
    {
        EngineSettings copy = copy();
        copy.copyCancelInterval = checkDuration("copy-cancel-interval", copyCancelInterval, Duration.ofNanos(1));
        return copy;
    }


    /**
     * Number of the rows a result reads between two checks of the time limit of its request (the setting
     * {@code result-check-rows}).
     *
     * @return number of the rows read between two checks of the time limit
     */
    public int resultCheckRows()
    {
        return resultCheckRows;
    }


    /**
     * Copy with the given number of the rows a result reads between two checks of the time limit of its request.
     *
     * @param resultCheckRows number of the rows read between two checks of the time limit
     * @return copy with the given number
     * @throws IllegalArgumentException if the number is not positive
     */
    public EngineSettings withResultCheckRows(int resultCheckRows)
    {
        EngineSettings copy = copy();
        copy.resultCheckRows = checkRange("result-check-rows", resultCheckRows, 1, Integer.MAX_VALUE);
        return copy;
    }


    /**
     * Copy with the setting of the given name set to the value its text gives (see {@link SettingValues}), as an
     * attribute of the resource of the configuration in {@code context.xml} sets it: the costs, the margins, the
     * ratios, the rows and the selectivities are numbers, the limits and the sizes integers, {@code service-reorder} is
     * {@code true} or {@code false}, {@code join-optimizer} is {@code off}, {@code settings} or {@code advice},
     * {@code statistics-function} is the schema and the name of the function joined by a dot, and the times are
     * integers followed by their units, or without a unit in seconds for {@code statistics-check-interval} and in
     * milliseconds for the others.
     *
     * @param name name of the setting (see {@link #NAMES})
     * @param value text of the value
     * @return copy with the setting set to the value
     * @throws IllegalArgumentException if no setting has the name, or the text gives no value of the setting
     */
    public EngineSettings with(String name, String value)
    {
        return switch(name)
        {
            case "join-optimizer" -> withJoinOptimizer(parseEnum(name, value, JoinOptimizerMode.class));
            case "lookup-cost" -> withLookupCost(parseDouble(name, value));
            case "lookup-row-cost" -> withLookupRowCost(parseDouble(name, value));
            case "hash-cost" -> withHashCost(parseDouble(name, value));
            case "comparison-cost" -> withComparisonCost(parseDouble(name, value));
            case "pairwise-margin" -> withPairwiseMargin(parseDouble(name, value));
            case "second-join-margin" -> withSecondJoinMargin(parseDouble(name, value));
            case "memoize-ratio" -> withMemoizeRatio(parseDouble(name, value));
            case "exhaustive-join-limit" -> withExhaustiveJoinLimit(parseInt(name, value));
            case "counted-join-ratio" -> withCountedJoinRatio(parseDouble(name, value));
            case "default-rows" -> withDefaultRows(parseDouble(name, value));
            case "equality-selectivity" -> withEqualitySelectivity(parseDouble(name, value));
            case "inequality-selectivity" -> withInequalitySelectivity(parseDouble(name, value));
            case "default-selectivity" -> withDefaultSelectivity(parseDouble(name, value));
            case "disjunct-limit" -> withDisjunctLimit(parseInt(name, value));
            case "box-comparison-limit" -> withBoxComparisonLimit(parseInt(name, value));
            case "triple-term-class-limit" -> withTripleTermClassLimit(parseInt(name, value));
            case "exists-round-limit" -> withExistsRoundLimit(parseInt(name, value));
            case "statistics-function" -> withStatisticsFunction(parseFunction(name, value));
            case "statistics-check-interval" -> withStatisticsCheckInterval(
                    parseDuration(name, value, ChronoUnit.SECONDS));
            case "statistics-cache-limit" -> withStatisticsCacheLimit(parseInt(name, value));
            case "service-reorder" -> withServiceReorder(parseBoolean(name, value));
            case "service-table-rows" -> withServiceTableRows(parseInt(name, value));
            case "service-batch-size" -> withServiceBatchSize(parseInt(name, value));
            case "service-check-interval" -> withServiceCheckInterval(parseDuration(name, value, ChronoUnit.MILLIS));
            case "service-part-size" -> withServicePartSize(parseInt(name, value));
            case "service-read-ahead" -> withServiceReadAhead(parseInt(name, value));
            case "copy-drain-time" -> withCopyDrainTime(parseDuration(name, value, ChronoUnit.MILLIS));
            case "copy-cancel-interval" -> withCopyCancelInterval(parseDuration(name, value, ChronoUnit.MILLIS));
            case "result-check-rows" -> withResultCheckRows(parseInt(name, value));
            default -> throw new IllegalArgumentException("unknown setting: " + name);
        };
    }


    /**
     * Copy of the settings, which a {@code with} method changes before it hands it out.
     *
     * @return copy of the settings
     */
    private EngineSettings copy()
    {
        try
        {
            return (EngineSettings) clone();
        }
        catch(CloneNotSupportedException e)
        {
            throw new AssertionError(e);
        }
    }
}
