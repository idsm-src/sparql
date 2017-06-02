package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BracketedExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.ExistsExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.FunctionCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.InExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.expression.UnaryExpression;



public class ExpressionTranslateVisitor extends ParameterizedTranslateVisitor<TranslatedExpression, TranslateRequest>
{
    @Override
    public TranslatedExpression visit(BinaryExpression binaryExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(InExpression inExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(UnaryExpression unaryExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(BracketedExpression bracketedExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(BuiltInCallExpression builtInCallExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(ExistsExpression existsExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(FunctionCallExpression functionCallExpression, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(IRI iri, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(Literal literal, TranslateRequest parameter)
    {
        return null;
    }


    @Override
    public TranslatedExpression visit(Variable variable, TranslateRequest parameter)
    {
        return null;
    }
}
