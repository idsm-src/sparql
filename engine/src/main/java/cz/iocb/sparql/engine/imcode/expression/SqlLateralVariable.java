package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.LateralTable;
import cz.iocb.sparql.engine.imcode.Scope;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlLateralJoin;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Reference from an expression inside the right side of a lateral join to a variable of its left side: the columns of
 * the current left solution addressed through the alias of the join (see {@link SqlLateralJoin}). Unlike
 * {@link SqlVariable}, it is no variable of the bindings it occurs in, so it needs nothing of them and is never
 * resolved against them; the join keeps its columns available. When the join lets its right side read the solution as
 * the left side provides it at the moment, the expression takes its classes and columns from that solution on every
 * optimisation (see {@link Scope#getLateralSolution}), like a value of the current solution of an EXISTS pattern (see
 * {@link SqlOuterVariable}), so it follows the classes and the constants the left side is optimised to; otherwise it
 * keeps what the join exposed when it was created.
 */
public final class SqlLateralVariable extends SqlExpressionIntercode
{
    /**
     * Alias through which the right side of the join refers to its left side.
     */
    private final LateralTable table;

    /**
     * The variable of the left side.
     */
    private final Variable variable;


    /**
     * Creates the expression.
     *
     * @param table alias through which the right side of the join refers to its left side
     * @param variable the variable of the left side
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    private SqlLateralVariable(LateralTable table, Variable variable, Map<ResourceClass, List<Column>> mappings,
            boolean canBeNull)
    {
        super(mappings, canBeNull, true);

        this.table = table;
        this.variable = variable;
    }


    /**
     * Reference to the variable of the binding, one of the bindings returned by
     * {@link SqlLateralJoin#getLateralVariableBindings} for the alias.
     *
     * @param table alias through which the right side of the join refers to its left side
     * @param binding the binding of the variable as seen by the right side
     * @return reference to the variable of the binding
     */
    public static SqlExpressionIntercode create(LateralTable table, VariableBinding binding)
    {
        return new SqlLateralVariable(table, binding.getVariable(), binding.getMappings(), binding.canBeNull());
    }


    @Override
    public Restrictions getRequirements(ClassRelations relations)
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, Scope scope, VariableBindings bindings,
            Restriction restriction, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        // the current solution as the left side of the join provides it now, when the join lets the right side read it
        VariableBindings solution = scope.getLateralSolution(table);
        VariableBinding binding = solution == null ? variableBinding : solution.get(variable);

        if(binding == null)
            return SqlNull.get();

        Map<ResourceClass, List<Column>> mappings = restriction.restrict(relations, binding.getMappings());

        if(mappings.equals(variableBinding.getMappings()) && binding.canBeNull() == variableBinding.canBeNull())
            return this;

        return new SqlLateralVariable(table, variable, mappings, binding.canBeNull());
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("lateral ");
        builder.append(variable);
    }


    /**
     * The variable of the left side.
     *
     * @return the variable of the left side
     */
    public Variable getVariable()
    {
        return variable;
    }


    @Override
    public boolean hasSameValue(SqlExpressionIntercode other)
    {
        //NOTE: the value is read from the current solution of the left side of the join, whatever classes and columns
        // each of the two references holds at the moment
        return other instanceof SqlLateralVariable lateral && table.equals(lateral.table)
                && variable.equals(lateral.variable);
    }


    @Override
    public boolean isCurrentValue()
    {
        return true;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlLateralVariable imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!table.equals(imcode.table))
            return false;

        if(!variable.equals(imcode.variable))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(table, variable);
    }
}
