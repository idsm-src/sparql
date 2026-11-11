package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
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
 * make the pattern refer to it. The current solution is read from the surrounding bindings on every optimisation (see
 * {@link #getCurrentSolution}), so the pattern is optimised for the classes the node holding the test provides at the
 * moment, as a join with them would optimise it. A test of a pattern independent of the surrounding bindings has no
 * current solution of its own. A pattern holding a SERVICE call not evaluated yet has no SQL; the expression then
 * carries a placeholder column until the second optimisation pass evaluates the call and rebuilds the expression.
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
     * @param origin the pattern as translated
     * @param mappings columns per resource class
     * @param bindings the variable bindings
     * @param variables the variables of the current solution the pattern may refer to
     * @param chained whether the current solution extends the current solution of the pattern the node holding the
     *            expression lies in
     * @param solution bindings of the current solution the pattern was built for, or null
     * @param optimized whether the expression was built by the optimisation
     */
    private SqlExists(boolean negated, SqlIntercode pattern, SqlIntercode origin,
            Map<ResourceClass, List<Column>> mappings, VariableBindings bindings, Set<Variable> variables,
            boolean chained, VariableBindings solution, boolean optimized)
    {
        super(mappings, false, pattern.isDeterministic());

        this.negated = negated;
        this.pattern = pattern;
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
     * solution of that pattern (see {@link Request#beginPattern}).
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @return existence test of a pattern independent of the surrounding bindings
     */
    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings)
    {
        return create(request, negated, pattern, pattern, bindings, Set.of(), false, null, Restriction.ALL, false);
    }


    /**
     * Existence test of a pattern evaluated for the current solution of the surrounding bindings: the pattern was
     * translated for the current solution with the given bindings, and its SQL is rendered here, so the pattern is
     * still being translated (see {@link Request#beginPattern}).
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @param variables the variables of the current solution the pattern may refer to
     * @param chained whether the current solution extends the current solution of the pattern the node holding the
     *            expression lies in
     * @param solution bindings of the current solution the pattern was translated for (see {@link #getCurrentSolution})
     * @return existence test of a pattern evaluated for the current solution of the surrounding bindings
     */
    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings, Set<Variable> variables, boolean chained, VariableBindings solution)
    {
        return create(request, negated, pattern, pattern, bindings, variables, chained, solution, Restriction.ALL,
                false);
    }


    /**
     * Existence test materialising only when needed; a pattern without solutions or with the single empty solution
     * makes it constant, and so does a pattern none of whose solutions can agree with the current solution.
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
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
    private static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            SqlIntercode origin, VariableBindings bindings, Set<Variable> variables, boolean chained,
            VariableBindings solution, Restriction restriction, boolean optimized)
    {
        ClassRelations relations = request.getConfiguration();

        if(pattern.equals(SqlNoSolution.get()))
            return negated ? trueValue : falseValue;

        if(pattern.equals(SqlEmptySolution.get()))
            return negated ? falseValue : trueValue;

        //NOTE: a solution of the pattern agrees with the current solution on the shared variables
        if(solution != null && VariableBindingPair.getPairs(relations, pattern.getVariableBindings(), solution).stream()
                .anyMatch(p -> !p.isJoinable()))
            return negated ? trueValue : falseValue;


        if(!restriction.contains(relations, xsdBoolean))
            return new SqlExists(negated, pattern, origin, singletonMap(xsdBoolean, null), bindings, variables, chained,
                    solution, optimized);

        // the call is evaluated by the second optimisation pass, which rebuilds the expression and everything built
        // on it; until then a placeholder stands for the test
        if(pattern.hasServiceSubpattern())
            return new SqlExists(negated, pattern, origin, singletonMap(xsdBoolean, List.of(unevaluated)), bindings,
                    variables, chained, solution, optimized);


        List<Column> result = translate(request, negated, pattern, solution);

        return new SqlExists(negated, pattern, origin, singletonMap(xsdBoolean, result), bindings, variables, chained,
                solution, optimized);
    }


    /**
     * Bindings of the current solution a pattern of an expression is evaluated for, given the bindings of the solutions
     * the expression is evaluated for: the variables the pattern may refer to, in the classes that have columns, the
     * columns addressed through the alias of the input of the node holding the expression (see
     * {@link Request#getInputTable}; the bindings of the sides of a left join are addressed already). When the node
     * lies in a pattern of another expression and its solutions extend the current solution of that pattern, the
     * solution is joined with it, so that a variable only that one binds is known to the nested pattern too. Called for
     * the node holding the expression, before its pattern is entered (see {@link Request#beginPattern}).
     *
     * @param request the current request
     * @param bindings bindings of the solutions the expression is evaluated for
     * @param variables the variables of the current solution the pattern may refer to
     * @param chained whether the solutions the expression is evaluated for extend the current solution of the pattern
     *            the node holding the expression lies in
     * @return bindings of the current solution
     */
    public static VariableBindings getCurrentSolution(Request request, VariableBindings bindings,
            Set<Variable> variables, boolean chained)
    {
        AliasTable table = request.getInputTable();

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

        VariableBindings enclosing = chained ? request.getCurrentSolution() : null;

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
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        // the current solution as the surrounding bindings provide it now
        VariableBindings solution = this.solution == null ? null :
                getCurrentSolution(request, bindings, variables, chained);

        // a pattern optimised for another current solution serves this one only if this one is not wider
        SqlIntercode base = solution == null || isNarrower(this.solution, solution) ? pattern : origin;

        // a walk in the pattern is not seeded from shared solutions (see Request.isWithinExpression)
        request.beginExpression();

        // the nodes of the pattern lie one level deeper than the node holding the expression
        request.beginPattern(solution);

        try
        {
            // only the existence of a solution compatible with the current one matters, so the pattern is optimised as
            // a join with the current solution would optimise it, until it settles
            SqlIntercode optPattern = base;

            for(int round = 0; round < maxRounds; round++)
            {
                Restrictions restrictions = solution == null ? new Restrictions() : SqlIntercode.getJoinRestrictions(
                        relations, optPattern.getVariableBindings(), solution, new Restrictions(), true);

                SqlIntercode next = optPattern.optimize(request, restrictions, true, evalServices);

                if(next.equals(optPattern))
                    break;

                optPattern = next;
            }

            if(optimized && optPattern == pattern && bindings.equals(this.bindings)
                    && Objects.equals(solution, this.solution) && restriction.isOptimized(relations, variableBinding))
                return this;

            return create(request, negated, optPattern, origin, bindings, variables, chained, solution, restriction,
                    true);
        }
        finally
        {
            request.endPattern();
            request.endExpression();
        }
    }


    /**
     * SQL boolean expression testing the existence of a solution of the pattern compatible with the current solution.
     * The columns of the current solution are addressed through the aliases of the inputs providing them, which no node
     * of the pattern declares, so the condition compares them with the columns of the pattern as they are; a test whose
     * pattern refers to those inputs nowhere else is a semi-join for the database. A filter at the top of the pattern
     * whose conditions refer to those inputs is therefore not left to the pattern: the test reads the solutions the
     * filter is given, under the alias the filter would give them, which the patterns nested in its conditions refer to
     * (see {@link Request#getInputTable}), and its condition holds the conditions of the filter too.
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param solution bindings of the current solution, or null
     * @return SQL boolean expression testing the existence of a solution of the pattern
     */
    private static List<Column> translate(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings solution)
    {
        ClassRelations relations = request.getConfiguration();

        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("NOT ");

        SqlIntercode source = pattern;
        AliasTable table = patternTable;
        String filterConditions = null;

        // a filter at the top of the pattern whose conditions refer to the current solution is evaluated by the test
        // itself, over the input of the filter under the alias the filter gives it, so that the pattern below does not
        // refer to the current solution because of the filter
        if(pattern instanceof SqlFilter filter)
        {
            String conditions = filter.translateConditions(request);

            if(request.refersToEnclosingInput(conditions))
            {
                source = filter.getChild();
                table = request.getInputTable();
                filterConditions = conditions;
            }
        }

        builder.append("EXISTS (SELECT 1 FROM (");
        builder.append(source.translate(request));
        builder.append(") AS ");
        builder.append(table);

        String condition = solution == null ? null :
                SqlIntercode.generateJoinCondition(relations, solution, pattern.getVariableBindings(), null, table);

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
        pattern.generateExplanation(builder, getIndent(existsIndent, true));

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

        if(!Objects.equals(origin, imcode.origin))
            return false;

        return true;
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return pattern.getVirtualTables();
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
