package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * The FROM clause of a query.
 */
public class DataSet extends BaseElement
{
    private IriNode sourceSelector;
    private boolean isDefault;


    public DataSet(IriNode sourceSelector, boolean isDefault)
    {
        setSourceSelector(sourceSelector);
        this.isDefault = isDefault;
    }


    public IriNode getSourceSelector()
    {
        return sourceSelector;
    }


    public void setSourceSelector(IriNode sourceSelector)
    {
        if(sourceSelector == null)
            throw new IllegalArgumentException();

        this.sourceSelector = sourceSelector;
    }


    public boolean isDefault()
    {
        return isDefault;
    }


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
