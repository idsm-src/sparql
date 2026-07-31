package cz.iocb.sparql.engine.model.base;

import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;



/**
 * Interface used to represent "complex" elements in the AST, that is those that are used internally in the initial
 * conversion from parse tree, but will never occur in the final AST.
 */
public interface ComplexElement extends Element
{
    /**
     * Helper method used for implementing visitors.
     */
    <T> T accept(ComplexElementVisitor<T> visitor);
}
