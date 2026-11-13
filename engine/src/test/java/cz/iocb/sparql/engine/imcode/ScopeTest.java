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
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.LateralTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Scopes of intermediate code (see {@link Scope}): entering a pattern of an expression or the right side of a lateral
 * join gives a new scope and leaves the scope it was entered from as it was; the nodes of a pattern name their inputs
 * one level deeper; a pattern without a current solution of its own keeps the enclosing one; SQL code whose columns are
 * addressed through the inputs of the enclosing nodes is told from code addressing an input of the scope itself, and
 * from code that only gives or spells such an alias; code addressing the left side of a lateral join refers to a
 * current solution too, unless it holds the join whole; and the current solution of the left side of a lateral join is
 * kept by the alias of the join, apart from the current solutions of the patterns.
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


    /**
     * An integer column of the given name addressed through the table.
     *
     * @param name the name of the column
     * @param table the table
     * @return the column addressed through the table
     */
    private static Column column(String name, Table table)
    {
        return new TableColumn(name, INT4).fromTable(table);
    }


    @Test
    void enclosingInputsAreFoundByDepth()
    {
        Scope pattern = Scope.TOP.enterPattern(outer);
        Scope nested = pattern.enterPattern(inner);

        Column top = column("x", Scope.TOP.getInputTable());
        Column left = column("x", Scope.TOP.getLeftInputTable());
        Column right = column("y", pattern.getRightInputTable());
        Column middle = column("y", pattern.getInputTable());
        Column own = column("z", nested.getInputTable());

        assertEquals("\"outer0\".\"x\" = \"outer2\".\"z\"", SqlCode.of(top, " = ", own).getText());
        assertEquals(Set.of(0), nested.getEnclosingInputs(SqlCode.of(top, " = ", own)));
        assertEquals("\"left0\".\"x\" = \"right1\".\"y\"", SqlCode.of(left, " = ", right).getText());
        assertEquals(Set.of(0, 1), nested.getEnclosingInputs(SqlCode.of(left, " = ", right)));
        assertTrue(nested.refersToEnclosingInput(middle.getCode()));
        assertFalse(nested.refersToEnclosingInput(own.getCode()));
        assertFalse(Scope.TOP.refersToEnclosingInput(top.getCode()));

        // code that gives the alias, or spells it in a constant, addresses no column through it
        assertFalse(nested.refersToEnclosingInput(SqlCode.of("(SELECT 1) AS ", pattern.getInputTable())));
        assertFalse(nested.refersToEnclosingInput(new SqlCode("'\"outer1\".\"y\"'::varchar")));
    }


    @Test
    void codeAddressingLateralSideRefersToCurrentSolution()
    {
        Scope pattern = Scope.TOP.enterPattern(outer);
        LateralTable table = new LateralTable(0);

        Column side = column("x", table);
        Column enclosing = column("x", Scope.TOP.getInputTable());
        Column own = column("y", pattern.getInputTable());

        assertEquals("\"lateral0\".\"x\"", side.toString());
        assertTrue(Scope.TOP.refersToCurrentSolution(side.getCode()));
        assertTrue(pattern.refersToCurrentSolution(SqlCode.of(own, " = ", side)));
        assertFalse(pattern.refersToEnclosingInput(SqlCode.of(own, " = ", side)));
        assertTrue(pattern.refersToCurrentSolution(enclosing.getCode()));
        assertFalse(pattern.refersToCurrentSolution(own.getCode()));
        assertFalse(Scope.TOP.refersToCurrentSolution(enclosing.getCode()));

        // neither code that gives the alias nor a column whose name spells it addresses the left side
        assertFalse(Scope.TOP.refersToCurrentSolution(SqlCode.of("(SELECT 1) AS ", table)));
        assertFalse(Scope.TOP.refersToCurrentSolution(new TableColumn("lateral0", INT4).getCode()));

        // code holding the whole join gives the alias, so it refers only to what the join refers to from outside
        SqlBuilder join = new SqlBuilder().append("SELECT ").append(side).append(" FROM (SELECT 1 AS \"x\") AS ");
        join.appendAlias(table).append(" CROSS JOIN LATERAL (SELECT 1 WHERE ").append(side).append(" > 0) AS \"tab1\"");

        assertFalse(Scope.TOP.refersToCurrentSolution(join.toCode()));
        assertFalse(pattern.refersToCurrentSolution(join.toCode()));

        join.append(" WHERE ").append(enclosing).append(" > 0");

        assertTrue(pattern.refersToCurrentSolution(join.toCode()));
        assertEquals(Set.of(0), pattern.getEnclosingInputs(join.toCode()));
    }


    @Test
    void lateralSolutionIsKeptByAlias()
    {
        LateralTable table = new LateralTable(0);
        Scope right = Scope.TOP.enterLateral(table, outer);
        Scope narrowed = right.enterLateral(table, inner);

        assertSame(outer, right.getLateralSolution(table));
        assertSame(inner, narrowed.getLateralSolution(table));
        assertNull(right.getLateralSolution(new LateralTable(1)));
        assertNull(Scope.TOP.getLateralSolution(table));

        // the solution of the left side is no solution the code of the right side extends
        assertNull(right.getCurrentSolution());
        assertFalse(right.isWithinExpression());
        assertEquals("\"outer0\"", right.getInputTable().toString());
        assertSame(outer, right.enterPattern(null).getLateralSolution(table));
    }
}
