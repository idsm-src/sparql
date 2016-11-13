package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;

import cz.iocb.chemweb.server.sparql.parser.model.Prologue;



/**
 * Common abstract ancestor of type tree elements.
 *
 */
public abstract class TypeElement implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /** Class identifier value */
    public String value;

    /**
     * Accept method for visitor design pattern.
     *
     * @param <T> Return type
     * @param visitor Visitor implementation
     * @return Object of return type
     */
    public abstract <T> T accept(Visitor<T> visitor);

    @Override
    public String toString()
    {
        return new PrintInfix(null).visitTree(this);
    }

    public String toString(Prologue prologue)
    {
        return new PrintInfix(prologue).visitTree(this);
    }
}
