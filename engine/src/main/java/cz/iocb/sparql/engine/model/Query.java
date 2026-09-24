package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;



/**
 * Abstract base class for SPARQL queries.
 */
public abstract class Query extends BaseElement
{
    /**
     * Declarations preceding the query.
     */
    private Prologue prologue;

    /**
     * The query body.
     */
    private Select select;


    /**
     * Creates the query from its prologue and select part.
     *
     * @param prologue the prologue of the query
     * @param select the query body
     */
    public Query(Prologue prologue, Select select)
    {
        this.prologue = prologue;
        this.select = select;
    }


    /**
     * Declarations preceding the query.
     *
     * @return declarations preceding the query
     */
    public Prologue getPrologue()
    {
        return prologue;
    }


    /**
     * Sets the prologue (required).
     *
     * @param prologue the prologue of the query
     */
    public void setPrologue(Prologue prologue)
    {
        if(prologue == null)
            throw new IllegalArgumentException();

        this.prologue = prologue;
    }


    /**
     * The query body.
     *
     * @return the query body
     */
    public Select getSelect()
    {
        return select;
    }


    /**
     * Sets the query body.
     *
     * @param select the query body
     */
    public void setSelect(Select select)
    {
        this.select = select;
    }
}
