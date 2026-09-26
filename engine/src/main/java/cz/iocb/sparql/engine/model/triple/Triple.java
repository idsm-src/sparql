package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.pattern.BasicPattern;
import cz.iocb.sparql.engine.model.pattern.PatternElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Triple without any syntax sugar, just a single subject ({@link #getSubject}), a single predicate
 * ({@link #getPredicate}) and a single object ( {@link #getObject}).
 */
public class Triple extends PatternElement implements BasicPattern
{
    /**
     * The subject.
     */
    private final Node subject;

    /**
     * The predicate: an IRI, a variable or a property path.
     */
    private final Verb predicate;

    /**
     * The object.
     */
    private final Node object;


    /**
     * Creates the triple; variables at any position, including those nested in triple terms, become in scope.
     *
     * @param subject the subject node
     * @param predicate the predicate
     * @param object the object node
     */
    public Triple(Node subject, Verb predicate, Node object)
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;


        addVariablesInScope(subject);

        if(predicate instanceof VariableNode variable)
            variablesInScope.add(variable);

        addVariablesInScope(object);
    }


    /**
     * Adds the node to the variables in scope if it is a variable, or the variables it contains if it is a triple term.
     *
     * @param node the subject or object node
     */
    private void addVariablesInScope(Node node)
    {
        if(node instanceof VariableNode variable)
        {
            variablesInScope.add(variable);
        }
        else if(node instanceof TripleTermNode tripleTerm)
        {
            addVariablesInScope(tripleTerm.getSubject());

            if(tripleTerm.getPredicate() instanceof VariableNode variable)
                variablesInScope.add(variable);

            addVariablesInScope(tripleTerm.getObject());
        }
    }


    /**
     * The subject.
     *
     * @return the subject
     */
    public Node getSubject()
    {
        return subject;
    }


    /**
     * The predicate: an IRI, a variable or a property path.
     *
     * @return the predicate: an IRI, a variable or a property path
     */
    public Verb getPredicate()
    {
        return predicate;
    }


    /**
     * The object.
     *
     * @return the object
     */
    public Node getObject()
    {
        return object;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
