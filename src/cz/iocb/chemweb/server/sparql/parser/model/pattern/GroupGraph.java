package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Pattern that contains a list of other patterns ({@link #getPatterns}).
 *
 * <p>
 * Corresponds to the rule [54] GroupGraphPatternSub in the SPARQL grammar.
 */
public class GroupGraph extends BaseElement implements GraphPattern
{
    private List<Pattern> patterns;

    public GroupGraph()
    {
        patterns = new ArrayList<>();
    }

    public GroupGraph(Collection<Pattern> patterns)
    {
        this.patterns = new ArrayList<>(patterns);
    }

    public List<Pattern> getPatterns()
    {
        return patterns;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
