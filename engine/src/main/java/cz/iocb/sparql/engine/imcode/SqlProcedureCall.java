package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.getAcceptedClass;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.getValueTypes;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.isOptionalValueColumn;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.intersect;
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
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.CommonTable;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ProcedureNodeClass;
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



/**
 * Call of a procedure (a set-returning SQL function) for every solution of the child: the parameters are expressions
 * over the child's variables, the results are bound to fresh variables, as the translator renames a result variable
 * that the child binds already and merges it with the result (see {@link SqlMerge}), so the node makes no join
 * condition. The variables written in place of the nodes standing for the parameters and the results in the text of the
 * call are bound as a pattern of triples would bind them, to blank nodes held by columns (see
 * {@link ProcedureNodeClass}), each procedure having a class of the nodes of its parameters and one of the nodes of its
 * rows of results (see {@link #getNodeClasses}): the node of the parameters is held by the values the call passes to
 * the parameters, so it is the same for every call of the procedure passing the same values, and all results of a call
 * share it; the node of the results of a call with several results is held by those values and the text of the row of
 * results the function returns. The constant values hold the nodes as they are, the others are read from the call (see
 * {@link #translateCall}), and a node the parent does not need is not bound. The function takes the values of the
 * parameters in the columns of their declared classes (see {@link ParameterDefinition}), so a literal of the datatype
 * of a canonical declared class supplies its value in every valid lexical form, a numeric declared class takes the
 * value of every numeric XPath substitutes or promotes to its datatype and the class of the string literals takes the
 * string of a string literal of any kind (see {@link SqlExpressionIntercode#getValue}), and a parameter supplying no
 * value of its class gives no solution. A call of a deterministic procedure (see
 * {@link ProcedureDefinition#setDeterministic}) over the empty solution, which takes nothing from any context, is
 * evaluated by a common table expression (see {@link Request#getCommonTableExpression}), so that the statement
 * evaluates it once even when it occurs several times, e.g. in every branch of a union a join above it was distributed
 * over, or in the pattern of an EXISTS evaluated for every current solution; PostgreSQL folds a common table expression
 * referenced once into the query unless the function is volatile. A call whose parameter takes a value of a current
 * solution, that of an EXISTS pattern or of the left side of a lateral join (see
 * {@link Scope#refersToCurrentSolution}), stays in its place, where the value is known. A result is bound to the
 * classes the parent accepts a value of only, and a specialiser of the procedure may adapt the call to them (see
 * {@link ProcedureDefinition#setSpecializer}).
 */
public final class SqlProcedureCall extends SqlIntercode
{
    /**
     * Alias of the function result in the generated SQL.
     */
    private static final Variable resultVar = new Variable("@res");

    /**
     * Prefix of the aliases of the values the call passes to the parameters in the generated SQL, which a node standing
     * for the parameters or the results reads (see {@link #getNodeColumns}).
     */
    private static final String parameterAlias = "@par";

    /**
     * Classes of the nodes standing for the parameters and the results of the calls of each procedure (see
     * {@link #getNodeClasses}), made on the first call of the procedure; the procedures are held weakly, and the
     * classes do not refer to them.
     */
    private static final Map<ProcedureDefinition, NodeClasses> nodeClasses = Collections
            .synchronizedMap(new WeakHashMap<>());

    /**
     * The segment of the next class of nodes of procedure calls: the segments follow the reserved segment of the blank
     * nodes of BNODE() upwards (see {@link cz.iocb.sparql.engine.mapping.classes.BuiltinClasses#bnodeStrBlankNode}),
     * whereas the blank nodes of the SERVICE calls of a request take the segments below zero downwards.
     */
    private static final AtomicInteger nextSegment = new AtomicInteger(Integer.MIN_VALUE + 1);


    /**
     * Classes of the nodes standing for the parameters and the results of the calls of a procedure.
     *
     * @param parameters class of the nodes standing for the parameters, held by the values passed to them
     * @param results class of the nodes standing for the rows of results, held by the values passed to the parameters
     *            and the text of the row
     */
    private record NodeClasses(ProcedureNodeClass parameters, ProcedureNodeClass results)
    {
    }

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
     * Variable receiving the node standing for the parameters, or null when the parent does not need it.
     */
    private final Variable parameterNode;

    /**
     * Variable receiving the node standing for the results, or null when the call has none or the parent does not need
     * it.
     */
    private final Variable resultNode;

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
     * @param parameterNode variable receiving the node standing for the parameters, or null
     * @param resultNode variable receiving the node standing for the results, or null
     * @param child the child node
     * @param columnMap the column map
     * @param resultClasses classes the call binds each result in
     * @param specialization specialisation of the call, or null
     */
    protected SqlProcedureCall(VariableBindings bindings, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, Variable parameterNode, Variable resultNode,
            SqlIntercode child, Map<Column, Column> columnMap, Map<ResultDefinition, Set<ResourceClass>> resultClasses,
            ProcedureSpecialization specialization)
    {
        super(bindings, child.isDeterministic() && procedure.isDeterministic());

        this.child = child;
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.parameterNode = parameterNode;
        this.resultNode = resultNode;
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
     * @param parameterNode variable receiving the node standing for the parameters, or null
     * @param resultNode variable receiving the node standing for the results, or null
     * @param child the child node
     * @return procedure call over the child's solutions
     */
    public static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, Variable parameterNode, Variable resultNode,
            SqlIntercode child)
    {
        return create(request, procedure, parameters, results, parameterNode, resultNode, child, null, null);
    }


    /**
     * Procedure call over the child, joined with it on the variables used as parameters and results.
     *
     * @param request the current request
     * @param procedure the procedure definition
     * @param parameters expression of each parameter
     * @param results variable receiving each result
     * @param parameterNode variable receiving the node standing for the parameters, or null
     * @param resultNode variable receiving the node standing for the results, or null
     * @param child the child node
     * @param restrictions what the parent needs of the variables
     * @param boundClasses classes the call binds each result in so far, narrowed further to those the parent accepts a
     *            value of; null for all the classes of the definitions
     * @return procedure call over the child, joined with it on the variables used as parameters and results, or no
     *         solutions when a variable is used as parameters of disjoint classes or the parent accepts a result or a
     *         node in no class
     */
    protected static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, Variable> results, Variable parameterNode, Variable resultNode,
            SqlIntercode child, Restrictions restrictions, Map<ResultDefinition, Set<ResourceClass>> boundClasses)
    {
        ClassRelations relations = request.getConfiguration();
        NodeClasses nodeClasses = getNodeClasses(procedure);

        if(!isAccepted(relations, restrictions, parameterNode, nodeClasses.parameters())
                || !isAccepted(relations, restrictions, resultNode, nodeClasses.results()))
            return SqlNoSolution.get();

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

        addNodeBindings(relations, callBindings, procedure, parameters, parameterNode, resultNode);


        Map<Column, Column> columnMap = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, callBindings, child.getVariableBindings(), null,
                null, null, columnMap).restrict(relations, restrictions);

        return new SqlProcedureCall(bindings, procedure, parameters, results, parameterNode, resultNode, child,
                columnMap, resultClasses, specialization);
    }


    /**
     * True if the parent accepts a value of the class of a node the call binds to the variable: the call binds the
     * variable always, so a value the parent accepts in no class leaves no solution.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param restrictions what the parent needs of the variables, or null
     * @param variable the variable receiving the node, or null
     * @param resClass the class of the node
     * @return true if the parent accepts a value of the class of the node, or no variable receives it, false otherwise
     */
    private static boolean isAccepted(ClassRelations relations, Restrictions restrictions, Variable variable,
            ResourceClass resClass)
    {
        Set<ResourceClass> accepted = variable == null || restrictions == null ? null :
                restrictions.getAcceptedClasses(variable);

        return accepted == null || accepted.stream().anyMatch(a -> !ResourceClass.areDisjunct(relations, a, resClass));
    }


    /**
     * Classes of the nodes standing for the parameters and the results of the calls of the procedure, made on its first
     * call, when it is defined: the node of the parameters is held by the columns of the values passed to the
     * parameters, in the order of their definitions, as the call passes them (see
     * {@link SqlExpressionIntercode#getValueTypes}), or by the empty string for a procedure without parameters, and the
     * node of the results by those columns and the text of the row of results. Each class takes a segment of its own.
     *
     * @param procedure the procedure definition
     * @return classes of the nodes standing for the parameters and the results of the calls of the procedure
     */
    private static NodeClasses getNodeClasses(ProcedureDefinition procedure)
    {
        return nodeClasses.computeIfAbsent(procedure, _ -> {
            List<SqlType> types = new ArrayList<>();
            List<Boolean> optional = new ArrayList<>();

            for(ParameterDefinition parameter : procedure.getParameters())
            {
                List<SqlType> valueTypes = getValueTypes(parameter.getParameterClass());

                for(int i = 0; i < valueTypes.size(); i++)
                {
                    types.add(valueTypes.get(i));
                    optional.add(isOptionalValueColumn(parameter.getParameterClass(), i));
                }
            }

            if(types.isEmpty())
            {
                types.add(VARCHAR);
                optional.add(false);
            }

            ProcedureNodeClass parameters = new ProcedureNodeClass(nextSegment.getAndIncrement(), types, optional);

            types.add(VARCHAR);
            optional.add(false);

            return new NodeClasses(parameters, new ProcedureNodeClass(nextSegment.getAndIncrement(), types, optional));
        });
    }


    /**
     * Columns holding the node standing for the parameters of a call: the columns of the values passed to the
     * parameters, a constant as it is and any other column read by its alias from the call (see
     * {@link #translateCall}), or the empty string for a procedure without parameters.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param parameters expression of each parameter
     * @return columns holding the node standing for the parameters of the call
     */
    private static List<Column> getNodeColumns(ClassRelations relations,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters)
    {
        List<Column> columns = new ArrayList<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
            for(Column column : entry.getValue().getValue(relations, entry.getKey().getParameterClass()))
                columns.add(column instanceof ConstantColumn ? column : new ExpressionColumn(
                        SqlCode.of("\"" + parameterAlias + columns.size() + "\""), column.getType()));

        if(columns.isEmpty())
            columns.add(new ValueColumn("", VARCHAR));

        return columns;
    }


    /**
     * Adds the bindings of the variables receiving the nodes standing for the parameters and the results (see
     * {@link #getNodeColumns}); the text of the row of results holding the node of the results is read from the result
     * of the function.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param bindings the bindings of the call
     * @param procedure the procedure definition
     * @param parameters expression of each parameter
     * @param parameterNode variable receiving the node standing for the parameters, or null
     * @param resultNode variable receiving the node standing for the results, or null
     */
    private static void addNodeBindings(ClassRelations relations, VariableBindings bindings,
            ProcedureDefinition procedure, LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            Variable parameterNode, Variable resultNode)
    {
        if(parameterNode == null && resultNode == null)
            return;

        NodeClasses classes = getNodeClasses(procedure);
        List<Column> columns = getNodeColumns(relations, parameters);

        if(parameterNode != null)
            bindings.add(new VariableBinding(parameterNode, classes.parameters(), columns, false));

        if(resultNode != null)
        {
            List<Column> row = new ArrayList<>(columns);
            row.add(new ExpressionColumn(SqlCode.of("quote_nullable(\"", resultVar, "\")"), VARCHAR));

            bindings.add(new VariableBinding(resultNode, classes.results(), row, false));
        }
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        LinkedHashMap<ResultDefinition, Variable> optResults = new LinkedHashMap<>();

        // the parent may need a result in no class (a join comparing it with classes the call never binds), and it has
        // to stay bound then, as its classes decide whether the parent finds a solution
        for(Entry<ResultDefinition, Variable> result : results.entrySet())
            if(restrictions.containsVar(result.getValue()))
                optResults.put(result.getKey(), result.getValue());

        // a node the parent does not need is not computed
        Variable optParameterNode = parameterNode != null && restrictions.containsVar(parameterNode) ? parameterNode :
                null;
        Variable optResultNode = resultNode != null && restrictions.containsVar(resultNode) ? resultNode : null;


        Restrictions childRestrictions = new Restrictions(restrictions);

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
            childRestrictions.add(entry.getValue().getRequirements(relations));

        // a procedure that is not deterministic is called for every solution of the child, so duplicates do matter
        SqlIntercode optChild = child.optimize(request, scope, childRestrictions,
                reduced && procedure.isDeterministic(), evalServices);
        LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> optParameters = optimize(request, scope, parameters,
                optChild);


        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> childParams = optimize(request, scope,
                        optParameters, child);
                childs.add(create(request, procedure, childParams, optResults, optParameterNode, optResultNode, child,
                        restrictions, resultClasses));
            }

            return SqlUnion.union(request, childs).optimize(request, scope, restrictions, reduced, evalServices);
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

        addNodeBindings(relations, callBindings, procedure, optParameters, optParameterNode, optResultNode);

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

        if(sameClasses && optResults.equals(results) && Objects.equals(optParameterNode, parameterNode)
                && Objects.equals(optResultNode, resultNode) && optParameters.equals(parameters) && optChild == child
                && restrictions.isOptimized(relations, bindings))
            return this;

        return create(request, procedure, optParameters, optResults, optParameterNode, optResultNode, optChild,
                restrictions, resultClasses);
    }


    /**
     * Optimises the parameter expressions over the child's bindings.
     *
     * @param request the current request
     * @param scope the scope of the call
     * @param parameters expression of each parameter
     * @param context solutions the call is evaluated for
     * @return the optimised parameter expressions
     */
    protected static LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> optimize(Request request, Scope scope,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters, SqlIntercode context)
    {
        LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> result = new LinkedHashMap<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> e : parameters.entrySet())
            result.put(e.getKey(), e.getValue().optimize(request, scope, context.getVariableBindings(),
                    new Restriction(getAcceptedClass(e.getKey().getParameterClass())), false));

        return result;
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        Set<Column> columns = bindings.getNonConstantColumns();

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream()
                    .map(c -> columnMap.get(c) != null ? SqlCode.of(columnMap.get(c), " AS ", c) : c.getCode())
                    .collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM ");

        SqlCode call = translateCall(request, scope);
        // the common table expression of a call is shared by every place of the statement calling it alike
        CommonTable table = procedure.isDeterministic() && child.equals(SqlEmptySolution.get())
                && !scope.refersToCurrentSolution(call) ? request.getCommonTableExpression(call) : null;

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

        return builder.toCode();
    }


    /**
     * SQL code calling the function with the values of the parameters in the columns of their declared classes for
     * every solution of the child.
     *
     * @param request the current request
     * @param scope the scope of the call
     * @return SQL code calling the function with the values of the parameters in the columns of their declared classes
     *         for every solution of the child
     */
    private SqlCode translateCall(Request request, Scope scope)
    {
        ClassRelations relations = request.getConfiguration();

        List<SqlCode> declared = new ArrayList<>();

        // the values a node standing for the parameters or the results reads by their aliases (see getNodeColumns)
        List<SqlCode> read = new ArrayList<>();

        // a value that the class of its parameter does not hold, missing in the value column, gives no solution, whether
        // the function is strict or not
        List<SqlCode> tested = new ArrayList<>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
        {
            ResourceClass resClass = entry.getKey().getParameterClass();
            SqlExpressionIntercode node = entry.getValue();

            boolean mayMiss = node.canBeNull()
                    || node.getResourceClasses().stream().anyMatch(r -> !r.isSubclassOf(resClass));
            List<Column> columns = node.getValue(relations, resClass);

            for(Column column : columns)
            {
                if(!(column instanceof ConstantColumn))
                    read.add(SqlCode.of(columnMap.getOrDefault(column, column),
                            " AS \"" + parameterAlias + declared.size() + "\""));

                declared.add(columnMap.getOrDefault(column, column).getCode());
            }

            Column value = columnMap.getOrDefault(columns.get(0), columns.get(0));

            if(mayMiss && !(value instanceof ConstantColumn))
                tested.add(SqlCode.of(value, " IS NOT NULL"));
        }

        Function function = specialization == null || specialization.getSqlProcedure() == null ?
                procedure.getSqlProcedure() : specialization.getSqlProcedure();
        List<SqlCode> arguments = specialization == null ? declared : specialization.getArguments(declared);

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");
        builder.append(function);
        builder.append(arguments.stream().collect(SqlCode.joining(", ", "(", ")")));

        builder.append(" AS \"");
        builder.append(resultVar);
        builder.append('"');

        // a node standing for the parameters or the results is held by the values passed to the parameters as declared,
        // whatever the specialisation of the call is
        if(parameterNode != null || resultNode != null)
            for(SqlCode value : read)
                builder.append(", ").append(value);

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
            builder.append(child.translate(request, scope));
            builder.append(" ) AS ");
            builder.appendAlias(scope.getInputTable());

            if(!tested.isEmpty())
                builder.append(" WHERE ").append(tested.stream().collect(SqlCode.joining(" AND ")));
        }

        return builder.toCode();
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
            result.add(new ExpressionColumn(SqlCode.of("\"", resultVar, "\""), resClass.getSqlTypes().get(0)));
        }
        else
        {
            for(Column field : fields)
            {
                if(field instanceof TableColumn)
                    result.add(new ExpressionColumn(SqlCode.of("(\"", resultVar, "\").", field), field.getType()));
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

        if(parameterNode != null)
        {
            indentInfo(builder, indent, true);
            builder.append("#parameters to ").append(parameterNode);
        }

        if(resultNode != null)
        {
            indentInfo(builder, indent, true);
            builder.append("#results to ").append(resultNode);
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

        if(!Objects.equals(parameterNode, imcode.parameterNode))
            return false;

        if(!Objects.equals(resultNode, imcode.resultNode))
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
        return Objects.hash(procedure, parameters, results, parameterNode, resultNode, child, resultClasses);
    }
}
