package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Scopes of intermediate code (see {@link Scope}): entering a pattern of an expression or the right side of a lateral
 * join gives a new scope and leaves the scope it was entered from as it was; the nodes of a pattern name their inputs
 * one level deeper; a pattern without a current solution of its own keeps the enclosing one; SQL referring to the
 * inputs of the enclosing nodes is told from SQL referring to an input of the scope itself; and the current solution of
 * the left side of a lateral join is kept by the alias of the join, apart from the current solutions of the patterns.
 */
public class ScopeTest
{
    /**
     * Bindings of a current solution binding the variable {@code x}.
     */
    private static final VariableBindings outer = bindings("x");

    /**
     * Bindings of a current solution binding the variable {@code y}.
     */
    private static final VariableBindings inner = bindings("y");


    /**
     * Bindings of the variable of the given name to an integer column of that name.
     *
     * @param name the name of the variable
     * @return bindings of the variable of the given name
     */
    private static VariableBindings bindings(String name)
    {
        VariableBindings bindings = new VariableBindings();
        bindings.add(new VariableBinding(new Variable(name), xsdInt, List.of(new TableColumn(name, INT4)), false));

        return bindings;
    }


    @Test
    void topScopeLiesInNoPattern()
    {
        assertFalse(Scope.TOP.isWithinExpression());
        assertNull(Scope.TOP.getCurrentSolution());
        assertEquals("\"outer0\"", Scope.TOP.getInputTable().toString());
        assertEquals("\"left0\"", Scope.TOP.getLeftInputTable().toString());
        assertEquals("\"right0\"", Scope.TOP.getRightInputTable().toString());
    }


    @Test
    void enteringPatternLeavesEnclosingScope()
    {
        Scope pattern = Scope.TOP.enterPattern(outer);

        assertTrue(pattern.isWithinExpression());
        assertSame(outer, pattern.getCurrentSolution());
        assertEquals("\"outer1\"", pattern.getInputTable().toString());

        assertFalse(Scope.TOP.isWithinExpression());
        assertNull(Scope.TOP.getCurrentSolution());
        assertEquals("\"outer0\"", Scope.TOP.getInputTable().toString());
    }


    @Test
    void patternWithoutSolutionKeepsEnclosingOne()
    {
        Scope pattern = Scope.TOP.enterPattern(outer);
        Scope independent = pattern.enterPattern(null);
        Scope nested = independent.enterPattern(inner);

        assertSame(outer, independent.getCurrentSolution());
        assertEquals("\"outer2\"", independent.getInputTable().toString());
        assertSame(inner, nested.getCurrentSolution());
        assertSame(outer, pattern.getCurrentSolution());
    }


    @Test
    void enclosingInputsAreFoundByDepth()
    {
        Scope nested = Scope.TOP.enterPattern(outer).enterPattern(inner);

        assertEquals(Set.of(0), nested.getEnclosingInputs("\"outer0\".\"x\" = \"outer2\".\"z\""));
        assertEquals(Set.of(0, 1), nested.getEnclosingInputs("\"left0\".\"x\" = \"right1\".\"y\""));
        assertTrue(nested.refersToEnclosingInput("\"outer1\".\"y\""));
        assertFalse(nested.refersToEnclosingInput("\"outer2\".\"z\""));
        assertFalse(nested.refersToEnclosingInput("(SELECT 1) AS \"outer1\""));
        assertFalse(Scope.TOP.refersToEnclosingInput("\"outer0\".\"x\""));
    }


    @Test
    void lateralSolutionIsKeptByAlias()
    {
        AliasTable table = new AliasTable("lateral0");
        Scope right = Scope.TOP.enterLateral(table, outer);
        Scope narrowed = right.enterLateral(table, inner);

        assertSame(outer, right.getLateralSolution(table));
        assertSame(inner, narrowed.getLateralSolution(table));
        assertNull(right.getLateralSolution(new AliasTable("lateral1")));
        assertNull(Scope.TOP.getLateralSolution(table));

        // the solution of the left side is no solution the code of the right side extends
        assertNull(right.getCurrentSolution());
        assertFalse(right.isWithinExpression());
        assertEquals("\"outer0\"", right.getInputTable().toString());
        assertSame(outer, right.enterPattern(null).getLateralSolution(table));
    }
}
