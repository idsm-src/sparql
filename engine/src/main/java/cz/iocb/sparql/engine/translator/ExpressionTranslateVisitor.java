package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.ADD;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.DIVIDE;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.MULTIPLY;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.SUBTRACT;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.GREATER_THAN;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.GREATER_THAN_OR_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.LESS_THAN;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.LESS_THAN_OR_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.NOT_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator.AND;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator.OR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLanguageTaggedString;
import static cz.iocb.sparql.engine.translator.TermGenerator.getIri;
import static cz.iocb.sparql.engine.translator.TermGenerator.getLiteral;
import static cz.iocb.sparql.engine.translator.TermGenerator.getVariable;
import java.util.LinkedList;
import java.util.List;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlCast;
import cz.iocb.sparql.engine.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.sparql.engine.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlFunctionCall;
import cz.iocb.sparql.engine.imcode.expression.SqlInExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryArithmetic;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.mapping.datatypes.UserDatatype;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.Prologue;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.Element;
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
import cz.iocb.sparql.engine.model.triple.TripleTermNode;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * Translates AST expressions into {@link SqlExpressionIntercode} over the given variable bindings. Logical operands are
 * wrapped in the effective boolean value, a function IRI naming a datatype becomes a cast, and EXISTS patterns are
 * translated by the parent visitor.
 */
public class ExpressionTranslateVisitor extends ElementVisitor<SqlExpressionIntercode>
{
    /**
     * Current request.
     */
    private final Request request;

    /**
     * Bindings of the variables the expressions may refer to.
     */
    private final VariableBindings bindings;

    /**
     * Translator used for nested patterns (EXISTS) and the current service.
     */
    private final TranslateVisitor parent;

    /**
     * Prologue of the query (BASE for IRI()).
     */
    private final Prologue prologue;


    /**
     * Creates the visitor over the given bindings.
     *
     * @param request the current request
     * @param bindings the variable bindings
     * @param parent translator allocating fresh variables
     */
    public ExpressionTranslateVisitor(Request request, VariableBindings bindings, TranslateVisitor parent)
    {
        this.request = request;
        this.bindings = bindings;
        this.parent = parent;
        this.prologue = parent.getPrologue();
    }


    @Override
    public SqlExpressionIntercode visitElement(Element element)
    {
        return super.visitElement(element);
    }


    @Override
    public SqlExpressionIntercode visit(BinaryExpression binaryExpression)
    {
        ClassRelations relations = request.getConfiguration();

        Operator operator = binaryExpression.getOperator();
        SqlExpressionIntercode left = visitElement(binaryExpression.getLeft());
        SqlExpressionIntercode right = visitElement(binaryExpression.getRight());

        return switch(operator)
        {
            case And -> SqlBinaryLogical.create(relations, AND, SqlEffectiveBooleanValue.create(relations, left),
                    SqlEffectiveBooleanValue.create(relations, right));
            case Or -> SqlBinaryLogical.create(relations, OR, SqlEffectiveBooleanValue.create(relations, left),
                    SqlEffectiveBooleanValue.create(relations, right));
            case Add -> SqlBinaryArithmetic.create(relations, ADD, left, right);
            case Subtract -> SqlBinaryArithmetic.create(relations, SUBTRACT, left, right);
            case Multiply -> SqlBinaryArithmetic.create(relations, MULTIPLY, left, right);
            case Divide -> SqlBinaryArithmetic.create(relations, DIVIDE, left, right);
            case Equals -> SqlBinaryComparison.create(relations, EQUAL, left, right);
            case NotEquals -> SqlBinaryComparison.create(relations, NOT_EQUAL, left, right);
            case LessThan -> SqlBinaryComparison.create(relations, LESS_THAN, left, right);
            case LessThanOrEqual -> SqlBinaryComparison.create(relations, LESS_THAN_OR_EQUAL, left, right);
            case GreaterThan -> SqlBinaryComparison.create(relations, GREATER_THAN, left, right);
            case GreaterThanOrEqual -> SqlBinaryComparison.create(relations, GREATER_THAN_OR_EQUAL, left, right);
            default -> null;
        };
    }


    @Override
    public SqlExpressionIntercode visit(InExpression inExpression)
    {
        SqlExpressionIntercode left = visitElement(inExpression.getLeft());
        List<SqlExpressionIntercode> right = new LinkedList<>();

        for(Expression expression : inExpression.getRight())
            right.add(visitElement(expression));

        return SqlInExpression.create(request.getConfiguration(), inExpression.isNegated(), left, right);
    }


    @Override
    public SqlExpressionIntercode visit(UnaryExpression unaryExpression)
    {
        ClassRelations relations = request.getConfiguration();

        SqlExpressionIntercode operand = visitElement(unaryExpression.getOperand());

        switch(unaryExpression.getOperator())
        {
            case Plus:
                return SqlUnaryArithmetic.create(relations, false, operand);

            case Minus:
                return SqlUnaryArithmetic.create(relations, true, operand);

            case Not:
                return SqlUnaryLogical.create(relations, SqlEffectiveBooleanValue.create(relations, operand));
        }

        return null;
    }


    @Override
    public SqlExpressionIntercode visit(BracketedExpression bracketedExpression)
    {
        return visitElement(bracketedExpression.getChild());
    }


    @Override
    public SqlExpressionIntercode visit(BuiltInCallExpression builtInCallExpression)
    {
        String function = builtInCallExpression.getFunctionName();
        List<SqlExpressionIntercode> arguments = new LinkedList<>();

        for(Expression expression : builtInCallExpression.getArguments())
            arguments.add(visitElement(expression));

        if(function.equalsIgnoreCase("iri") || function.equalsIgnoreCase("uri"))
            arguments.add(SqlIri.create(request, new Iri(prologue.getBase())));

        if(function.equalsIgnoreCase("if") && arguments.size() > 0)
            arguments.set(0, SqlEffectiveBooleanValue.create(request.getConfiguration(), arguments.get(0)));

        return SqlBuiltinCall.create(request, function.toLowerCase(), builtInCallExpression.isDistinct(), arguments);
    }


    @Override
    public SqlExpressionIntercode visit(ExistsExpression existsExpression)
    {
        SqlIntercode pattern = parent.visitElement(existsExpression.getPattern());
        return SqlExists.create(request, existsExpression.isNegated(), pattern, bindings);
    }


    @Override
    public SqlExpressionIntercode visit(FunctionCallExpression functionCallExpression)
    {
        ClassRelations relations = request.getConfiguration();

        Iri iri = getIri(functionCallExpression.getFunction());
        List<SqlExpressionIntercode> arguemnts = new LinkedList<>();

        for(Expression expression : functionCallExpression.getArguments())
            arguemnts.add(visitElement(expression));

        Datatype datatype = request.getConfiguration().getDatatype(iri);

        //TODO: add support for casting to user literals

        if(datatype != null && !(datatype instanceof UserDatatype))
        {
            // there is no cast to a language-tagged string (rdf:langString, rdf:dirLangString)
            if(!(datatype.getCanonicalLiteralClass() instanceof LiteralClass literalClass)
                    || isLanguageTaggedString(literalClass))
                return SqlNull.get();

            return SqlCast.create(relations, literalClass, arguemnts.get(0));
        }

        FunctionDefinition definition = request.getConfiguration().getFunctions(parent.getService())
                .get(iri.getValue());

        return SqlFunctionCall.create(relations, definition, arguemnts);
    }


    @Override
    public SqlExpressionIntercode visit(IriNode iri)
    {
        return SqlIri.create(request, getIri(iri));
    }


    @Override
    public SqlExpressionIntercode visit(LiteralNode literal)
    {
        return SqlLiteral.create(request, getLiteral(literal));
    }


    /**
     * A triple term written in an expression is the shorthand of the function {@code TRIPLE} applied to its components
     * (SPARQL 1.2, section 17.4.6.1), so it is translated as that call.
     */
    @Override
    public SqlExpressionIntercode visit(TripleTermNode tripleTerm)
    {
        List<SqlExpressionIntercode> arguments = List.of(visitElement(tripleTerm.getSubject()),
                visitElement(tripleTerm.getPredicate()), visitElement(tripleTerm.getObject()));

        return SqlBuiltinCall.create(request, "triple", false, arguments);
    }


    @Override
    public SqlExpressionIntercode visit(VariableNode variable)
    {
        return SqlVariable.create(request.getConfiguration(), bindings.get(getVariable(variable)));
    }
}
