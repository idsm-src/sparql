package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Triple term ({@code <<( subject predicate object )>>}) written in a triple pattern, whose subject and object may
 * still contain syntax sugar (an anonymous blank node {@code []}, possibly inside a nested triple term). It is expanded
 * into a {@link TripleTermNode}.
 *
 * <p>
 * This is a {@link ComplexElement}, so it won't appear in the output of the {@link Parser}.
 *
 * <p>
 * Corresponds to the rule [119] TripleTerm in the SPARQL grammar.
 */
public class ComplexTripleTerm extends BaseComplexNode implements ComplexNode, ComplexElement
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
     * Creates the triple term.
     *
     * @param subject the subject node
     * @param predicate the predicate
     * @param object the object node
     */
    public ComplexTripleTerm(ComplexNode subject, VarOrIri predicate, ComplexNode object)
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
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
