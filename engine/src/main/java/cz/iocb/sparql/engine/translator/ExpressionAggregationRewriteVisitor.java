package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.toList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.model.expression.BracketedExpression;
import cz.iocb.sparql.engine.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.model.expression.ExistsExpression;
import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.expression.FunctionCallExpression;
import cz.iocb.sparql.engine.model.expression.InExpression;
import cz.iocb.sparql.engine.model.expression.LiteralNode;
import cz.iocb.sparql.engine.model.expression.UnaryExpression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



public class ExpressionAggregationRewriteVisitor extends ElementVisitor<Expression>
{
    private static final String variablePrefix = "@aggregationvar";
    private final TranslateVisitor parent;
    private final LinkedHashMap<VariableNode, BuiltInCallExpression> aggregations = new LinkedHashMap<>();
    private final Set<VariableNode> scopeVars;
    private final Set<VariableNode> groupVars;


    public ExpressionAggregationRewriteVisitor(TranslateVisitor parent, Set<VariableNode> scopeVars,
            Set<VariableNode> groupVars)
    {
        this.parent = parent;
        this.scopeVars = scopeVars;
        this.groupVars = groupVars;
    }


    @Override
    public Expression visit(BinaryExpression binaryExpression)
    {
        Expression left = visitElement(binaryExpression.getLeft());
        Expression right = visitElement(binaryExpression.getRight());
        Expression result = new BinaryExpression(binaryExpression.getOperator(), left, right);
        result.setRange(binaryExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(InExpression inExpression)
    {
        Expression left = visitElement(inExpression.getLeft());
        List<Expression> right = inExpression.getRight().stream().map(e -> visitElement(e)).toList();
        Expression result = new InExpression(left, right, inExpression.isNegated());
        result.setRange(inExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(UnaryExpression unaryExpression)
    {
        Expression operand = visitElement(unaryExpression.getOperand());
        Expression result = new UnaryExpression(unaryExpression.getOperator(), operand);
        result.setRange(unaryExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(BracketedExpression bracketedExpression)
    {
        Expression result = new BracketedExpression(visitElement(bracketedExpression.getChild()));
        result.setRange(bracketedExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(BuiltInCallExpression builtInCallExpression)
    {
        if(builtInCallExpression.isAggregateFunction())
        {
            if(builtInCallExpression.getFunctionName().equalsIgnoreCase("count")
                    && builtInCallExpression.getArguments().isEmpty())
            {
                List<Expression> args = builtInCallExpression.isDistinct() ?
                        scopeVars.stream().map(v -> (Expression) v).collect(toList()) : List.of();

                BuiltInCallExpression card = new BuiltInCallExpression("card", args);
                card.setRange(builtInCallExpression.getRange());

                builtInCallExpression = card;
            }

            VariableNode result = parent.createVariableNode(variablePrefix);
            aggregations.put(result, builtInCallExpression);
            result.setRange(builtInCallExpression.getRange());
            return result;
        }
        else
        {
            List<Expression> arguments = builtInCallExpression.getArguments().stream().map(e -> visitElement(e))
                    .toList();
            Expression result = new BuiltInCallExpression(builtInCallExpression.getFunctionName(), arguments);
            result.setRange(builtInCallExpression.getRange());
            return result;
        }
    }


    @Override
    public Expression visit(ExistsExpression existsExpression)
    {
        return existsExpression;
    }


    @Override
    public Expression visit(FunctionCallExpression functionCallExpression)
    {
        List<Expression> arguments = functionCallExpression.getArguments().stream().map(e -> visitElement(e)).toList();
        Expression result = new FunctionCallExpression(functionCallExpression.getFunction(), arguments);
        result.setRange(functionCallExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(IriNode iri)
    {
        return iri;
    }


    @Override
    public Expression visit(LiteralNode literal)
    {
        return literal;
    }


    @Override
    public Expression visit(VariableNode variable)
    {
        if(groupVars.contains(variable))
            return variable;


        BuiltInCallExpression builtInCallExpression = new BuiltInCallExpression("sample", List.of(variable));
        builtInCallExpression.setRange(variable.getRange());

        VariableNode result = parent.createVariableNode(variablePrefix);
        aggregations.put(result, builtInCallExpression);
        result.setRange(variable.getRange());

        return result;
    }


    public LinkedHashMap<VariableNode, BuiltInCallExpression> getAggregations()
    {
        return aggregations;
    }
}
