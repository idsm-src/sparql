package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * The single solution an EXISTS pattern is evaluated for (the ContextSolution of SPARQL 1.2): the solution of the
 * surrounding pattern that the filter is being evaluated on, restricted to the variables the EXISTS pattern refers to.
 * Every group graph pattern inside the EXISTS pattern starts from it, so the pattern is joined with the current
 * solution wherever it binds a shared variable, and its filters and binds see the current values.
 *
 * The values are taken from the surrounding solution through the alias of the EXISTS expression (see
 * {@link cz.iocb.sparql.engine.imcode.expression.SqlExists}): the solution exposes them under column names fixed when
 * it is created, the expression supplies them under these names, and the solution reads them from the alias. A constant
 * of the surrounding pattern is kept as a constant, so the optimization of the pattern can use it. Like the bindings
 * exposed by {@link SqlLateralJoin}, the exposed columns are kept by every reconstruction of the EXISTS expression,
 * which supplies a column from what the surrounding pattern provides at the time of the translation.
 */
public final class SqlContextSolution extends SqlIntercode
{
    /**
     * Alias of the EXISTS expression the values are read from.
     */
    private final AliasTable table;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param table alias of the EXISTS expression the values are read from
     */
    protected SqlContextSolution(VariableBindings bindings, AliasTable table)
    {
        super(bindings, true);

        this.table = table;
    }


    /**
     * Context solution exposing the given variables of the surrounding bindings through the alias; the empty solution
     * when none of them is bound there. A class the surrounding pattern has no columns for cannot hold a value of the
     * solution and is left out.
     *
     * @param request the current request
     * @param table alias of the EXISTS expression the values are read from
     * @param surrounding bindings of the surrounding pattern
     * @param variables the variables to expose
     * @return context solution exposing the given variables of the surrounding bindings through the alias; the empty
     *         solution when none of them is bound there
     */
    public static SqlIntercode create(Request request, AliasTable table, VariableBindings surrounding,
            Set<Variable> variables)
    {
        VariableBindings bindings = new VariableBindings();

        for(VariableBinding binding : surrounding.getValues())
        {
            if(!variables.contains(binding.getVariable()))
                continue;

            VariableBinding exposed = new VariableBinding(binding.getVariable(), binding.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
            {
                ResourceClass resClass = entry.getKey();
                List<Column> columns = entry.getValue();

                if(columns == null)
                    continue;

                List<Column> names = resClass.createColumns(request.getColumnMap(), binding.getVariable());
                List<Column> exposedColumns = new ArrayList<>(columns.size());

                for(int i = 0; i < columns.size(); i++)
                {
                    Column column = columns.get(i);

                    if(column instanceof ConstantColumn)
                        exposedColumns.add(column);
                    else
                        exposedColumns.add(
                                new TableColumn(names.get(i).getName(), names.get(i).getType(), column.canBeNull()));
                }

                exposed.addMapping(resClass, exposedColumns);
            }

            if(!exposed.getClasses().isEmpty())
                bindings.add(exposed);
        }

        if(bindings.getVariables().isEmpty())
            return SqlEmptySolution.get();

        return new SqlContextSolution(bindings, table);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        VariableBindings optimized = bindings.restrict(request.getConfiguration(), restrictions);

        if(optimized.getVariables().isEmpty())
            return SqlEmptySolution.get();

        if(optimized.equals(bindings))
            return this;

        return new SqlContextSolution(optimized, table);
    }


    @Override
    public String translate(Request request)
    {
        Set<Column> columns = bindings.getNonConstantColumns();

        if(columns.isEmpty())
            return "SELECT 1";

        return columns.stream().map(c -> c.fromTable(table) + " AS " + c).sorted()
                .collect(joining(", ", "SELECT ", ""));
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return true;
    }


    /**
     * Alias of the EXISTS expression the values are read from.
     *
     * @return alias of the EXISTS expression the values are read from
     */
    public final AliasTable getLateralTable()
    {
        return table;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return false;
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return Set.of();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("context solution of ");
        builder.append(table);

        if(!bindings.getVariables().isEmpty())
            builder.append(bindings.getVariables().stream().map(v -> v.toString()).collect(joining(" ", " ", "")));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlContextSolution imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(table, imcode.table))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(table);
    }
}
