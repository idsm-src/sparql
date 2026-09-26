package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.toList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VarOrIri;
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
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.triple.TripleTermNode;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Rewrites the expressions of a grouped select (projections, HAVING, ORDER BY): every aggregate call is replaced by a
 * fresh variable, and a variable not in GROUP BY by a fresh variable bound to {@code SAMPLE} of it. {@code
 * COUNT(*)} becomes the internal {@code card} function (over the in-scope variables when DISTINCT). The replacements
 * are collected in {@link #getAggregations}.
 */
public class ExpressionAggregationRewriteVisitor extends ElementVisitor<Expression>
{
    /**
     * Name prefix of the fresh variables.
     */
    private static final String variablePrefix = "@aggregationvar";

    /**
     * Translator allocating fresh variables.
     */
    private final TranslateVisitor parent;

    /**
     * Fresh variables and the aggregates they stand for.
     */
    private final LinkedHashMap<VariableNode, BuiltInCallExpression> aggregations = new LinkedHashMap<>();

    /**
     * Variables in scope of the WHERE clause that are not grouped ({@code COUNT(DISTINCT *)} counts over them).
     */
    private final Set<VariableNode> scopeVars;

    /**
     * Grouped variables, usable directly.
     */
    private final Set<VariableNode> groupVars;


    /**
     * Creates the rewriter for one grouped select.
     *
     * @param parent translator allocating fresh variables
     * @param scopeVars in-scope variables that are not grouped
     * @param groupVars the grouped variables
     */
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
    public Expression visit(TripleTermNode tripleTerm)
    {
        Node subject = (Node) visitElement(tripleTerm.getSubject());
        VarOrIri predicate = (VarOrIri) visitElement(tripleTerm.getPredicate());
        Node object = (Node) visitElement(tripleTerm.getObject());
        Expression result = new TripleTermNode(subject, predicate, object);
        result.setRange(tripleTerm.getRange());
        return result;
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


    /**
     * Fresh variables and the aggregate expressions they stand for, in order of creation.
     *
     * @return fresh variables and the aggregate expressions they stand for, in order of creation
     */
    public LinkedHashMap<VariableNode, BuiltInCallExpression> getAggregations()
    {
        return aggregations;
    }
}
