package cz.iocb.sparql.engine.imcode.expression;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
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
 * resolved against them; the join keeps its columns available.
 */
public final class SqlLateralVariable extends SqlExpressionIntercode
{
    /**
     * The variable of the left side.
     */
    private final Variable variable;


    /**
     * Creates the expression.
     *
     * @param variable the variable of the left side
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    private SqlLateralVariable(Variable variable, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(mappings, canBeNull, true);

        this.variable = variable;
    }


    /**
     * Reference to the variable of the binding, one of the bindings returned by
     * {@link SqlLateralJoin#getLateralVariableBindings}.
     *
     * @param binding the binding of the variable as seen by the right side
     * @return reference to the variable of the binding
     */
    public static SqlExpressionIntercode create(VariableBinding binding)
    {
        return new SqlLateralVariable(binding.getVariable(), binding.getMappings(), binding.canBeNull());
    }


    @Override
    public Restrictions getRequirements(ClassRelations relations)
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        if(restriction.isOptimized(relations, variableBinding))
            return this;

        return new SqlLateralVariable(variable, restriction.restrict(relations, variableBinding.getMappings()),
                variableBinding.canBeNull());
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
    public Set<VirtualTable> getVirtualTables()
    {
        return Set.of();
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
