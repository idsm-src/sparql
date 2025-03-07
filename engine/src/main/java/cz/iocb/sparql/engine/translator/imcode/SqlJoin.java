package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.translator.imcode.SqlTableAccess.remap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlJoin extends SqlIntercode
{
    private final List<Table> tables;
    private final List<SqlIntercode> childs;
    private final Map<Column, Column> columnMap;


    protected SqlJoin(List<SqlIntercode> childs, List<Table> tables, UsedVariables variables,
            Map<Column, Column> columnMap)
    {
        super(variables, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMap = columnMap;
        this.tables = tables;
    }


    public static SqlIntercode join(Request request, SqlIntercode left, SqlIntercode right)
    {
        return convertToIntercode(request, expand(List.of(left, right)), null);
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

        return convertToIntercode(request, unionList, restrictions);
    }


    private static SqlIntercode convertToIntercode(Request request, List<List<SqlIntercode>> unionList,
            Set<String> restrictions)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        // just to be sure
        unionList = unionList.stream().map(innerList -> innerList.stream().collect(toList())).collect(toList());

        for(List<SqlIntercode> newChilds : unionList)
        {
            for(SqlRecursive recursive : newChilds.stream().filter(c -> c instanceof SqlRecursive)
                    .map(c -> (SqlRecursive) c).toList())
            {
                List<SqlIntercode> accesses = newChilds.stream().filter(c -> c instanceof SqlTableAccess).toList();

                List<SqlIntercode> newInitChilds = new ArrayList<SqlIntercode>(accesses);
                newInitChilds.add(recursive.init);

                SqlIntercode newRecursive = SqlRecursive
                        .create(request, SqlJoin.join(newInitChilds, restrictions), recursive.next, recursive.beginName,
                                recursive.joinName, recursive.endVar.getName(), recursive.graphName, restrictions)
                        .optimize(request, restrictions, false);

                newChilds.remove(recursive);
                newChilds.removeAll(accesses);
                newChilds.add(newRecursive);

                break; //TODO: process others
            }

            for(SqlIntercode recursive : newChilds.stream()
                    .filter(c -> c instanceof SqlDistinct d && d.getChild() instanceof SqlUnion).toList())
            {
                SqlDistinct distinct = (SqlDistinct) recursive;
                SqlUnion union = (SqlUnion) distinct.getChild();


                List<SqlTableAccess> candidates = newChilds.stream().filter(c -> c instanceof SqlTableAccess)
                        .map(c -> (SqlTableAccess) c).toList();

                List<SqlTableAccess> distinctParts = union.getChilds().stream().filter(c -> c instanceof SqlTableAccess)
                        .map(c -> (SqlTableAccess) c).toList();


                for(SqlTableAccess candidate : candidates)
                {
                    HashSet<String> shared = new HashSet<String>(candidate.getVariables().getNames());
                    shared.retainAll(distinct.getVariables().getNames());

                    for(SqlTableAccess distinctPart : distinctParts)
                    {
                        SqlIntercode intercode = tryReduceJoin(schema, candidate, distinctPart, shared);

                        if(intercode != null)
                        {
                            newChilds.remove(distinct);
                            newChilds.remove(candidate);
                            newChilds.add(intercode);
                        }
                    }
                }
            }
        }

        return SqlUnion.union(unionList.stream().map(l -> join(l, restrictions)).collect(toList()));
    }


    private static SqlIntercode tryReduceJoin(DatabaseSchema schema, SqlTableAccess candidate, SqlTableAccess distinct,
            HashSet<String> shared)
    {
        if(!distinct.getVariables().restrict(shared).getNonConstantColumns()
                .equals(distinct.getVariables().getNonConstantColumns()))
            return null;

        for(String s : shared)
        {
            UsedVariable v1 = candidate.getInternalVariable(s);
            UsedVariable v2 = distinct.getInternalVariable(s);

            if(v1 == null || v2 == null || v1.canBeNull() || v2.canBeNull() || v1.getMappings().size() != 1
                    || v2.getMappings().size() != 1 || !v1.getResourceClass().equals(v2.getResourceClass())
                    || !new UsedPairedVariable(v1, v2).isJoinable())
                return null;
        }

        if(Objects.equals(distinct.getTable(), candidate.getTable()))
        {
            if(!Conditions.and(candidate.getConditions(), distinct.getConditions()).equals(candidate.getConditions()))
                return null;

            for(String s : shared)
            {
                List<Column> cols1 = candidate.getInternalVariable(s).getMapping();
                List<Column> cols2 = distinct.getInternalVariable(s).getMapping();

                for(int i = 0; i < cols1.size(); i++)
                    if(!candidate.getConditions().getEqualColumns(cols1.get(i)).contains(cols2.get(i)))
                        return null;
            }

            if(!distinct.getInternalVariables().restrict(shared).getNonConstantColumns()
                    .equals(distinct.getVariables().getNonConstantColumns()))
                return null;


            UsedVariables internal = new UsedVariables(candidate.getInternalVariables());

            for(UsedVariable v : distinct.getVariables().getValues())
                if(!shared.contains(v.getName()))
                    internal.add(new UsedVariable(v));

            return SqlTableAccess.create(candidate.getTable(), candidate.getConditions(), internal,
                    candidate.getReduced());
        }
        else
        {
            List<List<ColumnPair>> keys = schema.getForeignKeys(distinct.getTable(), candidate.getTable());

            if(keys == null)
                return null;

            for(List<ColumnPair> key : keys)
            {
                Map<Column, Column> map = new HashMap<Column, Column>();

                for(ColumnPair pair : key)
                    for(Column c : distinct.getConditions().getEqualColumns(pair.getLeft()))
                        map.put(c, pair.getRight());

                if(!map.keySet().containsAll(distinct.getVariables().getNonConstantColumns()))
                    continue;

                if(!map.keySet().containsAll(distinct.getConditions().getNonConstantColumns()))
                    continue;

                SqlTableAccess remapped = (SqlTableAccess) SqlTableAccess.create(candidate.getTable(),
                        remap(map, distinct.getConditions()), remap(map, distinct.getInternalVariables()),
                        distinct.getReduced());

                SqlIntercode intercode = tryReduceJoin(schema, candidate, remapped, shared);

                if(intercode != null)
                    return intercode;
            }
        }

        return null;
    }


    @Override
    public String translate(Request request)
    {
        Set<Column> columns = variables.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> columnMap.get(c) + " AS " + c).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM ");

        for(int i = 0; i < childs.size(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append("(");
            builder.append(childs.get(i).translate(request));
            builder.append(") AS ");
            builder.append(tables.get(i));
        }

        String condition = generateJoinCondition(childs.stream().map(c -> c.getVariables()).collect(toList()), tables);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toString();
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
                if(v.isConstant())
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

                        if(v != null && !v.isConstant() && v.getMapping(e.getValue().getResourceClass()) != null)
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


        List<Table> tables = IntStream.range(0, childs.size()).mapToObj(i -> new Table("tab" + i)).collect(toList());

        Map<Column, Column> columnMap = new HashMap<Column, Column>();
        List<UsedVariables> allVars = childs.stream().map(c -> c.getVariables()).toList();
        UsedVariables variables = getJoinUsedVariables(allVars, tables, restrictions, columnMap);

        if(variables == null)
            return SqlNoSolution.get();

        return new SqlJoin(childs, tables, variables, columnMap);
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
