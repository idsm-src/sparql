package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The full SELECT query
 */
public class SelectQuery extends Query
{
    public SelectQuery(Prologue prologue, Select select)
    {
        super(prologue, select);
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
