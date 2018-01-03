package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Pattern used to compute set union of a list of patterns ({@link #getPatterns} ).
 *
 * <p>
 * Corresponds to the rule [67] GroupOrUnionGraphPattern in the SPARQL grammar, with at least one {@code UNION}.
 */
public class Union extends BaseElement implements Pattern
{
    private List<GraphPattern> patterns;

    public Union()
    {
        patterns = new ArrayList<>();
    }

    public Union(Collection<GraphPattern> patterns)
    {
        this.patterns = new ArrayList<>(patterns);
    }

    public List<GraphPattern> getPatterns()
    {
        return patterns;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
