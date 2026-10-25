package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
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
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.SqlContextSolution;
import cz.iocb.sparql.engine.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.imcode.SqlFilter;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlJoin;
import cz.iocb.sparql.engine.imcode.SqlLateralJoin;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.imcode.SqlUnion;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * EXISTS and NOT EXISTS: true if the pattern, evaluated for the current solution of the surrounding bindings, has a
 * solution. The pattern refers to the current solution through its context solution (see {@link SqlContextSolution}).
 * When the context solution is a plain child of the join the pattern is, the test compares the solutions of the pattern
 * without it with the current solution (see {@link #detach}), a form the database turns into a semi-join; otherwise the
 * expression supplies the exposed columns of the current solution under an alias, and the pattern is evaluated
 * laterally for them. Without a context solution, the pattern is independent of the current solution. A pattern holding
 * a SERVICE call not evaluated yet has no SQL; the expression then carries a placeholder column until the second
 * optimisation pass evaluates the call and rebuilds the expression.
 */
public final class SqlExists extends SqlExpressionIntercode
{
    /**
     * Alias of the pattern.
     */
    private static final AliasTable patternTable = new AliasTable("tab");

    /**
     * Alias of the detached pattern, exposing the compared columns under names of their own (see {@link #translate}).
     */
    private static final AliasTable existsTable = new AliasTable("exists");

    /**
     * Column standing for the test while its pattern holds a SERVICE call not evaluated yet. It never reaches the
     * database, as the second optimisation pass rebuilds the expression once the call is evaluated, and would fail
     * there otherwise.
     */
    private static final Column unevaluated = new ExpressionColumn("sparql.unevaluated_service()", BOOL, false);

    /**
     * True for NOT EXISTS.
     */
    private final boolean negated;

    /**
     * Tested pattern.
     */
    private final SqlIntercode pattern;

    /**
     * Bindings of the surrounding solution.
     */
    private final VariableBindings bindings;

    /**
     * Context solution the pattern refers to, as created for the pattern (with all exposed columns), or null.
     */
    private final SqlContextSolution context;

    /**
     * The pattern without its context solution, whose solutions compatible with the current solution are those of the
     * pattern (see {@link #detach}), or null when the pattern is evaluated laterally.
     */
    private final SqlIntercode detached;

    /**
     * True when the expression was built by the optimisation, which detaches the context solution when it can; the
     * translator builds the expression without trying.
     */
    private final boolean optimized;


    /**
     * Creates the expression; it is deterministic if the pattern is.
     *
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param mappings columns per resource class
     * @param bindings the variable bindings
     * @param context context solution the pattern refers to, or null
     * @param detached the pattern without its context solution, or null when the pattern is evaluated laterally
     * @param optimized whether the expression was built by the optimisation
     */
    protected SqlExists(boolean negated, SqlIntercode pattern, Map<ResourceClass, List<Column>> mappings,
            VariableBindings bindings, SqlContextSolution context, SqlIntercode detached, boolean optimized)
    {
        super(mappings, false, pattern.isDeterministic());

        this.negated = negated;
        this.pattern = pattern;
        this.bindings = bindings;
        this.context = context;
        this.detached = detached;
        this.optimized = optimized;

        if(context != null)
            this.referencedVariables.addAll(context.getVariableBindings().getVariables());
    }


    /**
     * Existence test of a pattern independent of the surrounding bindings.
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
        return create(request, negated, pattern, bindings, null, Restriction.ALL);
    }


    /**
     * Existence test of a pattern evaluated for the current solution of the surrounding bindings, which the pattern
     * refers to through the context solution.
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @param context context solution the pattern refers to
     * @return existence test of a pattern evaluated for the current solution of the surrounding bindings
     */
    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings, SqlContextSolution context)
    {
        return create(request, negated, pattern, bindings, context, Restriction.ALL);
    }


    /**
     * Existence test materialising only when needed; a pattern without solutions or with the single solution makes it
     * constant.
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @param context context solution the pattern refers to, or null
     * @param restriction the result classes the parent needs
     * @return existence test materialising only when needed; a pattern without solutions or with the single solution
     *         makes it constant
     */
    public static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings, SqlContextSolution context, Restriction restriction)
    {
        return create(request, negated, pattern, bindings, context, restriction, false, false);
    }


    /**
     * Existence test materialising only when needed (see
     * {@link #create(Request, boolean, SqlIntercode, VariableBindings, SqlContextSolution, Restriction)}), built by the
     * optimisation when asked to detach the context solution from the pattern, optimised already (see {@link #detach}).
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @param context context solution the pattern refers to, or null
     * @param restriction the result classes the parent needs
     * @param optimized whether the pattern is optimised and the context solution is to be detached from it
     * @param evalServices whether SERVICE stubs are evaluated
     * @return existence test materialising only when needed
     */
    private static SqlExpressionIntercode create(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings, SqlContextSolution context, Restriction restriction, boolean optimized,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        if(pattern.equals(SqlNoSolution.get()))
            return negated ? trueValue : falseValue;

        //NOTE: the context solution is a single solution like the empty one
        if(pattern.equals(SqlEmptySolution.get()) || pattern instanceof SqlContextSolution)
            return negated ? falseValue : trueValue;

        //NOTE: a solution of the pattern agrees with the current solution on the shared variables
        List<VariableBindingPair> pairs = VariableBindingPair.getPairs(relations, pattern.getVariableBindings(),
                bindings);

        if(pairs.stream().anyMatch(p -> !p.isJoinable()))
            return negated ? trueValue : falseValue;


        if(!restriction.contains(relations, xsdBoolean))
            return new SqlExists(negated, pattern, singletonMap(xsdBoolean, null), bindings, context, null, optimized);

        // the call is evaluated by the second optimisation pass, which rebuilds the expression and everything built
        // on it; until then a placeholder stands for the test
        if(pattern.hasServiceSubpattern())
            return new SqlExists(negated, pattern, singletonMap(xsdBoolean, List.of(unevaluated)), bindings, context,
                    null, optimized);


        SqlIntercode detached = optimized && context != null ? detach(request, pattern, context, evalServices) : null;

        List<Column> result = translate(request, negated, pattern, detached, bindings, context);

        return new SqlExists(negated, pattern, singletonMap(xsdBoolean, result), bindings, context, detached,
                optimized);
    }


    /**
     * The pattern without its context solution, optimised for the comparison with the current solution (see
     * {@link #translate}): the context solution is a child of the join the pattern is (of every join under a union,
     * also under a filter), every variable it binds there is bound by another child of the join, and the rest of the
     * pattern refers to it nowhere else. The rest is optimised under the classes the context solution binds the shared
     * variables in, as the join would optimise it, but without taking the context solution in. Null when the pattern
     * cannot be detached, notably when a child of the join has taken the context solution in (a walk seeded with it),
     * whose evaluation for every current solution the join then asks for.
     *
     * @param request the current request
     * @param pattern the optimised pattern
     * @param context the context solution of the pattern
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the pattern without its context solution, or null when the pattern cannot be detached
     */
    private static SqlIntercode detach(Request request, SqlIntercode pattern, SqlContextSolution context,
            boolean evalServices)
    {
        SqlIntercode rest = detachContext(request, pattern, context);

        if(rest == null)
            return null;

        Restrictions restrictions = SqlIntercode.getJoinRestrictions(request.getConfiguration(),
                rest.getVariableBindings(), context.getVariableBindings(), new Restrictions(), true);

        SqlIntercode optimized = rest.optimize(request, restrictions, true, evalServices);

        if(optimized.equals(SqlNoSolution.get()) || optimized.equals(SqlEmptySolution.get()))
            return null;

        // a nested group of the pattern may refer to the context solution too, which its SQL shows
        if(optimized.translate(request).contains(context.getLateralTable().toString()))
            return null;

        return optimized;
    }


    /**
     * The pattern without its context solution, as built (see {@link #detach}), or null when the context solution
     * cannot be detached from it.
     *
     * @param request the current request
     * @param pattern the pattern
     * @param context the context solution of the pattern
     * @return the pattern without its context solution, or null when the context solution cannot be detached from it
     */
    private static SqlIntercode detachContext(Request request, SqlIntercode pattern, SqlContextSolution context)
    {
        if(pattern instanceof SqlJoin join)
        {
            List<SqlIntercode> contexts = join.getChilds().stream().filter(c -> isContext(c, context)).toList();
            List<SqlIntercode> others = join.getChilds().stream().filter(c -> !isContext(c, context)).toList();

            if(contexts.isEmpty() || others.isEmpty())
                return null;

            SqlIntercode rest = others.get(0);

            for(int i = 1; i < others.size(); i++)
                rest = SqlJoin.join(request, rest, others.get(i));

            // the comparison stands for the join on the variables the rest binds only
            for(SqlIntercode child : contexts)
                for(Variable variable : child.getVariableBindings().getVariables())
                    if(rest.getVariableBindings().get(variable) == null)
                        return null;

            return rest;
        }

        if(pattern instanceof SqlUnion union)
        {
            List<SqlIntercode> branches = new ArrayList<>();

            // a branch without the context solution stays as it is
            for(SqlIntercode branch : union.getChilds())
            {
                SqlIntercode detached = detachContext(request, branch, context);
                branches.add(detached != null ? detached : branch);
            }

            return SqlUnion.union(request, branches);
        }

        if(pattern instanceof SqlFilter filter)
        {
            SqlIntercode child = detachContext(request, filter.getChild(), context);
            return child == null ? null : SqlFilter.filter(request, filter.getConditions(), child);
        }

        return null;
    }


    /**
     * True if the node is the context solution of the pattern, as created or restricted.
     *
     * @param node the node
     * @param context the context solution of the pattern
     * @return true if the node is the context solution of the pattern, false otherwise
     */
    private static boolean isContext(SqlIntercode node, SqlContextSolution context)
    {
        return node instanceof SqlContextSolution solution
                && solution.getLateralTable().equals(context.getLateralTable());
    }


    @Override
    public Restrictions getRequirements(ClassRelations relations)
    {
        if(context == null)
            return new Restrictions();

        return SqlLateralJoin.getLateralRequirements(context.getVariableBindings());
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        // the SQL of the pattern is rendered into the expression at once, outside a statement
        request.beginExpression();

        try
        {
            //NOTE: only the existence of a solution matters, no column of the pattern is needed
            SqlIntercode optPattern = pattern.optimize(request, new Restrictions(), true, evalServices);

            if(optPattern == pattern && bindings.equals(this.bindings) && optimized)
                return this;

            return create(request, negated, optPattern, bindings, context, restriction, true, evalServices);
        }
        finally
        {
            request.endExpression();
        }
    }


    /**
     * SQL boolean expression testing the existence of a solution of the pattern. With the context solution detached,
     * the pattern without it is evaluated by itself, its columns of the variables of the current solution are exposed
     * under names of their own, and the test compares them with the exposed columns of the current solution, supplied
     * as the surrounding bindings provide them now; the names of their own keep the columns of the current solution,
     * referred to from the outside, from being taken for columns of the pattern, and the database turns the test into a
     * semi-join. Otherwise the pattern is joined laterally to the exposed columns of the current solution, supplied
     * under their names.
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param detached the pattern without its context solution, or null when the pattern is evaluated laterally
     * @param bindings the variable bindings
     * @param context context solution the pattern refers to, or null
     * @return SQL boolean expression testing the existence of a solution of the pattern
     */
    public static List<Column> translate(Request request, boolean negated, SqlIntercode pattern, SqlIntercode detached,
            VariableBindings bindings, SqlContextSolution context)
    {
        ClassRelations relations = request.getConfiguration();

        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("NOT ");

        builder.append("EXISTS (SELECT 1 FROM ");

        Map<Column, Column> values = context == null ? Map.of() :
                SqlLateralJoin.getLateralValues(relations, context.getVariableBindings(), bindings);

        if(detached != null)
        {
            // the current solution as the surrounding bindings provide it now
            VariableBindings current = new VariableBindings();

            for(VariableBinding binding : context.getVariableBindings().getValues())
            {
                VariableBinding provided = new VariableBinding(binding.getVariable(), binding.canBeNull());

                for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                    if(entry.getValue() != null)
                        provided.addMapping(entry.getKey(),
                                entry.getValue().stream().map(c -> values.getOrDefault(c, c)).toList());

                current.add(provided);
            }

            // the columns of the shared variables of the detached pattern under names of their own
            Map<Column, Column> renamed = new LinkedHashMap<>();
            VariableBindings compared = new VariableBindings();

            for(VariableBinding binding : detached.getVariableBindings().getValues())
            {
                if(current.get(binding.getVariable()) == null)
                    continue;

                VariableBinding copy = new VariableBinding(binding.getVariable(), binding.canBeNull());

                for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                {
                    List<Column> columns = entry.getValue() == null ? null : new ArrayList<>();

                    if(columns != null)
                        for(Column column : entry.getValue())
                            columns.add(column instanceof ConstantColumn ? column : renamed.computeIfAbsent(column,
                                    c -> new TableColumn("@exists" + renamed.size(), c.getType(), c.canBeNull())));

                    copy.addMapping(entry.getKey(), columns);
                }

                compared.add(copy);
            }

            builder.append("(SELECT ");
            builder.append(renamed.isEmpty() ? "1" : renamed.entrySet().stream()
                    .map(e -> e.getKey().fromTable(patternTable) + " AS " + e.getValue()).collect(joining(", ")));
            builder.append(" FROM (");
            builder.append(detached.translate(request));
            builder.append(") AS ");
            builder.append(patternTable);
            builder.append(") AS ");
            builder.append(existsTable);

            String condition = SqlIntercode.generateJoinCondition(relations, current, compared, null, existsTable);

            if(condition != null)
            {
                builder.append(" WHERE ");
                builder.append(condition);
            }

            builder.append(")");

            return List.of(new ExpressionColumn(builder.toString(), BOOL, false));
        }

        if(!values.isEmpty())
        {
            builder.append("(SELECT ");
            builder.append(values.entrySet().stream().map(e -> e.getValue() + " AS " + e.getKey()).sorted()
                    .collect(joining(", ")));
            builder.append(") AS ");
            builder.append(context.getLateralTable());
            builder.append(" CROSS JOIN LATERAL ");
        }

        builder.append("(");
        builder.append(pattern.translate(request));
        builder.append(") AS ");
        builder.append(patternTable);
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

        if(context != null)
        {
            indentInfo(builder, existsIndent, true);
            builder.append(context.getLateralTable());
            builder.append(detached != null ? " compared on " : " using ");
            builder.append(context.getVariableBindings().getVariables().stream().map(Object::toString).sorted()
                    .collect(joining(", ")));
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

        if(!Objects.equals(negated, imcode.negated))
            return false;

        if(!Objects.equals(pattern, imcode.pattern))
            return false;

        if(!Objects.equals(bindings, imcode.bindings))
            return false;

        if(!Objects.equals(context, imcode.context))
            return false;

        if(!Objects.equals(detached, imcode.detached))
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
        return Objects.hash(negated, pattern, context, detached);
    }
}
