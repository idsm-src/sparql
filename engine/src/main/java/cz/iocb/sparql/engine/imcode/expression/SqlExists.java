package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
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
 * for the classes the node holding the test provides at the moment, as a join with them would optimise it. A test of a
 * pattern independent of the surrounding bindings has no current solution of its own. A pattern holding a SERVICE call
 * not evaluated yet has no SQL; the expression then carries a placeholder column until the second optimisation pass
 * evaluates the call and rebuilds the expression.
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
    private static final Column unevaluated = new ExpressionColumn("sparql.unevaluated_service()", BOOL, false);

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
     * The variables of the current solution the pattern may refer to.
     */
    private final Set<Variable> variables;

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
     * @param chained whether the current solution extends the current solution of the pattern the node holding the
     *            expression lies in
     * @param solution bindings of the current solution the pattern was built for, or null
     * @param optimized whether the expression was built by the optimisation
     */
    private SqlExists(boolean negated, SqlIntercode pattern, SqlIntercode evaluated, SqlIntercode origin,
            Map<ResourceClass, List<Column>> mappings, VariableBindings bindings, Set<Variable> variables,
            boolean chained, VariableBindings solution, boolean optimized)
    {
        super(mappings, false, pattern.isDeterministic());

        this.negated = negated;
        this.pattern = pattern;
        this.evaluated = evaluated;
        this.origin = origin;
        this.bindings = bindings;
        this.variables = variables;
        this.chained = chained;
        this.solution = solution;
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
                false, null, Restriction.ALL, false);
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
        return create(request, scope.enterPattern(solution), negated, pattern, pattern, null, pattern, bindings,
                variables, chained, solution, Restriction.ALL, false);
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
     * @param sql SQL of the solutions the test reads from the evaluated pattern (see {@link #translate}) when it has
     *            been rendered already, or null
     * @param origin the pattern as translated
     * @param bindings the variable bindings
     * @param variables the variables of the current solution the pattern may refer to
     * @param chained whether the current solution extends the current solution of the pattern the node holding the
     *            expression lies in
     * @param solution bindings of the current solution the pattern was built for, or null
     * @param restriction the result classes the parent needs
     * @param optimized whether the expression is built by the optimisation
     * @return existence test materialising only when needed
     */
    private static SqlExpressionIntercode create(Request request, Scope scope, boolean negated, SqlIntercode pattern,
            SqlIntercode evaluated, String sql, SqlIntercode origin, VariableBindings bindings, Set<Variable> variables,
            boolean chained, VariableBindings solution, Restriction restriction, boolean optimized)
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
                    variables, chained, solution, optimized);

        // the call is evaluated by the second optimisation pass, which rebuilds the expression and everything built
        // on it; until then a placeholder stands for the test
        if(pattern.hasServiceSubpattern())
            return new SqlExists(negated, pattern, evaluated, origin, singletonMap(xsdBoolean, List.of(unevaluated)),
                    bindings, variables, chained, solution, optimized);


        List<Column> result = translate(request, scope, negated, evaluated, solution, sql);

        return new SqlExists(negated, pattern, evaluated, origin, singletonMap(xsdBoolean, result), bindings, variables,
                chained, solution, optimized);
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
        AliasTable table = scope.getInputTable();

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


    @Override
    public Restrictions getRequirements(ClassRelations relations)
    {
        // the current solution is read from the surrounding bindings on every optimisation, so they have to keep every
        // variable the pattern may refer to in all its classes, also one bound to constants only
        return new Restrictions(variables);
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


    @Override
    public SqlExpressionIntercode optimize(Request request, Scope scope, VariableBindings bindings,
            Restriction restriction, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        // the current solution as the surrounding bindings provide it now
        VariableBindings solution = this.solution == null ? null :
                getCurrentSolution(request, scope, bindings, variables, chained);

        // a pattern optimised for another current solution serves this one only if this one is not wider
        SqlIntercode base = solution == null || isNarrower(this.solution, solution) ? pattern : origin;

        // the nodes of the pattern lie one level deeper than the node holding the expression
        Scope inner = scope.enterPattern(solution);

        SqlIntercode optPattern = settle(request, inner, base, solution, Map.of(), evalServices);

        if(optimized && optPattern == pattern && bindings.equals(this.bindings)
                && Objects.equals(solution, this.solution) && restriction.isOptimized(relations, variableBinding))
            return this;

        // the test evaluates the pattern with the values of the current solution in its table accesses, as far as the
        // pattern is not the first to read them; the SQL rendered to find that out is the SQL of the test when no value
        // is taken
        SqlIntercode evaluated = optPattern;
        String sql = null;

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

        return create(request, inner, negated, optPattern, evaluated, sql, origin, bindings, variables, chained,
                solution, restriction, true);
    }


    /**
     * The pattern optimised for the current solution until it settles. Only the existence of a solution compatible with
     * the current one matters, so the pattern is optimised as a join with the current solution would optimise it; a
     * solution binding a variable to another term than the given value of the current solution cannot be compatible
     * either, so the table accesses of the pattern take the values into their conditions (see
     * {@link Restrictions#getAcceptedValue}).
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
     * @param sql SQL of the solutions the test reads from the pattern
     * @return the values the table accesses of the pattern take, by their variables
     */
    private static Map<Variable, SqlExpressionIntercode> getValues(Scope scope, SqlIntercode pattern,
            VariableBindings solution, String sql)
    {
        Set<Integer> read = scope.getEnclosingInputs(sql);

        Map<Variable, SqlExpressionIntercode> values = new LinkedHashMap<>();

        for(VariableBinding binding : solution.getValues())
        {
            if(binding.canBeNull() || pattern.getVariable(binding.getVariable()) == null)
                continue;

            String columns = binding.getMappings().values().stream().filter(Objects::nonNull).flatMap(List::stream)
                    .map(Object::toString).collect(joining(" "));

            if(read.containsAll(scope.getEnclosingInputs(columns)))
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
     * SQL boolean expression testing the existence of a solution of the pattern compatible with the current solution.
     * The columns of the current solution are addressed through the aliases of the inputs providing them, which no node
     * of the pattern declares, so the condition compares them with the columns of the pattern as they are; a test whose
     * pattern refers to those inputs nowhere else is a semi-join for the database. A filter at the top of the pattern
     * whose conditions refer to those inputs is therefore not left to the pattern: the test reads the solutions the
     * filter is given, under the alias the filter would give them, which the patterns nested in its conditions refer to
     * (see {@link Scope#getInputTable}), and its condition holds the conditions of the filter too.
     *
     * @param request the current request
     * @param scope the scope of the pattern
     * @param negated whether the test is negated
     * @param pattern the pattern as the test evaluates it
     * @param solution bindings of the current solution, or null
     * @param sql SQL of the solutions the test reads (the pattern, or the child of the filter at its top that the test
     *            evaluates) when it has been rendered already, or null
     * @return SQL boolean expression testing the existence of a solution of the pattern
     */
    private static List<Column> translate(Request request, Scope scope, boolean negated, SqlIntercode pattern,
            VariableBindings solution, String sql)
    {
        ClassRelations relations = request.getConfiguration();

        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("NOT ");

        // a filter at the top of the pattern whose conditions refer to the current solution is evaluated by the test
        // itself, over the input of the filter under the alias the filter gives it, so that the pattern below does not
        // refer to the current solution because of the filter
        SqlFilter filter = getTopFilter(request, scope, pattern);
        SqlIntercode source = filter != null ? filter.getChild() : pattern;
        AliasTable table = filter != null ? scope.getInputTable() : patternTable;

        builder.append("EXISTS (SELECT 1 FROM (");
        builder.append(sql != null ? sql : source.translate(request, scope));
        builder.append(") AS ");
        builder.append(table);

        String condition = solution == null ? null :
                SqlIntercode.generateJoinCondition(relations, solution, pattern.getVariableBindings(), null, table);
        String filterConditions = filter != null ? filter.translateConditions(request) : null;

        if(condition != null || filterConditions != null)
        {
            builder.append(" WHERE ");
            builder.append(Stream.of(condition, filterConditions).filter(Objects::nonNull).collect(joining(" AND ")));
        }

        builder.append(")");

        return List.of(new ExpressionColumn(builder.toString(), BOOL, false));
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

        if(!Objects.equals(variables, imcode.variables))
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

        return true;
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return evaluated.getVirtualTables();
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return pattern.hasServiceSubpattern();
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(negated, pattern, variables, solution);
    }
}
