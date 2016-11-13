package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * The full SELECT query, including parts both before and after SELECT.
 */
public class SelectQuery extends Query
{
    private Select select;

    public SelectQuery()
    {
    }

    public SelectQuery(Select select)
    {
        this(null, select);
    }

    public SelectQuery(Prologue prologue, Select select)
    {
        super(prologue);

        this.select = select;
    }

    public Select getSelect()
    {
        return select;
    }

    public void setSelect(Select select)
    {
        this.select = select;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
