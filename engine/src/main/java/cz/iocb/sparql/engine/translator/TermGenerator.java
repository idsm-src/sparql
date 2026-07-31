package cz.iocb.sparql.engine.translator;

import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.model.expression.LiteralNode;
import cz.iocb.sparql.engine.model.triple.BlankNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;



public class TermGenerator
{
    public static RdfTerm getTerm(Node node)
    {
        if(node == null)
            return null;

        return switch(node)
        {
            case IriNode iri -> getIri(iri);
            case LiteralNode literal -> getLiteral(literal);
            case VariableNode var -> getVariable(var);
            case BlankNode bnode -> getVariable(bnode);
            default -> throw new IllegalArgumentException();
        };
    }


    public static Iri getIri(IriNode iri)
    {
        if(iri == null)
            return null;

        return new Iri(iri.getValue());
    }


    public static Literal getLiteral(LiteralNode literal)
    {
        if(literal == null)
            return null;

        if(literal.getTag() != null)
            return new LangStringLiteral(literal.getValue(), literal.getTag());

        return new TypedLiteral(literal.getValue(), getIri(literal.getType()));
    }


    public static Variable getVariable(VariableNode variable)
    {
        if(variable == null)
            return null;

        if(variable.getScope() == null || variable.getScope().isEmpty())
            return new Variable(variable.getName());

        return new Variable(variable.getName() + "@" + variable.getScope());
    }


    public static Variable getVariable(BlankNode bnode)
    {
        if(bnode == null)
            return null;

        return new Variable("@bn" + bnode.getName());
    }


    public static Variable getVariable(VariableOrBlankNode variable)
    {
        if(variable == null)
            return null;

        return switch(variable)
        {
            case VariableNode var -> getVariable(var);
            case BlankNode bnode -> getVariable(bnode);
            default -> throw new IllegalArgumentException();
        };
    }
}
