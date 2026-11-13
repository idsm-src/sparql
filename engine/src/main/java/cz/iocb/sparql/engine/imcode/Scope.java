package cz.iocb.sparql.engine.imcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.InputTable;
import cz.iocb.sparql.engine.database.LateralTable;
import cz.iocb.sparql.engine.database.OuterTable;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.imcode.expression.SqlLateralVariable;
import cz.iocb.sparql.engine.imcode.expression.SqlOuterVariable;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * The place of a piece of intermediate code among the constructs that are evaluated for a current solution: the
 * patterns of expressions (EXISTS) the code lies in, each with the current solution the pattern is evaluated for, and
 * the lateral joins in whose right sides the code lies and which let their right sides read the current solutions of
 * their left sides. The code is translated, optimised and rendered in its scope: the expressions referring to a current
 * solution take its classes and columns from the scope on every optimisation (see {@link SqlOuterVariable} and
 * {@link SqlLateralVariable}), and the nodes holding expressions name their inputs by the depth of the patterns, so
 * that a pattern can refer to them (see {@link #getInputTable}). A scope never changes; a construct that is evaluated
 * for a current solution hands a new scope down to the code it holds (see {@link #enterPattern} and
 * {@link #enterLateral}), and nothing else carries that information, so the same node can be optimised and rendered for
 * different places.
 */
public final class Scope
{
    /**
     * Name of the alias of the input of a node holding expressions, which the depth of the patterns of expressions
     * follows (see {@link #getInputTable}).
     */
    private static final String inputTableName = "outer";

    /**
     * Name of the alias of the left side of a left join (see {@link #getLeftInputTable}).
     */
    private static final String leftInputTableName = "left";

    /**
     * Name of the alias of the right side of a left join (see {@link #getRightInputTable}).
     */
    private static final String rightInputTableName = "right";

    /**
     * The scope of the code of a statement that lies in no pattern of an expression and in no right side of a lateral
     * join reading its left side.
     */
    public static final Scope TOP = new Scope(new ArrayList<>(), new HashMap<>());

    /**
     * Bindings of the current solutions of the patterns of expressions the code lies in, innermost last, null for a
     * pattern that has none of its own (see {@link #enterPattern}); their number is the depth of the patterns (see
     * {@link #getInputTable}).
     */
    private final List<VariableBindings> currentSolutions;

    /**
     * Bindings of the current solutions of the left sides of the lateral joins in whose right sides the code lies, by
     * the aliases of the joins (see {@link #enterLateral}).
     */
    private final Map<LateralTable, VariableBindings> lateralSolutions;


    /**
     * Creates the scope.
     *
     * @param currentSolutions bindings of the current solutions of the patterns of expressions, innermost last
     * @param lateralSolutions bindings of the current solutions of the left sides of the lateral joins, by their
     *            aliases
     */
    private Scope(List<VariableBindings> currentSolutions, Map<LateralTable, VariableBindings> lateralSolutions)
    {
        this.currentSolutions = currentSolutions;
        this.lateralSolutions = lateralSolutions;
    }


    /**
     * The scope of the nodes of a pattern of an expression (EXISTS) held by a node of this scope: they lie one level
     * deeper than that node (see {@link #getInputTable}). A pattern evaluated for the current solution of the solutions
     * its expression is evaluated for gets the bindings of that solution, from which the expressions of the pattern
     * read its values (see {@link SqlOuterVariable}): the bindings the node holding the expression provides at the
     * moment, joined with the current solution of the pattern the node itself lies in (see
     * {@link SqlExists#getCurrentSolution}). A pattern independent of those solutions has no current solution of its
     * own (null); the current solution of the pattern its expression lies in stays in effect for its nodes, because the
     * optimisation builds such a pattern from nodes of that pattern (the existence test of a leading OPTIONAL).
     *
     * @param solution bindings of the current solution of the pattern, its columns addressed through the aliases of the
     *            inputs of the nodes providing them, or null for a pattern without a current solution of its own
     * @return the scope of the nodes of the pattern
     */
    public Scope enterPattern(VariableBindings solution)
    {
        List<VariableBindings> solutions = new ArrayList<>(currentSolutions);
        solutions.add(solution);

        return new Scope(solutions, lateralSolutions);
    }


    /**
     * The scope of the right side of a lateral join of this scope that reads the current solution of the left side as
     * the left side provides it at the moment (see {@link SqlLateralJoin}): the expressions of the right side referring
     * to that solution take its classes and columns from the given bindings (see {@link SqlLateralVariable}). Unlike
     * the current solution of a pattern of an expression (see {@link #enterPattern}), the solution is no solution the
     * solutions of the right side extend, so it is kept apart, by the alias of the join, which is unique within the
     * request (see {@link cz.iocb.sparql.engine.request.Request#createLateralTable}).
     *
     * @param table alias through which the right side of the join refers to its left side
     * @param solution bindings of the current solution of the left side, its columns addressed through the alias
     * @return the scope of the right side of the join
     */
    public Scope enterLateral(LateralTable table, VariableBindings solution)
    {
        Map<LateralTable, VariableBindings> solutions = new HashMap<>(lateralSolutions);
        solutions.put(table, solution);

        return new Scope(currentSolutions, solutions);
    }


    /**
     * Bindings of the current solution in effect: that of the innermost pattern of an expression the code lies in which
     * has one of its own (see {@link #enterPattern}), or null outside any such pattern.
     *
     * @return bindings of the current solution in effect, or null outside any pattern evaluated for a current solution
     */
    public VariableBindings getCurrentSolution()
    {
        for(int i = currentSolutions.size() - 1; i >= 0; i--)
            if(currentSolutions.get(i) != null)
                return currentSolutions.get(i);

        return null;
    }


    /**
     * Bindings of the current solution of the left side of the lateral join with the given alias, as the left side
     * provides it at the moment, for the code of the right side of the join (see {@link #enterLateral}); null
     * otherwise, and for a join whose right side keeps what the join exposed to it when it was created.
     *
     * @param table alias through which the right side of the join refers to its left side
     * @return bindings of the current solution of the left side of the join, or null
     */
    public VariableBindings getLateralSolution(LateralTable table)
    {
        return lateralSolutions.get(table);
    }


    /**
     * True if the code lies in a pattern of an expression (EXISTS). Only the existence of a solution of such a pattern
     * matters, and the database stops evaluating it at its first solution, so a walk in it is not seeded from shared
     * solutions (see {@link SqlShared#canShare}): the seeds would be computed from the whole shared side before the
     * walk yields anything.
     *
     * @return true if the code lies in a pattern of an expression, false otherwise
     */
    public boolean isWithinExpression()
    {
        return !currentSolutions.isEmpty();
    }


    /**
     * True if the SQL code refers to the input of a node holding an expression in whose pattern the code lies (see
     * {@link #getInputTable}), so to a value of a current solution: such code has a meaning only at its place inside
     * the pattern. The code keeps the aliases it refers to (see {@link SqlCode#getOuterTables}); those of the nodes the
     * code lies in are the ones given at a lesser depth than the depth of this scope, whereas a part of the code of a
     * node of this scope, like the conditions of a filter, refers also to the input of that node, given at the depth of
     * this scope.
     *
     * @param code the SQL code
     * @return true if the SQL code refers to the input of a node holding an expression in whose pattern the code lies,
     *         false otherwise
     */
    public boolean refersToEnclosingInput(SqlCode code)
    {
        return !getEnclosingInputs(code).isEmpty();
    }


    /**
     * The inputs the SQL code refers to among the inputs of the nodes holding the expressions in whose patterns the
     * code lies (see {@link #refersToEnclosingInput}), each given by the number of the patterns its node lies in. Code
     * referring to such an input is evaluated for every row of the input anew, wherever in the patterns below the node
     * it stands.
     *
     * @param code the SQL code
     * @return depths of the inputs the SQL code refers to
     */
    public Set<Integer> getEnclosingInputs(SqlCode code)
    {
        Set<Integer> depths = new HashSet<>();

        for(OuterTable table : code.getOuterTables())
            if(table instanceof InputTable input && input.getDepth() < currentSolutions.size())
                depths.add(input.getDepth());

        return depths;
    }


    /**
     * True if the SQL code refers to a current solution: to the input of a node holding an expression in whose pattern
     * the code lies (see {@link #refersToEnclosingInput}), or to the left side of a lateral join in whose right side
     * the code lies (see {@link SqlLateralJoin}). Such code can neither be moved into a common table expression nor
     * explained on its own. Code holding a whole lateral join does not refer to the left side of that join, because the
     * join gives the alias itself.
     *
     * @param code the SQL code
     * @return true if the SQL code refers to a current solution, false otherwise
     */
    public boolean refersToCurrentSolution(SqlCode code)
    {
        return refersToEnclosingInput(code) || code.getOuterTables().stream().anyMatch(t -> t instanceof LateralTable);
    }


    /**
     * Alias of the input of a node of this scope holding expressions (a filter, a bind, an aggregation or a procedure
     * call): a name followed by the depth of the patterns of expressions the node lies in. A pattern of an expression
     * held by the node can refer to the columns of the input through the alias from any depth, because no subquery
     * between such a reference and the node declares the alias again: the subqueries belong to the pattern, whose nodes
     * lie deeper.
     *
     * @return alias of the input of a node of this scope holding expressions
     */
    public InputTable getInputTable()
    {
        return new InputTable(inputTableName, currentSolutions.size());
    }


    /**
     * Alias of the left side of a left join of this scope, whose conditions are expressions over the solutions of both
     * sides (see {@link #getInputTable}).
     *
     * @return alias of the left side of a left join of this scope
     */
    public InputTable getLeftInputTable()
    {
        return new InputTable(leftInputTableName, currentSolutions.size());
    }


    /**
     * Alias of the right side of a left join of this scope (see {@link #getLeftInputTable}).
     *
     * @return alias of the right side of a left join of this scope
     */
    public InputTable getRightInputTable()
    {
        return new InputTable(rightInputTableName, currentSolutions.size());
    }
}
