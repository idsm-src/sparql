package cz.iocb.chemweb.server.sparql.translator.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.ConstantLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedLiteralMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Bind;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Filter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Graph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GraphPattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GroupGraph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Minus;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Optional;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Pattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.translator.error.ErrorType;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.server.sparql.translator.sql.UsedPairedVariable.PairedClass;
import cz.iocb.chemweb.server.sparql.translator.visitor.GraphOrServiceRestriction;
import cz.iocb.chemweb.server.sparql.translator.visitor.GraphOrServiceRestriction.RestrictionType;



public class TranslateVisitor extends ElementVisitor<TranslatedSegment>
{
    private int tmpTableIndex = 0;

    private final Stack<List<DataSet>> selectDatasets = new Stack<>();
    private final Stack<GraphOrServiceRestriction> graphOrServiceRestrictions = new Stack<>();

    private final List<TranslateException> exceptions = new LinkedList<TranslateException>();
    private final List<TranslateException> warnings = new LinkedList<TranslateException>();

    private final List<QuadMapping> mappings;



    public TranslateVisitor(List<QuadMapping> mappings)
    {
        this.mappings = mappings;
    }


    @Override
    public TranslatedSegment visit(SelectQuery selectQuery)
    {
        TranslatedSegment translatedSelectClause = visitElement(selectQuery.getSelect());

        return translateSelectVariables(translatedSelectClause);
    }


    @Override
    public TranslatedSegment visit(Select select)
    {
        checkProjectionVariables(select);

        // register information about datasets used in this SELECT (sub)query
        selectDatasets.push(select.getDataSets()); // TODO: datasets are not supported

        // translate the WHERE clause
        TranslatedSegment translatedWhereClause = visitElement(select.getPattern());

        if(!select.getGroupByConditions().isEmpty())
            translatedWhereClause = translateBindInGroupBy(select.getGroupByConditions(), translatedWhereClause);

        TranslatedSegment translatedSelect = translateSelect(select, translatedWhereClause);

        // clear information about datasets of this SELECT (sub)query
        selectDatasets.pop();

        return translatedSelect;
    }


    @Override
    public TranslatedSegment visit(GroupGraph groupGraph)
    {
        TranslatedSegment translatedGroupGraphPattern = translatePatternList(groupGraph.getPatterns());

        return translatedGroupGraphPattern;
    }


    @Override
    public TranslatedSegment visit(Graph graph)
    {
        VarOrIri varOrIri = graph.getName();
        graphOrServiceRestrictions.push(new GraphOrServiceRestriction(RestrictionType.GRAPH_RESTRICTION, varOrIri));

        TranslatedSegment translatedPattern = visitElement(graph.getPattern());

        graphOrServiceRestrictions.pop();
        return translatedPattern;
    }


    @Override
    public TranslatedSegment visit(Service service)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Union union)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Values values)
    {
        return null;
    }


    @Override
    public TranslatedSegment visit(Triple triple)
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


        TranslatedSegment translatedPattern = new NoSolutionSegment();

        for(QuadMapping mapping : mappings)
        {
            if(mapping.match(graph, subject, predicate, object))
            {
                TranslatedSegment translate = translateTriple(mapping, graph, subject, predicate, object);
                translatedPattern = translateUnion(translatedPattern, translate);
            }
        }

        return translatedPattern;
    }


    @Override
    public TranslatedSegment visit(Minus minus)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public TranslatedSegment visit(Optional optional)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public TranslatedSegment visit(Bind bind)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public TranslatedSegment visit(Filter filter)
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
            else if(groupCondition.getExpression() instanceof Variable)
                groupVars.add(((Variable) groupCondition.getExpression()).getName());
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

                if(!(arg instanceof Variable))
                    return null;

                if(groupByVars.contains(((Variable) arg).getName()))
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


    /**
     * Checks whether the given function is an aggregate function (according to SPARQL 1.1).
     *
     * @param functionName Name of the SPARQL function.
     * @return true if the given function is an aggregate function.
     */
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

    private TranslatedSegment translateSelectVariables(TranslatedSegment translatedSelectClause)
    {
        UsedVariables variables = translatedSelectClause.getVariables();
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(UsedVariable variable : variables.getValues())
        {
            List<ResourceClass> classes = variable.getClasses();

            if(classes.size() == 0)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append("NULL AS \"");
                builder.append(variable.getName());
                builder.append('#');
                builder.append(ResourceClass.nullTag);
                builder.append('"');
            }
            else
            {
                List<ResourceClass> iriClasses = new ArrayList<ResourceClass>();
                List<ResourceClass> literalClasses = new ArrayList<ResourceClass>();

                for(ResourceClass resClass : classes)
                {
                    if(resClass instanceof IriClass)
                        iriClasses.add(resClass);
                    else
                        literalClasses.add(resClass);
                }

                if(!iriClasses.isEmpty())
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    if(iriClasses.size() > 1)
                        builder.append("COALESCE(");

                    for(int i = 0; i < iriClasses.size(); i++)
                    {
                        appendComma(builder, i > 0);

                        ResourceClass iriClass = iriClasses.get(i);
                        builder.append(iriClass.getSparqlValue(variable.getName()));
                    }

                    if(iriClasses.size() > 1)
                        builder.append(")");

                    builder.append(" AS \"");
                    builder.append(variable.getName());
                    builder.append('#');
                    builder.append(IriClass.iriTag);
                    builder.append('"');
                }

                for(ResourceClass literalClass : literalClasses)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(literalClass.getSparqlValue(variable.getName()));
                }
            }
        }

        if(!hasSelect)
        {
            builder.append("1 AS \"*#");
            builder.append(ResourceClass.nullTag);
            builder.append('"');
        }


        builder.append(" FROM (");
        builder.append(translatedSelectClause.getCode());
        builder.append(") AS ");
        builder.append(getTmpTableName());

        return new TranslatedSegment(builder.toString(), null);
    }


    private TranslatedSegment translateSelect(Select select, TranslatedSegment translatedWhereClause)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(select.isDistinct())
            builder.append("DISTINCT ");


        UsedVariables variables = null;
        boolean hasSelect = false;

        if(select.getProjections().isEmpty())
        {
            //TODO: check whether star can be used

            variables = translatedWhereClause.getVariables();

            for(UsedVariable variable : variables.getValues())
            {
                for(ResourceClass resClass : variable.getClasses())
                {
                    for(int i = 0; i < resClass.getPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(resClass.getSqlColumn(variable.getName(), i));
                    }
                }
            }
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

                    for(ResourceClass resClass : variable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPartsCount(); i++)
                        {
                            appendComma(builder, hasSelect);
                            hasSelect = true;

                            builder.append(resClass.getSqlColumn(varName, i));
                        }
                    }
                }
            }
        }

        if(!hasSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(translatedWhereClause.getCode());
        builder.append(" ) AS ");
        builder.append(getTmpTableName());


        if(select.getLimit() != null)
        {
            builder.append(" LIMIT ");
            builder.append(select.getLimit().toString());
        }


        if(select.getOffset() != null)
        {
            builder.append(" OFFSET ");
            builder.append(select.getOffset().toString());
        }


        return new TranslatedSegment(builder.toString(), variables);
    }


    private TranslatedSegment translateBindInGroupBy(List<GroupCondition> groupByList,
            TranslatedSegment translatedWhereClause)
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


    private TranslatedSegment translatePatternList(List<Pattern> patterns)
    {
        TranslatedSegment translatedGroupPattern = EmptySolutionSegment.get();

        LinkedList<Filter> filters = new LinkedList<>();


        for(Pattern pattern : patterns)
        {
            if(pattern instanceof Optional)
            {
                GraphPattern optionalPattern = ((Optional) pattern).getPattern();

                if(optionalPattern == null) //TODO: this possibility should be eliminated by the parser
                    continue;

                TranslatedSegment translatedPattern = null;
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
                TranslatedSegment translatedPattern = visitElement(pattern);
                translatedGroupPattern = translateJoin(translatedGroupPattern, translatedPattern);
            }
        }

        return translateFilters(filters, translatedGroupPattern);
    }


    private TranslatedSegment translateTriple(QuadMapping mapping, Node graph, Node subject, Node predicate,
            Node object)
    {
        StringBuilder builder = new StringBuilder();
        UsedVariables variables = new UsedVariables();


        builder.append("SELECT ");
        boolean hasSelect = false;

        hasSelect |= generateNodeSelect(builder, hasSelect, variables, graph, mapping.getGraph());
        hasSelect |= generateNodeSelect(builder, hasSelect, variables, subject, mapping.getSubject());
        hasSelect |= generateNodeSelect(builder, hasSelect, variables, predicate, mapping.getPredicate());
        hasSelect |= generateNodeSelect(builder, hasSelect, variables, object, mapping.getObject());

        if(!hasSelect)
            builder.append("1");

        for(UsedVariable usedVariable : variables.getValues())
        {
            if(usedVariable.getClasses().size() > 1)
            {
                UsedVariables noSolutionVariables = new UsedVariables();

                for(UsedVariable variable : variables.getValues())
                    noSolutionVariables.add(new UsedVariable(variable.getName(), true));

                return new NoSolutionSegment(noSolutionVariables);
            }
        }


        if(mapping.getTable() != null)
        {
            builder.append(" FROM ");
            builder.append(mapping.getTable());
        }


        builder.append(" WHERE ");
        boolean hasWhere = false;

        if(mapping.getCondition() != null)
        {
            hasWhere = true;
            builder.append(mapping.getCondition());
            builder.append(" ");
        }

        hasWhere |= generateNodeWhere(builder, hasWhere, graph, mapping.getGraph());
        hasWhere |= generateNodeWhere(builder, hasWhere, subject, mapping.getSubject());
        hasWhere |= generateNodeWhere(builder, hasWhere, predicate, mapping.getPredicate());
        hasWhere |= generateNodeWhere(builder, hasWhere, object, mapping.getObject());

        hasWhere |= generateNodesWhere(builder, hasWhere, graph, subject, mapping.getGraph(), mapping.getSubject());
        hasWhere |= generateNodesWhere(builder, hasWhere, graph, predicate, mapping.getGraph(), mapping.getPredicate());
        hasWhere |= generateNodesWhere(builder, hasWhere, graph, object, mapping.getGraph(), mapping.getObject());
        hasWhere |= generateNodesWhere(builder, hasWhere, subject, predicate, mapping.getSubject(),
                mapping.getPredicate());
        hasWhere |= generateNodesWhere(builder, hasWhere, subject, object, mapping.getSubject(), mapping.getObject());

        if(!hasWhere)
            builder.append("true");


        return new TranslatedSegment(builder.toString(), variables);
    }


    private boolean generateNodeSelect(StringBuilder builder, boolean hasSelect, UsedVariables variables, Node node,
            NodeMapping mapping)
    {
        if(!(node instanceof Variable))
            return false;


        Variable variable = (Variable) node;
        UsedVariable other = variables.get(variable.getName());

        if(other != null && other.containsClass(mapping.getResourceClass()))
            return false;

        if(other != null)
            other.addClass(mapping.getResourceClass());
        else
            variables.add(new UsedVariable(variable.getName(), mapping.getResourceClass(), false));

        appendComma(builder, hasSelect);

        for(int i = 0; i < mapping.getResourceClass().getPartsCount(); i++)
        {
            appendComma(builder, i > 0);

            builder.append(mapping.getSqlValueAccess(i));
            builder.append(" AS ");
            builder.append(mapping.getResourceClass().getSqlColumn(variable.getName(), i));
        }

        return true;
    }


    private boolean generateNodeWhere(StringBuilder builder, boolean hasWhere, Node node, NodeMapping mapping)
    {
        if(node instanceof Variable && !(mapping instanceof ParametrisedLiteralMapping))
            return false;

        if(mapping instanceof ConstantIriMapping || mapping instanceof ConstantLiteralMapping)
            return false;


        if(hasWhere)
            builder.append(" AND ");

        for(int i = 0; i < mapping.getResourceClass().getPartsCount(); i++)
        {
            if(i > 0)
                builder.append(" AND ");

            builder.append(mapping.getSqlValueAccess(i));

            if(node instanceof Variable)
            {
                builder.append(" IS NOT NULL ");
            }
            else
            {
                builder.append(" = ");
                builder.append(mapping.getResourceClass().getSqlValue(node, i));
            }
        }

        return true;
    }


    private boolean generateNodesWhere(StringBuilder builder, boolean hasWhere, Node node1, Node node2,
            NodeMapping map1, NodeMapping map2)
    {
        if(!(node1 instanceof Variable) || !(node2 instanceof Variable))
            return false;

        if(!node1.equals(node2))
            return false;


        if(hasWhere)
            builder.append(" AND ");

        if(map1.getResourceClass() == map2.getResourceClass())
        {
            for(int i = 0; i < map1.getResourceClass().getPartsCount(); i++)
            {
                if(i > 0)
                    builder.append(" AND ");

                builder.append(map1.getSqlValueAccess(i));
                builder.append(" = ");
                builder.append(map1.getSqlValueAccess(i));
            }
        }
        else
        {
            assert false;
        }

        return true;
    }


    private TranslatedSegment translateUnion(TranslatedSegment left, TranslatedSegment right)
    {
        if(left instanceof NoSolutionSegment)
        {
            UsedVariables variables = new UsedVariables();

            for(UsedVariable variable : right.getVariables().getValues())
                variables.add(variable);

            for(UsedVariable variable : left.getVariables().getValues())
                if(variables.get(variable.getName()) != null)
                    variables.add(variable);

            return new TranslatedSegment(right.getCode(), variables);
        }

        if(right instanceof NoSolutionSegment)
        {
            UsedVariables variables = new UsedVariables();

            for(UsedVariable variable : left.getVariables().getValues())
                variables.add(variable);

            for(UsedVariable variable : right.getVariables().getValues())
                if(variables.get(variable.getName()) != null)
                    variables.add(variable);

            return new TranslatedSegment(left.getCode(), variables);
        }

        if(left instanceof EmptySolutionSegment)
            return right;

        if(right instanceof EmptySolutionSegment)
            return left;


        ArrayList<UsedPairedVariable> pairs = pairVariables(left.getVariables(), right.getVariables());


        StringBuilder builder = new StringBuilder();


        boolean emptySelect = true;

        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getLeftVariable();

            if(leftVariable != null && !leftVariable.getClasses().isEmpty()
                    || rightVariable != null && !rightVariable.getClasses().isEmpty())
                emptySelect = false;
        }


        builder.append("SELECT ");

        if(!emptySelect)
        {
            boolean hasSelect = false;

            for(UsedPairedVariable pair : pairs)
            {
                UsedVariable leftVariable = pair.getLeftVariable();
                UsedVariable rightVariable = pair.getLeftVariable();

                String var = pair.getName();

                if((leftVariable == null || leftVariable.getClasses().isEmpty())
                        && (rightVariable == null || rightVariable.getClasses().isEmpty()))
                    continue;

                for(PairedClass pairedClass : pair.getClasses())
                {
                    ResourceClass resClass = pairedClass.getLeftClass() != null ? pairedClass.getLeftClass()
                            : pairedClass.getRightClass();

                    for(int i = 0; i < resClass.getPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(pairedClass.getLeftClass() == null)
                            builder.append("NULL AS ");

                        builder.append(resClass.getSqlColumn(var, i));
                    }
                }
            }

            assert hasSelect;
        }
        else
        {
            builder.append("1");
        }

        builder.append(" FROM (");
        builder.append(left.getCode());
        builder.append(") AS ");
        builder.append(getTmpTableName());

        builder.append(" UNION ALL ");


        builder.append("SELECT ");

        if(!emptySelect)
        {
            boolean hasSelect = false;

            for(UsedPairedVariable pair : pairs)
            {
                UsedVariable leftVariable = pair.getLeftVariable();
                UsedVariable rightVariable = pair.getLeftVariable();

                String var = pair.getName();

                if((leftVariable == null || leftVariable.getClasses().isEmpty())
                        && (rightVariable == null || rightVariable.getClasses().isEmpty()))
                    continue;

                for(PairedClass pairedClass : pair.getClasses())
                {
                    ResourceClass resClass = pairedClass.getLeftClass() != null ? pairedClass.getLeftClass()
                            : pairedClass.getRightClass();

                    for(int i = 0; i < resClass.getPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(pairedClass.getRightClass() == null)
                            builder.append("NULL AS ");

                        builder.append(resClass.getSqlColumn(var, i));
                    }
                }
            }

            assert hasSelect;
        }
        else
        {
            builder.append("1");
        }

        builder.append(" FROM (");
        builder.append(right.getCode());
        builder.append(") AS ");
        builder.append(getTmpTableName());


        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : pairs)
        {
            String varName = pair.getName();
            boolean canBeNull = pair.getLeftVariable() != null && pair.getLeftVariable().canBeNull()
                    || pair.getRightVariable() != null && pair.getRightVariable().canBeNull();

            UsedVariable newVar = new UsedVariable(varName, canBeNull);

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                    newVar.addClass(pairedClass.getRightClass());
                else
                    newVar.addClass(pairedClass.getLeftClass());
            }

            variables.add(newVar);
        }


        return new TranslatedSegment(builder.toString(), variables);
    }



    private TranslatedSegment translateJoin(TranslatedSegment left, TranslatedSegment right)
    {
        if(left instanceof NoSolutionSegment || right instanceof NoSolutionSegment)
        {
            UsedVariables variables = new UsedVariables();

            for(UsedVariable variable : left.getVariables().getValues())
                variables.add(new UsedVariable(variable.getName(), true));

            for(UsedVariable variable : right.getVariables().getValues())
                if(variables.get(variable.getName()) != null)
                    variables.add(new UsedVariable(variable.getName(), true));

            return new NoSolutionSegment(variables);
        }

        if(left instanceof EmptySolutionSegment)
            return right;

        if(right instanceof EmptySolutionSegment)
            return left;


        ArrayList<UsedPairedVariable> pairs = pairVariables(left.getVariables(), right.getVariables());
        String leftTable = getTmpTableName();
        String rightTable = getTmpTableName();

        UsedVariables variables = new UsedVariables();
        StringBuilder builder = new StringBuilder();


        builder.append("SELECT ");

        boolean hasSelect = false;
        boolean noSolution = false;

        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();


            String var = pair.getName();
            boolean canBeLeftNull = leftVariable == null ? true : leftVariable.canBeNull();
            boolean canBeRightNull = rightVariable == null ? true : rightVariable.canBeNull();

            UsedVariable variable = new UsedVariable(var, canBeLeftNull && canBeRightNull);
            variables.add(variable);

            if((leftVariable == null || leftVariable.getClasses().isEmpty())
                    && (rightVariable == null || rightVariable.getClasses().isEmpty()))
                continue;


            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() == null)
                {
                    if(leftVariable != null && !leftVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getRightClass());
                }
                else if(pairedClass.getRightClass() == null)
                {
                    if(rightVariable != null && !rightVariable.canBeNull())
                        continue;

                    variable.addClass(pairedClass.getLeftClass());
                }
                else // it is a join variable ...
                {
                    assert pairedClass.getLeftClass() == pairedClass.getRightClass();
                    variable.addClass(pairedClass.getLeftClass());
                }


                ResourceClass resClass = pairedClass.getLeftClass() != null ? pairedClass.getLeftClass()
                        : pairedClass.getRightClass();

                for(int i = 0; i < resClass.getPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    generateJoinSelectVarable(builder, leftVariable, rightVariable, leftTable, rightTable,
                            resClass.getSqlColumn(var, i));
                }
            }

            if(variable.getClasses().isEmpty())
                noSolution = true; //TODO: generate warning?
        }

        if(noSolution)
        {
            UsedVariables noSolutionVariables = new UsedVariables();

            for(UsedVariable variable : variables.getValues())
                noSolutionVariables.add(new UsedVariable(variable.getName(), true));

            return new NoSolutionSegment(noSolutionVariables);
        }


        builder.append(" FROM ");
        builder.append("(");
        builder.append(left.getCode());
        builder.append(") AS ");
        builder.append(leftTable);

        builder.append(", (");
        builder.append(right.getCode());
        builder.append(") AS ");
        builder.append(rightTable);


        builder.append(" WHERE ");

        boolean hasWhere = false;

        for(UsedPairedVariable pair : pairs)
        {
            String var = pair.getName();
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null) // it is a join variable ...
            {
                if(leftVariable.getClasses().isEmpty() || rightVariable.getClasses().isEmpty())
                    continue;

                if(hasWhere)
                    builder.append(" AND ");

                hasWhere = true;

                builder.append("(");
                boolean restricted = false;

                if(leftVariable.canBeNull())
                {
                    boolean use = false;
                    builder.append("(");

                    for(ResourceClass resClass : leftVariable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPartsCount(); i++)
                        {
                            if(use)
                                builder.append(" AND ");
                            else
                                use = true;

                            builder.append(leftTable).append('.').append(resClass.getSqlColumn(var, i));
                            builder.append(" IS NULL");
                        }
                    }

                    builder.append(")");

                    assert use;
                    restricted = true;
                }

                if(rightVariable.canBeNull())
                {
                    if(restricted)
                        builder.append(" OR ");

                    boolean use = false;
                    builder.append("(");

                    for(ResourceClass resClass : rightVariable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPartsCount(); i++)
                        {
                            if(use)
                                builder.append(" AND ");
                            else
                                use = true;

                            builder.append(rightTable).append('.').append(resClass.getSqlColumn(var, i));
                            builder.append(" IS NULL");
                        }
                    }

                    builder.append(")");

                    assert use;
                    restricted = true;
                }

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
                    {
                        assert pairedClass.getLeftClass() == pairedClass.getRightClass();
                        ResourceClass resClass = pairedClass.getLeftClass();

                        if(restricted)
                            builder.append(" OR ");

                        builder.append("(");

                        for(int i = 0; i < resClass.getPartsCount(); i++)
                        {
                            if(i > 0)
                                builder.append(" AND ");

                            builder.append(leftTable).append('.').append(resClass.getSqlColumn(var, i));
                            builder.append(" = ");
                            builder.append(rightTable).append('.').append(resClass.getSqlColumn(var, i));
                        }

                        builder.append(")");

                        restricted = true;
                    }
                }

                builder.append(")");

                assert restricted;
            }
        }

        if(!hasWhere)
            builder.append("true");

        return new TranslatedSegment(builder.toString(), variables);
    }


    private void generateJoinSelectVarable(StringBuilder builder, UsedVariable leftVariable, UsedVariable rightVariable,
            String leftTable, String rightTable, String var)
    {
        if(leftVariable == null || leftVariable.getClasses().isEmpty())
        {
            builder.append(rightTable).append('.').append(var);
        }
        else if(rightVariable == null || rightVariable.getClasses().isEmpty())
        {
            builder.append(leftTable).append('.').append(var);
        }
        else if(!leftVariable.canBeNull())
        {
            builder.append(leftTable).append('.').append(var);
        }
        else if(!rightVariable.canBeNull())
        {
            builder.append(rightTable).append('.').append(var);
        }
        else
        {
            builder.append("COALESCE(");
            builder.append(leftTable).append('.').append(var);
            builder.append(", ");
            builder.append(rightTable).append('.').append(var);
            builder.append(")");
        }

        builder.append(" AS ");
        builder.append(var);
    }


    private ArrayList<UsedPairedVariable> pairVariables(UsedVariables left, UsedVariables right)
    {
        LinkedHashSet<String> varNames = mergeVariableNames(left, right);
        ArrayList<UsedPairedVariable> pairs = new ArrayList<UsedPairedVariable>();

        for(String varName : varNames)
        {
            UsedVariable leftVar = left.get(varName);
            UsedVariable rightVar = right.get(varName);


            UsedPairedVariable paired = new UsedPairedVariable(varName, leftVar, rightVar);

            if(leftVar == null)
            {
                for(ResourceClass resClass : rightVar.getClasses())
                    paired.addClasses(null, resClass);
            }
            else if(rightVar == null)
            {
                for(ResourceClass resClass : leftVar.getClasses())
                    paired.addClasses(resClass, null);
            }
            else
            {
                LinkedHashSet<ResourceClass> classes = new LinkedHashSet<ResourceClass>();

                for(ResourceClass resClass : leftVar.getClasses())
                    classes.add(resClass);

                for(ResourceClass resClass : rightVar.getClasses())
                    classes.add(resClass);

                for(ResourceClass resClass : classes)
                {
                    ResourceClass l = leftVar.containsClass(resClass) ? resClass : null;
                    ResourceClass r = rightVar.containsClass(resClass) ? resClass : null;

                    paired.addClasses(l, r);
                }
            }

            pairs.add(paired);
        }

        return pairs;
    }


    private LinkedHashSet<String> mergeVariableNames(UsedVariables left, UsedVariables right)
    {
        LinkedHashSet<String> varNames = new LinkedHashSet<String>();

        for(UsedVariable variable : left.getValues())
            varNames.add(variable.getName());

        for(UsedVariable variable : right.getValues())
            varNames.add(variable.getName());

        return varNames;
    }


    private TranslatedSegment translateLeftJoin(TranslatedSegment translatedGroupPattern,
            TranslatedSegment translatedPattern, LinkedList<Filter> optionalFilters)
    {
        // TODO Auto-generated method stub
        return null;
    }


    private TranslatedSegment translateMinus(Minus pattern, TranslatedSegment translatedGroupPattern)
    {
        // TODO Auto-generated method stub
        return null;
    }


    private TranslatedSegment translateBind(Bind pattern, TranslatedSegment translatedGroupPattern)
    {
        // TODO Auto-generated method stub
        return null;
    }


    private TranslatedSegment translateFilters(LinkedList<Filter> filters, TranslatedSegment translatedGroupPattern)
    {
        // TODO
        return translatedGroupPattern;
    }


    private TranslatedSegment translateProcedureCall(ProcedureCallBase pattern,
            TranslatedSegment translatedGroupPattern)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**************************************************************************/

    private static void appendComma(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(", ");
    }


    private String getTmpTableName()
    {
        return "tab" + tmpTableIndex++;
    }
}
