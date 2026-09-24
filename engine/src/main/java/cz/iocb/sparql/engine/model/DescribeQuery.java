package cz.iocb.sparql.engine.model;

import java.util.LinkedList;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The full DESCRIBE query
 */
public class DescribeQuery extends Query
{
    /**
     * Resources to describe: variables or IRIs.
     */
    private final LinkedList<VarOrIri> resources;


    /**
     * Creates the query from its prologue, described resources and select part.
     *
     * @param prologue the prologue of the query
     * @param resources resources to describe
     * @param select the query body
     */
    public DescribeQuery(Prologue prologue, LinkedList<VarOrIri> resources, Select select)
    {
        super(prologue, select);
        this.resources = resources;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    /**
     * Resources to describe: variables or IRIs.
     *
     * @return resources to describe: variables or IRIs
     */
    public LinkedList<VarOrIri> getResources()
    {
        return resources;
    }
}
