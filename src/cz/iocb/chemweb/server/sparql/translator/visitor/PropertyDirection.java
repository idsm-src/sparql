package cz.iocb.chemweb.server.sparql.translator.visitor;

/**
 * Two-way direction flag for a general property (e.g. while joining two subqueries).
 */
public enum PropertyDirection
{

    /**
     * None of the child entities has the defined property.
     */
    NONE,

    /**
     * Only left child has the defined property.
     */
    LEFT,

    /**
     * Only right child has the defined property.
     */
    RIGHT,

    /**
     * Both child entities have the defined property.
     */
    BOTH
}
