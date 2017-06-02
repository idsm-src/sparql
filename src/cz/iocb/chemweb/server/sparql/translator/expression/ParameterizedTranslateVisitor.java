package cz.iocb.chemweb.server.sparql.translator.expression;

import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
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



public abstract class ParameterizedTranslateVisitor<T, P> extends ElementVisitor<T>
{
    private P parameter;


    public abstract T visit(BinaryExpression binaryExpression, P parameter);

    public abstract T visit(InExpression inExpression, P parameter);

    public abstract T visit(UnaryExpression unaryExpression, P parameter);

    public abstract T visit(BracketedExpression bracketedExpression, P parameter);

    public abstract T visit(BuiltInCallExpression builtInCallExpression, P parameter);

    public abstract T visit(ExistsExpression existsExpression, P parameter);

    public abstract T visit(FunctionCallExpression functionCallExpression, P parameter);

    public abstract T visit(IRI iri, P parameter);

    public abstract T visit(Literal literal, P parameter);

    public abstract T visit(Variable variable, P parameter);


    public T visitElement(Element element, P parameter)
    {
        if(element == null)
            return null;

        this.parameter = parameter;
        return element.accept(this);
    }


    @Override
    public T visit(BinaryExpression binaryExpression)
    {
        return visit(binaryExpression, parameter);
    }


    @Override
    public T visit(InExpression inExpression)
    {
        return visit(inExpression, parameter);
    }


    @Override
    public T visit(UnaryExpression unaryExpression)
    {
        return visit(unaryExpression, parameter);
    }


    @Override
    public T visit(BracketedExpression bracketedExpression)
    {
        return visit(bracketedExpression, parameter);
    }


    @Override
    public T visit(BuiltInCallExpression builtInCallExpression)
    {
        return visit(builtInCallExpression, parameter);
    }


    @Override
    public T visit(ExistsExpression existsExpression)
    {
        return visit(existsExpression, parameter);
    }


    @Override
    public T visit(FunctionCallExpression functionCallExpression)
    {
        return visit(functionCallExpression, parameter);
    }


    @Override
    public T visit(IRI iri)
    {
        return visit(iri, parameter);
    }


    @Override
    public T visit(Literal literal)
    {
        return visit(literal, parameter);
    }


    @Override
    public T visit(Variable variable)
    {
        return visit(variable, parameter);
    }
}
