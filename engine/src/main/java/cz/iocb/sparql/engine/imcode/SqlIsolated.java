package cz.iocb.sparql.engine.imcode;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Solutions of a pattern evaluated independently of the current solutions of the patterns of expressions (EXISTS) it
 * lies in: the child is optimised and translated in a scope without them (see {@link Scope#isolate}), so that no rule
 * takes its solutions for extensions of a current solution. It holds the pattern of a SERVICE call of a service of the
 * configuration inside such a pattern, which a remote call would evaluate by itself as well.
 */
public final class SqlIsolated extends SqlIntercode
{
    /**
     * The isolated solutions.
     */
    private final SqlIntercode child;


    /**
     * Creates the node.
     *
     * @param child the isolated solutions
     */
    private SqlIsolated(SqlIntercode child)
    {
        super(child.getVariableBindings(), child.isDeterministic());

        this.child = child;
    }


    /**
     * The solutions isolated; the empty solution and no solution are no different in any scope.
     *
     * @param child the solutions
     * @return the solutions isolated
     */
    public static SqlIntercode create(SqlIntercode child)
    {
        if(child instanceof SqlIsolated || child.equals(SqlEmptySolution.get()) || child.equals(SqlNoSolution.get()))
            return child;

        return new SqlIsolated(child);
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        SqlIntercode optimized = child.optimize(request, scope.isolate(), restrictions, reduced, evalServices);

        if(optimized == child)
            return this;

        return create(optimized);
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        return child.translate(request, scope.isolate());
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
        builder.append("isolated");

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    /**
     * The isolated solutions.
     *
     * @return the isolated solutions
     */
    public SqlIntercode getChild()
    {
        return child;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlIsolated imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        return Objects.equals(child, imcode.child);
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(child);
    }
}
