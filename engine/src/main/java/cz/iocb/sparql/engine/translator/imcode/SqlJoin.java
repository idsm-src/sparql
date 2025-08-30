package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedPairedVariable.PairedClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlJoin extends SqlIntercode
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
        return join(request, List.of(left, right), null);
    }


    protected static SqlIntercode join(Request request, List<SqlIntercode> childs, Restrictions restrictions)
    {
        List<Table> tables = IntStream.range(0, childs.size()).mapToObj(i -> new Table("tab" + i)).toList();

        Map<Column, Column> columnMap = new HashMap<Column, Column>();
        List<UsedVariables> allVars = childs.stream().map(c -> c.getVariables()).toList();
        UsedVariables variables = getJoinUsedVariables(request, allVars, tables, restrictions, columnMap);

        return new SqlJoin(childs, tables, variables, columnMap);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        List<SqlIntercode> optChilds = childs;

        if(optChilds.stream().anyMatch(c -> c instanceof SqlUnion || c instanceof SqlJoin))
            return SqlUnion
                    .union(request, expandJoin(optChilds).stream().map(l -> join(request, l, restrictions)).toList())
                    .optimize(request, restrictions, reduced, evalServices);

        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        while(true)
        {
            List<SqlIntercode> newOptChilds = reduceDistinctUnion(
                    reduceJoin(optimize(request, optChilds, restrictions, reduced, evalServices), restrictions, schema),
                    restrictions, schema);

            if(optChilds.stream().anyMatch(c -> c instanceof SqlUnion || c instanceof SqlJoin))
                return SqlUnion
                        .union(request,
                                expandJoin(optChilds).stream().map(l -> join(request, l, restrictions)).toList())
                        .optimize(request, restrictions, reduced, evalServices);

            if(newOptChilds.equals(optChilds))
                break;

            optChilds = newOptChilds;
        }

        if(optChilds.size() == 0)
            return SqlEmptySolution.get();

        if(optChilds.size() == 1)
            return optChilds.get(0);

        if(!isJoinable(optChilds))
            return SqlNoSolution.get();


        if(optChilds.equals(childs) && restrictions.isOptimized(variables))
            return this;

        return join(request, optChilds, restrictions);
    }


    public static List<SqlIntercode> optimize(Request request, List<SqlIntercode> childs, Restrictions restrictions,
            boolean reduced, boolean evalServices)
    {
        List<SqlIntercode> optimized = new ArrayList<SqlIntercode>(childs.size());

        for(SqlIntercode child : childs)
            optimized.add(child.optimize(request, getRestrictions(child, childs, restrictions), reduced, evalServices));

        return optimized;
    }


    private static List<SqlIntercode> reduceJoin(List<SqlIntercode> childs, Restrictions restrictions,
            DatabaseSchema schema)
    {
        if(childs.stream().anyMatch(c -> c == SqlNoSolution.get()))
            return List.of(SqlNoSolution.get());

        childs = childs.stream().filter(c -> c != SqlEmptySolution.get()).toList();

        if(childs.size() == 0)
            return List.of(SqlEmptySolution.get());

        if(!isJoinable(childs))
            return List.of(SqlNoSolution.get());


        ArrayList<SqlIntercode> optChilds = new ArrayList<SqlIntercode>(childs);


        HashMap<String, List<UsedVariable>> constants = new HashMap<String, List<UsedVariable>>();

        for(SqlIntercode child : childs)
        {
            for(UsedVariable v : child.getVariables().getValues())
            {
                if(v.isConstant())
                {
                    ResourceClass resClass = v.getResourceClass();

                    List<UsedVariable> list = constants.computeIfAbsent(v.getName(),
                            r -> new ArrayList<UsedVariable>());

                    for(UsedVariable old : list)
                    {
                        ResourceClass oldResClass = old.getResourceClass();

                        if(resClass == oldResClass && !old.equals(v))
                            return List.of(SqlNoSolution.get());

                        //TODO: support resource class generalization
                        if(oldResClass.getGeneralClass() != resClass.getGeneralClass())
                            return List.of(SqlNoSolution.get());
                    }

                    list.add(v);
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

                    for(Entry<String, List<UsedVariable>> e : constants.entrySet())
                    {
                        UsedVariable v = access.getInternalVariable(e.getKey());

                        if(v == null || v.isConstant())
                            continue;

                        for(UsedVariable o : e.getValue())
                        {
                            if(v.getMapping(o.getResourceClass()) != null)
                            {
                                cnd.addAreEqual(o.getMapping(), v.getMapping(o.getResourceClass()));
                                skip.put(e.getKey(), o);
                            }
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
                        SqlIntercode merged = SqlTableAccess.tryReduceJoinWithValues(schema, right, left, null);

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
                        Condition additionalLeft = new Condition();
                        Condition additionalRight = new Condition();

                        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left.getVariables(),
                                right.getVariables()))
                        {
                            UsedVariable leftVar = pair.getLeftVariable();
                            UsedVariable rightVar = pair.getRightVariable();

                            if(leftVar == null || rightVar == null)
                                continue;

                            //NOTE: currently, only simple join is taken into the account

                            if(pair.getClasses().size() > 1)
                                continue;

                            if(leftVar.canBeNull() || rightVar.canBeNull())
                                continue;

                            for(PairedClass pairedClass : pair.getClasses())
                            {
                                if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                                    continue;

                                List<Column> leftCols = leftVar.getMapping(pairedClass.getLeftClass());
                                List<Column> rightCols = rightVar.getMapping(pairedClass.getRightClass());

                                for(int c = 0; c < leftCols.size(); c++)
                                {
                                    Column leftCol = leftCols.get(c);
                                    Column rightCol = rightCols.get(c);

                                    if(leftCol instanceof ConstantColumn)
                                        additionalRight.addAreEqual(leftCol, rightCol);

                                    if(rightCol instanceof ConstantColumn)
                                        additionalLeft.addAreEqual(leftCol, rightCol);
                                }
                            }
                        }

                        if(Conditions.and(left.getConditions(), additionalLeft).isFalse()
                                || Conditions.and(right.getConditions(), additionalRight).isFalse())
                            return List.of(SqlNoSolution.get());
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
                        merged = SqlTableAccess.tryReduceJoin(schema, left, right, null);
                    }
                    else if(optChilds.get(j) instanceof SqlDistinct d && d.getChild() instanceof SqlTableAccess right)
                    {
                        merged = tryReduceDistinct(schema, left, right, null);
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


    /*
    private static List<SqlIntercode> improveRecursive(Request request, List<SqlIntercode> childs,
            Restrictions restrictions, DatabaseSchema schema)
    {
        List<SqlIntercode> newChilds = new ArrayList<SqlIntercode>(childs);
    
        for(SqlRecursive recursive : newChilds.stream().filter(c -> c instanceof SqlRecursive)
                .map(c -> (SqlRecursive) c).toList())
        {
            List<SqlIntercode> accesses = newChilds.stream().filter(c -> c instanceof SqlTableAccess).toList();
    
            List<SqlIntercode> newInitChilds = new ArrayList<SqlIntercode>(accesses);
            newInitChilds.add(recursive.init);
    
            SqlIntercode newRecursive = SqlRecursive.create(request, SqlJoin.join(request, newInitChilds, restrictions),
                    recursive.next, recursive.beginName, recursive.joinName, recursive.endVar.getName(),
                    recursive.graphName, restrictions);
    
            newChilds.remove(recursive);
            newChilds.removeAll(accesses);
            newChilds.add(newRecursive);
    
            break; //TODO: process others
        }
    
        return newChilds;
    }
    */


    private static List<SqlIntercode> reduceDistinctUnion(List<SqlIntercode> childs, Restrictions restrictions,
            DatabaseSchema schema)
    {
        List<SqlIntercode> newChilds = new ArrayList<SqlIntercode>(childs);

        loop:
        for(SqlIntercode child : newChilds)
        {
            if(!(child instanceof SqlDistinct distinct && distinct.getChild() instanceof SqlUnion union))
                continue;

            for(SqlIntercode c : newChilds)
            {
                if(!(c instanceof SqlTableAccess candidate) || !canBeDistinctUnionReduced(distinct, candidate))
                    continue;

                for(SqlIntercode d : union.getChilds())
                {
                    if(!(d instanceof SqlTableAccess distinctPart))
                        continue;

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
        }

        return newChilds;
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
            SqlTableAccess distinct, Restrictions restrictions)
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
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
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


    private static Restrictions getRestrictions(SqlIntercode child, List<SqlIntercode> childs,
            Restrictions restrictions)
    {
        List<UsedVariables> others = childs.stream().filter(c -> c != child).map(c -> c.getVariables()).toList();

        return getJoinRestrictions(child.getVariables(), others, restrictions);
    }


    private static List<List<SqlIntercode>> expand(SqlIntercode x)
    {
        if(x instanceof SqlUnion union)
            return expandUnion(union.getChilds());
        else if(x instanceof SqlJoin join)
            return expandJoin(join.getChilds());
        else
            return List.of(List.of(x));
    }


    private static List<List<SqlIntercode>> expandJoin(List<SqlIntercode> childs)
    {
        List<List<SqlIntercode>> result = List.of(List.of());

        for(SqlIntercode child : childs)
        {
            if(child == SqlNoSolution.get())
                return List.of();

            if(child == SqlEmptySolution.get())
                continue;

            ArrayList<List<SqlIntercode>> subresult = new ArrayList<List<SqlIntercode>>();

            for(List<SqlIntercode> e : expand(child))
            {
                for(List<SqlIntercode> r : result)
                {
                    List<SqlIntercode> merged = new ArrayList<SqlIntercode>();
                    merged.addAll(e);
                    merged.addAll(r);

                    if(isJoinable(merged))
                        subresult.add(merged);
                }
            }

            result = subresult;
        }

        return result;
    }


    private static List<List<SqlIntercode>> expandUnion(List<SqlIntercode> childs)
    {
        List<List<SqlIntercode>> result = new ArrayList<List<SqlIntercode>>();

        for(SqlIntercode child : childs)
            if(child != SqlNoSolution.get())
                result.addAll(expand(child));

        return result;
    }


    public final List<SqlIntercode> getChilds()
    {
        return childs;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return childs.stream().anyMatch(c -> c.hasServiceSubpattern());
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("join");

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

        if(!(object instanceof SqlJoin imcode))
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
