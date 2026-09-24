package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern that assigns a value ({@link #getExpression}) to a variable ( {@link #getVariable}).
 *
 * <p>
 * Corresponds to the rule [60] Bind in the SPARQL grammar.
 */
public class Bind extends PatternElement implements Pattern
{
    /**
     * Expression computing the value.
     */
    private final Expression expression;

    /**
     * Variable receiving the value.
     */
    private final VariableNode variable;


    /**
     * Creates the pattern; the variable becomes in scope.
     *
     * @param expression the expression
     * @param variable the variable
     */
    public Bind(Expression expression, VariableNode variable)
    {
        this.expression = expression;
        this.variable = variable;

        variablesInScope.add(variable);
    }


    /**
     * Expression computing the value.
     *
     * @return expression computing the value
     */
    public Expression getExpression()
    {
        return expression;
    }


    /**
     * Variable receiving the value.
     *
     * @return variable receiving the value
     */
    public VariableNode getVariable()
    {
        return variable;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
