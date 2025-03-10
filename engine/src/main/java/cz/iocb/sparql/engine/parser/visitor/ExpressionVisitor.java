package cz.iocb.sparql.engine.parser.visitor;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdStringType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import org.antlr.v4.runtime.ParserRuleContext;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.grammar.SparqlParser.AdditiveExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.AggregateContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ArgListContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BooleanLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BuiltInCallContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ConditionalAndExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ConditionalOrExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ConstraintContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ExistsFunctionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ExpressionListContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.FunctionCallContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.IriRefOrFunctionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.MultiplicativeExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NotExistsFunctionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralNegativeContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NumericLiteralPositiveContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.PrimaryExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.RdfLiteralContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.RegexExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.RelationalExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.RelationalSetExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.StrReplaceExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.SubStringExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.UnaryAdditiveExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.UnaryExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.UnaryLiteralExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.UnaryNegationExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.UnarySignedLiteralExpressionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarContext;
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.parser.Position;
import cz.iocb.sparql.engine.parser.Range;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Prologue;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.parser.model.expression.BracketedExpression;
import cz.iocb.sparql.engine.parser.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.ExistsExpression;
import cz.iocb.sparql.engine.parser.model.expression.Expression;
import cz.iocb.sparql.engine.parser.model.expression.FunctionCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.InExpression;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.expression.UnaryExpression;



public class ExpressionVisitor extends BaseVisitor<Expression>
{
    private final SparqlDatabaseConfiguration config;
    private final Prologue prologue;
    private final Stack<VarOrIri> graphs;
    private final Stack<VarOrIri> services;
    private final VariableScopes scopes;
    private final HashSet<String> usedBlankNodes;
    private final List<TranslateMessage> messages;
    private final boolean allowAggregates;


    public ExpressionVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, HashSet<String> usedBlankNodes,
            List<TranslateMessage> messages, boolean allowAggregates)
    {
        this.config = config;
        this.prologue = prologue;
        this.graphs = graphs;
        this.services = services;
        this.scopes = scopes;
        this.usedBlankNodes = usedBlankNodes;
        this.messages = messages;
        this.allowAggregates = allowAggregates;
    }


    @Override
    public Expression visitConstraint(ConstraintContext ctx)
    {
        if(ctx.expression() != null)
        {
            return new BracketedExpression(visit(ctx.expression()));
        }

        return super.visitConstraint(ctx);
    }


    @Override
    public Expression visitPrimaryExpression(PrimaryExpressionContext ctx)
    {
        if(ctx.expression() != null)
        {
            return new BracketedExpression(visit(ctx.expression()));
        }

        return super.visitPrimaryExpression(ctx);
    }


    @Override
    public UnaryExpression visitUnaryAdditiveExpression(UnaryAdditiveExpressionContext ctx)
    {
        UnaryExpression.Operator operator = ctx.op.getText().equals("+") ? UnaryExpression.Operator.Plus :
                UnaryExpression.Operator.Minus;

        return new UnaryExpression(operator, visit(ctx.expression()));
    }


    @Override
    public UnaryExpression visitUnaryNegationExpression(UnaryNegationExpressionContext ctx)
    {
        return new UnaryExpression(UnaryExpression.Operator.Not, visit(ctx.expression()));
    }


    @Override
    public BinaryExpression visitMultiplicativeExpression(MultiplicativeExpressionContext ctx)
    {
        BinaryExpression.Operator operator = ctx.op.getText().equals("*") ? BinaryExpression.Operator.Multiply :
                BinaryExpression.Operator.Divide;

        return new BinaryExpression(operator, visit(ctx.expression(0)), visit(ctx.expression(1)));
    }


    @Override
    public Expression visitAdditiveExpression(AdditiveExpressionContext ctx)
    {
        BinaryExpression.Operator operator = ctx.op.getText().equals("+") ? BinaryExpression.Operator.Add :
                BinaryExpression.Operator.Subtract;

        return new BinaryExpression(operator, visit(ctx.expression(0)), visit(ctx.expression(1)));
    }


    @Override
    public BinaryExpression visitUnarySignedLiteralExpression(UnarySignedLiteralExpressionContext ctx)
    {
        Expression left = visit(ctx.expression());
        Expression right = visit(ctx.unaryLiteralExpression());

        Literal signNumber = (Literal) (right instanceof Literal ? right : ((BinaryExpression) right).getLeft());
        Operator operator = signNumber.getStringValue().startsWith("+") ? Operator.Add : Operator.Subtract;

        Literal fixed = new Literal(signNumber.getStringValue().substring(1), signNumber.getDataType());
        fixed.setRange(new Range(moveByOneCharacter(signNumber.getRange().getStart()), signNumber.getRange().getEnd()));

        if(right instanceof BinaryExpression expression)
            expression.setLeft(fixed);

        return new BinaryExpression(operator, left, right instanceof BinaryExpression ? right : fixed);
    }


    private static Position moveByOneCharacter(Position original)
    {
        return new Position(original.getLineNumber(), original.getPositionInLine() + 1);
    }


    @Override
    public Expression visitUnaryLiteralExpression(UnaryLiteralExpressionContext ctx)
    {
        ParserRuleContext leftCtx = ctx.numericLiteralPositive() != null ? ctx.numericLiteralPositive() :
                ctx.numericLiteralNegative();
        Expression left = visit(leftCtx);

        if(ctx.op == null)
            return left;

        BinaryExpression.Operator operator = ctx.op.getText().equals("*") ? BinaryExpression.Operator.Multiply :
                BinaryExpression.Operator.Divide;

        Expression right = visit(ctx.unaryExpression());

        return new BinaryExpression(operator, left, right);
    }


    @Override
    public UnaryExpression visitUnaryExpression(UnaryExpressionContext ctx)
    {
        UnaryExpression.Operator operator;
        switch(ctx.op.getText())
        {
            case "!":
                operator = UnaryExpression.Operator.Not;
                break;
            case "+":
                operator = UnaryExpression.Operator.Plus;
                break;
            case "-":
                operator = UnaryExpression.Operator.Minus;
                break;
            default:
                throw new AssertionError();
        }

        return new UnaryExpression(operator, visit(ctx.primaryExpression()));
    }


    private List<Expression> parseExpressionList(ExpressionListContext ctx)
    {
        if(ctx == null)
            return new ArrayList<>();

        return ctx.expression().stream().map(this::visit).toList();
    }


    @Override
    public InExpression visitRelationalSetExpression(RelationalSetExpressionContext ctx)
    {
        Expression left = visit(ctx.expression());
        List<Expression> right = parseExpressionList(ctx.expressionList());
        boolean negated = ctx.NOT() != null;

        return new InExpression(left, right, negated);
    }


    @Override
    public BinaryExpression visitRelationalExpression(RelationalExpressionContext ctx)
    {
        BinaryExpression.Operator operator;
        switch(ctx.op.getText())
        {
            case "=":
                operator = BinaryExpression.Operator.Equals;
                break;
            case "!=":
                operator = BinaryExpression.Operator.NotEquals;
                break;
            case "<":
                operator = BinaryExpression.Operator.LessThan;
                break;
            case ">":
                operator = BinaryExpression.Operator.GreaterThan;
                break;
            case "<=":
                operator = BinaryExpression.Operator.LessThanOrEqual;
                break;
            case ">=":
                operator = BinaryExpression.Operator.GreaterThanOrEqual;
                break;
            default:
                throw new AssertionError();
        }

        return new BinaryExpression(operator, visit(ctx.expression(0)), visit(ctx.expression(1)));
    }


    @Override
    public BinaryExpression visitConditionalAndExpression(ConditionalAndExpressionContext ctx)
    {
        return new BinaryExpression(BinaryExpression.Operator.And, visit(ctx.expression(0)), visit(ctx.expression(1)));
    }


    @Override
    public BinaryExpression visitConditionalOrExpression(ConditionalOrExpressionContext ctx)
    {
        return new BinaryExpression(BinaryExpression.Operator.Or, visit(ctx.expression(0)), visit(ctx.expression(1)));
    }


    @Override
    public Expression visitBuiltInCall(BuiltInCallContext ctx)
    {
        // for alternatives that are handled in a special way ((NOT) EXISTS)
        Expression superResult = super.visitBuiltInCall(ctx);

        if(superResult != null)
            return superResult;


        String functionName = ctx.children.get(0).getText();

        ArgumentsVisitor argumentsVisitor = new ArgumentsVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, allowAggregates);

        List<Expression> arguments = argumentsVisitor.visitBuiltInCall(ctx);

        return new BuiltInCallExpression(functionName, arguments);
    }


    @Override
    public Expression visitRegexExpression(RegexExpressionContext ctx)
    {
        String functionName = ctx.children.get(0).getText();

        ArgumentsVisitor argumentsVisitor = new ArgumentsVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, allowAggregates);

        List<Expression> arguments = argumentsVisitor.visitRegexExpression(ctx);

        return new BuiltInCallExpression(functionName, arguments);
    }


    @Override
    public Expression visitSubStringExpression(SubStringExpressionContext ctx)
    {
        String functionName = ctx.children.get(0).getText();

        ArgumentsVisitor argumentsVisitor = new ArgumentsVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, allowAggregates);

        List<Expression> arguments = argumentsVisitor.visitSubStringExpression(ctx);

        return new BuiltInCallExpression(functionName, arguments);
    }


    @Override
    public Expression visitStrReplaceExpression(StrReplaceExpressionContext ctx)
    {
        String functionName = ctx.children.get(0).getText();

        ArgumentsVisitor argumentsVisitor = new ArgumentsVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, allowAggregates);

        List<Expression> arguments = argumentsVisitor.visitStrReplaceExpression(ctx);

        return new BuiltInCallExpression(functionName, arguments);
    }


    @Override
    public Expression visitAggregate(AggregateContext ctx)
    {
        if(!allowAggregates)
            messages.add(new TranslateMessage(MessageType.invalidContextOfAggregate, Range.compute((ctx))));

        String functionName = ctx.children.get(0).getText();

        ArgumentsVisitor argumentsVisitor = new ArgumentsVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, allowAggregates);

        List<Expression> arguments = argumentsVisitor.visitAggregate(ctx);

        BuiltInCallExpression result = new BuiltInCallExpression(functionName, arguments);

        if(argumentsVisitor.foundDistinct())
            result.setDistinct(true);

        return result;
    }


    @Override
    public ExistsExpression visitExistsFunction(ExistsFunctionContext ctx)
    {
        scopes.addScope();

        try
        {
            return new ExistsExpression(
                    new GraphPatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages)
                            .visit(ctx.groupGraphPattern()),
                    false);
        }
        finally
        {
            scopes.popScope();
        }
    }


    @Override
    public ExistsExpression visitNotExistsFunction(NotExistsFunctionContext ctx)
    {
        scopes.addScope();

        try
        {
            return new ExistsExpression(
                    new GraphPatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages)
                            .visit(ctx.groupGraphPattern()),
                    true);
        }
        finally
        {
            scopes.popScope();
        }
    }


    private FunctionCallExpression parseFunctionCall(IRI iri, ArgListContext ctx)
    {
        ArgumentsVisitor argumentsVisitor = new ArgumentsVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, false);

        List<Expression> arguments = argumentsVisitor.visit(ctx);

        FunctionCallExpression result = new FunctionCallExpression(iri, arguments);

        if(argumentsVisitor.foundDistinct())
            result.setDistinct(true);


        if(!services.stream().allMatch(s -> s instanceof IRI && config.getServices().contains(s)))
            return result;

        IRI service = services.isEmpty() ? config.getServiceIri() : (IRI) services.peek();

        DataType datatype = config.getDataType(iri);

        //TODO: add support for casting to user literals
        if(datatype != null && !(datatype.getGeneralLiteralClass() instanceof UserLiteralClass))
        {
            if(arguments.size() != 1)
                messages.add(new TranslateMessage(MessageType.wrongCountOfParameters, iri.getRange(),
                        iri.toString(prologue), 1));
        }
        else
        {
            FunctionDefinition definition = config.getFunctions(service).get(iri.getValue());

            if(definition == null)
                messages.add(new TranslateMessage(MessageType.unimplementedFunction, iri.getRange(),
                        iri.toString(prologue)));
            else if(arguments.size() < definition.getRequiredArgumentCount()
                    && arguments.size() > definition.getArgumentClasses().size())
                messages.add(new TranslateMessage(MessageType.wrongCountOfParameters, iri.getRange(),
                        iri.toString(prologue), definition.getArgumentClasses().size()));
        }

        return result;
    }


    @Override
    public Expression visitIriRefOrFunction(IriRefOrFunctionContext ctx)
    {
        IRI iri = new IriVisitor(prologue, messages).visit(ctx.iri());

        if(ctx.argList() == null)
            return iri;

        return parseFunctionCall(iri, ctx.argList());
    }


    @Override
    public FunctionCallExpression visitFunctionCall(FunctionCallContext ctx)
    {
        IRI iri = new IriVisitor(prologue, messages).visit(ctx.iri());

        return parseFunctionCall(iri, ctx.argList());
    }


    @Override
    public Variable visitVar(VarContext ctx)
    {
        return new Variable(scopes.addToScope(ctx.getText().substring(1), true), ctx.getText());
    }


    @Override
    public Literal visitRdfLiteral(RdfLiteralContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitRdfLiteral(ctx);
    }


    @Override
    public Literal visitNumericLiteral(NumericLiteralContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitNumericLiteral(ctx);
    }


    @Override
    public Literal visitNumericLiteralPositive(NumericLiteralPositiveContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitNumericLiteralPositive(ctx);
    }


    @Override
    public Literal visitNumericLiteralNegative(NumericLiteralNegativeContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitNumericLiteralNegative(ctx);
    }


    @Override
    public Literal visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return new LiteralVisitor(config, prologue, messages).visitBooleanLiteral(ctx);
    }
}


class ArgumentsVisitor extends BaseVisitor<List<Expression>>
{
    private final SparqlDatabaseConfiguration config;
    private final Prologue prologue;
    private final Stack<VarOrIri> graphs;
    private final Stack<VarOrIri> services;
    private final VariableScopes scopes;
    private final HashSet<String> usedBlankNodes;
    private final List<TranslateMessage> messages;
    private final boolean allowAggregates;
    private boolean foundDistinct = false;


    public ArgumentsVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, HashSet<String> usedBlankNodes,
            List<TranslateMessage> messages, boolean allowAggregates)
    {
        this.config = config;
        this.prologue = prologue;
        this.graphs = graphs;
        this.services = services;
        this.scopes = scopes;
        this.usedBlankNodes = usedBlankNodes;
        this.messages = messages;
        this.allowAggregates = allowAggregates;
    }


    public boolean foundDistinct()
    {
        return foundDistinct;
    }


    private List<Expression> visitExpressions(List<? extends ParserRuleContext> contexts)
    {
        return contexts.stream().map(new ExpressionVisitor(config, prologue, graphs, services, scopes, usedBlankNodes,
                messages, allowAggregates)::visit).toList();
    }


    @Override
    public List<Expression> visitBuiltInCall(BuiltInCallContext ctx)
    {
        // for alternatives that have their own rules
        List<Expression> superResult = super.visitBuiltInCall(ctx);

        if(superResult != null)
            return superResult;

        if(!ctx.expression().isEmpty())
        {
            return visitExpressions(ctx.expression());
        }

        if(ctx.expressionList() != null)
        {
            return visit(ctx.expressionList());
        }

        if(ctx.var() != null)
        {
            List<Expression> result = new ArrayList<>();
            result.add(new ExpressionVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages,
                    allowAggregates).visit(ctx.var()));
            return result;
        }

        return new ArrayList<>();
    }


    @Override
    public List<Expression> visitExpressionList(ExpressionListContext ctx)
    {
        return visitExpressions(ctx.expression());
    }


    @Override
    public List<Expression> visitAggregate(AggregateContext ctx)
    {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, false);

        if(ctx.DISTINCT() != null)
            foundDistinct = true;

        if(ctx.ASTERISK() != null)
            return new ArrayList<>();

        List<Expression> result = new ArrayList<>();
        result.add(expressionVisitor.visit(ctx.expression()));

        if(ctx.string() != null)
            result.add(withRange(new Literal(LiteralVisitor.unquote(ctx.string().getText()), xsdStringType),
                    ctx.string()));

        return result;
    }


    @Override
    public List<Expression> visitSubStringExpression(SubStringExpressionContext ctx)
    {
        return visitExpressions(ctx.expression());
    }


    @Override
    public List<Expression> visitStrReplaceExpression(StrReplaceExpressionContext ctx)
    {
        return visitExpressions(ctx.expression());
    }


    @Override
    public List<Expression> visitRegexExpression(RegexExpressionContext ctx)
    {
        return visitExpressions(ctx.expression());
    }


    @Override
    public List<Expression> visitArgList(ArgListContext ctx)
    {
        if(ctx.DISTINCT() != null)
            foundDistinct = true;

        if(ctx.expressionList() == null)
            return new ArrayList<>();

        return visit(ctx.expressionList());
    }
}
