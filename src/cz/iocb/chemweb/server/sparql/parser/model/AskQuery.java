package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * The full ASK query
 */
public class AskQuery extends Query
{
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
