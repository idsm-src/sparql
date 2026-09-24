package cz.iocb.sparql.engine.model.base;

import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Common interface for all types used in the AST.
 */
public interface Element
{
    /**
     * Helper method used for implementing visitors.
     *
     * <p>
     * Each implementation should look exactly the same:
     *
     * {@code return visitor.visit(this)}
     *
     * @param <T> the result type of the visitor
     * @param visitor the visitor
     * @return the result of the visit
     */
    <T> T accept(ElementVisitor<T> visitor);

    /**
     * Returns the source range for this element.
     *
     * @return the source range for this element
     */
    Range getRange();

    /**
     * Sets the source range for this element.
     *
     * <p>
     * Should be called only when creating a new element.
     *
     * @param range the source range
     */
    void setRange(Range range);
}
