package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.toSet;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.Range;
import cz.iocb.sparql.engine.parser.model.AskQuery;
import cz.iocb.sparql.engine.parser.model.ConstructQuery;
import cz.iocb.sparql.engine.parser.model.DataSet;
import cz.iocb.sparql.engine.parser.model.DescribeQuery;
import cz.iocb.sparql.engine.parser.model.GroupCondition;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.OrderCondition;
import cz.iocb.sparql.engine.parser.model.OrderCondition.Direction;
import cz.iocb.sparql.engine.parser.model.Projection;
import cz.iocb.sparql.engine.parser.model.Prologue;
import cz.iocb.sparql.engine.parser.model.Query;
import cz.iocb.sparql.engine.parser.model.Select;
import cz.iocb.sparql.engine.parser.model.SelectQuery;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.Expression;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.pattern.Bind;
import cz.iocb.sparql.engine.parser.model.pattern.Filter;
import cz.iocb.sparql.engine.parser.model.pattern.Graph;
import cz.iocb.sparql.engine.parser.model.pattern.GraphPattern;
import cz.iocb.sparql.engine.parser.model.pattern.GroupGraph;
import cz.iocb.sparql.engine.parser.model.pattern.Minus;
import cz.iocb.sparql.engine.parser.model.pattern.MultiProcedureCall;
import cz.iocb.sparql.engine.parser.model.pattern.Optional;
import cz.iocb.sparql.engine.parser.model.pattern.Pattern;
import cz.iocb.sparql.engine.parser.model.pattern.ProcedureCall;
import cz.iocb.sparql.engine.parser.model.pattern.ProcedureCallBase;
import cz.iocb.sparql.engine.parser.model.pattern.ProcedureCallBase.Parameter;
import cz.iocb.sparql.engine.parser.model.pattern.Service;
import cz.iocb.sparql.engine.parser.model.pattern.Union;
import cz.iocb.sparql.engine.parser.model.pattern.Values;
import cz.iocb.sparql.engine.parser.model.pattern.Values.ValuesList;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.parser.model.triple.Triple;
import cz.iocb.sparql.engine.parser.model.triple.Verb;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.imcode.SqlAggregation;
import cz.iocb.sparql.engine.translator.imcode.SqlBind;
import cz.iocb.sparql.engine.translator.imcode.SqlConstruct;
import cz.iocb.sparql.engine.translator.imcode.SqlConstruct.Template;
import cz.iocb.sparql.engine.translator.imcode.SqlDistinct;
import cz.iocb.sparql.engine.translator.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.translator.imcode.SqlFilter;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.SqlJoin;
import cz.iocb.sparql.engine.translator.imcode.SqlLeftJoin;
import cz.iocb.sparql.engine.translator.imcode.SqlMerge;
import cz.iocb.sparql.engine.translator.imcode.SqlMinus;
import cz.iocb.sparql.engine.translator.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.translator.imcode.SqlProcedureCall;
import cz.iocb.sparql.engine.translator.imcode.SqlSelect;
import cz.iocb.sparql.engine.translator.imcode.SqlServiceStub;
import cz.iocb.sparql.engine.translator.imcode.SqlUnion;
import cz.iocb.sparql.engine.translator.imcode.SqlValues;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class TranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final String variablePrefix = "@additionalvar";

    private int variableId = 0;
    private int serviceId = 0;

    private final Request request;

    private final Stack<IRI> serviceRestrictions = new Stack<>();
    private final Stack<VarOrIri> graphRestrictions = new Stack<>();

    private final SparqlDatabaseConfiguration configuration;
    private final List<UserIriClass> iriClasses;

    private HashMap<String, List<Range>> variableOccurrences;
    private List<DataSet> datasets;
    private Prologue prologue;


    public TranslateVisitor(Request request)
    {
        this.request = request;

        this.configuration = request.getConfiguration();
        this.iriClasses = configuration.getIriClasses();

        serviceRestrictions.add(configuration.getServiceIri());
        graphRestrictions.add(null);
    }


    @Override
    public SqlIntercode visit(SelectQuery selectQuery)
    {
        prologue = selectQuery.getPrologue();
        datasets = selectQuery.getSelect().getDataSets();

        Select select = selectQuery.getSelect();
        return visitElement(select);
    }


    @Override
    public SqlIntercode visit(AskQuery askQuery)
    {
        prologue = askQuery.getPrologue();
        datasets = askQuery.getSelect().getDataSets();

        Variable variable = new Variable(null, "@ask");

        SqlIntercode translatedSelect = visitElement(askQuery.getSelect());
        SqlExpressionIntercode expression = SqlExists.create(request, false, translatedSelect, new UsedVariables());
        SqlIntercode bind = SqlBind.bind(request, variable.getSqlName(), expression, SqlEmptySolution.get());

        return SqlSelect.createTopLevel(request, List.of(variable.getName()), bind);
    }


    @Override
    public SqlIntercode visit(DescribeQuery describeQuery)
    {
        prologue = describeQuery.getPrologue();
        datasets = describeQuery.getSelect().getDataSets();

        Variable subject = new Variable(null, "@subject");
        Variable predicate = new Variable(null, "@predikate");
        Variable object = new Variable(null, "@object");

        PathTranslateVisitor visitor = new PathTranslateVisitor(request, this, datasets);
        SqlIntercode select = visitElement(describeQuery.getSelect());
        List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

        for(VarOrIri resource : describeQuery.getResources())
        {
            if(resource instanceof IRI iri)
            {
                SqlExpressionIntercode expression = SqlIri.create(request, iri);

                SqlIntercode subjectPattern = visitor.translate(null, iri, predicate, object);
                subjectPattern = SqlBind.bind(request, subject.getSqlName(), expression, subjectPattern);
                unionList.add(subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, iri);
                objectPattern = SqlBind.bind(request, object.getSqlName(), expression, objectPattern);
                unionList.add(objectPattern);
            }
            else
            {
                Variable variable = (Variable) resource;
                SqlExpressionIntercode expression = SqlVariable.create(select.getVariable(variable.getSqlName()));

                SqlExpressionIntercode filter = SqlBuiltinCall.create(request, "bound", false, List.of(expression));
                SqlIntercode source = SqlFilter.filter(request, List.of(filter), select);

                SqlIntercode subjectPattern = visitor.translate(null, variable, predicate, object);
                subjectPattern = SqlJoin.join(request, source, subjectPattern);
                subjectPattern = SqlBind.bind(request, subject.getSqlName(), expression, subjectPattern);
                unionList.add(subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, variable);
                objectPattern = SqlJoin.join(request, source, objectPattern);
                objectPattern = SqlBind.bind(request, object.getSqlName(), expression, objectPattern);
                unionList.add(objectPattern);
            }
        }

        List<String> variables = List.of(subject.getSqlName(), predicate.getSqlName(), object.getSqlName());
        return SqlSelect.createTopLevel(request, variables, SqlUnion.union(request, unionList));
    }


    @Override
    public SqlIntercode visit(ConstructQuery constructQuery)
    {
        prologue = constructQuery.getPrologue();
        datasets = constructQuery.getSelect().getDataSets();

        SqlIntercode source = visitElement(constructQuery.getSelect());

        List<Template> templates = new ArrayList<Template>();

        for(Pattern pattern : constructQuery.getTemplates())
        {
            Triple triple = (Triple) pattern;
            templates.add(new Template(triple.getSubject(), (Node) triple.getPredicate(), triple.getObject()));
        }

        return SqlSelect.createTopLevel(request, SqlConstruct.getColumns(), SqlDistinct.create(request,
                SqlConstruct.construct(request, templates, source), new HashSet<>(SqlConstruct.getColumns())));
    }


    @Override
    public SqlIntercode visit(Select select)
    {
        // translate the WHERE clause
        GraphPattern pattern = select.getPattern();

        if(select.getValues() != null && !select.isInAggregateMode())
        {
            List<Pattern> patterns = new LinkedList<Pattern>();

            for(Pattern subpattern : ((GroupGraph) pattern).getPatterns())
            {
                if(subpattern instanceof ProcedureCallBase procedureCall)
                {
                    List<Variable> variables = new LinkedList<Variable>();
                    boolean[] mask = new boolean[select.getValues().getVariables().size()];

                    for(Parameter par : procedureCall.getParameters())
                    {
                        if(par.getValue() instanceof Variable variable)
                        {
                            int idx = select.getValues().getVariables().indexOf(variable);

                            if(idx != -1)
                            {
                                variables.add(variable);
                                mask[idx] = true;
                            }
                        }
                    }

                    if(!variables.isEmpty())
                    {
                        List<List<Expression>> selected = new LinkedList<List<Expression>>();
                        List<ValuesList> values = new LinkedList<ValuesList>();

                        for(ValuesList valuesList : select.getValues().getValuesLists())
                        {
                            List<Expression> stripped = new ArrayList<Expression>(variables.size());

                            for(int i = 0; i < valuesList.getValues().size(); i++)
                                if(mask[i])
                                    stripped.add(valuesList.getValues().get(i));

                            if(!selected.contains(stripped))
                            {
                                selected.add(stripped);
                                values.add(new ValuesList(stripped));
                            }
                        }

                        patterns.add(new Values(variables, values));
                    }
                }

                patterns.add(subpattern);
            }

            GroupGraph rewriten = new GroupGraph(patterns);
            rewriten.setRange(pattern.getRange());
            pattern = rewriten;
        }


        SqlIntercode translatedWhereClause = visitElement(pattern);


        // translate the GROUP BY clause
        HashSet<String> groupByVars = new HashSet<String>();

        for(GroupCondition groupBy : select.getGroupByConditions())
        {
            if(groupBy.getExpression() instanceof Variable && groupBy.getVariable() == null)
            {
                groupByVars.add(((Variable) groupBy.getExpression()).getSqlName());
            }
            else
            {
                Variable variable = groupBy.getVariable();

                if(variable == null)
                    variable = createVariable(variablePrefix);

                groupByVars.add(variable.getSqlName());

                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request,
                        translatedWhereClause.getVariables(), this);
                SqlExpressionIntercode expression = visitor.visitElement(groupBy.getExpression());

                //TODO: add optimizations based on the expression value

                translatedWhereClause = SqlBind.bind(request, variable.getSqlName(), expression, translatedWhereClause);
            }
        }


        List<Projection> projections = select.getProjections();
        List<OrderCondition> orderByConditions = select.getOrderByConditions();

        if(select.isInAggregateMode())
        {
            HashSet<String> validVars = new HashSet<String>(groupByVars);

            ExpressionAggregationRewriteVisitor rewriter = new ExpressionAggregationRewriteVisitor(this, validVars);

            List<Filter> havingConditions = select.getHavingConditions().stream()
                    .map(e -> new Filter(rewriter.visitElement(e))).toList();


            projections = new LinkedList<Projection>();

            for(Projection projection : select.getProjections())
            {
                if(projection.getExpression() == null)
                {
                    projections.add(projection);
                }
                else
                {
                    Projection rewrited = new Projection(rewriter.visitElement(projection.getExpression()),
                            projection.getVariable());
                    validVars.add(projection.getVariable().getName());
                    rewrited.setRange(projection.getRange());
                    projections.add(rewrited);
                }
            }


            orderByConditions = new LinkedList<OrderCondition>();

            for(OrderCondition condition : select.getOrderByConditions())
            {
                OrderCondition rewrited = new OrderCondition(condition.getDirection(),
                        rewriter.visitElement(condition.getExpression()));
                rewrited.setRange(condition.getRange());
                orderByConditions.add(rewrited);
            }


            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request,
                    translatedWhereClause.getVariables(), this);

            LinkedHashMap<String, SqlExpressionIntercode> aggregations = new LinkedHashMap<>();

            for(Entry<Variable, BuiltInCallExpression> entry : rewriter.getAggregations().entrySet())
                aggregations.put(entry.getKey().getSqlName(), visitor.visitElement(entry.getValue()));

            SqlIntercode intercode = SqlAggregation.aggregate(request, groupByVars, aggregations,
                    translatedWhereClause);

            translatedWhereClause = intercode;


            // translate having as filters
            translatedWhereClause = translateFilters(havingConditions, translatedWhereClause);
        }


        // translate final values clause
        if(select.getValues() != null)
            translatedWhereClause = SqlJoin.join(request, translatedWhereClause, visitElement(select.getValues()));


        // translate projection expressions
        for(Projection projection : projections)
        {
            if(projection.getExpression() != null)
            {
                Bind bind = new Bind(projection.getExpression(), projection.getVariable());
                translatedWhereClause = translateBind(bind, translatedWhereClause);
            }
        }


        // translate order by expressions
        LinkedHashMap<String, Direction> orderByVariables = new LinkedHashMap<String, Direction>();

        for(OrderCondition condition : orderByConditions)
        {
            if(condition.getExpression() instanceof Variable variable)
            {
                String varName = variable.getSqlName();

                if(translatedWhereClause.getVariables().get(varName) != null)
                    orderByVariables.put(varName, condition.getDirection());
            }
            else
            {
                Variable variable = createVariable(variablePrefix);
                String varName = variable.getSqlName();

                Bind bind = new Bind(condition.getExpression(), variable);
                translatedWhereClause = translateBind(bind, translatedWhereClause);

                if(translatedWhereClause.getVariables().get(varName) != null)
                    orderByVariables.put(varName, condition.getDirection());
            }
        }

        Set<String> variables = new HashSet<String>();

        for(Projection projection : select.getProjections())
            variables.add(projection.getVariable().getSqlName());

        if(select.isSubSelect() && getGraph() instanceof Variable variable)
            variables.add(variable.getSqlName());


        if(select.isSubSelect())
            return SqlSelect.create(request, variables, translatedWhereClause, select.isDistinct(), orderByVariables,
                    select.getOffset(), select.getLimit());

        List<String> selectVariables = select.getVariablesInScope().stream().map(v -> v.getSqlName()).toList();

        return SqlSelect.createTopLevel(request, selectVariables, translatedWhereClause, select.isDistinct(),
                orderByVariables, select.getOffset(), select.getLimit());
    }


    @Override
    public SqlIntercode visit(GroupGraph groupGraph)
    {
        SqlIntercode translatedGroupGraphPattern = translatePatternList(groupGraph.getPatterns(),
                SqlEmptySolution.get());

        return translatedGroupGraphPattern;
    }


    @Override
    public SqlIntercode visit(Graph graph)
    {
        Set<IRI> graphs = configuration.getGraphs(getService());

        if(!datasets.isEmpty())
        {
            Set<IRI> all = graphs;

            graphs = datasets.stream().filter(d -> !d.isDefault() && all.contains(d.getSourceSelector()))
                    .map(d -> d.getSourceSelector()).collect(toSet());
        }


        if(graph.getName() instanceof IRI && !graphs.contains(graph.getName()))
            return SqlNoSolution.get();


        boolean rename = false;

        if(graph.getName() instanceof Variable)
        {
            rename = new ElementVisitor<Boolean>()
            {
                @Override
                public Boolean visit(Variable variable)
                {
                    return variable.equals(graph.getName());
                }

                @Override
                protected Boolean aggregateResult(List<Boolean> results)
                {
                    return results.stream().anyMatch(e -> e != null && e);
                }
            }.visitElement(graph.getPattern());
        }


        VarOrIri graphVariable = graph.getName();

        if(rename)
            graphVariable = createVariable("@graph");


        graphRestrictions.push(graphVariable);

        SqlIntercode translatedPattern = visitElement(graph.getPattern());

        graphRestrictions.pop();

        if(rename)
            translatedPattern = SqlMerge.create(request, ((Variable) graph.getName()).getSqlName(),
                    ((Variable) graphVariable).getSqlName(), translatedPattern);


        if(graph.getName() instanceof Variable graphName)
        {
            String varName = graphName.getSqlName();
            UsedVariable variable = translatedPattern.getVariables().get(varName);

            if(variable == null || variable.canBeNull())
            {
                if(translatedPattern.isDeterministic())
                {
                    //TODO: can be ignored, if the graph variable is not required outside the graph pattern

                    List<List<Node>> values = new ArrayList<List<Node>>();

                    for(Node g : graphs)
                        values.add(List.of(g));

                    translatedPattern = SqlJoin.join(request, translatedPattern,
                            translateValues(List.of(varName), values));
                }
                else if(variable == null)
                {
                    List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

                    for(Node g : graphs)
                        unionList.add(
                                SqlBind.bind(request, varName, SqlIri.create(request, (IRI) g), translatedPattern));

                    translatedPattern = SqlUnion.union(request, unionList);
                }
                else
                {
                    List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

                    for(Node g : graphs)
                        unionList.add(SqlJoin.join(request, translatedPattern,
                                translateValues(List.of(varName), List.of(List.of(g)))));

                    translatedPattern = SqlUnion.union(request, unionList);
                }
            }
        }

        return translatedPattern;
    }


    @Override
    public SqlIntercode visit(Service service)
    {
        // handled specially as part of GroupGraph
        assert false;
        return null;
    }


    @Override
    public SqlIntercode visit(Union union)
    {
        return SqlUnion.union(request, union.getPatterns().stream().map(p -> visitElement(p)).toList());
    }


    @Override
    public SqlIntercode visit(Values values)
    {
        List<List<Node>> lines = new ArrayList<List<Node>>(values.getValuesLists().size());

        for(ValuesList list : values.getValuesLists())
        {
            List<Node> line = new ArrayList<Node>(list.getValues().size());

            for(Expression expression : list.getValues())
                line.add((Node) expression);

            lines.add(line);
        }

        return translateValues(values.getVariables().stream().map(v -> v.getSqlName()).toList(), lines);
    }


    @Override
    public SqlIntercode visit(Triple triple)
    {
        Node graph = getGraph();
        Node subject = triple.getSubject();
        Verb predicate = triple.getPredicate();
        Node object = triple.getObject();

        PathTranslateVisitor pathVisitor = new PathTranslateVisitor(request, this, datasets);
        SqlIntercode translatedPattern = pathVisitor.translate(graph, subject, predicate, object);

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


    private SqlIntercode translatePatternList(List<Pattern> patterns, SqlIntercode base)
    {
        SqlIntercode translatedGroupPattern = base;

        LinkedList<Filter> filters = new LinkedList<>();

        for(Pattern pattern : patterns)
        {
            if(pattern instanceof Optional optional)
            {
                GraphPattern optionalPattern = optional.getPattern();

                SqlIntercode translatedPattern = null;
                LinkedList<Filter> optionalFilters = new LinkedList<>();


                if(optionalPattern instanceof GroupGraph groupGraph)
                {
                    LinkedList<Pattern> optionalPatterns = new LinkedList<Pattern>();

                    for(Pattern subpattern : groupGraph.getPatterns())
                    {
                        if(subpattern instanceof Filter filter)
                            optionalFilters.add(filter);
                        else
                            optionalPatterns.add(subpattern);
                    }

                    translatedPattern = translatePatternList(optionalPatterns, SqlEmptySolution.get());
                }
                else
                {
                    translatedPattern = optionalPattern.accept(this);
                }

                translatedGroupPattern = translateLeftJoin(translatedGroupPattern, translatedPattern, optionalFilters);
            }
            else if(pattern instanceof Minus minus)
            {
                translatedGroupPattern = translateMinus(minus, translatedGroupPattern);
            }
            else if(pattern instanceof Bind bind)
            {
                translatedGroupPattern = translateBind(bind, translatedGroupPattern);
            }
            else if(pattern instanceof Filter filter)
            {
                filters.add(filter);
            }
            else if(pattern instanceof ProcedureCallBase procedureCall)
            {
                translatedGroupPattern = translateProcedureCall(procedureCall, translatedGroupPattern);
            }
            else if(pattern instanceof Service service)
            {
                translatedGroupPattern = translateFilters(filters, translatedGroupPattern);
                filters.clear();

                translatedGroupPattern = translateService(service, translatedGroupPattern);
            }
            else
            {
                SqlIntercode translatedPattern = visitElement(pattern);

                translatedGroupPattern = SqlJoin.join(request, translatedGroupPattern, translatedPattern);
            }
        }

        return translateFilters(filters, translatedGroupPattern);
    }


    private SqlIntercode translateLeftJoin(SqlIntercode translatedGroupPattern, SqlIntercode translatedPattern,
            LinkedList<Filter> optionalFilters)
    {
        List<SqlExpressionIntercode> conditions = new LinkedList<SqlExpressionIntercode>();

        if(!optionalFilters.isEmpty())
        {
            UsedVariables variables = SqlLeftJoin.getExpressionVariables(request, translatedGroupPattern.getVariables(),
                    translatedPattern.getVariables());

            for(Filter filter : optionalFilters)
            {
                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request, variables, this);
                SqlExpressionIntercode expression = SqlEffectiveBooleanValue
                        .create(visitor.visitElement(filter.getConstraint()));

                if(!expression.equals(trueValue))
                    conditions.add(expression);
            }
        }

        return SqlLeftJoin.leftJoin(request, translatedGroupPattern, translatedPattern, conditions);
    }


    private SqlIntercode translateMinus(Minus pattern, SqlIntercode translatedGroupPattern)
    {
        SqlIntercode minusPattern = visitElement(pattern.getPattern());

        return SqlMinus.minus(request, translatedGroupPattern, minusPattern);
    }


    private SqlIntercode translateBind(Bind bind, SqlIntercode translatedGroupPattern)
    {
        String variableName = bind.getVariable().getSqlName();

        ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request,
                translatedGroupPattern.getVariables(), this);
        SqlExpressionIntercode expression = visitor.visitElement(bind.getExpression());

        return SqlBind.bind(request, variableName, expression, translatedGroupPattern);
    }


    private SqlIntercode translateFilters(List<Filter> filters, SqlIntercode groupPattern)
    {
        if(filters.size() == 0)
            return groupPattern;


        List<SqlExpressionIntercode> filterExpressions = new ArrayList<SqlExpressionIntercode>(filters.size());

        for(Filter filter : filters)
        {
            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request, groupPattern.getVariables(),
                    this);
            SqlExpressionIntercode expression = SqlEffectiveBooleanValue
                    .create(visitor.visitElement(filter.getConstraint()));

            filterExpressions.add(expression);
        }

        return SqlFilter.filter(request, filterExpressions, groupPattern);
    }


    private SqlIntercode translateValues(List<String> variableNames, List<List<Node>> lines)
    {
        if(lines.size() == 0)
            return SqlNoSolution.get();


        Map<String, List<ResourceClass>> resourceClasses = new HashMap<String, List<ResourceClass>>();
        LinkedHashMap<Column, List<Column>> data = new LinkedHashMap<Column, List<Column>>();
        Map<List<Column>, Column> revData = new HashMap<List<Column>, Column>();
        UsedVariables variables = new UsedVariables();

        for(int i = 0; i < variableNames.size(); i++)
        {
            if(variables.get(variableNames.get(i)) != null)
                continue; // ignore repeated occurrences of the variable

            LinkedHashSet<ResourceClass> resClasses = new LinkedHashSet<ResourceClass>();
            boolean canBeNull = false;

            List<ResourceClass> nodeResourceClasses = new ArrayList<ResourceClass>();

            for(List<Node> line : lines)
            {
                Node node = line.get(i);

                if(node != null)
                {
                    ResourceClass resClass = request.getResourceClass(node);

                    nodeResourceClasses.add(resClass);
                    resClasses.add(resClass);
                }
                else
                {
                    nodeResourceClasses.add(null);
                    canBeNull = true;
                }
            }

            if(resClasses.size() == 0)
                continue;

            resourceClasses.put(variableNames.get(i), nodeResourceClasses);

            UsedVariable variable = new UsedVariable(variableNames.get(i), canBeNull);

            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> classColumns = new ArrayList<List<Column>>(resClass.getColumnCount());

                for(int j = 0; j < resClass.getColumnCount(); j++)
                    classColumns.add(new ArrayList<Column>(lines.size()));

                for(int k = 0; k < lines.size(); k++)
                {
                    if(resClass.equals(nodeResourceClasses.get(k)))
                    {
                        List<Column> cols = request.getColumns(resClass, lines.get(k).get(i));

                        for(int j = 0; j < resClass.getColumnCount(); j++)
                            classColumns.get(j).add(cols.get(j));
                    }
                    else
                    {
                        for(int j = 0; j < resClass.getColumnCount(); j++)
                            classColumns.get(j).add(new ConstantColumn(null, resClass.getSqlTypes().get(j)));
                    }
                }


                List<Column> tableColumns = resClass.createColumns(request.getColumnMap(), variableNames.get(i));
                List<Column> mapping = new ArrayList<Column>(resClass.getColumnCount());

                for(int j = 0; j < resClass.getColumnCount(); j++)
                {
                    List<Column> columns = classColumns.get(j);

                    if(columns.stream().allMatch(c -> c.equals(columns.get(0))))
                    {
                        mapping.add(columns.get(0));
                    }
                    else
                    {
                        Column tableColumn = revData.get(columns);

                        if(tableColumn == null)
                        {
                            tableColumn = tableColumns.get(j);
                            revData.put(columns, tableColumn);
                            data.put(tableColumn, columns);
                        }

                        mapping.add(tableColumn);
                    }
                }

                variable.addMapping(resClass, mapping);
            }

            variables.add(variable);
        }

        return SqlValues.create(variables, resourceClasses, data, lines.size());
    }


    private SqlIntercode translateProcedureCall(ProcedureCallBase procedureCallBase, SqlIntercode context)
    {
        IRI procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = configuration.getProcedures(getService())
                .get(procedureName.getValue());
        UsedVariables contextVariables = context.getVariables();


        /* process parameters */

        ExpressionTranslateVisitor translator = new ExpressionTranslateVisitor(request, contextVariables, this);

        LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameterNodes = new LinkedHashMap<>();

        for(ParameterDefinition parameter : procedureDefinition.getParameters())
            parameterNodes.put(parameter, null);

        for(Parameter parameter : procedureCallBase.getParameters())
        {
            String parameterName = parameter.getName().getValue();
            ParameterDefinition parameterDefinition = procedureDefinition.getParameter(parameterName);

            SqlExpressionIntercode value = translator.visitElement(parameter.getValue());

            parameterNodes.put(parameterDefinition, value);
        }


        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameterNodes.entrySet())
        {
            if(entry.getValue() == null)
                entry.setValue(translator.visitElement(entry.getKey().getDefaultValue()));
        }


        /* check results */

        LinkedHashMap<ResultDefinition, String> resultNodes = new LinkedHashMap<ResultDefinition, String>();

        Set<String> used = new HashSet<String>(contextVariables.getNames());
        LinkedHashMap<Variable, Node> conditions = new LinkedHashMap<Variable, Node>();

        if(procedureCallBase instanceof ProcedureCall)
        {
            /* single-result procedure call */

            ResultDefinition resultDefinition = procedureDefinition.getResult(null);
            Node result = ((ProcedureCall) procedureCallBase).getResult();

            if(!(result instanceof VariableOrBlankNode variable) || used.contains(variable.getSqlName()))
            {
                Variable fakeResult = createVariable(variablePrefix);
                conditions.put(fakeResult, result);
                result = fakeResult;
            }

            used.add(((VariableOrBlankNode) result).getSqlName());

            resultNodes.put(resultDefinition, ((VariableOrBlankNode) result).getSqlName());
        }
        else
        {
            /* multi-result procedure call */

            MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;

            for(Parameter resultParameter : multiProcedureCall.getResults())
            {
                String parameterName = resultParameter.getName().getValue();
                ResultDefinition resultDefinition = procedureDefinition.getResult(parameterName);

                Node result = resultParameter.getValue();

                if(!(result instanceof VariableOrBlankNode variable) || used.contains(variable.getSqlName()))
                {
                    Variable fakeResult = createVariable(variablePrefix);
                    conditions.put(fakeResult, result);
                    result = fakeResult;
                }

                used.add(((VariableOrBlankNode) result).getSqlName());

                resultNodes.put(resultDefinition, ((VariableOrBlankNode) result).getSqlName());
            }
        }


        SqlIntercode intercode = SqlProcedureCall.create(request, procedureDefinition, parameterNodes, resultNodes,
                context);

        for(Entry<Variable, Node> entry : conditions.entrySet())
        {
            Variable fakeResult = entry.getKey();
            Node result = entry.getValue();

            VariableOrBlankNode var = null;

            if(result instanceof Literal literal)
            {
                var = createVariable(variablePrefix);
                intercode = SqlBind.bind(request, var.getSqlName(), SqlLiteral.create(request, literal), intercode);
            }
            else if(result instanceof IRI iri)
            {
                var = createVariable(variablePrefix);
                intercode = SqlBind.bind(request, var.getSqlName(), SqlIri.create(request, iri), intercode);
            }
            else if(result instanceof VariableOrBlankNode variable)
            {
                var = variable;
            }

            intercode = SqlMerge.create(request, var.getSqlName(), fakeResult.getSqlName(), intercode);
        }

        return intercode;
    }


    private SqlIntercode translateService(Service service, SqlIntercode context)
    {
        VarOrIri name = service.getName();

        if(configuration.getServices().contains(name))
        {
            serviceRestrictions.add((IRI) name);
            graphRestrictions.add(null);

            Pattern pattern = service.getPattern();
            List<Pattern> patterns = pattern instanceof GroupGraph group ? group.getPatterns() : List.of(pattern);

            //FIXME: passing context in this way may cause conflicts with the standard
            SqlIntercode result = translatePatternList(patterns, context);

            graphRestrictions.pop();
            serviceRestrictions.pop();

            return result;
        }

        if(request.isServiceReorderEnabled())
        {
            SqlIntercode call = SqlServiceStub.create(request, name, service.getPattern(), SqlEmptySolution.get(),
                    new UserStrBlankNodeClass(--serviceId), service.isSilent());

            return SqlJoin.join(request, call, context);
        }
        else
        {
            return SqlServiceStub.create(request, name, service.getPattern(), context,
                    new UserStrBlankNodeClass(--serviceId), service.isSilent());
        }
    }


    public SqlSelect translate(Query sparqlQuery, BigInteger offset, BigInteger limit, List<String> order)
            throws SQLException, ServiceException
    {
        variableOccurrences = new HashMap<String, List<Range>>();

        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(Variable variable)
            {
                List<Range> occurrences = variableOccurrences.get(variable.getName());

                if(occurrences == null)
                {
                    occurrences = new LinkedList<Range>();
                    variableOccurrences.put(variable.getName(), occurrences);
                }

                // implicit projection variables are added with null range by parser,
                // they are ignored here due to engines that replace blank nodes by variables
                if(variable.getRange() != null)
                    occurrences.add(variable.getRange());

                return defaultResult();
            }
        }.visitElement(sparqlQuery == null ? null : sparqlQuery.getSelect().getPattern()); //FIXME: virtuoso workaround (only sparqlQuery should be used)


        try
        {
            SqlSelect imcode = (SqlSelect) visitElement(sparqlQuery);

            if(imcode == null)
                return null;

            if(offset != null || limit != null || !order.isEmpty())
                imcode = imcode.addExternalLimits(offset, limit, order);

            return imcode;
        }
        catch(ServiceRuntimeException e)
        {
            throw(ServiceException) e.getCause();
        }
        catch(SQLRuntimeException e)
        {
            throw(SQLException) e.getCause();
        }
    }


    protected Variable createVariable(String prefix)
    {
        return new Variable(null, prefix + variableId++);
    }


    public final IRI getService()
    {
        return serviceRestrictions.peek();
    }


    public final VarOrIri getGraph()
    {
        return graphRestrictions.peek();
    }


    public final List<UserIriClass> getIriClasses()
    {
        return iriClasses;
    }


    public Prologue getPrologue()
    {
        return prologue;
    }
}
