package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
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
 * shared, so that every reference renders the same SQL, and a reference exposes what its parent needs of it. Outside
 * the translation of a statement, where no common table expression can be declared, the child is written in place.
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
     * True if the node can be shared: it is deterministic, holds no SERVICE call not evaluated yet, refers neither to
     * the current solution of an EXISTS nor to a lateral join (its SQL would read an alias a common table expression
     * cannot see), and no pattern of an expression is being optimised (its SQL is rendered into the expression at once,
     * outside a statement).
     *
     * @param request the current request
     * @param node the node
     * @return true if the node can be shared, false otherwise
     */
    protected static boolean canShare(Request request, SqlIntercode node)
    {
        if(request.isWithinExpression() || !node.isDeterministic() || node.hasServiceSubpattern())
            return false;

        if(node instanceof SqlShared || node instanceof SqlValues || node instanceof SqlContextSolution
                || node.equals(SqlEmptySolution.get()) || node.equals(SqlNoSolution.get()))
            return false;

        return !node.translate(request).contains("\"lateral");
    }


    /**
     * Distinct values of the variable in the shared solutions, shared themselves, to seed a walk with (see
     * {@link SqlRecursive#seed}).
     *
     * @param request the current request
     * @param shared the shared solutions
     * @param variable the variable
     * @param evalServices whether SERVICE stubs are evaluated
     * @return distinct values of the variable in the shared solutions, shared
     */
    protected static SqlIntercode seed(Request request, SqlIntercode shared, Variable variable, boolean evalServices)
    {
        Restrictions restrictions = new Restrictions(Set.of(variable));

        return share(SqlDistinct.create(request, shared, Set.of(variable)).optimize(request, restrictions, false,
                evalServices));
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        // the child stays as optimised, so that every reference reads the same common table expression, and the
        // reference exposes no more than it did (the seeds read from the shared solutions expose the start alone)
        VariableBindings restricted = bindings.restrict(request.getConfiguration(), restrictions);

        if(restricted.equals(bindings))
            return this;

        return new SqlShared(restricted, child);
    }


    @Override
    public String translate(Request request)
    {
        String query = child.translate(request);

        AliasTable table = request.getCommonTableExpression(query);

        if(table == null)
            return query;

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        return "SELECT " + (columns.isEmpty() ? "1" : columns.stream().map(Object::toString).collect(joining(", ")))
                + " FROM " + table;
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
    public Set<VirtualTable> getVirtualTables()
    {
        return child.getVirtualTables();
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
