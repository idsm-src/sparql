package cz.iocb.chemweb.server.sparql.translator.sql;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Stack;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.ConstantMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Bind;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Filter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Graph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GraphPattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GroupGraph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Minus;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.MultiProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Optional;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Pattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase.Parameter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.TranslateResult;
import cz.iocb.chemweb.server.sparql.translator.error.ErrorType;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlEmptySolution;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlIntercode;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlJoin;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlNoSolution;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlProcedureCall;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlProcedureCall.ClassifiedNode;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlQuery;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlSelect;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlTableAccess;
import cz.iocb.chemweb.server.sparql.translator.sql.imcode.SqlUnion;
import cz.iocb.chemweb.server.sparql.translator.visitor.GraphOrServiceRestriction;
import cz.iocb.chemweb.server.sparql.translator.visitor.GraphOrServiceRestriction.RestrictionType;



public class TranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private final Stack<List<DataSet>> selectDatasets = new Stack<>();
    private final Stack<GraphOrServiceRestriction> graphOrServiceRestrictions = new Stack<>();

    private final List<TranslateException> exceptions = new LinkedList<TranslateException>();
    private final List<TranslateException> warnings = new LinkedList<TranslateException>();

    private final List<ResourceClass> classes;
    private final List<QuadMapping> mappings;
    private final DatabaseSchema schema;
    private final LinkedHashMap<String, ProcedureDefinition> procedures;

    private Prologue prologue;


    public TranslateVisitor(List<ResourceClass> classes, List<QuadMapping> mappings, DatabaseSchema schema,
            LinkedHashMap<String, ProcedureDefinition> procedures)
    {
        this.classes = classes;
        this.mappings = mappings;
        this.schema = schema;
        this.procedures = procedures;
    }


    @Override
    public SqlIntercode visit(SelectQuery selectQuery)
    {
        SqlIntercode translatedSelect = visitElement(selectQuery.getSelect());
        return new SqlQuery(translatedSelect);
    }


    @Override
    public SqlIntercode visit(Select select)
    {
        checkProjectionVariables(select);

        // register information about datasets used in this SELECT (sub)query
        selectDatasets.push(select.getDataSets()); // TODO: datasets are not supported

        // translate the WHERE clause
        SqlIntercode translatedWhereClause = visitElement(select.getPattern());

        if(!select.getGroupByConditions().isEmpty())
            translatedWhereClause = translateBindInGroupBy(select.getGroupByConditions(), translatedWhereClause);



        UsedVariables variables = null;

        if(select.getProjections().isEmpty())
        {
            //TODO: check whether star can be used

            variables = new UsedVariables();

            //TODO: use code that add variables in the correct order
            for(UsedVariable variable : translatedWhereClause.getVariables().getValues())
                if(!variable.getName().startsWith(BlankNode.prefix))
                    variables.add(variable);
        }
        else
        {
            variables = new UsedVariables();

            for(Projection projection : select.getProjections())
            {
                String varName = projection.getVariable().getName();

                if(projection.getExpression() != null)
                {
                    //TODO: expression translation is not implemented yet
                }
                else
                {
                    UsedVariable variable = translatedWhereClause.getVariables().get(varName);

                    if(variable == null)
                        variable = new UsedVariable(varName, true);

                    variables.add(variable);
                }
            }
        }


        SqlSelect sqlSelect = new SqlSelect(variables, translatedWhereClause);


        if(select.isDistinct())
            sqlSelect.setDistinct(true);

        if(select.getLimit() != null)
            sqlSelect.setLimit(select.getLimit());

        if(select.getOffset() != null)
            sqlSelect.setOffset(select.getOffset());


        // clear information about datasets of this SELECT (sub)query
        selectDatasets.pop();

        return sqlSelect;
    }


    @Override
    public SqlIntercode visit(GroupGraph groupGraph)
    {
        SqlIntercode translatedGroupGraphPattern = translatePatternList(groupGraph.getPatterns());

        return translatedGroupGraphPattern;
    }


    @Override
    public SqlIntercode visit(Graph graph)
    {
        VarOrIri varOrIri = graph.getName();
        graphOrServiceRestrictions.push(new GraphOrServiceRestriction(RestrictionType.GRAPH_RESTRICTION, varOrIri));

        SqlIntercode translatedPattern = visitElement(graph.getPattern());

        graphOrServiceRestrictions.pop();
        return translatedPattern;
    }


    @Override
    public SqlIntercode visit(Service service)
    {
        return null;
    }


    @Override
    public SqlIntercode visit(Union union)
    {
        return null;
    }


    @Override
    public SqlIntercode visit(Values values)
    {
        return null;
    }


    @Override
    public SqlIntercode visit(Triple triple)
    {
        if(!(triple.getPredicate() instanceof Node))
            return null; //TODO: paths are not yet implemented


        Node graph = null;
        Node subject = triple.getSubject();
        Node predicate = (Node) triple.getPredicate();
        Node object = triple.getObject();


        if(!graphOrServiceRestrictions.isEmpty())
        {
            GraphOrServiceRestriction restriction = graphOrServiceRestrictions.peek();

            if(restriction.isRestrictionType(RestrictionType.GRAPH_RESTRICTION))
                graph = restriction.getVarOrIri();
            else
                assert false; //TODO: services are not supported
        }


        SqlIntercode translatedPattern = null;

        for(QuadMapping mapping : mappings)
        {
            if(mapping.match(graph, subject, predicate, object))
            {
                SqlTableAccess translated = new SqlTableAccess(mapping.getTable(), mapping.getCondition());

                processNodeMapping(translated, graph, mapping.getGraph());
                processNodeMapping(translated, subject, mapping.getSubject());
                processNodeMapping(translated, predicate, mapping.getPredicate());
                processNodeMapping(translated, object, mapping.getObject());

                for(UsedVariable usedVariable : translated.getVariables().getValues())
                    if(usedVariable.getClasses().size() > 1)
                        continue;

                processNodeCondition(translated, graph, mapping.getGraph());
                processNodeCondition(translated, subject, mapping.getSubject());
                processNodeCondition(translated, predicate, mapping.getPredicate());
                processNodeCondition(translated, object, mapping.getObject());

                if(processNodesCondition(translated, graph, subject, mapping.getGraph(), mapping.getSubject()))
                    continue;

                if(processNodesCondition(translated, graph, predicate, mapping.getGraph(), mapping.getPredicate()))
                    continue;

                if(processNodesCondition(translated, graph, object, mapping.getGraph(), mapping.getObject()))
                    continue;

                if(processNodesCondition(translated, subject, predicate, mapping.getSubject(), mapping.getPredicate()))
                    continue;

                if(processNodesCondition(translated, subject, object, mapping.getSubject(), mapping.getObject()))
                    continue;


                if(translatedPattern == null)
                    translatedPattern = translated;
                else
                    translatedPattern = SqlUnion.union(translatedPattern, translated);
            }
        }


        if(translatedPattern == null)
        {
            UsedVariables noSolutionVariables = new UsedVariables();

            if(graph instanceof VariableOrBlankNode)
                noSolutionVariables.add(new UsedVariable(((VariableOrBlankNode) graph).getName(), true));

            if(subject instanceof VariableOrBlankNode)
                noSolutionVariables.add(new UsedVariable(((VariableOrBlankNode) subject).getName(), true));

            if(predicate instanceof VariableOrBlankNode)
                noSolutionVariables.add(new UsedVariable(((VariableOrBlankNode) predicate).getName(), true));

            if(object instanceof VariableOrBlankNode)
                noSolutionVariables.add(new UsedVariable(((VariableOrBlankNode) object).getName(), true));

            return new SqlNoSolution(noSolutionVariables);
        }


        return translatedPattern;
    }


    @Override
    public SqlIntercode visit(Minus minus)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public SqlIntercode visit(Optional optional)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public SqlIntercode visit(Bind bind)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public SqlIntercode visit(Filter filter)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }

    /**************************************************************************/

    private void checkProjectionVariables(Select select)
    {
        if(!select.getGroupByConditions().isEmpty() && select.getProjections().isEmpty())
            exceptions.add(new TranslateException(ErrorType.invalidProjection, select.getRange()));

        if(!isInAggregateMode(select))
            return;

        HashSet<String> groupVars = new HashSet<>();

        for(GroupCondition groupCondition : select.getGroupByConditions())
        {
            if(groupCondition.getVariable() != null)
                groupVars.add(groupCondition.getVariable().getName());
            else if(groupCondition.getExpression() instanceof VariableOrBlankNode)
                groupVars.add(((VariableOrBlankNode) groupCondition.getExpression()).getName());
        }

        for(Expression havingCondition : select.getHavingConditions())
            checkExpressionForGroupedSolutions(havingCondition, groupVars);

        HashSet<String> vars = new HashSet<>();
        for(Projection projection : select.getProjections())
        {
            if(vars.contains(projection.getVariable().getName()))
                exceptions.add(new TranslateException(ErrorType.repeatOfProjectionVariable,
                        projection.getVariable().getRange(), projection.getVariable().toString()));

            vars.add(projection.getVariable().getName());

            if(projection.getExpression() != null)
                checkExpressionForGroupedSolutions(projection.getExpression(), groupVars);
            else
                checkExpressionForGroupedSolutions(projection.getVariable(), groupVars);
        }

        for(OrderCondition orderCondition : select.getOrderByConditions())
            checkExpressionForGroupedSolutions(orderCondition.getExpression(), groupVars);
    }


    private boolean isInAggregateMode(Select select)
    {
        // check whether aggregateMode is explicit
        if(!select.getGroupByConditions().isEmpty() || !select.getHavingConditions().isEmpty())
            return true;

        // check whether aggregateMode is implicit
        for(Projection projection : select.getProjections())
            if(projection.getExpression() != null && containsAggregateFunction(projection.getExpression()))
                return true;

        for(OrderCondition orderCondition : select.getOrderByConditions())
            if(containsAggregateFunction(orderCondition.getExpression()))
                return true;

        return false;
    }


    private void checkExpressionForGroupedSolutions(Expression expresion, HashSet<String> groupByVars)
    {
        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(BuiltInCallExpression func)
            {
                if(!isAggregateFunction(func.getFunctionName()) || func.getArguments().isEmpty())
                    return null;

                Expression arg = func.getArguments().get(0);

                if(!(arg instanceof VariableOrBlankNode))
                    return null;

                if(groupByVars.contains(((VariableOrBlankNode) arg).getName()))
                    exceptions.add(new TranslateException(ErrorType.invalidVariableInAggregate, arg.getRange(),
                            arg.toString()));

                return null;
            }

            @Override
            public Void visit(Variable var)
            {
                if(!groupByVars.contains(var.getName()))
                    exceptions.add(new TranslateException(ErrorType.invalidVariableOutsideAggregate, var.getRange(),
                            var.toString()));

                return null;
            }

            @Override
            public Void visit(GroupGraph expr)
            {
                return null;
            }

            @Override
            public Void visit(Select expr)
            {
                return null;
            }
        }.visitElement(expresion);
    }


    private void checkExpressionForUnGroupedSolutions(Expression expresion)
    {
        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(BuiltInCallExpression call)
            {
                if(isAggregateFunction(call.getFunctionName()))
                    exceptions.add(new TranslateException(ErrorType.invalidContextOfAggregate, call.getRange()));

                return null;
            }

            @Override
            public Void visit(GroupGraph expr)
            {
                return null;
            }

            @Override
            public Void visit(Select expr)
            {
                return null;
            }
        }.visitElement(expresion);
    }


    private boolean containsAggregateFunction(Expression expression)
    {
        if(expression == null)
            return false;

        Boolean ret = new ElementVisitor<Boolean>()
        {
            @Override
            public Boolean visit(BuiltInCallExpression call)
            {
                return isAggregateFunction(call.getFunctionName());
            }

            @Override
            public Boolean visit(GroupGraph expr)
            {
                return false;
            }

            @Override
            public Boolean visit(Select expr)
            {
                return false;
            }

            @Override
            protected Boolean aggregateResult(List<Boolean> results)
            {
                return results.stream().anyMatch(x -> (x != null && x));
            }

        }.visitElement(expression);

        return ret != null && ret;
    }


    private boolean isAggregateFunction(String functionName)
    {
        functionName = functionName.toUpperCase(Locale.US);

        switch(functionName)
        {
            // aggregate functions according to SPARQL 1.1
            case "COUNT":
            case "SUM":
            case "MIN":
            case "MAX":
            case "AVG":
            case "GROUP_CONCAT":
            case "SAMPLE":
                return true;
        }

        return false;
    }

    /**************************************************************************/

    private SqlIntercode translateBindInGroupBy(List<GroupCondition> groupByList, SqlIntercode translatedWhereClause)
    {
        for(GroupCondition groupBy : groupByList)
        {
            if(groupBy.getVariable() != null)
            {
                Bind bind = new Bind(groupBy.getExpression(), groupBy.getVariable());
                bind.setRange(groupBy.getRange());

                translatedWhereClause = translateBind(bind, translatedWhereClause);
            }

            if(groupBy.getExpression() != null)
                checkExpressionForUnGroupedSolutions(groupBy.getExpression());
        }

        return translatedWhereClause;
    }


    private SqlIntercode translatePatternList(List<Pattern> patterns)
    {
        SqlIntercode translatedGroupPattern = new SqlEmptySolution();

        LinkedList<Filter> filters = new LinkedList<>();


        for(Pattern pattern : patterns)
        {
            if(pattern instanceof Optional)
            {
                GraphPattern optionalPattern = ((Optional) pattern).getPattern();

                if(optionalPattern == null) //TODO: this possibility should be eliminated by the parser
                    continue;

                SqlIntercode translatedPattern = null;
                LinkedList<Filter> optionalFilters = new LinkedList<>();


                if(optionalPattern instanceof GroupGraph)
                {
                    LinkedList<Pattern> optionalPatterns = new LinkedList<Pattern>();

                    for(Pattern subpattern : ((GroupGraph) optionalPattern).getPatterns())
                    {
                        if(subpattern instanceof Filter)
                            optionalFilters.add((Filter) subpattern);
                        else
                            optionalPatterns.add(subpattern);
                    }

                    translatedPattern = translatePatternList(optionalPatterns);
                }
                else
                {
                    translatedPattern = optionalPattern.accept(this);
                }

                translatedGroupPattern = translateLeftJoin(translatedGroupPattern, translatedPattern, optionalFilters);
            }
            else if(pattern instanceof Minus)
            {
                translatedGroupPattern = translateMinus((Minus) pattern, translatedGroupPattern);
            }
            else if(pattern instanceof Bind)
            {
                translatedGroupPattern = translateBind((Bind) pattern, translatedGroupPattern);
            }
            else if(pattern instanceof Filter)
            {
                filters.add((Filter) pattern);
            }
            else if(pattern instanceof ProcedureCallBase)
            {
                translatedGroupPattern = translateProcedureCall((ProcedureCallBase) pattern, translatedGroupPattern);
            }
            else
            {
                SqlIntercode translatedPattern = visitElement(pattern);

                if(translatedPattern == null)
                    System.err.println(pattern.getClass().getCanonicalName());

                translatedGroupPattern = SqlJoin.join(schema, translatedGroupPattern, translatedPattern);
            }
        }

        return translateFilters(filters, translatedGroupPattern);
    }


    private void processNodeMapping(SqlTableAccess translated, Node node, NodeMapping mapping)
    {
        if(!(node instanceof VariableOrBlankNode))
            return;

        translated.addMapping(((VariableOrBlankNode) node).getName(), mapping);
    }


    private void processNodeCondition(SqlTableAccess translated, Node node, NodeMapping mapping)
    {
        if(node instanceof VariableOrBlankNode && mapping instanceof ParametrisedLiteralMapping)
            translated.addNotNullCondition((ParametrisedLiteralMapping) mapping);

        if(!(node instanceof VariableOrBlankNode) && mapping instanceof ParametrisedMapping)
            translated.addValueCondition(node, (ParametrisedMapping) mapping);
    }


    private boolean processNodesCondition(SqlTableAccess translated, Node node1, Node node2, NodeMapping map1,
            NodeMapping map2)
    {
        if(!(node1 instanceof VariableOrBlankNode) || !(node2 instanceof VariableOrBlankNode))
            return false;

        if(!node1.equals(node2))
            return false;

        if(map1 instanceof ConstantMapping && map2 instanceof ConstantMapping)
        {
            if(map1.equals(map2))
                return false;
            else
                return true;
        }

        if(!map1.equals(map2))
            translated.addEqualityCondition(map1, map2);

        return false;
    }


    private SqlIntercode translateLeftJoin(SqlIntercode translatedGroupPattern, SqlIntercode translatedPattern,
            LinkedList<Filter> optionalFilters)
    {
        // TODO
        return null;
    }


    private SqlIntercode translateMinus(Minus pattern, SqlIntercode translatedGroupPattern)
    {
        // TODO
        return null;
    }


    private SqlIntercode translateBind(Bind pattern, SqlIntercode translatedGroupPattern)
    {
        // TODO
        return null;
    }


    private SqlIntercode translateFilters(LinkedList<Filter> filters, SqlIntercode translatedGroupPattern)
    {
        // TODO
        return translatedGroupPattern;
    }


    private SqlIntercode translateProcedureCall(ProcedureCallBase procedureCallBase, SqlIntercode context)
    {
        IRI procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = procedures.get(procedureName.getUri().toString());
        UsedVariables contextVariables = context.getVariables();


        /* check graph */

        if(!graphOrServiceRestrictions.isEmpty())
        {
            GraphOrServiceRestriction restriction = graphOrServiceRestrictions.peek();

            if(restriction.isRestrictionType(RestrictionType.GRAPH_RESTRICTION))
            {
                exceptions.add(new TranslateException(ErrorType.procedureCallInsideGraph,
                        procedureCallBase.getProcedure().getRange(), procedureName.toString(prologue)));
            }
        }


        /* check paramaters */

        LinkedHashMap<ParameterDefinition, ClassifiedNode> parameterNodes = new LinkedHashMap<ParameterDefinition, ClassifiedNode>();

        for(ParameterDefinition parameter : procedureDefinition.getParameters())
            parameterNodes.put(parameter, null);


        for(ProcedureCall.Parameter parameter : procedureCallBase.getParameters())
        {
            String parameterName = parameter.getName().getUri().toString();
            ParameterDefinition parameterDefinition = procedureDefinition.getParameter(parameterName);

            if(parameterDefinition == null)
            {
                exceptions.add(new TranslateException(ErrorType.invalidParameterPredicate,
                        parameter.getName().getRange(), parameter.getName().toString(prologue),
                        procedureCallBase.getProcedure().toString(prologue)));
            }
            else if(parameterNodes.get(parameterDefinition) != null)
            {
                exceptions.add(new TranslateException(ErrorType.repeatOfParameterPredicate,
                        parameter.getName().getRange(), parameter.getName().toString(prologue)));
            }
            else
            {
                Node value = parameter.getValue();
                ResourceClass valueClass = null;

                if(value instanceof VariableOrBlankNode)
                {
                    String variableName = ((VariableOrBlankNode) value).getName();
                    UsedVariable variable = contextVariables.get(variableName);

                    if(variable == null)
                    {
                        if(value instanceof Variable)
                            exceptions.add(new TranslateException(ErrorType.unboundedVariableParameterValue,
                                    value.getRange(), parameter.getName().toString(prologue), variableName));
                        else if(value instanceof BlankNode)
                            exceptions.add(new TranslateException(ErrorType.unboundedBlankNodeParameterValue,
                                    value.getRange(), parameter.getName().toString(prologue)));
                    }
                }
                else
                {
                    valueClass = classes.stream().filter(c -> c.match(value)).findAny().orElse(null);
                }

                parameterNodes.put(parameterDefinition, new ClassifiedNode(value, valueClass));
            }
        }


        for(Entry<ParameterDefinition, ClassifiedNode> entry : parameterNodes.entrySet())
        {
            ClassifiedNode parameterValue = entry.getValue();

            if(parameterValue == null)
            {
                ParameterDefinition parameterDefinition = entry.getKey();

                if(parameterDefinition.getDefaultValue() != null)
                    entry.setValue(new ClassifiedNode(parameterDefinition.getDefaultValue(),
                            parameterDefinition.getParameterClass()));
                else
                    exceptions.add(new TranslateException(ErrorType.missingParameterPredicate,
                            procedureCallBase.getProcedure().getRange(),
                            new IRI(parameterDefinition.getParamName()).toString(prologue),
                            procedureCallBase.getProcedure().toString(prologue)));
            }
        }


        /* check results */

        LinkedHashMap<ResultDefinition, ClassifiedNode> resultNodes = new LinkedHashMap<ResultDefinition, ClassifiedNode>();

        for(ResultDefinition result : procedureDefinition.getResults())
            resultNodes.put(result, null);


        if(procedureCallBase instanceof ProcedureCall)
        {
            /* single-result procedure call */

            ResultDefinition resultDefinition = procedureDefinition.getResult(null);

            Node result = ((ProcedureCall) procedureCallBase).getResult();
            ResourceClass resultClass = null;

            if(!(result instanceof VariableOrBlankNode))
                resultClass = classes.stream().filter(c -> c.match(result)).findAny().orElse(null);

            resultNodes.put(resultDefinition, new ClassifiedNode(result, resultClass));
        }
        else
        {
            /* multi-result procedure call */

            MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;

            for(Parameter result : multiProcedureCall.getResults())
            {
                String parameterName = result.getName().getUri().toString();
                ResultDefinition resultDefinition = procedureDefinition.getResult(parameterName);

                if(resultDefinition == null)
                {
                    exceptions.add(new TranslateException(ErrorType.invalidResultPredicate, result.getName().getRange(),
                            result.getName().toString(prologue), procedureCallBase.getProcedure().toString(prologue)));

                }
                else if(resultNodes.get(resultDefinition) != null)
                {
                    exceptions.add(new TranslateException(ErrorType.repeatOfResultPredicate,
                            result.getName().getRange(), result.getName().toString(prologue)));
                }
                else
                {
                    ResourceClass resultClass = null;

                    if(!(result instanceof VariableOrBlankNode))
                        resultClass = classes.stream().filter(c -> c.match(result.getValue())).findAny().orElse(null);

                    resultNodes.put(resultDefinition, new ClassifiedNode(result.getValue(), resultClass));
                }
            }
        }


        return SqlProcedureCall.create(procedureDefinition, parameterNodes, resultNodes, context);
    }


    public String translate(SelectQuery sparqlQuery) throws TranslateExceptions
    {
        SqlIntercode code = visitElement(sparqlQuery);

        if(!exceptions.isEmpty())
            throw new TranslateExceptions(exceptions);

        return code.translate();
    }


    public TranslateResult tryTranslate(SelectQuery sparqlQuery)
    {
        visitElement(sparqlQuery);
        return new TranslateResult(null, exceptions, warnings);
    }
}
