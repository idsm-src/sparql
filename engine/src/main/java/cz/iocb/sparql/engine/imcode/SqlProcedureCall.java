package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.getAcceptedClass;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.intersect;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureSpecialization;
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
 * already bound). The function takes the values of the parameters in the columns of their declared classes (see
 * {@link ParameterDefinition}), so a literal of the datatype of a canonical declared class supplies its value in every
 * valid lexical form and a numeric declared class takes the value of every numeric XPath substitutes or promotes to its
 * datatype (see {@link SqlExpressionIntercode#getValue}), and a parameter supplying no value of its class gives no
 * solution. A call over the empty solution, which takes nothing from any context, is evaluated by a common table
 * expression of the statement (see {@link Request#getCommonTableExpression}), so that the statement evaluates it once
 * even when it occurs several times, e.g. in every branch of a union a join above it was distributed over; PostgreSQL
 * folds a common table expression referenced once into the query. A result is bound to the classes the parent accepts a
 * value of only, and a specialiser of the procedure may adapt the call to them (see
 * {@link ProcedureDefinition#setSpecializer}).
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
     * Classes the call binds each result in: those of its definition the parent accepts a value of (see
     * {@link #getResultClasses}), narrowed by the specialisation when it binds fewer.
     */
    private final Map<ResultDefinition, Set<ResourceClass>> resultClasses;

    /**
     * Specialisation of the call to the classes it binds the results in, or null for the call as declared.
     */
    private final ProcedureSpecialization specialization;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param procedure the procedure definition
     * @param parameters expression of each parameter
     * @param results variable receiving each result
     * @param child the child node
     * @param columnMap the column map
     * @param resultClasses classes the call binds each result in
     * @param specialization specialisation of the call, or null
     */
    protected SqlProcedureCall(VariableBindings bindings, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, SqlIntercode child, Map<Column, Column> columnMap,
            Map<ResultDefinition, Set<ResourceClass>> resultClasses, ProcedureSpecialization specialization)
    {
        super(bindings, child.isDeterministic()); //TODO: is procedure deterministic?

        this.child = child;
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.columnMap = columnMap;
        this.resultClasses = resultClasses;
        this.specialization = specialization;
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
        return create(request, procedure, parameters, results, child, null, null);
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
     * @param boundClasses classes the call binds each result in so far, narrowed further to those the parent accepts a
     *            value of; null for all the classes of the definitions
     * @return procedure call over the child, joined with it on the variables used as parameters and results, or no
     *         solutions when a variable is used as parameters of disjoint classes or the parent accepts a result in no
     *         class
     */
    protected static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, SqlIntercode child, Restrictions restrictions,
            Map<ResultDefinition, Set<ResourceClass>> boundClasses)
    {
        ClassRelations relations = request.getConfiguration();

        Map<Variable, Set<ResourceClass>> resClasses = new HashMap<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = getAcceptedClass(definition.getParameterClass());
            SqlExpressionIntercode node = entry.getValue();

            if(node instanceof SqlVariable var)
            {
                Set<ResourceClass> classes = resClasses.computeIfAbsent(var.getVariable(), _ -> new HashSet<>());

                if(classes.stream().anyMatch(c -> ResourceClass.areDisjunct(relations, c, resClass)))
                    return SqlNoSolution.get();

                classes.add(resClass);
            }
        }


        VariableBindings callBindings = new VariableBindings();

        for(Entry<Variable, Set<ResourceClass>> e : resClasses.entrySet())
        {
            ResourceClass interClass = intersect(relations, e.getValue());
            callBindings.add(getParameterBinding(relations, child, e.getKey(), interClass));
        }

        Map<ResultDefinition, Set<ResourceClass>> resultClasses = new HashMap<>();

        for(Entry<ResultDefinition, Variable> entry : results.entrySet())
        {
            Set<ResourceClass> classes = getResultClasses(relations, entry.getKey(), entry.getValue(), restrictions,
                    boundClasses == null ? null : boundClasses.get(entry.getKey()));

            if(classes.isEmpty())
                return SqlNoSolution.get();

            resultClasses.put(entry.getKey(), classes);
        }

        ProcedureSpecialization specialization = procedure.getSpecializer() == null || results.isEmpty() ? null :
                procedure.getSpecializer().specialize(Collections.unmodifiableMap(resultClasses));

        for(Entry<ResultDefinition, Variable> entry : results.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            Set<ResourceClass> classes = resultClasses.get(definition);
            Set<ResourceClass> bound = specialization == null ? null : specialization.getResultClasses(definition);

            // the specialisation may bind fewer classes, never more than the query can use
            if(bound != null)
            {
                classes = classes.stream().filter(bound::contains).collect(toSet());

                if(classes.isEmpty())
                    return SqlNoSolution.get();

                resultClasses.put(definition, classes);
            }

            Map<ResourceClass, List<Column>> mappings = new HashMap<>();

            for(ResourceClass resClass : classes)
                mappings.put(resClass, getSqlResultColumns(resClass, definition.getMappings().get(resClass)));

            callBindings.add(new VariableBinding(entry.getValue(), mappings, false));
        }


        Map<Column, Column> columnMap = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, callBindings, child.getVariableBindings(), null,
                null, null, columnMap).restrict(relations, restrictions);

        return new SqlProcedureCall(bindings, procedure, parameters, results, child, columnMap, resultClasses,
                specialization);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        LinkedHashMap<ResultDefinition, Variable> optResults = new LinkedHashMap<>();

        for(Entry<ResultDefinition, Variable> result : results.entrySet())
            if(restrictions.contains(relations, result.getValue(), result.getKey().getMappings().keySet()))
                optResults.put(result.getKey(), result.getValue());


        Restrictions childRestrictions = new Restrictions(restrictions);

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
            childRestrictions.add(entry.getValue().getRequirements(relations));

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
                childs.add(create(request, procedure, childParams, optResults, child, restrictions, resultClasses));
            }

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        Map<Variable, Set<ResourceClass>> resClasses = new HashMap<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : optParameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = getAcceptedClass(definition.getParameterClass());
            SqlExpressionIntercode node = entry.getValue();

            if(node instanceof SqlVariable var)
            {
                Set<ResourceClass> classes = resClasses.computeIfAbsent(var.getVariable(), _ -> new HashSet<>());

                if(classes.stream().anyMatch(c -> ResourceClass.areDisjunct(relations, c, resClass)))
                    return SqlNoSolution.get();

                classes.add(resClass);
            }
            else if(node == null || ResourceClass.areDisjunct(relations, resClass, node.getResourceClasses()))
            {
                return SqlNoSolution.get();
            }
        }


        VariableBindings callBindings = new VariableBindings();

        for(Entry<Variable, Set<ResourceClass>> e : resClasses.entrySet())
        {
            ResourceClass interClass = intersect(relations, e.getValue());
            callBindings.add(getParameterBinding(relations, optChild, e.getKey(), interClass));
        }


        for(Entry<ResultDefinition, Variable> entry : optResults.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            Set<ResourceClass> classes = getResultClasses(relations, definition, entry.getValue(), restrictions,
                    resultClasses.get(definition));

            if(classes.isEmpty())
                return SqlNoSolution.get();

            Map<ResourceClass, List<Column>> mappings = new HashMap<>();

            for(ResourceClass resClass : classes)
                mappings.put(resClass, getSqlResultColumns(resClass, definition.getMappings().get(resClass)));

            callBindings.add(new VariableBinding(entry.getValue(), mappings, false));
        }

        for(SqlIntercode r : getJoinList(optChild))
        {
            List<VariableBindingPair> pairs = VariableBindingPair.getPairs(relations, callBindings,
                    r.getVariableBindings());

            if(pairs.stream().anyMatch(p -> !p.isJoinable()))
                return SqlNoSolution.get();
        }


        // the classes the call binds the results in narrow to the classes the parent accepts
        boolean sameClasses = optResults.entrySet().stream().allMatch(
                e -> getResultClasses(relations, e.getKey(), e.getValue(), restrictions, resultClasses.get(e.getKey()))
                        .equals(resultClasses.get(e.getKey())));

        if(sameClasses && optResults.equals(results) && optParameters.equals(parameters) && optChild == child
                && restrictions.isOptimized(relations, bindings))
            return this;

        return create(request, procedure, optParameters, optResults, optChild, restrictions, resultClasses);
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
                    new Restriction(getAcceptedClass(e.getKey().getParameterClass())), false));

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

        builder.append(" FROM ");

        String call = translateCall(request);
        AliasTable table = child.equals(SqlEmptySolution.get()) ? request.getCommonTableExpression(call) : null;

        if(table != null)
        {
            builder.append(table);
        }
        else
        {
            builder.append("(");
            builder.append(call);
            builder.append(" ) AS tab ");
        }

        return builder.toString();
    }


    /**
     * SQL calling the function with the values of the parameters in the columns of their declared classes for every
     * solution of the child.
     *
     * @param request the current request
     * @return SQL calling the function with the values of the parameters in the columns of their declared classes for
     *         every solution of the child
     */
    private String translateCall(Request request)
    {
        ClassRelations relations = request.getConfiguration();

        List<String> declared = new ArrayList<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
        {
            ResourceClass resClass = entry.getKey().getParameterClass();
            SqlExpressionIntercode node = entry.getValue();

            for(Column column : node.getValue(relations, resClass))
                declared.add(columnMap.getOrDefault(column, column).toString());
        }

        Function function = specialization == null || specialization.getSqlProcedure() == null ?
                procedure.getSqlProcedure() : specialization.getSqlProcedure();
        List<String> arguments = specialization == null ? declared : specialization.getArguments(declared);

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        builder.append(function);
        builder.append(arguments.stream().collect(joining(", ", "(", ")")));

        builder.append(" AS \"");
        builder.append(resultVar);
        builder.append('"');

        for(Column column : child.getVariableBindings()
                .restrict(relations, new Restrictions(this.getVariableBindings().getVariables()))
                .getNonConstantColumns())
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

        return builder.toString();
    }


    /**
     * Classes the call binds a result in: of the classes bound so far (all the classes of the definition at first), a
     * class the parent accepts no value of is left out, so that the call binds the result to the classes the query can
     * use and a specialisation may skip the work for the others. The classes only narrow: once left out, a class does
     * not come back when a later optimisation pass, seeing the narrowed bindings, accepts every class of them.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param definition the result definition
     * @param variable the variable receiving the result
     * @param restrictions what the parent needs of the variables, or null
     * @param classes classes the call binds the result in so far, or null for all the classes of the definition
     * @return classes the call binds the result in
     */
    private static Set<ResourceClass> getResultClasses(ClassRelations relations, ResultDefinition definition,
            Variable variable, Restrictions restrictions, Set<ResourceClass> classes)
    {
        Set<ResourceClass> accepted = restrictions == null ? null : restrictions.getAcceptedClasses(variable);

        return definition.getMappings().keySet().stream().filter(c -> (classes == null || classes.contains(c))
                && (accepted == null || accepted.stream().anyMatch(a -> !ResourceClass.areDisjunct(relations, a, c))))
                .collect(toSet());
    }


    /**
     * Binding of a variable the call takes as parameters accepting the class: the classes the child binds the variable
     * in, narrowed to the class, so that a class of the child the parameters accept keeps the columns of the child; the
     * class with NULL constants when the child binds the variable in no class overlapping it.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param child the child node
     * @param variable the variable
     * @param resClass the class the parameters accept
     * @return binding of a variable the call takes as parameters accepting the class
     */
    private static VariableBinding getParameterBinding(ClassRelations relations, SqlIntercode child, Variable variable,
            ResourceClass resClass)
    {
        VariableBinding binding = child.getVariableBindings().get(variable);
        Map<ResourceClass, List<Column>> mappings = new HashMap<>();

        if(binding != null)
        {
            for(ResourceClass childClass : binding.getCompatibleClasses(relations, resClass))
            {
                ResourceClass narrowed = intersect(relations, childClass, resClass);
                mappings.put(narrowed, binding.deriveMapping(relations, narrowed));
            }
        }

        if(mappings.isEmpty())
            mappings.put(resClass, resClass.getSqlTypes().stream().map(t -> (Column) new NullColumn(t)).toList());

        return new VariableBinding(variable, mappings, false);
    }


    /**
     * Columns reading a result from the function result: the whole result, or its fields.
     *
     * @param resClass class of the result
     * @param fields fields of the SQL result row, or null
     * @return columns reading a result from the function result: the whole result, or its fields
     */
    private static List<Column> getSqlResultColumns(ResourceClass resClass, List<Column> fields)
    {
        List<Column> result = new ArrayList<>();

        if(fields == null)
        {
            result.add(new ExpressionColumn("\"" + resultVar + "\"", resClass.getSqlTypes().get(0)));
        }
        else
        {
            for(Column field : fields)
            {
                if(field instanceof TableColumn)
                    result.add(new ExpressionColumn("(\"" + resultVar + "\")." + field, field.getType()));
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
        return child.hasServiceSubpattern() || parameters.values().stream().anyMatch(p -> p.hasServiceSubpattern());
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        Set<VirtualTable> tables = getVirtualTables(child);
        tables.addAll(getVirtualTables(parameters.values()));
        return tables;
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, List.of(child), variable, resClass, index);
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

        if(!Objects.equals(resultClasses, imcode.resultClasses))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(procedure, parameters, results, child, resultClasses);
    }
}
