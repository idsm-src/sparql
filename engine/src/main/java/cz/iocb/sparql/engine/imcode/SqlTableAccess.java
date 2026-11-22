package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.NodeTerm;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindingPair.ResourceClassPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Access to one table, or to constants only when the table is null: the mapped columns are exposed as variables and the
 * rows are restricted by conditions. Joins, left joins and distinct unions of accesses to the same table or along keys
 * are merged into a single access ({@link #tryReduceJoin} and friends), and so is a join with VALUES, unless the merge
 * would give the conditions of the access too many disjuncts (see {@link #disjunctLimit}); a union of two accesses
 * telling their rows apart by the null test of a column is written as a single scan ({@link #translateUnion}).
 * {@code reduced} means the parent does not care about duplicates; {@code distinctColumns} asks for deduplication over
 * those columns (see {@link cz.iocb.sparql.engine.mapping.SingleTableQuadMapping}). Inside a pattern evaluated for a
 * current solution (an EXISTS pattern, or a pattern evaluated per graph, whose current solution gives the graph), an
 * access whose variable the nodes above merge with its value in the current solution takes the value into its own
 * conditions, compared in the class of its columns like a constant (see {@link ValueCondition}); so does an access
 * whose variable a filter above requires to be a constant term, which makes the filter needless (see
 * {@link SqlIntercode#addAcceptedValues}). With the join optimizer on, an access whose variable a filter above accepts
 * only with one of several constant terms takes the terms into its conditions as a disjunction (see
 * {@link #restrictToAcceptedTerms}). An access to a dictionary table whose row constants select by a key reads the row
 * when it is optimised and takes it by the key the access is joined by (see {@link #resolveDictionaryRows}), and an
 * access to a probed table that constants restrict by an index is asked for a row then, giving no solution when it has
 * none (see {@link #probeRows}).
 */
public final class SqlTableAccess extends SqlIntercode
{
    /**
     * Restriction of the rows of an access to those holding a value the parent accepts of a variable (see
     * {@link Restrictions#getAcceptedValue}): a value of the current solution that the pattern the access lies in is
     * evaluated for (see {@link SqlExpressionIntercode#isCurrentValue}), or a constant term a filter requires of the
     * variable (see {@link SqlIntercode#addAcceptedValues}). The term held by the columns is the value of the
     * expression. The value is converted to the class of the columns, so that the database can look the rows up by an
     * index of the table, whereas the node merging the variable with the value, or filtering by it, compares them in
     * their common class. The condition is kept apart from the conditions on the rows: it leaves the variable bound to
     * the columns, so that the nodes above conclude nothing from it by themselves (they ask {@link #isRestricted}), and
     * the columns of a current solution change whenever the node providing it is optimised (the node holding the EXISTS
     * expression, the enumeration of the graphs a pattern is evaluated in), so the expression is read again on every
     * optimisation of the access (see {@link cz.iocb.sparql.engine.imcode.expression.SqlOuterVariable} and
     * {@link cz.iocb.sparql.engine.imcode.expression.SqlLateralVariable}).
     *
     * @param resourceClass class of the term held by the columns
     * @param columns the columns of the table holding the term, as the internal bindings give them
     * @param value expression giving the value, which refers to the current solution only or to nothing; it is never
     *            unbound when the access takes it in, and should the current solution leave it unbound later, every row
     *            passes
     */
    record ValueCondition(ResourceClass resourceClass, List<Column> columns, SqlExpressionIntercode value)
    {
    }


    /**
     * The greatest number of disjuncts the conditions of an access may get by a merge that multiplies them: by taking
     * in the rows of VALUES, or by the merge with another access to its table or to a table referencing it. The
     * conditions are kept in disjunctive normal form, so they get a disjunct for every row of the VALUES and every
     * disjunct they have, or for every pair of the disjuncts of the two accesses that do not contradict each other, and
     * building them, like every later operation on them, compares every two disjuncts: the time grows with the square
     * of their number, whereas the database joins the access with a list left in VALUES, or the two accesses left
     * apart, as fast as it evaluates the disjunction.
     */
    private static final int disjunctLimit = 30;

    /**
     * Settings of the planner of the database under which a probed table is asked for a row (see {@link #hasRows}): no
     * scan of the whole table, so that the row is looked up by the index of the columns asked about.
     */
    private static final List<String> probeSettings = List.of("enable_seqscan = off", "enable_bitmapscan = off");

    /**
     * Database schema the table belongs to; null for constants only.
     */
    private final DatabaseSchema schema;

    /**
     * Accessed table; null for constants only.
     */
    private final SourceTable table;

    /**
     * Conditions on the rows.
     */
    private final Conditions conditions;

    /**
     * Bindings in terms of the table's own columns.
     */
    private final VariableBindings internal;

    /**
     * Columns over which deduplication is requested.
     */
    private final Set<Column> distinctColumns;

    /**
     * Whether the parent tolerates duplicate rows.
     */
    private final boolean reduced;

    /**
     * Values the rows are restricted to, in conjunction with the conditions (see {@link ValueCondition}): values of the
     * current solution of the enclosing pattern and the constant terms the filters above require; empty when the parent
     * has accepted no value.
     */
    private final List<ValueCondition> values;


    /**
     * Creates the node; the exposed bindings use one representative per set of columns equated by the conditions and
     * carry the knowledge whether their values may be null.
     *
     * @param schema the database schema, or null for a constant-only access
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @param distinctColumns columns over which deduplication is requested
     * @param values values the rows are restricted to
     */
    protected SqlTableAccess(DatabaseSchema schema, SourceTable table, Conditions conditions, VariableBindings internal,
            boolean reduced, Set<Column> distinctColumns, List<ValueCondition> values)
    {
        super(getExternalVariableBindings(schema, table, internal, conditions), true);

        this.schema = schema;
        this.table = table;
        this.conditions = conditions;
        this.internal = internal;
        this.reduced = reduced;
        this.distinctColumns = distinctColumns;
        this.values = values;
    }


    /**
     * Access exposing the given bindings, restricted by the conditions, with a deduplication request over
     * {@code distinctColumns}.
     *
     * @param request the current request
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @param distinctColumns columns over which deduplication is requested
     * @return access exposing the given bindings, restricted by the conditions, with a deduplication request over
     *         {@code distinctColumns}
     */
    public static SqlIntercode create(Request request, SourceTable table, Conditions conditions,
            VariableBindings internal, boolean reduced, Set<Column> distinctColumns)
    {
        return create(request.getConfiguration().getDatabaseSchema(), table, conditions, internal, reduced,
                distinctColumns);
    }


    /**
     * Access exposing the given bindings, restricted by the conditions.
     *
     * @param request the current request
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @return access exposing the given bindings, restricted by the conditions
     */
    public static SqlIntercode create(Request request, SourceTable table, Conditions conditions,
            VariableBindings internal, boolean reduced)
    {
        return create(request, table, conditions, internal, reduced, Set.of());
    }


    /**
     * Access exposing the given bindings, restricted by the conditions.
     *
     * @param request the current request
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @return access exposing the given bindings, restricted by the conditions
     */
    public static SqlIntercode create(Request request, SourceTable table, Conditions conditions,
            VariableBindings internal)
    {
        return create(request, table, conditions, internal, false);
    }


    /**
     * Unrestricted access exposing the given bindings.
     *
     * @param request the current request
     * @param table the table
     * @param internal bindings in terms of the table's own columns
     * @return unrestricted access exposing the given bindings
     */
    public static SqlIntercode create(Request request, SourceTable table, VariableBindings internal)
    {
        return create(request, table, new Conditions(true), internal, false);
    }


    /**
     * Access exposing the given bindings, restricted by the conditions, with a deduplication request over
     * {@code distinctColumns}; the variant for the merges of accesses, which know the schema but not the request.
     *
     * @param schema the database schema, or null for a constant-only access
     * @param table the table, or null for a constant-only access
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @param distinctColumns columns over which deduplication is requested
     * @return access exposing the given bindings, restricted by the conditions, with a deduplication request over
     *         {@code distinctColumns}
     */
    static SqlIntercode create(DatabaseSchema schema, SourceTable table, Conditions conditions,
            VariableBindings internal, boolean reduced, Set<Column> distinctColumns)
    {
        return new SqlTableAccess(schema, table, conditions, internal, reduced, distinctColumns, List.of());
    }


    /**
     * Access exposing the given bindings, restricted by the conditions.
     *
     * @param schema the database schema, or null for a constant-only access
     * @param table the table, or null for a constant-only access
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @return access exposing the given bindings, restricted by the conditions
     */
    static SqlIntercode create(DatabaseSchema schema, SourceTable table, Conditions conditions,
            VariableBindings internal, boolean reduced)
    {
        return create(schema, table, conditions, internal, reduced, Set.of());
    }


    /**
     * Access exposing the given bindings, restricted by the conditions.
     *
     * @param schema the database schema, or null for a constant-only access
     * @param table the table, or null for a constant-only access
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @return access exposing the given bindings, restricted by the conditions
     */
    static SqlIntercode create(DatabaseSchema schema, SourceTable table, Conditions conditions,
            VariableBindings internal)
    {
        return create(schema, table, conditions, internal, false);
    }


    /**
     * Unrestricted access exposing the given bindings.
     *
     * @param schema the database schema, or null for a constant-only access
     * @param table the table, or null for a constant-only access
     * @param internal bindings in terms of the table's own columns
     * @return unrestricted access exposing the given bindings
     */
    static SqlIntercode create(DatabaseSchema schema, SourceTable table, VariableBindings internal)
    {
        return create(schema, table, new Conditions(true), internal, false);
    }


    /**
     * Bindings for the parent: equated columns replaced by their representative, expressions by generated columns, each
     * carrying the knowledge whether its values may be null.
     *
     * @param schema the database schema, or null for a constant-only access
     * @param table the table, or null for a constant-only access
     * @param bindings the variable bindings
     * @param conditions the conditions
     * @return bindings for the parent: equated columns replaced by their representative, expressions by generated
     *         columns
     */
    private static VariableBindings getExternalVariableBindings(DatabaseSchema schema, SourceTable table,
            VariableBindings bindings, Conditions conditions)
    {
        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        Map<Column, Column> expressions = new HashMap<>();

        VariableBindings result = new VariableBindings();

        for(VariableBinding variableBinding : bindings.getValues())
        {
            VariableBinding binding = new VariableBinding(variableBinding.getVariable(), variableBinding.canBeNull());

            for(Entry<ResourceClass, List<Column>> map : variableBinding.getMappings().entrySet())
                binding.addMapping(map.getKey(),
                        selectColumns(schema, table, conditions, representants, expressions, map.getValue()));

            result.add(binding);
        }

        return result;
    }


    /**
     * For each column equated with others in every disjunct, a representative: a constant if present, else a plain
     * column rather than an expression.
     *
     * @param conditions the conditions
     * @return for each column equated with others in every disjunct, a representative: a constant if present, else a
     *         plain column rather than an expression
     */
    private static Map<Column, Column> selectColumnRepresentants(Conditions conditions)
    {
        Set<ColumnComparison> equals = null;

        for(Condition condition : conditions.getConditions())
        {
            if(equals == null)
                equals = new HashSet<>(condition.getEquivalences());
            else
                equals.retainAll(condition.getEquivalences());
        }

        if(equals == null)
            return new HashMap<>();


        Map<Column, Column> representants = new HashMap<>();

        for(ColumnComparison p : equals)
        {
            Column r1 = representants.getOrDefault(p.getLeft(), p.getLeft());
            Column r2 = representants.getOrDefault(p.getRight(), p.getRight());
            Column r = r1 instanceof ConstantColumn || r2 instanceof ExpressionColumn ? r1 : r2;

            representants.replaceAll((_, v) -> v.equals(r1) || v.equals(r2) ? r : v);

            if(!(p.getLeft() instanceof ConstantColumn))
                representants.put(p.getLeft(), r);

            if(!(p.getRight() instanceof ConstantColumn))
                representants.put(p.getRight(), r);
        }

        return representants;
    }


    /**
     * Replaces the columns by their representatives and expressions by generated columns (allocated on first use); the
     * resulting columns carry the knowledge whether their values may be null.
     *
     * @param schema the database schema, or null for a constant-only access
     * @param table the table, or null for a constant-only access
     * @param conditions the conditions
     * @param set representative of each equated column
     * @param expressions generated columns of expressions, extended on demand
     * @param columns the columns
     * @return the replaced columns
     */
    private static List<Column> selectColumns(DatabaseSchema schema, SourceTable table, Conditions conditions,
            Map<Column, Column> set, Map<Column, Column> expressions, List<Column> columns)
    {
        if(columns == null)
            return null;

        List<Column> optimized = new ArrayList<>(columns.size());

        for(Column column : columns)
        {
            column = set.get(column) != null ? set.get(column) : column;

            // the exposed columns carry the knowledge whether their values may be null
            boolean canBeNull = !isKnownNotNull(schema, table, conditions, column);

            if(column instanceof ExpressionColumn)
            {
                if(!expressions.containsKey(column))
                    expressions.put(column,
                            new TableColumn("#expr-" + expressions.size(), column.getType(), canBeNull));

                column = expressions.get(column);
            }
            else if(column instanceof TableColumn && !canBeNull)
            {
                column = new TableColumn(column.getName(), column.getType(), false);
            }

            assert column != null;

            optimized.add(column);
        }

        return optimized;
    }


    /**
     * True if the column may be NULL in the rows of the table: a NULL constant, an expression that may be null, or a
     * table column that is nullable in the schema. Without a schema (or a table) every column not known to be not null
     * by itself may be null.
     *
     * @param schema the database schema, or null
     * @param table the table, or null
     * @param column the column
     * @return true if the column may be NULL in the rows of the table, false otherwise
     */
    public static boolean mayBeNull(DatabaseSchema schema, SourceTable table, Column column)
    {
        if(!column.canBeNull())
            return false;

        if(schema == null || table == null)
            return true;

        return schema.isNullableColumn(table, column);
    }


    /**
     * Predicate for {@link Condition#addAreEqual(List, List, java.util.function.IntPredicate)}: the position of the
     * class needs the null-safe equality when it is optional and both compared columns may be NULL in the table. When
     * either of them cannot be NULL, the strict equality has the same meaning and is preferred, since it can use
     * indexes and hash joins.
     *
     * @param schema the database schema, or null
     * @param table the table, or null
     * @param resClass the resource class of the columns
     * @param cols1 the first columns
     * @param cols2 the second columns
     * @return the predicate
     */
    public static IntPredicate needsNullSafeEquality(DatabaseSchema schema, SourceTable table, ResourceClass resClass,
            List<Column> cols1, List<Column> cols2)
    {
        return i -> resClass.isOptionalColumn(i) && mayBeNull(schema, table, cols1.get(i))
                && mayBeNull(schema, table, cols2.get(i));
    }


    /**
     * Returns the table columns that are bound to a constant by every disjunct of the conditions, or to a value of the
     * current solution of the enclosing pattern that is never unbound, which is a constant for one evaluation of the
     * pattern.
     *
     * @return the table columns that are bound to a constant by every disjunct of the conditions or to a value of the
     *         current solution
     */
    private Set<Column> getConstantBoundColumns()
    {
        Set<Column> columns = new HashSet<>();

        for(ColumnComparison p : conditions.getAreEqual())
        {
            if(p.getLeft() instanceof ConstantColumn && p.getRight() instanceof TableColumn)
                columns.add(p.getRight());

            if(p.getLeft() instanceof TableColumn && p.getRight() instanceof ConstantColumn)
                columns.add(p.getLeft());
        }

        columns.addAll(getValueColumns(true));

        return columns;
    }


    /**
     * Returns the table columns restricted to a value of the current solution of the enclosing pattern (see
     * {@link #values}).
     *
     * @param bound whether only the columns restricted to a value that is never unbound are returned, which hold that
     *            value in every row; a row passes any value that is unbound
     * @return the table columns restricted to a value of the current solution
     */
    Set<Column> getValueColumns(boolean bound)
    {
        Set<Column> columns = new HashSet<>();

        for(ValueCondition value : values)
            if(!bound || !value.value().canBeNull())
                value.columns().stream().filter(c -> c instanceof TableColumn).forEach(columns::add);

        return columns;
    }


    /**
     * True if the rows are restricted to a value of the current solution of the enclosing pattern (see
     * {@link #values}).
     *
     * @return true if the rows are restricted to a value of the current solution, false otherwise
     */
    boolean hasValues()
    {
        return !values.isEmpty();
    }


    /**
     * True if the access restricts its rows to the same values of the current solution as the other access (see
     * {@link #values}).
     *
     * @param other the other access
     * @return true if both accesses restrict their rows to the same values of the current solution, false otherwise
     */
    boolean hasSameValues(SqlTableAccess other)
    {
        return containsAll(values, other.values) && containsAll(other.values, values);
    }


    /**
     * True if the first restrictions to values of the current solution include all the other ones: for each of them,
     * the same columns are restricted to the same value.
     *
     * @param values the restrictions
     * @param others the other restrictions
     * @return true if the first restrictions include all the other ones, false otherwise
     */
    private static boolean containsAll(List<ValueCondition> values, List<ValueCondition> others)
    {
        return others.stream().allMatch(o -> values.stream().anyMatch(v -> v.resourceClass().equals(o.resourceClass())
                && v.columns().equals(o.columns()) && v.value().hasSameValue(o.value())));
    }


    /**
     * The restrictions to values of the current solution of two accesses merged into one, each taken once.
     *
     * @param left the restrictions of one access
     * @param right the restrictions of the other access
     * @return the restrictions of both accesses
     */
    private static List<ValueCondition> mergeValues(List<ValueCondition> left, List<ValueCondition> right)
    {
        if(right.isEmpty() || containsAll(left, right))
            return left;

        List<ValueCondition> result = new ArrayList<>(left);

        for(ValueCondition value : right)
            if(!containsAll(result, List.of(value)))
                result.add(value);

        return result;
    }


    /**
     * The access with other conditions and internal bindings, which keeps its table, its deduplication and the values
     * of the current solution its rows are restricted to.
     *
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @return the access with the given conditions and internal bindings
     */
    SqlTableAccess with(Conditions conditions, VariableBindings internal)
    {
        return with(conditions, internal, reduced);
    }


    /**
     * The access with other conditions and internal bindings, for a parent that tolerates duplicate rows or not as
     * given; it keeps its table, its deduplication and the values of the current solution its rows are restricted to.
     *
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether the parent tolerates duplicate rows
     * @return the access with the given conditions and internal bindings
     */
    SqlTableAccess with(Conditions conditions, VariableBindings internal, boolean reduced)
    {
        return new SqlTableAccess(schema, table, conditions, internal, reduced, distinctColumns, values);
    }


    /**
     * Returns the given columns together with the constant-bound columns, closed under the equalities of the
     * conditions.
     *
     * @param selected the selected variables
     * @return the given columns together with the constant-bound columns, closed under the equalities of the conditions
     */
    private Set<Column> getCoveredColumns(Collection<Column> selected)
    {
        Set<Column> columns = getConstantBoundColumns();

        for(Column column : selected)
        {
            columns.add(column);
            columns.addAll(conditions.getEqualTableColumns(column));
        }

        return columns;
    }


    /**
     * Returns the internal columns of the selected variables together with the constant-bound columns, closed under the
     * equalities of the conditions.
     *
     * @param selected the selected variables
     * @return the internal columns of the selected variables together with the constant-bound columns, closed under the
     *         equalities of the conditions
     */
    private Set<Column> getCoveredVariableColumns(Collection<Variable> selected)
    {
        Set<Column> columns = new HashSet<>();

        for(Variable var : selected)
        {
            VariableBinding binding = internal.get(var);

            if(binding != null)
                columns.addAll(binding.getNonConstantColumns());
        }

        return getCoveredColumns(columns);
    }


    /**
     * True if the column is known to be not null in the rows of the access: it is not nullable in the schema, or the
     * conditions require it to be not null or compare it by a strict operator in every disjunct. Only such columns can
     * be matched against unique keys, since a unique index does not make rows with NULL key values unique.
     *
     * @param schema the database schema
     * @param column the column
     * @return true if the column is known to be not null in the rows of the access, false otherwise
     */
    boolean isKnownNotNull(DatabaseSchema schema, Column column)
    {
        return isKnownNotNull(schema, table, conditions, column);
    }


    /**
     * True if the column is known to be not null in the rows selected by the conditions from the table, see
     * {@link #isKnownNotNull(DatabaseSchema, Column)}.
     *
     * @param schema the database schema, or null
     * @param table the table, or null
     * @param conditions the conditions
     * @param column the column
     * @return true if the column is known to be not null, false otherwise
     */
    private static boolean isKnownNotNull(DatabaseSchema schema, SourceTable table, Conditions conditions,
            Column column)
    {
        if(!mayBeNull(schema, table, column))
            return true;

        if(conditions.getIsNotNull().contains(column))
            return true;

        if(conditions.getAreEqual().stream().anyMatch(p -> p.contains(column)))
            return true;

        if(conditions.getAreNotEqual().stream().anyMatch(p -> p.contains(column)))
            return true;

        return false;
    }


    /**
     * The columns of the given set known to be not null, see {@link #isKnownNotNull(DatabaseSchema, Column)}.
     *
     * @param schema the database schema
     * @param columns the columns
     * @return the columns of the given set known to be not null
     */
    private Set<Column> getKnownNotNull(DatabaseSchema schema, Set<Column> columns)
    {
        return columns.stream().filter(c -> isKnownNotNull(schema, c)).collect(toSet());
    }


    /**
     * Returns whether the deduplication requested by {@link #distinctColumns} is implied by a key of the table, in
     * which case the request can be dropped.
     *
     * @param schema the database schema
     * @return true if a key of the table covers the distinct columns, false otherwise
     */
    private boolean isDistinctImpliedByKey(DatabaseSchema schema)
    {
        return table == null
                || schema.getCompatibleKey(table, getKnownNotNull(schema, getCoveredColumns(distinctColumns))) != null;
    }


    /**
     * The variable whose terms the access only reads for a node (see {@link NodeTerm#reader}): the access reads the
     * table of the term as the reader of the node does, restricted by nothing more, and binds nothing but the node and
     * the variable the node stands for.
     *
     * @param request the current request
     * @param node the node
     * @return the variable the node stands for when the access only reads its terms for the node, or null
     */
    Variable getReadVariable(Request request, Variable node)
    {
        NodeTerm term = request.getNodeTerms().get(node);

        if(term == null || !term.determining() || !(term.reader() instanceof SqlTableAccess reader))
            return null;

        if(!Objects.equals(table, reader.table) || !conditions.equals(reader.conditions) || !values.isEmpty()
                || isDeduplicating() || bindings.get(node) == null || bindings.get(node).canBeNull())
            return null;

        for(Variable variable : bindings.getVariables())
            if(!variable.equals(node) && !variable.equals(term.variable()))
                return null;

        return term.variable();
    }


    /**
     * Returns whether the access deduplicates its rows itself, so that it reads them all before it gives the first one:
     * a deduplication is requested and the duplicates matter to the parent.
     *
     * @return true if the access deduplicates its rows
     */
    boolean isDeduplicating()
    {
        return !reduced && !distinctColumns.isEmpty();
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        if(table == null)
            return true;

        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();
        Set<Column> columns = getCoveredVariableColumns(selected);

        if(schema.getCompatibleKey(table, getKnownNotNull(schema, columns)) != null)
            return true;

        // the access deduplicates its rows itself, unless the duplicates do not matter to its parent
        return isDeduplicating() && columns.containsAll(distinctColumns);
    }


    /**
     * True if every row of the other access to the same table is a row of this access and gives its variables the same
     * values: the other access binds every variable of this one alike, its conditions imply the conditions of this one,
     * and it is restricted to every value this one is restricted to. A join with this access then gives the solutions
     * of the other access, each at least once (see {@link SqlJoin}).
     *
     * @param other the other access
     * @return true if every row of the other access is a row of this access giving its variables the same values
     */
    boolean isSubsumedBy(SqlTableAccess other)
    {
        if(table == null || !table.equals(other.table))
            return false;

        for(Variable variable : internal.getVariables())
            if(!internal.get(variable).equals(other.internal.get(variable)))
                return false;

        return other.conditions.implies(conditions) && containsAll(other.values, values);
    }


    /**
     * True if the VALUES can be merged into the access: distinct rows, every variable bound also in the access and
     * never null on either side, and few enough rows to keep the conditions of the access small (see
     * {@link #disjunctLimit}; a single row adds no disjunct, so it is always few enough). The access has to bind a
     * variable in every class of the VALUES, or, when the VALUES is made of terms (see {@link SqlValues#hasTerms}), in
     * a single class with columns, to which its terms are converted (see {@link SqlValues#asConditions}).
     *
     * @param left the left access
     * @param right the VALUES node
     * @return true if the VALUES can be merged into the access, false otherwise
     */
    private static boolean canBeJoinedWithValues(SqlTableAccess left, SqlValues right)
    {
        // asked first, because the test of distinct rows compares every two of them
        if(right.getSize() > 1 && (long) left.conditions.getConditions().size() * right.getSize() > disjunctLimit)
            return false;

        if(!right.isDistinct())
            return false;

        for(VariableBinding binding : right.getVariableBindings().getValues())
        {
            if(binding.canBeNull())
                return false;

            VariableBinding tableBinding = left.getVariableBindings().get(binding.getVariable());

            if(tableBinding == null || tableBinding.canBeNull())
                return false;

            boolean held = binding.getClasses().stream()
                    .allMatch(c -> binding.getMapping(c) != null && tableBinding.getMapping(c) != null);

            boolean convertible = right.hasTerms(binding.getVariable()) && tableBinding.getClasses().size() == 1
                    && tableBinding.getMapping(tableBinding.getClasses().iterator().next()) != null;

            if(!held && !convertible)
                return false;
        }

        return true;
    }


    /**
     * Columns on which two accesses to the same table are equated through their shared variables; empty if the tables
     * differ or the variables are not joined in a single class. A shared variable that may be unbound on a side equates
     * no columns, because an unbound value is compatible with any value: when both sides take it from the same columns
     * of a row (see {@link #isHeldAlike}), it contributes nothing, as the variable is compatible in the rows that the
     * other columns make one row (a key of the table), and otherwise the result is empty as well.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the left access
     * @param right the right access
     * @return columns on which two accesses to the same table are equated through their shared variables; empty if the
     *         tables differ or the variables are not joined in a single class
     */
    static Set<Column> getJoinColumns(ClassRelations relations, SqlTableAccess left, SqlTableAccess right)
    {
        // only the same tables can by merged
        if(!Objects.equals(left.table, right.table))
            return Set.of();


        Set<Column> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, left.internal, right.internal))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return Set.of();

            if(leftBinding.canBeNull() || rightBinding.canBeNull())
            {
                if(!isHeldAlike(left, right, pair))
                    return Set.of();

                // an unbound value is compatible with any value, so the columns of the variable are not equated
                continue;
            }

            for(ResourceClassPair pairedClass : pair.getClasses())
            {
                if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
                    return Set.of();

                List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
                List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

                for(int i = 0; i < leftCols.size(); i++)
                {
                    Set<Column> leftColumns = left.conditions.getEqualTableColumns(leftCols.get(i));
                    Set<Column> rightColumns = right.conditions.getEqualTableColumns(rightCols.get(i));

                    leftColumns.retainAll(rightColumns);
                    columns.addAll(leftColumns);
                }
            }
        }

        for(ColumnComparison pair : left.conditions.getAreEqual())
        {
            if(pair.getLeft() instanceof ConstantColumn && pair.getRight() instanceof TableColumn)
                if(right.conditions.getAreEqual().contains(pair))
                    columns.add(pair.getRight());

            if(pair.getLeft() instanceof TableColumn && pair.getRight() instanceof ConstantColumn)
                if(right.conditions.getAreEqual().contains(pair))
                    columns.add(pair.getLeft());
        }

        return columns;
    }


    /**
     * True if the two accesses to the same table take the shared variable from the same columns of a row in a single
     * class, whether a side may leave it unbound or not: each column is the same on both sides or equal to the other
     * one by the conditions of a side in all its disjuncts (the null-safe equalities included), so that in a row both
     * sides read the variable either holds the same value on both sides or is unbound on a side, which is compatible
     * with any value. Bindings of no columns are alike only when they are the same.
     *
     * @param left the left access
     * @param right the right access
     * @param pair the bindings of the shared variable on the two sides, paired in a single pair of classes
     * @return true if the two accesses take the variable from the same columns of a row, false otherwise
     */
    private static boolean isHeldAlike(SqlTableAccess left, SqlTableAccess right, VariableBindingPair pair)
    {
        VariableBinding leftBinding = pair.getLeftVariableBinding();
        VariableBinding rightBinding = pair.getRightVariableBinding();

        if(pair.getClasses().size() != 1)
            return false;

        ResourceClassPair pairedClass = pair.getClasses().iterator().next();

        if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
            return false;

        List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
        List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

        if(leftCols == null || rightCols == null)
            return leftBinding.equals(rightBinding);

        for(int i = 0; i < leftCols.size(); i++)
            if(!leftCols.get(i).equals(rightCols.get(i))
                    && !left.conditions.getEqualColumns(leftCols.get(i)).contains(rightCols.get(i))
                    && !right.conditions.getEqualColumns(rightCols.get(i)).contains(leftCols.get(i)))
                return false;

        return true;
    }


    /**
     * True if the shared columns of two accesses to the same table contain a key of the table.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @return true if the shared columns of two accesses to the same table contain a key of the table, false otherwise
     */
    private static boolean canBeJoinedByPrimaryKey(ClassRelations relations, DatabaseSchema schema, SqlTableAccess left,
            SqlTableAccess right)
    {
        Set<Column> columns = left.getKnownNotNull(schema, getJoinColumns(relations, left, right));

        return schema.getCompatibleKey(left.table, columns) != null;
    }


    /**
     * Pairs of parent and child columns equated through shared variables; empty if the variables are not joined in a
     * single class.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @return pairs of parent and child columns equated through shared variables; empty if the variables are not joined
     *         in a single class
     */
    static Set<ColumnPair> getJoinColumnPairs(ClassRelations relations, SqlTableAccess parent, SqlTableAccess child)
    {
        Set<ColumnPair> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, parent.internal, child.internal))
        {
            VariableBinding parentBinding = pair.getLeftVariableBinding();
            VariableBinding childBinding = pair.getRightVariableBinding();

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return Set.of();

            if(parentBinding.canBeNull() || childBinding.canBeNull())
                return Set.of();

            for(ResourceClassPair pairedClass : pair.getClasses())
            {
                if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
                    return Set.of();

                List<Column> parentCols = parentBinding.getMapping(pairedClass.getLeftClass());
                List<Column> childCols = childBinding.getMapping(pairedClass.getRightClass());

                for(int i = 0; i < parentCols.size(); i++)
                {
                    Set<Column> parentColumns = parent.conditions.getEqualTableColumns(parentCols.get(i));
                    Set<Column> childColumns = child.conditions.getEqualTableColumns(childCols.get(i));

                    for(Column parentColumn : parentColumns)
                        for(Column childColumn : childColumns)
                            columns.add(new ColumnPair(parentColumn, childColumn));
                }
            }
        }

        for(ColumnComparison pair : parent.conditions.getAreEqual())
        {
            if(pair.getLeft() instanceof ConstantColumn && pair.getRight() instanceof TableColumn)
                for(Column childColumn : child.conditions.getEqualTableColumns(pair.getLeft()))
                    columns.add(new ColumnPair(pair.getRight(), childColumn));

            if(pair.getLeft() instanceof TableColumn && pair.getRight() instanceof ConstantColumn)
                for(Column childColumn : child.conditions.getEqualTableColumns(pair.getRight()))
                    columns.add(new ColumnPair(pair.getLeft(), childColumn));
        }

        return columns;
    }


    /**
     * Foreign key allowing the parent access to be merged into the child access (no expressions, no deduplication
     * request, the parent columns covered, also those restricted to values, which restrict the child columns
     * referencing them instead, see {@link #joinByForeignKey}), or null.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @return foreign key allowing the parent access to be merged into the child access (no expressions, no
     *         deduplication request, the parent columns covered), or null
     */
    private static Set<ColumnPair> canBeJoinedByForeignKey(ClassRelations relations, DatabaseSchema schema,
            SqlTableAccess parent, SqlTableAccess child)
    {
        if(schema.getForeignKeys(parent.table, child.table).isEmpty())
            return null;

        if(parent.hasExpression())
            return null;

        // the parent is dropped, which is exact only when its rows are unique with respect to the key columns
        if(!parent.distinctColumns.isEmpty())
            return null;

        // the parent is dropped, which requires that each child row has the referenced parent row
        Set<ColumnPair> columns = getJoinColumnPairs(relations, parent, child).stream()
                .filter(p -> child.isKnownNotNull(schema, p.getRight())).collect(toSet());

        // the parent is dropped, so whatever refers to its columns is moved onto the child columns referencing them:
        // the bindings, the conditions and the values the rows are restricted to
        Set<Column> parentColumns = new HashSet<>();
        parentColumns.addAll(parent.conditions.getNonConstantColumns());
        parentColumns.addAll(parent.getInternalVariableBindings().getNonConstantColumns());
        parentColumns.addAll(parent.getValueColumns(false));

        return schema.getCompatibleForeignKey(parent.table, child.table, columns, parentColumns);
    }


    /**
     * The values the parent access restricts its rows to (see {@link #values}) as restrictions of the columns of a
     * child access that reference the restricted columns by the foreign key: a child row passes them exactly when the
     * parent row it references passes those of the parent.
     *
     * @param parent access to the referenced table, whose restricted columns the key covers
     * @param key the foreign key, or the pairs of it that cover the restricted columns
     * @return the values the parent access restricts its rows to as restrictions of the child columns of the key
     */
    private static List<ValueCondition> getReferencedValues(SqlTableAccess parent, Set<ColumnPair> key)
    {
        Map<Column, Column> map = new HashMap<>();

        for(ColumnPair pair : key)
            map.put(pair.getLeft(), pair.getRight());

        return parent.values.stream()
                .map(v -> new ValueCondition(v.resourceClass(), remap(map, v.columns()), v.value())).toList();
    }


    /**
     * True if two accesses to the same table bind the same variables in the same columns.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @return true if two accesses to the same table bind the same variables in the same columns, false otherwise
     */
    private static boolean canBeDistinctUnionizedByPrimaryKey(DatabaseSchema schema, SqlTableAccess left,
            SqlTableAccess right)
    {
        // only the same tables can by merged
        if(!Objects.equals(left.table, right.table))
            return false;

        // the sets of variables (and their resource classes) have to be the same

        if(!left.internal.getVariables().equals(right.internal.getVariables()))
            return false;

        if(left.internal.getVariables().stream()
                .anyMatch(n -> !left.internal.get(n).getMappings().equals(right.internal.get(n).getMappings())))
            return false;

        // the merged access takes the disjunction of the conditions, which the values restricting both alike stay in
        // conjunction with
        if(!left.hasSameValues(right))
            return false;

        return true;
    }


    /**
     * True if a distinct union of the parent and child accesses can be merged into the parent access along a foreign
     * key. A child row whose key columns are not null references a parent row and has its values, so the parent access
     * yields the values of all such child rows when it takes every parent row or when the condition of the child
     * implies its condition. Otherwise the parent access misses the child rows that reference parent rows failing its
     * condition, and widening its condition by that of the child is not exact either: a parent row that meets only the
     * condition of the child need not be referenced by any child row. The same goes for the values the accesses are
     * restricted to (see {@link #values}): the child has to be restricted to every value the parent is, in its columns
     * referencing the restricted ones.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @return true if a distinct union of the parent and child accesses can be merged into the parent access, false
     *         otherwise
     */
    private static boolean canBeDistinctUnionizedByForeignKey(ClassRelations relations, DatabaseSchema schema,
            SqlTableAccess parent, SqlTableAccess child)
    {
        if(parent.table == null || child.table == null || schema.getForeignKeys(parent.table, child.table).isEmpty())
            return false;

        // because we cannot rewrite expressions
        if(child.hasExpression())
            return false;

        // the sets of variables (and their resource classes) have to be the same

        if(!parent.internal.getVariables().equals(child.internal.getVariables()))
            return false;

        if(parent.internal.getVariables().stream().anyMatch(n -> !parent.internal.get(n).getMappings().keySet()
                .equals(child.internal.get(n).getMappings().keySet())))
            return false;

        Set<ColumnPair> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, parent.internal, child.internal))
        {
            VariableBinding parentBinding = pair.getLeftVariableBinding();
            VariableBinding childBinding = pair.getRightVariableBinding();

            for(ResourceClassPair pairedClass : pair.getClasses())
            {
                List<Column> parentCols = parentBinding.getMapping(pairedClass.getLeftClass());
                List<Column> childCols = childBinding.getMapping(pairedClass.getRightClass());

                for(int i = 0; i < parentCols.size(); i++)
                    columns.add(new ColumnPair(parentCols.get(i), childCols.get(i)));
            }
        }

        Set<Column> childColumns = new HashSet<>();
        childColumns.addAll(child.getVariableBindings().getNonConstantColumns());

        if(!child.conditions.isTrue())
            childColumns.addAll(child.conditions.getNonConstantColumns());

        Set<ColumnPair> key = schema.isPartOfForeignKey(parent.table, child.table, columns, childColumns);

        // the child is dropped, which requires that each child row references a parent row; a foreign key matched
        // simply guarantees that only for the rows with no null in the key columns
        if(key == null || !key.stream().allMatch(p -> child.isKnownNotNull(schema, p.getRight())))
            return false;

        // the referenced parent row has to pass the values the parent is restricted to as well, which it does when
        // the key covers the restricted columns and the child is restricted to the same values in its columns
        // referencing them; a value restricting the child alone only leaves some of its rows out
        if(!key.stream().map(ColumnPair::getLeft).collect(toSet()).containsAll(parent.getValueColumns(false))
                || !containsAll(child.values, getReferencedValues(parent, key)))
            return false;

        if(parent.conditions.isTrue())
            return true;

        // a child condition with a trivially true disjunct cannot imply the condition of the parent
        if(child.conditions.isTrue())
            return false;

        // the condition of the child moved from the child columns of the key, the only ones it refers to, onto their
        // parent columns
        Map<Column, Column> map = new HashMap<>();

        for(ColumnPair pair : key)
            map.put(pair.getRight(), pair.getLeft());

        return remap(map, child.conditions).implies(parent.conditions);
    }


    /**
     * The conditions without the requirements that the given columns be not null; everything else every disjunct
     * requires is kept.
     *
     * @param conditions the conditions
     * @param columns the columns no longer required to be not null
     * @return the conditions without the requirements that the columns be not null
     */
    private static Conditions withoutIsNotNull(Conditions conditions, Set<Column> columns)
    {
        Conditions result = new Conditions(false);

        for(Condition cnd : conditions.getConditions())
        {
            Condition condition = new Condition();

            for(Column c : cnd.getIsNotNull())
                if(!columns.contains(c))
                    condition.addIsNotNull(c);

            for(Column c : cnd.getIsNull())
                condition.addIsNull(c);

            for(ColumnComparison p : cnd.getAreEqual())
                condition.addAreEqual(p.getLeft(), p.getRight());

            for(ColumnComparison p : cnd.getAreNotDistinct())
                condition.addAreNotDistinct(p.getLeft(), p.getRight());

            for(ColumnComparison p : cnd.getAreNotEqual())
                condition.addAreNotEqual(p.getLeft(), p.getRight());

            result.add(condition);
        }

        return result;
    }


    /**
     * The binding of a variable of an optional part of a merged access whose part holds only in the rows in which the
     * given column is not null: every other column of the binding is null in the other rows too, so that the variable
     * is unbound there.
     *
     * @param binding the binding of the variable in the access of the optional part
     * @param column the column the optional part requires to be not null, or null when it requires nothing more than
     *            the merged access does
     * @return the binding of the variable in the merged access
     */
    private static VariableBinding toOptional(VariableBinding binding, Column column)
    {
        if(column == null)
            return binding;

        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
        {
            if(entry.getValue() == null)
            {
                mappings.put(entry.getKey(), null);
                continue;
            }

            List<Column> modified = new ArrayList<>(entry.getValue().size());

            for(Column col : entry.getValue())
            {
                if(col.equals(column))
                    modified.add(col);
                else
                    modified.add(new ExpressionColumn(
                            SqlCode.of("CASE WHEN ", column, " IS NOT NULL THEN ", col, " END"), col.getType()));
            }

            mappings.put(entry.getKey(), modified);
        }

        return new VariableBinding(binding.getVariable(), mappings, true);
    }


    /**
     * True if a left join of two accesses to the same table on a key can be merged: at most one extra not-null
     * condition on the right side and no other right-side conditions than those every row of the left side meets.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @return true if a left join of two accesses to the same table on a key can be merged, false otherwise
     */
    private static boolean canBeLeftJoinedByPrimaryKey(ClassRelations relations, DatabaseSchema schema,
            SqlTableAccess left, SqlTableAccess right)
    {
        if(!canBeJoinedByPrimaryKey(relations, schema, left, right))
            return false;

        // a value of the current solution restricting the right side alone would be a condition of the optional part
        if(!containsAll(left.values, right.values))
            return false;

        Set<Column> extraNotNulls = new HashSet<>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());

        if(extraNotNulls.size() > 1)
            return false;


        // conditions added by right table
        Conditions rightConditions = withoutIsNotNull(right.conditions, extraNotNulls);

        // condition added by the join
        Condition joinCondition = new Condition();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, left.internal, right.internal))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            if(!leftBinding.canBeNull() && !rightBinding.canBeNull())
            {
                for(ResourceClassPair pairedClass : pair.getClasses())
                {
                    List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(leftCols, rightCols,
                            needsNullSafeEquality(schema, left.table, pairedClass.getLeftClass(), leftCols, rightCols));
                }
            }
        }

        // check that nothing has been added: every row of the left side meets the conditions of the right side
        Conditions conditions = Conditions.and(rightConditions, joinCondition);

        return left.conditions.implies(conditions);
    }


    /**
     * Merges the VALUES into the access as conditions; the access keeps the classes it binds the variables in, and no
     * solution is left when it can hold the values of no row.
     *
     * @param request the current request
     * @param left the left access
     * @param right the VALUES node
     * @param restrictions what the parent needs of the variables
     * @return the merged access
     */
    private static SqlIntercode joinWithValues(Request request, SqlTableAccess left, SqlValues right,
            Restrictions restrictions)
    {
        // the conditions are evaluated inside the access, so they refer to the table's own columns and expressions,
        // not to the aliases under which the access exposes them
        Conditions values = right.asConditions(request, left.schema, left.table, left.internal);

        if(values.isFalse())
            return SqlNoSolution.get();

        Conditions conditions = Conditions.and(left.conditions, values);

        return left.with(conditions, left.internal.restrict(request.getConfiguration(), restrictions));
    }


    /**
     * Returns whether the distinct access, which denotes a set of rows over the given distinct columns, is pinned by
     * the join to at most one row for each row of the other access to the same table, so that it only restricts the
     * rows of the other access and does not change their multiplicities. That requires the join columns (the columns
     * tied on both sides to the same variables that are always bound, or to the same constants, see
     * {@link #getJoinColumns}) to cover the distinct columns, all columns the distinct access binds and all columns its
     * conditions depend on. Otherwise the existence of a matching row is not decided by the row of the other access
     * itself: another row with the same join values may satisfy the conditions while that row does not, and a variable
     * that may be unbound is compatible also with the rows that differ in its columns.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param access the other access
     * @param distinct the deduplicated access
     * @param distinctColumns columns over which deduplication is requested
     * @return true if the distinct access can be merged without changing the multiplicities, false otherwise
     */
    static boolean canBeJoinedByDistinctColumns(ClassRelations relations, SqlTableAccess access,
            SqlTableAccess distinct, Set<Column> distinctColumns)
    {
        Set<Column> joinColumns = getJoinColumns(relations, distinct, access);

        if(!joinColumns.containsAll(distinctColumns))
            return false;

        if(!joinColumns.containsAll(distinct.internal.getNonConstantColumns()))
            return false;

        if(!joinColumns.containsAll(distinct.conditions.getNonConstantColumns()))
            return false;

        // nor is it decided by the row itself when the distinct access restricts a column outside the join
        if(!joinColumns.containsAll(distinct.getValueColumns(false)))
            return false;

        return true;
    }


    /**
     * Returns whether the distinct access is deduplicated over its own distinct columns and can be merged into the
     * other access, see {@link #canBeJoinedByDistinctColumns(ClassRelations, SqlTableAccess, SqlTableAccess, Set)}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param access the other access
     * @param distinct the deduplicated access
     * @return true if the distinct access can be merged without changing the multiplicities, false otherwise
     */
    private static boolean canBeJoinedByDistinctColumns(ClassRelations relations, SqlTableAccess access,
            SqlTableAccess distinct)
    {
        return !distinct.distinctColumns.isEmpty()
                && canBeJoinedByDistinctColumns(relations, access, distinct, distinct.distinctColumns);
    }


    /**
     * Merges two accesses to the same table joined on a key into one access.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access, no solution when the conditions contradict, or null when the merged conditions would
     *         have too many disjuncts (see {@link #conjoin})
     */
    static SqlIntercode joinByPrimaryKey(ClassRelations relations, SqlTableAccess left, SqlTableAccess right,
            Restrictions restrictions)
    {
        return join(relations, left, right, restrictions, union(left.distinctColumns, right.distinctColumns));
    }


    /**
     * Merges the distinct access into the other access to the same table, see {@link #canBeJoinedByDistinctColumns}.
     * The result keeps the multiplicities of the other access, including its deduplication request.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param access the other access
     * @param distinct the deduplicated access
     * @param restrictions what the parent needs of the variables
     * @return the merged access, no solution when the conditions contradict, or null when the merged conditions would
     *         have too many disjuncts (see {@link #conjoin})
     */
    static SqlIntercode joinByDistinctColumns(ClassRelations relations, SqlTableAccess access, SqlTableAccess distinct,
            Restrictions restrictions)
    {
        return join(relations, access, distinct, restrictions, access.distinctColumns);
    }


    /**
     * Merges two accesses to the same table: their conditions are conjoined with the equalities of the shared
     * variables; no solution when contradictory, and no merge when the conjunction would have too many disjuncts.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @param distinctColumns columns over which deduplication is requested
     * @return the merged access, no solution when the conditions contradict, or null when the merged conditions would
     *         have too many disjuncts (see {@link #conjoin})
     */
    private static SqlIntercode join(ClassRelations relations, SqlTableAccess left, SqlTableAccess right,
            Restrictions restrictions, Set<Column> distinctColumns)
    {
        Condition joinCondition = new Condition();
        VariableBindings bindings = new VariableBindings(left.internal);

        for(Variable var : right.internal.getVariables())
            if(left.internal.get(var) == null)
                bindings.add(right.internal.get(var));

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, left.internal, right.internal))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            // a variable the left side may leave unbound and the right side binds always in the row they share (see
            // isHeldAlike) is bound always in the merged row, which the conditions of the right side hold for
            if(leftBinding.canBeNull() && !rightBinding.canBeNull())
                bindings.add(rightBinding);

            if(!leftBinding.canBeNull() && !rightBinding.canBeNull())
            {
                for(ResourceClassPair pairedClass : pair.getClasses())
                {
                    List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(leftCols, rightCols, needsNullSafeEquality(left.schema, left.table,
                            pairedClass.getLeftClass(), leftCols, rightCols));
                }
            }
        }

        bindings = bindings.restrict(relations, restrictions);

        Conditions conditions = conjoin(left.conditions, right.conditions);

        if(conditions == null)
            return null;

        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        return new SqlTableAccess(left.schema, left.table, conditions, bindings, left.reduced && right.reduced,
                distinctColumns, mergeValues(left.values, right.values));
    }


    /**
     * Conjunction of the conditions of two accesses that are to be merged into one, or null when the merge is not worth
     * it: when the conjunction takes more conjunctions of their disjuncts (see
     * {@link Conditions#and(Conditions, Conditions, int)}) than {@link #disjunctLimit} and than either of the
     * conditions has disjuncts. It happens when the conditions restrict different columns to lists of values (each
     * access has taken in VALUES of a variable of its own), as then every value combines with every value; the
     * conditions restricting one column to lists of values give only the values the lists have in common, and a
     * condition of one disjunct leaves the number of the disjuncts of the other one as it is.
     *
     * @param left the conditions of one access
     * @param right the conditions of the other access, in terms of the columns of the merged access
     * @return conjunction of the conditions, or null when it would have too many disjuncts
     */
    private static Conditions conjoin(Conditions left, Conditions right)
    {
        int limit = Math.max(disjunctLimit, Math.max(left.getConditions().size(), right.getConditions().size()));

        return Conditions.and(left, right, limit);
    }


    /**
     * Merges the parent access into the child access along the foreign key, so the parent table is not accessed at all:
     * the bindings and the conditions of the parent and the values its rows are restricted to (see {@link #values}) are
     * moved onto the child columns of the key, which hold the values of the parent columns they reference.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @param key the foreign key
     * @param restrictions what the parent needs of the variables
     * @return the merged access, no solution when the conditions contradict, or null when the merged conditions would
     *         have too many disjuncts (see {@link #conjoin})
     */
    static SqlIntercode joinByForeignKey(ClassRelations relations, SqlTableAccess parent, SqlTableAccess child,
            Set<ColumnPair> key, Restrictions restrictions)
    {
        // only the parent columns of the key have their values in the child row; a parent column equated with one of
        // them by the parent condition must not be moved onto the same child column, which would turn that equality
        // into a tautology
        Map<Column, Column> map = new HashMap<>();

        for(ColumnPair pair : key)
            map.put(pair.getLeft(), pair.getRight());


        Condition joinCondition = new Condition();
        VariableBindings bindings = new VariableBindings(child.internal);

        for(Variable var : parent.internal.getVariables())
            if(child.internal.get(var) == null)
                bindings.add(remap(map, parent.internal.get(var)));

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, child.internal, parent.internal))
        {
            VariableBinding childBinding = pair.getLeftVariableBinding();
            VariableBinding parentBinding = pair.getRightVariableBinding();

            if(!childBinding.canBeNull() && !parentBinding.canBeNull())
            {
                for(ResourceClassPair pairedClass : pair.getClasses())
                {
                    List<Column> childCols = childBinding.getMapping(pairedClass.getLeftClass());
                    List<Column> parentCols = parentBinding.getMapping(pairedClass.getRightClass());

                    List<Column> remapped = remap(map, parentCols);

                    joinCondition.addAreEqual(childCols, remapped, needsNullSafeEquality(child.schema, child.table,
                            pairedClass.getLeftClass(), childCols, remapped));
                }
            }
        }

        bindings = bindings.restrict(relations, restrictions);

        Conditions conditions = conjoin(child.conditions, remap(map, parent.conditions));

        if(conditions == null)
            return null;

        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        // each child row has exactly one parent row, so only the deduplication of the child remains; a value the
        // parent is restricted to restricts the child column referencing the restricted one
        return new SqlTableAccess(child.schema, child.table, conditions, bindings, child.reduced && parent.reduced,
                child.distinctColumns, mergeValues(child.values, getReferencedValues(parent, key)));
    }


    /**
     * Merges a left join of two accesses to the same table on a key: right-side variables become nullable, an extra
     * not-null condition of the right side becomes a CASE guarding its columns.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access
     */
    private static SqlTableAccess leftJoinByPrimaryKey(ClassRelations relations, SqlTableAccess left,
            SqlTableAccess right, Restrictions restrictions)
    {
        Set<Column> extraNotNulls = new HashSet<>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());
        Column extraCondition = extraNotNulls.isEmpty() ? null : extraNotNulls.iterator().next();

        Conditions conditions = new Conditions(left.conditions);
        VariableBindings bindings = new VariableBindings(left.internal);

        for(Variable var : right.internal.getVariables())
            if(left.internal.get(var) == null)
                bindings.add(toOptional(right.internal.get(var), extraCondition));

        bindings = bindings.restrict(relations, restrictions);

        return new SqlTableAccess(left.schema, left.table, conditions, bindings, left.reduced && right.reduced,
                union(left.distinctColumns, right.distinctColumns), left.values);
    }


    /**
     * Merges a distinct union of a parent and a child access along a foreign key into the parent access, which yields
     * the values of all the child rows (see {@link #canBeDistinctUnionizedByForeignKey}).
     *
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @return the merged access
     */
    private static SqlIntercode distinctUnionizeByForeignKey(SqlTableAccess parent, SqlTableAccess child)
    {
        Conditions conditions = new Conditions(parent.conditions);

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding binding : parent.internal.getValues())
        {
            boolean canBeNull = binding.canBeNull() || child.internal.get(binding.getVariable()).canBeNull();
            bindings.add(new VariableBinding(binding.getVariable(), binding.getMappings(), canBeNull));
        }

        return new SqlTableAccess(parent.schema, parent.table, conditions, bindings, true, parent.distinctColumns,
                parent.values);
    }


    /**
     * Merges a distinct union of two accesses to the same table binding the same columns into one access with the
     * disjunction of the conditions.
     *
     * @param left the left access
     * @param right the right access
     * @return the merged access
     */
    private static SqlTableAccess distinctUnionizeByPrimaryKey(SqlTableAccess left, SqlTableAccess right)
    {
        Conditions conditions = Conditions.or(left.conditions, right.conditions);

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding binding : left.internal.getValues())
        {
            boolean canBeNull = binding.canBeNull() || right.internal.get(binding.getVariable()).canBeNull();
            bindings.add(new VariableBinding(binding.getVariable(), binding.getMappings(), canBeNull));
        }

        return new SqlTableAccess(left.schema, left.table, conditions, bindings, true,
                union(left.distinctColumns, right.distinctColumns), left.values);
    }


    /**
     * Merges a VALUES node into the access as conditions when its rows are distinct, bound in classes the access binds
     * the variables in or convertible to them, and few enough (see {@link #canBeJoinedWithValues}), or returns null.
     *
     * @param request the current request
     * @param left the left access
     * @param right the VALUES node
     * @param mergeRestrictions what the parent needs of the variables
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceJoinWithValues(Request request, SqlTableAccess left, SqlValues right,
            Restrictions mergeRestrictions)
    {
        if(SqlTableAccess.canBeJoinedWithValues(left, right))
            return joinWithValues(request, left, right, mergeRestrictions);

        return null;
    }


    /**
     * Merges the two accesses into one when a foreign key, a shared key or declared distinct columns allow it and the
     * merged conditions do not get too many disjuncts (see {@link #conjoin}); null otherwise.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceJoin(ClassRelations relations, DatabaseSchema schema, SqlTableAccess left,
            SqlTableAccess right, Restrictions restrictions)
    {
        Set<ColumnPair> dropLeft = SqlTableAccess.canBeJoinedByForeignKey(relations, schema, left, right);

        if(dropLeft != null)
            return SqlTableAccess.joinByForeignKey(relations, left, right, dropLeft, restrictions);


        Set<ColumnPair> dropRight = SqlTableAccess.canBeJoinedByForeignKey(relations, schema, right, left);

        if(dropRight != null)
            return SqlTableAccess.joinByForeignKey(relations, right, left, dropRight, restrictions);


        if(SqlTableAccess.canBeJoinedByPrimaryKey(relations, schema, left, right))
            return SqlTableAccess.joinByPrimaryKey(relations, left, right, restrictions);


        if(SqlTableAccess.canBeJoinedByDistinctColumns(relations, left, right))
            return SqlTableAccess.joinByDistinctColumns(relations, left, right, restrictions);

        if(SqlTableAccess.canBeJoinedByDistinctColumns(relations, right, left))
            return SqlTableAccess.joinByDistinctColumns(relations, right, left, restrictions);

        return null;
    }


    /**
     * Merges a left join of two accesses to the same table on a key into one access; null otherwise.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceLeftJoin(ClassRelations relations, DatabaseSchema schema, SqlTableAccess left,
            SqlTableAccess right, Restrictions restrictions)
    {
        if(SqlTableAccess.canBeLeftJoinedByPrimaryKey(relations, schema, left, right))
            return SqlTableAccess.leftJoinByPrimaryKey(relations, left, right, restrictions);

        return null;
    }


    /**
     * The access joined with the access of a part of an OPTIONAL that reads the row of the access by a key and
     * restricts it no further, or null: the part has a row for every row of the access, so a left join may take it from
     * its right side to its left side (inspired by the join transfer of Ontop), see
     * {@link #canBeLeftJoinedByPrimaryKey}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param access the access of the left side
     * @param part the access of a part of the OPTIONAL
     * @return the access joined with the part, or null
     */
    public static SqlTableAccess tryTransfer(ClassRelations relations, DatabaseSchema schema, SqlTableAccess access,
            SqlTableAccess part)
    {
        if(!canBeLeftJoinedByPrimaryKey(relations, schema, access, part)
                || !access.conditions.getIsNotNull().containsAll(part.conditions.getIsNotNull()))
            return null;

        return joinByPrimaryKey(relations, access, part, null) instanceof SqlTableAccess joined ? joined : null;
    }


    /**
     * Merges two accesses to the same table, each the right side of an OPTIONAL of the same solutions, into the access
     * of a single OPTIONAL, or returns null (a merge inspired by the merging of left joins in Ontop). Both accesses
     * have to be joined to those solutions by the given variables only, bound alike on both sides and holding a key of
     * the table, so that they read the same row of a solution, if any; their conditions have to be the same but for a
     * requirement that a column of their own be not null, at most one on each side. The merged access reads the row by
     * the conditions they share, and a variable of one side only is unbound in a row in which the column that side
     * requires to be not null is null (see {@link #toOptional}), as it is unbound when that OPTIONAL has no solution.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param first the access of one OPTIONAL
     * @param second the access of the other OPTIONAL
     * @param shared the variables both accesses share with the solutions they extend
     * @return the merged access, or null
     */
    public static SqlTableAccess tryMergeOptionals(ClassRelations relations, DatabaseSchema schema,
            SqlTableAccess first, SqlTableAccess second, Set<Variable> shared)
    {
        if(first.table == null || !first.table.equals(second.table) || !first.hasSameValues(second))
            return null;

        Set<Variable> common = new HashSet<>(first.internal.getVariables());
        common.retainAll(second.internal.getVariables());

        if(!common.equals(shared))
            return null;

        Set<Column> columns = new HashSet<>();

        for(Variable variable : shared)
        {
            VariableBinding binding = first.internal.get(variable);

            if(binding.canBeNull() || !binding.equals(second.internal.get(variable)) || binding.hasExpressionColumn())
                return null;

            columns.addAll(binding.getNonConstantColumns());
        }

        if(schema.getCompatibleKey(first.table,
                first.getKnownNotNull(schema, first.getCoveredColumns(columns))) == null)
            return null;

        Set<Column> firstExtra = new HashSet<>(first.conditions.getIsNotNull());
        firstExtra.removeAll(second.conditions.getIsNotNull());

        Set<Column> secondExtra = new HashSet<>(second.conditions.getIsNotNull());
        secondExtra.removeAll(first.conditions.getIsNotNull());

        if(firstExtra.size() > 1 || secondExtra.size() > 1)
            return null;

        Conditions conditions = withoutIsNotNull(first.conditions, firstExtra);

        if(!conditions.equals(withoutIsNotNull(second.conditions, secondExtra)))
            return null;

        Column firstColumn = firstExtra.isEmpty() ? null : firstExtra.iterator().next();
        Column secondColumn = secondExtra.isEmpty() ? null : secondExtra.iterator().next();

        VariableBindings bindings = new VariableBindings();

        for(Variable variable : first.internal.getVariables())
            bindings.add(shared.contains(variable) ? first.internal.get(variable) :
                    toOptional(first.internal.get(variable), firstColumn));

        for(Variable variable : second.internal.getVariables())
            if(!shared.contains(variable))
                bindings.add(toOptional(second.internal.get(variable), secondColumn));

        return new SqlTableAccess(first.schema, first.table, conditions, bindings, first.reduced && second.reduced,
                union(first.distinctColumns, second.distinctColumns), first.values);
    }


    /**
     * The access of an OPTIONAL as a member of an inner join with the solutions it extends, or null (inspired by Ontop,
     * which turns a left join into an inner one where a foreign key guarantees the right side a row). The solutions are
     * made of rows of the referencing access, which binds the shared variables always from the columns of a foreign key
     * referencing a key of the table of the OPTIONAL by exactly the columns that access binds them from, so every
     * solution has its row. The access of the OPTIONAL may require nothing more of the row but that one column of its
     * own be not null, and a variable of its own is unbound in a row in which that column is null (see
     * {@link #toOptional}), as it is unbound when the OPTIONAL has no solution.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param referencing an access the solutions are made of, binding the shared variables always
     * @param optional the access of the OPTIONAL
     * @param shared the variables the access of the OPTIONAL shares with the solutions, bound always by both
     * @return the access of the OPTIONAL as a member of an inner join, or null
     */
    public static SqlTableAccess tryJoinByForeignKey(ClassRelations relations, DatabaseSchema schema,
            SqlTableAccess referencing, SqlTableAccess optional, Set<Variable> shared)
    {
        if(optional.table == null || referencing.table == null || !optional.values.isEmpty())
            return null;

        Set<Column> columns = new HashSet<>();

        for(Variable variable : shared)
        {
            VariableBinding binding = optional.internal.get(variable);
            VariableBinding other = referencing.internal.get(variable);

            if(binding == null || other == null || binding.canBeNull() || other.canBeNull())
                return null;

            columns.addAll(binding.getNonConstantColumns());
        }

        Set<ColumnPair> pairs = getJoinColumnPairs(relations, optional, referencing).stream()
                .filter(p -> referencing.isKnownNotNull(schema, p.getRight())).collect(toSet());

        if(columns.isEmpty()
                || schema.getCompatibleForeignKey(optional.table, referencing.table, pairs, columns) == null)
            return null;

        // the referenced row may be required to hold a column of its own only, besides its key
        Set<Column> extra = new HashSet<>(optional.conditions.getIsNotNull());
        extra.removeAll(columns);

        Set<Column> required = new HashSet<>(extra);
        required.addAll(columns);

        if(extra.size() > 1 || !withoutIsNotNull(optional.conditions, required).isTrue())
            return null;

        Column column = extra.isEmpty() ? null : extra.iterator().next();
        VariableBindings bindings = new VariableBindings();

        for(Variable variable : optional.internal.getVariables())
            bindings.add(shared.contains(variable) ? optional.internal.get(variable) :
                    toOptional(optional.internal.get(variable), column));

        return new SqlTableAccess(optional.schema, optional.table, withoutIsNotNull(optional.conditions, extra),
                bindings, optional.reduced, optional.distinctColumns, optional.values);
    }


    /**
     * Merges a distinct union of two accesses related by a foreign key into one access; null otherwise.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceDistinctUnion(ClassRelations relations, DatabaseSchema schema,
            SqlTableAccess left, SqlTableAccess right)
    {
        if(SqlTableAccess.canBeDistinctUnionizedByForeignKey(relations, schema, left, right))
            return SqlTableAccess.distinctUnionizeByForeignKey(left, right);

        if(SqlTableAccess.canBeDistinctUnionizedByForeignKey(relations, schema, right, left))
            return SqlTableAccess.distinctUnionizeByForeignKey(right, left);


        if(SqlTableAccess.canBeDistinctUnionizedByPrimaryKey(schema, left, right))
            return SqlTableAccess.distinctUnionizeByPrimaryKey(left, right);

        return null;
    }


    /**
     * Combines the deduplication requests of two accesses to the same table that are merged through a key. An empty set
     * stands for rows distinguished by their own identity, which is the finest distinction possible, so it absorbs the
     * other side: each row of a multiset side matches at most one row of the other side and dictates the multiplicities
     * of the result. Two non-empty sets denote through the key the same rows, so their union applies.
     *
     * @param left the first set
     * @param right the second set
     * @return union of the two sets
     */
    private static Set<Column> union(Set<Column> left, Set<Column> right)
    {
        if(left.isEmpty() || right.isEmpty())
            return Set.of();

        Set<Column> result = new HashSet<>(left);
        result.addAll(right);
        return result;
    }


    /**
     * The bindings with their columns renamed by the map.
     *
     * @param map the column map
     * @param bindings the variable bindings
     * @return the bindings with their columns renamed by the map
     */
    protected static VariableBindings remap(Map<Column, Column> map, VariableBindings bindings)
    {
        VariableBindings result = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
            result.add(remap(map, binding));

        return result;
    }


    /**
     * The binding with its columns renamed by the map.
     *
     * @param map the column map
     * @param binding the variable binding
     * @return the binding with its columns renamed by the map
     */
    protected static VariableBinding remap(Map<Column, Column> map, VariableBinding binding)
    {
        VariableBinding result = new VariableBinding(binding.getVariable(), binding.canBeNull());

        for(Map.Entry<ResourceClass, List<Column>> mapping : binding.getMappings().entrySet())
            result.addMapping(mapping.getKey(), remap(map, mapping.getValue()));

        return result;
    }


    /**
     * The columns renamed by the map.
     *
     * @param map the column map
     * @param columns the columns
     * @return the columns renamed by the map
     */
    protected static List<Column> remap(Map<Column, Column> map, List<Column> columns)
    {
        if(columns == null)
            return null;

        List<Column> result = new ArrayList<>();

        for(Column column : columns)
            result.add(remap(map, column));

        return result;
    }


    /**
     * The column renamed by the map (unchanged if absent).
     *
     * @param map the column map
     * @param column the column
     * @return the column renamed by the map (unchanged if absent)
     */
    protected static Column remap(Map<Column, Column> map, Column column)
    {
        if(column == null)
            return null;

        if(column instanceof ConstantColumn)
            return column;

        if(map.get(column) == null)
            throw new RuntimeException("not found: " + column.getName());

        return map.get(column);
    }


    /**
     * The condition with its columns renamed by the map.
     *
     * @param map the column map
     * @param conditions the conditions
     * @return the condition with its columns renamed by the map
     */
    protected static Condition remap(Map<Column, Column> map, Condition conditions)
    {
        Condition result = new Condition();

        for(Column c : conditions.getIsNotNull())
            result.addIsNotNull(remap(map, c));

        for(Column c : conditions.getIsNull())
            result.addIsNull(remap(map, c));

        for(ColumnComparison e : conditions.getAreEqual())
            result.addAreEqual(remap(map, e.getLeft()), remap(map, e.getRight()));

        for(ColumnComparison e : conditions.getAreNotDistinct())
            result.addAreNotDistinct(remap(map, e.getLeft()), remap(map, e.getRight()));

        for(ColumnComparison p : conditions.getAreNotEqual())
            result.addAreNotEqual(remap(map, p.getLeft()), remap(map, p.getRight()));

        return result;
    }


    /**
     * The conditions with their columns renamed by the map.
     *
     * @param map the column map
     * @param conditions the conditions
     * @return the conditions with their columns renamed by the map
     */
    protected static Conditions remap(Map<Column, Column> map, Conditions conditions)
    {
        Conditions result = new Conditions(false);

        for(Condition condition : conditions.getConditions())
            result.add(remap(map, condition));

        return result;
    }


    /**
     * True if some bound or conditioned column is an SQL expression.
     *
     * @return true if some bound or conditioned column is an SQL expression, false otherwise
     */
    boolean hasExpression()
    {
        if(internal.getNonConstantColumns().stream().anyMatch(c -> c instanceof ExpressionColumn))
            return true;

        if(conditions.getNonConstantColumns().stream().anyMatch(c -> c instanceof ExpressionColumn))
            return true;

        return false;
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        if(conditions.isFalse())
            return SqlNoSolution.get();

        // the values the parent accepts for the variables restrict the rows by themselves
        List<ValueCondition> optimizedValues = optimizeValues(request, scope, restrictions, evalServices);

        if(optimizedValues == null)
            return SqlNoSolution.get();

        // so do the terms the parent accepts of the variables, as conditions on the rows
        Conditions optimizedConditions = restrictToAcceptedTerms(request, restrictions);

        if(optimizedConditions.isFalse())
            return SqlNoSolution.get();

        VariableBindings optimizedBindings = internal.restrict(relations, restrictions);

        // a row of a dictionary table that constants select is read now and taken by the key the access is joined by
        optimizedConditions = resolveDictionaryRows(request, optimizedConditions, optimizedBindings, optimizedValues);

        if(optimizedConditions.isFalse())
            return SqlNoSolution.get();

        // a probed table is asked whether it has a row the constants select
        optimizedConditions = probeRows(request, optimizedConditions);

        if(optimizedConditions.isFalse())
            return SqlNoSolution.get();

        Set<Column> optimizedDistinct = distinctColumns;

        if(!distinctColumns.isEmpty() && isDistinctImpliedByKey(request.getConfiguration().getDatabaseSchema()))
            optimizedDistinct = Set.of();

        if(this.reduced == reduced && optimizedBindings.equals(internal) && optimizedDistinct == distinctColumns
                && optimizedValues == values && optimizedConditions == conditions)
            return this;

        return new SqlTableAccess(schema, table, optimizedConditions, optimizedBindings, reduced, optimizedDistinct,
                optimizedValues);
    }


    /**
     * The conditions on the rows together with the terms the parent accepts of the variables the access binds always in
     * a single class (see {@link Restrictions#getAcceptedTerms}), when the join optimizer is on: a row holding another
     * term in the columns of such a variable gives a solution that never reaches the result. The terms are converted to
     * the class of the columns and make a disjunction of equalities with them, as the rows of inline data merged into
     * the access do (see {@link SqlValues#asConditions}), so that the database looks the rows up by an index and the
     * join optimizer estimates them by the statistics of the table for every term; the filter accepting the terms
     * compares values, which the database can neither look up nor estimate where the columns hold the terms in boxes,
     * and which it does not push into an access that is looked up for the rows of another one. The filter stays above
     * the access. A term the class cannot hold is left out, and no row is left when it can hold none. The conditions
     * stay as they are when they restrict the rows so already, and when the disjunction would give them too many
     * disjuncts (see {@link #disjunctLimit}). Without the optimizer the database plans the joins by its own estimates,
     * which would count the filter and the conditions as two restrictions, so the terms are left to the filter.
     *
     * @param request the current request
     * @param restrictions what the parent needs of the variables
     * @return the conditions on the rows together with the accepted terms (the conditions of this access when nothing
     *         has changed)
     */
    private Conditions restrictToAcceptedTerms(Request request, Restrictions restrictions)
    {
        if(table == null || !request.getConfiguration().isJoinOptimizerEnabled())
            return conditions;

        Conditions result = conditions;

        for(VariableBinding binding : internal.getValues())
        {
            Set<RdfTerm> terms = restrictions.getAcceptedTerms(binding.getVariable());
            Conditions accepted = terms == null ? null : getTermConditions(request, binding, terms);

            if(accepted == null || result.implies(accepted))
                continue;

            if((long) result.getConditions().size() * accepted.getConditions().size() > disjunctLimit)
                continue;

            result = Conditions.and(result, accepted);
        }

        return result;
    }


    /**
     * The conditions on the rows of a dictionary table (see
     * {@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#addDictionaryTable}) in terms of the key the
     * access is joined by: the key of the table whose columns are exactly the columns the bindings read. A disjunct
     * that restricts the columns of another key to constants selects one row at most, which is read now (see
     * {@link #resolveDictionaryRow}), and the disjunct becomes the equalities of the columns of the key the access is
     * joined by with their values in that row, or is left out when there is no such row (no disjunct left is no
     * solution). The access then refers to no other column than those of that key, so it is merged into an access
     * referencing the key by a foreign key (see {@link #joinByForeignKey}), which takes the constants in its own
     * columns: the dictionary table is not read by the query, and the rows of the referencing table are estimated and
     * looked up by the constants. The conditions stay as they are when the bindings read another column as well, as the
     * query has to read the table for it anyway, and when the rows are restricted to values (see {@link #values}).
     *
     * @param request the current request
     * @param conditions the conditions on the rows
     * @param bindings the bindings the parent needs, in terms of the table's own columns
     * @param values the values the rows are restricted to
     * @return the conditions in terms of the key the access is joined by (the given ones when nothing has changed)
     */
    private Conditions resolveDictionaryRows(Request request, Conditions conditions, VariableBindings bindings,
            List<ValueCondition> values)
    {
        if(table == null || !values.isEmpty() || !request.getConfiguration().isDictionaryTable(table))
            return conditions;

        Set<Column> bound = bindings.getNonConstantColumns();
        List<Column> key = schema.getCompatibleKey(table, bound);

        if(key == null || !key.containsAll(bound))
            return conditions;

        Conditions result = new Conditions(false);
        boolean changed = false;

        for(Condition condition : conditions.getConditions())
        {
            Condition resolved = resolveDictionaryRow(request, condition, key);

            changed |= resolved != condition;

            if(resolved != null)
                result.add(resolved);
        }

        return changed ? result : conditions;
    }


    /**
     * A disjunct of the conditions on the rows of a dictionary table in terms of the given key (see
     * {@link #resolveDictionaryRows}): when the disjunct refers to other columns than those of the key and restricts
     * the columns of some key of the table to constants, the rows it holds for are read from the table, which is one
     * row at most, and the disjunct becomes the equalities of the columns of the given key with their values in that
     * row. As the whole disjunct is evaluated, the row satisfies its other parts too. A row read during the request
     * before is taken from the request (see {@link Request#getDictionaryRows}).
     *
     * @param request the current request
     * @param condition the disjunct
     * @param key the key the access is joined by
     * @return the disjunct in terms of the key, the given one when it stays as it is, or null when no row satisfies it
     * @throws SQLRuntimeException when the table cannot be read
     */
    private Condition resolveDictionaryRow(Request request, Condition condition, List<Column> key)
    {
        if(key.containsAll(condition.getNonConstantColumns()))
            return condition;

        Set<Column> constants = new HashSet<>();

        for(Set<ColumnComparison> comparisons : List.of(condition.getAreEqual(), condition.getAreNotDistinct()))
        {
            for(ColumnComparison comparison : comparisons)
            {
                if(comparison.getLeft() instanceof TableColumn column && isConstant(comparison.getRight()))
                    constants.add(column);
                else if(comparison.getRight() instanceof TableColumn column && isConstant(comparison.getLeft()))
                    constants.add(column);
            }
        }

        if(schema.getCompatibleKey(table, constants) == null)
            return condition;

        SqlBuilder builder = new SqlBuilder();
        builder.append("SELECT ");
        builder.append(key.stream().map(c -> SqlCode.of("(", c, ")::text")).collect(SqlCode.joining(", ")));
        builder.append(" FROM ");
        builder.append(table);
        appendConditions(builder, new Conditions(condition), null, Set.of());
        builder.append(" LIMIT 2");

        SqlCode code = builder.toCode();

        // the disjunct has to be decided by the row alone
        if(!code.getOuterTables().isEmpty() || !code.getDeclaredTables().isEmpty())
            return condition;

        List<List<String>> rows = request.getDictionaryRows().get(code.getText());

        if(rows == null)
        {
            try
            {
                rows = request.lookup(List.of(code.getText()), result -> {
                    List<List<String>> read = new ArrayList<>();

                    while(result.next())
                    {
                        List<String> row = new ArrayList<>(key.size());

                        for(int i = 1; i <= key.size(); i++)
                            row.add(result.getString(i));

                        read.add(row);
                    }

                    return read;
                }).get(0);
            }
            catch(SQLException e)
            {
                throw new SQLRuntimeException(e);
            }

            request.getDictionaryRows().put(code.getText(), rows);
        }

        if(rows.isEmpty())
            return null;

        // more rows than one, or a row without a value of the key, cannot be taken by the key
        if(rows.size() > 1 || rows.get(0).contains(null))
            return condition;

        Condition resolved = new Condition();

        for(int i = 0; i < key.size(); i++)
            resolved.addAreEqual(key.get(i), new ValueColumn(rows.get(0).get(i), key.get(i).getType()));

        return resolved;
    }


    /**
     * The conditions on the rows of a probed table (see
     * {@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#addProbedTable}) without the disjuncts that no
     * row of the table satisfies, as far as an index tells (see {@link #hasRows}); no disjunct left is no solution.
     *
     * @param request the current request
     * @param conditions the conditions on the rows
     * @return the conditions without the disjuncts no row satisfies (the given ones when nothing has changed)
     */
    private Conditions probeRows(Request request, Conditions conditions)
    {
        if(table == null || !request.getConfiguration().isProbedTable(table))
            return conditions;

        Conditions result = new Conditions(false);
        boolean changed = false;

        for(Condition condition : conditions.getConditions())
        {
            if(hasRows(request, condition))
                result.add(condition);
            else
                changed = true;
        }

        return changed ? result : conditions;
    }


    /**
     * False if the table is found to have no row that a disjunct of the conditions holds for: the table is asked for a
     * row with the constants the disjunct requires in the leading columns of one of its indexes (see
     * {@link DatabaseSchema#getLookupColumns}), which is a lookup in that index, and the disjunct holds for none when
     * there is none. The other parts of the disjunct are left out of the question, as a row satisfying them might be
     * found only after many that do not. The question is asked with the scans of the whole table turned off for it (see
     * {@link #probeSettings}): the database would otherwise scan the table for a row it estimates to be common,
     * expecting to meet one soon, which reads the whole table when the rows lie in the order of the column asked about
     * and there is none, or the first one lies far. A disjunct that requires no constant in a leading column of an
     * index is not asked about. An answer got during the request before is taken from the request (see
     * {@link Request#getProbedRows}).
     *
     * @param request the current request
     * @param condition the disjunct
     * @return false if the table is found to have no row the disjunct holds for, true otherwise
     * @throws SQLRuntimeException when the table cannot be read
     */
    private boolean hasRows(Request request, Condition condition)
    {
        Map<Column, Column> constants = new HashMap<>();

        for(ColumnComparison comparison : condition.getAreEqual())
        {
            if(comparison.getLeft() instanceof TableColumn column && isConstant(comparison.getRight()))
                constants.putIfAbsent(column, comparison.getRight());
            else if(comparison.getRight() instanceof TableColumn column && isConstant(comparison.getLeft()))
                constants.putIfAbsent(column, comparison.getLeft());
        }

        List<Column> columns = schema.getLookupColumns(table, constants.keySet());

        if(columns.isEmpty())
            return true;

        Condition probe = new Condition();

        for(Column column : columns)
            probe.addAreEqual(column, constants.get(column));

        SqlBuilder builder = new SqlBuilder();
        builder.append("SELECT 1 FROM ");
        builder.append(table);
        appendConditions(builder, new Conditions(probe), null, Set.of());
        builder.append(" LIMIT 1");

        SqlCode code = builder.toCode();

        // the question has to be answered by the table alone
        if(!code.getOuterTables().isEmpty() || !code.getDeclaredTables().isEmpty())
            return true;

        Boolean found = request.getProbedRows().get(code.getText());

        if(found == null)
        {
            try
            {
                found = request.lookup(probeSettings, List.of(code.getText()), result -> result.next()).get(0);
            }
            catch(SQLException e)
            {
                throw new SQLRuntimeException(e);
            }

            request.getProbedRows().put(code.getText(), found);
        }

        return found;
    }


    /**
     * True if the column stands for a constant in a condition on the rows: a constant value, or an expression, which is
     * how a constant term is written in the class of some columns (like an IRI put into a box).
     *
     * @param column the column
     * @return true if the column stands for a constant in a condition on the rows, false otherwise
     */
    private static boolean isConstant(Column column)
    {
        return column instanceof ValueColumn || column instanceof ExpressionColumn;
    }


    /**
     * The condition that the columns of a variable hold one of the given terms: a disjunction of the equalities of the
     * columns with the terms converted to their class, a term the class cannot hold being left out (the condition is
     * false when it can hold none). Null when the access does not bind the variable always, in a single class that has
     * columns.
     *
     * @param request the current request
     * @param binding binding of the variable in terms of the table's own columns, or null
     * @param terms the terms
     * @return the condition that the columns of the variable hold one of the terms, or null
     */
    private Conditions getTermConditions(Request request, VariableBinding binding, Set<RdfTerm> terms)
    {
        if(binding == null || binding.canBeNull() || binding.getMappings().size() != 1)
            return null;

        Entry<ResourceClass, List<Column>> mapping = binding.getMappings().entrySet().iterator().next();

        if(mapping.getValue() == null)
            return null;

        Conditions accepted = new Conditions(false);

        for(RdfTerm term : terms)
        {
            if(!request.match(mapping.getKey(), term))
                continue;

            List<Column> columns = request.getColumns(mapping.getKey(), term);

            Condition condition = new Condition();
            condition.addAreEqual(mapping.getValue(), columns,
                    needsNullSafeEquality(schema, table, mapping.getKey(), mapping.getValue(), columns));

            accepted.add(condition);
        }

        return accepted;
    }


    /**
     * True if the conditions restrict the rows to those holding one of the given terms in the columns of the variable,
     * as they do once the access has taken the terms a filter accepts of the variable in (see
     * {@link #restrictToAcceptedTerms}); the join optimizer then counts the filter by the rows of the access alone.
     *
     * @param request the current request
     * @param variable the variable
     * @param terms the terms
     * @return true if the conditions restrict the rows to those holding one of the terms in the columns of the
     *         variable, false otherwise
     */
    boolean isRestrictedTo(Request request, Variable variable, Set<RdfTerm> terms)
    {
        Conditions accepted = getTermConditions(request, internal.get(variable), terms);

        return accepted != null && conditions.implies(accepted);
    }


    /**
     * The values of the current solution the rows are restricted to (see {@link #values}) as the current solution
     * provides them now, together with the values the parent accepts for the variables the access binds always (see
     * {@link Restrictions#getAcceptedValue}): a row holding another term in the columns of such a variable gives a
     * solution that never reaches the result. A value is taken in when it is never unbound and can be converted to the
     * class of the columns with the check of its representability; otherwise the node merging the variable with the
     * value goes on comparing them. A value taken in before stays, because that node may be gone (see
     * {@link #isRestricted}), unless the current solution does not bind it at all any more, which leaves every row
     * compatible. A value that is never unbound and of no class overlapping the class of the columns, or differing from
     * the constants the columns are, is held by no row.
     *
     * @param request the current request
     * @param scope the scope of the access
     * @param restrictions what the parent needs of the variables
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the values the rows are restricted to (the list of this access when nothing has changed), or null when no
     *         row can hold a value
     */
    private List<ValueCondition> optimizeValues(Request request, Scope scope, Restrictions restrictions,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        List<ValueCondition> result = new ArrayList<>();

        for(ValueCondition condition : values)
        {
            // the value is read from the current solution as the node providing that solution provides it now
            SqlExpressionIntercode value = condition.value().optimize(request, scope, new VariableBindings(),
                    Restriction.ALL, evalServices);

            if(value.equals(SqlNull.get()))
                continue;

            if(isHeldByNoRow(relations, condition.resourceClass(), condition.columns(), value))
                return null;

            result.add(value == condition.value() ? condition :
                    new ValueCondition(condition.resourceClass(), condition.columns(), value));
        }

        for(VariableBinding binding : internal.getValues())
        {
            SqlExpressionIntercode accepted = restrictions.getAcceptedValue(binding.getVariable());

            if(accepted == null || binding.canBeNull() || binding.getMappings().size() != 1)
                continue;

            Entry<ResourceClass, List<Column>> mapping = binding.getMappings().entrySet().iterator().next();

            if(mapping.getValue() == null)
                continue;

            if(containsAll(result, List.of(new ValueCondition(mapping.getKey(), mapping.getValue(), accepted))))
                continue;

            SqlExpressionIntercode value = accepted.optimize(request, scope, new VariableBindings(), Restriction.ALL,
                    evalServices);

            if(value.equals(SqlNull.get()) || !isComparedInClass(relations, mapping.getKey(), value))
                continue;

            if(isHeldByNoRow(relations, mapping.getKey(), mapping.getValue(), value))
                return null;

            result.add(new ValueCondition(mapping.getKey(), mapping.getValue(), value));
        }

        return result.equals(values) ? values : result;
    }


    /**
     * The conditions on the rows together with the equalities of the columns with the constants the rows are restricted
     * to (see {@link #values}), which are conditions on the rows as well; a value of a current solution, known only
     * when the statement runs, adds nothing. A constant term is a constant also where its conversion to the class of
     * the columns is an expression and no constant column, like an IRI or a literal put into a box, as the constants of
     * the triple patterns are in the conditions on the rows. The join optimizer estimates the rows of the access by
     * them.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @return the conditions on the rows together with the equalities of the columns with the constants the rows are
     *         restricted to
     */
    Conditions getConstantConditions(ClassRelations relations)
    {
        Condition constants = new Condition();

        for(ValueCondition condition : values)
        {
            SqlExpressionIntercode value = condition.value();

            if(value.isCurrentValue() || !isComparedInClass(relations, condition.resourceClass(), value))
                continue;

            List<Column> derived = value.getBinding().deriveMapping(relations, condition.resourceClass());

            if(value.getBinding().isConstant() || derived.stream().allMatch(c -> c instanceof ConstantColumn))
                constants.addAreEqual(condition.columns(), derived, condition.resourceClass()::isOptionalColumn);
        }

        return constants.isTrue() ? conditions : Conditions.and(conditions, constants);
    }


    /**
     * True if the value can be compared with columns of the class in that class: it is never unbound and it can be
     * converted to the class with the check of its representability (see {@link VariableBinding#canDeriveMapping}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param resClass class of the columns
     * @param value expression giving the value
     * @return true if the value can be compared in the class of the columns, false otherwise
     */
    private static boolean isComparedInClass(ClassRelations relations, ResourceClass resClass,
            SqlExpressionIntercode value)
    {
        return !value.canBeNull() && value.getBinding().canDeriveMapping(relations, resClass)
                && value.getBinding().deriveMapping(relations, resClass) != null;
    }


    /**
     * True if no row can hold the value in the columns: the value is never unbound and either it is of no class
     * overlapping the class of the columns, or it differs from the constants the columns are.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param resClass class of the columns
     * @param columns the columns
     * @param value expression giving the value
     * @return true if no row can hold the value in the columns, false otherwise
     */
    private static boolean isHeldByNoRow(ClassRelations relations, ResourceClass resClass, List<Column> columns,
            SqlExpressionIntercode value)
    {
        if(value.canBeNull())
            return false;

        if(!value.getBinding().contains(relations, resClass))
            return true;

        if(!isComparedInClass(relations, resClass, value))
            return false;

        List<Column> derived = value.getBinding().deriveMapping(relations, resClass);

        for(int i = 0; i < derived.size(); i++)
            if(derived.get(i) instanceof ConstantColumn && columns.get(i) instanceof ConstantColumn
                    && !derived.get(i).equals(columns.get(i)))
                return true;

        return false;
    }


    /**
     * SQL condition restricting the rows to the values the access has taken (see {@link #values}): the columns equal
     * the value converted to their class, which yields nulls for a value outside the class, so that the database can
     * look the rows up by an index. A value that cannot be compared so (which it could when it was taken in) is
     * compared as a join would compare it: in the common class, any row being compatible with an unbound value. Null
     * when there is nothing to compare.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @return SQL condition restricting the rows to the values the access has taken, or null
     */
    private SqlCode translateValues(ClassRelations relations)
    {
        List<SqlCode> comparisons = new ArrayList<>();

        for(ValueCondition condition : values)
        {
            ResourceClass resClass = condition.resourceClass();
            SqlExpressionIntercode value = condition.value();

            VariableBinding held = new VariableBinding(Map.of(resClass, condition.columns()), false);
            VariableBinding required = !isComparedInClass(relations, resClass, value) ? value.getBinding() :
                    new VariableBinding(Map.of(resClass, value.getBinding().deriveMapping(relations, resClass)), false);

            SqlCode comparison = generateJoinCondition(relations, held, required, null, null);

            if(comparison != null)
                comparisons.add(comparison);
        }

        return comparisons.isEmpty() ? null : comparisons.stream().collect(SqlCode.joining(" AND "));
    }


    /**
     * The columns that the condition restricting the rows to the values the access has taken (see
     * {@link #translateValues}) compares strictly, rejecting a row whose column is null: the columns of a value
     * compared in their class (see {@link #isComparedInClass}), except the optional ones, which the comparison takes
     * null-safely.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @return the columns the condition restricting the rows to the values compares strictly
     */
    private Set<Column> getValueComparedColumns(ClassRelations relations)
    {
        Set<Column> compared = new HashSet<>();

        for(ValueCondition condition : values)
            if(isComparedInClass(relations, condition.resourceClass(), condition.value()))
                addComparedColumns(compared, condition.resourceClass(), condition.columns());

        return compared;
    }


    /**
     * Adds the columns of the class that an equality with values of the class never null compares strictly (see
     * {@link SqlType#equal}): all but the optional ones, which are compared null-safely, and the constants.
     *
     * @param compared the columns compared strictly
     * @param resClass the class of the columns
     * @param columns the columns
     */
    private static void addComparedColumns(Set<Column> compared, ResourceClass resClass, List<Column> columns)
    {
        for(int i = 0; i < columns.size(); i++)
            if(!resClass.isOptionalColumn(i) && !(columns.get(i) instanceof ConstantColumn))
                compared.add(columns.get(i));
    }


    @Override
    protected boolean isRestricted(Variable variable, SqlExpressionIntercode value)
    {
        VariableBinding binding = internal.get(variable);

        if(binding == null || binding.canBeNull() || binding.getMappings().size() != 1)
            return false;

        Entry<ResourceClass, List<Column>> mapping = binding.getMappings().entrySet().iterator().next();

        return mapping.getValue() != null
                && containsAll(values, List.of(new ValueCondition(mapping.getKey(), mapping.getValue(), value)));
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        return translateAccess(request, null, Set.of());
    }


    /**
     * The access restricted to the rows compatible with the row of the outer table and fenced like a looked up access
     * (see {@link #translateLateral}), so that the database looks them up through an index of the table for every row.
     */
    @Override
    SqlCode translateForRow(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        SqlCode condition = getRowCondition(request.getConfiguration(), outer);

        return condition == null ? translate(request, scope) :
                translateLateral(request, condition, getRowComparedColumns(request.getConfiguration(), outer));
    }


    /**
     * The columns the access holds a variable in, with their class, when the access binds the variable always, to
     * columns of a single class, and the binding gives the variable a value that is never unbound: such a value is
     * compared with the columns by an equality, which an index of the table can serve as long as the columns are
     * compared as they are (see {@link #isLookedUpBy}). Null otherwise.
     *
     * @param value binding of the variable in the rows the access is looked up for
     * @return the columns the access holds the variable in, with their class, or null
     */
    private Entry<ResourceClass, List<Column>> getLookupColumns(VariableBinding value)
    {
        VariableBinding binding = bindings.get(value.getVariable());
        VariableBinding inner = internal.get(value.getVariable());

        if(table == null || binding == null || binding.canBeNull() || inner.getMappings().size() != 1
                || value.canBeNull())
            return null;

        Entry<ResourceClass, List<Column>> mapping = inner.getMappings().entrySet().iterator().next();

        return mapping.getValue() == null ? null : mapping;
    }


    /**
     * True if the values that the binding gives a variable can be converted to the class of the columns the access
     * holds the variable in (see {@link #getLookupColumns}), with the check of their representability (see
     * {@link VariableBinding#canDeriveMapping}), so that they are compared with the columns in that class, a value
     * outside the class being held by no row. Not every class can check that: an IRI cannot be told to belong to no
     * user IRI class (see {@link ResourceClass#canCheckRepresentability}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param value binding of the variable in the rows the access is looked up for
     * @return true if the values can be converted to the class of the columns of the access, false otherwise
     */
    private boolean isConvertedFor(ClassRelations relations, VariableBinding value)
    {
        Entry<ResourceClass, List<Column>> mapping = getLookupColumns(value);

        return mapping != null && value.canDeriveMapping(relations, mapping.getKey())
                && value.deriveMapping(relations, mapping.getKey()) != null;
    }


    /**
     * True if the values that the binding gives a variable can be compared with the columns the access holds the
     * variable in as the columns are, so that an index of the table can serve the comparison. The values are converted
     * to the class of the columns when they can be (see {@link #isConvertedFor}). Otherwise they are compared in the
     * common class of the two, as a join compares them (see
     * {@link #generateJoinCondition(ClassRelations, VariableBindings, VariableBindings, Table, Table, Map)}), which
     * leaves the columns as they are only if their class is stored like that common class (the IRIs of no user IRI
     * class are stored like any IRIs); columns that the common class stores otherwise would be converted, and the
     * database would have to read every row.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param value binding of the variable in the rows the access is looked up for
     * @return true if the values can be compared with the columns of the access as the columns are, false otherwise
     */
    boolean isLookedUpBy(ClassRelations relations, VariableBinding value)
    {
        Entry<ResourceClass, List<Column>> mapping = getLookupColumns(value);

        if(mapping == null)
            return false;

        if(isConvertedFor(relations, value))
            return true;

        ResourceClass resClass = mapping.getKey();
        List<Column> columns = mapping.getValue();

        for(Entry<ResourceClass, List<Column>> source : value.getMappings().entrySet())
        {
            if(ResourceClass.areDisjunct(relations, source.getKey(), resClass))
                continue;

            ResourceClass common = unionize(source.getKey(), resClass);

            if(source.getValue() == null || !resClass.toGeneralClass(common, columns, false).equals(columns))
                return false;
        }

        return true;
    }


    /**
     * SQL condition that a row of the access is compatible with a row of an outer table, by which the access is looked
     * up for that row: every variable both bind has the same value in them or is unbound in one (see
     * {@link #generateJoinCondition(ClassRelations, VariableBindings, VariableBindings, Table, Table, Map)}). Where the
     * value of the outer row can be converted to the class of the columns (see {@link #isConvertedFor}), it is compared
     * with them in that class, so that an index of the table serves the lookup; any other value is compared as a join
     * compares it. Null when the condition always holds.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param outer bindings of the outer rows
     * @param table alias the columns of the outer rows are addressed through
     * @param joined classes per variable in the whole join the access is a child of (see {@link #getJoinedClasses}); a
     *            variable without an entry compares all its classes
     * @return SQL condition over the columns of the table and of the outer table, or null
     */
    SqlCode getLookupCondition(ClassRelations relations, VariableBindings outer, Table table,
            Map<Variable, Set<ResourceClass>> joined)
    {
        // the values converted to the classes of the columns, addressed through the alias already, and the columns
        // they are compared with; the other variables are compared as a join compares them
        VariableBindings converted = new VariableBindings();
        VariableBindings held = new VariableBindings();
        VariableBindings others = new VariableBindings();

        for(VariableBinding value : outer.getValues())
        {
            VariableBinding inner = internal.get(value.getVariable());

            if(inner == null)
                continue;

            if(!isConvertedFor(relations, value))
            {
                others.add(value);
                continue;
            }

            ResourceClass resClass = inner.getMappings().keySet().iterator().next();

            converted.add(new VariableBinding(value.getVariable(),
                    Map.of(resClass, value.deriveMapping(relations, resClass, table)), false));
            held.add(new VariableBinding(value.getVariable(), inner.getMappings(), false));
        }

        SqlCode direct = generateJoinCondition(relations, converted, held, null, null, joined);
        SqlCode general = generateJoinCondition(relations, others, internal, table, null, joined);

        return direct == null ? general : general == null ? direct : SqlCode.of(direct, " AND ", general);
    }


    /**
     * The columns of the access that the condition looking it up for a row of an outer table (see
     * {@link #getLookupCondition}) compares strictly, rejecting a row whose column is null: the columns of the
     * variables whose values are converted to the class of the columns (see {@link #isConvertedFor}), except the
     * optional ones; the other variables are compared as a join compares them, which a null may satisfy.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param outer bindings of the outer rows
     * @return the columns of the access that the lookup condition compares strictly
     */
    Set<Column> getLookupComparedColumns(ClassRelations relations, VariableBindings outer)
    {
        Set<Column> compared = new HashSet<>();

        for(VariableBinding value : outer.getValues())
        {
            VariableBinding inner = internal.get(value.getVariable());

            if(inner != null && isConvertedFor(relations, value))
            {
                Entry<ResourceClass, List<Column>> mapping = inner.getMappings().entrySet().iterator().next();
                addComparedColumns(compared, mapping.getKey(), mapping.getValue());
            }
        }

        return compared;
    }


    /**
     * The variables by whose values in a row of the outer table the access is restricted to the rows compatible with
     * the row: those whose values are compared with the columns of the access as the columns are (see
     * {@link #isLookedUpBy}), by plain equalities, which an index of the table can serve. A row whose column of another
     * variable is null gives a solution compatible with every outer row, so such a variable restricts nothing.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param outer the rows the access is evaluated for one by one
     * @return the variables the access is restricted by
     */
    Set<Variable> getRowVariables(ClassRelations relations, JoinOptimizer.Outer outer)
    {
        Set<Variable> variables = new HashSet<>();

        for(VariableBinding value : outer.bindings().getValues())
            if(isLookedUpBy(relations, value))
                variables.add(value.getVariable());

        return variables;
    }


    /**
     * SQL condition restricting the rows of the access to those compatible with a row of the outer table: its columns
     * equal the values of the row (see {@link #getRowVariables}). Null when no variable restricts the access.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param outer the rows the access is evaluated for one by one
     * @return SQL condition over the columns of the table and of the outer table, or null
     */
    SqlCode getRowCondition(ClassRelations relations, JoinOptimizer.Outer outer)
    {
        VariableBindings values = new VariableBindings();

        for(Variable variable : getRowVariables(relations, outer))
            values.add(outer.bindings().get(variable));

        return getLookupCondition(relations, values, outer.table(), Map.of());
    }


    /**
     * The columns of the access that the condition restricting it to the rows compatible with a row of the outer table
     * (see {@link #getRowCondition}) compares strictly (see {@link #getLookupComparedColumns}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param outer the rows the access is evaluated for one by one
     * @return the columns of the access that the row condition compares strictly
     */
    Set<Column> getRowComparedColumns(ClassRelations relations, JoinOptimizer.Outer outer)
    {
        VariableBindings values = new VariableBindings();

        for(Variable variable : getRowVariables(relations, outer))
            values.add(outer.bindings().get(variable));

        return getLookupComparedColumns(relations, values);
    }


    /**
     * SQL code of the access evaluated for a row of the outer tables it is joined with laterally: the correlated
     * condition, which refers to the columns of the outer tables, restricts the rows along with the conditions of the
     * access, and the subquery is fenced by {@code OFFSET 0}, so that the planner does not flatten it into the outer
     * join but looks the rows up through an index of the table for every outer row (see {@link JoinOptimizer}).
     *
     * @param request the current request
     * @param correlatedCondition SQL condition over the columns of the table and of the outer tables
     * @param compared columns of the access that the correlated condition compares strictly, rejecting a row whose
     *            column is null (see {@link #getLookupComparedColumns}), so that their null tests are left out
     * @return SQL code of the access evaluated for a row of the outer tables
     */
    public SqlCode translateLateral(Request request, SqlCode correlatedCondition, Set<Column> compared)
    {
        return translateAccess(request, correlatedCondition, compared);
    }


    /**
     * SQL code of the access, restricted and fenced for a lateral join when the correlated condition is given (see
     * {@link #translateLateral}). It reads the table of the access, which the statement has to declare when it is a
     * virtual one.
     *
     * @param request the current request
     * @param correlatedCondition SQL condition over the columns of the table and of the outer tables, or null
     * @param correlatedColumns columns of the access that the correlated condition compares strictly
     * @return SQL code of the access
     */
    private SqlCode translateAccess(Request request, SqlCode correlatedCondition, Set<Column> correlatedColumns)
    {
        // the values of the current solution restrict the rows along with the conditions
        SqlCode valueCondition = translateValues(request.getConfiguration());

        Set<Column> compared = new HashSet<>(correlatedColumns);
        compared.addAll(getValueComparedColumns(request.getConfiguration()));
        SqlCode extraCondition = valueCondition == null ? correlatedCondition :
                correlatedCondition == null ? valueCondition : SqlCode.of(valueCondition, " AND ", correlatedCondition);

        Map<Column, Column> rev = getExpressions();

        boolean canBeLimited = reduced;

        for(VariableBinding binding : bindings.getValues())
            for(List<Column> columns : binding.getMappings().values())
                canBeLimited &= columns == null
                        || columns.stream().allMatch(column -> column instanceof ConstantColumn);


        // when the duplicates do not matter to the parent, the deduplication is left out entirely
        boolean distinct = isDeduplicating();

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        // the projected columns as they are written in the table (expressions instead of their aliases)
        Set<Column> projected = new HashSet<>();

        for(Column column : columns)
            projected.add(rev.getOrDefault(column, column));

        Set<Column> groupColumns = null;

        if(distinct
                && (!getCoveredColumns(projected).containsAll(distinctColumns) || SqlType.hasGroupingKey(projected)))
        {
            // some distinct column is not projected, or some column is grouped by an additional key as well (see
            // SqlType.getGroupingKey), so the rows have to be grouped instead
            groupColumns = new HashSet<>(projected);
            groupColumns.addAll(distinctColumns);
        }


        // a reduced access binding constants only just tests whether the table has a matching row; the test is an
        // EXISTS subquery and not a LIMIT 1, because a subquery with LIMIT is never parallel-safe for the planner and
        // one such member makes the whole UNION ALL or join around it sequential
        boolean existence = canBeLimited && table != null;

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");

        if(distinct && groupColumns == null)
            builder.append("DISTINCT ");

        if(!columns.isEmpty())
            builder.append(
                    columns.stream().map(c -> rev.containsKey(c) ? SqlCode.of(rev.get(c), " AS ", c) : c.getCode())
                            .collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        if(existence)
            builder.append(" WHERE EXISTS (SELECT 1");

        if(table != null)
        {
            builder.append(" FROM ");
            builder.append(table);
        }

        appendConditions(builder, conditions, extraCondition, compared);

        if(groupColumns != null)
        {
            builder.append(" GROUP BY ");
            builder.append(SqlType.getGroupingKeys(groupColumns.stream().sorted().toList()));
        }

        if(existence)
            builder.append(")");

        if(correlatedCondition != null)
            builder.append(" OFFSET 0");

        return builder.toCode();
    }


    /**
     * The expressions behind the exposed columns: an exposed binding stands for an SQL expression of the table by a
     * placeholder column, which the generated code replaces by the expression itself.
     *
     * @return the expression behind each exposed placeholder column
     */
    private Map<Column, Column> getExpressions()
    {
        Map<Column, Column> rev = new HashMap<>();

        for(VariableBinding binding : bindings.getValues())
        {
            VariableBinding inner = internal.get(binding.getVariable());

            for(ResourceClass resClass : binding.getClasses())
            {
                List<Column> vCols = binding.getMapping(resClass);
                List<Column> iCols = inner.getMapping(resClass);

                if(iCols != null)
                {
                    for(int i = 0; i < iCols.size(); i++)
                        if(iCols.get(i) instanceof ExpressionColumn)
                            rev.put(vCols.get(i), iCols.get(i));
                }
            }
        }

        return rev;
    }


    /**
     * Appends the WHERE clause of the conditions and of the extra condition, nothing when the conditions always hold
     * and there is no extra one. An equivalence class of columns is written as the comparisons of its least member with
     * the others (see {@link Condition#getSpanningEqualities}), which imply the rest, and the null test of a column
     * that the strict comparisons of the same disjunct or of the extra condition compare is left out, as those reject a
     * null; the database would count the fraction of nulls twice otherwise.
     *
     * @param builder the SQL code being built
     * @param conditions the conditions
     * @param extra SQL code of an extra condition in conjunction with the conditions, or null
     * @param extraCompared columns that the extra condition compares strictly, rejecting a row whose column is null
     */
    private static void appendConditions(SqlBuilder builder, Conditions conditions, SqlCode extra,
            Set<Column> extraCompared)
    {
        if(conditions.isTrue() && extra == null)
            return;

        builder.append(" WHERE ");

        List<SqlCode> disjuncts = new ArrayList<>();

        for(Condition condition : conditions.isTrue() ? List.<Condition> of() : conditions.getConditions())
        {
            List<SqlCode> parts = new ArrayList<>();

            // the columns a strict comparison of the disjunct or of the extra condition rejects the nulls of
            Set<Column> compared = new HashSet<>(extraCompared);

            for(ColumnComparison pair : condition.getSpanningEqualities())
            {
                parts.add(pair.getType().equal(pair.getLeft(), pair.getRight()));
                compared.add(pair.getLeft());
                compared.add(pair.getRight());
            }

            for(ColumnComparison pair : condition.getSpanningNotDistinct())
                parts.add(pair.getType().notDistinct(pair.getLeft(), pair.getRight()));

            for(ColumnComparison pair : condition.getAreNotEqual())
            {
                parts.add(pair.getType().notEqual(pair.getLeft(), pair.getRight()));
                compared.add(pair.getLeft());
                compared.add(pair.getRight());
            }

            for(Column column : condition.getIsNotNull())
                if(!compared.contains(column))
                    parts.add(SqlCode.of(column, " IS NOT NULL"));

            for(Column column : condition.getIsNull())
                parts.add(SqlCode.of(column, " IS NULL"));

            // a disjunct whose null tests the extra condition implies holds wherever the extra condition does
            if(parts.isEmpty())
            {
                disjuncts.clear();
                break;
            }

            disjuncts.add(parts.stream().collect(SqlCode.joining(" AND ", "(", ")")));
        }

        if(disjuncts.isEmpty())
        {
            builder.append(extra != null ? extra : SqlCode.TRUE);
            return;
        }

        if(extra != null)
            builder.append("(");

        builder.append(disjuncts.stream().collect(SqlCode.joining(" OR ")));

        if(extra != null)
        {
            builder.append(") AND ");
            builder.append(extra);
        }
    }


    /**
     * True if the union of the two accesses can be translated as a single scan of their table (see
     * {@link #translateUnion}): both read the same table without deduplication and project some column, their rows are
     * restricted to the same values (see {@link #values}), and their conditions differ only in the null test of one
     * column, which one access requires to be null and the other to be non-null, so that every row satisfying the
     * common part of the conditions belongs to exactly one of them.
     *
     * @param left the left access
     * @param right the right access
     * @return true if the union of the accesses can be translated as a single scan of their table, false otherwise
     */
    static boolean canBeUnionized(SqlTableAccess left, SqlTableAccess right)
    {
        if(left.table == null || !left.table.equals(right.table))
            return false;

        if(!left.distinctColumns.isEmpty() || !right.distinctColumns.isEmpty())
            return false;

        // the values restricting both accesses alike restrict the single scan
        if(!left.hasSameValues(right))
            return false;

        if(left.bindings.getNonConstantColumns().isEmpty() || right.bindings.getNonConstantColumns().isEmpty())
            return false;

        return getDiscriminator(left, right) != null;
    }


    /**
     * The column whose null test tells the rows of the left access from the rows of the right one: the conditions of
     * the accesses are single conjunctions differing only in the null test of the column, which one of them requires to
     * be null and the other to be non-null (see {@link Condition#resolve}); null when there is no such column.
     *
     * @param left the left access
     * @param right the right access
     * @return the column telling the rows of the accesses apart, or null
     */
    private static Column getDiscriminator(SqlTableAccess left, SqlTableAccess right)
    {
        if(left.conditions.getConditions().size() != 1 || right.conditions.getConditions().size() != 1)
            return null;

        Condition l = left.conditions.getConditions().iterator().next();
        Condition r = right.conditions.getConditions().iterator().next();

        if(l.resolve(r) == null)
            return null;

        Set<Column> columns = new HashSet<>(l.getIsNull());
        columns.addAll(r.getIsNull());
        columns.removeIf(c -> l.getIsNull().contains(c) && r.getIsNull().contains(c));

        assert columns.size() == 1;

        return columns.iterator().next();
    }


    /**
     * Translates the union of two accesses satisfying {@link #canBeUnionized} as a single scan of their table: the rows
     * are restricted by the common part of the conditions and by the values both accesses are restricted to, and a
     * column the accesses expose differently is computed by a CASE over the null test telling their rows apart, so that
     * each row yields the columns of the access it belongs to and nulls for the other one. Unlike a UNION ALL of two
     * filtered scans, the single scan can be pulled up by the database into the surrounding query, looked up by an
     * index when it is joined, and its join selectivity is estimated from the statistics of the columns.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the left access
     * @param right the right access
     * @param leftColumns the exposed column of the left access behind each column of the union (missing or a null
     *            constant when the left access does not provide the column)
     * @param rightColumns the exposed column of the right access behind each column of the union
     * @param columns the columns of the union in the order of the projection
     * @return SQL code of the union of the two accesses as a single scan of their table
     */
    static SqlCode translateUnion(ClassRelations relations, SqlTableAccess left, SqlTableAccess right,
            Map<Column, Column> leftColumns, Map<Column, Column> rightColumns, Collection<Column> columns)
    {
        Condition l = left.conditions.getConditions().iterator().next();
        Condition r = right.conditions.getConditions().iterator().next();
        Column discriminator = getDiscriminator(left, right);

        SqlCode leftTest = SqlCode.of(discriminator,
                l.getIsNull().contains(discriminator) ? " IS NULL" : " IS NOT NULL");
        SqlCode rightTest = SqlCode.of(discriminator,
                r.getIsNull().contains(discriminator) ? " IS NULL" : " IS NOT NULL");

        Map<Column, Column> leftExpressions = left.getExpressions();
        Map<Column, Column> rightExpressions = right.getExpressions();

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Column column : columns)
        {
            appendComma(builder, hasSelect);
            hasSelect = true;

            Column lc = getSource(leftExpressions, leftColumns.get(column));
            Column rc = getSource(rightExpressions, rightColumns.get(column));

            if(lc == null && rc == null)
            {
                builder.append(new NullColumn(column.getType()));
            }
            else if(lc != null && lc.equals(rc))
            {
                builder.append(lc);
            }
            else
            {
                builder.append("CASE");

                if(lc != null)
                    builder.append(" WHEN ").append(leftTest).append(" THEN ").append(lc);

                if(rc != null)
                    builder.append(" WHEN ").append(rightTest).append(" THEN ").append(rc);

                builder.append(" END");
            }

            builder.append(" AS ");
            builder.append(column);
        }

        if(!hasSelect)
            builder.append("1");

        builder.append(" FROM ");
        builder.append(left.table);

        appendConditions(builder, new Conditions(l.resolve(r)), left.translateValues(relations),
                left.getValueComparedColumns(relations));

        return builder.toCode();
    }


    /**
     * The expression of the table behind a column exposed by an access, or null when there is no column or it is a null
     * constant.
     *
     * @param expressions the expressions behind the exposed placeholder columns of the access
     * @param column the exposed column, or null
     * @return the expression of the table behind the column, or null
     */
    private static Column getSource(Map<Column, Column> expressions, Column column)
    {
        if(column == null || column instanceof NullColumn)
            return null;

        return expressions.getOrDefault(column, column);
    }


    /**
     * The accessed table; null for an access to constants only.
     *
     * @return the accessed table; null for an access to constants only
     */
    public SourceTable getTable()
    {
        return table;
    }


    /**
     * Whether the parent tolerates duplicate rows.
     *
     * @return true if the parent tolerates duplicate rows, false otherwise
     */
    protected boolean getReduced()
    {
        return reduced;
    }


    /**
     * Conditions on the rows.
     *
     * @return conditions on the rows
     */
    protected Conditions getConditions()
    {
        return conditions;
    }


    /**
     * Bindings in terms of the table's own columns (before representative columns of equalities are chosen).
     *
     * @return bindings in terms of the table's own columns (before representative columns of equalities are chosen)
     */
    protected VariableBindings getInternalVariableBindings()
    {
        return internal;
    }


    /**
     * Binding of the variable in terms of the table's own columns.
     *
     * @param var the variable
     * @return binding of the variable in terms of the table's own columns
     */
    protected VariableBinding getInternalVariableBinding(Variable var)
    {
        return internal.get(var);
    }


    /**
     * Columns over which deduplication is requested; empty when none.
     *
     * @return columns over which deduplication is requested; empty when none
     */
    protected Set<Column> getDistinctColumns()
    {
        return distinctColumns;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return false;
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        VariableBinding binding = internal.get(variable);
        List<Column> columns = binding != null ? binding.getMapping(resClass) : null;

        if(schema == null || columns == null)
            return null;

        return schema.getCollation(table, columns.get(index));
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("access");

        if(table != null)
        {
            builder.append(" ");
            builder.append(table instanceof DatabaseTable database ? database.getSchema() + "." : "");
            builder.append(table.getName());
        }

        if(!distinctColumns.isEmpty())
        {
            builder.append(" distinct");
            builder.append(distinctColumns.stream().sorted().map(c -> c.getName()).collect(joining(",", "(", ")")));
        }

        if(!conditions.isTrue())
        {
            builder.append(" where ");

            boolean hasWhere = false;

            for(Condition condition : conditions.getConditions())
            {
                if(hasWhere)
                    builder.append(" or ");

                hasWhere = true;

                if(conditions.getConditions().size() > 1)
                    builder.append("(");

                boolean hasCondition = false;

                for(ColumnComparison pair : condition.getAreEqual())
                {
                    if(hasCondition)
                        builder.append(" and ");

                    hasCondition = true;
                    builder.append(pair.getLeft().getName());
                    builder.append(" = ");
                    builder.append(pair.getRight().getName());
                }

                for(ColumnComparison pair : condition.getAreNotDistinct())
                {
                    if(hasCondition)
                        builder.append(" and ");

                    hasCondition = true;
                    builder.append(pair.getLeft().getName());
                    builder.append(" is not distinct from ");
                    builder.append(pair.getRight().getName());
                }

                for(ColumnComparison pair : condition.getAreNotEqual())
                {
                    if(hasCondition)
                        builder.append(" and ");

                    hasCondition = true;
                    builder.append(pair.getLeft().getName());
                    builder.append(" <> ");
                    builder.append(pair.getRight().getName());
                }

                for(Column column : condition.getIsNotNull())
                {
                    if(hasCondition)
                        builder.append(" and ");

                    hasCondition = true;
                    builder.append(column.getName());
                    builder.append(" is not null");
                }

                for(Column column : condition.getIsNull())
                {
                    if(hasCondition)
                        builder.append(" and ");

                    hasCondition = true;
                    builder.append(column.getName());
                    builder.append(" is null");
                }

                if(conditions.getConditions().size() > 1)
                    builder.append(")");
            }
        }


        for(ValueCondition value : values)
        {
            indentInfo(builder, indent, false);
            builder.append(value.columns().stream().map(c -> c.getName()).collect(joining(",", "(", ")")));
            builder.append(" = ");
            value.value().generateExplanation(builder, indent + "  ");
        }

        for(VariableBinding binding : internal.getValues())
        {
            indentInfo(builder, indent, false);
            builder.append(binding.getVariable());
            builder.append(" as");

            for(Entry<ResourceClass, List<Column>> e : binding.getMappings().entrySet())
            {
                if(e.getValue() != null)
                {
                    builder.append(" ");
                    builder.append(e.getKey().getResourceName());
                    builder.append(e.getValue().stream().map(c -> c.toString()).collect(joining(",", "(", ")")));
                }
            }
        }
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlTableAccess imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(reduced, imcode.reduced))
            return false;

        if(!Objects.equals(table, imcode.table))
            return false;

        if(!Objects.equals(internal, imcode.internal))
            return false;

        if(!Objects.equals(conditions, imcode.conditions))
            return false;

        if(!Objects.equals(distinctColumns, imcode.distinctColumns))
            return false;

        if(!Objects.equals(values, imcode.values))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(reduced, table, internal, conditions, distinctColumns, values);
    }
}
