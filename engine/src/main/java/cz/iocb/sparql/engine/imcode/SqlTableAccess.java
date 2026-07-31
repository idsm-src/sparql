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
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindingPair.ResourceClassPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlTableAccess extends SqlIntercode
{
    private final Table table;
    private final Conditions conditions;
    private final VariableBindings internal;
    private final boolean reduced;


    protected SqlTableAccess(Table table, Conditions conditions, VariableBindings internal, boolean reduced)
    {
        super(getExternalVariableBindings(internal, conditions), true);

        this.table = table;
        this.conditions = conditions;
        this.internal = internal;
        this.reduced = reduced;
    }


    public static SqlIntercode create(Table table, Conditions conditions, VariableBindings internal, boolean reduced)
    {
        return new SqlTableAccess(table, conditions, internal, reduced);
    }


    public static SqlIntercode create(Table table, Conditions conditions, VariableBindings internal)
    {
        return create(table, conditions, internal, false);
    }


    public static SqlIntercode create(Table table, VariableBindings internal)
    {
        return create(table, new Conditions(true), internal, false);
    }


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


    private static Map<Column, Column> selectColumnRepresentants(Conditions conditions)
    {
        Set<ColumnComparison> equals = null;

        for(Condition condition : conditions.getConditions())
        {
            if(equals == null)
                equals = new HashSet<>(condition.getAreEqual());
            else
                equals.retainAll(condition.getAreEqual());
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


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        if(table == null)
            return true;

        Set<Column> columns = new HashSet<>();

        for(ColumnComparison p : conditions.getAreEqual())
        {
            if(p.getLeft() instanceof ConstantColumn && p.getRight() instanceof TableColumn)
                columns.add(p.getRight());

            if(p.getLeft() instanceof TableColumn && p.getRight() instanceof ConstantColumn)
                columns.add(p.getLeft());
        }

        for(Variable var : selected)
        {
            VariableBinding binding = bindings.get(var);

            if(binding != null)
            {
                for(Column column : binding.getNonConstantColumns())
                    for(Column col : conditions.getEqualTableColumns(column))
                        columns.add(col);
            }
        }

        return request.getConfiguration().getDatabaseSchema().getCompatibleKey(table, columns) != null;
    }


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


    private static boolean canBeJoinedByPrimaryKey(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        Set<Column> columns = getJoinColumns(left, right);

        return schema.getCompatibleKey(left.table, columns) != null;
    }


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


    private static Set<ColumnPair> canBeJoinedByForeignKey(DatabaseSchema schema, SqlTableAccess parent,
            SqlTableAccess child)
    {
        if(schema.getForeignKeys(parent.table, child.table).isEmpty())
            return null;

        if(parent.hasExpression())
            return null;

        Set<ColumnPair> columns = getJoinColumnPairs(parent, child);

        Set<Column> parentColumns = new HashSet<>();
        parentColumns.addAll(parent.conditions.getNonConstantColumns());
        parentColumns.addAll(parent.getInternalVariableBindings().getNonConstantColumns());

        return schema.getCompatibleForeignKey(parent.table, child.table, columns, parentColumns);
    }


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


    private static SqlIntercode joinWithValues(SqlTableAccess left, SqlValues right, Restrictions restrictions)
    {
        Conditions conditions = Conditions.and(left.conditions, right.asConditions(left.getVariableBindings()));

        return create(left.table, conditions, left.internal.restrict(restrictions), left.reduced);
    }


    static SqlIntercode joinByPrimaryKey(SqlTableAccess left, SqlTableAccess right, Restrictions restrictions)
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

        return new SqlTableAccess(left.table, conditions, bindings, left.reduced && right.reduced);
    }


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

        return new SqlTableAccess(child.table, conditions, bindings, child.reduced && parent.reduced);
    }


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

                    modified = columns;
                }

                boolean canBeNull = rightBinding.canBeNull() || extraCondition != null;
                bindings.add(new VariableBinding(var, resClass, columns, canBeNull));
            }
        }

        bindings = bindings.restrict(restrictions);

        return new SqlTableAccess(left.table, conditions, bindings, left.reduced && right.reduced);
    }


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

        return new SqlTableAccess(parent.table, conditions, bindings, true);
    }


    private static SqlTableAccess distinctUnionizeByPrimaryKey(SqlTableAccess left, SqlTableAccess right)
    {
        Conditions conditions = Conditions.or(left.conditions, right.conditions);

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding binding : left.internal.getValues())
        {
            boolean canBeNull = binding.canBeNull() || right.internal.get(binding.getVariable()).canBeNull();
            bindings.add(new VariableBinding(binding.getVariable(), binding.getMappings(), canBeNull));
        }

        return new SqlTableAccess(left.table, conditions, bindings, true);
    }


    public static SqlIntercode tryReduceJoinWithValues(DatabaseSchema schema, SqlTableAccess left, SqlValues right,
            Restrictions mergeRestrictions)
    {
        if(SqlTableAccess.canBeJoinedWithValues(schema, left, right))
            return joinWithValues(left, right, mergeRestrictions);

        return null;
    }


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

        return null;
    }


    public static SqlIntercode tryReduceLeftJoin(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right,
            Restrictions restrictions)
    {
        if(SqlTableAccess.canBeLeftJoinedByPrimaryKey(schema, left, right))
            return SqlTableAccess.leftJoinByPrimaryKey(left, right, restrictions);

        return null;
    }


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


    protected static VariableBindings remap(Map<Column, Column> map, VariableBindings bindings)
    {
        VariableBindings result = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
            result.add(remap(map, binding));

        return result;
    }


    protected static VariableBinding remap(Map<Column, Column> map, VariableBinding binding)
    {
        VariableBinding result = new VariableBinding(binding.getVariable(), binding.canBeNull());

        for(Map.Entry<ResourceClass, List<Column>> mapping : binding.getMappings().entrySet())
            result.addMapping(mapping.getKey(), remap(map, mapping.getValue()));

        return result;
    }


    protected static List<Column> remap(Map<Column, Column> map, List<Column> columns)
    {
        if(columns == null)
            return null;

        List<Column> result = new ArrayList<>();

        for(Column column : columns)
            result.add(remap(map, column));

        return result;
    }


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


    protected static Condition remap(Map<Column, Column> map, Condition conditions)
    {
        Condition result = new Condition();

        for(Column c : conditions.getIsNotNull())
            result.addIsNotNull(remap(map, c));

        for(Column c : conditions.getIsNull())
            result.addIsNull(remap(map, c));

        for(ColumnComparison e : conditions.getAreEqual())
            result.addAreEqual(remap(map, e.getLeft()), remap(map, e.getRight()));

        for(ColumnComparison p : conditions.getAreNotEqual())
            result.addAreNotEqual(remap(map, p.getLeft()), remap(map, p.getRight()));

        return result;
    }


    protected static Conditions remap(Map<Column, Column> map, Conditions conditions)
    {
        Conditions result = new Conditions(false);

        for(Condition condition : conditions.getConditions())
            result.add(remap(map, condition));

        return result;
    }


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

        if(this.reduced == reduced && optimizedBindings.equals(internal))
            return this;

        return create(table, conditions, optimizedBindings, reduced);
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


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

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

        if(canBeLimited && table != null)
            builder.append(" LIMIT 1");

        return builder.toString();
    }


    public Table getTable()
    {
        return table;
    }


    protected boolean getReduced()
    {
        return reduced;
    }


    protected Conditions getConditions()
    {
        return conditions;
    }


    protected VariableBindings getInternalVariableBindings()
    {
        return internal;
    }


    protected VariableBinding getInternalVariableBinding(Variable var)
    {
        return internal.get(var);
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return false;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("access");

        if(table != null)
        {
            builder.append(" ");
            builder.append(table.getSchema());
            builder.append(".");
            builder.append(table.getName());
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
                    builder.append(e.getKey().getName());
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

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(reduced, table, internal, conditions);
    }
}
