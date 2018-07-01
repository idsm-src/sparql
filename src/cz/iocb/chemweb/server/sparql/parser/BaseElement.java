package cz.iocb.chemweb.server.sparql.parser;



/**
 * Common base class for all {@link Element}s.
 *
 * <p>
 * Provides implementation for the interface methods.
 */
public abstract class BaseElement implements Element
{
    private Range range;

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
