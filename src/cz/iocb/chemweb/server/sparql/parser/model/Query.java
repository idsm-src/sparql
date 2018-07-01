package cz.iocb.chemweb.server.sparql.parser.model;

import java.util.HashSet;
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


    // NOTE: added by galgonek
    HashSet<String> queryVariables;

    public HashSet<String> getQueryVariables()
    {
        return queryVariables;
    }

    public void setQueryVariables(HashSet<String> usedVariables)
    {
        if(usedVariables == null)
            throw new IllegalArgumentException();

        this.queryVariables = usedVariables;
    }
}
