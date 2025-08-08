package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedPairedVariable.PairedClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public abstract class SqlIntercode extends SqlBaseClass
{
    public static class Restrictions
    {
        private static final Set<ResourceClass> all = new HashSet<ResourceClass>();

        private final Map<String, Set<ResourceClass>> map = new HashMap<String, Set<ResourceClass>>();

        public Restrictions()
        {
        }

        public Restrictions(Restrictions restrictions)
        {
            map.putAll(restrictions.map);
        }

        public Restrictions(Collection<String> vars)
        {
            for(String var : vars)
                map.put(var, all);
        }

        public Restrictions(Restrictions a, Restrictions b)
        {
            Set<String> vars = new HashSet<String>();
            vars.addAll(a.getNames());
            vars.addAll(b.getNames());

            for(String v : vars)
            {
                Set<ResourceClass> sa = a.map.get(v);
                Set<ResourceClass> sb = b.map.get(v);

                if(sa == all || sb == all)
                {
                    map.put(v, all);
                }
                else if(sa == null)
                {
                    map.put(v, sb);
                }
                else if(sb == null)
                {
                    map.put(v, sa);
                }
                else
                {
                    Set<ResourceClass> sc = new HashSet<ResourceClass>();
                    sc.addAll(sa);
                    sc.addAll(sa);
                    map.put(v, sc);
                }
            }
        }

        public void add(String var)
        {
            map.put(var, all);
        }

        public void add(Collection<String> vars)
        {
            for(String var : vars)
                map.put(var, all);
        }

        public Set<String> getNames()
        {
            return map.keySet();
        }

        public boolean contains(String var, ResourceClass resClass)
        {
            if(!map.containsKey(var))
                return false;

            if(map.get(var) == all)
                return true;

            for(ResourceClass r : map.get(var))
                if(resClass == r || resClass == r.getGeneralClass() || resClass.getGeneralClass() == r)
                    return true;

            return false;
        }

        public boolean canBeOptimized(UsedVariables vars)
        {
            UsedVariables opt = vars.restrict(this);

            for(UsedVariable var : vars.getValues())
            {
                UsedVariable o = opt.get(var.getName());

                if(o == null)
                    return true;

                if(!o.getMappings().equals(var.getMappings()))
                    return true;
            }

            return false;
        }


        public void add(String var, Set<ResourceClass> set)
        {
            if(map.get(var) == all)
                return;

            if(map.containsKey(var))
                map.get(var).addAll(set);
            else
                map.put(var, set);
        }


        public void set(String name, Set<ResourceClass> set)
        {
            map.put(name, set);
        }

        public boolean contains(String var, Set<ResourceClass> classes)
        {
            for(ResourceClass c : classes)
                if(contains(var, c))
                    return true;

            return false;
        }

        public void add(Restrictions restrictions)
        {
            for(String v : restrictions.getNames())
            {
                Set<ResourceClass> sa = map.get(v);
                Set<ResourceClass> sb = restrictions.map.get(v);

                if(sb == all || sa == null)
                {
                    map.put(v, sb);
                }
                else if(sa != all && sb != null)
                {
                    Set<ResourceClass> sc = new HashSet<ResourceClass>();
                    sc.addAll(sa);
                    sc.addAll(sa);
                    map.put(v, sb);
                }
            }
        }

        public boolean containsVar(String var)
        {
            return map.containsKey(var);
        }

        @Override
        public boolean equals(Object other)
        {
            if(other == null)
                return false;

            if(other.getClass() != getClass())
                return false;

            return map.equals(((Restrictions) other).map);
        }

        public boolean isOptimized(UsedVariables variables)
        {
            return variables.restrict(this).equals(variables);
        }

        public Set<ResourceClass> get(String key)
        {
            Set<ResourceClass> restriction = map.get(key);

            return restriction == all ? null : restriction;
        }
    }


    protected final UsedVariables variables;
    protected final boolean isDeterministic;


    protected SqlIntercode(UsedVariables variables, boolean isDeterministic)
    {
        this.variables = variables;
        this.isDeterministic = isDeterministic;
    }


    public abstract SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced,
            boolean evalServices);


    public abstract String translate(Request request);


    public abstract boolean hasServiceSubpattern();


    public final UsedVariables getVariables()
    {
        return variables;
    }


    public final UsedVariable getVariable(String variable)
    {
        return variables.get(variable);
    }


    public final Map<ResourceClass, List<Column>> getMappings(String variable)
    {
        return variables.get(variable).getMappings();
    }


    public final List<Column> getMapping(String variable, ResourceClass resClass)
    {
        return variables.get(variable).getMapping(resClass);
    }


    public final List<Column> getMapping(String variable)
    {
        if(variables.get(variable) == null)
            return null;

        return variables.get(variable).getMapping();
    }


    public final boolean hasConstantVariable(String variable)
    {
        UsedVariable var = variables.get(variable);

        return var == null || var.isConstant();
    }


    public final boolean hasConstantVariables(Set<String> variables)
    {
        return variables.stream().allMatch(v -> hasConstantVariable(v));
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    protected static UsedVariables getJoinUsedVariables(Request request, List<UsedVariables> allVars,
            List<Table> tables, Restrictions restrictions, Map<Column, Column> map)
    {
        Map<Column, Column> columnMap = new HashMap<Column, Column>();

        UsedVariables variables = new UsedVariables();

        for(String name : allVars.stream().flatMap(v -> v.getNames().stream()).collect(toSet()))
        {
            List<UsedVariable> vars = allVars.stream().map(v -> v.get(name)).toList();
            List<UsedVariable> defs = vars.stream().filter(v -> v != null).toList();

            if(defs.stream().anyMatch(v -> !v.canBeNull()))
            {
                defs = defs.stream().filter(v -> !v.canBeNull()).toList();
                Set<ResourceClass> resClasses = selectSharedClasses(cleanGeneralClasses(collectClasses(defs)), defs);

                if(resClasses.isEmpty())
                    return new UsedVariables();

                UsedVariable var = createUsedVariable(request, name, resClasses, vars, tables, columnMap, false);

                if(var == null)
                    return new UsedVariables();

                variables.add(var);
            }
            else
            {
                Set<ResourceClass> resClasses = cleanSpecificClasses(collectClasses(defs));
                UsedVariable var = createUsedVariable(request, name, resClasses, vars, tables, columnMap, true);

                variables.add(var);
            }
        }

        columnMap.entrySet().forEach(e -> map.put(e.getValue(), e.getKey()));

        return variables.restrict(restrictions);
    }


    private static UsedVariable createUsedVariable(Request request, String name, Set<ResourceClass> resClasses,
            List<UsedVariable> vars, List<Table> tables, Map<Column, Column> columnMap, boolean canBeNull)
    {
        UsedVariable variable = new UsedVariable(name, canBeNull);

        if(!canBeNull)
        {
            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> mappings = new ArrayList<>();

                for(int i = 0; i < vars.size(); i++)
                {
                    UsedVariable var = vars.get(i);

                    if(var == null || var.canBeNull() || !var.containsClass(resClass))
                        continue;

                    mappings.add(toTableColumns(tables.get(i), var.getMapping(resClass)));
                }

                if(mappings.stream().anyMatch(m -> m == null))
                {
                    assert mappings.stream().allMatch(m -> m == null);
                    variable.addMapping(resClass, null);
                }
                else
                {
                    if(IntStream.range(0, resClass.getColumnCount()).anyMatch(i -> mappings.stream().map(m -> m.get(i))
                            .filter(c -> c instanceof ConstantColumn).distinct().count() > 1))
                        return null;

                    List<Column> columns = selectColumns(resClass, mappings);

                    variable.addMapping(resClass,
                            getMappedColuns(resClass.createColumns(request.getColumnMap(), name), columns, columnMap));
                }
            }
        }
        else
        {
            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> variants = new ArrayList<>();

                for(int i = 0; i < vars.size(); i++)
                {
                    UsedVariable var = vars.get(i);
                    Table table = tables.get(i);

                    if(var == null)
                        continue;

                    for(ResourceClass specClass : var.getCompatibleClasses(resClass))
                    {
                        List<Column> columns = toTableColumns(table, var.getMapping(specClass));
                        variants.add(resClass == specClass ? columns : specClass.toGeneralClass(columns, true));
                    }
                }

                List<Column> columns = coalesceVariants(resClass.getColumnCount(), variants);

                variable.addMapping(resClass,
                        getMappedColuns(resClass.createColumns(request.getColumnMap(), name), columns, columnMap));
            }
        }

        return variable;
    }


    private static List<Column> selectColumns(ResourceClass resClass, List<List<Column>> mappings)
    {
        return IntStream
                .range(0, resClass.getColumnCount()).mapToObj(i -> mappings.stream().map(m -> m.get(i))
                        .filter(c -> c instanceof ConstantColumn).findFirst().orElse(mappings.getFirst().get(i)))
                .toList();
    }


    private static List<Column> getMappedColuns(List<Column> output, List<Column> input, Map<Column, Column> map)
    {
        List<Column> mapping = new ArrayList<>();

        for(int i = 0; i < output.size(); i++)
        {
            Column access = input.get(i);

            if(access instanceof ConstantColumn)
            {
                mapping.add(access);
            }
            else
            {
                Column column = map.get(access);

                if(column == null)
                {
                    column = output.get(i);
                    map.put(access, column);
                }

                mapping.add(column);
            }
        }

        return mapping;
    }


    private static List<Column> coalesceVariants(int cols, List<List<Column>> variants)
    {
        if(variants.size() == 1)
            return variants.getFirst();

        return IntStream
                .range(0, cols).mapToObj(i -> (Column) new ExpressionColumn(variants.stream()
                        .map(l -> l.get(i).toString()).distinct().sorted().collect(joining(", ", "coalesce(", ")"))))
                .toList();
    }


    private static Set<ResourceClass> selectSharedClasses(Set<ResourceClass> classes, List<UsedVariable> variables)
    {
        return classes.stream().filter(
                c -> variables.stream().allMatch(v -> v.containsClass(c) || v.containsClass(c.getGeneralClass())))
                .collect(toSet());
    }


    protected static Set<ResourceClass> cleanSpecificClasses(Set<ResourceClass> classes)
    {
        return classes.stream().filter(r -> r == r.getGeneralClass() || !classes.contains(r.getGeneralClass()))
                .collect(toSet());
    }


    protected static Set<ResourceClass> cleanGeneralClasses(Set<ResourceClass> classes)
    {
        return classes.stream().filter(r -> classes.stream().noneMatch(x -> x != r && x.getGeneralClass() == r))
                .collect(toSet());
    }


    private static Set<ResourceClass> collectClasses(List<UsedVariable> variables)
    {
        return variables.stream().flatMap(v -> v.getClasses().stream()).collect(toSet());
    }


    private static List<Column> toTableColumns(Table table, List<Column> columns)
    {
        if(columns == null)
            return null;

        return columns.stream().map(c -> c.fromTable(table)).toList();
    }


    protected static UsedVariables getJoinUsedVariables(Request request, UsedVariables left, UsedVariables right,
            Table leftTable, Table rightTable, Restrictions restrictions, Map<Column, Column> map)
    {
        return getJoinUsedVariables(request, Arrays.asList(left, right), Arrays.asList(leftTable, rightTable),
                restrictions, map);
    }


    public static boolean isJoinConditionAlwaysTrue(UsedVariables left, UsedVariables right)
    {
        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left, right))
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                if(leftVariable.canBeNull())
                    return false;

                if(rightVariable.canBeNull())
                    return false;

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                        return false;

                    ResourceClass resClass = pairedClass.getLeftClass();
                    List<Column> leftCols = leftVariable.getMapping(resClass);
                    List<Column> rightCols = rightVariable.getMapping(resClass);

                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        Column leftCol = leftCols.get(i);
                        Column rightCol = rightCols.get(i);

                        if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn)
                                || !leftCol.equals(rightCol))
                            return false;
                    }
                }
            }
        }

        return true;
    }


    public static String generateJoinCondition(List<UsedVariables> vars, List<Table> tables)
    {
        int size = vars.size();

        List<String> conditions = IntStream.range(0, size)
                .mapToObj(i -> IntStream.range(i + 1, size)
                        .mapToObj(j -> generateJoinCondition(vars.get(i), vars.get(j), tables.get(i), tables.get(j))))
                .flatMap(c -> c).filter(c -> c != null).toList();

        if(conditions.isEmpty())
            return null;

        return conditions.stream().collect(joining(" AND ", "(", ")"));
    }


    public static String generateJoinCondition(UsedVariables left, UsedVariables right, Table leftTable,
            Table rightTable)
    {
        List<String> join = new ArrayList<String>();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left, right))
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                List<String> condition = new ArrayList<String>();

                if(leftVariable.canBeNull())
                    condition.add(leftVariable.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(leftTable) + " IS NULL").sorted().collect(joining(" AND ")));

                if(rightVariable.canBeNull())
                    condition.add(rightVariable.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(rightTable) + " IS NULL").sorted().collect(joining(" AND ")));

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
                    {
                        List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass()).stream()
                                .map(c -> c.fromTable(leftTable)).toList();
                        List<Column> rightCols = rightVariable.getMapping(pairedClass.getRightClass()).stream()
                                .map(c -> c.fromTable(rightTable)).toList();

                        Set<String> compare = new HashSet<String>();

                        if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                        {
                            ResourceClass resClass = pairedClass.getLeftClass();

                            for(int i = 0; i < resClass.getColumnCount(); i++)
                            {
                                Column leftCol = leftCols.get(i);
                                Column rightCol = rightCols.get(i);

                                if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                                    compare.add(leftCol + " = " + rightCol);
                                else if(!leftCol.equals(rightCol))
                                    compare.add("false");
                            }
                        }
                        else
                        {
                            ResourceClass leftClass = pairedClass.getLeftClass();
                            ResourceClass rightClass = pairedClass.getRightClass();

                            List<Column> genLeftCols = leftClass.toGeneralClass(leftCols, false);
                            List<Column> genRightCols = rightClass.toGeneralClass(rightCols, false);

                            for(int i = 0; i < leftClass.getGeneralClass().getColumnCount(); i++)
                                compare.add(genLeftCols.get(i) + " = " + genRightCols.get(i));
                        }

                        if(compare.isEmpty())
                            condition.add("true");

                        if(!compare.contains("false"))
                            condition.add(compare.stream().sorted().collect(joining(" AND ")));
                    }
                }


                if(condition.isEmpty())
                    condition.add("false");

                if(!condition.contains("true"))
                    join.add(condition.stream().sorted().collect(joining(" OR ", "(", ")")));
            }
        }

        if(join.isEmpty())
            return null;

        return join.stream().sorted().collect(joining(" AND ", "(", ")"));
    }


    public static String generateJoinCondition(UsedVariable leftVariable, UsedVariable rightVariable, Table leftTable,
            Table rightTable)
    {
        if(leftVariable == null || rightVariable == null)
            return null;

        List<String> condition = new ArrayList<String>();

        if(leftVariable.canBeNull())
            condition.add(leftVariable.getNonConstantColumns().stream().map(c -> c.fromTable(leftTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        if(rightVariable.canBeNull())
            condition.add(rightVariable.getNonConstantColumns().stream().map(c -> c.fromTable(rightTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        for(PairedClass pairedClass : (new UsedPairedVariable(leftVariable, rightVariable)).getClasses())
        {
            if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
            {
                List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass()).stream()
                        .map(c -> c.fromTable(leftTable)).toList();
                List<Column> rightCols = rightVariable.getMapping(pairedClass.getRightClass()).stream()
                        .map(c -> c.fromTable(rightTable)).toList();

                Set<String> compare = new HashSet<String>();

                if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                {
                    ResourceClass resClass = pairedClass.getLeftClass();

                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        Column leftCol = leftCols.get(i);
                        Column rightCol = rightCols.get(i);

                        if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                            compare.add(leftCol + " = " + rightCol);
                        else if(!leftCol.equals(rightCol))
                            compare.add("false");
                    }
                }
                else
                {
                    ResourceClass leftClass = pairedClass.getLeftClass();
                    ResourceClass rightClass = pairedClass.getRightClass();

                    List<Column> genLeftCols = leftClass.toGeneralClass(leftCols, false);
                    List<Column> genRightCols = rightClass.toGeneralClass(rightCols, false);

                    for(int i = 0; i < leftClass.getGeneralClass().getColumnCount(); i++)
                        compare.add(genLeftCols.get(i) + " = " + genRightCols.get(i));
                }

                if(compare.isEmpty())
                    condition.add("true");

                if(!compare.contains("false"))
                    condition.add(compare.stream().sorted().collect(joining(" AND ")));
            }
        }

        assert !condition.isEmpty();

        if(condition.contains("true"))
            return null;

        return condition.stream().sorted().collect(joining(" OR ", "(", ")"));
    }


    public boolean isDistinct(Request request, Collection<String> selected)
    {
        return false;
    }


    public static Restrictions getJoinRestrictions(UsedVariables variables, UsedVariables other,
            Restrictions restrictions)
    {
        return getJoinRestrictions(variables, Set.of(other), restrictions);
    }


    protected static Restrictions getJoinRestrictions(UsedVariables variables, Collection<UsedVariables> others,
            Restrictions restrictions)
    {
        Restrictions joinRestrictions = new Restrictions();

        for(UsedVariable var : variables.getValues())
        {
            String name = var.getName();

            Set<UsedVariable> vars = others.stream().map(v -> v.get(name)).filter(v -> v != null).collect(toSet());

            if(!vars.isEmpty())
            {
                if(var.canBeNull())
                {
                    joinRestrictions.set(name, var.getMappings().keySet());
                }
                else
                {
                    Set<ResourceClass> set = new HashSet<ResourceClass>();

                    for(ResourceClass rc : var.getMappings().keySet())
                        if(vars.stream().anyMatch(v -> v.containsClass(rc) || v.containsClass(rc.getGeneralClass())))
                            set.add(rc);

                    joinRestrictions.set(name, set);
                }
            }
        }

        return new Restrictions(joinRestrictions, restrictions);
    }


    protected static boolean isJoinable(List<SqlIntercode> childs)
    {
        List<UsedVariables> allVars = childs.stream().map(c -> c.getVariables()).toList();

        for(String name : allVars.stream().flatMap(v -> v.getNames().stream()).collect(toSet()))
        {
            List<UsedVariable> vars = allVars.stream().map(v -> v.get(name)).filter(v -> v != null).toList();

            if(vars.size() > 1 && vars.stream().anyMatch(v -> !v.canBeNull()))
            {
                vars = vars.stream().filter(v -> !v.canBeNull()).toList();
                Set<ResourceClass> resClasses = selectSharedClasses(cleanGeneralClasses(collectClasses(vars)), vars);

                if(resClasses.isEmpty())
                    return false;

                for(ResourceClass resClass : resClasses)
                {
                    Map<Integer, ConstantColumn> consts = new HashMap<Integer, ConstantColumn>();

                    for(UsedVariable var : vars)
                    {
                        if(var.canBeNull() || !var.containsClass(resClass))
                            continue;

                        List<Column> cols = var.getMapping(resClass);

                        for(int j = 0; j < resClass.getColumnCount(); j++)
                            if(cols.get(j) instanceof ConstantColumn c && !consts.computeIfAbsent(j, k -> c).equals(c))
                                return false;
                    }
                }
            }
        }

        return true;
    }


    protected static boolean isJoinable(SqlIntercode left, SqlIntercode right)
    {
        for(SqlIntercode l : getJoinList(left))
        {
            for(SqlIntercode r : getJoinList(right))
            {
                ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(l.getVariables(), r.getVariables());

                if(pairs.stream().anyMatch(p -> !p.isJoinable()))
                    return false;
            }
        }

        return true;
    }


    protected static List<SqlIntercode> getJoinList(SqlIntercode child)
    {
        if(child instanceof SqlJoin join)
            return join.getChilds().stream().flatMap(c -> getJoinList(c).stream()).toList();

        if(child instanceof SqlFilter filter)
            return getJoinList(filter.getChild());

        if(child instanceof SqlMinus minus)
            return getJoinList(minus.getLeft());

        if(child instanceof SqlLeftJoin join)
            return getJoinList(join.getLeft());

        if(child instanceof SqlDistinct distinct)
            return getJoinList(distinct.getChild());

        return List.of(child);
    }
}
