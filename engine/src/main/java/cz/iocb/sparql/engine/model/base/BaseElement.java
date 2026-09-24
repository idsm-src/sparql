package cz.iocb.sparql.engine.model.base;



/**
 * Common base class for all {@link Element}s.
 *
 * <p>
 * Provides implementation for the interface methods.
 */
public abstract class BaseElement implements Element
{
    /**
     * Source range of the element, or null.
     */
    private Range range;


    /**
     * Creates the element without a source range.
     */
    protected BaseElement()
    {
    }


    @Override
    public Range getRange()
    {
        return range;
    }


    @Override
    public void setRange(Range range)
    {
        this.range = range;
    }
}
