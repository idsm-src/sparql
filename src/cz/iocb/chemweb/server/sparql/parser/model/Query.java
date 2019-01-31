package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;



/**
 * Abstract base class for SPARQL queries.
 */
public abstract class Query extends BaseElement
{
    private Prologue prologue;

    public Query(Prologue prologue)
    {
        this.prologue = prologue;
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
}
