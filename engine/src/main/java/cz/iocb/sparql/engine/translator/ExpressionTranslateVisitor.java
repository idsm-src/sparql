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
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryArithmetic;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
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
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



public class ExpressionTranslateVisitor extends ElementVisitor<SqlExpressionIntercode>
{
    private final Request request;
    private final VariableBindings bindings;
    private final TranslateVisitor parent;
    private final Prologue prologue;


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
        Operator operator = binaryExpression.getOperator();
        SqlExpressionIntercode left = visitElement(binaryExpression.getLeft());
        SqlExpressionIntercode right = visitElement(binaryExpression.getRight());

        return switch(operator)
        {
            case And -> SqlBinaryLogical.create(AND, SqlEffectiveBooleanValue.create(left),
                    SqlEffectiveBooleanValue.create(right));
            case Or -> SqlBinaryLogical.create(OR, SqlEffectiveBooleanValue.create(left),
                    SqlEffectiveBooleanValue.create(right));
            case Add -> SqlBinaryArithmetic.create(ADD, left, right);
            case Subtract -> SqlBinaryArithmetic.create(SUBTRACT, left, right);
            case Multiply -> SqlBinaryArithmetic.create(MULTIPLY, left, right);
            case Divide -> SqlBinaryArithmetic.create(DIVIDE, left, right);
            case Equals -> SqlBinaryComparison.create(EQUAL, left, right);
            case NotEquals -> SqlBinaryComparison.create(NOT_EQUAL, left, right);
            case LessThan -> SqlBinaryComparison.create(LESS_THAN, left, right);
            case LessThanOrEqual -> SqlBinaryComparison.create(LESS_THAN_OR_EQUAL, left, right);
            case GreaterThan -> SqlBinaryComparison.create(GREATER_THAN, left, right);
            case GreaterThanOrEqual -> SqlBinaryComparison.create(GREATER_THAN_OR_EQUAL, left, right);
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

        return SqlInExpression.create(inExpression.isNegated(), left, right);
    }


    @Override
    public SqlExpressionIntercode visit(UnaryExpression unaryExpression)
    {
        SqlExpressionIntercode operand = visitElement(unaryExpression.getOperand());

        switch(unaryExpression.getOperator())
        {
            case Plus:
                return SqlUnaryArithmetic.create(false, operand);

            case Minus:
                return SqlUnaryArithmetic.create(true, operand);

            case Not:
                return SqlUnaryLogical.create(SqlEffectiveBooleanValue.create(operand));
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
            arguments.set(0, SqlEffectiveBooleanValue.create(arguments.get(0)));

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
        Iri iri = getIri(functionCallExpression.getFunction());
        List<SqlExpressionIntercode> arguemnts = new LinkedList<>();

        for(Expression expression : functionCallExpression.getArguments())
            arguemnts.add(visitElement(expression));

        Datatype datatype = request.getConfiguration().getDatatype(iri);

        //TODO: add support for casting to user literals

        if(datatype != null && !(datatype.getGeneralLiteralClass() instanceof UserLiteralClass))
            return SqlCast.create(datatype.getGeneralLiteralClass(), arguemnts.get(0));

        FunctionDefinition definition = request.getConfiguration().getFunctions(parent.getService())
                .get(iri.getValue());

        return SqlFunctionCall.create(definition, arguemnts);
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


    @Override
    public SqlExpressionIntercode visit(VariableNode variable)
    {
        return SqlVariable.create(bindings.get(getVariable(variable)));
    }
}
