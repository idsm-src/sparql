package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The full ASK query
 */
public class AskQuery extends Query
{
    /**
     * Creates the query from its prologue and select part.
     *
     * @param prologue the prologue of the query
     * @param select the query body
     */
    public AskQuery(Prologue prologue, Select select)
    {
        super(prologue, select);
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
