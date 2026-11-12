package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.imcode.Scope;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Value of a variable in the current solution an EXISTS pattern is evaluated for. The value is no variable of the
 * bindings the expression occurs in: its columns are the columns of the input of the node holding the EXISTS expression
 * (or of a node above it), addressed through the alias of that input, which no node of the pattern declares again (see
 * {@link Scope#getInputTable}). On every optimisation the expression takes the columns the current solution of the
 * innermost pattern provides at the moment (see {@link Scope#getCurrentSolution}), so it follows the classes the node
 * holding the EXISTS expression is optimised to.
 */
public final class SqlOuterVariable extends SqlExpressionIntercode
{
    /**
     * The variable of the current solution.
     */
    private final Variable variable;


    /**
     * Creates the expression.
     *
     * @param variable the variable of the current solution
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    private SqlOuterVariable(Variable variable, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, true);

        this.variable = variable;
    }


    /**
     * Value of the variable in the current solution with the given bindings; NULL when the current solution does not
     * bind the variable.
     *
     * @param variable the variable
     * @param solution bindings of the current solution
     * @return value of the variable in the current solution
     */
    public static SqlExpressionIntercode create(Variable variable, VariableBindings solution)
    {
        VariableBinding binding = solution.get(variable);

        if(binding == null)
            return SqlNull.get();

        return new SqlOuterVariable(variable, binding.getMappings(), binding.canBeNull());
    }


    @Override
    public Restrictions getRequirements(ClassRelations relations)
    {
        //NOTE: the value is required of the node holding the EXISTS expression by the expression itself
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, Scope scope, VariableBindings bindings,
            Restriction restriction, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        // the current solution as the node holding the EXISTS expression provides it now
        VariableBindings solution = scope.getCurrentSolution();

        if(solution == null)
            return this;

        VariableBinding binding = solution.get(variable);

        if(binding == null)
            return SqlNull.get();

        Map<ResourceClass, List<Column>> mappings = restriction.restrict(relations, binding.getMappings());

        if(mappings.equals(variableBinding.getMappings()) && binding.canBeNull() == variableBinding.canBeNull())
            return this;

        return new SqlOuterVariable(variable, mappings, binding.canBeNull());
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("outer ");
        builder.append(variable);
    }


    /**
     * The variable of the current solution.
     *
     * @return the variable of the current solution
     */
    public Variable getVariable()
    {
        return variable;
    }


    @Override
    public boolean hasSameValue(SqlExpressionIntercode other)
    {
        //NOTE: the value is read from the current solution in effect, which the optimisation changes for both alike
        return other instanceof SqlOuterVariable outer && variable.equals(outer.variable);
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

        if(!(object instanceof SqlOuterVariable imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!variable.equals(imcode.variable))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(variable);
    }
}
