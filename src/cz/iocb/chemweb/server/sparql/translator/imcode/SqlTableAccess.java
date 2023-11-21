package cz.iocb.chemweb.server.sparql.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.Condition;
import cz.iocb.chemweb.server.sparql.database.Condition.ColumnComparison;
import cz.iocb.chemweb.server.sparql.database.Conditions;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.ConstantMapping;
import cz.iocb.chemweb.server.sparql.mapping.MappedNode;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlTableAccess extends SqlIntercode
{
    private final Table table;
    private final Conditions conditions;
    private final Map<String, ResourceClass> resources;
    private final Map<String, List<Column>> mappings;
    private final Map<Column, Column> expressions;
    private final boolean reduced;


    public SqlTableAccess(UsedVariables variables, Table table, Conditions conditions,
            Map<String, ResourceClass> resources, Map<String, List<Column>> mappings, Map<Column, Column> expressions,
            boolean reduced)
    {
        super(variables, true);

        this.table = table;
        this.conditions = conditions;
        this.resources = resources;
        this.mappings = mappings;
        this.expressions = expressions;
        this.reduced = reduced;
    }


    public static SqlIntercode create(Table table, Conditions extraCondition, List<MappedNode> maps)
    {
        return create(table, extraCondition, maps, false);
    }


    protected static SqlIntercode create(Table table, Conditions extraCondition, List<MappedNode> maps, boolean reduced)
    {
        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();

        DatabaseSchema schema = Request.currentRequest().getConfiguration().getDatabaseSchema();

        Condition condition = new Condition();

        for(MappedNode map : maps)
        {
            Node node = map.getNode();
            NodeMapping mapping = map.getMapping();

            if(node == null)
                continue;

            if(!(node instanceof VariableOrBlankNode) && mapping instanceof ConstantMapping)
                continue; //NOTE: already checked

            ResourceClass resourceClass = mapping.getResourceClass();
            List<Column> columns = mapping.getColumns();

            for(Column column : columns)
                if(schema.isNullableColumn(table, column))
                    condition.addIsNotNull(column);

            if(node instanceof VariableOrBlankNode)
            {
                String variableName = ((VariableOrBlankNode) node).getSqlName();

                if(mappings.get(variableName) == null)
                {
                    mappings.put(variableName, columns);
                    resources.put(variableName, resourceClass);
                }
                else if(resources.get(variableName) == resourceClass)
                {
                    List<Column> current = mappings.get(variableName);
                    condition.addAreEqual(columns, current);
                }
                else
                {
                    //NOTE: CommonIriClass cannot be used in mappings
                    assert resources.get(variableName).getGeneralClass() != resourceClass.getGeneralClass();
                    return SqlNoSolution.get();
                }
            }
            else if(mapping instanceof ParametrisedMapping)
            {
                List<Column> values = mapping.getResourceClass().toColumns(node);
                condition.addAreEqual(columns, values);
            }
        }

        Conditions conditions = Conditions.and(extraCondition, condition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        UsedVariables variables = new UsedVariables();

        for(String name : mappings.keySet())
        {
            UsedVariable variable = new UsedVariable(name, false);
            variable.addMapping(resources.get(name), selectColumns(representants, expressions, mappings.get(name)));
            variables.add(variable);
        }

        return new SqlTableAccess(variables, table, conditions, resources, mappings, expressions, reduced);
    }


    private static Map<Column, Column> selectColumnRepresentants(Conditions conditions)
    {
        Set<ColumnComparison> equals = null;

        for(Condition condition : conditions.getConditions())
        {
            if(equals == null)
                equals = new HashSet<ColumnComparison>(condition.getAreEqual());
            else
                equals.retainAll(condition.getAreEqual());
        }

        if(equals == null)
            return new HashMap<Column, Column>();


        Map<Column, Column> representants = new HashMap<Column, Column>();

        for(ColumnComparison p : equals)
        {
            Column r1 = representants.getOrDefault(p.getLeft(), p.getLeft());
            Column r2 = representants.getOrDefault(p.getRight(), p.getRight());
            Column r = r1 instanceof ConstantColumn || r2 instanceof ExpressionColumn ? r1 : r2;

            representants.replaceAll((k, v) -> v.equals(r1) || v.equals(r2) ? r : v);

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
        ArrayList<Column> optimized = new ArrayList<Column>(columns.size());

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


    public boolean isDistinct(Collection<String> selected)
    {
        if(table == null)
            return true;

        Set<Column> columns = new HashSet<Column>();

        for(ColumnComparison p : conditions.getAreEqual())
        {
            if(p.getLeft() instanceof ConstantColumn && p.getRight() instanceof TableColumn)
                columns.add(p.getRight());

            if(p.getLeft() instanceof TableColumn && p.getRight() instanceof ConstantColumn)
                columns.add(p.getLeft());
        }

        for(String varName : selected)
        {
            UsedVariable variable = variables.get(varName);

            if(variable != null)
            {
                for(Column column : variable.getNonConstantColumns())
                    for(Column col : conditions.getEqualTableColumns(column))
                        columns.add(col);
            }
        }

        return Request.currentRequest().getConfiguration().getDatabaseSchema().getCompatibleKey(table, columns) != null;
    }


    public static boolean canBeMerged(DatabaseSchema schema, SqlTableAccess left, SqlValues right)
    {
        for(UsedVariable variable : right.getVariables().getValues())
        {
            if(variable.canBeNull())
                return false;

            UsedVariable tableVariable = left.getVariables().get(variable.getName());

            if(tableVariable == null)
                return false;

            if(tableVariable.canBeNull() /*&& right.getSize() > 1*/)
                return false;
        }

        return true;
    }


    public static boolean canBeMerged(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        // only the same tables can by merged
        if(left.table == null && right.table != null || left.table != null && !left.table.equals(right.table))
            return false;


        Set<Column> columns = new HashSet<Column>();

        for(UsedPairedVariable paired : UsedPairedVariable.getPairs(left.getVariables(), right.getVariables()))
        {
            if(paired.getLeftVariable() == null || paired.getRightVariable() == null)
                continue;

            //NOTE: currently, merging is allowed only in the case that variables are not joined by different resource classes
            if(!paired.getLeftVariable().getClasses().equals(paired.getRightVariable().getClasses()))
                return false;

            //NOTE: currently, only simple join (without IS NULL conditions) can be taken into the account
            if(paired.getLeftVariable().canBeNull() || paired.getRightVariable().canBeNull())
                if(!left.mappings.get(paired.getName()).equals(right.mappings.get(paired.getName())))
                    return false;

            for(PairedClass p : paired.getClasses())
            {
                assert p.getLeftClass() == p.getRightClass();

                List<Column> leftCols = paired.getLeftVariable().getMapping(p.getLeftClass());
                List<Column> rightCols = paired.getRightVariable().getMapping(p.getRightClass());

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

        return schema.getCompatibleKey(left.table, columns) != null;
    }


    public static List<ColumnPair> canBeDroped(DatabaseSchema schema, SqlTableAccess parent, SqlTableAccess child)
    {
        if(parent.table == null || child.table == null || schema.getForeignKeys(parent.table, child.table) == null)
            return null;

        if(!parent.expressions.isEmpty())
            return null;

        // FIXME:
        if(parent.conditions.getNonConstantColumns().stream().anyMatch(c -> c instanceof ExpressionColumn))
            return null;


        Set<ColumnPair> columns = new HashSet<ColumnPair>();

        for(UsedPairedVariable paired : UsedPairedVariable.getPairs(parent.getVariables(), child.getVariables()))
        {
            if(paired.getLeftVariable() == null || paired.getRightVariable() == null)
                continue;

            //NOTE: currently, merging is allowed only in the case that variables are not joined by different resource classes
            if(!paired.getLeftVariable().getClasses().equals(paired.getRightVariable().getClasses()))
                return null;

            //NOTE: currently, only simple join (without IS NULL conditions) can be taken into the account
            if(paired.getLeftVariable().canBeNull() || paired.getRightVariable().canBeNull())
                return null;

            for(PairedClass p : paired.getClasses())
            {
                assert p.getLeftClass() == p.getRightClass();

                List<Column> parentCols = paired.getLeftVariable().getMapping(p.getLeftClass());
                List<Column> childCols = paired.getRightVariable().getMapping(p.getRightClass());

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

        Set<Column> parentColumns = new HashSet<Column>();
        parentColumns.addAll(parent.conditions.getNonConstantColumns());
        parentColumns.addAll(parent.getVariables().getNonConstantColumns());

        return schema.getCompatibleForeignKey(parent.table, child.table, columns, parentColumns);
    }


    public static boolean canBeLeftMerged(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        if(!canBeMerged(schema, left, right))
            return false;

        Set<Column> extraNotNulls = new HashSet<Column>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());

        if(extraNotNulls.size() > 1)
            return false;


        // conditions added by right table
        Conditions rightConditions = new Conditions();

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

        for(Entry<String, List<Column>> entry : left.mappings.entrySet())
        {
            List<Column> leftCols = entry.getValue();
            List<Column> rightCols = right.mappings.get(entry.getKey());

            if(rightCols != null)
                joinCondition.addAreEqual(leftCols, rightCols);
        }


        // check that nothing has been added
        Conditions conditions = Conditions.and(rightConditions, joinCondition);

        if(!conditions.equals(left.conditions))
            return false;

        return true;
    }


    public static SqlTableAccess merge(SqlTableAccess left, SqlValues right, Set<String> restrictions)
    {
        Conditions conditions = Conditions.and(left.conditions, right.asConditions(left.getVariables()));

        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();
        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        UsedVariables variables = new UsedVariables();

        for(UsedVariable leftVariable : left.variables.getValues())
        {
            if(restrictions == null || restrictions.contains(leftVariable.getName()))
            {
                String name = leftVariable.getName();

                ResourceClass resClass = left.resources.get(name);
                List<Column> columns = left.mappings.get(name);
                resources.put(name, resClass);
                mappings.put(name, columns);

                UsedVariable variable = new UsedVariable(name, leftVariable.canBeNull());
                variable.addMapping(resClass, selectColumns(representants, expressions, columns));
                variables.add(variable);
            }
        }

        return new SqlTableAccess(variables, left.table, conditions, resources, mappings, expressions, left.reduced);
    }


    public static SqlTableAccess merge(SqlTableAccess left, SqlTableAccess right, Set<String> restrictions)
    {
        Conditions conditions = Conditions.and(left.conditions, right.conditions);

        // condition added by the join
        Condition joinCondition = new Condition();

        for(Entry<String, List<Column>> entry : left.mappings.entrySet())
        {
            String name = entry.getKey();
            List<Column> leftCols = entry.getValue();
            List<Column> rightCols = right.mappings.get(name);

            if(rightCols != null)
                joinCondition.addAreEqual(leftCols, rightCols);
        }

        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return null;


        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();
        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        UsedVariables variables = new UsedVariables();

        for(UsedVariable leftVariable : left.variables.getValues())
        {
            if(restrictions == null || restrictions.contains(leftVariable.getName()))
            {
                String name = leftVariable.getName();

                ResourceClass resClass = left.resources.get(name);
                List<Column> columns = left.mappings.get(name);
                resources.put(name, resClass);
                mappings.put(name, columns);

                UsedVariable variable = new UsedVariable(name, leftVariable.canBeNull());
                variable.addMapping(resClass, selectColumns(representants, expressions, columns));
                variables.add(variable);
            }
        }

        for(UsedVariable rightVariable : right.variables.getValues())
        {
            if(restrictions == null || restrictions.contains(rightVariable.getName()))
            {
                String name = rightVariable.getName();

                if(variables.get(name) == null)
                {
                    ResourceClass resClass = right.resources.get(name);
                    List<Column> columns = right.mappings.get(name);
                    resources.put(name, resClass);
                    mappings.put(name, columns);

                    UsedVariable variable = new UsedVariable(name, rightVariable.canBeNull());
                    variable.addMapping(resClass, selectColumns(representants, expressions, columns));
                    variables.add(variable);
                }
            }
        }

        return new SqlTableAccess(variables, left.table, conditions, resources, mappings, expressions,
                left.reduced && right.reduced);
    }


    public static SqlTableAccess merge(SqlTableAccess parent, SqlTableAccess child, List<ColumnPair> key,
            Set<String> restrictions)
    {
        Map<Column, Column> map = new HashMap<Column, Column>();

        for(ColumnPair pair : key)
            for(Column col : parent.conditions.getEqualTableColumns(pair.getLeft()))
                map.put(col, pair.getRight());

        Conditions conditions = Conditions.and(child.conditions, remap(map, parent.conditions));

        // condition added by the join
        Condition joinCondition = new Condition();

        for(Entry<String, List<Column>> entry : parent.mappings.entrySet())
        {
            String name = entry.getKey();
            List<Column> parentCols = entry.getValue();
            List<Column> childCols = child.mappings.get(name);

            if(childCols != null)
                joinCondition.addAreEqual(childCols, remap(map, parentCols));
        }

        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return null;


        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();
        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        UsedVariables variables = new UsedVariables();

        for(UsedVariable childVariable : child.variables.getValues())
        {
            if(restrictions == null || restrictions.contains(childVariable.getName()))
            {
                String name = childVariable.getName();

                ResourceClass resClass = child.resources.get(name);
                List<Column> columns = child.mappings.get(name);
                resources.put(name, resClass);
                mappings.put(name, columns);

                UsedVariable variable = new UsedVariable(name, childVariable.canBeNull());
                variable.addMapping(resClass, selectColumns(representants, expressions, columns));
                variables.add(variable);
            }
        }

        for(UsedVariable parentVariable : parent.variables.getValues())
        {
            if(restrictions == null || restrictions.contains(parentVariable.getName()))
            {
                String name = parentVariable.getName();

                if(variables.get(name) == null)
                {
                    ResourceClass resClass = parent.resources.get(name);
                    List<Column> columns = remap(map, parent.mappings.get(name));
                    resources.put(name, resClass);
                    mappings.put(name, columns);

                    UsedVariable variable = new UsedVariable(name, parentVariable.canBeNull());
                    variable.addMapping(resClass, selectColumns(representants, expressions, columns));
                    variables.add(variable);
                }
            }
        }


        return new SqlTableAccess(variables, child.table, conditions, resources, mappings, expressions,
                parent.reduced && child.reduced);
    }


    public static SqlTableAccess leftMerge(SqlTableAccess left, SqlTableAccess right, Set<String> restrictions)
    {
        Set<Column> extraNotNulls = new HashSet<Column>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());
        Column extraCondition = extraNotNulls.isEmpty() ? null : extraNotNulls.iterator().next();

        Conditions conditions = new Conditions(left.conditions);

        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();
        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        UsedVariables variables = new UsedVariables();

        for(UsedVariable leftVariable : left.variables.getValues())
        {
            if(restrictions == null || restrictions.contains(leftVariable.getName()))
            {
                String name = leftVariable.getName();

                ResourceClass resClass = left.resources.get(name);
                List<Column> columns = left.mappings.get(name);
                resources.put(name, resClass);
                mappings.put(name, columns);

                UsedVariable variable = new UsedVariable(name, leftVariable.canBeNull());
                variable.addMapping(resClass, selectColumns(representants, expressions, columns));
                variables.add(variable);
            }
        }

        for(UsedVariable rightVariable : right.variables.getValues())
        {
            if(restrictions == null || restrictions.contains(rightVariable.getName()))
            {
                String name = rightVariable.getName();

                if(variables.get(name) == null)
                {
                    ResourceClass resClass = right.resources.get(name);
                    List<Column> columns = right.mappings.get(name);

                    if(extraCondition != null)
                    {
                        List<Column> modified = new ArrayList<Column>(columns.size());

                        for(Column col : columns)
                        {
                            if(col == extraCondition)
                                modified.add(col);
                            else
                                modified.add(new ExpressionColumn(
                                        "CASE WHEN " + extraCondition + " IS NOT NULL THEN " + col + " END"));
                        }

                        modified = columns;
                    }

                    resources.put(name, resClass);
                    mappings.put(name, columns);

                    UsedVariable variable = new UsedVariable(name, rightVariable.canBeNull() || extraCondition != null);
                    variable.addMapping(resClass, selectColumns(representants, expressions, columns));
                    variables.add(variable);
                }
            }
        }

        return new SqlTableAccess(variables, left.table, conditions, resources, mappings, expressions,
                left.reduced && right.reduced);
    }


    private static List<Column> remap(Map<Column, Column> map, List<Column> columns)
    {
        List<Column> result = new ArrayList<Column>();

        for(Column column : columns)
            result.add(remap(map, column));

        return result;
    }


    private static Column remap(Map<Column, Column> map, Column column)
    {
        if(column == null)
            return null;

        if(column instanceof ConstantColumn)
            return column;

        if(map.get(column) == null)
            throw new RuntimeException("not found: " + column.getName());

        return map.get(column);
    }


    private static Condition remap(Map<Column, Column> map, Condition conditions)
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


    private static Conditions remap(Map<Column, Column> map, Conditions conditions)
    {
        Conditions result = new Conditions();

        for(Condition condition : conditions.getConditions())
            result.add(remap(map, condition));

        return result;
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        UsedVariables optVariables = new UsedVariables();

        Map<Column, Column> optExpressions = new HashMap<Column, Column>();
        Map<String, List<Column>> optMappings = new HashMap<String, List<Column>>();
        Map<String, ResourceClass> optResources = new HashMap<String, ResourceClass>();
        Map<Column, Column> representants = selectColumnRepresentants(conditions);

        for(UsedVariable variable : variables.getValues())
        {
            String name = variable.getName();

            if(restrictions.contains(name))
            {
                ResourceClass resClass = resources.get(name);
                List<Column> columns = mappings.get(name);

                optResources.put(name, resClass);
                optMappings.put(name, columns);

                UsedVariable optVariable = new UsedVariable(name, variable.canBeNull());
                optVariable.addMapping(resClass, selectColumns(representants, optExpressions, columns));
                optVariables.add(optVariable);
            }
        }

        return new SqlTableAccess(optVariables, table, conditions, optResources, optMappings, optExpressions, reduced);
    }


    @Override
    public String translate()
    {
        boolean canBeLimited = reduced;

        for(UsedVariable var : variables.getValues())
            for(List<Column> columns : var.getMappings().values())
                canBeLimited &= columns.stream().allMatch(column -> column instanceof ConstantColumn);


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        Map<Column, Column> rev = new HashMap<Column, Column>();
        expressions.forEach((k, v) -> rev.put(v, k));

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

        if(canBeLimited)
            builder.append(" LIMIT 1");

        return builder.toString();
    }


    protected Table getTable()
    {
        return table;
    }


    protected Conditions getConditions()
    {
        return conditions;
    }
}
