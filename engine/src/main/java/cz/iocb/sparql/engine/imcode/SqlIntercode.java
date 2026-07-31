package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getDisjunctClasses;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getIntersectionClass;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindingPair.ResourceClassPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



public abstract class SqlIntercode extends SqlBaseClass
{
    public static class Restrictions
    {
        private final Map<Variable, Set<ResourceClass>> map = new HashMap<>();

        public Restrictions()
        {
        }

        public Restrictions(Restrictions restrictions)
        {
            map.putAll(restrictions.map);
        }

        public Restrictions(Collection<Variable> vars)
        {
            for(Variable var : vars)
                map.put(var, Set.of(box));
        }

        public Restrictions(Restrictions a, Restrictions b)
        {
            Set<Variable> vars = new HashSet<>();
            vars.addAll(a.getNames());
            vars.addAll(b.getNames());

            for(Variable v : vars)
            {
                Set<ResourceClass> sa = a.map.get(v);
                Set<ResourceClass> sb = b.map.get(v);

                if(sa == null)
                {
                    map.put(v, sb);
                }
                else if(sb == null)
                {
                    map.put(v, sa);
                }
                else
                {
                    Set<ResourceClass> sc = new HashSet<>();
                    sc.addAll(sa);
                    sc.addAll(sb);
                    map.put(v, sc);
                }
            }
        }

        public void add(Variable var)
        {
            map.put(var, Set.of(box));
        }

        public void add(Collection<Variable> vars)
        {
            for(Variable var : vars)
                map.put(var, Set.of(box));
        }

        public Set<Variable> getNames()
        {
            return map.keySet();
        }

        public boolean contains(Variable var, ResourceClass resClass)
        {
            if(!map.containsKey(var))
                return false;

            for(ResourceClass r : map.get(var))
                if(!ResourceClass.areDisjunct(r, resClass))
                    return true;

            return false;
        }

        public boolean canBeOptimized(VariableBindings bindings)
        {
            VariableBindings opt = bindings.restrict(this);

            for(VariableBinding binding : bindings.getValues())
            {
                VariableBinding o = opt.get(binding.getVariable());

                if(o == null)
                    return true;

                if(!o.getMappings().equals(binding.getMappings()))
                    return true;
            }

            return false;
        }


        public void add(Variable var, Set<ResourceClass> set)
        {
            map.computeIfAbsent(var, _ -> new HashSet<>()).addAll(set);
        }


        public void add(Variable var, ResourceClass resClass)
        {
            map.computeIfAbsent(var, _ -> new HashSet<>()).add(resClass);
        }


        public void set(Variable variable, Set<ResourceClass> set)
        {
            map.put(variable, set);
        }

        public boolean contains(Variable var, Set<ResourceClass> classes)
        {
            for(ResourceClass c : classes)
                if(contains(var, c))
                    return true;

            return false;
        }

        public void add(Restrictions restrictions)
        {
            for(Variable v : restrictions.getNames())
            {
                Set<ResourceClass> sa = map.get(v);
                Set<ResourceClass> sb = restrictions.map.get(v);

                if(sa == null)
                {
                    map.put(v, sb);
                }
                else if(sb != null)
                {
                    Set<ResourceClass> sc = new HashSet<>();
                    sc.addAll(sa);
                    sc.addAll(sb);
                    map.put(v, sc);
                }
            }
        }

        public boolean containsVar(Variable var)
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

        public boolean isOptimized(VariableBindings bindings)
        {
            return bindings.restrict(this).equals(bindings);
        }

        public Set<ResourceClass> get(Variable var)
        {
            return map.get(var);
        }

        public Restriction getRestriction(Variable var)
        {
            return new Restriction(get(var));
        }
    }


    protected final VariableBindings bindings;
    protected final boolean isDeterministic;


    protected SqlIntercode(VariableBindings bindings, boolean isDeterministic)
    {
        this.bindings = bindings;
        this.isDeterministic = isDeterministic;
    }


    public abstract SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced,
            boolean evalServices);


    public abstract String translate(Request request);


    public abstract boolean hasServiceSubpattern();


    public abstract void generateExplanation(StringBuilder builder, String indent);


    public String getExplanation()
    {
        StringBuilder builder = new StringBuilder();
        generateExplanation(builder, "\n");
        return builder.toString();
    }


    public final VariableBindings getVariableBindings()
    {
        return bindings;
    }


    public final VariableBinding getVariable(Variable variable)
    {
        return bindings.get(variable);
    }


    public final Map<ResourceClass, List<Column>> getMappings(Variable variable)
    {
        return bindings.get(variable).getMappings();
    }


    public final List<Column> getMapping(Variable variable, ResourceClass resClass)
    {
        return bindings.get(variable).getMapping(resClass);
    }


    public final boolean hasConstantVariable(Variable variable)
    {
        VariableBinding binding = bindings.get(variable);

        return binding == null || binding.isConstant();
    }


    public final boolean hasConstantVariables(Set<Variable> variables)
    {
        return variables.stream().allMatch(v -> hasConstantVariable(v));
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    private static Set<ResourceClass> joinResourceClasses(List<VariableBinding> defs)
    {
        List<Set<ResourceClass>> result = List.of(Set.of());

        for(VariableBinding def : defs)
        {
            List<Set<ResourceClass>> nextResult = new ArrayList<>();

            for(ResourceClass resClass : def.getClasses())
                for(Set<ResourceClass> set : result)
                    if(set.stream().allMatch(c -> !ResourceClass.areDisjunct(c, resClass)))
                        nextResult.add(Stream.concat(set.stream(), Stream.of(resClass)).collect(toSet()));

            result = nextResult;
        }

        return getDisjunctClasses(result.stream().map(s -> getIntersectionClass(s)).collect(toSet()));
    }


    protected static VariableBindings getJoinVariableBindings(Request request, List<VariableBindings> allVars,
            List<Table> tables, Restrictions restrictions, Map<Column, Column> map)
    {
        Map<Column, Column> columnMap = new HashMap<>();

        VariableBindings bindings = new VariableBindings();

        for(Variable variable : allVars.stream().flatMap(v -> v.getVariables().stream()).collect(toSet()))
        {
            List<VariableBinding> vars = allVars.stream().map(v -> v.get(variable)).toList();
            List<VariableBinding> defs = vars.stream().filter(v -> v != null).toList();

            if(defs.stream().anyMatch(v -> !v.canBeNull()))
            {
                defs = defs.stream().filter(v -> !v.canBeNull()).toList();
                Set<ResourceClass> resClasses = joinResourceClasses(defs);

                if(resClasses.isEmpty())
                    return new VariableBindings();

                VariableBinding binding = createVariableBinding(request, variable, resClasses, vars, tables, columnMap,
                        false);

                if(binding == null)
                    return new VariableBindings();

                bindings.add(binding);
            }
            else
            {
                Set<ResourceClass> resClasses = ResourceClass.getDisjunctClasses(collectClasses(defs));
                VariableBinding binding = createVariableBinding(request, variable, resClasses, vars, tables, columnMap,
                        true);

                bindings.add(binding);
            }
        }

        columnMap.entrySet().forEach(e -> map.put(e.getValue(), e.getKey()));

        return bindings.restrict(restrictions);
    }


    private static VariableBinding createVariableBinding(Request request, Variable variable,
            Set<ResourceClass> resClasses, List<VariableBinding> vars, List<Table> tables,
            Map<Column, Column> columnMap, boolean canBeNull)
    {
        VariableBinding variableBinding = new VariableBinding(variable, canBeNull);

        if(!canBeNull)
        {
            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> mappings = new ArrayList<>();

                for(int i = 0; i < vars.size(); i++)
                {
                    VariableBinding binding = vars.get(i);

                    if(binding == null || binding.canBeNull() || !binding.containsClass(resClass))
                        continue;

                    mappings.add(toTableColumns(tables.get(i), binding.getMapping(resClass)));
                }

                if(mappings.stream().anyMatch(m -> m == null))
                {
                    assert mappings.stream().allMatch(m -> m == null);
                    variableBinding.addMapping(resClass, null);
                }
                else
                {
                    if(IntStream.range(0, resClass.getColumnCount()).anyMatch(i -> mappings.stream().map(m -> m.get(i))
                            .filter(c -> c instanceof ConstantColumn).distinct().count() > 1))
                        return null;

                    List<Column> columns = selectColumns(resClass, mappings);

                    variableBinding.addMapping(resClass, getMappedColuns(
                            resClass.createColumns(request.getColumnMap(), variable), columns, columnMap));
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
                    VariableBinding binding = vars.get(i);
                    Table table = tables.get(i);

                    if(binding == null)
                        continue;

                    for(ResourceClass specClass : binding.getCompatibleClasses(resClass))
                    {
                        List<Column> columns = toTableColumns(table, binding.getMapping(specClass));
                        variants.add(specClass.toGeneralClass(resClass, columns, true));
                    }
                }

                List<Column> columns = coalesceVariants(resClass.getColumnCount(), variants);

                variableBinding.addMapping(resClass,
                        getMappedColuns(resClass.createColumns(request.getColumnMap(), variable), columns, columnMap));
            }
        }

        return variableBinding;
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


    private static Set<ResourceClass> collectClasses(List<VariableBinding> variables)
    {
        return variables.stream().flatMap(v -> v.getClasses().stream()).collect(toSet());
    }


    private static List<Column> toTableColumns(Table table, List<Column> columns)
    {
        if(columns == null)
            return null;

        return columns.stream().map(c -> c.fromTable(table)).toList();
    }


    protected static VariableBindings getJoinVariableBindings(Request request, VariableBindings left,
            VariableBindings right, Table leftTable, Table rightTable, Restrictions restrictions,
            Map<Column, Column> map)
    {
        return getJoinVariableBindings(request, Arrays.asList(left, right), Arrays.asList(leftTable, rightTable),
                restrictions, map);
    }


    public static boolean isJoinConditionAlwaysTrue(VariableBindings left, VariableBindings right)
    {
        for(VariableBindingPair pair : VariableBindingPair.getPairs(left, right))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            if(leftBinding.canBeNull())
                return false;

            if(rightBinding.canBeNull())
                return false;

            for(ResourceClassPair pairedClass : pair.getClasses())
            {
                if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
                    return false;

                ResourceClass resClass = pairedClass.getLeftClass();
                List<Column> leftCols = leftBinding.getMapping(resClass);
                List<Column> rightCols = rightBinding.getMapping(resClass);

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

        return true;
    }


    public static String generateJoinCondition(List<VariableBindings> vars, List<Table> tables)
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


    public static String generateJoinCondition(VariableBindings left, VariableBindings right, Table leftTable,
            Table rightTable)
    {
        Set<String> join = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(left, right))
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            Set<String> condition = new HashSet<>();

            if(leftBinding.canBeNull())
            {
                Set<Column> cols = leftBinding.getNonConstantColumns();

                if(cols.isEmpty())
                    condition.add("true");
                else
                    condition.add(leftBinding.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(leftTable) + " IS NULL").sorted().collect(joining(" AND ")));
            }

            if(rightBinding.canBeNull())
            {
                Set<Column> cols = rightBinding.getNonConstantColumns();

                if(cols.isEmpty())
                    condition.add("true");
                else
                    condition.add(rightBinding.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(rightTable) + " IS NULL").sorted().collect(joining(" AND ")));
            }

            for(ResourceClassPair pairedClass : pair.getClasses())
            {
                ResourceClass leftClass = pairedClass.getLeftClass();
                ResourceClass rightClass = pairedClass.getRightClass();

                if(leftClass == null || rightClass == null)
                    continue;

                ResourceClass unionClass = ResourceClass.getUnionClass(leftClass, rightClass);

                List<Column> leftCols = toTableColumns(leftTable, leftBinding.getMapping(leftClass));
                List<Column> rightCols = toTableColumns(rightTable, rightBinding.getMapping(rightClass));

                List<Column> genLeftCols = leftClass.toGeneralClass(unionClass, leftCols, false);
                List<Column> genRightCols = rightClass.toGeneralClass(unionClass, rightCols, false);

                Set<String> compare = new HashSet<>();

                for(int i = 0; i < unionClass.getColumnCount(); i++)
                {
                    Column leftCol = genLeftCols.get(i);
                    Column rightCol = genRightCols.get(i);

                    if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                        compare.add(leftCol + " = " + rightCol);
                    else if(!leftCol.equals(rightCol))
                        compare.add("false");
                }

                if(compare.isEmpty())
                    compare.add("true");

                if(!compare.contains("false"))
                    condition.add(compare.stream().sorted().collect(joining(" AND ")));
            }

            if(condition.isEmpty())
                condition.add("false");

            if(!condition.contains("true"))
                join.add(condition.stream().sorted().collect(joining(" OR ", "(", ")")));
        }

        if(join.isEmpty())
            return null;

        return join.stream().sorted().collect(joining(" AND ", "(", ")"));
    }


    public static String generateJoinCondition(VariableBinding leftBinding, VariableBinding rightBinding,
            Table leftTable, Table rightTable)
    {
        if(leftBinding == null || rightBinding == null)
            return null;

        List<String> condition = new ArrayList<>();

        if(leftBinding.canBeNull())
            condition.add(leftBinding.getNonConstantColumns().stream().map(c -> c.fromTable(leftTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        if(rightBinding.canBeNull())
            condition.add(rightBinding.getNonConstantColumns().stream().map(c -> c.fromTable(rightTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        for(ResourceClassPair pairedClass : (new VariableBindingPair(leftBinding, rightBinding)).getClasses())
        {
            ResourceClass leftClass = pairedClass.getLeftClass();
            ResourceClass rightClass = pairedClass.getRightClass();

            if(leftClass == null || rightClass == null)
                continue;

            ResourceClass unionClass = ResourceClass.getUnionClass(leftClass, rightClass);

            List<Column> leftCols = toTableColumns(leftTable, leftBinding.getMapping(leftClass));
            List<Column> rightCols = toTableColumns(rightTable, rightBinding.getMapping(rightClass));

            List<Column> genLeftCols = leftClass.toGeneralClass(unionClass, leftCols, false);
            List<Column> genRightCols = rightClass.toGeneralClass(unionClass, rightCols, false);

            Set<String> compare = new HashSet<>();

            for(int i = 0; i < unionClass.getColumnCount(); i++)
            {
                Column leftCol = genLeftCols.get(i);
                Column rightCol = genRightCols.get(i);

                if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                    compare.add(leftCol + " = " + rightCol);
                else if(!leftCol.equals(rightCol))
                    compare.add("false");
            }

            if(compare.isEmpty())
                condition.add("true");

            if(!compare.contains("false"))
                condition.add(compare.stream().sorted().collect(joining(" AND ")));
        }

        assert !condition.isEmpty();

        if(condition.contains("true"))
            return null;

        return condition.stream().sorted().collect(joining(" OR ", "(", ")"));
    }


    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return false;
    }


    public static Restrictions getJoinRestrictions(VariableBindings bindings, VariableBindings other,
            Restrictions restrictions)
    {
        return getJoinRestrictions(bindings, Set.of(other), restrictions);
    }


    protected static Restrictions getJoinRestrictions(VariableBindings bindings, Collection<VariableBindings> others,
            Restrictions restrictions)
    {
        Restrictions joinRestrictions = new Restrictions();

        for(VariableBinding binding : bindings.getValues())
        {
            Variable variable = binding.getVariable();

            Set<VariableBinding> vars = others.stream().map(v -> v.get(variable)).filter(v -> v != null)
                    .collect(toSet());

            if(!vars.isEmpty())
            {
                if(binding.canBeNull())
                {
                    joinRestrictions.set(variable, binding.getMappings().keySet());
                }
                else
                {
                    Set<ResourceClass> set = new HashSet<>();

                    for(ResourceClass rc : binding.getMappings().keySet())
                        if(vars.stream().anyMatch(v -> !ResourceClass.areDisjunct(rc, v.getClasses())))
                            set.add(rc);

                    joinRestrictions.set(variable, set);
                }
            }
        }

        return new Restrictions(joinRestrictions, restrictions);
    }


    protected static boolean isJoinable(List<SqlIntercode> childs)
    {
        List<VariableBindings> allVars = childs.stream().map(c -> c.getVariableBindings()).toList();

        for(Variable variable : allVars.stream().flatMap(v -> v.getVariables().stream()).collect(toSet()))
        {
            List<VariableBinding> uvars = allVars.stream().map(v -> v.get(variable))
                    .filter(v -> v != null && !v.canBeNull()).toList();

            if(uvars.size() > 1)
            {
                Set<ResourceClass> resClasses = joinResourceClasses(uvars);

                if(resClasses.isEmpty())
                    return false;

                for(ResourceClass resClass : resClasses)
                {
                    Map<Integer, ConstantColumn> consts = new HashMap<>();

                    for(VariableBinding binding : uvars)
                    {
                        if(binding.canBeNull() || !binding.containsClass(resClass))
                            continue;

                        List<Column> cols = binding.getMapping(resClass);

                        for(int j = 0; j < resClass.getColumnCount(); j++)
                            if(cols.get(j) instanceof ConstantColumn c && !consts.computeIfAbsent(j, _ -> c).equals(c))
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
                List<VariableBindingPair> pairs = VariableBindingPair.getPairs(l.getVariableBindings(),
                        r.getVariableBindings());

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


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlIntercode imcode))
            return false;

        if(hashCode() != imcode.hashCode())
            return false;

        if(!Objects.equals(isDeterministic, imcode.isDeterministic))
            return false;

        if(!Objects.equals(bindings, imcode.bindings))
            return false;

        return true;
    }
}
