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



/**
 * Converts AST nodes to the {@link RdfTerm}s the translator works with. Variables are qualified by their parser scope
 * ({@code name@scope}); blank nodes become variables named {@code @bn<label>}, which cannot clash with query variables.
 */
public class TermGenerator
{
    /**
     * Not instantiable.
     */
    private TermGenerator()
    {
    }


    /**
     * Term of a subject, predicate or object node; null for a null node.
     *
     * @param node the subject node
     * @return term of a subject, predicate or object node; null for a null node
     */
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


    /**
     * IRI term of the node; null for a null node.
     *
     * @param iri the IRI node
     * @return IRI term of the node; null for a null node
     */
    public static Iri getIri(IriNode iri)
    {
        if(iri == null)
            return null;

        return new Iri(iri.getValue());
    }


    /**
     * Literal term of the node; null for a null node.
     *
     * @param literal the literal node
     * @return literal term of the node; null for a null node
     */
    public static Literal getLiteral(LiteralNode literal)
    {
        if(literal == null)
            return null;

        if(literal.getTag() != null)
            return new LangStringLiteral(literal.getValue(), literal.getTag());

        return new TypedLiteral(literal.getValue(), getIri(literal.getType()));
    }


    /**
     * Variable named {@code name} or {@code name@scope} when the node is bound in a named scope.
     *
     * @param variable the variable node
     * @return variable named {@code name} or {@code name@scope} when the node is bound in a named scope
     */
    public static Variable getVariable(VariableNode variable)
    {
        if(variable == null)
            return null;

        if(variable.getScope() == null || variable.getScope().isEmpty())
            return new Variable(variable.getName());

        return new Variable(variable.getName() + "@" + variable.getScope());
    }


    /**
     * Hidden variable standing for the blank node.
     *
     * @param bnode the blank node
     * @return hidden variable standing for the blank node
     */
    public static Variable getVariable(BlankNode bnode)
    {
        if(bnode == null)
            return null;

        return new Variable("@bn" + bnode.getName());
    }


    /**
     * Variable of a variable or blank node.
     *
     * @param variable the variable node
     * @return variable of a variable or blank node
     */
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
