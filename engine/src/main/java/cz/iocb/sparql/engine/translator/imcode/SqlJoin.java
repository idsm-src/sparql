package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
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
import cz.iocb.sparql.engine.translator.UsedPairedVariable.PairedClass;
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
        unionList = unionList.stream()
                .map(innerList -> (List<SqlIntercode>) innerList.stream().collect(toCollection(ArrayList::new)))
                .toList();

        for(List<SqlIntercode> newChilds : unionList)
        {
            for(SqlRecursive recursive : newChilds.stream().filter(c -> c instanceof SqlRecursive)
                    .map(c -> (SqlRecursive) c).toList())
            {
                List<SqlIntercode> accesses = newChilds.stream().filter(c -> c instanceof SqlTableAccess).toList();

                List<SqlIntercode> newInitChilds = new ArrayList<SqlIntercode>(accesses);
                newInitChilds.add(recursive.init);

                SqlIntercode newRecursive = SqlRecursive.create(request,
                        SqlJoin.join(request, newInitChilds, restrictions), recursive.next, recursive.beginName,
                        recursive.joinName, recursive.endVar.getName(), recursive.graphName, restrictions)
                        .optimize(request, restrictions, false);

                newChilds.remove(recursive);
                newChilds.removeAll(accesses);
                newChilds.add(newRecursive);

                break; //TODO: process others
            }

            loop:
            for(SqlIntercode child : newChilds.stream()
                    .filter(c -> c instanceof SqlDistinct d && d.getChild() instanceof SqlUnion).toList())
            {
                SqlDistinct distinct = (SqlDistinct) child;
                SqlUnion union = (SqlUnion) distinct.getChild();


                List<SqlTableAccess> candidates = newChilds.stream().filter(c -> c instanceof SqlTableAccess)
                        .map(c -> (SqlTableAccess) c).toList();

                List<SqlTableAccess> distinctParts = union.getChilds().stream().filter(c -> c instanceof SqlTableAccess)
                        .map(c -> (SqlTableAccess) c).toList();


                for(SqlTableAccess candidate : candidates)
                {
                    if(!canBeDistinctUnionReduced(distinct, candidate))
                        continue;

                    for(SqlTableAccess distinctPart : distinctParts)
                    {
                        SqlIntercode intercode = tryReduceDistinct(schema, candidate, distinctPart, restrictions);

                        if(intercode instanceof SqlTableAccess a && a.getConditions().equals(candidate.getConditions()))
                        {
                            newChilds.remove(distinct);
                            newChilds.remove(candidate);
                            newChilds.add(intercode);
                            continue loop;
                        }
                    }
                }


                List<SqlIntercode> unionChilds = union.getChilds().stream()
                        .filter(c -> newChilds.stream().allMatch(x -> UsedPairedVariable
                                .getPairs(c.getVariables(), x.getVariables()).stream().allMatch(p -> p.isJoinable())))
                        .toList();

                SqlDistinct.create(request, SqlUnion.union(request, unionChilds), distinct.getVariables().getNames());
            }
        }

        return SqlUnion.union(request, unionList.stream().map(l -> join(request, l, restrictions)).toList());
    }


    private static boolean canBeDistinctUnionReduced(SqlDistinct distinct, SqlIntercode candidate)
    {
        Set<Column> columns = new HashSet<Column>();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(distinct.getVariables(), candidate.getVariables()))
        {
            UsedVariable distinctVar = pair.getLeftVariable();
            UsedVariable candidateVar = pair.getRightVariable();

            if(distinctVar == null || candidateVar == null)
                continue;

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return false;

            if(distinctVar.canBeNull() || candidateVar.canBeNull())
                return false;

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                    return false;

                columns.addAll(distinctVar.getMapping(pairedClass.getLeftClass()));
            }
        }

        return columns.containsAll(distinct.getVariables().getNonConstantColumns());
    }


    private static SqlIntercode tryReduceDistinct(DatabaseSchema schema, SqlTableAccess candidate,
            SqlTableAccess distinct, Set<String> restrictions)
    {
        if(Objects.equals(distinct.getTable(), candidate.getTable()))
        {
            Set<Column> joinColumns = SqlTableAccess.getJoinColumns(distinct, candidate);

            if(!joinColumns.containsAll(distinct.getVariables().getNonConstantColumns()))
                return null;

            return SqlTableAccess.joinByPrimaryKey(candidate, distinct, restrictions);
        }
        else
        {
            if(distinct.hasExpression())
                return null;

            Set<ColumnPair> columns = SqlTableAccess.getJoinColumnPairs(distinct, candidate);

            Set<Column> parentColumns = new HashSet<Column>();
            parentColumns.addAll(distinct.getConditions().getNonConstantColumns());
            parentColumns.addAll(distinct.getInternalVariables().getNonConstantColumns());

            if(!columns.stream().map(p -> p.getLeft()).collect(toSet()).containsAll(parentColumns))
                return null;

            List<Set<ColumnPair>> keys = schema.getForeignKeys(distinct.getTable(), candidate.getTable());

            for(Set<ColumnPair> key : keys)
                if(key.containsAll(columns))
                    return SqlTableAccess.joinByForeignKey(distinct, candidate, columns, restrictions);
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

        String condition = generateJoinCondition(childs.stream().map(c -> c.getVariables()).toList(), tables);

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

            if(child instanceof SqlUnion union)
            {
                List<List<SqlIntercode>> newUnionList = new ArrayList<List<SqlIntercode>>();

                for(SqlIntercode unionChild : union.getChilds())
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
                    SqlIntercode merged = null;

                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        Set<String> mergeRestrictions = getRestrictions(optChilds, i, j, restrictions);
                        merged = SqlTableAccess.tryReduceJoin(schema, left, right, mergeRestrictions);
                    }
                    else if(optChilds.get(j) instanceof SqlDistinct d && d.getChild() instanceof SqlTableAccess right)
                    {
                        Set<String> mergeRestrictions = getRestrictions(optChilds, i, j, restrictions);
                        merged = tryReduceDistinct(schema, left, right, mergeRestrictions);
                    }

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

        return optChilds;
    }


    private static SqlIntercode join(Request request, List<SqlIntercode> childs, Set<String> restrictions)
    {
        if(childs.size() == 0)
            return SqlEmptySolution.get();

        if(childs.size() == 1)
            return childs.get(0);


        List<Table> tables = IntStream.range(0, childs.size()).mapToObj(i -> new Table("tab" + i)).toList();

        Map<Column, Column> columnMap = new HashMap<Column, Column>();
        List<UsedVariables> allVars = childs.stream().map(c -> c.getVariables()).toList();
        UsedVariables variables = getJoinUsedVariables(request, allVars, tables, restrictions, columnMap);

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
        if(child instanceof SqlJoin join)
            return new ArrayList<SqlIntercode>(join.getChilds());
        else
            return List.of(child);
    }


    public final List<SqlIntercode> getChilds()
    {
        return childs;
    }
}
