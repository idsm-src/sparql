package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.Table.toTableColumns;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
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
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindingPair.ResourceClassPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Node of the intermediate code: a relational-algebra tree the translator builds from the SPARQL AST. Every node knows
 * the variables it binds ({@link #getVariableBindings}) and implements two operations: {@link #optimize}, which
 * rewrites the subtree for what its parent actually needs, and {@link #translate}, which emits the SQL subquery. Nodes
 * are immutable and compared structurally.
 */
public abstract class SqlIntercode extends SqlBaseClass
{
    /**
     * What a parent requires of a child's variables: for each needed variable, the resource classes whose values must
     * be materialised (the box stands for any class). Variables absent from the restrictions are not needed at all.
     */
    public static class Restrictions
    {
        /**
         * Needed classes per variable.
         */
        private final Map<Variable, Set<ResourceClass>> map = new HashMap<>();

        /**
         * Creates empty restrictions (nothing is needed).
         */
        public Restrictions()
        {
        }


        /**
         * Copy constructor.
         *
         * @param restrictions what the parent needs of the variables
         */
        public Restrictions(Restrictions restrictions)
        {
            map.putAll(restrictions.map);
        }


        /**
         * Requires the given variables in any class.
         *
         * @param vars the variables
         */
        public Restrictions(Collection<Variable> vars)
        {
            for(Variable var : vars)
                map.put(var, Set.of(box));
        }


        /**
         * Union of two restrictions.
         *
         * @param a one restriction
         * @param b the other restriction
         */
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


        /**
         * Requires the variable in any class.
         *
         * @param var the variable
         */
        public void add(Variable var)
        {
            map.put(var, Set.of(box));
        }


        /**
         * Requires the variables in any class.
         *
         * @param vars the variables
         */
        public void add(Collection<Variable> vars)
        {
            for(Variable var : vars)
                map.put(var, Set.of(box));
        }


        /**
         * The needed variables.
         *
         * @return the needed variables
         */
        public Set<Variable> getNames()
        {
            return map.keySet();
        }


        /**
         * True if the variable is needed in some class overlapping the given one.
         *
         * @param var the variable
         * @param resClass the resource class
         * @return true if the variable is needed in some class overlapping the given one, false otherwise
         */
        public boolean contains(Variable var, ResourceClass resClass)
        {
            if(!map.containsKey(var))
                return false;

            for(ResourceClass r : map.get(var))
                if(!ResourceClass.areDisjunct(r, resClass))
                    return true;

            return false;
        }


        /**
         * True if restricting the bindings would drop a variable or the columns of some class.
         *
         * @param bindings the variable bindings
         * @return true if restricting the bindings would drop a variable or the columns of some class, false otherwise
         */
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


        /**
         * Adds needed classes of the variable.
         *
         * @param var the variable
         * @param set the needed classes
         */
        public void add(Variable var, Set<ResourceClass> set)
        {
            map.computeIfAbsent(var, _ -> new HashSet<>()).addAll(set);
        }


        /**
         * Adds a needed class of the variable.
         *
         * @param var the variable
         * @param resClass the resource class
         */
        public void add(Variable var, ResourceClass resClass)
        {
            map.computeIfAbsent(var, _ -> new HashSet<>()).add(resClass);
        }


        /**
         * Replaces the needed classes of the variable.
         *
         * @param variable the variable
         * @param set the needed classes
         */
        public void set(Variable variable, Set<ResourceClass> set)
        {
            map.put(variable, set);
        }


        /**
         * True if the variable is needed in some class overlapping one of the given ones.
         *
         * @param var the variable
         * @param classes the classes
         * @return true if the variable is needed in some class overlapping one of the given ones, false otherwise
         */
        public boolean contains(Variable var, Set<ResourceClass> classes)
        {
            for(ResourceClass c : classes)
                if(contains(var, c))
                    return true;

            return false;
        }


        /**
         * Adds all the other restrictions.
         *
         * @param restrictions what the parent needs of the variables
         */
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


        /**
         * True if the variable is needed at all.
         *
         * @param var the variable
         * @return true if the variable is needed at all, false otherwise
         */
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


        /**
         * True if the bindings materialise exactly what is needed.
         *
         * @param bindings the variable bindings
         * @return true if the bindings materialise exactly what is needed, false otherwise
         */
        public boolean isOptimized(VariableBindings bindings)
        {
            return bindings.restrict(this).equals(bindings);
        }


        /**
         * Needed classes of the variable, or null.
         *
         * @param var the variable
         * @return needed classes of the variable, or null
         */
        public Set<ResourceClass> get(Variable var)
        {
            return map.get(var);
        }


        /**
         * The classes needed for the variable, as an expression restriction.
         *
         * @param var the variable
         * @return the classes needed for the variable, as an expression restriction
         */
        public Restriction getRestriction(Variable var)
        {
            return new Restriction(get(var));
        }
    }


    /**
     * Variables bound by the subtree.
     */
    protected final VariableBindings bindings;

    /**
     * Whether repeated evaluation gives the same solutions.
     */
    protected final boolean isDeterministic;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param isDeterministic whether the node is deterministic
     */
    protected SqlIntercode(VariableBindings bindings, boolean isDeterministic)
    {
        this.bindings = bindings;
        this.isDeterministic = isDeterministic;
    }


    /**
     * Returns a subtree with the same solutions (or, when {@code reduced}, the same solutions up to duplicates) that is
     * optimised for the parent's needs: variables and classes outside {@code restrictions} are dropped, constants
     * propagated and nodes simplified or merged. With {@code evalServices}, SERVICE stubs are evaluated against their
     * endpoints and replaced by their results.
     *
     * @param request the current request
     * @param restrictions what the parent needs of the variables
     * @param reduced whether duplicate solutions may be dropped
     * @param evalServices whether SERVICE stubs are evaluated
     * @return a subtree with the same solutions (or, when {@code reduced}, the same solutions up to duplicates) that is
     *         optimised for the parent's needs: variables and classes outside {@code restrictions} are dropped,
     *         constants propagated and nodes simplified or merged
     */
    public abstract SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced,
            boolean evalServices);


    /**
     * SQL of the subtree: a SELECT statement whose result columns are those of the variable bindings.
     *
     * @param request the current request
     * @return SQL of the subtree: a SELECT statement whose result columns are those of the variable bindings
     */
    public abstract String translate(Request request);


    /**
     * True if the subtree contains a SERVICE stub that has not been evaluated yet.
     *
     * @return true if the subtree contains a SERVICE stub that has not been evaluated yet, false otherwise
     */
    public abstract boolean hasServiceSubpattern();


    /**
     * Appends a human-readable tree of the subtree, for debugging.
     *
     * @param builder the builder to append to
     * @param indent the current indentation of the explanation
     */
    public abstract void generateExplanation(StringBuilder builder, String indent);


    /**
     * Human-readable tree of the subtree, for debugging.
     *
     * @return human-readable tree of the subtree, for debugging
     */
    public String getExplanation()
    {
        StringBuilder builder = new StringBuilder();
        generateExplanation(builder, "\n");
        return builder.toString();
    }


    /**
     * Variables bound by the subtree with their classes and result columns.
     *
     * @return variables bound by the subtree with their classes and result columns
     */
    public final VariableBindings getVariableBindings()
    {
        return bindings;
    }


    /**
     * Binding of the variable, or null when unbound.
     *
     * @param variable the variable
     * @return binding of the variable, or null when unbound
     */
    public final VariableBinding getVariable(Variable variable)
    {
        return bindings.get(variable);
    }


    /**
     * Columns per class of the variable.
     *
     * @param variable the variable
     * @return columns per class of the variable
     */
    public final Map<ResourceClass, List<Column>> getMappings(Variable variable)
    {
        return bindings.get(variable).getMappings();
    }


    /**
     * Columns of the variable in the class, or null.
     *
     * @param variable the variable
     * @param resClass the resource class
     * @return columns of the variable in the class, or null
     */
    public final List<Column> getMapping(Variable variable, ResourceClass resClass)
    {
        return bindings.get(variable).getMapping(resClass);
    }


    /**
     * True if the variable is unbound or bound only to constant columns.
     *
     * @param variable the variable
     * @return true if the variable is unbound or bound only to constant columns, false otherwise
     */
    public final boolean hasConstantVariable(Variable variable)
    {
        VariableBinding binding = bindings.get(variable);

        return binding == null || binding.isConstant();
    }


    /**
     * True if every variable is unbound or bound only to constant columns.
     *
     * @param variables the variables
     * @return true if every variable is unbound or bound only to constant columns, false otherwise
     */
    public final boolean hasConstantVariables(Set<Variable> variables)
    {
        return variables.stream().allMatch(v -> hasConstantVariable(v));
    }


    /**
     * True if repeated evaluation gives the same solutions (no {@code RAND}, {@code BNODE}, nondeterministic functions
     * or unevaluated services).
     *
     * @return true if repeated evaluation gives the same solutions (no {@code RAND}, {@code BNODE}, nondeterministic
     *         functions or unevaluated services), false otherwise
     */
    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    /**
     * Classes a variable can take in a join where it is always bound in the given children: the intersections of one
     * class per child, merged into disjoint classes.
     *
     * @param defs bindings of the variable in the children where it is always bound
     * @return classes a variable can take in a join where it is always bound in the given children: the intersections
     *         of one class per child, merged into disjoint classes
     */
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

        //FIXME: getDisjunctClasses is probably not needed
        return getDisjunctClasses(result.stream().map(s -> getIntersectionClass(s)).collect(toSet()));
    }


    /**
     * Bindings of the join of the given children (aliased by {@code tables}). A variable bound non-null in some child
     * keeps only the classes compatible across those children (empty bindings are returned if there is none, or if
     * constants differ), otherwise all its classes are unioned and coalesced. Fresh output columns are allocated;
     * {@code map} receives for each of them the child column it is taken from.
     *
     * @param request the current request
     * @param allVars bindings of each child
     * @param tables the tables
     * @param restrictions what the parent needs of the variables
     * @param map the column map
     * @return bindings of the join of the given children (aliased by {@code tables})
     */
    protected static VariableBindings getJoinVariableBindings(Request request, List<VariableBindings> allVars,
            List<? extends Table> tables, Restrictions restrictions, Map<Column, Column> map)
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


    /**
     * Binding of one join variable: for an always-bound variable the columns of each class are taken from the children
     * binding it (null when constants conflict), otherwise the variants of all children are coalesced.
     *
     * @param request the current request
     * @param variable the variable
     * @param resClasses the resource classes
     * @param vars bindings of the variable in each child, null where unbound
     * @param tables the tables
     * @param columnMap the column map
     * @param canBeNull whether the value may be null
     * @return the join binding of the variable, or null when constants conflict
     */
    private static VariableBinding createVariableBinding(Request request, Variable variable,
            Set<ResourceClass> resClasses, List<VariableBinding> vars, List<? extends Table> tables,
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

                    if(binding == null || binding.canBeNull() || !binding.contains(resClass))
                        continue;

                    mappings.add(binding.deriveMapping(resClass, tables.get(i)));
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
                            resClass.createColumns(request.getColumnMap(), variable), columns, columnMap, false));
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

                variableBinding.addMapping(resClass, getMappedColuns(
                        resClass.createColumns(request.getColumnMap(), variable), columns, columnMap, true));
            }
        }

        return variableBinding;
    }


    /**
     * Picks for each column position a constant if some child provides one, otherwise the first child's column.
     *
     * @param resClass the resource class
     * @param mappings columns per resource class
     * @return the selected columns
     */
    private static List<Column> selectColumns(ResourceClass resClass, List<List<Column>> mappings)
    {
        List<Column> columns = new ArrayList<>(resClass.getColumnCount());

        for(int i = 0; i < resClass.getColumnCount(); i++)
        {
            int position = i;

            Column column = mappings.stream().map(m -> m.get(position)).filter(c -> c instanceof ConstantColumn)
                    .findFirst().orElse(mappings.getFirst().get(i));

            // the joined columns are equal, so the value is not null when any of them is not null
            if(column instanceof ExpressionColumn && column.canBeNull()
                    && mappings.stream().anyMatch(m -> !m.get(position).canBeNull()))
                column = new ExpressionColumn(column.getName(), false);

            columns.add(column);
        }

        return columns;
    }


    /**
     * Maps child columns to the output columns, reusing an output column already assigned to the same child column;
     * constants pass through. Unless the variable may be unbound, an output column keeps the knowledge that the child
     * column is not null.
     *
     * @param output fresh output columns
     * @param input child columns
     * @param map the column map
     * @param canBeNull whether the variable may be unbound
     * @return the output columns
     */
    private static List<Column> getMappedColuns(List<Column> output, List<Column> input, Map<Column, Column> map,
            boolean canBeNull)
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
                    // the exposed column keeps the knowledge that the accessed value is not null
                    column = new TableColumn(output.get(i).getName(), canBeNull || access.canBeNull());
                    map.put(access, column);
                }

                mapping.add(column);
            }
        }

        return mapping;
    }


    /**
     * COALESCE of the variants per column position; a single variant is returned as is.
     *
     * @param cols number of columns
     * @param variants column variants to coalesce
     * @return COALESCE of the variants per column position; a single variant is returned as is
     */
    private static List<Column> coalesceVariants(int cols, List<List<Column>> variants)
    {
        if(variants.size() == 1)
            return variants.getFirst();

        return IntStream
                .range(0, cols).mapToObj(i -> (Column) new ExpressionColumn(variants.stream()
                        .map(l -> l.get(i).toString()).distinct().sorted().collect(joining(", ", "COALESCE(", ")"))))
                .toList();
    }


    /**
     * Union of the classes of the bindings.
     *
     * @param variables the variables
     * @return union of the classes of the bindings
     */
    private static Set<ResourceClass> collectClasses(List<VariableBinding> variables)
    {
        return variables.stream().flatMap(v -> v.getClasses().stream()).collect(toSet());
    }


    /**
     * Bindings of the join of two children, see
     * {@link #getJoinVariableBindings(Request, List, List, Restrictions, Map)}.
     *
     * @param request the current request
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @param leftTable the left table
     * @param rightTable the right table
     * @param restrictions what the parent needs of the variables
     * @param map the column map
     * @return bindings of the join of two children, see
     *         {@link #getJoinVariableBindings(Request, List, List, Restrictions, Map)}
     */
    protected static VariableBindings getJoinVariableBindings(Request request, VariableBindings left,
            VariableBindings right, Table leftTable, Table rightTable, Restrictions restrictions,
            Map<Column, Column> map)
    {
        return getJoinVariableBindings(request, Arrays.asList(left, right), Arrays.asList(leftTable, rightTable),
                restrictions, map);
    }


    /**
     * True if the shared variables need no join condition: all are always bound to identical constant columns.
     *
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @return true if the shared variables need no join condition, false otherwise
     */
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


    /**
     * SQL condition joining every pair of children on their shared variables, or null if none is needed.
     *
     * @param vars the variables
     * @param tables the tables
     * @return SQL condition joining every pair of children on their shared variables, or null if none is needed
     */
    public static String generateJoinCondition(List<VariableBindings> vars, List<? extends Table> tables)
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


    /**
     * SQL condition that every shared variable is compatible on both sides (equal values after conversion to a common
     * class, or unbound on either side), or null if it always holds.
     *
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @param leftTable the left table
     * @param rightTable the right table
     * @return SQL condition that every shared variable is compatible on both sides (equal values after conversion to a
     *         common class, or unbound on either side), or null if it always holds
     */
    public static String generateJoinCondition(VariableBindings left, VariableBindings right, Table leftTable,
            Table rightTable)
    {
        Set<String> join = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(left, right))
        {
            String condition = generateJoinCondition(pair, leftTable, rightTable);

            if(condition != null)
                join.add(condition);
        }

        if(join.isEmpty())
            return null;

        return join.stream().sorted().collect(joining(" AND ", "(", ")"));
    }


    /**
     * SQL condition that one variable is compatible on both sides, or null if it always holds (also when the variable
     * is bound on one side only).
     *
     * @param leftBinding the variable binding
     * @param rightBinding the variable binding
     * @param leftTable the left table
     * @param rightTable the right table
     * @return SQL condition that one variable is compatible on both sides, or null if it always holds
     */
    public static String generateJoinCondition(VariableBinding leftBinding, VariableBinding rightBinding,
            Table leftTable, Table rightTable)
    {
        if(leftBinding == null || rightBinding == null)
            return null;

        return generateJoinCondition(new VariableBindingPair(leftBinding, rightBinding), leftTable, rightTable);
    }


    /**
     * SQL condition that the paired variable is compatible on both sides: unbound on either side, or equal values after
     * conversion to a common class. Null if the condition always holds, {@code (false)} if it never holds.
     *
     * @param pair the paired bindings of the variable
     * @param leftTable the left table
     * @param rightTable the right table
     * @return SQL condition that the paired variable is compatible on both sides, or null if it always holds
     */
    private static String generateJoinCondition(VariableBindingPair pair, Table leftTable, Table rightTable)
    {
        VariableBinding leftBinding = pair.getLeftVariableBinding();
        VariableBinding rightBinding = pair.getRightVariableBinding();

        Set<String> condition = new HashSet<>();

        if(leftBinding.canBeNull())
            condition.add(leftBinding.getIsNull(leftTable));

        if(rightBinding.canBeNull())
            condition.add(rightBinding.getIsNull(rightTable));

        for(ResourceClassPair pairedClass : pair.getClasses())
        {
            ResourceClass leftClass = pairedClass.getLeftClass();
            ResourceClass rightClass = pairedClass.getRightClass();

            if(leftClass == null || rightClass == null)
                continue;

            ResourceClass unionClass = unionize(leftClass, rightClass);

            List<Column> leftCols = toTableColumns(leftTable, leftBinding.getMapping(leftClass));
            List<Column> rightCols = toTableColumns(rightTable, rightBinding.getMapping(rightClass));

            List<Column> genLeftCols = leftClass.toGeneralClass(unionClass, leftCols, false);
            List<Column> genRightCols = rightClass.toGeneralClass(unionClass, rightCols, false);

            Set<String> compare = new HashSet<>();

            for(int i = 0; i < unionClass.getColumnCount(); i++)
            {
                Column leftCol = genLeftCols.get(i);
                Column rightCol = genRightCols.get(i);
                SqlType type = unionClass.getSqlTypes().get(i);

                if(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn)
                {
                    if(!leftCol.equals(rightCol))
                        compare.add("false");
                }
                else if(!unionClass.isOptionalColumn(i))
                {
                    compare.add(type.equal(leftCol, rightCol));
                }
                else if(leftCol instanceof NullColumn)
                {
                    compare.add(rightCol.canBeNull() ? "(" + rightCol + " IS NULL)" : "false");
                }
                else if(rightCol instanceof NullColumn)
                {
                    compare.add(leftCol.canBeNull() ? "(" + leftCol + " IS NULL)" : "false");
                }
                else if(!leftCol.canBeNull() || !rightCol.canBeNull())
                {
                    compare.add(type.equal(leftCol, rightCol));
                }
                else
                {
                    compare.add(type.notDistinct(leftCol, rightCol));
                }
            }

            if(compare.isEmpty())
                compare.add("true");

            if(!compare.contains("false"))
                condition.add(compare.stream().sorted().collect(joining(" AND ")));
        }

        if(condition.isEmpty())
            condition.add("false");

        if(condition.contains("true"))
            return null;

        return condition.stream().sorted().collect(joining(" OR ", "(", ")"));
    }


    /**
     * True if the solutions projected to the selected variables are known to contain no duplicates.
     *
     * @param request the current request
     * @param selected the selected variables
     * @return true if the solutions projected to the selected variables are known to contain no duplicates, false
     *         otherwise
     */
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return false;
    }


    /**
     * Restrictions for a join child on top of the parent's: its variables shared with the other side are needed in the
     * classes compatible with that side (in all classes when the variable may be unbound).
     *
     * @param bindings the variable bindings
     * @param other bindings of the other side
     * @param restrictions what the parent needs of the variables
     * @return restrictions for a join child on top of the parent's: its variables shared with the other side are needed
     *         in the classes compatible with that side (in all classes when the variable may be unbound)
     */
    public static Restrictions getJoinRestrictions(VariableBindings bindings, VariableBindings other,
            Restrictions restrictions)
    {
        return getJoinRestrictions(bindings, Set.of(other), restrictions);
    }


    /**
     * Restrictions for a join child against several other children, see
     * {@link #getJoinRestrictions(VariableBindings, VariableBindings, Restrictions)}.
     *
     * @param bindings the variable bindings
     * @param others bindings of the other children
     * @param restrictions what the parent needs of the variables
     * @return restrictions for a join child against several other children, see
     *         {@link #getJoinRestrictions(VariableBindings, VariableBindings, Restrictions)}
     */
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


    /**
     * False if a variable is always bound in several children with disjoint classes or different constants, so the join
     * is empty.
     *
     * @param childs the child nodes
     * @return false if a variable is always bound in several children with disjoint classes or different constants, so
     *         the join is empty, true otherwise
     */
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


    /**
     * False if some shared variable can never match between a join component of the left and a join component of the
     * right subtree (see {@link VariableBindingPair#isJoinable}).
     *
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @return false if some shared variable can never match between a join component of the left and a join component
     *         of the right subtree (see {@link VariableBindingPair#isJoinable}), true otherwise
     */
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


    /**
     * The components the subtree joins, looking through joins, filters, distinct, and the left sides of left joins and
     * minus.
     *
     * @param child the child node
     * @return the components the subtree joins, looking through joins, filters, distinct, and the left sides of left
     *         joins and minus
     */
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

        if(child instanceof SqlLateralJoin join)
            return Stream.concat(getJoinList(join.getLeft()).stream(), getJoinList(join.getRight()).stream()).toList();

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
