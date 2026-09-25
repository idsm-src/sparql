package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getIntersectionClass;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



//TODO: add support for binding parameter and result variables



/**
 * Call of a procedure (a set-returning SQL function) for every solution of the child: the parameters are expressions
 * over the child's variables, the results are bound to variables (joined with the child when a result variable is
 * already bound).
 */
public final class SqlProcedureCall extends SqlIntercode
{
    /**
     * Alias of the function result in the generated SQL.
     */
    private static final Variable resultVar = new Variable("@res");

    /**
     * Solutions the procedure is called for.
     */
    private final SqlIntercode child;

    /**
     * Called procedure.
     */
    private final ProcedureDefinition procedure;

    /**
     * Expression of each parameter.
     */
    private final LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters;

    /**
     * Variable receiving each result.
     */
    private final LinkedHashMap<ResultDefinition, Variable> results;

    /**
     * For each output column, the column of the call or the child it is taken from.
     */
    private final Map<Column, Column> columnMap;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param procedure the procedure definition
     * @param parameters expression of each parameter
     * @param results variable receiving each result
     * @param child the child node
     * @param columnMap the column map
     */
    protected SqlProcedureCall(VariableBindings bindings, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, SqlIntercode child, Map<Column, Column> columnMap)
    {
        super(bindings, child.isDeterministic()); //TODO: is procedure deterministic?

        this.child = child;
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.columnMap = columnMap;
    }


    /**
     * Procedure call over the child's solutions.
     *
     * @param request the current request
     * @param procedure the procedure definition
     * @param parameters expression of each parameter
     * @param results variable receiving each result
     * @param child the child node
     * @return procedure call over the child's solutions
     */
    public static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, SqlIntercode child)
    {
        return create(request, procedure, parameters, results, child, null);
    }


    /**
     * Procedure call over the child, joined with it on the variables used as parameters and results.
     *
     * @param request the current request
     * @param procedure the procedure definition
     * @param parameters expression of each parameter
     * @param results variable receiving each result
     * @param child the child node
     * @param restrictions what the parent needs of the variables
     * @return procedure call over the child, joined with it on the variables used as parameters and results
     */
    protected static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, SqlIntercode child, Restrictions restrictions)
    {
        Map<Variable, Set<ResourceClass>> resClasses = new HashMap<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = definition.getParameterClass();
            SqlExpressionIntercode node = entry.getValue();

            if(node instanceof SqlVariable var)
                resClasses.computeIfAbsent(var.getVariable(), _ -> new HashSet<>()).add(resClass);
        }


        VariableBindings callBindings = new VariableBindings();

        for(Entry<Variable, Set<ResourceClass>> e : resClasses.entrySet())
        {
            ResourceClass interClass = getIntersectionClass(e.getValue());
            Variable variable = e.getKey();

            if(interClass == null)
                continue;

            callBindings.add(new VariableBinding(variable, interClass, getColumns(child, variable, interClass), false));
        }

        for(Entry<ResultDefinition, Variable> entry : results.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            Variable variable = entry.getValue();

            Map<ResourceClass, List<Column>> mappings = new HashMap<>();

            for(Entry<ResourceClass, List<Column>> e : definition.getMappings().entrySet())
                mappings.put(e.getKey(), getSqlResultColumns(e.getValue()));

            callBindings.add(new VariableBinding(variable, mappings, false));
        }


        Map<Column, Column> columnMap = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, callBindings, child.getVariableBindings(), null,
                null, null, columnMap).restrict(restrictions);

        return new SqlProcedureCall(bindings, procedure, parameters, results, child, columnMap);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        LinkedHashMap<ResultDefinition, Variable> optResults = new LinkedHashMap<>();

        for(Entry<ResultDefinition, Variable> result : results.entrySet())
            if(restrictions.contains(result.getValue(), result.getKey().getMappings().keySet()))
                optResults.put(result.getKey(), result.getValue());


        Restrictions childRestrictions = new Restrictions(restrictions);

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
            childRestrictions.add(entry.getValue().getRequirements());

        //FIXME: is procedure deterministic?

        SqlIntercode optChild = child.optimize(request, childRestrictions, reduced, evalServices);
        LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> optParameters = optimize(request, parameters,
                optChild);


        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> childParams = optimize(request,
                        optParameters, child);
                childs.add(create(request, procedure, childParams, optResults, child, restrictions));
            }

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        Map<Variable, Set<ResourceClass>> resClasses = new HashMap<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : optParameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = definition.getParameterClass();
            SqlExpressionIntercode node = entry.getValue();

            if(node instanceof SqlVariable var)
                resClasses.computeIfAbsent(var.getVariable(), _ -> new HashSet<>()).add(resClass);
            else if(node == null || ResourceClass.areDisjunct(resClass, node.getResourceClasses()))
                return SqlNoSolution.get();
        }


        VariableBindings callBindings = new VariableBindings();

        for(Entry<Variable, Set<ResourceClass>> e : resClasses.entrySet())
        {
            ResourceClass interClass = getIntersectionClass(e.getValue());
            Variable variable = e.getKey();

            if(interClass == null)
                return SqlNoSolution.get();

            callBindings
                    .add(new VariableBinding(variable, interClass, getColumns(optChild, variable, interClass), false));
        }


        for(Entry<ResultDefinition, Variable> entry : optResults.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            Variable variable = entry.getValue();

            Map<ResourceClass, List<Column>> mappings = new HashMap<>();

            for(Entry<ResourceClass, List<Column>> e : definition.getMappings().entrySet())
                mappings.put(e.getKey(), getSqlResultColumns(e.getValue()));

            callBindings.add(new VariableBinding(variable, mappings, false));
        }

        for(SqlIntercode r : getJoinList(optChild))
        {
            List<VariableBindingPair> pairs = VariableBindingPair.getPairs(callBindings, r.getVariableBindings());

            if(pairs.stream().anyMatch(p -> !p.isJoinable()))
                return SqlNoSolution.get();
        }


        if(optResults.equals(results) && optParameters.equals(parameters) && optChild == child
                && restrictions.isOptimized(bindings))
            return this;

        return create(request, procedure, optParameters, optResults, optChild, restrictions);
    }


    /**
     * Optimises the parameter expressions over the child's bindings.
     *
     * @param request the current request
     * @param parameters expression of each parameter
     * @param context solutions the call is evaluated for
     * @return the optimised parameter expressions
     */
    protected static LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> optimize(Request request,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters, SqlIntercode context)
    {
        LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> result = new LinkedHashMap<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> e : parameters.entrySet())
            result.put(e.getKey(), e.getValue().optimize(request, context.getVariableBindings(),
                    new Restriction(e.getKey().getParameterClass()), false));

        return result;
    }


    @Override
    public String translate(Request request)
    {
        Set<Column> columns = bindings.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        generateInnerSelect(request, builder);
        builder.append(" ) AS tab ");

        return builder.toString();
    }


    /**
     * Appends the SQL calling the function with the parameters converted to their declared classes.
     *
     * @param request the current request
     * @param builder the builder to append to
     */
    private void generateInnerSelect(Request request, StringBuilder builder)
    {
        builder.append("SELECT ");

        builder.append(procedure.getSqlProcedure());

        boolean hasParameter = false;
        builder.append("(");

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
        {
            appendComma(builder, hasParameter);
            hasParameter = true;

            ResourceClass resClass = entry.getKey().getParameterClass();
            SqlExpressionIntercode node = entry.getValue();
            List<Column> columns = node.get(resClass);

            for(int i = 0; i < resClass.getColumnCount(); i++)
            {
                appendComma(builder, i > 0);
                builder.append(columnMap.getOrDefault(columns.get(i), columns.get(i)));
            }
        }

        builder.append(")");

        builder.append(" AS \"");
        builder.append(resultVar);
        builder.append('"');

        for(Column column : child.getVariableBindings()
                .restrict(new Restrictions(this.getVariableBindings().getVariables())).getNonConstantColumns())
        {
            builder.append(", ");
            builder.append(column);
        }

        if(!child.equals(SqlEmptySolution.get()))
        {
            builder.append(" FROM (");
            builder.append(child.translate(request));
            builder.append(" ) AS tab");
        }
    }


    /**
     * Columns of the variable in the class as provided by the child, or NULL constants when unbound.
     *
     * @param child the child node
     * @param variable the variable
     * @param resClass the resource class
     * @return columns of the variable in the class as provided by the child, or NULL constants when unbound
     */
    private static List<Column> getColumns(SqlIntercode child, Variable variable, ResourceClass resClass)
    {
        VariableBinding binding = child.getVariableBindings().get(variable);

        if(binding == null)
            return resClass.getSqlTypes().stream().map(t -> (Column) new NullColumn(t)).toList();

        return binding.deriveMapping(resClass);
    }


    /**
     * Columns reading a result from the function result: the whole result, or its fields.
     *
     * @param fields fields of the SQL result row, or null
     * @return columns reading a result from the function result: the whole result, or its fields
     */
    private static List<Column> getSqlResultColumns(List<Column> fields)
    {
        List<Column> result = new ArrayList<>();

        if(fields == null)
        {
            result.add(new ExpressionColumn("\"" + resultVar + "\""));
        }
        else
        {
            for(Column field : fields)
            {
                if(field instanceof TableColumn)
                    result.add(new ExpressionColumn("(\"" + resultVar + "\")." + field));
                else if(field instanceof ConstantColumn)
                    result.add(field);
                else
                    throw new IllegalArgumentException();
            }
        }

        return result;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        Set<VirtualTable> tables = getVirtualTables(child);
        tables.addAll(getVirtualTables(parameters.values()));
        return tables;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("call ");
        builder.append(procedure.getProcedureName());

        for(Entry<ResultDefinition, Variable> e : results.entrySet())
        {
            indentInfo(builder, indent, true);

            if(e.getKey().getResultName() != null)
                builder.append(e.getKey().getResultName()).append(" ");
            else
                builder.append("#result");

            builder.append(" to ");
            builder.append(e.getValue());
        }

        for(Entry<ParameterDefinition, SqlExpressionIntercode> e : parameters.entrySet())
        {
            indentInfo(builder, indent, true);
            builder.append(e.getKey().getParamName());
            builder.append(" as ");
            e.getValue().generateExplanation(builder, null);
        }

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlProcedureCall imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(procedure, imcode.procedure))
            return false;

        if(!Objects.equals(parameters, imcode.parameters))
            return false;

        if(!Objects.equals(results, imcode.results))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(procedure, parameters, results, child);
    }
}
