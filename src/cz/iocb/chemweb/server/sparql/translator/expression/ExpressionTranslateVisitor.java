package cz.iocb.chemweb.server.sparql.translator.expression;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.error.MessageType;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.FunctionDefinition;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BracketedExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.ExistsExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.FunctionCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.InExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.expression.UnaryExpression;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryLogical;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlCast;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExists;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionError;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlFunctionCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlInExpression;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlIri;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlLiteral;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlUnaryArithmetic;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlUnaryLogical;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class ExpressionTranslateVisitor extends ElementVisitor<SqlExpressionIntercode>
{
    private final VariableAccessor variableAccessor;
    private final TranslateVisitor parentTranslator;
    private final Request request;
    private final Prologue prologue;
    private final List<TranslateMessage> messages;
    private final HashMap<String, FunctionDefinition> functions;


    public ExpressionTranslateVisitor(VariableAccessor variableAccessor, TranslateVisitor parentTranslator)
    {
        this.variableAccessor = variableAccessor;
        this.parentTranslator = parentTranslator;
        this.request = parentTranslator.getRequest();
        this.prologue = parentTranslator.getPrologue();
        this.messages = parentTranslator.getMessages();
        this.functions = parentTranslator.getRequest().getConfiguration().getFunctions(parentTranslator.getService());
    }


    @Override
    public SqlExpressionIntercode visitElement(Element element)
    {
        if(element == null)
            return SqlExpressionError.create();

        return super.visitElement(element);
    }


    @Override
    public SqlExpressionIntercode visit(BinaryExpression binaryExpression)
    {
        Operator operator = binaryExpression.getOperator();
        SqlExpressionIntercode left = visitElement(binaryExpression.getLeft());
        SqlExpressionIntercode right = visitElement(binaryExpression.getRight());

        switch(operator)
        {
            case And:
            case Or:
                return SqlBinaryLogical.create(operator, left, right);

            case Add:
            case Subtract:
            case Multiply:
            case Divide:
                return SqlBinaryArithmetic.create(operator, left, right);

            case Equals:
            case NotEquals:
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                return SqlBinaryComparison.create(operator, left, right);
        }

        return null;
    }


    @Override
    public SqlExpressionIntercode visit(InExpression inExpression)
    {
        SqlExpressionIntercode left = visitElement(inExpression.getLeft());
        List<SqlExpressionIntercode> right = new LinkedList<SqlExpressionIntercode>();

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
                return SqlUnaryLogical.create(operand);
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
        List<SqlExpressionIntercode> arguments = new LinkedList<SqlExpressionIntercode>();

        for(Expression expression : builtInCallExpression.getArguments())
            arguments.add(visitElement(expression));

        if(function.equalsIgnoreCase("iri") || function.equalsIgnoreCase("uri"))
            arguments.add(SqlIri.create(prologue.getBase(), request));

        if(function.equalsIgnoreCase("count") && builtInCallExpression.isDistinct() && arguments.size() == 0)
            variableAccessor.getUsedVariables().getValues().stream()
                    .forEach(v -> arguments.add(SqlVariable.create(v.getName(), variableAccessor)));

        return SqlBuiltinCall.create(function.toLowerCase(), builtInCallExpression.isDistinct(), arguments);
    }


    @Override
    public SqlExpressionIntercode visit(ExistsExpression existsExpression)
    {
        SqlIntercode pattern = parentTranslator.visitElement(existsExpression.getPattern());
        return SqlExists.create(existsExpression.isNegated(), pattern, variableAccessor);
    }


    @Override
    public SqlExpressionIntercode visit(FunctionCallExpression functionCallExpression)
    {
        IRI iri = functionCallExpression.getFunction();
        List<SqlExpressionIntercode> arguemnts = new LinkedList<SqlExpressionIntercode>();

        for(Expression expression : functionCallExpression.getArguments())
            arguemnts.add(visitElement(expression));


        Optional<LiteralClass> resourceClass = BuiltinClasses.getLiteralClasses().stream()
                .filter(r -> r.getTypeIri().equals(iri)).findFirst();

        if(resourceClass.isPresent())
        {
            if(arguemnts.size() != 1)
            {
                messages.add(new TranslateMessage(MessageType.wrongCountOfParameters, iri.getRange(),
                        iri.toString(prologue), 1));

                return SqlNull.get();
            }

            return SqlCast.create(resourceClass.get(), arguemnts.get(0));
        }


        FunctionDefinition definition = functions.get(iri.getValue());

        if(definition == null)
        {
            messages.add(
                    new TranslateMessage(MessageType.unimplementedFunction, iri.getRange(), iri.toString(prologue)));

            return SqlNull.get();
        }

        if(arguemnts.size() != definition.getArgumentClasses().size())
        {
            messages.add(new TranslateMessage(MessageType.wrongCountOfParameters, iri.getRange(),
                    iri.toString(prologue), definition.getArgumentClasses().size()));

            return SqlNull.get();
        }

        return SqlFunctionCall.create(definition, arguemnts);
    }


    @Override
    public SqlExpressionIntercode visit(IRI iri)
    {
        return SqlIri.create(iri, request);
    }


    @Override
    public SqlExpressionIntercode visit(Literal literal)
    {
        return SqlLiteral.create(literal);
    }


    @Override
    public SqlExpressionIntercode visit(Variable variable)
    {
        return SqlVariable.create(variable.getSqlName(), variableAccessor);
    }
}
