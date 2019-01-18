package cz.iocb.chemweb.server.sparql.translator.expression;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
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
import cz.iocb.chemweb.server.sparql.translator.TranslatedSegment;
import cz.iocb.chemweb.server.sparql.translator.error.ErrorType;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryLogical;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlCast;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExists;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
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
    private final Prologue prologue;
    private final LinkedHashMap<String, UserIriClass> iriClasses;
    private final List<TranslateException> exceptions;
    //private final List<TranslateException> warnings;


    public ExpressionTranslateVisitor(VariableAccessor variableAccessor, TranslateVisitor parentTranslator)
    {
        this.variableAccessor = variableAccessor;
        this.parentTranslator = parentTranslator;
        this.prologue = parentTranslator.getPrologue();
        this.iriClasses = parentTranslator.getConfiguration().getIriClasses();
        this.exceptions = parentTranslator.getExceptions();
        //this.warnings = parentTranslator.getWarnings();
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

        if(function.equalsIgnoreCase("iri"))
            arguments.add(SqlIri.create(prologue.getBase(), iriClasses));

        return SqlBuiltinCall.create(function.toLowerCase(), arguments);
    }


    @Override
    public SqlExpressionIntercode visit(ExistsExpression existsExpression)
    {
        TranslatedSegment pattern = parentTranslator.visitElement(existsExpression.getPattern());
        return SqlExists.create(existsExpression.isNegated(), pattern, variableAccessor);
    }


    @Override
    public SqlExpressionIntercode visit(FunctionCallExpression functionCallExpression)
    {
        IRI function = functionCallExpression.getFunction();
        List<SqlExpressionIntercode> arguemnts = new LinkedList<SqlExpressionIntercode>();

        for(Expression expression : functionCallExpression.getArguments())
            arguemnts.add(visitElement(expression));

        Optional<LiteralClass> resourceClass = BuiltinClasses.getLiteralClasses().stream()
                .filter(r -> r.getTypeIri().equals(function)).findFirst();

        if(!resourceClass.isPresent())
        {
            exceptions.add(new TranslateException(ErrorType.unimplementedFunction, function.getRange(),
                    function.toString(prologue)));

            return SqlNull.get();
        }

        if(arguemnts.size() != 1)
        {
            exceptions.add(new TranslateException(ErrorType.wrongCountOfParameters, function.getRange(),
                    function.toString(prologue), 1));

            return SqlNull.get();
        }

        return SqlCast.create(resourceClass.get(), arguemnts.get(0));
    }


    @Override
    public SqlExpressionIntercode visit(IRI iri)
    {
        return SqlIri.create(iri, iriClasses);
    }


    @Override
    public SqlExpressionIntercode visit(Literal literal)
    {
        return SqlLiteral.create(literal);
    }


    @Override
    public SqlExpressionIntercode visit(Variable variable)
    {
        return SqlVariable.create(variable.getName(), variableAccessor);
    }
}
