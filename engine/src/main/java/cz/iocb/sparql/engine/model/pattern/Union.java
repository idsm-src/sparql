package cz.iocb.sparql.engine.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern used to compute set union of a list of patterns ({@link #getPatterns} ).
 *
 * <p>
 * Corresponds to the rule [67] GroupOrUnionGraphPattern in the SPARQL grammar, with at least one {@code UNION}.
 */
public class Union extends PatternElement implements Pattern
{
    /**
     * Branches of the union.
     */
    private final List<GraphPattern> patterns;


    /**
     * Creates the union; the variables of all branches become in scope.
     *
     * @param patterns the branches
     */
    public Union(Collection<GraphPattern> patterns)
    {
        this.patterns = Collections.unmodifiableList(new ArrayList<>(patterns));

        for(Pattern pattern : patterns)
            variablesInScope.addAll(pattern.getVariablesInScope());
    }


    /**
     * Branches of the union.
     *
     * @return branches of the union
     */
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
