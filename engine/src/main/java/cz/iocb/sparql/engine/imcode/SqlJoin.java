package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
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
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Estimator;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindingPair.ResourceClassPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlJoin extends SqlIntercode
{
    private final List<Table> tables;
    private final List<SqlIntercode> childs;
    private final Map<Column, Column> columnMap;


    protected SqlJoin(List<SqlIntercode> childs, List<Table> tables, VariableBindings bindings,
            Map<Column, Column> columnMap)
    {
        super(bindings, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMap = columnMap;
        this.tables = tables;
    }


    public static SqlIntercode join(Request request, SqlIntercode left, SqlIntercode right)
    {
        return join(request, List.of(left, right), null);
    }


    protected static SqlIntercode join(Request request, List<SqlIntercode> childs)
    {
        return join(request, childs, null);
    }


    protected static SqlIntercode join(Request request, List<SqlIntercode> childs, Restrictions restrictions)
    {
        List<Table> tables = IntStream.range(0, childs.size()).mapToObj(i -> new Table("tab" + i)).toList();

        Map<Column, Column> columnMap = new HashMap<>();
        List<VariableBindings> allVars = childs.stream().map(c -> c.getVariableBindings()).toList();
        VariableBindings bindings = getJoinVariableBindings(request, allVars, tables, restrictions, columnMap);

        return new SqlJoin(childs, tables, bindings, columnMap);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        List<SqlIntercode> flatChilds = flatChilds(optimize(request, flatChilds(childs), restrictions, reduced));

        boolean hasService = false;

        if(evalServices)
        {
            List<SqlIntercode> tmp = new ArrayList<>();

            for(SqlIntercode l : flatChilds)
            {
                if(l.hasServiceSubpattern() && !(request.isServiceReorderEnabled() && l instanceof SqlServiceStub))
                    tmp.add(l.optimize(request, getRestrictions(l, flatChilds, restrictions), reduced, true));
                else
                    tmp.add(l);

                if(request.isServiceReorderEnabled() && l instanceof SqlServiceStub)
                    hasService = true;
            }

            flatChilds = flatChilds(tmp);
        }

        List<SqlIntercode> deterministic = new ArrayList<>();
        List<SqlIntercode> nondeterministic = new ArrayList<>();

        for(SqlIntercode child : flatChilds)
        {
            if(child.isDeterministic())
                deterministic.add(child);
            else
                nondeterministic.add(child);
        }


        Restrictions newRestrictions = new Restrictions(restrictions);

        for(SqlIntercode child : nondeterministic)
            for(VariableBinding v : child.getVariableBindings().getValues())
                newRestrictions.add(v.getVariable(), v.getClasses());


        List<SqlIntercode> optChilds = deterministic;

        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        while(true)
        {
            List<SqlIntercode> newChilds = reduceDistinctUnion(
                    reduceJoin(optimize(request, optChilds, newRestrictions, reduced, evalServices), newRestrictions,
                            schema),
                    newRestrictions, schema);

            if(!Objects.equals(new Multiset<>(newChilds), new Multiset<>(optChilds)))
                optChilds = newChilds;

            if(newChilds.size() > 1 && newChilds.stream().anyMatch(c -> c instanceof SqlUnion || c instanceof SqlJoin))
            {
                newChilds = List.of(SqlUnion
                        .union(request,
                                expandJoin(newChilds).stream().map(l -> join(request, l, newRestrictions)).toList())
                        .optimize(request, newRestrictions, reduced, evalServices));

                if(newChilds.size() == 1 && newChilds.get(0) instanceof SqlJoin join)
                    newChilds = join.getChilds();

                if(!Objects.equals(new Multiset<>(newChilds), new Multiset<>(optChilds)))
                    optChilds = newChilds;
            }

            if(optChilds != newChilds)
                break;
        }


        if(evalServices && hasService)
        {
            Estimator estimator = new Estimator(request);

            record Stats(Set<Variable> vars, long count, int lost)
            {
            }

            Map<SqlServiceStub, Stats> stats = new HashMap<>();


            for(SqlIntercode item : nondeterministic)
            {
                if(!(item instanceof SqlServiceStub service))
                    continue;

                Collection<Variable> vars = service.getServiceVariables().values();

                Restrictions serviceRestrictions = new Restrictions(vars);

                SqlIntercode join = join(request, optChilds).optimize(request, serviceRestrictions, false, false);

                Map<Variable, Long> basic = new HashMap<>();

                for(Variable var : vars)
                {
                    if(join.bindings.getVariables().contains(var))
                    {
                        try
                        {
                            basic.put(var, estimator.estimateDistinct(request, join, var));
                        }
                        catch(SQLException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }


                ArrayDeque<Variable> order = new ArrayDeque<>(basic.entrySet().stream()
                        .sorted(Map.Entry.<Variable, Long> comparingByValue(Comparator.reverseOrder()))
                        .map(Map.Entry::getKey).toList());

                Set<Variable> set = new HashSet<>(order);
                long count = estimator.estimateDistinct(request, join, set);

                while(count > 20000l) //TODO: export limit
                {
                    set.remove(order.pop());
                    count = estimator.estimateDistinct(request, join, set);
                }

                stats.put(service, new Stats(set, count, vars.size() - set.size()));
            }

            Entry<SqlServiceStub, Stats> entry = stats
                    .entrySet().stream().sorted(Map.Entry.<SqlServiceStub, Stats> comparingByValue((a,
                            b) -> a.lost == b.lost ? Long.compare(a.count, b.count) : Integer.compare(a.lost, b.lost)))
                    .findFirst().get();

            SqlServiceStub service = entry.getKey();

            Set<Variable> vars = entry.getValue().vars;
            SqlIntercode context = SqlDistinct.create(request, join(request, optChilds), vars).optimize(request,
                    new Restrictions(vars), false, false);

            SqlIntercode evaluated = service.eval(request, context, getRestrictions(service, flatChilds, restrictions));


            nondeterministic.remove(service);

            optChilds = new ArrayList<>(optChilds);
            optChilds.add(evaluated);

            List<SqlIntercode> fullChilds = mergeChilds(request, optChilds, nondeterministic, restrictions, reduced);

            return join(request, fullChilds, restrictions).optimize(request, restrictions, reduced, true);
        }


        List<SqlIntercode> fullChilds = mergeChilds(request, optChilds, nondeterministic, restrictions, reduced);

        if(fullChilds.size() == 0)
            return SqlEmptySolution.get();

        if(fullChilds.size() == 1)
            return fullChilds.get(0);

        if(!isJoinable(fullChilds))
            return SqlNoSolution.get();


        if(fullChilds.equals(childs) && restrictions.isOptimized(bindings))
            return this;

        return join(request, fullChilds, restrictions);
    }


    private static List<SqlIntercode> flatChilds(List<SqlIntercode> input)
    {
        List<SqlIntercode> output = new ArrayList<>();

        Deque<SqlIntercode> stack = new ArrayDeque<>();

        stack.addAll(input);

        while(!stack.isEmpty())
        {
            SqlIntercode child = stack.pop();

            if(child instanceof SqlJoin join)
                stack.addAll(join.childs);
            else
                output.add(child);
        }

        return output;
    }


    private static List<SqlIntercode> mergeChilds(Request request, List<SqlIntercode> deterministic,
            List<SqlIntercode> nondeterministic, Restrictions restrictions, boolean reduced)
    {
        if(nondeterministic.size() == 1 && nondeterministic.get(0) instanceof SqlUnion union)
        {
            List<SqlIntercode> branches = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                List<SqlIntercode> l = new ArrayList<>();
                l.addAll(deterministic);
                l.add(child);

                branches.add(join(request, l, null));
            }

            return List.of(SqlUnion.union(request, branches).optimize(request, restrictions, reduced, false));
        }


        List<SqlIntercode> result = new ArrayList<>();

        result.addAll(deterministic);
        result.addAll(nondeterministic);

        return result;
    }


    public static List<SqlIntercode> optimize(Request request, List<SqlIntercode> childs, Restrictions restrictions,
            boolean reduced, boolean evalServices)
    {
        List<SqlIntercode> optimized = new ArrayList<>(childs.size());

        for(SqlIntercode child : childs)
            optimized.add(child.optimize(request, getRestrictions(child, childs, restrictions), reduced, evalServices));

        return optimized;
    }


    public static List<SqlIntercode> optimize(Request request, List<SqlIntercode> childs, Restrictions restrictions,
            boolean reduced)
    {
        return optimize(request, childs, restrictions, reduced, false);
    }


    private static List<SqlIntercode> reduceJoin(List<SqlIntercode> childs, Restrictions restrictions,
            DatabaseSchema schema)
    {
        if(childs.stream().anyMatch(c -> c.equals(SqlNoSolution.get())))
            return List.of(SqlNoSolution.get());

        childs = childs.stream().filter(c -> !c.equals(SqlEmptySolution.get())).toList();

        if(childs.size() == 0)
            return List.of(SqlEmptySolution.get());

        if(!isJoinable(childs))
            return List.of(SqlNoSolution.get());


        Map<Variable, List<VariableBinding>> constants = new HashMap<>();

        for(SqlIntercode child : childs)
            for(VariableBinding v : child.getVariableBindings().getValues())
                if(v.isConstant()) // children are joinable, so constants should be consistent
                    constants.computeIfAbsent(v.getVariable(), _ -> new ArrayList<>()).add(v);

        List<SqlIntercode> optChilds = new ArrayList<>(childs);

        if(!constants.isEmpty())
        {
            for(int i = 0; i < optChilds.size(); i++)
            {
                SqlIntercode child = optChilds.get(i);

                if(child instanceof SqlTableAccess access)
                {
                    Condition cnd = new Condition();
                    Map<Variable, VariableBinding> skip = new HashMap<>();

                    for(Entry<Variable, List<VariableBinding>> e : constants.entrySet())
                    {
                        VariableBinding v = access.getInternalVariableBinding(e.getKey());

                        if(v == null || v.isConstant())
                            continue;

                        for(VariableBinding o : e.getValue())
                        {
                            if(v.getMapping(o.getClasses().iterator().next()) != null)
                            {
                                ResourceClass r = o.getClasses().iterator().next();

                                cnd.addAreEqual(o.getMapping(r), v.getMapping(r));
                                skip.put(e.getKey(), o);
                            }
                        }
                    }

                    if(!skip.isEmpty())
                    {
                        Conditions cnds = Conditions.and(access.getConditions(), cnd);

                        if(cnds.isFalse())
                            return List.of(SqlNoSolution.get());

                        VariableBindings internal = new VariableBindings();

                        for(Variable variable : access.getVariableBindings().getVariables())
                        {
                            if(skip.containsKey(variable))
                                internal.add(new VariableBinding(skip.get(variable)));
                            else
                                internal.add(access.getInternalVariableBinding(variable));
                        }

                        optChilds.set(i, SqlTableAccess.create(access.getTable(), cnds, internal, access.getReduced()));
                    }
                }
            }
        }


        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlValues values)
            {
                if(values.getSize() > 1)
                {
                    List<SqlIntercode> copyChilds = new ArrayList<>(optChilds);

                    boolean[] mask = new boolean[values.getSize()];

                    for(int j = 0; j < values.getSize(); j++)
                    {
                        SqlIntercode line = values.getSlice(j);
                        copyChilds.set(i, line);

                        mask[j] = isJoinable(copyChilds);
                    }


                    optChilds.set(i, values.strip(mask));
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

                        if(Objects.equals(merged, SqlNoSolution.get()))
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

                        for(VariableBindingPair pair : VariableBindingPair.getPairs(left.getVariableBindings(),
                                right.getVariableBindings()))
                        {
                            VariableBinding leftBinding = pair.getLeftVariableBinding();
                            VariableBinding rightBinding = pair.getRightVariableBinding();

                            //NOTE: currently, only simple join is taken into the account

                            if(pair.getClasses().size() > 1)
                                continue;

                            if(leftBinding.canBeNull() || rightBinding.canBeNull())
                                continue;

                            for(ResourceClassPair pairedClass : pair.getClasses())
                            {
                                if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
                                    continue;

                                List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
                                List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

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

                    if(Objects.equals(merged, SqlNoSolution.get()))
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


    private static List<SqlIntercode> reduceDistinctUnion(List<SqlIntercode> childs, Restrictions restrictions,
            DatabaseSchema schema)
    {
        List<SqlIntercode> newChilds = new ArrayList<>(childs);

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
        Set<Column> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(distinct.getVariableBindings(),
                candidate.getVariableBindings()))
        {
            VariableBinding distinctBinding = pair.getLeftVariableBinding();
            VariableBinding candidateBinding = pair.getRightVariableBinding();

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return false;

            if(distinctBinding.canBeNull() || candidateBinding.canBeNull())
                return false;

            for(ResourceClassPair pairedClass : pair.getClasses())
            {
                if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
                    return false;

                columns.addAll(distinctBinding.getMapping(pairedClass.getLeftClass()));
            }
        }

        return columns.containsAll(distinct.getVariableBindings().getNonConstantColumns());
    }


    private static SqlIntercode tryReduceDistinct(DatabaseSchema schema, SqlTableAccess candidate,
            SqlTableAccess distinct, Restrictions restrictions)
    {
        if(Objects.equals(distinct.getTable(), candidate.getTable()))
        {
            Set<Column> joinColumns = SqlTableAccess.getJoinColumns(distinct, candidate);

            if(!joinColumns.containsAll(distinct.getVariableBindings().getNonConstantColumns()))
                return null;

            return SqlTableAccess.joinByPrimaryKey(candidate, distinct, restrictions);
        }
        else
        {
            if(distinct.hasExpression())
                return null;

            Set<ColumnPair> columns = SqlTableAccess.getJoinColumnPairs(distinct, candidate);

            Set<Column> parentColumns = new HashSet<>();
            parentColumns.addAll(distinct.getConditions().getNonConstantColumns());
            parentColumns.addAll(distinct.getInternalVariableBindings().getNonConstantColumns());

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
        Set<Column> columns = bindings.getNonConstantColumns();

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

        String condition = generateJoinCondition(childs.stream().map(c -> c.getVariableBindings()).toList(), tables);

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
        List<VariableBindings> others = childs.stream().filter(c -> c != child).map(c -> c.getVariableBindings())
                .toList();

        return getJoinRestrictions(child.getVariableBindings(), others, restrictions);
    }


    private static List<List<SqlIntercode>> expand(SqlIntercode child)
    {
        if(child instanceof SqlUnion union)
            return expandUnion(union.getChilds());
        else if(child instanceof SqlJoin join)
            return expandJoin(join.getChilds());
        else
            return List.of(List.of(child));
    }


    private static List<List<SqlIntercode>> expandJoin(List<SqlIntercode> childs)
    {
        List<List<SqlIntercode>> result = List.of(List.of());

        for(SqlIntercode child : childs)
        {
            if(child.equals(SqlNoSolution.get()))
                return List.of();

            if(child.equals(SqlEmptySolution.get()))
                continue;

            List<List<SqlIntercode>> subresult = new ArrayList<>();

            for(List<SqlIntercode> e : expand(child))
            {
                for(List<SqlIntercode> r : result)
                {
                    List<SqlIntercode> merged = new ArrayList<>();
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
        List<List<SqlIntercode>> result = new ArrayList<>();

        for(SqlIntercode child : childs)
            if(!child.equals(SqlNoSolution.get()))
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
