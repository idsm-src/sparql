package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.InputTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.Scope;
import cz.iocb.sparql.engine.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.imcode.SqlFilter;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.DerivedClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.NodeTerm;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * EXISTS and NOT EXISTS: true if the pattern, evaluated for the current solution of the surrounding bindings, has a
 * solution. Following SPARQL 1.2, every solution of the pattern extends the current solution, but no node of the
 * pattern stands for it: the test compares the solutions of the pattern with the current solution in its own condition,
 * a form the database turns into a semi-join when the pattern refers to the current solution nowhere else, and the
 * expressions and the nested patterns that need a value of the current solution refer to the columns of the input of
 * the node holding the test directly (see {@link SqlOuterVariable}). A filter at the top of the pattern that refers to
 * the current solution is evaluated in the condition of the test as well (see {@link #translate}), so that it does not
 * make the pattern refer to it. A pattern that refers to the current solution all the same is evaluated for every
 * current solution anew, so the test evaluates it with the values of the current solution in its table accesses (see
 * {@link #getValues}), which the database can then look up by the indexes of the tables. The current solution is read
 * from the surrounding bindings on every optimisation (see {@link #getCurrentSolution}), so the pattern is optimised
 * for the classes the node holding the test provides at the moment, as a join with them would optimise it. Bindings
 * that no longer bind a variable they bound always when the pattern was translated are those of a node left without a
 * solution; the pattern, whose nodes are translated for the values of such variables, is not optimised for them. A test
 * of a pattern independent of the surrounding bindings has no current solution of its own. A pattern holding a SERVICE
 * call not evaluated yet has no SQL; the expression then carries a placeholder column until the second optimisation
 * pass evaluates the call and rebuilds the expression. With the join optimizer on, a filter whose condition the test is
 * chooses, when it is translated, whether the database joins the pattern with the solutions of the filter or the
 * pattern is evaluated for each of them, and writes the test anew in the latter case (see
 * {@link #translate(Request, Scope, SqlCode)}). Where the pattern shares a node of a join mapping with the solutions it
 * is tested for, and the node gives the term of a variable both ways, the test compares the node in place of the
 * variable (see {@link #getComparedVariables}), so that nothing reads the terms on account of the test.
 */
public final class SqlExists extends SqlExpressionIntercode
{
    /**
     * Alias of the pattern.
     */
    private static final AliasTable patternTable = new AliasTable("tab");

    /**
     * Column standing for the test while its pattern holds a SERVICE call not evaluated yet. It never reaches the
     * database, as the second optimisation pass rebuilds the expression once the call is evaluated, and would fail
     * there otherwise.
     */
    private static final Column unevaluated = new ExpressionColumn(new SqlCode("sparql.unevaluated_service()"), BOOL,
            false);

    /**
     * Upper bound of the rounds in which the pattern is optimised for a current solution (see {@link #optimize}).
     */
    private static final int maxRounds = 8;

    /**
     * True for NOT EXISTS.
     */
    private final boolean negated;

    /**
     * Tested pattern, optimised for the current solution {@link #solution}.
     */
    private final SqlIntercode pattern;

    /**
     * The pattern as the test evaluates it, from which the SQL of the test is rendered: the tested pattern whose table
     * accesses have taken the values of the current solution that the pattern would not be the first to read (see
     * {@link #getValues}). It is derived from the tested pattern on every optimisation again, so the tested pattern
     * itself stays free of those values and is tested as it is once nothing in it reads the current solution.
     */
    private final SqlIntercode evaluated;

    /**
     * The pattern as translated, from which the optimisation starts again when the surrounding bindings provide a
     * current solution the optimised pattern does not serve (see {@link #isNarrower}).
     */
    private final SqlIntercode origin;

    /**
     * Bindings of the surrounding solution.
     */
    private final VariableBindings bindings;

    /**
     * The variables of the current solution the pattern may refer to, and the nodes that may stand in for some of them
     * in the test (see {@link #getComparedVariables}).
     */
    private final Set<Variable> variables;

    /**
     * The variables a node stands in for in the test (see {@link #getReplacedVariables}): the surrounding solutions are
     * not asked for them on account of the test.
     */
    private final Set<Variable> replaced;

    /**
     * True when the solutions of the node holding the expression extend the current solution of the pattern the node
     * lies in (a node of a group of an EXISTS pattern), which the current solution of this pattern then extends too;
     * false for a node above the WHERE clause of a sub-select, whose solutions hold only what the WHERE clause, the
     * grouping and the projection leave in them.
     */
    private final boolean chained;

    /**
     * Bindings of the current solution the pattern was built for (see {@link #getCurrentSolution}); null for a pattern
     * independent of the surrounding bindings, which has no current solution of its own.
     */
    private final VariableBindings solution;

    /**
     * The variables of the current solution that the surrounding bindings bound always when the pattern was translated.
     * The nodes of the pattern rely on their values (a merge with such a value binds its variable only in the classes
     * of the value), so the pattern is optimised only for bindings that still bind them always (see {@link #optimize}).
     */
    private final Set<Variable> bound;

    /**
     * True when the expression was built by the optimisation.
     */
    private final boolean optimized;


    /**
     * Creates the expression; it is deterministic if the pattern is.
     *
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param evaluated the pattern as the test evaluates it
     * @param origin the pattern as translated
     * @param mappings columns per resource class
     * @param bindings the variable bindings
     * @param variables the variables of the current solution the pattern may refer to
     * @param replaced the variables a node stands in for in the test
     * @param chained whether the current solution extends the current solution of the pattern the node holding the
     *            expression lies in
     * @param solution bindings of the current solution the pattern was built for, or null
     * @param bound the variables of the current solution the surrounding bindings bound always when the pattern was
     *            translated
     * @param optimized whether the expression was built by the optimisation
     */
    private SqlExists(boolean negated, SqlIntercode pattern, SqlIntercode evaluated, SqlIntercode origin,
            Map<ResourceClass, List<Column>> mappings, VariableBindings bindings, Set<Variable> variables,
            Set<Variable> replaced, boolean chained, VariableBindings solution, Set<Variable> bound, boolean optimized)
    {
        super(mappings, false, pattern.isDeterministic());

        this.negated = negated;
        this.pattern = pattern;
        this.evaluated = evaluated;
        this.origin = origin;
        this.bindings = bindings;
        this.variables = variables;
        this.replaced = replaced;
        this.chained = chained;
        this.solution = solution;
        this.bound = bound;
        this.optimized = optimized;

        if(solution != null)
            this.referencedVariables.addAll(solution.getVariables());
    }


    /**
     * Existence test of a pattern independent of the surrounding bindings. The pattern has no current solution of its
     * own; when the test lies in the pattern of another test, the nodes of its pattern may still read the current
     * solution of that pattern (see {@link Scope#enterPattern}).
     *
     * @param request the current request
     * @param scope the scope of the node holding the test
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @return existence test of a pattern independent of the surrounding bindings
     */
    public static SqlExpressionIntercode create(Request request, Scope scope, boolean negated, SqlIntercode pattern,
            VariableBindings bindings)
    {
        return create(request, scope.enterPattern(null), negated, pattern, pattern, null, pattern, bindings, Set.of(),
                Set.of(), false, null, Set.of(), Restriction.ALL, false);
    }


    /**
     * Existence test of a pattern evaluated for the current solution of the surrounding bindings: the pattern was
     * translated for the current solution with the given bindings, in the scope its SQL is rendered in here (see
     * {@link Scope#enterPattern}).
     *
     * @param request the current request
     * @param scope the scope of the node holding the test
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @param variables the variables of the current solution the pattern may refer to
     * @param chained whether the current solution extends the current solution of the pattern the node holding the
     *            expression lies in
     * @param solution bindings of the current solution the pattern was translated for (see {@link #getCurrentSolution})
     * @return existence test of a pattern evaluated for the current solution of the surrounding bindings
     */
    public static SqlExpressionIntercode create(Request request, Scope scope, boolean negated, SqlIntercode pattern,
            VariableBindings bindings, Set<Variable> variables, boolean chained, VariableBindings solution)
    {
        Set<Variable> bound = new HashSet<>();

        for(Variable variable : variables)
            if(bindings.get(variable) != null && !bindings.get(variable).canBeNull())
                bound.add(variable);

        return create(request, scope.enterPattern(solution), negated, pattern, pattern, null, pattern, bindings,
                variables, getReplacedVariables(request, pattern, bindings, variables), chained, solution, bound,
                Restriction.ALL, false);
    }


    /**
     * Existence test materialising only when needed; a pattern without solutions or with the single empty solution
     * makes it constant, and so does a pattern none of whose solutions can agree with the current solution.
     *
     * @param request the current request
     * @param scope the scope of the pattern
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param evaluated the pattern as the test evaluates it
     * @param sql SQL code of the solutions the test reads from the evaluated pattern (see {@link #translate}) when it
     *            has been rendered already, or null
     * @param origin the pattern as translated
     * @param bindings the variable bindings
     * @param variables the variables of the current solution the pattern may refer to
     * @param replaced the variables a node stands in for in the test
     * @param chained whether the current solution extends the current solution of the pattern the node holding the
     *            expression lies in
     * @param solution bindings of the current solution the pattern was built for, or null
     * @param bound the variables of the current solution the surrounding bindings bound always when the pattern was
     *            translated
     * @param restriction the result classes the parent needs
     * @param optimized whether the expression is built by the optimisation
     * @return existence test materialising only when needed
     */
    private static SqlExpressionIntercode create(Request request, Scope scope, boolean negated, SqlIntercode pattern,
            SqlIntercode evaluated, SqlCode sql, SqlIntercode origin, VariableBindings bindings,
            Set<Variable> variables, Set<Variable> replaced, boolean chained, VariableBindings solution,
            Set<Variable> bound, Restriction restriction, boolean optimized)
    {
        ClassRelations relations = request.getConfiguration();

        if(evaluated.equals(SqlNoSolution.get()))
            return negated ? trueValue : falseValue;

        if(evaluated.equals(SqlEmptySolution.get()))
            return negated ? falseValue : trueValue;

        //NOTE: a solution of the pattern agrees with the current solution on the shared variables
        if(solution != null && VariableBindingPair.getPairs(relations, evaluated.getVariableBindings(), solution)
                .stream().anyMatch(p -> !p.isJoinable()))
            return negated ? trueValue : falseValue;


        if(!restriction.contains(relations, xsdBoolean))
            return new SqlExists(negated, pattern, evaluated, origin, singletonMap(xsdBoolean, null), bindings,
                    variables, replaced, chained, solution, bound, optimized);

        // the call is evaluated by the second optimisation pass, which rebuilds the expression and everything built
        // on it; until then a placeholder stands for the test
        if(pattern.hasServiceSubpattern())
            return new SqlExists(negated, pattern, evaluated, origin, singletonMap(xsdBoolean, List.of(unevaluated)),
                    bindings, variables, replaced, chained, solution, bound, optimized);


        // the SQL of the test is rendered at once, as the column of the expression, which keeps what the code reads
        SqlCode test = translate(request, scope, negated, evaluated, solution, sql);
        List<Column> result = List.of(new ExpressionColumn(test, BOOL, false));

        return new SqlExists(negated, pattern, evaluated, origin, singletonMap(xsdBoolean, result), bindings, variables,
                replaced, chained, solution, bound, optimized);
    }


    /**
     * Bindings of the current solution a pattern of an expression is evaluated for, given the bindings of the solutions
     * the expression is evaluated for: the variables the pattern may refer to, in the classes that have columns, the
     * columns addressed through the alias of the input of the node holding the expression (see
     * {@link Scope#getInputTable}; the bindings of the sides of a left join are addressed already). When the node lies
     * in a pattern of another expression and its solutions extend the current solution of that pattern, the solution is
     * joined with it, so that a variable only that one binds is known to the nested pattern too. The scope of the
     * pattern is entered with the result (see {@link Scope#enterPattern}).
     *
     * @param request the current request
     * @param scope the scope of the node holding the expression
     * @param bindings bindings of the solutions the expression is evaluated for
     * @param variables the variables of the current solution the pattern may refer to
     * @param chained whether the solutions the expression is evaluated for extend the current solution of the pattern
     *            the node holding the expression lies in
     * @return bindings of the current solution
     */
    public static VariableBindings getCurrentSolution(Request request, Scope scope, VariableBindings bindings,
            Set<Variable> variables, boolean chained)
    {
        InputTable table = scope.getInputTable();

        VariableBindings own = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
        {
            if(!variables.contains(binding.getVariable()))
                continue;

            VariableBinding addressed = new VariableBinding(binding.getVariable(), binding.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                if(entry.getValue() != null)
                    addressed.addMapping(entry.getKey(), entry.getValue().stream()
                            .map(c -> c instanceof TableColumn ? c.fromTable(table) : c).toList());

            if(addressed.hasMapping())
                own.add(addressed);
        }

        VariableBindings enclosing = chained ? scope.getCurrentSolution() : null;

        if(enclosing == null)
            return own;

        VariableBindings outer = new VariableBindings();

        for(VariableBinding binding : enclosing.getValues())
            if(variables.contains(binding.getVariable()))
                outer.add(binding);

        if(outer.getVariables().isEmpty())
            return own;

        return SqlIntercode.getExtendedSolution(request, own, outer);
    }


    /**
     * The variables that a node stands in for in the test: a node among the given variables that both the pattern and
     * the surrounding bindings bind always, and that gives the term of its variable both ways (see
     * {@link NodeTerm#determining}), agrees in a solution of the pattern and in the current solution exactly when the
     * variable does, so the test compares the node and not the variable, and nothing has to read the term for it.
     *
     * @param request the current request
     * @param pattern the tested pattern, or null when it is not known yet
     * @param bindings bindings of the solutions the expression is evaluated for
     * @param variables the variables of the current solution the pattern may refer to, with the nodes that may stand in
     *            for some of them
     * @return the variables that a node stands in for in the test
     */
    private static Set<Variable> getReplacedVariables(Request request, SqlIntercode pattern, VariableBindings bindings,
            Set<Variable> variables)
    {
        Set<Variable> replaced = new HashSet<>();

        if(pattern == null)
            return replaced;

        for(Variable node : variables)
        {
            NodeTerm term = request.getNodeTerms().get(node);
            VariableBinding outer = bindings.get(node);
            VariableBinding inner = pattern.getVariable(node);

            if(term != null && term.determining() && variables.contains(term.variable()) && outer != null
                    && !outer.canBeNull() && inner != null && !inner.canBeNull())
                replaced.add(term.variable());
        }

        return replaced;
    }


    /**
     * The variables the current solution of the test holds, out of the given ones: the variables the pattern may refer
     * to but for those a node stands in for (see {@link #getReplacedVariables}), and the nodes standing in for them; a
     * node that stands in for no variable is no part of a solution.
     *
     * @param request the current request
     * @param pattern the tested pattern, or null when it is not known yet
     * @param bindings bindings of the solutions the expression is evaluated for
     * @param variables the variables of the current solution the pattern may refer to, with the nodes that may stand in
     *            for some of them
     * @return the variables the current solution of the test holds
     */
    public static Set<Variable> getComparedVariables(Request request, SqlIntercode pattern, VariableBindings bindings,
            Set<Variable> variables)
    {
        Set<Variable> replaced = getReplacedVariables(request, pattern, bindings, variables);
        Set<Variable> compared = new HashSet<>();

        for(Variable variable : variables)
        {
            NodeTerm term = request.getNodeTerms().get(variable);

            if(term == null ? !replaced.contains(variable) : replaced.contains(term.variable()))
                compared.add(variable);
        }

        return compared;
    }


    @Override
    public Restrictions getRequirements(ClassRelations relations)
    {
        // the current solution is read from the surrounding bindings on every optimisation, so they have to keep every
        // variable the pattern may refer to in all its classes, also one bound to constants only; a variable a node
        // stands in for is not read
        return new Restrictions(variables.stream().filter(v -> !replaced.contains(v)).toList());
    }


    /**
     * True if a pattern optimised for one current solution serves another one too: the other one binds no variable the
     * first one leaves unbound, leaves unbound no variable the first one binds always, and binds no variable in a class
     * or to a constant the first one does not. The optimisation only narrows a pattern (its filters fold for the
     * classes of the current solution, its branches that cannot agree with the solution are left out), so a narrower
     * solution is served by narrowing the pattern further, whereas a wider one needs the pattern as translated.
     *
     * @param before bindings of the current solution the pattern was optimised for
     * @param now bindings of the other current solution
     * @return true if the other current solution is not wider than the one the pattern was optimised for, false
     *         otherwise
     */
    private static boolean isNarrower(VariableBindings before, VariableBindings now)
    {
        for(VariableBinding binding : before.getValues())
            if(now.get(binding.getVariable()) == null && !binding.canBeNull())
                return false;

        for(VariableBinding binding : now.getValues())
        {
            VariableBinding former = before.get(binding.getVariable());

            if(former == null || binding.canBeNull() && !former.canBeNull())
                return false;

            ResourceClass union = DerivedClass.unionize(former.getClasses());

            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
            {
                if(!entry.getKey().isSubclassOf(union))
                    return false;

                List<Column> columns = former.getMapping(entry.getKey());

                if(columns == null || entry.getValue() == null)
                    continue;

                for(int i = 0; i < columns.size(); i++)
                    if(columns.get(i) instanceof ConstantColumn && !columns.get(i).equals(entry.getValue().get(i)))
                        return false;
            }
        }

        return true;
    }


    /**
     * True if the bindings of the solutions the expression is evaluated for bind one of the compared variables always
     * in classes disjoint with those the current solution of the pattern the node holding the expression lies in binds
     * it always (see {@link Scope#getCurrentSolution}). Every solution of a node inside a pattern extends the current
     * solution of the pattern, so such bindings are the bindings of no solution, like the trial bindings of
     * {@link SqlIntercode#addAcceptedClasses} binding a variable in a class that the current solution excludes; the
     * current solution of the test would not extend the current solution of the pattern (see
     * {@link #getCurrentSolution}), and the pattern, optimised for its earlier current solutions, would be compared
     * with it in classes it holds without columns.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param scope the scope of the node holding the expression
     * @param bindings bindings of the solutions the expression is evaluated for
     * @param variables the variables the test compares
     * @return true if the bindings bind a compared variable always in classes disjoint with those the current solution
     *         of the pattern the node lies in binds it always, false otherwise
     */
    private static boolean excludesEnclosingSolution(ClassRelations relations, Scope scope, VariableBindings bindings,
            Set<Variable> variables)
    {
        VariableBindings enclosing = scope.getCurrentSolution();

        if(enclosing == null)
            return false;

        for(Variable variable : variables)
        {
            VariableBinding own = bindings.get(variable);
            VariableBinding other = enclosing.get(variable);

            if(own != null && other != null && !own.canBeNull() && !other.canBeNull()
                    && ResourceClass.areDisjunct(relations, own.getClasses(), other.getClasses()))
                return true;
        }

        return false;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, Scope scope, VariableBindings bindings,
            Restriction restriction, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        // the optimisation only narrows the bindings of a node, so a variable they bound always is left out, or left
        // possibly unbound, only by the bindings of a node that has no solution any more; the test is then evaluated
        // for nothing and stays as it is, the nodes of its pattern being translated for the values of those variables
        Set<Variable> replaced = getReplacedVariables(request, pattern, bindings, variables);

        for(Variable variable : bound)
            if(!replaced.contains(variable) && (bindings.get(variable) == null || bindings.get(variable).canBeNull()))
                return this;

        Set<Variable> compared = getComparedVariables(request, pattern, bindings, variables);

        // surrounding bindings that cannot extend the current solution of the pattern the node holding the expression
        // lies in are the bindings of no solution: the test is evaluated for nothing, and it is folded as a test no
        // solution of whose pattern can agree with the current solution (see create)
        if(chained && excludesEnclosingSolution(relations, scope, bindings, compared))
            return negated ? trueValue : falseValue;

        // the current solution as the surrounding bindings provide it now
        VariableBindings solution = this.solution == null ? null :
                getCurrentSolution(request, scope, bindings, compared, chained);

        // a pattern optimised for another current solution serves this one only if this one is not wider
        SqlIntercode base = solution == null || isNarrower(this.solution, solution) ? pattern : origin;

        // the nodes of the pattern lie one level deeper than the node holding the expression
        Scope inner = scope.enterPattern(solution);

        SqlIntercode optPattern = settle(request, inner, base, solution, Map.of(), evalServices);

        if(optimized && optPattern == pattern && bindings.equals(this.bindings)
                && Objects.equals(solution, this.solution) && replaced.equals(this.replaced)
                && restriction.isOptimized(relations, variableBinding))
            return this;

        // the test evaluates the pattern with the values of the current solution in its table accesses, as far as the
        // pattern is not the first to read them; the SQL rendered to find that out is the SQL of the test when no value
        // is taken
        SqlIntercode evaluated = optPattern;
        SqlCode sql = null;

        if(restriction.contains(relations, xsdBoolean) && !optPattern.hasServiceSubpattern()
                && hasValues(optPattern, solution))
        {
            SqlFilter filter = getTopFilter(request, inner, optPattern);
            sql = (filter != null ? filter.getChild() : optPattern).translate(request, inner);

            Map<Variable, SqlExpressionIntercode> values = getValues(inner, optPattern, solution, sql);

            if(!values.isEmpty())
            {
                evaluated = settle(request, inner, optPattern, solution, values, evalServices);
                sql = null;
            }
        }

        return create(request, inner, negated, optPattern, evaluated, sql, origin, bindings, variables, replaced,
                chained, solution, bound, restriction, true);
    }


    /**
     * The pattern optimised for the current solution until it settles. Only the existence of a solution compatible with
     * the current one matters, so the pattern is optimised as a join with the current solution would optimise it; a
     * solution binding a variable to another term than the given value of the current solution cannot be compatible
     * either, so the table accesses of the pattern take the values into their conditions (see
     * {@link Restrictions#getAcceptedValue}). A pattern that shares no variable with the current solution, like the
     * pattern of an ASK query, is read for its first solution only (see {@link Restrictions#getNeededRows}): the test
     * then compares nothing, so the database cannot turn it into a join that would read the pattern whole.
     *
     * @param request the current request
     * @param scope the scope of the pattern
     * @param pattern the pattern
     * @param solution bindings of the current solution, or null
     * @param values values of the current solution the table accesses of the pattern take, by their variables
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the pattern optimised for the current solution
     */
    private static SqlIntercode settle(Request request, Scope scope, SqlIntercode pattern, VariableBindings solution,
            Map<Variable, SqlExpressionIntercode> values, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        SqlIntercode optPattern = pattern;

        for(int round = 0; round < maxRounds; round++)
        {
            Restrictions restrictions = solution == null ? new Restrictions() : SqlIntercode.getJoinRestrictions(
                    relations, optPattern.getVariableBindings(), solution, new Restrictions(), true);

            values.forEach(restrictions::setAcceptedValue);

            // a test comparing nothing with the current solution is no join for the database, which evaluates the
            // pattern by itself and stops at its first solution
            if(solution == null || solution.getVariables().isEmpty())
                restrictions.setNeededRows(1);

            SqlIntercode next = optPattern.optimize(request, scope, restrictions, true, evalServices);

            if(next.equals(optPattern))
                break;

            optPattern = next;
        }

        return optPattern;
    }


    /**
     * True if the current solution has a value the table accesses of the pattern could take (see {@link #getValues}): a
     * variable the pattern binds that the current solution never leaves unbound.
     *
     * @param pattern the pattern
     * @param solution bindings of the current solution, or null
     * @return true if the current solution has a value the table accesses of the pattern could take, false otherwise
     */
    private static boolean hasValues(SqlIntercode pattern, VariableBindings solution)
    {
        if(solution == null)
            return false;

        for(VariableBinding binding : solution.getValues())
            if(!binding.canBeNull() && pattern.getVariable(binding.getVariable()) != null)
                return true;

        return false;
    }


    /**
     * The values of the current solution that the table accesses of the pattern take into their conditions (see
     * {@link #settle}): the values of the variables the pattern binds that the current solution never leaves unbound
     * and that the pattern is not the first to read. A value is read from the inputs of the nodes holding the tests the
     * pattern lies in (see {@link Scope#getEnclosingInputs}), and SQL reading an input is evaluated for every row of
     * the input anew. So a pattern that reads an input anyway (in a filter below its top, in a part evaluated for the
     * current solution by itself, in the start of a walk) gains from the values of that input, which let the database
     * look the rows up by the indexes of the tables instead of comparing every solution of the pattern with the current
     * solution afterwards; a value that is a constant costs nothing either. A pattern that reads no input takes no
     * value of it, because the database then evaluates the pattern once and joins it with the rows of the input by the
     * comparison of the test (see {@link #translate}).
     *
     * @param scope the scope of the pattern
     * @param pattern the pattern
     * @param solution bindings of the current solution
     * @param sql SQL code of the solutions the test reads from the pattern
     * @return the values the table accesses of the pattern take, by their variables
     */
    private static Map<Variable, SqlExpressionIntercode> getValues(Scope scope, SqlIntercode pattern,
            VariableBindings solution, SqlCode sql)
    {
        Set<Integer> read = scope.getEnclosingInputs(sql);

        Map<Variable, SqlExpressionIntercode> values = new LinkedHashMap<>();

        for(VariableBinding binding : solution.getValues())
        {
            if(binding.canBeNull() || pattern.getVariable(binding.getVariable()) == null)
                continue;

            // the value is read from the inputs its columns are addressed through
            if(binding.getMappings().values().stream().filter(Objects::nonNull).flatMap(List::stream)
                    .allMatch(c -> read.containsAll(scope.getEnclosingInputs(c.getCode()))))
                values.put(binding.getVariable(), SqlOuterVariable.create(binding.getVariable(), solution));
        }

        return values;
    }


    /**
     * The filter at the top of the pattern that the test evaluates in its own condition (see {@link #translate}): one
     * whose conditions refer to the current solution. Null when the pattern has no such filter.
     *
     * @param request the current request
     * @param scope the scope of the pattern
     * @param pattern the pattern
     * @return the filter at the top of the pattern that the test evaluates in its own condition, or null
     */
    private static SqlFilter getTopFilter(Request request, Scope scope, SqlIntercode pattern)
    {
        if(pattern instanceof SqlFilter filter && scope.refersToEnclosingInput(filter.translateConditions(request)))
            return filter;

        return null;
    }


    /**
     * SQL code of the boolean expression testing the existence of a solution of the pattern compatible with the current
     * solution; it reads the declared tables the code of the pattern and the conditions of a filter the test evaluates
     * read. The columns of the current solution are addressed through the aliases of the inputs providing them, which
     * no node of the pattern declares, so the condition compares them with the columns of the pattern as they are; a
     * test whose pattern refers to those inputs nowhere else is a semi-join for the database. A filter at the top of
     * the pattern whose conditions refer to those inputs is therefore not left to the pattern: the test reads the
     * solutions the filter is given, under the alias the filter would give them, which the patterns nested in its
     * conditions refer to (see {@link Scope#getInputTable}), and its condition holds the conditions of the filter too.
     *
     * @param request the current request
     * @param scope the scope of the pattern
     * @param negated whether the test is negated
     * @param pattern the pattern as the test evaluates it
     * @param solution bindings of the current solution, or null
     * @param sql SQL code of the solutions the test reads (the pattern, or the child of the filter at its top that the
     *            test evaluates) when it has been rendered already, or null
     * @return SQL code of the boolean expression testing the existence of a solution of the pattern
     */
    private static SqlCode translate(Request request, Scope scope, boolean negated, SqlIntercode pattern,
            VariableBindings solution, SqlCode sql)
    {
        ClassRelations relations = request.getConfiguration();

        SqlBuilder builder = new SqlBuilder();

        if(negated)
            builder.append("NOT ");

        // a filter at the top of the pattern whose conditions refer to the current solution is evaluated by the test
        // itself, over the input of the filter under the alias the filter gives it, so that the pattern below does not
        // refer to the current solution because of the filter
        SqlFilter filter = getTopFilter(request, scope, pattern);
        SqlIntercode source = filter != null ? filter.getChild() : pattern;
        Table table = filter != null ? scope.getInputTable() : patternTable;

        builder.append("EXISTS (SELECT 1 FROM (");
        builder.append(sql != null ? sql : source.translate(request, scope));
        builder.append(") AS ");
        builder.appendAlias(table);

        SqlCode condition = solution == null ? null :
                SqlIntercode.generateJoinCondition(relations, solution, pattern.getVariableBindings(), null, table);

        if(condition != null || filter != null)
            builder.append(" WHERE ");

        if(condition != null)
            builder.append(condition);

        if(condition != null && filter != null)
            builder.append(" AND ");

        if(filter != null)
            builder.append(filter.translateConditions(request));

        builder.append(")");

        return builder.toCode();
    }


    /**
     * Bindings of the current solution the pattern is evaluated for (see {@link #getCurrentSolution}).
     *
     * @return bindings of the current solution the pattern is evaluated for, or null for a pattern independent of the
     *         surrounding bindings
     */
    public VariableBindings getSolution()
    {
        return solution;
    }


    /**
     * The node whose solutions the test reads: the pattern as the test evaluates it, or the child of the filter at the
     * top of the pattern when the test evaluates the filter in its own condition (see {@link #translate}). It lies in
     * the scope of the pattern, which the scope of the node holding the test enters with the current solution (see
     * {@link Scope#enterPattern}).
     *
     * @param request the current request
     * @param scope the scope of the node holding the test
     * @return the node whose solutions the test reads
     */
    public SqlIntercode getSource(Request request, Scope scope)
    {
        SqlFilter filter = getTopFilter(request, scope.enterPattern(solution), evaluated);

        return filter != null ? filter.getChild() : evaluated;
    }


    /**
     * SQL code of the test reading the given SQL code of its solutions (see {@link #getSource}) instead of the code the
     * test was rendered with when it was built: the node holding the test writes the solutions another way when the
     * join optimizer chooses so, evaluated for each of the solutions the test is evaluated for.
     *
     * @param request the current request
     * @param scope the scope of the node holding the test
     * @param sql SQL code of the solutions the test reads
     * @return SQL code of the boolean expression testing the existence of a solution of the pattern
     */
    public SqlCode translate(Request request, Scope scope, SqlCode sql)
    {
        return translate(request, scope.enterPattern(solution), negated, evaluated, solution, sql);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        String existsIndent = indent + "  ";

        builder.append("(").append(existsIndent);

        if(negated)
            builder.append("not ");

        builder.append("exists");

        if(solution != null && !solution.getVariables().isEmpty())
        {
            indentInfo(builder, existsIndent, true);
            builder.append("for ");
            builder.append(solution.getVariables().stream().map(Object::toString).sorted().collect(joining(", ")));
        }

        indentChild(builder, existsIndent, true);
        evaluated.generateExplanation(builder, getIndent(existsIndent, true));

        builder.append(existsIndent).append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlExists imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(negated != imcode.negated || chained != imcode.chained)
            return false;

        if(!Objects.equals(variables, imcode.variables) || !Objects.equals(replaced, imcode.replaced))
            return false;

        if(!Objects.equals(solution, imcode.solution))
            return false;

        if(!Objects.equals(bindings, imcode.bindings))
            return false;

        if(!Objects.equals(pattern, imcode.pattern))
            return false;

        if(!Objects.equals(evaluated, imcode.evaluated))
            return false;

        if(!Objects.equals(origin, imcode.origin))
            return false;

        if(!Objects.equals(bound, imcode.bound))
            return false;

        return true;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return pattern.hasServiceSubpattern();
    }


    @Override
    public boolean hasPattern()
    {
        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(negated, pattern, variables, solution);
    }
}
