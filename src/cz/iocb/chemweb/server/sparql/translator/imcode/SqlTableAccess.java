package cz.iocb.chemweb.server.sparql.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
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
    public static final class Condition
    {
        final Set<Column> isNotNull = new HashSet<Column>();
        final Set<Column> isNull = new HashSet<Column>();
        final Map<Column, Column> areEqual = new HashMap<Column, Column>();
        final Set<ColumnPair> areNotEqual = new HashSet<ColumnPair>();

        public Condition()
        {
        }

        public Condition(Condition conditions)
        {
            if(conditions == null)
                return;

            isNotNull.addAll(conditions.isNotNull);
            isNull.addAll(conditions.isNull);
            areEqual.putAll(conditions.areEqual);
            areNotEqual.addAll(conditions.areNotEqual);
        }

        public boolean addIsNotNull(Column column)
        {
            if(isNull.contains(column))
                return false;

            if(areEqual.containsKey(column))
                return true;

            for(ColumnPair c : areNotEqual)
                if(c.getLeft().equals(column) || c.getRight().equals(column))
                    return true;

            isNotNull.add(column);

            return true;
        }

        public boolean addIsNull(Column column)
        {
            if(isNotNull.contains(column))
                return false;

            if(areEqual.containsKey(column))
                return false;

            for(ColumnPair c : areNotEqual)
                if(c.getLeft().equals(column) || c.getRight().equals(column))
                    return false;

            isNotNull.add(column);

            return true;
        }

        public boolean addAreEqual(Column col1, Column col2)
        {
            if(col1.equals(col2)) // ignore this condition
                return true;

            if(isNull.contains(col1) || isNull.contains(col2))
                return false;

            Column cnd1 = col1 instanceof ConstantColumn ? col1 : areEqual.get(col1);
            Column cnd2 = col2 instanceof ConstantColumn ? col2 : areEqual.get(col2);

            if(cnd1 instanceof ConstantColumn && cnd2 instanceof ConstantColumn)
            {
                return cnd1.equals(cnd2);
            }
            else if(cnd1 != null && cnd2 != null)
            {
                Column to = getMoreSpecific(cnd1, cnd2);
                Column from = to == cnd1 ? cnd2 : cnd2;
                areEqual.entrySet().stream().filter(e -> e.getValue() == from).forEach(e -> e.setValue(to));
            }
            else if(cnd1 == null && cnd2 == null)
            {
                Column col = col1 instanceof TableColumn ? col1 : col2;
                areEqual.put(col1, col);
                areEqual.put(col2, col);
                isNotNull.remove(col1);
                isNotNull.remove(col2);
            }
            else if(cnd1 == null && cnd2 != null)
            {
                areEqual.put(col1, cnd2);
                isNotNull.remove(col1);
            }
            else if(cnd1 != null && cnd2 == null)
            {
                areEqual.put(col2, cnd1);
                isNotNull.remove(col2);
            }

            for(ColumnPair c : areNotEqual)
                if(areEqual.containsKey(c.getLeft()) && areEqual.get(c.getLeft()) == areEqual.get(c.getRight()))
                    return false;

            return true;
        }

        private Column getMoreSpecific(Column col1, Column col2)
        {
            if(col1.getClass() == col2.getClass())
                return col1.toString().compareTo(col2.toString()) < 0 ? col1 : col2;

            if(col1 instanceof ConstantColumn)
                return col1;

            if(col2 instanceof ConstantColumn)
                return col2;

            if(col1 instanceof TableColumn)
                return col1;

            if(col2 instanceof TableColumn)
                return col2;

            throw new IllegalArgumentException();
        }

        public boolean addAreNotEqual(Column col1, Column col2)
        {
            if(col1.equals(col2))
                return false;

            areNotEqual.add(col1.toString().compareTo(col2.toString()) < 0 ? new ColumnPair(col1, col2) :
                    new ColumnPair(col2, col1));
            isNotNull.remove(col1);
            isNotNull.remove(col2);

            if(isNull.contains(col1) || isNull.contains(col2))
                return false;

            if(areEqual.containsKey(col1) && areEqual.get(col1) == areEqual.get(col2))
                return false;

            return true;
        }

        public boolean addAreEqual(List<Column> cols1, List<Column> cols2)
        {
            assert cols1.size() == cols2.size();

            for(int i = 0; i < cols1.size(); i++)
                if(!addAreEqual(cols1.get(i), cols2.get(i)))
                    return false;

            return true;
        }

        public boolean add(Condition conditions)
        {
            if(conditions == null)
                return true;

            for(Column c : conditions.isNotNull)
                if(!addIsNotNull(c))
                    return false;

            for(Column c : conditions.isNull)
                if(!addIsNull(c))
                    return false;

            for(Entry<Column, Column> e : conditions.areEqual.entrySet())
                if(!addAreEqual(e.getKey(), e.getValue()))
                    return false;

            for(ColumnPair p : conditions.areNotEqual)
                if(!addAreNotEqual(p.getLeft(), p.getRight()))
                    return false;

            return true;
        }

        public boolean add(Condition conditions, Map<Column, Column> map)
        {
            if(conditions == null)
                return true;

            for(Column c : conditions.isNotNull)
                if(!addIsNotNull(remap(map, c)))
                    return false;

            for(Column c : conditions.isNull)
                if(!addIsNull(remap(map, c)))
                    return false;

            for(Entry<Column, Column> e : conditions.areEqual.entrySet())
                if(!addAreEqual(remap(map, e.getKey()), remap(map, e.getValue())))
                    return false;

            for(ColumnPair p : conditions.areNotEqual)
                if(!addAreNotEqual(remap(map, p.getLeft()), remap(map, p.getRight())))
                    return false;

            return true;
        }

        public Set<Column> getEquals(Column col)
        {
            Set<Column> result = new HashSet<Column>();
            Column cond = areEqual.get(col);

            if(cond == null)
            {
                result.add(col);
            }
            else
            {
                for(Entry<Column, Column> e : areEqual.entrySet())
                    if(e.getValue() == cond)
                        result.add(e.getKey());
            }

            return result;
        }

        public boolean hasExpressionColumn()
        {
            if(isNotNull.stream().anyMatch(c -> c instanceof ExpressionColumn))
                return true;

            if(isNull.stream().anyMatch(c -> c instanceof ExpressionColumn))
                return true;

            if(areEqual.keySet().stream().anyMatch(c -> c instanceof ExpressionColumn))
                return true;

            if(areNotEqual.stream()
                    .anyMatch(c -> c.getLeft() instanceof ExpressionColumn || c.getRight() instanceof ExpressionColumn))
                return true;

            return false;
        }

        public boolean isEmpty()
        {
            if(!isNotNull.isEmpty())
                return false;

            if(!isNull.isEmpty())
                return false;

            if(!areEqual.isEmpty())
                return false;

            if(!areNotEqual.isEmpty())
                return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return areEqual.hashCode();
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            Condition other = (Condition) object;

            if(!isNotNull.equals(other.isNotNull))
                return false;

            if(!isNull.equals(other.isNull))
                return false;

            if(!areEqual.equals(other.areEqual))
                return false;

            if(!areNotEqual.equals(other.areNotEqual))
                return false;

            return true;
        }

        public Set<Column> getNonConstantColumns()
        {
            Set<Column> columns = new HashSet<Column>();

            columns.addAll(isNotNull);
            columns.addAll(isNull);

            columns.addAll(areEqual.keySet());
            columns.addAll(areEqual.values().stream().filter(c -> !(c instanceof ConstantColumn)).collect(toSet()));

            areNotEqual.forEach(p -> columns.add(p.getLeft()));
            areNotEqual.forEach(p -> columns.add(p.getRight()));

            return columns;
        }

        public Set<Column> getIsNotNull()
        {
            return isNotNull;
        }

        public Set<Column> getIsNull()
        {
            return isNull;
        }

        public Map<Column, Column> getAreEqual()
        {
            return areEqual;
        }

        public Set<ColumnPair> getAreNotEqual()
        {
            return areNotEqual;
        }
    }


    private final Table table;
    private final Condition conditions;
    private final Map<String, ResourceClass> resources;
    private final Map<String, List<Column>> mappings;
    private final Map<Column, Column> expressions;
    private final boolean reduced;


    public SqlTableAccess(UsedVariables variables, Table table, Condition conditions,
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


    public static SqlIntercode create(Table table, Condition extraCondition, List<MappedNode> maps)
    {
        return create(table, extraCondition, maps, false);
    }


    protected static SqlIntercode create(Table table, Condition extraCondition, List<MappedNode> maps, boolean reduced)
    {
        Condition conditions = new Condition(extraCondition);
        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();

        DatabaseSchema schema = Request.currentRequest().getConfiguration().getDatabaseSchema();

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
                    if(!conditions.addIsNotNull(column))
                        return SqlNoSolution.get();

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

                    if(!conditions.addAreEqual(columns, current))
                        return SqlNoSolution.get();
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

                if(!conditions.addAreEqual(columns, values))
                    return SqlNoSolution.get();
            }
        }

        UsedVariables variables = new UsedVariables();

        for(String name : mappings.keySet())
        {
            UsedVariable variable = new UsedVariable(name, false);
            variable.addMapping(resources.get(name),
                    selectColumns(conditions.areEqual, expressions, mappings.get(name)));
            variables.add(variable);
        }

        return new SqlTableAccess(variables, table, conditions, resources, mappings, expressions, reduced);
    }


    private static List<Column> selectColumns(Map<Column, Column> conditions, Map<Column, Column> expressions,
            List<Column> columns)
    {
        ArrayList<Column> optimized = new ArrayList<Column>(columns.size());

        for(Column column : columns)
        {
            column = conditions.get(column) != null ? conditions.get(column) : column;

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

        for(Entry<Column, Column> entry : conditions.areEqual.entrySet())
            if(entry.getValue() instanceof ConstantColumn)
                columns.add(entry.getKey());

        for(String varName : selected)
        {
            UsedVariable variable = variables.get(varName);

            if(variable != null)
            {
                for(Column column : variable.getNonConstantColumns())
                {
                    Column cnd = conditions.areEqual.get(column);

                    if(cnd == null)
                        columns.add(column);
                    else
                        conditions.areEqual.entrySet().stream().filter(e -> e.getValue() == cnd)
                                .forEach(e -> columns.add(e.getKey()));
                }
            }
        }

        return Request.currentRequest().getConfiguration().getDatabaseSchema().getCompatibleKey(table, columns) != null;
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
                    Column leftCol = leftCols.get(i);
                    Column rightCol = rightCols.get(i);

                    if(leftCol instanceof TableColumn && rightCol instanceof TableColumn)
                    {
                        Set<Column> leftColumns = left.conditions.getEquals(leftCol);
                        Set<Column> rightColumns = right.conditions.getEquals(rightCol);

                        leftColumns.retainAll(rightColumns);
                        columns.addAll(leftColumns);
                    }
                    else if(leftCol instanceof ConstantColumn && rightCol instanceof TableColumn)
                    {
                        if(leftCol.equals(left.conditions.areEqual.get(rightCol)))
                            columns.add(rightCol);
                    }
                    else if(leftCol instanceof TableColumn && rightCol instanceof ConstantColumn)
                    {
                        if(rightCol.equals(right.conditions.areEqual.get(leftCol)))
                            columns.add(leftCol);
                    }
                }
            }
        }

        for(Entry<Column, Column> e : left.conditions.areEqual.entrySet())
            if(e.getValue() instanceof ConstantColumn && e.getValue().equals(right.conditions.areEqual.get(e.getKey())))
                columns.add(e.getKey());

        return schema.getCompatibleKey(left.table, columns) != null;
    }


    public static List<ColumnPair> canBeDroped(DatabaseSchema schema, SqlTableAccess parent, SqlTableAccess child)
    {
        if(parent.table == null || child.table == null || schema.getForeignKeys(parent.table, child.table) == null)
            return null;

        if(!parent.expressions.isEmpty())
            return null;

        if(parent.conditions.hasExpressionColumn())
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
                    Column parentCol = parentCols.get(i);
                    Column childCol = childCols.get(i);

                    if(parentCol instanceof TableColumn && childCol instanceof TableColumn)
                    {
                        for(Column parentColumn : parent.conditions.getEquals(parentCol))
                            for(Column childColumn : child.conditions.getEquals(childCol))
                                columns.add(new ColumnPair(parentColumn, childColumn));
                    }
                    else if(parentCol instanceof ConstantColumn && childCol instanceof TableColumn)
                    {
                        for(Entry<Column, Column> cond : parent.conditions.areEqual.entrySet())
                            if(cond.getValue().equals(parentCol))
                                columns.add(new ColumnPair(cond.getKey(), childCol));
                    }
                    else if(parentCol instanceof TableColumn && childCol instanceof ConstantColumn)
                    {
                        for(Entry<Column, Column> cond : child.conditions.areEqual.entrySet())
                            if(cond.getValue().equals(childCol))
                                columns.add(new ColumnPair(parentCol, cond.getKey()));
                    }
                }
            }
        }

        for(Entry<Column, Column> parentEntry : parent.conditions.areEqual.entrySet())
            if(parentEntry.getValue() instanceof ConstantColumn)
                for(Entry<Column, Column> childEntry : child.conditions.areEqual.entrySet())
                    if(parentEntry.getValue().equals(childEntry.getValue()))
                        columns.add(new ColumnPair(parentEntry.getKey(), childEntry.getKey()));


        Set<Column> parentColumns = new HashSet<Column>();
        parentColumns.addAll(parent.conditions.getNonConstantColumns());
        parentColumns.addAll(parent.getVariables().getNonConstantColumns());

        return schema.getCompatibleForeignKey(parent.table, child.table, columns, parentColumns);
    }


    public static boolean canBeLeftMerged(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        if(!canBeMerged(schema, left, right))
            return false;

        Set<Column> extraNotNulls = new HashSet<Column>(right.conditions.isNotNull);
        extraNotNulls.removeAll(left.conditions.isNotNull);

        if(extraNotNulls.size() > 1)
            return false;

        Condition cndLeft = new Condition(left.conditions);
        Condition cndRight = new Condition(right.conditions);

        cndRight.isNotNull.removeAll(extraNotNulls);
        cndLeft.add(cndRight);

        // check whether nothing is added by right table
        if(!cndLeft.equals(left.conditions))
            return false;

        for(Entry<String, List<Column>> entry : left.mappings.entrySet())
        {
            List<Column> leftCols = entry.getValue();
            List<Column> rightCols = right.mappings.get(entry.getKey());

            if(rightCols != null && !cndLeft.addAreEqual(leftCols, rightCols))
                return false; //NOTE: join condition is always false
        }

        // check whether nothing is added by join condition
        if(!cndLeft.equals(left.conditions))
            return false;

        return true;
    }


    public static SqlIntercode merge(SqlTableAccess left, SqlTableAccess right, Set<String> restrictions)
    {
        Condition conditions = new Condition(left.conditions);

        if(!conditions.add(right.conditions))
            return SqlNoSolution.get();


        for(Entry<String, List<Column>> entry : left.mappings.entrySet())
        {
            String name = entry.getKey();
            List<Column> leftCols = entry.getValue();
            List<Column> rightCols = right.mappings.get(name);

            if(rightCols != null && !conditions.addAreEqual(leftCols, rightCols))
                return SqlNoSolution.get();
        }


        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();

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
                variable.addMapping(resClass, selectColumns(conditions.areEqual, expressions, columns));
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
                    variable.addMapping(resClass, selectColumns(conditions.areEqual, expressions, columns));
                    variables.add(variable);
                }
            }
        }

        return new SqlTableAccess(variables, left.table, conditions, resources, mappings, expressions,
                left.reduced && right.reduced);
    }


    public static SqlIntercode merge(SqlTableAccess parent, SqlTableAccess child, List<ColumnPair> key,
            Set<String> restrictions)
    {
        Map<Column, Column> map = new HashMap<Column, Column>();

        for(ColumnPair pair : key)
            for(Column col : parent.conditions.getEquals(pair.getLeft()))
                map.put(col, pair.getRight());


        Condition conditions = new Condition(child.conditions);

        if(!conditions.add(parent.conditions, map))
            return SqlNoSolution.get();

        for(Entry<String, List<Column>> entry : parent.mappings.entrySet())
        {
            String name = entry.getKey();
            List<Column> parentCols = entry.getValue();
            List<Column> childCols = child.mappings.get(name);

            if(childCols != null && !conditions.addAreEqual(childCols, remap(map, parentCols)))
                return SqlNoSolution.get();
        }


        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();

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
                variable.addMapping(resClass, selectColumns(conditions.areEqual, expressions, columns));
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
                    variable.addMapping(resClass, selectColumns(conditions.areEqual, expressions, columns));
                    variables.add(variable);
                }
            }
        }


        return new SqlTableAccess(variables, child.table, conditions, resources, mappings, expressions,
                parent.reduced && child.reduced);
    }


    public static SqlIntercode leftMerge(SqlTableAccess left, SqlTableAccess right, Set<String> restrictions)
    {
        Set<Column> extraNotNulls = new HashSet<Column>(right.conditions.isNotNull);
        extraNotNulls.removeAll(left.conditions.isNotNull);
        Column extraCondition = extraNotNulls.isEmpty() ? null : extraNotNulls.iterator().next();

        Condition conditions = new Condition(left.conditions);

        Map<Column, Column> expressions = new HashMap<Column, Column>();
        Map<String, ResourceClass> resources = new HashMap<String, ResourceClass>();
        Map<String, List<Column>> mappings = new HashMap<String, List<Column>>();

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
                variable.addMapping(resClass, selectColumns(conditions.areEqual, expressions, columns));
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
                    variable.addMapping(resClass, selectColumns(conditions.areEqual, expressions, columns));
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


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        UsedVariables optVariables = new UsedVariables();

        Map<Column, Column> optExpressions = new HashMap<Column, Column>();
        Map<String, List<Column>> optMappings = new HashMap<String, List<Column>>();
        Map<String, ResourceClass> optResources = new HashMap<String, ResourceClass>();

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
                optVariable.addMapping(resClass, selectColumns(conditions.areEqual, optExpressions, columns));
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

        if(!conditions.isEmpty())
        {
            builder.append(" WHERE ");
            boolean hasWhere = false;

            for(Entry<Column, Column> entry : conditions.areEqual.entrySet())
            {
                if(entry.getKey() != entry.getValue())
                {
                    appendAnd(builder, hasWhere);
                    hasWhere = true;

                    builder.append(entry.getKey());
                    builder.append(" = ");
                    builder.append(entry.getValue());
                }
            }

            for(ColumnPair pair : conditions.areNotEqual)
            {
                appendAnd(builder, hasWhere);
                hasWhere = true;

                builder.append(pair.getLeft());
                builder.append(" <> ");
                builder.append(pair.getRight());
            }

            for(Column column : conditions.isNotNull)
            {
                appendAnd(builder, hasWhere);
                hasWhere = true;

                builder.append(column);
                builder.append(" IS NOT NULL");
            }

            for(Column column : conditions.isNull)
            {
                appendAnd(builder, hasWhere);
                hasWhere = true;

                builder.append(column);
                builder.append(" IS NULL");
            }
        }

        if(canBeLimited)
            builder.append(" LIMIT 1");

        return builder.toString();
    }
}
