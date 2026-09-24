package cz.iocb.sparql.engine.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern that contains a list of other patterns ({@link #getPatterns}).
 *
 * <p>
 * Corresponds to the rule [54] GroupGraphPatternSub in the SPARQL grammar.
 */
public class GroupGraph extends PatternElement implements GraphPattern
{
    /**
     * Patterns of the group, in order.
     */
    private final List<Pattern> patterns;


    /**
     * Creates the group; the variables of all patterns become in scope.
     *
     * @param patterns patterns of the group
     */
    public GroupGraph(Collection<Pattern> patterns)
    {
        this.patterns = Collections.unmodifiableList(new ArrayList<>(patterns));

        for(Pattern pattern : patterns)
            variablesInScope.addAll(pattern.getVariablesInScope());
    }


    /**
     * Patterns of the group, in order.
     *
     * @return patterns of the group, in order
     */
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
