package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The FROM clause of a query.
 */
public class DataSet extends BaseElement
{
    /**
     * IRI of the graph.
     */
    private IriNode sourceSelector;

    /**
     * True for FROM, false for FROM NAMED.
     */
    private boolean isDefault;


    /**
     * Creates the dataset clause for the graph.
     *
     * @param sourceSelector IRI of the graph
     * @param isDefault true for FROM, false for FROM NAMED
     */
    public DataSet(IriNode sourceSelector, boolean isDefault)
    {
        setSourceSelector(sourceSelector);
        this.isDefault = isDefault;
    }


    /**
     * IRI of the graph.
     *
     * @return IRI of the graph
     */
    public IriNode getSourceSelector()
    {
        return sourceSelector;
    }


    /**
     * Sets the IRI of the graph (required).
     *
     * @param sourceSelector IRI of the graph
     */
    public void setSourceSelector(IriNode sourceSelector)
    {
        if(sourceSelector == null)
            throw new IllegalArgumentException();

        this.sourceSelector = sourceSelector;
    }


    /**
     * True for {@code FROM} (a default graph source), false for {@code FROM NAMED}.
     *
     * @return true for {@code FROM} (a default graph source), false for {@code FROM NAMED}
     */
    public boolean isDefault()
    {
        return isDefault;
    }


    /**
     * Sets whether the graph is a default graph source (FROM) or a named one (FROM NAMED).
     *
     * @param isDefault true for FROM, false for FROM NAMED
     */
    public void setDefault(boolean isDefault)
    {
        this.isDefault = isDefault;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
