package cz.iocb.sparql.engine.parser;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import cz.iocb.sparql.engine.grammar.SparqlParser.ExprTripleTermContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ExpressionListContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.FunctionCallContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.IriContext;
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
import cz.iocb.sparql.engine.grammar.SparqlParser.VerbContext;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.mapping.datatypes.UserDatatype;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.Prologue;
import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.Range;
import cz.iocb.sparql.engine.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.model.expression.BinaryExpression.Operator;
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
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * Builds {@link Expression}s. Reports aggregates where they are not allowed, unknown extension functions and wrong
 * argument counts (checked against the function definitions of the current service).
 */
public class ExpressionVisitor extends BaseVisitor<Expression>
{
    /**
     * Configuration providing datatypes and functions.
     */
    private final SparqlDatabaseConfiguration config;

    /**
     * Prologue of the query.
     */
    private final Prologue prologue;

    /**
     * Enclosing GRAPH names, innermost last.
     */
    private final Stack<VarOrIri> graphs;

    /**
     * Enclosing SERVICE names, innermost last.
     */
    private final Stack<VarOrIri> services;

    /**
     * Variable scopes of the query.
     */
    private final VariableScopes scopes;

    /**
     * Blank node labels used so far in the query.
     */
    private final Set<String> usedBlankNodes;

    /**
     * Messages collected during parsing.
     */
    private final List<TranslateMessage> messages;

    /**
     * Whether aggregates may appear in the visited expressions.
     */
    private final boolean allowAggregates;


    /**
     * Creates a visitor; {@code allowAggregates} is true only for projections, HAVING and ORDER BY of a select.
     *
     * @param config the endpoint configuration
     * @param prologue the prologue of the query
     * @param graphs the enclosing GRAPH names
     * @param services the enclosing SERVICE names
     * @param scopes the variable scopes
     * @param usedBlankNodes blank node labels used in the query
     * @param messages the message list to append to
     * @param allowAggregates whether aggregates may appear
     */
    public ExpressionVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, Set<String> usedBlankNodes,
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

        LiteralNode signNumber = (LiteralNode) (right instanceof LiteralNode ? right :
                ((BinaryExpression) right).getLeft());
        Operator operator = signNumber.getValue().startsWith("+") ? Operator.Add : Operator.Subtract;

        //Literal org = signNumber.getLiteral();

        LiteralNode fixed = new LiteralNode(signNumber.getValue().substring(1), signNumber.getType());
        fixed.setRange(new Range(moveByOneCharacter(signNumber.getRange().getStart()), signNumber.getRange().getEnd()));

        if(right instanceof BinaryExpression expression)
            expression.setLeft(fixed);

        return new BinaryExpression(operator, left, right instanceof BinaryExpression ? right : fixed);
    }


    /**
     * The position one character to the right.
     *
     * @param original the position to move
     * @return the position one character to the right
     */
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
    public Expression visitUnaryExpression(UnaryExpressionContext ctx)
    {
        if(ctx.unaryExpression() != null)
            return new UnaryExpression(UnaryExpression.Operator.Not, visit(ctx.unaryExpression()));

        Expression operand = visit(ctx.primaryExpression());

        if(ctx.op == null)
            return operand;

        UnaryExpression.Operator operator = ctx.op.getText().equals("+") ? UnaryExpression.Operator.Plus :
                UnaryExpression.Operator.Minus;

        return new UnaryExpression(operator, operand);
    }


    /**
     * Parses the expressions of an argument list.
     *
     * @param ctx the parse tree node
     * @return the parsed expressions
     */
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


    /**
     * Builds an extension function call and checks the function against the datatypes and the functions of the current
     * service.
     *
     * @param iri IRI of the function
     * @param ctx the parse tree node
     * @return the call
     */
    private FunctionCallExpression parseFunctionCall(IriNode iri, ArgListContext ctx)
    {
        ArgumentsVisitor argumentsVisitor = new ArgumentsVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, false);

        List<Expression> arguments = argumentsVisitor.visit(ctx);

        FunctionCallExpression result = new FunctionCallExpression(iri, arguments);

        if(argumentsVisitor.foundDistinct())
            result.setDistinct(true);


        if(!services.stream()
                .allMatch(s -> s instanceof IriNode n && config.getServices().contains(new Iri(n.getValue()))))
            return result;

        Iri service = services.isEmpty() ? config.getServiceIri() : new Iri(((IriNode) services.peek()).getValue());

        Datatype datatype = config.getDatatype(new Iri(iri.getValue()));

        //TODO: add support for casting to user literals

        if(datatype != null && !(datatype instanceof UserDatatype))
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
        IriNode iri = new IriVisitor(prologue, messages).visit(ctx.iri());

        if(ctx.argList() == null)
            return iri;

        return parseFunctionCall(iri, ctx.argList());
    }


    @Override
    public IriNode visitIri(IriContext ctx)
    {
        return new IriVisitor(prologue, messages).visit(ctx);
    }


    @Override
    public TripleTermNode visitExprTripleTerm(ExprTripleTermContext ctx)
    {
        Node subject = (Node) visit(ctx.exprTripleTermSubject());
        VarOrIri predicate = parseVerb(ctx.verb());
        Node object = (Node) visit(ctx.exprTripleTermObject());

        return new TripleTermNode(subject, predicate, object);
    }


    /**
     * Parses the predicate of a triple term: an IRI, a variable (unbound, like other variables of expressions), or
     * {@code rdf:type} for {@code a}.
     *
     * @param ctx the parse tree node
     * @return the predicate
     */
    private VarOrIri parseVerb(VerbContext ctx)
    {
        if(ctx.A() != null)
            return withRange(new IriNode(Rdf.TYPE), ctx);

        // the VarOrIri rule yields only IRIs and variables
        return (VarOrIri) visit(ctx.varOrIri());
    }


    @Override
    public FunctionCallExpression visitFunctionCall(FunctionCallContext ctx)
    {
        IriNode iri = new IriVisitor(prologue, messages).visit(ctx.iri());

        return parseFunctionCall(iri, ctx.argList());
    }


    @Override
    public VariableNode visitVar(VarContext ctx)
    {
        return new VariableNode(scopes.addToScope(ctx.getText().substring(1), true), ctx.getText());
    }


    @Override
    public LiteralNode visitRdfLiteral(RdfLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitRdfLiteral(ctx);
    }


    @Override
    public LiteralNode visitNumericLiteral(NumericLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitNumericLiteral(ctx);
    }


    @Override
    public LiteralNode visitNumericLiteralPositive(NumericLiteralPositiveContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitNumericLiteralPositive(ctx);
    }


    @Override
    public LiteralNode visitNumericLiteralNegative(NumericLiteralNegativeContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitNumericLiteralNegative(ctx);
    }


    @Override
    public LiteralNode visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return new LiteralVisitor(prologue, messages).visitBooleanLiteral(ctx);
    }
}


/**
 * Collects the argument list of a built-in call, aggregate or extension function call, and records whether it contained
 * {@code DISTINCT}.
 */
class ArgumentsVisitor extends BaseVisitor<List<Expression>>
{
    /**
     * Configuration providing datatypes and functions.
     */
    private final SparqlDatabaseConfiguration config;

    /**
     * Prologue of the query.
     */
    private final Prologue prologue;

    /**
     * Enclosing GRAPH names, innermost last.
     */
    private final Stack<VarOrIri> graphs;

    /**
     * Enclosing SERVICE names, innermost last.
     */
    private final Stack<VarOrIri> services;

    /**
     * Variable scopes of the query.
     */
    private final VariableScopes scopes;

    /**
     * Blank node labels used so far in the query.
     */
    private final Set<String> usedBlankNodes;

    /**
     * Messages collected during parsing.
     */
    private final List<TranslateMessage> messages;

    /**
     * Whether aggregates may appear in the arguments.
     */
    private final boolean allowAggregates;

    /**
     * Set when the visited list started with DISTINCT.
     */
    private boolean foundDistinct = false;


    /**
     * Creates the visitor sharing the state of the enclosing expression visitor.
     *
     * @param config the endpoint configuration
     * @param prologue the prologue of the query
     * @param graphs the enclosing GRAPH names
     * @param services the enclosing SERVICE names
     * @param scopes the variable scopes
     * @param usedBlankNodes blank node labels used in the query
     * @param messages the message list to append to
     * @param allowAggregates whether aggregates may appear
     */
    public ArgumentsVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, Set<String> usedBlankNodes,
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


    /**
     * True if the visited argument list started with {@code DISTINCT}.
     *
     * @return true if the visited argument list started with {@code DISTINCT}, false otherwise
     */
    public boolean foundDistinct()
    {
        return foundDistinct;
    }


    /**
     * Parses each context as an expression.
     *
     * @param contexts the parse tree nodes
     * @return the parsed expressions
     */
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
            result.add(withRange(new LiteralNode(LiteralVisitor.unquote(ctx.string().getText()),
                    new IriNode(xsdStringIri.getValue())), ctx.string()));

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
