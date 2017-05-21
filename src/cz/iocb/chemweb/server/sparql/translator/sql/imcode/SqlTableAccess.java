package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

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
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public class SqlTableAccess extends SqlIntercode
{
    private final String table;
    private final String condition;

    private final LinkedHashMap<String, NodeMapping> mappings = new LinkedHashMap<String, NodeMapping>();

    private final ArrayList<ParametrisedLiteralMapping> notNullConditions = new ArrayList<ParametrisedLiteralMapping>();
    private final ArrayList<Pair<Node, ParametrisedMapping>> valueConditions = new ArrayList<Pair<Node, ParametrisedMapping>>();
    private final ArrayList<Pair<NodeMapping, NodeMapping>> equalityConditions = new ArrayList<Pair<NodeMapping, NodeMapping>>();


    public SqlTableAccess(String table, String condition)
    {
        super(new UsedVariables());
        this.table = table;
        this.condition = condition;
    }


    public void addMapping(String variable, NodeMapping mapping)
    {
        UsedVariable other = variables.get(variable);

        if(other != null && other.containsClass(mapping.getResourceClass()))
            return;

        if(other != null)
            other.addClass(mapping.getResourceClass());
        else
            variables.add(new UsedVariable(variable, mapping.getResourceClass(), false));

        mappings.put(variable, mapping);
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


    public void addEqualityCondition(NodeMapping map1, NodeMapping map2)
    {
        Pair<NodeMapping, NodeMapping> cond = new Pair<NodeMapping, NodeMapping>(map1, map2);
        Pair<NodeMapping, NodeMapping> revCond = new Pair<NodeMapping, NodeMapping>(map2, map1);

        if(!equalityConditions.contains(cond) && !equalityConditions.contains(revCond))
            equalityConditions.add(cond);
    }


    public void addEqualityConditions(List<Pair<NodeMapping, NodeMapping>> conditions)
    {
        for(Pair<NodeMapping, NodeMapping> cond : conditions)
            if(!equalityConditions.contains(cond))
                equalityConditions.add(cond);
    }


    public static boolean canBeMerged(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        if(!left.table.equals(right.table))
            return false;


        LinkedList<String> columns = new LinkedList<String>();

        for(Entry<String, NodeMapping> leftEntry : left.mappings.entrySet())
        {
            for(Entry<String, NodeMapping> rightEntry : right.mappings.entrySet())
            {
                if(!leftEntry.getKey().equals(rightEntry.getKey()))
                    continue;

                NodeMapping leftMap = leftEntry.getValue();
                NodeMapping rightMap = rightEntry.getValue();

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


    public static List<KeyPair> canBeDroped(DatabaseSchema schema, SqlTableAccess parent, SqlTableAccess foreign)
    {
        List<List<KeyPair>> keys = schema.getForeignKeys(parent.table, foreign.table);

        if(keys == null)
            return null;

        //TODO: can be parent.condition transformed?
        if(parent.condition != null)
            return null;


        ArrayList<KeyPair> columns = new ArrayList<KeyPair>();
        LinkedHashSet<String> parentColumns = new LinkedHashSet<String>();

        for(Entry<String, NodeMapping> parentEntry : parent.mappings.entrySet())
        {
            NodeMapping parentMap = parentEntry.getValue();

            if(parentMap instanceof ParametrisedMapping)
                for(int i = 0; i < parentMap.getResourceClass().getPartsCount(); i++)
                    parentColumns.add(parentMap.getSqlValueAccess(i));


            for(Entry<String, NodeMapping> foreignEntry : foreign.mappings.entrySet())
            {
                if(!parentEntry.getKey().equals(foreignEntry.getKey()))
                    continue;

                NodeMapping foreignMap = foreignEntry.getValue();

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
        merged.equalityConditions.addAll(left.equalityConditions);

        merged.mappings.putAll(right.mappings);
        merged.addValueConditions(right.valueConditions);
        merged.addNotNullConditions(right.notNullConditions);
        merged.addEqualityConditions(right.equalityConditions);

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
            assert !var.canBeNull();
            merged.getVariables().add(var);
        }

        for(UsedVariable var : parent.getVariables().getValues())
        {
            assert !var.canBeNull();

            if(foreign.getVariables().get(var.getName()) == null)
                merged.getVariables().add(var);
        }

        merged.mappings.putAll(foreign.mappings);
        merged.valueConditions.addAll(foreign.valueConditions);
        merged.notNullConditions.addAll(foreign.notNullConditions);
        merged.equalityConditions.addAll(foreign.equalityConditions);


        for(Entry<String, NodeMapping> entry : parent.mappings.entrySet())
        {
            String variable = entry.getKey();
            NodeMapping mapping = entry.getValue();
            NodeMapping remaped = mapping.remapColumns(columnMap);

            NodeMapping foreignMapping = foreign.mappings.get(variable);

            if(foreignMapping == null)
                merged.mappings.put(variable, remaped);
            else if(!remaped.equals(foreignMapping))
                merged.equalityConditions.add(new Pair<NodeMapping, NodeMapping>(foreignMapping, remaped));
        }


        for(Pair<Node, ParametrisedMapping> cond : parent.valueConditions)
            merged.addValueCondition(cond.getLeft(), (ParametrisedMapping) cond.getRight().remapColumns(columnMap));

        for(ParametrisedLiteralMapping mapping : parent.notNullConditions)
            merged.addNotNullCondition((ParametrisedLiteralMapping) mapping.remapColumns(columnMap));

        for(Pair<NodeMapping, NodeMapping> cond : parent.equalityConditions)
            merged.addEqualityCondition(cond.getLeft().remapColumns(columnMap),
                    cond.getRight().remapColumns(columnMap));


        return merged;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<String, NodeMapping> entry : mappings.entrySet())
        {
            appendComma(builder, hasSelect);
            hasSelect = true;

            NodeMapping mapping = entry.getValue();

            for(int i = 0; i < mapping.getResourceClass().getPartsCount(); i++)
            {
                appendComma(builder, i > 0);

                builder.append(mapping.getSqlValueAccess(i));
                builder.append(" AS ");
                builder.append(mapping.getResourceClass().getSqlColumn(entry.getKey(), i));
            }
        }

        if(!hasSelect)
            builder.append("1");


        if(table != null)
        {
            builder.append(" FROM ");
            builder.append(table);
        }


        if(!valueConditions.isEmpty() || !notNullConditions.isEmpty() || !equalityConditions.isEmpty())
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


            for(Pair<NodeMapping, NodeMapping> entry : equalityConditions)
            {
                NodeMapping map1 = entry.getLeft();
                NodeMapping map2 = entry.getRight();

                appendAnd(builder, hasWhere);
                hasWhere = true;

                assert map1.getResourceClass() == map2.getResourceClass();

                for(int i = 0; i < map1.getResourceClass().getPartsCount(); i++)
                {
                    appendAnd(builder, i > 0);
                    builder.append(map1.getSqlValueAccess(i));
                    builder.append(" = ");
                    builder.append(map2.getSqlValueAccess(i));
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
