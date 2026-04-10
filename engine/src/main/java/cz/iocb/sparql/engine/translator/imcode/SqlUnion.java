package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlUnion extends SqlIntercode
{
    private final List<SqlIntercode> childs;
    private final List<Map<Column, Column>> columnMappings;


    protected SqlUnion(UsedVariables variables, List<SqlIntercode> childs, List<Map<Column, Column>> columnMappings)
    {
        super(variables, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMappings = columnMappings;
    }


    public static SqlIntercode union(Request request, List<SqlIntercode> branches)
    {
        /* special cases */

        branches = branches.stream().filter(i -> i != SqlNoSolution.get()).toList();

        if(branches.isEmpty())
            return SqlNoSolution.get();

        if(branches.size() == 1)
            return branches.get(0);


        /* standard union */

        Set<String> varNames = branches.stream().flatMap(i -> i.variables.getNames().stream()).collect(toSet());
        Map<String, Set<ResourceClass>> classes = new HashMap<String, Set<ResourceClass>>();

        for(String varName : varNames)
        {
            Set<ResourceClass> resources = new HashSet<ResourceClass>();

            for(SqlIntercode branche : branches)
            {
                UsedVariable var = branche.getVariables().get(varName);

                if(var != null)
                    resources.addAll(var.getClasses());
            }

            classes.put(varName, ResourceClass.getDisjunctClasses(resources));
        }


        List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

        for(SqlIntercode branch : branches)
        {
            if(branch instanceof SqlUnion union)
                childs.addAll(union.childs);
            else
                childs.add(branch);
        }


        Map<List<Column>, Column> unionColumns = new HashMap<List<Column>, Column>();
        List<Map<Column, Column>> columnMappings = new ArrayList<Map<Column, Column>>(childs.size());

        for(int i = 0; i < childs.size(); i++)
            columnMappings.add(new HashMap<Column, Column>());

        UsedVariables variables = new UsedVariables();

        for(Entry<String, Set<ResourceClass>> entry : classes.entrySet())
        {
            String name = entry.getKey();
            List<UsedVariable> vars = childs.stream().map(c -> c.getVariable(name)).toList();

            boolean canBeNull = vars.stream().anyMatch(v -> v == null || v.canBeNull());
            UsedVariable variable = new UsedVariable(name, canBeNull);

            for(ResourceClass resourceClass : entry.getValue())
            {
                if(vars.stream().filter(Objects::nonNull).flatMap(v -> v.getMappings().entrySet().stream())
                        .anyMatch(r -> isNull(r.getValue()) && !areDisjunct(r.getKey(), resourceClass)))
                {
                    variable.addMapping(resourceClass, null);
                    continue;
                }


                List<List<Column>> cols = new ArrayList<List<Column>>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                    cols.add(new ArrayList<Column>(childs.size()));


                for(UsedVariable var : vars)
                {
                    if(var == null)
                    {
                        for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            cols.get(i).add(new ConstantColumn(null, resourceClass.getSqlTypes().get(i)));
                    }
                    else
                    {
                        List<Column> c = var.deriveMapping(resourceClass);

                        for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            cols.get(i).add(c.get(i));
                    }
                }


                List<Column> columns = resourceClass.createColumns(request.getColumnMap(), variable.getName());
                List<Column> mapping = new ArrayList<Column>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                {
                    List<Column> c = cols.get(i);

                    if(c.get(0) instanceof ConstantColumn c0 && c.stream().allMatch(d -> d.equals(c0)))
                    {
                        mapping.add(c.get(0));
                    }
                    else if(unionColumns.containsKey(c))
                    {
                        mapping.add(unionColumns.get(c));
                    }
                    else
                    {
                        Column col = columns.get(i);
                        unionColumns.put(c, col);
                        mapping.add(col);

                        for(int j = 0; j < childs.size(); j++)
                            columnMappings.get(j).put(col, c.get(j));
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
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        List<SqlIntercode> optChilds = childs.stream()
                .map(c -> c.optimize(request, restrictions, reduced, evalServices))
                .filter(i -> i != SqlNoSolution.get()).toList();


        if(optChilds.isEmpty())
            return SqlNoSolution.get();

        if(optChilds.size() == 1)
            return optChilds.get(0);


        if(optChilds.equals(childs))
            return this;

        return union(request, optChilds);
    }


    @Override
    public String translate(Request request)
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
            builder.append(child.translate(request));
            builder.append(") AS tab");
            builder.append(i);
        }

        return builder.toString();
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return childs.stream().anyMatch(c -> c.hasServiceSubpattern());
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("union");

        for(int i = 0; i < childs.size(); i++)
        {
            indentChild(builder, indent, i == childs.size() - 1);
            childs.get(i).generateExplanation(builder, getIndent(indent, i == childs.size() - 1));
        }
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlUnion imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(new Multiset<>(childs), new Multiset<>(imcode.childs)))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(new Multiset<>(childs));
    }
}
