package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;



/**
 * Abstract base class for SPARQL queries.
 */
public abstract class Query extends BaseElement
{
    private Prologue prologue;
    private Select select;

    public Query(Prologue prologue, Select select)
    {
        this.prologue = prologue;
        this.select = select;
    }

    public Prologue getPrologue()
    {
        return prologue;
    }

    public void setPrologue(Prologue prologue)
    {
        if(prologue == null)
            throw new IllegalArgumentException();

        this.prologue = prologue;
    }

    public Select getSelect()
    {
        return select;
    }

    public void setSelect(Select select)
    {
        this.select = select;
    }
}
