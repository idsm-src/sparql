package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindingPair.ResourceClassPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Access to one table, or to constants only when the table is null: the mapped columns are exposed as variables and the
 * rows are restricted by conditions. Joins, left joins and distinct unions of accesses to the same table or along keys
 * are merged into a single access ({@link #tryReduceJoin} and friends). {@code reduced} means the parent does not care
 * about duplicates; {@code distinctColumns} asks for deduplication over those columns (see
 * {@link cz.iocb.sparql.engine.mapping.SingleTableQuadMapping}).
 */
public final class SqlTableAccess extends SqlIntercode
{
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
     * Creates the node; the exposed bindings use one representative per set of columns equated by the conditions.
     *
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @param distinctColumns columns over which deduplication is requested
     */
    protected SqlTableAccess(SourceTable table, Conditions conditions, VariableBindings internal, boolean reduced,
            Set<Column> distinctColumns)
    {
        super(getExternalVariableBindings(internal, conditions), true);

        this.table = table;
        this.conditions = conditions;
        this.internal = internal;
        this.reduced = reduced;
        this.distinctColumns = distinctColumns;
    }


    /**
     * Access exposing the given bindings, restricted by the conditions, with a deduplication request over
     * {@code distinctColumns}.
     *
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @param distinctColumns columns over which deduplication is requested
     * @return access exposing the given bindings, restricted by the conditions, with a deduplication request over
     *         {@code distinctColumns}
     */
    public static SqlIntercode create(SourceTable table, Conditions conditions, VariableBindings internal,
            boolean reduced, Set<Column> distinctColumns)
    {
        return new SqlTableAccess(table, conditions, internal, reduced, distinctColumns);
    }


    /**
     * Access exposing the given bindings, restricted by the conditions.
     *
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @param reduced whether duplicate solutions may be dropped
     * @return access exposing the given bindings, restricted by the conditions
     */
    public static SqlIntercode create(SourceTable table, Conditions conditions, VariableBindings internal,
            boolean reduced)
    {
        return create(table, conditions, internal, reduced, Set.of());
    }


    /**
     * Access exposing the given bindings, restricted by the conditions.
     *
     * @param table the table
     * @param conditions the conditions
     * @param internal bindings in terms of the table's own columns
     * @return access exposing the given bindings, restricted by the conditions
     */
    public static SqlIntercode create(SourceTable table, Conditions conditions, VariableBindings internal)
    {
        return create(table, conditions, internal, false);
    }


    /**
     * Unrestricted access exposing the given bindings.
     *
     * @param table the table
     * @param internal bindings in terms of the table's own columns
     * @return unrestricted access exposing the given bindings
     */
    public static SqlIntercode create(SourceTable table, VariableBindings internal)
    {
        return create(table, new Conditions(true), internal, false);
    }


    /**
     * Bindings for the parent: equated columns replaced by their representative, expressions by generated columns.
     *
     * @param bindings the variable bindings
     * @param conditions the conditions
     * @return bindings for the parent: equated columns replaced by their representative, expressions by generated
     *         columns
     */
    private static VariableBindings getExternalVariableBindings(VariableBindings bindings, Conditions conditions)
    {
        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        Map<Column, Column> expressions = new HashMap<>();

        VariableBindings result = new VariableBindings();

        for(VariableBinding variableBinding : bindings.getValues())
        {
            VariableBinding binding = new VariableBinding(variableBinding.getVariable(), variableBinding.canBeNull());

            for(Entry<ResourceClass, List<Column>> map : variableBinding.getMappings().entrySet())
                binding.addMapping(map.getKey(), selectColumns(representants, expressions, map.getValue()));

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
     * Replaces the columns by their representatives and expressions by generated columns (allocated on first use).
     *
     * @param set representative of each equated column
     * @param expressions generated columns of expressions, extended on demand
     * @param columns the columns
     * @return the replaced columns
     */
    private static List<Column> selectColumns(Map<Column, Column> set, Map<Column, Column> expressions,
            List<Column> columns)
    {
        if(columns == null)
            return null;

        List<Column> optimized = new ArrayList<>(columns.size());

        for(Column column : columns)
        {
            column = set.get(column) != null ? set.get(column) : column;

            if(column instanceof ExpressionColumn)
            {
                if(!expressions.containsKey(column))
                    expressions.put(column, new TableColumn("#expr-" + expressions.size()));

                column = expressions.get(column);
            }

            assert column != null;

            optimized.add(column);
        }

        return optimized;
    }


    /**
     * Returns the table columns that are bound to a constant by every disjunct of the conditions.
     *
     * @return the table columns that are bound to a constant by every disjunct of the conditions
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

        return columns;
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
     * Returns whether the deduplication requested by {@link #distinctColumns} is implied by a key of the table, in
     * which case the request can be dropped.
     *
     * @param schema the database schema
     * @return true if a key of the table covers the distinct columns, false otherwise
     */
    private boolean isDistinctImpliedByKey(DatabaseSchema schema)
    {
        return table == null || schema.getCompatibleKey(table, getCoveredColumns(distinctColumns)) != null;
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        if(table == null)
            return true;

        Set<Column> columns = getCoveredVariableColumns(selected);

        if(request.getConfiguration().getDatabaseSchema().getCompatibleKey(table, columns) != null)
            return true;

        // the access deduplicates its rows itself, unless the duplicates do not matter to its parent
        return !reduced && !distinctColumns.isEmpty() && columns.containsAll(distinctColumns);
    }


    /**
     * True if the VALUES can be merged into the access: distinct rows, every variable bound in one class also bound in
     * the access and never null on either side.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the VALUES node
     * @return true if the VALUES can be merged into the access, false otherwise
     */
    private static boolean canBeJoinedWithValues(DatabaseSchema schema, SqlTableAccess left, SqlValues right)
    {
        if(!right.isDistinct())
            return false;

        for(VariableBinding binding : right.getVariableBindings().getValues())
        {
            if(binding.canBeNull())
                return false;

            //TODO: support multiple resource class ...
            if(binding.getMappings().size() != 1)
                return false;

            VariableBinding tableBinding = left.getVariableBindings().get(binding.getVariable());

            if(tableBinding == null)
                return false;

            //TODO: support resource class generalization
            if(tableBinding.getMapping(binding.getClasses().iterator().next()) == null)
                return false;

            if(tableBinding.canBeNull() /*&& right.getSize() > 1*/)
                return false;
        }

        return true;
    }


    /**
     * Columns on which two accesses to the same table are equated through their shared variables; empty if the tables
     * differ or the variables are not joined in a single class.
     *
     * @param left the left access
     * @param right the right access
     * @return columns on which two accesses to the same table are equated through their shared variables; empty if the
     *         tables differ or the variables are not joined in a single class
     */
    static Set<Column> getJoinColumns(SqlTableAccess left, SqlTableAccess right)
    {
        // only the same tables can by merged
        if(!Objects.equals(left.table, right.table))
            return Set.of();


        Set<Column> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(left.internal, right.internal))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return Set.of();

            if(leftBinding.canBeNull() || rightBinding.canBeNull())
                if(!leftBinding.equals(rightBinding)) // TODO: take representatives into account
                    return Set.of();

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
     * True if the shared columns of two accesses to the same table contain a key of the table.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @return true if the shared columns of two accesses to the same table contain a key of the table, false otherwise
     */
    private static boolean canBeJoinedByPrimaryKey(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        Set<Column> columns = getJoinColumns(left, right);

        return schema.getCompatibleKey(left.table, columns) != null;
    }


    /**
     * Pairs of parent and child columns equated through shared variables; empty if the variables are not joined in a
     * single class.
     *
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @return pairs of parent and child columns equated through shared variables; empty if the variables are not joined
     *         in a single class
     */
    static Set<ColumnPair> getJoinColumnPairs(SqlTableAccess parent, SqlTableAccess child)
    {
        Set<ColumnPair> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(parent.internal, child.internal))
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
     * request, parent columns covered), or null.
     *
     * @param schema the database schema
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @return foreign key allowing the parent access to be merged into the child access (no expressions, no
     *         deduplication request, parent columns covered), or null
     */
    private static Set<ColumnPair> canBeJoinedByForeignKey(DatabaseSchema schema, SqlTableAccess parent,
            SqlTableAccess child)
    {
        if(schema.getForeignKeys(parent.table, child.table).isEmpty())
            return null;

        if(parent.hasExpression())
            return null;

        // the parent is dropped, which is exact only when its rows are unique with respect to the key columns
        if(!parent.distinctColumns.isEmpty())
            return null;

        Set<ColumnPair> columns = getJoinColumnPairs(parent, child);

        Set<Column> parentColumns = new HashSet<>();
        parentColumns.addAll(parent.conditions.getNonConstantColumns());
        parentColumns.addAll(parent.getInternalVariableBindings().getNonConstantColumns());

        return schema.getCompatibleForeignKey(parent.table, child.table, columns, parentColumns);
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

        return true;
    }


    /**
     * Foreign key allowing a distinct union of the parent and child accesses to be merged (each parent row has exactly
     * one child row), or null.
     *
     * @param schema the database schema
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @return foreign key allowing a distinct union of the parent and child accesses to be merged (each parent row has
     *         exactly one child row), or null
     */
    private static Set<ColumnPair> canBeDistinctUnionizedByForeignKey(DatabaseSchema schema, SqlTableAccess parent,
            SqlTableAccess child)
    {
        if(parent.table == null || child.table == null || schema.getForeignKeys(parent.table, child.table).isEmpty())
            return null;

        // because we cannot rewrite expressions
        if(child.hasExpression())
            return null;

        // the sets of variables (and their resource classes) have to be the same

        if(!parent.internal.getVariables().equals(child.internal.getVariables()))
            return null;

        if(parent.internal.getVariables().stream().anyMatch(n -> !parent.internal.get(n).getMappings().keySet()
                .equals(child.internal.get(n).getMappings().keySet())))
            return null;

        Set<ColumnPair> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(parent.internal, child.internal))
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

        return schema.isPartOfForeignKey(parent.table, child.table, columns, childColumns);
    }


    /**
     * True if a left join of two accesses to the same table on a key can be merged: at most one extra not-null
     * condition on the right side and no other right-side conditions.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @return true if a left join of two accesses to the same table on a key can be merged, false otherwise
     */
    private static boolean canBeLeftJoinedByPrimaryKey(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        if(!canBeJoinedByPrimaryKey(schema, left, right))
            return false;

        Set<Column> extraNotNulls = new HashSet<>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());

        if(extraNotNulls.size() > 1)
            return false;


        // conditions added by right table
        Conditions rightConditions = new Conditions(false);

        for(Condition cnd : right.conditions.getConditions())
        {
            Condition condition = new Condition();

            for(Column c : cnd.getIsNotNull())
                if(!extraNotNulls.contains(c))
                    condition.addIsNotNull(c);

            for(Column c : cnd.getIsNull())
                condition.addIsNull(c);

            for(ColumnComparison p : cnd.getAreEqual())
                condition.addAreEqual(p.getLeft(), p.getRight());

            for(ColumnComparison p : cnd.getAreNotDistinct())
                condition.addAreNotDistinct(p.getLeft(), p.getRight());

            for(ColumnComparison p : cnd.getAreNotEqual())
                condition.addAreNotEqual(p.getLeft(), p.getRight());

            rightConditions.add(condition);
        }

        // condition added by the join
        Condition joinCondition = new Condition();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(left.internal, right.internal))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            if(!leftBinding.canBeNull() && !rightBinding.canBeNull())
            {
                for(ResourceClassPair pairedClass : pair.getClasses())
                {
                    List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(leftCols, rightCols);
                }
            }
        }

        // check that nothing has been added
        Conditions conditions = Conditions.and(rightConditions, joinCondition);

        if(!conditions.equals(left.conditions))
            return false;

        return true;
    }


    /**
     * Merges the VALUES into the access as conditions.
     *
     * @param left the left access
     * @param right the VALUES node
     * @param restrictions what the parent needs of the variables
     * @return the merged access
     */
    private static SqlIntercode joinWithValues(SqlTableAccess left, SqlValues right, Restrictions restrictions)
    {
        Conditions conditions = Conditions.and(left.conditions, right.asConditions(left.getVariableBindings()));

        return create(left.table, conditions, left.internal.restrict(restrictions), left.reduced, left.distinctColumns);
    }


    /**
     * Returns whether the distinct access, which denotes a set of rows over the given distinct columns, is pinned by
     * the join to at most one row for each row of the other access to the same table, so that it only restricts the
     * rows of the other access and does not change their multiplicities. That requires the join columns (the columns
     * tied on both sides to the same variables, or to the same constants) to cover the distinct columns, all columns
     * the distinct access binds and all columns its conditions depend on. Otherwise the existence of a matching row is
     * not decided by the row of the other access itself: another row with the same join values may satisfy the
     * conditions while that row does not.
     *
     * @param access the other access
     * @param distinct the deduplicated access
     * @param distinctColumns columns over which deduplication is requested
     * @return true if the distinct access can be merged without changing the multiplicities, false otherwise
     */
    static boolean canBeJoinedByDistinctColumns(SqlTableAccess access, SqlTableAccess distinct,
            Set<Column> distinctColumns)
    {
        Set<Column> joinColumns = getJoinColumns(distinct, access);

        if(!joinColumns.containsAll(distinctColumns))
            return false;

        if(!joinColumns.containsAll(distinct.internal.getNonConstantColumns()))
            return false;

        if(!joinColumns.containsAll(distinct.conditions.getNonConstantColumns()))
            return false;

        return true;
    }


    /**
     * Returns whether the distinct access is deduplicated over its own distinct columns and can be merged into the
     * other access, see {@link #canBeJoinedByDistinctColumns(SqlTableAccess, SqlTableAccess, Set)}.
     *
     * @param access the other access
     * @param distinct the deduplicated access
     * @return true if the distinct access can be merged without changing the multiplicities, false otherwise
     */
    private static boolean canBeJoinedByDistinctColumns(SqlTableAccess access, SqlTableAccess distinct)
    {
        return !distinct.distinctColumns.isEmpty()
                && canBeJoinedByDistinctColumns(access, distinct, distinct.distinctColumns);
    }


    /**
     * Merges two accesses to the same table joined on a key into one access.
     *
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access
     */
    static SqlIntercode joinByPrimaryKey(SqlTableAccess left, SqlTableAccess right, Restrictions restrictions)
    {
        return join(left, right, restrictions, union(left.distinctColumns, right.distinctColumns));
    }


    /**
     * Merges the distinct access into the other access to the same table, see {@link #canBeJoinedByDistinctColumns}.
     * The result keeps the multiplicities of the other access, including its deduplication request.
     *
     * @param access the other access
     * @param distinct the deduplicated access
     * @param restrictions what the parent needs of the variables
     * @return the merged access
     */
    static SqlIntercode joinByDistinctColumns(SqlTableAccess access, SqlTableAccess distinct, Restrictions restrictions)
    {
        return join(access, distinct, restrictions, access.distinctColumns);
    }


    /**
     * Merges two accesses to the same table: their conditions are conjoined with the equalities of the shared
     * variables; no solution when contradictory.
     *
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @param distinctColumns columns over which deduplication is requested
     * @return the merged access, or no solution when the conditions contradict
     */
    private static SqlIntercode join(SqlTableAccess left, SqlTableAccess right, Restrictions restrictions,
            Set<Column> distinctColumns)
    {
        Condition joinCondition = new Condition();
        VariableBindings bindings = new VariableBindings(left.internal);

        for(Variable var : right.internal.getVariables())
            if(left.internal.get(var) == null)
                bindings.add(right.internal.get(var));

        for(VariableBindingPair pair : VariableBindingPair.getPairs(left.internal, right.internal))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            if(!leftBinding.canBeNull() && !rightBinding.canBeNull())
            {
                for(ResourceClassPair pairedClass : pair.getClasses())
                {
                    List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(leftCols, rightCols);
                }
            }
        }

        bindings = bindings.restrict(restrictions);

        Conditions conditions = Conditions.and(left.conditions, right.conditions);
        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        return new SqlTableAccess(left.table, conditions, bindings, left.reduced && right.reduced, distinctColumns);
    }


    /**
     * Merges the parent access into the child access along the foreign key, so the parent table is not accessed at all.
     *
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @param key the foreign key
     * @param restrictions what the parent needs of the variables
     * @return the merged access
     */
    static SqlIntercode joinByForeignKey(SqlTableAccess parent, SqlTableAccess child, Set<ColumnPair> key,
            Restrictions restrictions)
    {
        Map<Column, Column> map = new HashMap<>();

        for(ColumnPair pair : key)
            for(Column col : parent.conditions.getEqualTableColumns(pair.getLeft()))
                map.put(col, pair.getRight());


        Condition joinCondition = new Condition();
        VariableBindings bindings = new VariableBindings(child.internal);

        for(Variable var : parent.internal.getVariables())
            if(child.internal.get(var) == null)
                bindings.add(remap(map, parent.internal.get(var)));

        for(VariableBindingPair pair : VariableBindingPair.getPairs(child.internal, parent.internal))
        {
            VariableBinding childBinding = pair.getLeftVariableBinding();
            VariableBinding parentBinding = pair.getRightVariableBinding();

            if(!childBinding.canBeNull() && !parentBinding.canBeNull())
            {
                for(ResourceClassPair pairedClass : pair.getClasses())
                {
                    List<Column> childCols = childBinding.getMapping(pairedClass.getLeftClass());
                    List<Column> parentCols = parentBinding.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(childCols, remap(map, parentCols));
                }
            }
        }

        bindings = bindings.restrict(restrictions);

        Conditions conditions = Conditions.and(child.conditions, remap(map, parent.conditions));
        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        // each child row has exactly one parent row, so only the deduplication of the child remains
        return new SqlTableAccess(child.table, conditions, bindings, child.reduced && parent.reduced,
                child.distinctColumns);
    }


    /**
     * Merges a left join of two accesses to the same table on a key: right-side variables become nullable, an extra
     * not-null condition of the right side becomes a CASE guarding its columns.
     *
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access
     */
    private static SqlTableAccess leftJoinByPrimaryKey(SqlTableAccess left, SqlTableAccess right,
            Restrictions restrictions)
    {
        Set<Column> extraNotNulls = new HashSet<>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());
        Column extraCondition = extraNotNulls.isEmpty() ? null : extraNotNulls.iterator().next();

        Conditions conditions = new Conditions(left.conditions);
        VariableBindings bindings = new VariableBindings(left.internal);

        for(Variable var : right.internal.getVariables())
        {
            if(left.internal.get(var) == null)
            {
                VariableBinding rightBinding = right.internal.get(var);

                ResourceClass resClass = rightBinding.getClasses().iterator().next();
                List<Column> columns = rightBinding.getMapping(resClass);

                if(extraCondition != null)
                {
                    List<Column> modified = new ArrayList<>(columns.size());

                    for(Column col : columns)
                    {
                        if(col.equals(extraCondition))
                            modified.add(col);
                        else
                            modified.add(new ExpressionColumn(
                                    "CASE WHEN " + extraCondition + " IS NOT NULL THEN " + col + " END"));
                    }

                    columns = modified;
                }

                boolean canBeNull = rightBinding.canBeNull() || extraCondition != null;
                bindings.add(new VariableBinding(var, resClass, columns, canBeNull));
            }
        }

        bindings = bindings.restrict(restrictions);

        return new SqlTableAccess(left.table, conditions, bindings, left.reduced && right.reduced,
                union(left.distinctColumns, right.distinctColumns));
    }


    /**
     * Merges a distinct union of a parent and a child access along a foreign key into an access to the parent table
     * with the disjunction of the conditions.
     *
     * @param parent access to the referenced table
     * @param child access to the referencing table
     * @param key the foreign key
     * @return the merged access
     */
    private static SqlIntercode distinctUnionizeByForeignKey(SqlTableAccess parent, SqlTableAccess child,
            Set<ColumnPair> key)
    {
        Conditions conditions = null;

        if(parent.conditions.isTrue())
        {
            conditions = new Conditions(parent.conditions);
        }
        else
        {
            Map<Column, Column> map = new HashMap<>();

            for(ColumnPair pair : key)
                for(Column col : child.conditions.getEqualTableColumns(pair.getRight()))
                    map.put(col, pair.getLeft());

            conditions = Conditions.or(parent.conditions, remap(map, child.conditions));
        }

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding binding : parent.internal.getValues())
        {
            boolean canBeNull = binding.canBeNull() || child.internal.get(binding.getVariable()).canBeNull();
            bindings.add(new VariableBinding(binding.getVariable(), binding.getMappings(), canBeNull));
        }

        return new SqlTableAccess(parent.table, conditions, bindings, true, parent.distinctColumns);
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

        return new SqlTableAccess(left.table, conditions, bindings, true,
                union(left.distinctColumns, right.distinctColumns));
    }


    /**
     * Merges a VALUES node into the access as conditions when its rows are distinct and bound in a single class, or
     * returns null.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the VALUES node
     * @param mergeRestrictions what the parent needs of the variables
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceJoinWithValues(DatabaseSchema schema, SqlTableAccess left, SqlValues right,
            Restrictions mergeRestrictions)
    {
        if(SqlTableAccess.canBeJoinedWithValues(schema, left, right))
            return joinWithValues(left, right, mergeRestrictions);

        return null;
    }


    /**
     * Merges the two accesses into one when a foreign key, a shared key or declared distinct columns allow it; null
     * otherwise.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceJoin(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right,
            Restrictions restrictions)
    {
        Set<ColumnPair> dropLeft = SqlTableAccess.canBeJoinedByForeignKey(schema, left, right);

        if(dropLeft != null)
            return SqlTableAccess.joinByForeignKey(left, right, dropLeft, restrictions);


        Set<ColumnPair> dropRight = SqlTableAccess.canBeJoinedByForeignKey(schema, right, left);

        if(dropRight != null)
            return SqlTableAccess.joinByForeignKey(right, left, dropRight, restrictions);


        if(SqlTableAccess.canBeJoinedByPrimaryKey(schema, left, right))
            return SqlTableAccess.joinByPrimaryKey(left, right, restrictions);


        if(SqlTableAccess.canBeJoinedByDistinctColumns(left, right))
            return SqlTableAccess.joinByDistinctColumns(left, right, restrictions);

        if(SqlTableAccess.canBeJoinedByDistinctColumns(right, left))
            return SqlTableAccess.joinByDistinctColumns(right, left, restrictions);

        return null;
    }


    /**
     * Merges a left join of two accesses to the same table on a key into one access; null otherwise.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @param restrictions what the parent needs of the variables
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceLeftJoin(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right,
            Restrictions restrictions)
    {
        if(SqlTableAccess.canBeLeftJoinedByPrimaryKey(schema, left, right))
            return SqlTableAccess.leftJoinByPrimaryKey(left, right, restrictions);

        return null;
    }


    /**
     * Merges a distinct union of two accesses related by a foreign key into one access; null otherwise.
     *
     * @param schema the database schema
     * @param left the left access
     * @param right the right access
     * @return the merged access, or null
     */
    public static SqlIntercode tryReduceDistinctUnion(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        Set<ColumnPair> dropRight = SqlTableAccess.canBeDistinctUnionizedByForeignKey(schema, left, right);

        if(dropRight != null)
            return SqlTableAccess.distinctUnionizeByForeignKey(left, right, dropRight);

        Set<ColumnPair> dropLeft = SqlTableAccess.canBeDistinctUnionizedByForeignKey(schema, right, left);

        if(dropLeft != null)
            return SqlTableAccess.distinctUnionizeByForeignKey(right, left, dropLeft);


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
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        if(conditions.isFalse())
            return SqlNoSolution.get();

        VariableBindings optimizedBindings = internal.restrict(restrictions);

        Set<Column> optimizedDistinct = distinctColumns;

        if(!distinctColumns.isEmpty() && isDistinctImpliedByKey(request.getConfiguration().getDatabaseSchema()))
            optimizedDistinct = Set.of();

        if(this.reduced == reduced && optimizedBindings.equals(internal) && optimizedDistinct == distinctColumns)
            return this;

        return create(table, conditions, optimizedBindings, reduced, optimizedDistinct);
    }


    @Override
    public String translate(Request request)
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


        boolean canBeLimited = reduced;

        for(VariableBinding binding : bindings.getValues())
            for(List<Column> columns : binding.getMappings().values())
                canBeLimited &= columns == null
                        || columns.stream().allMatch(column -> column instanceof ConstantColumn);


        // when the duplicates do not matter to the parent, the deduplication is left out entirely
        boolean distinct = !reduced && !distinctColumns.isEmpty();

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        // the projected columns as they are written in the table (expressions instead of their aliases)
        Set<Column> projected = new HashSet<>();

        for(Column column : columns)
            projected.add(rev.getOrDefault(column, column));

        Set<Column> groupColumns = null;

        if(distinct && !getCoveredColumns(projected).containsAll(distinctColumns))
        {
            // some distinct column is not projected, so the rows have to be grouped instead
            groupColumns = new HashSet<>(projected);
            groupColumns.addAll(distinctColumns);
        }


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(distinct && groupColumns == null)
            builder.append("DISTINCT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (rev.containsKey(c) ? rev.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        if(table != null)
        {
            builder.append(" FROM ");
            builder.append(table);
        }

        if(!conditions.isTrue())
        {
            builder.append(" WHERE ");

            boolean hasWhere = false;

            for(Condition condition : conditions.getConditions())
            {
                appendOr(builder, hasWhere);
                hasWhere = true;

                builder.append("(");
                boolean hasCondition = false;

                // TODO: do not use derivable conditions

                for(ColumnComparison pair : condition.getAreEqual())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(pair.getLeft());
                    builder.append(" = ");
                    builder.append(pair.getRight());
                }

                for(ColumnComparison pair : condition.getAreNotDistinct())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(pair.getLeft());
                    builder.append(" IS NOT DISTINCT FROM ");
                    builder.append(pair.getRight());
                }

                for(ColumnComparison pair : condition.getAreNotEqual())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(pair.getLeft());
                    builder.append(" <> ");
                    builder.append(pair.getRight());
                }

                // TODO: do not use derivable conditions

                for(Column column : condition.getIsNotNull())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(column);
                    builder.append(" IS NOT NULL");
                }

                for(Column column : condition.getIsNull())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(column);
                    builder.append(" IS NULL");
                }

                builder.append(")");
            }
        }

        if(groupColumns != null)
        {
            builder.append(" GROUP BY ");
            builder.append(groupColumns.stream().sorted().map(Object::toString).collect(joining(", ")));
        }

        if(canBeLimited && table != null)
            builder.append(" LIMIT 1");

        return builder.toString();
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
    public Set<VirtualTable> getVirtualTables()
    {
        return table instanceof VirtualTable virtual ? Set.of(virtual) : Set.of();
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

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(reduced, table, internal, conditions, distinctColumns);
    }
}
