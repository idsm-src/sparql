package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.SqlContextSolution;
import cz.iocb.sparql.engine.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlLateralJoin;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * EXISTS and NOT EXISTS: true if the pattern, evaluated for the current solution of the surrounding bindings, has a
 * solution. The pattern refers to the current solution through its context solution (see {@link SqlContextSolution}):
 * the expression supplies the exposed columns of the current solution under an alias, and the pattern is evaluated
 * laterally for them. Without a context solution, the pattern is independent of the current solution.
 */
public final class SqlExists extends SqlExpressionIntercode
{
    /**
     * Alias of the pattern.
     */
    private static final AliasTable patternTable = new AliasTable("tab");

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
     * Creates the expression; it is deterministic if the pattern is.
     *
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param mappings columns per resource class
     * @param bindings the variable bindings
     * @param context context solution the pattern refers to, or null
     */
    protected SqlExists(boolean negated, SqlIntercode pattern, Map<ResourceClass, List<Column>> mappings,
            VariableBindings bindings, SqlContextSolution context)
    {
        super(mappings, false, pattern.isDeterministic());

        this.negated = negated;
        this.pattern = pattern;
        this.bindings = bindings;
        this.context = context;

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
            return new SqlExists(negated, pattern, singletonMap(xsdBoolean, null), bindings, context);


        List<Column> result = translate(request, negated, pattern, bindings, context);

        return new SqlExists(negated, pattern, singletonMap(xsdBoolean, result), bindings, context);
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
        //NOTE: only the existence of a solution matters, no column of the pattern is needed
        SqlIntercode optPattern = pattern.optimize(request, new Restrictions(), true, evalServices);

        if(optPattern == pattern && bindings.equals(this.bindings))
            return this;

        return create(request, negated, optPattern, bindings, context, restriction);
    }


    /**
     * SQL boolean expression testing the existence of a solution of the pattern: the pattern is joined laterally to the
     * exposed columns of the current solution, supplied under their names as the surrounding bindings provide them now.
     *
     * @param request the current request
     * @param negated whether the test is negated
     * @param pattern the tested pattern
     * @param bindings the variable bindings
     * @param context context solution the pattern refers to, or null
     * @return SQL boolean expression testing the existence of a solution of the pattern
     */
    public static List<Column> translate(Request request, boolean negated, SqlIntercode pattern,
            VariableBindings bindings, SqlContextSolution context)
    {
        ClassRelations relations = request.getConfiguration();

        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("NOT ");

        builder.append("EXISTS (SELECT 1 FROM ");

        Map<Column, Column> values = context == null ? Map.of() :
                SqlLateralJoin.getLateralValues(relations, context.getVariableBindings(), bindings);

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
            builder.append(" using ");
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

        return true;
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return pattern.getVirtualTables();
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(negated, pattern, context);
    }
}
