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
    private final Expression expression;
    private final VariableNode variable;


    public Bind(Expression expression, VariableNode variable)
    {
        this.expression = expression;
        this.variable = variable;

        variablesInScope.add(variable);
    }


    public Expression getExpression()
    {
        return expression;
    }


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
