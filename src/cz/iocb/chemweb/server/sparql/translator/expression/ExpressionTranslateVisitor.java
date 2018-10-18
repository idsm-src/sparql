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
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.TranslatedSegment;
import cz.iocb.chemweb.server.sparql.translator.error.ErrorType;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryLogical;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlCast;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlIri;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlLiteral;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlUnaryArithmetic;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlUnaryLogical;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class ExpressionTranslateVisitor extends ElementVisitor<SqlExpressionIntercode>
{
    private final VariableAccessor variableAccessor;

    private final SparqlDatabaseConfiguration configuration;
    private final LinkedHashMap<String, UserIriClass> iriClasses;

    private final Prologue prologue;
    private final List<TranslateException> exceptions;
    private final List<TranslateException> warnings;


    public ExpressionTranslateVisitor(VariableAccessor variableAccessor, SparqlDatabaseConfiguration configuration,
            Prologue prologue, List<TranslateException> exceptions, List<TranslateException> warnings)
    {
        this.variableAccessor = variableAccessor;
        this.configuration = configuration;
        this.iriClasses = configuration.getIriClasses();
        this.prologue = prologue;
        this.exceptions = exceptions;
        this.warnings = warnings;
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
        SqlExpressionIntercode result = null;

        Operator compareOperator = inExpression.isNegated() ? Operator.NotEquals : Operator.Equals;
        Operator logicalOperator = inExpression.isNegated() ? Operator.And : Operator.Or;

        for(Expression expression : inExpression.getRight())
        {
            SqlExpressionIntercode right = visitElement(expression);
            SqlExpressionIntercode compare = SqlBinaryComparison.create(compareOperator, left, right);

            if(result == null)
                result = compare;
            else
                result = SqlBinaryLogical.create(logicalOperator, result, compare);
        }

        return result;
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

        return SqlBuiltinCall.create(function.toLowerCase(), arguments);
    }


    @Override
    public SqlExpressionIntercode visit(ExistsExpression existsExpression)
    {
        TranslateVisitor translator = new TranslateVisitor(configuration);
        TranslatedSegment pattern = translator.visitElement(existsExpression.getPattern());

        return null; //SqlExists.create(pattern);
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
