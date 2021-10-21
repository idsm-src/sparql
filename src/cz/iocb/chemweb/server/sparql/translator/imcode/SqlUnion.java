package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlUnion extends SqlIntercode
{
    private final List<SqlIntercode> childs;
    private final List<Map<Column, Column>> columnMappings;


    protected SqlUnion(UsedVariables variables, List<SqlIntercode> childs, List<Map<Column, Column>> columnMappings)
    {
        super(variables, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMappings = columnMappings;
    }


    public static SqlIntercode union(SqlIntercode left, SqlIntercode right)
    {
        /* special cases */

        if(left == SqlNoSolution.get())
            return right;

        if(right == SqlNoSolution.get())
            return left;


        /* standard union */

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left.getVariables(), right.getVariables());
        Map<String, Set<ResourceClass>> classes = new HashMap<String, Set<ResourceClass>>();

        for(UsedPairedVariable pair : pairs)
        {
            Set<ResourceClass> set = new HashSet<ResourceClass>();
            classes.put(pair.getName(), set);

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                    set.add(pairedClass.getRightClass());
                else if(pairedClass.getRightClass() == null)
                    set.add(pairedClass.getLeftClass());
                else if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                    set.add(pairedClass.getLeftClass());
                else
                    set.add(pairedClass.getLeftClass().getGeneralClass());
            }
        }


        List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

        if(left instanceof SqlUnion)
            childs.addAll(((SqlUnion) left).childs);
        else
            childs.add(left);


        if(right instanceof SqlUnion)
            childs.addAll(((SqlUnion) right).childs);
        else
            childs.add(right);


        Map<List<Column>, Column> unionColumns = new HashMap<List<Column>, Column>();

        List<Map<Column, Column>> columnMappings = new ArrayList<Map<Column, Column>>(childs.size());

        for(int i = 0; i < childs.size(); i++)
            columnMappings.add(new HashMap<Column, Column>());

        UsedVariables variables = new UsedVariables();

        for(Entry<String, Set<ResourceClass>> entry : classes.entrySet())
        {
            String name = entry.getKey();

            boolean canBeNull = left.getVariables().get(name) == null || left.getVariables().get(name).canBeNull()
                    || right.getVariables().get(name) == null || right.getVariables().get(name).canBeNull();

            UsedVariable variable = new UsedVariable(name, canBeNull);

            for(ResourceClass resourceClass : entry.getValue())
            {
                List<Column> columns = resourceClass.createColumns(variable.getName());
                List<Column> mapping = new ArrayList<Column>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                {
                    List<Column> cols = new ArrayList<Column>(childs.size());

                    for(SqlIntercode child : childs)
                    {
                        UsedVariable var = child.getVariables().get(name);

                        if(var == null)
                        {
                            cols.add(new ConstantColumn("NULL::" + resourceClass.getSqlTypes().get(i)));
                        }
                        else if(var.containsClass(resourceClass))
                        {
                            cols.add(var.getMapping(resourceClass).get(i));
                        }
                        else
                        {
                            Set<ResourceClass> variants = var.getCompatibleClasses(resourceClass);

                            if(variants.isEmpty())
                            {
                                cols.add(new ConstantColumn("NULL::" + resourceClass.getSqlTypes().get(i)));
                            }
                            else
                            {
                                StringBuilder builder = new StringBuilder();

                                if(variants.size() > 1)
                                    builder.append("coalesce(");

                                boolean hasAlternative = false;

                                for(ResourceClass variant : variants)
                                {
                                    appendComma(builder, hasAlternative);
                                    hasAlternative = true;

                                    builder.append(
                                            variant.toGeneralClass(var.getMapping(variant), var.canBeNull()).get(i));
                                }

                                if(variants.size() > 1)
                                    builder.append(")");

                                cols.add(new ExpressionColumn(builder.toString()));
                            }
                        }
                    }


                    if(cols.get(0) instanceof ConstantColumn && Collections.frequency(cols, cols.get(0)) == cols.size())
                    {
                        mapping.add(cols.get(0));
                    }
                    else if(unionColumns.containsKey(cols))
                    {
                        Column col = unionColumns.get(cols);
                        mapping.add(col);
                    }
                    else
                    {
                        Column col = columns.get(i);
                        unionColumns.put(cols, col);
                        mapping.add(col);

                        for(int j = 0; j < childs.size(); j++)
                            columnMappings.get(j).put(col, cols.get(j));
                    }
                }

                variable.addMapping(resourceClass, mapping);
            }

            variables.add(variable);
        }

        return new SqlUnion(variables, childs, columnMappings);
    }


    public final List<SqlIntercode> getChilds()
    {
        return childs;
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        SqlIntercode result = SqlNoSolution.get();

        for(SqlIntercode child : childs)
            result = union(result, child.optimize(restrictions, reduced));

        return result;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        Set<Column> columns = variables.getNonConstantColumns();


        for(int i = 0; i < childs.size(); i++)
        {
            if(i > 0)
                builder.append(" UNION ALL ");

            SqlIntercode child = childs.get(i);
            Map<Column, Column> columnMapping = columnMappings.get(i);

            builder.append("SELECT ");
            boolean hasSelect = false;

            for(Column column : columns)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                Column col = columnMapping.get(column);

                if(col == null)
                    builder.append("NULL AS ").append(column);
                else if(column.equals(col))
                    builder.append(column);
                else
                    builder.append(col).append(" AS ").append(column);
            }

            if(!hasSelect)
                builder.append("1");

            builder.append(" FROM (");
            builder.append(child.translate());
            builder.append(") AS tab");
            builder.append(i);
        }

        return builder.toString();
    }
}
