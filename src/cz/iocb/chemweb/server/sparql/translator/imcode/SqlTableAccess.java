package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlTableAccess extends SqlIntercode
{
    /**
     * Name of the table for which the access is generated
     */
    private final String table;

    /**
     * Extra sql condition used in the generated where clause
     */
    private final String condition;

    /**
     * Map variable names to lists of their node mappings
     */
    private final LinkedHashMap<String, ArrayList<NodeMapping>> mappings = new LinkedHashMap<String, ArrayList<NodeMapping>>();

    /**
     * List of mappings containing columns that have to be checked for not-null property
     */
    private final ArrayList<ParametrisedLiteralMapping> notNullConditions = new ArrayList<ParametrisedLiteralMapping>();

    /**
     * List of pairs of non-variable nodes and their mappings that have to be checked for values equality
     */
    private final ArrayList<Pair<Node, ParametrisedMapping>> valueConditions = new ArrayList<Pair<Node, ParametrisedMapping>>();


    public SqlTableAccess(String table, String condition)
    {
        super(new UsedVariables());
        this.table = table;
        this.condition = condition;
    }


    public void addVariableClass(String variable, ResourceClass resourceClass)
    {
        UsedVariable other = variables.get(variable);

        if(other != null && other.containsClass(resourceClass))
            return;

        if(other != null)
            other.addClass(resourceClass);
        else
            variables.add(new UsedVariable(variable, resourceClass, false));
    }


    public void addMapping(String variable, NodeMapping mapping)
    {
        ArrayList<NodeMapping> list = mappings.get(variable);

        if(list == null)
        {
            list = new ArrayList<NodeMapping>();
            mappings.put(variable, list);
        }

        if(!list.contains(mapping))
            list.add(mapping);
    }


    public void addMappings(LinkedHashMap<String, ArrayList<NodeMapping>> values)
    {
        for(Entry<String, ArrayList<NodeMapping>> entry : values.entrySet())
        {
            ArrayList<NodeMapping> list = mappings.get(entry.getKey());

            if(list == null)
            {
                list = new ArrayList<NodeMapping>();
                mappings.put(entry.getKey(), list);
            }

            for(NodeMapping nodeMapping : entry.getValue())
                if(!list.contains(nodeMapping))
                    list.add(nodeMapping);
        }
    }


    public void addNotNullCondition(ParametrisedLiteralMapping mapping)
    {
        if(!notNullConditions.contains(mapping))
            notNullConditions.add(mapping);
    }


    public void addNotNullConditions(List<ParametrisedLiteralMapping> mappings)
    {
        for(ParametrisedLiteralMapping mapping : mappings)
            if(!notNullConditions.contains(mapping))
                notNullConditions.add(mapping);
    }


    public void addValueCondition(Node node, ParametrisedMapping mapping)
    {
        Pair<Node, ParametrisedMapping> cond = new Pair<Node, ParametrisedMapping>(node, mapping);

        if(!valueConditions.contains(cond))
            valueConditions.add(cond);
    }


    public void addValueConditions(List<Pair<Node, ParametrisedMapping>> conditions)
    {
        for(Pair<Node, ParametrisedMapping> cond : conditions)
            if(!valueConditions.contains(cond))
                valueConditions.add(cond);
    }


    public static boolean canBeMerged(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        // at this time, we allow merging only in the case that variables share the same classes
        for(UsedPairedVariable pairedVariable : UsedPairedVariable.getPairs(left.getVariables(), right.getVariables()))
            for(PairedClass p : pairedVariable.getClasses())
                if(p.getLeftClass() != null && p.getRightClass() != null && p.getLeftClass() != p.getRightClass())
                    return false;


        if(!left.table.equals(right.table))
            return false;


        LinkedList<String> columns = new LinkedList<String>();

        for(Entry<String, ArrayList<NodeMapping>> leftEntry : left.mappings.entrySet())
        {
            ArrayList<NodeMapping> leftMappings = leftEntry.getValue();
            ArrayList<NodeMapping> rightMappings = right.mappings.get(leftEntry.getKey());

            if(rightMappings == null)
                continue;


            for(NodeMapping leftMap : leftMappings)
            {
                for(NodeMapping rightMap : rightMappings)
                {
                    assert leftMap.getResourceClass() == rightMap.getResourceClass();

                    if(leftMap instanceof ParametrisedIriMapping && rightMap instanceof ParametrisedIriMapping
                            || leftMap instanceof ParametrisedLiteralMapping
                                    && rightMap instanceof ParametrisedLiteralMapping)
                    {
                        for(int i = 0; i < leftMap.getResourceClass().getPartsCount(); i++)
                        {
                            String leftColumn = leftMap.getSqlValueAccess(i);
                            String rightColumn = rightMap.getSqlValueAccess(i);

                            if(leftColumn.equals(rightColumn))
                                columns.add(leftColumn);
                        }
                    }
                }
            }
        }


        for(Pair<Node, ParametrisedMapping> leftEntry : left.valueConditions)
        {
            for(Pair<Node, ParametrisedMapping> rightEntry : right.valueConditions)
            {
                Node leftNode = leftEntry.getLeft();
                Node rightNode = rightEntry.getLeft();

                ParametrisedMapping leftMap = leftEntry.getRight();
                ParametrisedMapping rightMap = rightEntry.getRight();


                if(!leftNode.equals(rightNode))
                    continue;

                if(leftMap.getResourceClass() != rightMap.getResourceClass())
                    continue;

                for(int i = 0; i < leftMap.getResourceClass().getPartsCount(); i++)
                {
                    String leftColumn = leftMap.getSqlValueAccess(i);
                    String rightColumn = rightMap.getSqlValueAccess(i);

                    if(leftColumn.equals(rightColumn))
                        columns.add(leftColumn);
                }
            }
        }


        return schema.getCompatibleKey(left.table, columns) != null;
    }


    public static boolean canBeLeftMerged(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        if(!canBeMerged(schema, left, right))
            return false;

        if(right.condition != null && !right.condition.equals(left.condition))
            return false;

        // Only one not null condition may be extra in the right table. For more extra conditions,
        // we cannot be sure that all relevant columns are null for the same rows. Thus, the following
        // example cannot be simply optimized as self left join:
        // { ?S <ex:pred0> ?V } optional { ?S <ex:pred1> ?Literal1. ?S <ex:pred2> ?Literal2 }
        ParametrisedLiteralMapping extraCondition = null;

        for(ParametrisedLiteralMapping condition : right.notNullConditions)
        {
            if(!left.notNullConditions.contains(condition))
            {
                if(extraCondition != null)
                    return false;
                else
                    extraCondition = condition;
            }
        }

        for(Pair<Node, ParametrisedMapping> condition : right.valueConditions)
            if(!left.valueConditions.contains(condition))
                return false;

        for(Entry<String, ArrayList<NodeMapping>> entry : right.mappings.entrySet())
        {
            ArrayList<NodeMapping> rightList = entry.getValue();
            ArrayList<NodeMapping> leftList = left.mappings.get(entry.getKey());

            if(leftList == null)
            {
                // only variable using extraCondition can be included on the right side
                // if it is not included on the left side

                if(rightList.size() > 1 || rightList.get(0) != extraCondition)
                    return false;
            }
            else
            {
                for(NodeMapping nodeMapping : rightList)
                    if(!leftList.contains(nodeMapping))
                        return false;
            }
        }

        return true;
    }


    public static List<KeyPair> canBeDroped(DatabaseSchema schema, SqlTableAccess parent, SqlTableAccess foreign)
    {
        // at this time, we allow dropping only in the case that variables share the same classes
        for(UsedPairedVariable paired : UsedPairedVariable.getPairs(parent.getVariables(), foreign.getVariables()))
            for(PairedClass p : paired.getClasses())
                if(p.getLeftClass() != null && p.getRightClass() != null && p.getLeftClass() != p.getRightClass())
                    return null;


        List<List<KeyPair>> keys = schema.getForeignKeys(parent.table, foreign.table);

        if(keys == null)
            return null;

        //TODO: can be parent.condition transformed?
        if(parent.condition != null)
            return null;


        ArrayList<KeyPair> columns = new ArrayList<KeyPair>();
        LinkedHashSet<String> parentColumns = new LinkedHashSet<String>();

        for(Entry<String, ArrayList<NodeMapping>> parentEntry : parent.mappings.entrySet())
        {
            ArrayList<NodeMapping> parentMappings = parentEntry.getValue();

            for(NodeMapping parentMap : parentMappings)
                if(parentMap instanceof ParametrisedMapping)
                    for(int i = 0; i < parentMap.getResourceClass().getPartsCount(); i++)
                        parentColumns.add(parentMap.getSqlValueAccess(i));


            ArrayList<NodeMapping> foreignMappings = foreign.mappings.get(parentEntry.getKey());

            if(foreignMappings == null)
                continue;


            for(NodeMapping parentMap : parentMappings)
            {
                for(NodeMapping foreignMap : foreignMappings)
                {
                    assert parentMap.getResourceClass() == foreignMap.getResourceClass();

                    if(parentMap instanceof ParametrisedIriMapping && foreignMap instanceof ParametrisedIriMapping
                            || parentMap instanceof ParametrisedLiteralMapping
                                    && foreignMap instanceof ParametrisedLiteralMapping)
                    {
                        for(int i = 0; i < parentMap.getResourceClass().getPartsCount(); i++)
                        {
                            String parentColumn = parentMap.getSqlValueAccess(i);
                            String foreignColumn = foreignMap.getSqlValueAccess(i);

                            columns.add(new KeyPair(parentColumn, foreignColumn));
                        }
                    }
                }
            }
        }


        for(Pair<Node, ParametrisedMapping> parentEntry : parent.valueConditions)
        {
            for(Pair<Node, ParametrisedMapping> foreignEntry : foreign.valueConditions)
            {
                Node parentNode = parentEntry.getLeft();
                Node foreignNode = foreignEntry.getLeft();

                ParametrisedMapping parentMap = parentEntry.getRight();
                ParametrisedMapping foreignMap = foreignEntry.getRight();


                if(!parentNode.equals(foreignNode))
                    continue;

                if(parentMap.getResourceClass() != foreignMap.getResourceClass())
                    continue;

                for(int i = 0; i < parentMap.getResourceClass().getPartsCount(); i++)
                {
                    String parentColumn = parentMap.getSqlValueAccess(i);
                    String foreignColumn = foreignMap.getSqlValueAccess(i);

                    columns.add(new KeyPair(parentColumn, foreignColumn));
                }
            }
        }


        return schema.getCompatibleForeignKey(parent.table, foreign.table, columns, parentColumns);
    }


    public static SqlTableAccess merge(SqlTableAccess left, SqlTableAccess right)
    {
        String condition = null;

        if(left.condition != null && right.condition != null && !left.condition.equals(right.condition))
            condition = "(" + left.condition + ") AND (" + right.condition + ")";
        else if(left.condition != null)
            condition = left.condition;
        else
            condition = right.condition;


        SqlTableAccess merged = new SqlTableAccess(left.table, condition);

        for(UsedVariable var : left.getVariables().getValues())
        {
            UsedVariable other = right.getVariables().get(var.getName());

            if(other == null)
            {
                merged.getVariables().add(var);
            }
            else
            {
                assert var.getClasses().size() == 1;
                assert other.getClasses().size() == 1;
                assert var.getClasses().get(0) == other.getClasses().get(0);

                UsedVariable newVar = new UsedVariable(var.getName(), var.getClasses().get(0),
                        var.canBeNull() && other.canBeNull());
                merged.getVariables().add(newVar);
            }
        }

        for(UsedVariable var : right.getVariables().getValues())
        {
            if(left.getVariables().get(var.getName()) == null)
                merged.getVariables().add(var);
        }


        merged.mappings.putAll(left.mappings);
        merged.valueConditions.addAll(left.valueConditions);
        merged.notNullConditions.addAll(left.notNullConditions);

        merged.addMappings(right.mappings);
        merged.addValueConditions(right.valueConditions);
        merged.addNotNullConditions(right.notNullConditions);

        return merged;
    }


    public static SqlTableAccess leftMerge(SqlTableAccess left, SqlTableAccess right)
    {
        SqlTableAccess merged = new SqlTableAccess(left.table, left.condition);

        for(UsedVariable var : left.getVariables().getValues())
        {
            UsedVariable other = right.getVariables().get(var.getName());

            if(other == null)
            {
                merged.getVariables().add(var);
            }
            else
            {
                assert var.getClasses().size() == 1;
                assert other.getClasses().size() == 1;
                assert var.getClasses().get(0) == other.getClasses().get(0);

                UsedVariable newVar = new UsedVariable(var.getName(), var.getClasses().get(0),
                        var.canBeNull() && other.canBeNull());
                merged.getVariables().add(newVar);
            }
        }

        for(UsedVariable var : right.getVariables().getValues())
        {
            if(left.getVariables().get(var.getName()) == null)
            {
                assert var.getClasses().size() == 1;
                merged.getVariables().add(new UsedVariable(var.getName(), var.getClasses().get(0), true));
            }
        }


        merged.mappings.putAll(left.mappings);
        merged.valueConditions.addAll(left.valueConditions);
        merged.notNullConditions.addAll(left.notNullConditions);

        merged.addMappings(right.mappings);

        return merged;
    }


    public static SqlTableAccess merge(SqlTableAccess parent, SqlTableAccess foreign, List<KeyPair> columnMap)
    {
        String condition = null;

        if(foreign.condition != null && parent.condition != null && !foreign.condition.equals(parent.condition))
            condition = "(" + foreign.condition + ") AND (" + parent.condition + ")";
        else if(foreign.condition != null)
            condition = foreign.condition;
        else
            condition = parent.condition;


        SqlTableAccess merged = new SqlTableAccess(foreign.table, condition);

        for(UsedVariable var : foreign.getVariables().getValues())
        {
            UsedVariable other = parent.getVariables().get(var.getName());

            if(other == null)
            {
                merged.getVariables().add(var);
            }
            else
            {
                assert var.getClasses().size() == 1;
                assert other.getClasses().size() == 1;
                assert var.getClasses().get(0) == other.getClasses().get(0);

                UsedVariable newVar = new UsedVariable(var.getName(), var.getClasses().get(0),
                        var.canBeNull() && other.canBeNull());
                merged.getVariables().add(newVar);
            }
        }

        for(UsedVariable var : parent.getVariables().getValues())
        {
            if(foreign.getVariables().get(var.getName()) == null)
                merged.getVariables().add(var);
        }


        merged.mappings.putAll(foreign.mappings);
        merged.valueConditions.addAll(foreign.valueConditions);
        merged.notNullConditions.addAll(foreign.notNullConditions);

        for(Pair<Node, ParametrisedMapping> cond : parent.valueConditions)
            merged.addValueCondition(cond.getLeft(), (ParametrisedMapping) cond.getRight().remapColumns(columnMap));

        for(ParametrisedLiteralMapping mapping : parent.notNullConditions)
            merged.addNotNullCondition((ParametrisedLiteralMapping) mapping.remapColumns(columnMap));


        for(Entry<String, ArrayList<NodeMapping>> entry : parent.mappings.entrySet())
        {
            String variable = entry.getKey();

            for(NodeMapping mapping : entry.getValue())
            {
                NodeMapping remaped = mapping.remapColumns(columnMap);
                ArrayList<NodeMapping> list = merged.mappings.get(variable);

                if(list == null)
                {
                    list = new ArrayList<NodeMapping>();
                    merged.mappings.put(variable, list);
                }

                if(!list.contains(remaped))
                    list.add(remaped);
            }
        }

        return merged;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<String, ArrayList<NodeMapping>> entry : mappings.entrySet())
        {
            appendComma(builder, hasSelect);
            hasSelect = true;

            ArrayList<NodeMapping> mappings = entry.getValue();
            ResourceClass resourceClass = mappings.get(0).getResourceClass(); // all mappings have the same class

            for(int i = 0; i < resourceClass.getPartsCount(); i++)
            {
                appendComma(builder, i > 0);

                //TODO: remove COALESCE if it is possible

                if(mappings.size() > 1)
                    builder.append("COALESCE(");

                for(int j = 0; j < mappings.size(); j++)
                {
                    appendComma(builder, j > 0);
                    builder.append(mappings.get(j).getSqlValueAccess(i));
                }

                if(mappings.size() > 1)
                    builder.append(")");

                builder.append(" AS ");
                builder.append(resourceClass.getSqlColumn(entry.getKey(), i));
            }
        }


        if(!hasSelect)
            builder.append("1");


        if(table != null)
        {
            builder.append(" FROM ");
            builder.append(table);
        }


        boolean hasWhereCondition = !valueConditions.isEmpty() || !notNullConditions.isEmpty();

        for(Entry<String, ArrayList<NodeMapping>> entry : mappings.entrySet())
            if(entry.getValue().size() > 1)
                hasWhereCondition = true;

        if(hasWhereCondition)
        {
            builder.append(" WHERE ");
            boolean hasWhere = false;

            if(condition != null)
            {
                hasWhere = true;
                builder.append("(");
                builder.append(condition);
                builder.append(") ");
            }


            for(Pair<Node, ParametrisedMapping> entry : valueConditions)
            {
                Node node = entry.getLeft();
                ParametrisedMapping mapping = entry.getRight();

                appendAnd(builder, hasWhere);
                hasWhere = true;

                for(int i = 0; i < mapping.getResourceClass().getPartsCount(); i++)
                {
                    appendAnd(builder, i > 0);
                    builder.append(mapping.getSqlValueAccess(i));
                    builder.append(" = ");
                    builder.append(mapping.getResourceClass().getSqlValue(node, i));
                }
            }


            for(ParametrisedLiteralMapping mapping : notNullConditions)
            {
                appendAnd(builder, hasWhere);
                hasWhere = true;

                for(int i = 0; i < mapping.getResourceClass().getPartsCount(); i++)
                {
                    appendAnd(builder, i > 0);
                    builder.append(mapping.getSqlValueAccess(i));
                    builder.append(" IS NOT NULL ");
                }
            }


            for(Entry<String, ArrayList<NodeMapping>> entry : mappings.entrySet())
            {
                for(int m = 0; m < entry.getValue().size() - 1; m++)
                {
                    NodeMapping map1 = entry.getValue().get(m);
                    NodeMapping map2 = entry.getValue().get(m + 1);

                    appendAnd(builder, hasWhere);
                    hasWhere = true;

                    assert map1.getResourceClass() == map2.getResourceClass();

                    for(int i = 0; i < map1.getResourceClass().getPartsCount(); i++)
                    {
                        appendAnd(builder, i > 0);

                        builder.append("(");
                        builder.append(map1.getSqlValueAccess(i));
                        builder.append(" = ");
                        builder.append(map2.getSqlValueAccess(i));

                        //TODO: remove IS NULL if it is possible

                        builder.append(" OR ");
                        builder.append(map1.getSqlValueAccess(i));
                        builder.append(" IS NULL");

                        builder.append(" OR ");
                        builder.append(map2.getSqlValueAccess(i));
                        builder.append(" IS NULL");
                        builder.append(")");
                    }
                }
            }
        }


        return builder.toString();
    }
}


final class Pair<L, R>
{
    private L left;
    private R right;


    public Pair(L left, R right)
    {
        this.left = left;
        this.right = right;
    }


    public final L getLeft()
    {
        return left;
    }


    public final R getRight()
    {
        return right;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof Pair))
            return false;

        @SuppressWarnings("unchecked")
        Pair<L, R> other = (Pair<L, R>) obj;

        return left.equals(other.left) && right.equals(other.right);
    }
}
