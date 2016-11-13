package cz.iocb.chemweb.server.sparql.parser;

import cz.iocb.chemweb.server.sparql.parser.model.Prologue;



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
    public String toString()
    {
        ToStringVisitor visitor = new ToStringVisitor();
        visitor.visitElement(this);
        return visitor.getString();
    }

    public String toString(Prologue prologue)
    {
        ToStringVisitor visitor = new ToStringVisitor(prologue);
        visitor.visitElement(this);
        return visitor.getString();
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
