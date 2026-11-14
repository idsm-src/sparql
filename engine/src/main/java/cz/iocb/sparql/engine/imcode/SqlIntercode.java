package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.Table.toTableColumns;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.intersect;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator;
import cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;
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
     * be materialised (the box stands for any class). Variables absent from the restrictions are not needed at all. For
     * a variable a join compares with the same variable of another child, the restrictions also carry the classes of
     * that other side (the join classes, see {@link #getJoinClasses}): a union presents its classes overlapping one of
     * them as a single class, so that the join compares a single pair of classes instead of a disjunction of pairs.
     * Where that other side also drops the solutions it cannot join (an inner join, the right side of a left join), the
     * classes it binds without a null are the accepted classes of the variable (see {@link #getAcceptedClasses}): a
     * solution binding the variable to a value of a class disjoint with all of them never reaches the result, so a
     * child may leave such solutions out. Inside a pattern evaluated for a current solution (an EXISTS pattern, or a
     * pattern evaluated per graph), a variable merged with its value in the current solution has that value as its
     * accepted value (see {@link #getAcceptedValue}), and so has a variable the term a filter requires of it (see
     * {@link SqlIntercode#addAcceptedValues}): a solution binding the variable to another term never reaches the result
     * either, and a table access binding the variable takes the value into its conditions (see {@link SqlTableAccess}).
     */
    public static class Restrictions
    {
        /**
         * Needed classes per variable.
         */
        private final Map<Variable, Set<ResourceClass>> map = new HashMap<>();

        /**
         * Join classes per variable: the classes the variable is compared with on the other sides of the joins above.
         */
        private final Map<Variable, Set<ResourceClass>> joinClasses = new HashMap<>();

        /**
         * Accepted classes per variable: the classes a bound value of the variable has to overlap for its solution to
         * reach the result; a variable without an entry is accepted in any class.
         */
        private final Map<Variable, Set<ResourceClass>> acceptedClasses = new HashMap<>();

        /**
         * Accepted value per variable: the term a bound value of the variable has to be for its solution to reach the
         * result, given by an expression that has the same value for all the solutions (a value of the current solution
         * the pattern is evaluated for, or a constant); a variable without an entry is accepted with any value.
         */
        private final Map<Variable, SqlExpressionIntercode> acceptedValues = new HashMap<>();

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
            addJoinClasses(restrictions.joinClasses);
            addAcceptedClasses(restrictions.acceptedClasses);
            addAcceptedValues(restrictions.acceptedValues);
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

            addJoinClasses(a.joinClasses);
            addJoinClasses(b.joinClasses);
            addAcceptedClasses(a.acceptedClasses);
            addAcceptedClasses(b.acceptedClasses);
            addAcceptedValues(a.acceptedValues);
            addAcceptedValues(b.acceptedValues);
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
         * @param relations declarations which unrelated user IRI classes may overlap
         * @param var the variable
         * @param resClass the resource class
         * @return true if the variable is needed in some class overlapping the given one, false otherwise
         */
        public boolean contains(ClassRelations relations, Variable var, ResourceClass resClass)
        {
            if(!map.containsKey(var))
                return false;

            for(ResourceClass r : map.get(var))
                if(!ResourceClass.areDisjunct(relations, r, resClass))
                    return true;

            return false;
        }


        /**
         * True if restricting the bindings would drop a variable or the columns of some class.
         *
         * @param relations declarations which unrelated user IRI classes may overlap
         * @param bindings the variable bindings
         * @return true if restricting the bindings would drop a variable or the columns of some class, false otherwise
         */
        public boolean canBeOptimized(ClassRelations relations, VariableBindings bindings)
        {
            VariableBindings opt = bindings.restrict(relations, this);

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
         * Adds needed classes of the variable. The stored set is replaced by a new one, because it may be immutable or
         * shared with other restrictions.
         *
         * @param var the variable
         * @param set the needed classes
         */
        public void add(Variable var, Set<ResourceClass> set)
        {
            Set<ResourceClass> classes = new HashSet<>(set);
            Set<ResourceClass> current = map.get(var);

            if(current != null)
                classes.addAll(current);

            map.put(var, classes);
        }


        /**
         * Adds a needed class of the variable.
         *
         * @param var the variable
         * @param resClass the resource class
         */
        public void add(Variable var, ResourceClass resClass)
        {
            add(var, Set.of(resClass));
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
         * @param relations declarations which unrelated user IRI classes may overlap
         * @param var the variable
         * @param classes the classes
         * @return true if the variable is needed in some class overlapping one of the given ones, false otherwise
         */
        public boolean contains(ClassRelations relations, Variable var, Set<ResourceClass> classes)
        {
            for(ResourceClass c : classes)
                if(contains(relations, var, c))
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

            addJoinClasses(restrictions.joinClasses);
            addAcceptedClasses(restrictions.acceptedClasses);
            addAcceptedValues(restrictions.acceptedValues);
        }


        /**
         * Adds join classes of the variable, see {@link #getJoinClasses(Variable)}.
         *
         * @param var the variable
         * @param classes classes of the other side of a join
         */
        public void addJoinClasses(Variable var, Set<ResourceClass> classes)
        {
            joinClasses.computeIfAbsent(var, _ -> new HashSet<>()).addAll(classes);
        }


        /**
         * Adds join classes of several variables, see {@link #getJoinClasses(Variable)}.
         *
         * @param classes join classes per variable
         */
        private void addJoinClasses(Map<Variable, Set<ResourceClass>> classes)
        {
            for(Entry<Variable, Set<ResourceClass>> entry : classes.entrySet())
                addJoinClasses(entry.getKey(), entry.getValue());
        }


        /**
         * Join classes of the variable: the classes the variable is compared with on the other sides of the joins above
         * the child, or an empty set when no join compares it. A union presents its classes overlapping one of them as
         * a single class (see {@link SqlIntercode#unionResourceClasses(ClassRelations, List, Set)}).
         *
         * @param var the variable
         * @return join classes of the variable, or an empty set when no join compares it
         */
        public Set<ResourceClass> getJoinClasses(Variable var)
        {
            return joinClasses.getOrDefault(var, Set.of());
        }


        /**
         * Adds accepted classes of the variable, see {@link #getAcceptedClasses(Variable)}. The sets of several joins
         * are united, so that a value is still accepted when it overlaps the classes of any of them, which is weaker
         * than what the joins require but never leaves out a solution they keep.
         *
         * @param var the variable
         * @param classes classes of a side of a join that drops the solutions it cannot join
         */
        public void addAcceptedClasses(Variable var, Set<ResourceClass> classes)
        {
            acceptedClasses.computeIfAbsent(var, _ -> new HashSet<>()).addAll(classes);
        }


        /**
         * Adds accepted classes of several variables, see {@link #getAcceptedClasses(Variable)}.
         *
         * @param classes accepted classes per variable
         */
        private void addAcceptedClasses(Map<Variable, Set<ResourceClass>> classes)
        {
            for(Entry<Variable, Set<ResourceClass>> entry : classes.entrySet())
                addAcceptedClasses(entry.getKey(), entry.getValue());
        }


        /**
         * Accepted classes of the variable: the classes a bound value of the variable has to overlap for its solution
         * to reach the result, because the joins above the child drop the solutions they cannot join with the values of
         * these classes. Null when the variable is accepted in any class.
         *
         * @param var the variable
         * @return accepted classes of the variable, or null when it is accepted in any class
         */
        public Set<ResourceClass> getAcceptedClasses(Variable var)
        {
            return acceptedClasses.get(var);
        }


        /**
         * Accepts the given variables in any class, keeping the accepted classes of the others. A node whose solutions
         * are not all dropped above (a slice, the left side of a left join) calls it for the variables it passes on
         * without that guarantee. The accepted values are kept and dropped along with the accepted classes.
         *
         * @param vars the variables that keep their accepted classes and values
         */
        public void retainAcceptedClasses(Collection<Variable> vars)
        {
            acceptedClasses.keySet().retainAll(vars);
            acceptedValues.keySet().retainAll(vars);
        }


        /**
         * Accepts the variable in any class and with any value.
         *
         * @param var the variable
         */
        public void removeAcceptedClasses(Variable var)
        {
            acceptedClasses.remove(var);
            acceptedValues.remove(var);
        }


        /**
         * Sets the accepted value of the variable, see {@link #getAcceptedValue(Variable)}.
         *
         * @param var the variable
         * @param value expression giving the only term a bound value of the variable may be, the same for all the
         *            solutions
         */
        public void setAcceptedValue(Variable var, SqlExpressionIntercode value)
        {
            acceptedValues.put(var, value);
        }


        /**
         * Adds accepted values of several variables, see {@link #getAcceptedValue(Variable)}. A variable that has an
         * accepted value already keeps it: both are requirements of the nodes above, so either of them holds.
         *
         * @param values accepted value per variable
         */
        private void addAcceptedValues(Map<Variable, SqlExpressionIntercode> values)
        {
            for(Entry<Variable, SqlExpressionIntercode> entry : values.entrySet())
                acceptedValues.putIfAbsent(entry.getKey(), entry.getValue());
        }


        /**
         * Accepted value of the variable: the term a bound value of the variable has to be for its solution to reach
         * the result, because a node above merges the variable with the value or filters the solutions by a condition
         * requiring the term. It is given by an expression that has the same value for all the solutions: a value of
         * the current solution the pattern being optimised is evaluated for (see
         * {@link SqlExpressionIntercode#isCurrentValue}), or a constant. A solution leaving the variable unbound is
         * accepted. Null when the variable is accepted with any value.
         *
         * @param var the variable
         * @return expression giving the accepted value of the variable, or null when it is accepted with any value
         */
        public SqlExpressionIntercode getAcceptedValue(Variable var)
        {
            return acceptedValues.get(var);
        }


        /**
         * Accepts the variable with any value, keeping its accepted classes.
         *
         * @param var the variable
         */
        public void removeAcceptedValue(Variable var)
        {
            acceptedValues.remove(var);
        }


        /**
         * Accepts every variable with any value of a current solution. A node whose children are evaluated by a
         * statement of their own (the context of a SERVICE call) calls it, because the values of a current solution are
         * not known there; a constant, which a filter requires of a variable, is known everywhere and stays accepted
         * alone.
         */
        public void removeCurrentValues()
        {
            acceptedValues.values().removeIf(v -> v.isCurrentValue());
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

            return map.equals(((Restrictions) other).map) && joinClasses.equals(((Restrictions) other).joinClasses)
                    && acceptedClasses.equals(((Restrictions) other).acceptedClasses)
                    && acceptedValues.equals(((Restrictions) other).acceptedValues);
        }


        /**
         * True if the bindings materialise exactly what is needed.
         *
         * @param relations declarations which unrelated user IRI classes may overlap
         * @param bindings the variable bindings
         * @return true if the bindings materialise exactly what is needed, false otherwise
         */
        public boolean isOptimized(ClassRelations relations, VariableBindings bindings)
        {
            return bindings.restrict(relations, this).equals(bindings);
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
     * Guard of a loop that optimises the sides of a join, each with restrictions derived from the bindings the other
     * sides had at the end of the previous round, until nothing changes any more. A side presents the classes of a
     * shared variable overlapping one of its join classes, the classes of the other sides, as their union (see
     * {@link Restrictions#getJoinClasses}), so two sides binding the variable in the same classes, one presenting them
     * merged and the other one apart, follow each other's presentation of the previous round: they swap their
     * presentations in every round, and the loop would never end. The guard records the states the loop passes through;
     * a state coming back closes a cycle, and the classes the variables take in the states of the cycle become settled
     * classes, which the restrictions of every side add to the classes of the other sides of each shared variable from
     * then on: a side then merges the classes as far as any state of the cycle merged them, and it needs the variable
     * in every class overlapping a settled class, so that a merge never takes in a class whose columns the side has
     * dropped. The settled classes only grow and are drawn from finitely many classes, so only finitely many cycles add
     * new ones; a cycle adding none is not caused by the presentations, which follow the same join classes in all its
     * rounds then, and ends the loop in its current state. A loop records the state of a round only when its own test
     * of a fixpoint has failed, so a loop that reaches a fixpoint without cycling is not affected at all.
     *
     * @see SqlIntercode#getJoinRestrictions(ClassRelations, VariableBindings, Collection, Restrictions, boolean, Map)
     */
    protected static final class Settlement
    {
        /**
         * The states recorded since the settled classes last changed, in the order the loop passed through them.
         */
        private final List<Object> states = new ArrayList<>();

        /**
         * The bindings of the sides in each recorded state.
         */
        private final List<List<VariableBindings>> bindings = new ArrayList<>();

        /**
         * Settled classes per variable.
         */
        private final Map<Variable, Set<ResourceClass>> classes = new HashMap<>();


        /**
         * Creates a guard with no state recorded and no settled classes.
         */
        public Settlement()
        {
        }


        /**
         * Settled classes per variable: the classes the variables took in the states of the cycles the loop has passed
         * through, none until the loop cycles.
         *
         * @return settled classes per variable
         */
        public Map<Variable, Set<ResourceClass>> getClasses()
        {
            return classes;
        }


        /**
         * Records the state the loop reached at the end of a round: the sides the next round starts from, with the
         * other values the round depends on. A state recorded since the settled classes last changed, other than the
         * last one, closes a cycle: the classes the variables of the sides take in the states of the cycle are added to
         * the settled classes, and the record starts again from the state.
         *
         * @param state the state, compared by equality
         * @param sides the sides the next round starts from
         * @return false if the state closes a cycle adding no settled class, so that the loop has to end, true
         *         otherwise
         */
        public boolean record(Object state, Collection<SqlIntercode> sides)
        {
            int index = states.indexOf(state);

            if(index >= 0 && index == states.size() - 1)
                return true;

            List<VariableBindings> sideBindings = sides.stream().map(s -> s.getVariableBindings()).toList();

            if(index < 0)
            {
                states.add(state);
                bindings.add(sideBindings);
                return true;
            }

            boolean grown = false;

            for(List<VariableBindings> cycle : bindings.subList(index, bindings.size()))
                for(VariableBindings side : cycle)
                    for(VariableBinding binding : side.getValues())
                        grown |= classes.computeIfAbsent(binding.getVariable(), _ -> new HashSet<>())
                                .addAll(binding.getClasses());

            states.clear();
            bindings.clear();
            states.add(state);
            bindings.add(sideBindings);

            return grown;
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
     * endpoints and replaced by their results. The subtree is optimised for the place it lies in: the expressions that
     * refer to a current solution read it from the scope (see {@link Scope}).
     *
     * @param request the current request
     * @param scope the scope of the node
     * @param restrictions what the parent needs of the variables
     * @param reduced whether duplicate solutions may be dropped
     * @param evalServices whether SERVICE stubs are evaluated
     * @return a subtree with the same solutions (or, when {@code reduced}, the same solutions up to duplicates) that is
     *         optimised for the parent's needs: variables and classes outside {@code restrictions} are dropped,
     *         constants propagated and nodes simplified or merged
     */
    public abstract SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices);


    /**
     * SQL code of the subtree: a query whose result columns are those of the variable bindings, together with the
     * declared tables it reads (see {@link SqlCode}), which the statement the code becomes a part of declares in its
     * {@code WITH} clause (see {@link SqlSelect#translateWithClause}). The scope has to be the one the subtree was
     * optimised in, because the SQL of the patterns of its expressions has been rendered for it (see
     * {@link Scope#getInputTable}).
     *
     * @param request the current request
     * @param scope the scope of the node
     * @return SQL code of the subtree: a query whose result columns are those of the variable bindings
     */
    public abstract SqlCode translate(Request request, Scope scope);


    /**
     * True if the subtree contains a SERVICE stub that has not been evaluated yet, the patterns of the EXISTS
     * expressions of its nodes included; the second optimisation pass has to reach every such stub.
     *
     * @return true if the subtree contains a SERVICE stub that has not been evaluated yet, false otherwise
     */
    public abstract boolean hasServiceSubpattern();


    /**
     * The node filtered by the condition, with the condition evaluated on the solutions that the SERVICE calls of the
     * node are evaluated for (their contexts, see {@link SqlServiceStub}), so that the endpoints are not asked about
     * solutions the condition drops anyway; null when the node cannot take the condition in. A filter standing above a
     * call does not restrict its context, which a statement of its own reads before the call is made. A node takes the
     * condition in by handing it down to a part its solutions are made of, when the condition holds for a solution of
     * the node exactly when it holds for the solution of the part it is made of (see {@link #keepsValues}), until the
     * condition gets to a context; the condition is then rebuilt over the bindings of the context, when they allow it
     * (see {@link #filterBy}). A node that is no such composition (a slice, a node evaluated for a current solution)
     * takes no condition in.
     *
     * @param request the current request
     * @param scope the scope of the node
     * @param condition the condition, which gives the same value whenever it is evaluated for the same solution (see
     *            {@link #isSinkable})
     * @param bindings the bindings the condition is optimised for, those of a node above
     * @return the node filtered by the condition, with the condition evaluated on the contexts of its SERVICE calls, or
     *         null when the node cannot take the condition in
     */
    protected SqlIntercode sink(Request request, Scope scope, SqlExpressionIntercode condition,
            VariableBindings bindings)
    {
        return null;
    }


    /**
     * Filter of the node by the condition rebuilt over the bindings of the node, or null when the condition cannot be
     * rebuilt over them. An expression decides what it reads of an operand by the classes the operand had when the
     * expression was optimised last, so it can be rebuilt over bindings whose classes it knows: every class the node
     * gives a deciding variable of the condition has to be a class the variable has in the bindings the condition is
     * optimised for, or a subclass of one, as the classes of a node narrowed by an optimisation are. The bindings of a
     * node below may hold another class as well, of values that never get to the node above (a variable a SERVICE call
     * shares with its context is bound there in any class a received value may take).
     *
     * @param request the current request
     * @param scope the scope of the node
     * @param condition the condition
     * @param bindings the bindings the condition is optimised for
     * @param node the node to filter
     * @return filter of the node by the condition rebuilt over the bindings of the node, or null when the condition
     *         cannot be rebuilt over them
     */
    protected static SqlIntercode filterBy(Request request, Scope scope, SqlExpressionIntercode condition,
            VariableBindings bindings, SqlIntercode node)
    {
        for(Variable variable : getDecidingVariables(request.getConfiguration(), condition))
        {
            VariableBinding binding = node.getVariable(variable);

            if(binding == null)
                continue;

            VariableBinding known = bindings.get(variable);

            if(known == null)
                return null;

            for(ResourceClass resClass : binding.getClasses())
                if(known.getClasses().stream().noneMatch(c -> resClass.isSubclassOf(c)))
                    return null;
        }

        SqlExpressionIntercode rebuilt = condition.optimize(request, scope, node.getVariableBindings(),
                new Restriction(xsdBoolean), false);

        return SqlFilter.filter(request, List.of(rebuilt), node);
    }


    /**
     * True if the condition may be handed down to the context of a SERVICE call (see {@link #sink}): it gives the same
     * value whenever it is evaluated for the same solution, so it is deterministic, which a condition holding a call
     * that is not evaluated yet is not, and it does not refer to a current solution (see
     * {@link Scope#refersToCurrentSolution}), which the statement reading a context does not know.
     *
     * @param request the current request
     * @param scope the scope of the node holding the condition
     * @param condition the condition, with its boolean value materialised
     * @return true if the condition may be handed down to the context of a SERVICE call, false otherwise
     */
    protected static boolean isSinkable(Request request, Scope scope, SqlExpressionIntercode condition)
    {
        if(!condition.isDeterministic())
            return false;

        return !scope.refersToCurrentSolution(translateConditions(request.getConfiguration(), List.of(condition)));
    }


    /**
     * The conditions with every conjunction replaced by its operands: the conditions of a node have to hold all, and a
     * conjunction is true only when both its operands are, so the result holds for the same solutions. It lets a part
     * of a condition be handed down by itself (see {@link #sink}).
     *
     * @param conditions the conditions, all of which have to hold
     * @return the conditions with every conjunction replaced by its operands
     */
    protected static List<SqlExpressionIntercode> getConjuncts(List<SqlExpressionIntercode> conditions)
    {
        List<SqlExpressionIntercode> conjuncts = new ArrayList<>();

        for(SqlExpressionIntercode condition : conditions)
        {
            if(condition instanceof SqlBinaryLogical logical && logical.getOperator() == LogicalOperator.AND)
                conjuncts.addAll(getConjuncts(List.of(logical.getLeft(), logical.getRight())));
            else
                conjuncts.add(condition);
        }

        return conjuncts;
    }


    /**
     * The variables whose values decide the value of the condition: the variables its expression refers to and the
     * variables it needs of the solutions it is evaluated for, among them every variable of those solutions that a
     * pattern of its EXISTS expressions may read.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param condition the condition
     * @return the variables whose values decide the value of the condition
     */
    protected static Set<Variable> getDecidingVariables(ClassRelations relations, SqlExpressionIntercode condition)
    {
        Set<Variable> variables = new HashSet<>(condition.getReferencedVariables());
        variables.addAll(condition.getRequirements(relations).getNames());

        return variables;
    }


    /**
     * True if every solution a node makes of a solution of one of its parts gives each of the variables the value the
     * solution of the part gives it, a variable the part leaves unbound staying unbound: the part binds the variable
     * always, or none of the other parts the solution is made of binds it. A condition decided by such variables (see
     * {@link #getDecidingVariables}) holds for a solution of the node exactly when it holds for the solution of the
     * part, so filtering the part by it is the same as filtering the node.
     *
     * @param variables the variables
     * @param part bindings of the part
     * @param others bindings of the other parts the solutions of the node are made of
     * @return true if every solution the node makes of a solution of the part gives each of the variables the value the
     *         solution of the part gives it, false otherwise
     */
    protected static boolean keepsValues(Set<Variable> variables, VariableBindings part,
            Collection<VariableBindings> others)
    {
        for(Variable variable : variables)
        {
            VariableBinding binding = part.get(variable);

            if(binding != null && !binding.canBeNull())
                continue;

            if(others.stream().anyMatch(o -> o.get(variable) != null))
                return false;
        }

        return true;
    }


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
     * Classes a variable can take in a join where it is always bound in the given children: the intersections of the
     * combinations of one class per child whose classes pairwise overlap. The intersections are pairwise disjoint, as
     * two combinations differ in the class taken from some child, and the classes of one child are disjoint.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param defs bindings of the variable in the children where it is always bound
     * @return classes a variable can take in a join where it is always bound in the given children: the intersections
     *         of the combinations of one class per child whose classes pairwise overlap
     */
    private static Set<ResourceClass> joinResourceClasses(ClassRelations relations, List<VariableBinding> defs)
    {
        List<Set<ResourceClass>> result = List.of(Set.of());

        for(VariableBinding def : defs)
        {
            List<Set<ResourceClass>> nextResult = new ArrayList<>();

            for(ResourceClass resClass : def.getClasses())
                for(Set<ResourceClass> set : result)
                    if(set.stream().allMatch(c -> !ResourceClass.areDisjunct(relations, c, resClass)))
                        nextResult.add(Stream.concat(set.stream(), Stream.of(resClass)).collect(toSet()));

            result = nextResult;
        }

        return result.stream().map(s -> intersect(relations, s)).collect(toSet());
    }


    /**
     * Classes a variable can take in a union of the given children: the classes of the children, where overlapping
     * classes are merged into their union, so that the classes are pairwise disjoint and every class of a child is a
     * subclass of one of them.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param defs bindings of the variable in the children where it is bound
     * @return classes a variable can take in a union of the given children: the classes of the children, where
     *         overlapping classes are merged into their union
     */
    protected static Set<ResourceClass> unionResourceClasses(ClassRelations relations, List<VariableBinding> defs)
    {
        List<ResourceClass> classes = defs.stream().flatMap(d -> d.getClasses().stream()).toList();

        return new HashSet<>(mergeOverlappingClasses(relations, classes).values());
    }


    /**
     * The classes a variable can take when it takes the given ones: the overlapping classes are merged into their
     * union, so that the classes are pairwise disjoint, as the classes of a variable have to be (a class present in its
     * binding holds all the values of the class, see
     * {@link VariableBinding#deriveMapping(ClassRelations, ResourceClass, Table)}), and every given class is a subclass
     * of one of them. The terms of a VALUES clause or of SERVICE results, classified one by one, may take overlapping
     * classes, e.g. a class and its subclass.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param classes the classes
     * @return the class each of the given classes is merged into, in the order of the given classes
     */
    public static Map<ResourceClass, ResourceClass> mergeOverlappingClasses(ClassRelations relations,
            Collection<ResourceClass> classes)
    {
        Set<ResourceClass> unions = new HashSet<>();

        for(ResourceClass resClass : classes)
        {
            Set<ResourceClass> group = new HashSet<>();
            group.add(resClass);

            for(ResourceClass other : unions)
                if(!ResourceClass.areDisjunct(relations, other, resClass))
                    group.add(other);

            unions.removeAll(group);
            unions.add(unionize(group));
        }

        Map<ResourceClass, ResourceClass> result = new LinkedHashMap<>();

        for(ResourceClass resClass : classes)
            result.put(resClass, unions.stream().filter(u -> resClass.isSubclassOf(u)).findFirst().orElseThrow());

        return result;
    }


    /**
     * Classes a variable can take in a union of the given children that is compared with the given join classes (see
     * {@link Restrictions#getJoinClasses}): the classes of {@link #unionResourceClasses(ClassRelations, List)}, where
     * the classes overlapping a join class are merged into their union, so that the join compares the class with a
     * single class of the union, in the columns of their common superclass, instead of a disjunction of pairs of
     * classes, which a database can neither hash nor merge. The classes overlapping a join class are counted before any
     * merge, as a merge keeps the overlaps of its members, so one pass over the join classes finds every group.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param defs bindings of the variable in the children where it is bound
     * @param joinClasses classes the variable is compared with by the joins above the union
     * @return classes a variable can take in a union of the given children, where the classes overlapping a join class
     *         are merged into their union
     */
    protected static Set<ResourceClass> unionResourceClasses(ClassRelations relations, List<VariableBinding> defs,
            Set<ResourceClass> joinClasses)
    {
        Set<ResourceClass> result = unionResourceClasses(relations, defs);

        for(ResourceClass joinClass : joinClasses)
        {
            Set<ResourceClass> group = result.stream().filter(c -> !ResourceClass.areDisjunct(relations, c, joinClass))
                    .collect(toSet());

            if(group.size() > 1)
            {
                result.removeAll(group);
                result.add(unionize(group));
            }
        }

        return result;
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
        ClassRelations relations = request.getConfiguration();

        Map<Column, Column> columnMap = new HashMap<>();

        VariableBindings bindings = new VariableBindings();

        for(Variable variable : allVars.stream().flatMap(v -> v.getVariables().stream()).collect(toSet()))
        {
            List<VariableBinding> vars = allVars.stream().map(v -> v.get(variable)).toList();
            List<VariableBinding> defs = vars.stream().filter(v -> v != null).toList();

            if(defs.stream().anyMatch(v -> !v.canBeNull()))
            {
                defs = defs.stream().filter(v -> !v.canBeNull()).toList();
                Set<ResourceClass> resClasses = joinResourceClasses(relations, defs);

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
                Set<ResourceClass> resClasses = unionResourceClasses(relations, defs,
                        restrictions == null ? Set.of() : restrictions.getJoinClasses(variable));
                VariableBinding binding = createVariableBinding(request, variable, resClasses, vars, tables, columnMap,
                        true);

                bindings.add(binding);
            }
        }

        columnMap.entrySet().forEach(e -> map.put(e.getValue(), e.getKey()));

        return bindings.restrict(relations, restrictions);
    }


    /**
     * Binding of one join variable: for an always-bound variable the columns of each class are taken from the children
     * binding it (null when constants conflict), otherwise the variants of all children are coalesced. A child that
     * cannot derive the columns of a class with the representability check (see
     * {@link VariableBinding#canDeriveMapping}) is left out when another child holds the class itself, whose columns
     * then tell whether the value belongs to the class.
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
        ClassRelations relations = request.getConfiguration();

        VariableBinding variableBinding = new VariableBinding(variable, canBeNull);

        if(!canBeNull)
        {
            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> mappings = new ArrayList<>();

                boolean held = vars.stream().anyMatch(v -> v != null && !v.canBeNull() && v.containsClass(resClass));

                for(int i = 0; i < vars.size(); i++)
                {
                    VariableBinding binding = vars.get(i);

                    if(binding == null || binding.canBeNull() || !binding.contains(relations, resClass))
                        continue;

                    // the holding child decides the class instead, the join condition makes their values equal
                    if(held && !binding.canDeriveMapping(relations, resClass))
                        continue;

                    mappings.add(binding.deriveMapping(relations, resClass, tables.get(i)));
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
                boolean materialised = true;

                for(int i = 0; i < vars.size(); i++)
                {
                    VariableBinding binding = vars.get(i);
                    Table table = tables.get(i);

                    if(binding == null)
                        continue;

                    for(ResourceClass specClass : binding.getCompatibleClasses(relations, resClass))
                    {
                        List<Column> columns = toTableColumns(table, binding.getMapping(specClass));

                        // a class the parent does not need has no columns in the children (nor in the join)
                        if(columns == null)
                            materialised = false;
                        else
                            variants.add(specClass.toGeneralClass(resClass, columns, true));
                    }
                }

                if(!materialised)
                {
                    assert variants.isEmpty();
                    variableBinding.addMapping(resClass, null);
                    continue;
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
                column = new ExpressionColumn(column.getCode(), column.getType(), false);

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
                    column = new TableColumn(output.get(i).getName(), output.get(i).getType(),
                            canBeNull || access.canBeNull());
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

        return IntStream.range(0, cols)
                .mapToObj(i -> (Column) new ExpressionColumn(variants.stream().map(l -> l.get(i).getCode()).distinct()
                        .sorted().collect(SqlCode.joining(", ", "COALESCE(", ")")),
                        variants.getFirst().get(i).getType()))
                .toList();
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
     * Bindings of the solution extending two compatible solutions whose columns are addressed already (through the
     * aliases of the inputs providing them): the bindings of the join of the two (see
     * {@link #getJoinVariableBindings(Request, List, List, Restrictions, Map)}) referring to the addressed columns
     * themselves instead of output columns of a join, in the classes that have columns. When the two solutions can
     * never be compatible, the first one is returned alone.
     *
     * @param request the current request
     * @param solution bindings of a solution
     * @param other bindings of the solution the first one is extended with
     * @return bindings of the solution extending both solutions
     */
    public static VariableBindings getExtendedSolution(Request request, VariableBindings solution,
            VariableBindings other)
    {
        Map<Column, Column> map = new HashMap<>();
        VariableBindings joined = getJoinVariableBindings(request, solution, other, null, null, null, map);

        if(joined.getVariables().isEmpty())
            return solution;

        VariableBindings result = new VariableBindings();

        for(VariableBinding binding : joined.getValues())
        {
            VariableBinding addressed = new VariableBinding(binding.getVariable(), binding.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                if(entry.getValue() != null)
                    addressed.addMapping(entry.getKey(),
                            entry.getValue().stream().map(c -> map.getOrDefault(c, c)).toList());

            if(addressed.hasMapping())
                result.add(addressed);
        }

        return result;
    }


    /**
     * True if the shared variables need no join condition: all are always bound to identical constant columns.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @return true if the shared variables need no join condition, false otherwise
     */
    public static boolean isJoinConditionAlwaysTrue(ClassRelations relations, VariableBindings left,
            VariableBindings right)
    {
        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, left, right))
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
     * Classes each variable takes in the join of children with the given bindings: for a variable two or more children
     * bind without a null, the classes of {@link #joinResourceClasses} over those children; no entry for the other
     * variables. A pair of classes of the variable that two of the children compare and that meets none of these
     * classes holds no solution of the whole join, as another child excludes the values, so the join condition need not
     * compare it (see
     * {@link #generateJoinCondition(ClassRelations, VariableBindings, VariableBindings, Table, Table, Map)}). A child
     * restricted in a join that was flattened into this one, or expanded with it, may even lack the columns of such a
     * class, as the child that excludes the class was its partner there.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param vars bindings of the children
     * @return classes per variable bound without a null by two or more of the children
     */
    public static Map<Variable, Set<ResourceClass>> getJoinedClasses(ClassRelations relations,
            List<VariableBindings> vars)
    {
        Map<Variable, Set<ResourceClass>> result = new HashMap<>();

        for(Variable variable : vars.stream().flatMap(v -> v.getVariables().stream()).collect(toSet()))
        {
            List<VariableBinding> defs = vars.stream().map(v -> v.get(variable))
                    .filter(v -> v != null && !v.canBeNull()).toList();

            if(defs.size() > 1)
                result.put(variable, joinResourceClasses(relations, defs));
        }

        return result;
    }


    /**
     * SQL condition joining every pair of children on their shared variables, or null if none is needed. Only the pairs
     * of classes that meet a class of the variable in the whole join are compared (see {@link #getJoinedClasses}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param vars the variables
     * @param tables the tables
     * @return SQL condition joining every pair of children on their shared variables, or null if none is needed
     */
    public static SqlCode generateJoinCondition(ClassRelations relations, List<VariableBindings> vars,
            List<? extends Table> tables)
    {
        int size = vars.size();
        Map<Variable, Set<ResourceClass>> joined = getJoinedClasses(relations, vars);

        List<SqlCode> conditions = IntStream.range(0, size)
                .mapToObj(i -> IntStream.range(i + 1, size).mapToObj(j -> generateJoinCondition(relations, vars.get(i),
                        vars.get(j), tables.get(i), tables.get(j), joined)))
                .flatMap(c -> c).filter(c -> c != null).toList();

        if(conditions.isEmpty())
            return null;

        return conditions.stream().collect(SqlCode.joining(" AND ", "(", ")"));
    }


    /**
     * SQL condition that every shared variable is compatible on both sides (equal values after conversion to a common
     * class, or unbound on either side), or null if it always holds.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @param leftTable the left table
     * @param rightTable the right table
     * @return SQL condition that every shared variable is compatible on both sides (equal values after conversion to a
     *         common class, or unbound on either side), or null if it always holds
     */
    public static SqlCode generateJoinCondition(ClassRelations relations, VariableBindings left, VariableBindings right,
            Table leftTable, Table rightTable)
    {
        return generateJoinCondition(relations, left, right, leftTable, rightTable, Map.of());
    }


    /**
     * SQL condition that every shared variable of two children of a join is compatible on both sides, comparing only
     * the pairs of classes that meet a class the variable takes in the whole join (see {@link #getJoinedClasses}), or
     * null if it always holds.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @param leftTable the left table
     * @param rightTable the right table
     * @param joined classes per variable in the whole join; a variable without an entry compares all its classes
     * @return SQL condition that every shared variable is compatible on both sides, or null if it always holds
     */
    public static SqlCode generateJoinCondition(ClassRelations relations, VariableBindings left, VariableBindings right,
            Table leftTable, Table rightTable, Map<Variable, Set<ResourceClass>> joined)
    {
        Set<SqlCode> join = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, left, right))
        {
            SqlCode condition = generateJoinCondition(relations, pair, leftTable, rightTable,
                    joined.get(pair.getVariable()));

            if(condition != null)
                join.add(condition);
        }

        if(join.isEmpty())
            return null;

        return join.stream().sorted().collect(SqlCode.joining(" AND ", "(", ")"));
    }


    /**
     * SQL condition that one variable is compatible on both sides, or null if it always holds (also when the variable
     * is bound on one side only).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param leftBinding the variable binding
     * @param rightBinding the variable binding
     * @param leftTable the left table
     * @param rightTable the right table
     * @return SQL condition that one variable is compatible on both sides, or null if it always holds
     */
    public static SqlCode generateJoinCondition(ClassRelations relations, VariableBinding leftBinding,
            VariableBinding rightBinding, Table leftTable, Table rightTable)
    {
        if(leftBinding == null || rightBinding == null)
            return null;

        return generateJoinCondition(relations, new VariableBindingPair(relations, leftBinding, rightBinding),
                leftTable, rightTable, null);
    }


    /**
     * SQL condition that the paired variable is compatible on both sides: unbound on either side, or equal values after
     * conversion to a common class. Null if the condition always holds, {@code (false)} if it never holds. When the
     * classes the variable takes in the whole join are given, a pair of classes meeting none of them is not compared,
     * as no solution of the join holds such a value (see {@link #getJoinedClasses}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param pair the paired bindings of the variable
     * @param leftTable the left table
     * @param rightTable the right table
     * @param joined classes the variable takes in the whole join, or null to compare all pairs of classes
     * @return SQL condition that the paired variable is compatible on both sides, or null if it always holds
     */
    private static SqlCode generateJoinCondition(ClassRelations relations, VariableBindingPair pair, Table leftTable,
            Table rightTable, Set<ResourceClass> joined)
    {
        VariableBinding leftBinding = pair.getLeftVariableBinding();
        VariableBinding rightBinding = pair.getRightVariableBinding();

        Set<SqlCode> condition = new HashSet<>();

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

            if(joined != null && joined.stream().allMatch(c -> ResourceClass.areDisjunct(relations, c, leftClass)
                    || ResourceClass.areDisjunct(relations, c, rightClass)))
                continue;

            ResourceClass unionClass = unionize(leftClass, rightClass);

            List<Column> leftCols = toTableColumns(leftTable, leftBinding.getMapping(leftClass));
            List<Column> rightCols = toTableColumns(rightTable, rightBinding.getMapping(rightClass));

            List<Column> genLeftCols = leftClass.toGeneralClass(unionClass, leftCols, false);
            List<Column> genRightCols = rightClass.toGeneralClass(unionClass, rightCols, false);

            Set<SqlCode> compare = new HashSet<>();

            for(int i = 0; i < unionClass.getColumnCount(); i++)
            {
                Column leftCol = genLeftCols.get(i);
                Column rightCol = genRightCols.get(i);
                SqlType type = unionClass.getSqlTypes().get(i);

                if(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn)
                {
                    if(!leftCol.equals(rightCol))
                        compare.add(SqlCode.FALSE);
                }
                else if(!unionClass.isOptionalColumn(i))
                {
                    compare.add(type.equal(leftCol, rightCol));
                }
                else if(leftCol instanceof NullColumn)
                {
                    compare.add(rightCol.canBeNull() ? SqlCode.of("(", rightCol, " IS NULL)") : SqlCode.FALSE);
                }
                else if(rightCol instanceof NullColumn)
                {
                    compare.add(leftCol.canBeNull() ? SqlCode.of("(", leftCol, " IS NULL)") : SqlCode.FALSE);
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
                compare.add(SqlCode.TRUE);

            if(!compare.contains(SqlCode.FALSE))
                condition.add(compare.stream().sorted().collect(SqlCode.joining(" AND ")));
        }

        if(condition.isEmpty())
            condition.add(SqlCode.FALSE);

        if(condition.contains(SqlCode.TRUE))
            return null;

        return condition.stream().sorted().collect(SqlCode.joining(" OR ", "(", ")"));
    }


    /**
     * SQL code of the conjunction of the conditions: their boolean columns joined by {@code AND}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param conditions the conditions
     * @return SQL code of the conjunction of the conditions
     */
    protected static SqlCode translateConditions(ClassRelations relations, List<SqlExpressionIntercode> conditions)
    {
        return conditions.stream().map(c -> c.get(relations, xsdBoolean).get(0)).collect(SqlCode.joining(" AND "));
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
     * True if every solution of the node binds the variable to the value of the expression, which is the same for all
     * the solutions (a value of the current solution the pattern is evaluated for, or a constant), and the node keeps
     * doing so however it is optimised later: a table access restricted to the value by its conditions (see
     * {@link SqlTableAccess}), or a node that takes the variable over from such a node. Merging the variable with the
     * value then changes nothing, so the merge is left out for good (see {@link SqlMerge}), and so is a condition of a
     * filter requiring the value (see {@link #getUnsatisfied}). A walk does not qualify, although it may start from the
     * value: it gives the start up when it is turned, when its siblings seed it from its other end or when it reduces
     * to its initial solutions (see {@link SqlRecursive}).
     *
     * @param variable the variable
     * @param value expression giving the value
     * @return true if every solution of the node binds the variable to the value of the expression, false otherwise
     */
    protected boolean isRestricted(Variable variable, SqlExpressionIntercode value)
    {
        return false;
    }


    /**
     * Collation of a column of the variable in the solutions as an SQL identifier, or null when the column uses the
     * default collation of the database or its collation is not known. Only a column of a database table brings another
     * collation in; the nodes that take the columns over from their children report the collations of the child columns
     * (see {@link #getCollation(Request, Collection, Variable, ResourceClass, int)}).
     *
     * @param request the current request
     * @param variable the variable
     * @param resClass a resource class of the variable
     * @param index position of the column among the columns of the class
     * @return collation of the column as an SQL identifier, or null when the column uses the default collation of the
     *         database or its collation is not known
     */
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return null;
    }


    /**
     * Collation of a column of the variable as the given children deliver it, combined the way PostgreSQL combines the
     * implicit collations of the inputs of an expression: the default collation gives way to any other one, and two
     * different other ones have no common collation. A child binding the variable in the class delivers its column of
     * the class; when the classes of the child are converted to the class instead, a character column of the class may
     * come from any of their columns.
     *
     * @param request the current request
     * @param childs the child nodes
     * @param variable the variable
     * @param resClass a resource class of the variable
     * @param index position of the column among the columns of the class
     * @return the common collation as an SQL identifier, or null when the children deliver the default collation, their
     *         collations are not known, or they differ
     */
    protected static String getCollation(Request request, Collection<? extends SqlIntercode> childs, Variable variable,
            ResourceClass resClass, int index)
    {
        ClassRelations relations = request.getConfiguration();
        SqlType type = resClass.getSqlTypes().get(index);
        Set<String> collations = new HashSet<>();

        for(SqlIntercode child : childs)
        {
            VariableBinding binding = child.getVariable(variable);

            if(binding == null)
                continue;

            if(binding.containsClass(resClass))
                collations.add(child.getCollation(request, variable, resClass, index));
            else if(type.equals(SqlType.VARCHAR) || type.equals(SqlType.TEXT))
                for(ResourceClass childClass : binding.getCompatibleClasses(relations, resClass))
                    for(int i = 0; i < childClass.getColumnCount(); i++)
                        collations.add(child.getCollation(request, variable, childClass, i));
        }

        collations.remove(null);

        return collations.size() == 1 ? collations.iterator().next() : null;
    }


    /**
     * Restrictions for a join child on top of the parent's: its variables shared with the other side are needed in the
     * classes compatible with that side (in all classes when the variable may be unbound).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param bindings the variable bindings
     * @param other bindings of the other side
     * @param restrictions what the parent needs of the variables
     * @param filtered true if the join drops the solutions of the child it cannot join with the other side, so that the
     *            classes the other side binds without a null are the accepted classes of the shared variables (see
     *            {@link Restrictions#getAcceptedClasses}); false on the side of a left join or a minus whose solutions
     *            survive without a partner
     * @return restrictions for a join child on top of the parent's: its variables shared with the other side are needed
     *         in the classes compatible with that side (in all classes when the variable may be unbound)
     */
    public static Restrictions getJoinRestrictions(ClassRelations relations, VariableBindings bindings,
            VariableBindings other, Restrictions restrictions, boolean filtered)
    {
        return getJoinRestrictions(relations, bindings, Set.of(other), restrictions, filtered);
    }


    /**
     * Restrictions for a join child against several other children, see
     * {@link #getJoinRestrictions(ClassRelations, VariableBindings, VariableBindings, Restrictions, boolean)}.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param bindings the variable bindings
     * @param others bindings of the other children
     * @param restrictions what the parent needs of the variables
     * @param filtered true if the join drops the solutions of the child it cannot join with the other children
     * @return restrictions for a join child against several other children, see
     *         {@link #getJoinRestrictions(ClassRelations, VariableBindings, VariableBindings, Restrictions, boolean)};
     *         the classes of the other children are the join classes of the shared variables (see
     *         {@link Restrictions#getJoinClasses}) and, when the join is filtered, those bound without a null are their
     *         accepted classes (see {@link Restrictions#getAcceptedClasses})
     */
    protected static Restrictions getJoinRestrictions(ClassRelations relations, VariableBindings bindings,
            Collection<VariableBindings> others, Restrictions restrictions, boolean filtered)
    {
        return getJoinRestrictions(relations, bindings, others, restrictions, filtered, Map.of());
    }


    /**
     * Restrictions for a join child against several other children whose optimisation loop has settled classes (see
     * {@link Settlement}): the restrictions of
     * {@link #getJoinRestrictions(ClassRelations, VariableBindings, Collection, Restrictions, boolean)}, where the
     * settled classes of a shared variable count as the classes of one more side that may leave the variable unbound.
     * They are join classes of the variable, and the child needs the variable in every class overlapping one of them,
     * so that a class a presentation merges for a settled class always keeps its columns; they are no accepted classes,
     * which the sides themselves give.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param bindings the variable bindings
     * @param others bindings of the other children
     * @param restrictions what the parent needs of the variables
     * @param filtered true if the join drops the solutions of the child it cannot join with the other children
     * @param settled settled classes per variable, see {@link Settlement#getClasses}
     * @return restrictions for a join child against several other children whose optimisation loop has settled classes
     */
    protected static Restrictions getJoinRestrictions(ClassRelations relations, VariableBindings bindings,
            Collection<VariableBindings> others, Restrictions restrictions, boolean filtered,
            Map<Variable, Set<ResourceClass>> settled)
    {
        Restrictions joinRestrictions = new Restrictions();

        for(VariableBinding binding : bindings.getValues())
        {
            Variable variable = binding.getVariable();

            Set<VariableBinding> vars = others.stream().map(v -> v.get(variable)).filter(v -> v != null)
                    .collect(toSet());

            if(!vars.isEmpty())
            {
                Set<ResourceClass> settledClasses = settled.getOrDefault(variable, Set.of());

                for(VariableBinding v : vars)
                    joinRestrictions.addJoinClasses(variable, v.getClasses());

                if(!settledClasses.isEmpty())
                    joinRestrictions.addJoinClasses(variable, settledClasses);

                // a side that may leave the variable unbound joins a value of any class
                if(filtered)
                    for(VariableBinding v : vars)
                        if(!v.canBeNull())
                            joinRestrictions.addAcceptedClasses(variable, v.getClasses());

                if(binding.canBeNull())
                {
                    joinRestrictions.set(variable, binding.getMappings().keySet());
                }
                else
                {
                    Set<ResourceClass> set = new HashSet<>();

                    for(ResourceClass rc : binding.getMappings().keySet())
                        if(vars.stream().anyMatch(v -> !ResourceClass.areDisjunct(relations, rc, v.getClasses()))
                                || !ResourceClass.areDisjunct(relations, rc, settledClasses))
                            set.add(rc);

                    joinRestrictions.set(variable, set);
                }
            }
        }

        return new Restrictions(joinRestrictions, restrictions);
    }


    /**
     * Adds the classes in which the conditions can hold as the accepted classes of their variables (see
     * {@link Restrictions#getAcceptedClasses}): a solution binding a variable to a value of a class under which some
     * condition folds to false or to an error never satisfies the conditions, so the node holding the solutions may
     * leave it out. Every class of the binding of a variable is tried alone, with the variable bound (a class without
     * columns, because it is not materialised or the binding belongs to a call not evaluated yet, over columns created
     * for the trial), and a variable accepted in all its classes gets no entry. Remote SERVICE calls inside the
     * conditions are not evaluated by the trial.
     *
     * @param request the current request
     * @param scope the scope of the node holding the conditions
     * @param restrictions the restrictions to extend
     * @param conditions the conditions, all of which have to hold
     * @param bindings the variable bindings the conditions are evaluated under
     */
    protected static void addAcceptedClasses(Request request, Scope scope, Restrictions restrictions,
            Collection<SqlExpressionIntercode> conditions, VariableBindings bindings)
    {
        for(SqlExpressionIntercode condition : conditions)
        {
            for(Variable variable : condition.getReferencedVariables())
            {
                VariableBinding binding = bindings.get(variable);

                if(binding == null || binding.getClasses().size() < 2)
                    continue;

                Set<ResourceClass> accepted = new HashSet<>();

                for(ResourceClass resClass : binding.getClasses())
                {
                    List<Column> columns = binding.getMapping(resClass);

                    if(columns == null)
                        columns = resClass.createColumns(request.getColumnMap(), variable);

                    // the trial asks about the solutions binding the variable, so the variable is bound in it
                    VariableBindings trial = new VariableBindings(bindings);
                    trial.add(new VariableBinding(variable, resClass, columns, false));

                    SqlExpressionIntercode expression = condition.optimize(request, scope, trial,
                            new Restriction(xsdBoolean), false);

                    if(!expression.equals(SqlNull.get()) && !expression.equals(falseValue)
                            && !(expression instanceof SqlBooleanExpression e && e.isFalseOrError()))
                        accepted.add(resClass);
                }

                if(accepted.size() < binding.getClasses().size())
                    restrictions.addAcceptedClasses(variable, accepted);
            }
        }
    }


    /**
     * A term a condition requires of a variable (see {@link #getRequiredTerm}).
     *
     * @param variable the variable
     * @param value constant expression giving the term, with the columns of all its classes
     */
    protected record RequiredTerm(Variable variable, SqlExpressionIntercode value)
    {
    }


    /**
     * The term the condition requires of a variable, or null when it requires none. A condition requires a term when it
     * holds only for the solutions binding the variable to that term: {@code sameTerm} of the variable and a constant
     * says so, and so does an equality of the variable and a constant IRI, which is equal to no other term. An equality
     * with a literal may hold for the other literals of its value too, so it requires a term only where the classes the
     * variable takes single one out (see {@link SqlBinaryComparison#getOnlyEqualTerm}): the literal itself when it is a
     * string, or the literal of its value in the datatype of the column when the variable is a number of one datatype.
     *
     * @param request the current request
     * @param condition the condition
     * @param bindings the variable bindings giving the classes the variables of the condition take
     * @return the term the condition requires of a variable, or null when it requires none
     */
    protected static RequiredTerm getRequiredTerm(Request request, SqlExpressionIntercode condition,
            VariableBindings bindings)
    {
        // the effective boolean value of a comparison is the comparison itself
        if(condition instanceof SqlEffectiveBooleanValue value)
            return getRequiredTerm(request, value.getOperand(), bindings);

        if(condition instanceof SqlBuiltinCall call && call.getFunction().equals("sameterm"))
            return getRequiredTerm(request, call.getArguments().get(0), call.getArguments().get(1), true, bindings);

        if(condition instanceof SqlBinaryComparison comparison && comparison.getOperator() == ComparisonOperator.EQUAL)
            return getRequiredTerm(request, comparison.getLeft(), comparison.getRight(), false, bindings);

        return null;
    }


    /**
     * The term a comparison of two operands requires of a variable, or null when it requires none: one of the operands
     * is a variable and the other one a constant IRI, or any constant when the comparison is the identity of terms, or
     * a literal that only one term the variable can be is equal to.
     *
     * @param request the current request
     * @param left one operand
     * @param right the other operand
     * @param identity true for the identity of terms ({@code sameTerm}), false for the equality of values
     * @param bindings the variable bindings giving the classes the variable takes
     * @return the term the comparison requires of a variable, or null when it requires none
     */
    private static RequiredTerm getRequiredTerm(Request request, SqlExpressionIntercode left,
            SqlExpressionIntercode right, boolean identity, VariableBindings bindings)
    {
        if(!(left instanceof SqlVariable variable))
            return right instanceof SqlVariable ? getRequiredTerm(request, right, left, identity, bindings) : null;

        if(right instanceof SqlIri iri)
            return new RequiredTerm(variable.getVariable(), SqlIri.create(request, iri.getIri()));

        if(!(right instanceof SqlLiteral literal))
            return null;

        if(identity)
            return new RequiredTerm(variable.getVariable(), SqlLiteral.create(request, literal.getLiteral()));

        VariableBinding binding = bindings.get(variable.getVariable());
        Literal term = SqlBinaryComparison.getOnlyEqualTerm(request, binding == null ? Set.of() : binding.getClasses(),
                literal.getLiteral());

        return term == null ? null : new RequiredTerm(variable.getVariable(), SqlLiteral.create(request, term));
    }


    /**
     * Adds the terms the conditions require of their variables (see {@link #getRequiredTerm}) as the accepted values of
     * the variables (see {@link Restrictions#getAcceptedValue}): a solution binding a variable to another term than the
     * one a condition requires of it never satisfies the conditions, so the node holding the solutions may leave it
     * out, and a table access binding the variable takes the term into its conditions (see {@link SqlTableAccess}). The
     * conjuncts of a conjunction count by themselves, as all of them have to hold. The classes of the variables are
     * read from the bindings of the node as built, which still carry every class, like the accepted classes are (see
     * {@link #addAcceptedClasses}).
     *
     * @param request the current request
     * @param restrictions the restrictions to extend
     * @param conditions the conditions, all of which have to hold
     * @param bindings the variable bindings of the node holding the solutions, as built
     */
    protected static void addAcceptedValues(Request request, Restrictions restrictions,
            Collection<SqlExpressionIntercode> conditions, VariableBindings bindings)
    {
        for(SqlExpressionIntercode condition : conditions)
        {
            // the effective boolean value of a conjunction is the conjunction itself
            SqlExpressionIntercode operand = condition instanceof SqlEffectiveBooleanValue value ? value.getOperand() :
                    condition;

            if(operand instanceof SqlBinaryLogical logical && logical.getOperator() == LogicalOperator.AND)
            {
                addAcceptedValues(request, restrictions, List.of(logical.getLeft(), logical.getRight()), bindings);
                continue;
            }

            RequiredTerm term = getRequiredTerm(request, condition, bindings);

            if(term != null)
                restrictions.setAcceptedValue(term.variable(), term.value());
        }
    }


    /**
     * The condition without the conjuncts the node satisfies by itself: a conjunct requiring a term of a variable (see
     * {@link #getRequiredTerm}) holds for every solution of a node that binds the variable to the term for good (see
     * {@link #isRestricted}), because its table accesses have taken the term into their conditions (see
     * {@link #addAcceptedValues}). The condition itself when the node satisfies no conjunct of it; otherwise the
     * conjuncts left, all of which have to hold.
     *
     * @param request the current request
     * @param condition the condition
     * @param node the node whose solutions the condition is evaluated for
     * @return the condition without the conjuncts the node satisfies by itself
     */
    protected static List<SqlExpressionIntercode> getUnsatisfied(Request request, SqlExpressionIntercode condition,
            SqlIntercode node)
    {
        List<SqlExpressionIntercode> conjuncts = getConjuncts(List.of(condition));
        List<SqlExpressionIntercode> kept = new ArrayList<>();

        for(SqlExpressionIntercode conjunct : conjuncts)
        {
            RequiredTerm term = getRequiredTerm(request, conjunct, node.getVariableBindings());

            if(term == null || !node.isRestricted(term.variable(), term.value()))
                kept.add(conjunct);
        }

        return kept.size() == conjuncts.size() ? List.of(condition) : kept;
    }


    /**
     * False if a variable is always bound in several children with disjoint classes or different constants, so the join
     * is empty.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param childs the child nodes
     * @return false if a variable is always bound in several children with disjoint classes or different constants, so
     *         the join is empty, true otherwise
     */
    protected static boolean isJoinable(ClassRelations relations, List<SqlIntercode> childs)
    {
        List<VariableBindings> allVars = childs.stream().map(c -> c.getVariableBindings()).toList();

        for(Variable variable : allVars.stream().flatMap(v -> v.getVariables().stream()).collect(toSet()))
        {
            List<VariableBinding> uvars = allVars.stream().map(v -> v.get(variable))
                    .filter(v -> v != null && !v.canBeNull()).toList();

            if(uvars.size() > 1)
            {
                Set<ResourceClass> resClasses = joinResourceClasses(relations, uvars);

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

                        // a child restricted in a join that was flattened into this one, or expanded with it, lacks the
                        // columns of a class another child of that join excludes (see getJoinedClasses), which a part
                        // of the children (see SqlJoin.expandJoin) may hold without the excluding child
                        if(cols == null)
                            continue;

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
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @return false if some shared variable can never match between a join component of the left and a join component
     *         of the right subtree (see {@link VariableBindingPair#isJoinable}), true otherwise
     */
    protected static boolean isJoinable(ClassRelations relations, SqlIntercode left, SqlIntercode right)
    {
        for(SqlIntercode l : getJoinList(left))
        {
            for(SqlIntercode r : getJoinList(right))
            {
                List<VariableBindingPair> pairs = VariableBindingPair.getPairs(relations, l.getVariableBindings(),
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
