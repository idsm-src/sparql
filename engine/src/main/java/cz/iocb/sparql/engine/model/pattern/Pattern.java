package cz.iocb.sparql.engine.model.pattern;

import java.util.LinkedHashSet;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.Element;



/**
 * Marker interface used to represent any single pattern, like a triple, UNION or a group of patterns in braces.
 *
 * <p>
 * Corresponds to the rules [56] GraphPatternNotTriples and [81] TriplesSameSubjectPath in the SPARQL grammar.
 */
public interface Pattern extends Element
{
    LinkedHashSet<VariableNode> getVariablesInScope();
}
