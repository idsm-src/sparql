package cz.iocb.sparql.engine.imcode;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.CommonTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Solutions evaluated once by the statement and read from a common table expression wherever they are needed (see
 * {@link Request#getCommonTableExpression}). A node that is to seed a walk but cannot be taken into it, because it is
 * not distinct over the start of the walk or stands outside the join (the left side of an OPTIONAL or MINUS, the other
 * walk of a chain), is shared this way: the distinct values of the start, read from the shared solutions and shared
 * themselves (see {@link #seed}), seed the walk, while the node keeps its place. The child is kept as optimised when
 * shared, so that every reference renders the same SQL, and a reference exposes what its parent needs of it. The SQL of
 * an expression, rendered at once during the optimisation, refers to the common table expression as well; the statement
 * whose SQL holds the expression declares it. A common table expression referred to more than once is materialised, so
 * a restriction of a reference (a constant graph, the graph of the current solution of an EXISTS) does not enter it:
 * the shared child is evaluated in full once per statement.
 */
public final class SqlShared extends SqlIntercode
{
    /**
     * Shared solutions.
     */
    private final SqlIntercode child;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param child the shared solutions
     */
    protected SqlShared(VariableBindings bindings, SqlIntercode child)
    {
        super(bindings, child.isDeterministic());

        this.child = child;
    }


    /**
     * The node shared; the node itself when it is shared already.
     *
     * @param node the node
     * @return the node shared
     */
    protected static SqlIntercode share(SqlIntercode node)
    {
        if(node instanceof SqlShared)
            return node;

        return new SqlShared(node.getVariableBindings(), node);
    }


    /**
     * True if the node can be shared: it is deterministic, holds no SERVICE call not evaluated yet, refers to no
     * current solution (see {@link Scope#refersToCurrentSolution}), neither to that of an EXISTS nor to that of the
     * left side of a lateral join (its SQL would read an alias a common table expression cannot see), and it does not
     * lie in a pattern of an expression (see {@link Scope#isWithinExpression}): seeding a walk inside an EXISTS from
     * shared solutions would evaluate them in full before the first solution of the pattern, which the database would
     * otherwise stop at. The named graphs the translation pairs zero-length paths from constants with are shared as
     * code of the statement (see {@link #shareOptimized}), so EXISTS patterns read them from the common table
     * expression too.
     *
     * @param request the current request
     * @param scope the scope of the node
     * @param node the node
     * @return true if the node can be shared, false otherwise
     */
    protected static boolean canShare(Request request, Scope scope, SqlIntercode node)
    {
        if(scope.isWithinExpression() || !node.isDeterministic() || node.hasServiceSubpattern())
            return false;

        if(node instanceof SqlShared || node instanceof SqlValues || node.equals(SqlEmptySolution.get())
                || node.equals(SqlNoSolution.get()))
            return false;

        return !scope.refersToCurrentSolution(node.translate(request, scope));
    }


    /**
     * The node optimised for the variable alone and shared when it can be (see {@link #canShare}): solutions that
     * several parts of a pattern join, like the named graphs every zero-length path from a constant is paired with, are
     * then evaluated once by the statement. The node is optimised as code of the statement (see {@link Scope#TOP}),
     * wherever the parts joining it lie.
     *
     * @param request the current request
     * @param node the node
     * @param variable the only variable needed of the node
     * @return the node optimised for the variable and shared when it can be
     */
    public static SqlIntercode shareOptimized(Request request, SqlIntercode node, Variable variable)
    {
        SqlIntercode optimized = node.optimize(request, Scope.TOP, new Restrictions(Set.of(variable)), false, false);

        if(!canShare(request, Scope.TOP, optimized))
            return optimized;

        return share(optimized);
    }


    /**
     * Distinct values of the variable in the shared solutions, shared themselves, to seed a walk with (see
     * {@link SqlRecursive#seed}).
     *
     * @param request the current request
     * @param scope the scope of the node
     * @param shared the shared solutions
     * @param variable the variable
     * @param evalServices whether SERVICE stubs are evaluated
     * @return distinct values of the variable in the shared solutions, shared
     */
    protected static SqlIntercode seed(Request request, Scope scope, SqlIntercode shared, Variable variable,
            boolean evalServices)
    {
        Restrictions restrictions = new Restrictions(Set.of(variable));

        return share(SqlDistinct.create(request, shared, Set.of(variable)).optimize(request, scope, restrictions, false,
                evalServices));
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        // the child stays as optimised, so that every reference reads the same common table expression, and the
        // reference exposes no more than it did (the seeds read from the shared solutions expose the start alone)
        VariableBindings restricted = bindings.restrict(request.getConfiguration(), restrictions);

        if(restricted.equals(bindings))
            return this;

        return new SqlShared(restricted, child);
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        CommonTable table = request.getCommonTableExpression(child.translate(request, scope));

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");
        builder.append(columns.isEmpty() ? "1" : columns.stream().collect(SqlCode.joining(", ")));
        builder.append(" FROM ");
        builder.append(table);

        return builder.toCode();
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return child.isDistinct(request, selected);
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, List.of(child), variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("shared");

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    /**
     * Shared solutions.
     *
     * @return shared solutions
     */
    public final SqlIntercode getChild()
    {
        return child;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlShared imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(child);
    }
}
