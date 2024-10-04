package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlJoin extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final List<SqlIntercode> childs;
    private final List<UsedVariables> variables;
    private final List<Map<Column, Column>> columnMaps;


    protected SqlJoin(List<SqlIntercode> childs, List<UsedVariables> variables, List<Map<Column, Column>> columnMaps)
    {
        super(variables.get(variables.size() - 1), childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.variables = variables;
        this.columnMaps = columnMaps;
    }


    public static SqlIntercode join(SqlIntercode left, SqlIntercode right)
    {
        return convertToIntercode(expand(List.of(left, right)), null);
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        Set<String> childRestrictions = getRestrictions(childs, restrictions);

        List<List<SqlIntercode>> unionList = expand(optimize(request, childs, childRestrictions, reduced));
        unionList = reoptimizeUnion(request, unionList, restrictions, reduced);
        unionList = reduceUnion(request, unionList, restrictions);

        return convertToIntercode(unionList, restrictions);
    }


    private static SqlIntercode convertToIntercode(List<List<SqlIntercode>> unionList, Set<String> restrictions)
    {
        return SqlUnion.union(unionList.stream().map(l -> join(l, restrictions)).collect(toList()));
    }


    @Override
    public String translate(Request request)
    {
        String translate = childs.get(0).translate(request);
        UsedVariables leftVariables = childs.get(0).getVariables();

        for(int i = 1; i < childs.size(); i++)
        {
            Set<Column> columns = variables.get(i - 1).getNonConstantColumns();
            Map<Column, Column> map = columnMaps.get(i - 1);

            StringBuilder builder = new StringBuilder();

            builder.append("SELECT ");

            if(!columns.isEmpty())
                builder.append(columns.stream().map(c -> map.get(c) + " AS " + c).collect(joining(", ")));
            else
                builder.append("1");

            builder.append(" FROM (");
            builder.append(translate);
            builder.append(" ) AS ");
            builder.append(leftTable);

            builder.append(", (");
            builder.append(childs.get(i).translate(request));
            builder.append(" ) AS ");
            builder.append(rightTable);

            String condition = generateJoinCondition(leftVariables, childs.get(i).getVariables(), leftTable,
                    rightTable);

            if(condition != null)
            {
                builder.append(" WHERE ");
                builder.append(condition);
            }

            leftVariables = variables.get(i - 1);
            translate = builder.toString();
        }

        return translate;
    }


    private static HashSet<String> getRestrictions(List<SqlIntercode> childs, int i, int j, Set<String> restrictions)
    {
        if(restrictions == null)
            return null;

        HashSet<String> subrestrictions = new HashSet<String>(restrictions);

        for(int k = 0; k < childs.size(); k++)
            if(k != i && k != j)
                subrestrictions.addAll(childs.get(k).getVariables().getNames());

        return subrestrictions;
    }


    public static List<SqlIntercode> optimize(Request request, List<SqlIntercode> childs, Set<String> restrictions,
            boolean reduced)
    {
        List<SqlIntercode> optimized = new ArrayList<SqlIntercode>(childs.size());

        for(SqlIntercode child : childs)
            optimized.add(child.optimize(request, restrictions, reduced));

        return optimized;
    }


    public static List<List<SqlIntercode>> reoptimizeUnion(Request request, List<List<SqlIntercode>> unionList,
            Set<String> restrictions, boolean reduced)
    {
        List<List<SqlIntercode>> optUnionList = new ArrayList<List<SqlIntercode>>();
        Stack<List<SqlIntercode>> unionStack = new Stack<List<SqlIntercode>>();
        unionStack.addAll(unionList);

        while(!unionStack.isEmpty())
        {
            List<SqlIntercode> joinList = unionStack.pop();

            Set<String> newRestrictions = getRestrictions(joinList, restrictions);

            if(!newRestrictions.containsAll(getVariables(joinList)))
                unionStack.addAll(expand(reoptimizeJoin(request, joinList, newRestrictions, reduced)));
            else
                optUnionList.add(joinList);
        }

        return optUnionList;
    }


    private static List<SqlIntercode> reoptimizeJoin(Request request, List<SqlIntercode> childs,
            Set<String> restrictions, boolean reduced)
    {
        List<SqlIntercode> optimized = new ArrayList<SqlIntercode>(childs.size());

        for(SqlIntercode child : childs)
            if(restrictions.containsAll(child.getVariables().getNames()))
                optimized.add(child);
            else
                optimized.add(child.optimize(request, restrictions, reduced));

        return optimized;
    }


    public static List<List<SqlIntercode>> expand(List<SqlIntercode> childs)
    {
        List<List<SqlIntercode>> unionList = new ArrayList<List<SqlIntercode>>();
        unionList.add(new ArrayList<SqlIntercode>());

        for(SqlIntercode child : childs)
        {
            if(child == SqlNoSolution.get())
                return new ArrayList<List<SqlIntercode>>(0);

            if(child instanceof SqlUnion)
            {
                List<List<SqlIntercode>> newUnionList = new ArrayList<List<SqlIntercode>>();

                for(SqlIntercode unionChild : ((SqlUnion) child).getChilds())
                {
                    List<SqlIntercode> itemList = getJoinList(unionChild);

                    for(List<SqlIntercode> joinList : unionList)
                    {
                        List<SqlIntercode> newJoinList = new ArrayList<SqlIntercode>(joinList.size() + itemList.size());
                        newJoinList.addAll(joinList);
                        newJoinList.addAll(itemList);
                        newUnionList.add(newJoinList);
                    }
                }

                unionList = newUnionList;
            }
            else if(child != SqlEmptySolution.get())
            {
                List<SqlIntercode> itemList = getJoinList(child);

                for(List<SqlIntercode> joinList : unionList)
                    joinList.addAll(itemList);
            }
        }

        return unionList;
    }


    private List<List<SqlIntercode>> reduceUnion(Request request, List<List<SqlIntercode>> childs,
            Set<String> restrictions)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        List<List<SqlIntercode>> newChilds = new ArrayList<List<SqlIntercode>>();

        for(List<SqlIntercode> child : childs)
        {
            List<SqlIntercode> reduced = reduceJoin(child, restrictions, schema);

            if(reduced != null)
                newChilds.add(reduced);
        }

        return newChilds;
    }


    private static List<SqlIntercode> reduceJoin(List<SqlIntercode> childs, Set<String> restrictions,
            DatabaseSchema schema)
    {
        ArrayList<SqlIntercode> optChilds = new ArrayList<SqlIntercode>(childs);


        HashMap<String, UsedVariable> constants = new HashMap<String, UsedVariable>();

        for(SqlIntercode child : childs)
        {
            for(UsedVariable v : child.getVariables().getValues())
            {
                if(child.hasConstantVariable(v.getName()))
                {
                    UsedVariable old = constants.put(v.getName(), v);

                    if(old != null && !old.equals(v))
                        return List.of(SqlNoSolution.get());
                }
            }
        }

        if(!constants.isEmpty())
        {
            for(int i = 0; i < optChilds.size(); i++)
            {
                SqlIntercode child = optChilds.get(i);

                if(child instanceof SqlTableAccess access)
                {
                    Condition cnd = new Condition();
                    HashMap<String, UsedVariable> skip = new HashMap<String, UsedVariable>();

                    for(Entry<String, UsedVariable> e : constants.entrySet())
                    {
                        UsedVariable v = access.getInternalVariable(e.getKey());

                        if(v != null && !child.hasConstantVariable(e.getKey()))
                        {
                            cnd.addAreEqual(e.getValue().getMapping(), v.getMapping(e.getValue().getResourceClass()));
                            skip.put(e.getKey(), e.getValue());
                        }
                    }

                    if(!skip.isEmpty())
                    {
                        Conditions cnds = Conditions.and(access.getConditions(), cnd);

                        if(cnds.isFalse())
                            return List.of(SqlNoSolution.get());

                        UsedVariables internal = new UsedVariables();

                        for(String name : access.getVariables().getNames())
                        {
                            if(skip.containsKey(name))
                                internal.add(new UsedVariable(skip.get(name)));
                            else
                                internal.add(access.getInternalVariable(name));
                        }

                        optChilds.set(i, SqlTableAccess.create(access.getTable(), cnds, internal, access.getReduced()));
                    }
                }
            }
        }


        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlValues left)
            {
                for(int j = 0; j < optChilds.size(); j++)
                {
                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        HashSet<String> mergeRestrictions = getRestrictions(optChilds, i, j, restrictions);
                        SqlIntercode merged = SqlTableAccess.tryReduceJoinWithValues(schema, right, left,
                                mergeRestrictions);

                        if(merged == SqlNoSolution.get())
                            return List.of(SqlNoSolution.get());

                        if(merged != null)
                        {
                            optChilds.set(j, merged);
                            optChilds.remove(i);

                            if(j < i)
                                i--;

                            break;
                        }
                    }
                }
            }
        }


        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlTableAccess left)
            {
                for(int j = 0; j < i; j++)
                {
                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        Set<String> mergeRestrictions = getRestrictions(optChilds, i, j, restrictions);
                        SqlIntercode merged = SqlTableAccess.tryReduceJoin(schema, left, right, mergeRestrictions);

                        if(merged == SqlNoSolution.get())
                            return List.of(SqlNoSolution.get());

                        if(merged != null)
                        {
                            optChilds.set(i, merged);
                            optChilds.remove(j);
                            i -= 2;
                            break;
                        }
                    }
                }
            }
        }

        return optChilds;
    }


    private static SqlIntercode join(List<SqlIntercode> childs, Set<String> restrictions)
    {
        if(childs.size() == 0)
            return SqlEmptySolution.get();

        if(childs.size() == 1)
            return childs.get(0);

        UsedVariables variables = childs.get(0).getVariables();
        List<UsedVariables> variablesChain = new ArrayList<UsedVariables>(childs.size() - 1);
        List<Map<Column, Column>> columnMaps = new ArrayList<Map<Column, Column>>(childs.size() - 1);

        for(int i = 1; i < childs.size(); i++)
        {
            Map<Column, Column> map = new HashMap<Column, Column>();
            variables = getJoinUsedVariables(variables, childs.get(i).getVariables(), leftTable, rightTable, null, map);

            if(variables == null)
                return SqlNoSolution.get();

            if(i == childs.size() - 1)
                variables = variables.restrict(restrictions);

            variablesChain.add(variables);
            columnMaps.add(map);
        }

        return new SqlJoin(childs, variablesChain, columnMaps);
    }


    private static Set<String> getVariables(List<SqlIntercode> childs)
    {
        Set<String> allVariables = new HashSet<String>();

        for(SqlIntercode child : childs)
            allVariables.addAll(child.getVariables().getNames());

        return allVariables;
    }


    private static Set<String> getRestrictions(List<SqlIntercode> childs, Set<String> restrictions)
    {
        Set<String> allVariables = new HashSet<String>();
        Set<String> childRestrictions = new HashSet<String>(restrictions);

        for(SqlIntercode child : childs)
        {
            for(String variable : child.getVariables().getNames())
            {
                if(allVariables.contains(variable))
                    childRestrictions.add(variable);
                else
                    allVariables.add(variable);
            }
        }

        return childRestrictions;
    }


    public final static List<SqlIntercode> getJoinList(SqlIntercode child)
    {
        if(child instanceof SqlJoin)
            return new ArrayList<SqlIntercode>(((SqlJoin) child).getChilds());
        else
            return List.of(child);
    }


    public final List<SqlIntercode> getChilds()
    {
        return childs;
    }
}
