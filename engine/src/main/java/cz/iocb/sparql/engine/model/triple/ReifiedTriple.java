package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Reified triple ({@code << subject predicate object ~ reifier >>}), the shorthand for the node {@link #getReifier
 * reifier} that reifies the enclosed triple. It is expanded into the reifier node standing in its place and the triple
 * {@code reifier rdf:reifies <<( subject predicate object )>>}; a fresh blank node is used when no reifier is written.
 *
 * <p>
 * This is a {@link ComplexElement}, so it won't appear in the output of the {@link Parser}.
 *
 * <p>
 * Corresponds to the rule [116] ReifiedTriple in the SPARQL grammar.
 */
public class ReifiedTriple extends BaseComplexNode implements ComplexNode, ComplexElement
{
    /**
     * The subject, possibly with syntax sugar.
     */
    private final ComplexNode subject;

    /**
     * The predicate: an IRI or a variable.
     */
    private final VarOrIri predicate;

    /**
     * The object, possibly with syntax sugar.
     */
    private final ComplexNode object;

    /**
     * The reifier: a variable, an IRI or a blank node; null when omitted.
     */
    private final Node reifier;


    /**
     * Creates the reified triple.
     *
     * @param subject the subject node
     * @param predicate the predicate
     * @param object the object node
     * @param reifier the reifier node, or null when omitted
     */
    public ReifiedTriple(ComplexNode subject, VarOrIri predicate, ComplexNode object, Node reifier)
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.reifier = reifier;
    }


    /**
     * The subject, possibly with syntax sugar.
     *
     * @return the subject, possibly with syntax sugar
     */
    public ComplexNode getSubject()
    {
        return subject;
    }


    /**
     * The predicate: an IRI or a variable.
     *
     * @return the predicate: an IRI or a variable
     */
    public VarOrIri getPredicate()
    {
        return predicate;
    }


    /**
     * The object, possibly with syntax sugar.
     *
     * @return the object, possibly with syntax sugar
     */
    public ComplexNode getObject()
    {
        return object;
    }


    /**
     * The reifier: a variable, an IRI or a blank node; null when omitted, which stands for a fresh blank node.
     *
     * @return the reifier: a variable, an IRI or a blank node; null when omitted, which stands for a fresh blank node
     */
    public Node getReifier()
    {
        return reifier;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public <T> T accept(ComplexElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
