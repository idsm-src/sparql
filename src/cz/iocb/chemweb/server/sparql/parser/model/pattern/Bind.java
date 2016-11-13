package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;



/**
 * Pattern that assigns a value ({@link #getExpression}) to a variable ( {@link #getVariable}).
 *
 * <p>
 * Corresponds to the rule [60] Bind in the SPARQL grammar.
 */
public class Bind extends BaseElement implements Pattern
{
    private Expression expression;
    private Variable variable;

    public Bind()
    {
    }

    public Bind(Expression expression, Variable variable)
    {
        this.expression = expression;
        this.variable = variable;
    }

    public Expression getExpression()
    {
        return expression;
    }

    public void setExpression(Expression expression)
    {
        this.expression = expression;
    }

    public Variable getVariable()
    {
        return variable;
    }

    public void setVariable(Variable variable)
    {
        this.variable = variable;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
