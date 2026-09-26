package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.PREDICATE;
import static cz.iocb.sparql.engine.imcode.SqlConstruct.ConstructColumn.SUBJECT;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.translator.TermGenerator.getIri;
import static cz.iocb.sparql.engine.translator.TermGenerator.getLiteral;
import static cz.iocb.sparql.engine.translator.TermGenerator.getTerm;
import static cz.iocb.sparql.engine.translator.TermGenerator.getVariable;
import static java.util.stream.Collectors.toMap;
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
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.imcode.SqlAggregation;
import cz.iocb.sparql.engine.imcode.SqlBind;
import cz.iocb.sparql.engine.imcode.SqlConstruct;
import cz.iocb.sparql.engine.imcode.SqlConstruct.BlankNodeTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.IriTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.LiteralTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.RdfTermTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.Template;
import cz.iocb.sparql.engine.imcode.SqlConstruct.VariableTemplate;
import cz.iocb.sparql.engine.imcode.SqlDistinct;
import cz.iocb.sparql.engine.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.imcode.SqlFilter;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlJoin;
import cz.iocb.sparql.engine.imcode.SqlLateralJoin;
import cz.iocb.sparql.engine.imcode.SqlLeftJoin;
import cz.iocb.sparql.engine.imcode.SqlMerge;
import cz.iocb.sparql.engine.imcode.SqlMinus;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.imcode.SqlProcedureCall;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.imcode.SqlServiceStub;
import cz.iocb.sparql.engine.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.imcode.SqlUnion;
import cz.iocb.sparql.engine.imcode.SqlValues;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.sparql.engine.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.model.AskQuery;
import cz.iocb.sparql.engine.model.ConstructQuery;
import cz.iocb.sparql.engine.model.DataSet;
import cz.iocb.sparql.engine.model.DescribeQuery;
import cz.iocb.sparql.engine.model.GroupCondition;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.OrderCondition;
import cz.iocb.sparql.engine.model.OrderCondition.Direction;
import cz.iocb.sparql.engine.model.Projection;
import cz.iocb.sparql.engine.model.Prologue;
import cz.iocb.sparql.engine.model.Query;
import cz.iocb.sparql.engine.model.Select;
import cz.iocb.sparql.engine.model.SelectQuery;
import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.model.base.Range;
import cz.iocb.sparql.engine.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.expression.LiteralNode;
import cz.iocb.sparql.engine.model.pattern.Bind;
import cz.iocb.sparql.engine.model.pattern.Filter;
import cz.iocb.sparql.engine.model.pattern.Graph;
import cz.iocb.sparql.engine.model.pattern.GraphPattern;
import cz.iocb.sparql.engine.model.pattern.GroupGraph;
import cz.iocb.sparql.engine.model.pattern.Minus;
import cz.iocb.sparql.engine.model.pattern.MultiProcedureCall;
import cz.iocb.sparql.engine.model.pattern.Optional;
import cz.iocb.sparql.engine.model.pattern.Pattern;
import cz.iocb.sparql.engine.model.pattern.ProcedureCall;
import cz.iocb.sparql.engine.model.pattern.ProcedureCallBase;
import cz.iocb.sparql.engine.model.pattern.ProcedureCallBase.Parameter;
import cz.iocb.sparql.engine.model.pattern.Service;
import cz.iocb.sparql.engine.model.pattern.Union;
import cz.iocb.sparql.engine.model.pattern.Values;
import cz.iocb.sparql.engine.model.pattern.Values.ValuesList;
import cz.iocb.sparql.engine.model.triple.BlankNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.triple.Triple;
import cz.iocb.sparql.engine.model.triple.Verb;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Translates the AST of a query into intermediate code ({@link SqlIntercode}). It applies the SPARQL algebra of group
 * graph patterns (joins, left joins for OPTIONAL, MINUS, filters, binds, values, sub-selects), rewrites grouped selects
 * into aggregations, handles GRAPH and SERVICE scoping, procedure calls and dataset clauses, and finally wraps the
 * result in a {@link SqlSelect}.
 */
public class TranslateVisitor extends ElementVisitor<SqlIntercode>
{
    /**
     * Name prefix of the fresh variables.
     */
    private static final String variablePrefix = "@additionalvar";

    /**
     * Counter of fresh variables.
     */
    private int variableId = 0;

    /**
     * Counter of SERVICE calls, giving each its blank node segment.
     */
    private int serviceId = 0;

    /**
     * Current request.
     */
    private final Request request;

    /**
     * Enclosing services, innermost last (the own service at the bottom).
     */
    private final Stack<Iri> serviceRestrictions = new Stack<>();

    /**
     * Enclosing graphs, innermost last (null for the default graph).
     */
    private final Stack<RdfTerm> graphRestrictions = new Stack<>();

    /**
     * Configuration of the endpoint.
     */
    private final SparqlDatabaseConfiguration configuration;

    /**
     * User IRI classes of the configuration.
     */
    private final List<UserIriClass> iriClasses;

    /**
     * Source ranges of every occurrence of each variable name in the WHERE clause.
     */
    private Map<String, List<Range>> variableOccurrences;

    /**
     * Quad mappings in effect after applying the dataset clauses.
     */
    private List<QuadMapping> mappings;

    /**
     * Prologue of the query being translated.
     */
    private Prologue prologue;


    /**
     * Creates the translator for the request.
     *
     * @param request the current request
     */
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

        setDatasets(selectQuery.getSelect().getDataSets());

        Select select = selectQuery.getSelect();
        return visitElement(select);
    }


    @Override
    public SqlIntercode visit(AskQuery askQuery)
    {
        prologue = askQuery.getPrologue();

        setDatasets(askQuery.getSelect().getDataSets());

        Variable variable = new Variable("@ask");

        SqlIntercode translatedSelect = visitElement(askQuery.getSelect());
        SqlExpressionIntercode expression = SqlExists.create(request, false, translatedSelect, new VariableBindings());
        SqlIntercode bind = SqlBind.bind(request, variable, expression, SqlEmptySolution.get());

        return SqlSelect.createTopLevel(request, List.of(variable), bind);
    }


    @Override
    public SqlIntercode visit(DescribeQuery describeQuery)
    {
        prologue = describeQuery.getPrologue();

        setDatasets(describeQuery.getSelect().getDataSets());

        Variable subject = new Variable("@subject");
        VariableNode predicate = new VariableNode("@predikate");
        Variable object = new Variable("@object");

        PathTranslateVisitor visitor = new PathTranslateVisitor(request, this, mappings);
        SqlIntercode select = visitElement(describeQuery.getSelect());
        List<SqlIntercode> unionList = new ArrayList<>();

        for(VarOrIri resource : describeQuery.getResources())
        {
            if(resource instanceof IriNode iriNode)
            {
                Iri iri = getIri(iriNode);
                SqlExpressionIntercode expression = SqlIri.create(request, iri);

                SqlIntercode subjectPattern = visitor.translate(null, iri, predicate, object);
                subjectPattern = SqlBind.bind(request, subject, expression, subjectPattern);
                unionList.add(subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, iri);
                objectPattern = SqlBind.bind(request, object, expression, objectPattern);
                unionList.add(objectPattern);
            }
            else
            {
                Variable variable = getVariable((VariableNode) resource);
                SqlExpressionIntercode expression = SqlVariable.create(select.getVariable(variable));

                SqlExpressionIntercode filter = SqlBuiltinCall.create(request, "bound", false, List.of(expression));
                SqlIntercode source = SqlFilter.filter(request, List.of(filter), select);

                SqlIntercode subjectPattern = visitor.translate(null, variable, predicate, object);
                subjectPattern = SqlJoin.join(request, source, subjectPattern);
                subjectPattern = SqlBind.bind(request, subject, expression, subjectPattern);
                unionList.add(subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, variable);
                objectPattern = SqlJoin.join(request, source, objectPattern);
                objectPattern = SqlBind.bind(request, object, expression, objectPattern);
                unionList.add(objectPattern);
            }
        }

        List<Variable> variables = List.of(subject, getVariable(predicate), object);
        return SqlSelect.createTopLevel(request, variables, SqlUnion.union(request, unionList));
    }


    @Override
    public SqlIntercode visit(ConstructQuery constructQuery)
    {
        prologue = constructQuery.getPrologue();

        setDatasets(constructQuery.getSelect().getDataSets());

        SqlIntercode source = visitElement(constructQuery.getSelect());

        List<Template> templates = new ArrayList<>();

        for(Pattern pattern : constructQuery.getTemplates())
        {
            Triple triple = (Triple) pattern;
            templates.add(new Template(createTemplate(triple.getSubject()),
                    createTemplate((Node) triple.getPredicate()), createTemplate(triple.getObject())));
        }

        SqlIntercode construct = SqlConstruct.construct(request, templates, source);

        SqlIntercode filter = SqlFilter.filter(request,
                List.of(SqlBuiltinCall.create(request, "isiri", false,
                        List.of(SqlVariable.create(construct.getVariable(PREDICATE.getVariable())))),
                        SqlUnaryLogical.create(SqlBuiltinCall.create(request, "isliteral", false,
                                List.of(SqlVariable.create(construct.getVariable(SUBJECT.getVariable())))))),
                construct);

        return SqlSelect.createTopLevel(request, SqlConstruct.getColumns(),
                SqlDistinct.create(request, filter, new HashSet<>(SqlConstruct.getColumns())));
    }


    /**
     * Template position for a node of a CONSTRUCT template.
     *
     * @param node the template node
     * @return template position for a node of a CONSTRUCT template
     */
    private RdfTermTemplate<?> createTemplate(Node node)
    {
        return switch(node)
        {
            case IriNode iri -> new IriTemplate(getIri(iri));
            case LiteralNode literal -> new LiteralTemplate(getLiteral(literal));
            case VariableNode var -> new VariableTemplate(getVariable(var));
            case BlankNode bnode -> new BlankNodeTemplate(bnode.getName());
            default -> throw new IllegalArgumentException();
        };
    }


    @Override
    public SqlIntercode visit(Select select)
    {
        // translate the WHERE clause
        GraphPattern pattern = select.getPattern();

        if(select.getValues() != null && !select.isInAggregateMode())
        {
            List<Pattern> patterns = new LinkedList<>();

            for(Pattern subpattern : ((GroupGraph) pattern).getPatterns())
            {
                if(subpattern instanceof ProcedureCallBase procedureCall)
                {
                    List<VariableNode> variables = new LinkedList<>();
                    boolean[] mask = new boolean[select.getValues().getVariables().size()];

                    for(Parameter par : procedureCall.getParameters())
                    {
                        if(par.getValue() instanceof VariableNode variable)
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
                        List<List<Expression>> selected = new LinkedList<>();
                        List<ValuesList> values = new LinkedList<>();

                        for(ValuesList valuesList : select.getValues().getValuesLists())
                        {
                            List<Expression> stripped = new ArrayList<>(variables.size());

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
        Set<Variable> groupByVars = new HashSet<>();
        Set<VariableNode> validVars = new HashSet<>();

        for(GroupCondition groupBy : select.getGroupByConditions())
        {
            if(groupBy.getExpression() instanceof VariableNode varNode && groupBy.getVariable() == null)
            {
                groupByVars.add(getVariable(varNode));
                validVars.add(varNode);
            }
            else
            {
                VariableNode varNode = groupBy.getVariable();
                Variable var = varNode != null ? getVariable(varNode) : createVariable(variablePrefix);

                groupByVars.add(var);

                if(varNode != null)
                    validVars.add(varNode);

                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request,
                        translatedWhereClause.getVariableBindings(), this);
                SqlExpressionIntercode expression = visitor.visitElement(groupBy.getExpression());

                //TODO: add optimizations based on the expression value

                translatedWhereClause = SqlBind.bind(request, var, expression, translatedWhereClause);
            }
        }


        List<Projection> projections = select.getProjections();
        List<OrderCondition> orderByConditions = select.getOrderByConditions();

        if(select.isInAggregateMode())
        {
            Set<VariableNode> scopeVars = select.getPattern().getVariablesInScope().stream()
                    .filter(v -> !validVars.contains(v)).collect(toSet());

            ExpressionAggregationRewriteVisitor rewriter = new ExpressionAggregationRewriteVisitor(this, scopeVars,
                    validVars);

            List<Filter> havingConditions = select.getHavingConditions().stream()
                    .map(e -> new Filter(rewriter.visitElement(e))).toList();


            projections = new LinkedList<>();

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
                    validVars.add(projection.getVariable());
                    rewrited.setRange(projection.getRange());
                    projections.add(rewrited);
                }
            }


            orderByConditions = new LinkedList<>();

            for(OrderCondition condition : select.getOrderByConditions())
            {
                OrderCondition rewrited = new OrderCondition(condition.getDirection(),
                        rewriter.visitElement(condition.getExpression()));
                rewrited.setRange(condition.getRange());
                orderByConditions.add(rewrited);
            }


            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request,
                    translatedWhereClause.getVariableBindings(), this);

            LinkedHashMap<Variable, SqlExpressionIntercode> aggregations = new LinkedHashMap<>();

            for(Entry<VariableNode, BuiltInCallExpression> entry : rewriter.getAggregations().entrySet())
                aggregations.put(getVariable(entry.getKey()), visitor.visitElement(entry.getValue()));

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
        LinkedHashMap<Variable, Direction> orderByVariables = new LinkedHashMap<>();

        for(OrderCondition condition : orderByConditions)
        {
            if(condition.getExpression() instanceof VariableNode varNode)
            {
                Variable var = getVariable(varNode);

                if(translatedWhereClause.getVariableBindings().get(var) != null)
                    orderByVariables.put(var, condition.getDirection());
            }
            else
            {
                VariableNode variable = createVariableNode(variablePrefix);
                Variable var = getVariable(variable);

                Bind bind = new Bind(condition.getExpression(), variable);
                translatedWhereClause = translateBind(bind, translatedWhereClause);

                if(translatedWhereClause.getVariableBindings().get(var) != null)
                    orderByVariables.put(var, condition.getDirection());
            }
        }

        Set<Variable> variables = new HashSet<>();

        for(Projection projection : select.getProjections())
            variables.add(getVariable(projection.getVariable()));

        if(select.isSubSelect() && getGraph() instanceof Variable variable)
            variables.add(variable);


        if(select.isSubSelect())
            return SqlSelect.create(request, variables, translatedWhereClause, select.isDistinct(), orderByVariables,
                    select.getOffset(), select.getLimit());

        List<Variable> selectVariables = select.getVariablesInScope().stream().map(v -> getVariable(v)).toList();

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
        RdfTerm graphTerm = getTerm(graph.getName());

        SqlIntercode quads = null;

        if(graphTerm instanceof Iri)
        {
            //NOTE: no mapping can provide the graph, so the graph does not exist regardless of the data
            quads = translateGraphQuads(graphTerm);

            if(quads.equals(SqlNoSolution.get()))
                return SqlNoSolution.get();
        }


        boolean rename = false;

        if(graph.getName() instanceof VariableNode)
        {
            rename = new ElementVisitor<Boolean>()
            {
                @Override
                public Boolean visit(VariableNode variable)
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


        if(rename)
            graphTerm = createVariable("@graph");


        graphRestrictions.push(graphTerm);

        SqlIntercode translatedPattern = visitElement(graph.getPattern());

        graphRestrictions.pop();

        if(rename)
            translatedPattern = SqlMerge.create(request, getVariable(((VariableNode) graph.getName())),
                    ((Variable) graphTerm), translatedPattern);


        if(graph.getName() instanceof VariableNode graphNode)
        {
            Variable var = getVariable(graphNode);
            VariableBinding binding = translatedPattern.getVariableBindings().get(var);

            if(binding == null || binding.canBeNull())
            {
                SqlIntercode graphs = SqlDistinct.create(request, translateGraphQuads(var), Set.of(var));

                /* NOTE: The pattern does not refer to the graphs, but it has to be evaluated again for every graph,
                 * which matters if it is nondeterministic.
                 */
                translatedPattern = SqlLateralJoin.lateralJoin(request, graphs, translatedPattern,
                        request.createLateralTable(), new Restrictions());
            }
        }
        else if(!isGraphWitnessed(graph, graphTerm))
        {
            //NOTE: the pattern has solutions even if the graph does not exist, so the existence has to be checked
            SqlExpressionIntercode exists = SqlExists.create(request, false, quads, new VariableBindings());

            translatedPattern = SqlFilter.filter(request, List.of(exists), translatedPattern);
        }

        return translatedPattern;
    }


    /**
     * Code matching any quad of the graph, used to check that the graph exists.
     *
     * @param graph the GRAPH pattern
     * @return code matching any quad of the graph, used to check that the graph exists
     */
    private SqlIntercode translateGraphQuads(RdfTerm graph)
    {
        PathTranslateVisitor pathVisitor = new PathTranslateVisitor(request, this, mappings);

        return pathVisitor.translate(graph, createVariable("@graphvar"), createVariableNode("@graphvar"),
                createVariable("@graphvar"));
    }


    /**
     * True if every solution of the GRAPH pattern necessarily comes from a triple evaluated in the graph, so the graph
     * is known to exist; false for patterns like {@code {}}, VALUES or BIND alone, whose graph has to be checked
     * separately.
     *
     * @param graph the GRAPH pattern
     * @param graphTerm the graph term
     * @return true if every solution of the GRAPH pattern necessarily comes from a triple evaluated in the graph, so
     *         the graph is known to exist; false for patterns like {@code {}}, VALUES or BIND alone, whose graph has to
     *         be checked separately
     */
    private boolean isGraphWitnessed(Graph graph, RdfTerm graphTerm)
    {
        return new ElementVisitor<Boolean>()
        {
            @Override
            protected Boolean defaultResult()
            {
                return false;
            }

            @Override
            protected Boolean aggregateResult(List<Boolean> results)
            {
                return results.stream().anyMatch(e -> e != null && e);
            }

            @Override
            public Boolean visit(Triple triple)
            {
                return true;
            }

            @Override
            public Boolean visit(GroupGraph groupGraph)
            {
                //NOTE: the group is a join, so it suffices if any of its members witnesses the graph
                return groupGraph.getPatterns().stream().anyMatch(p -> visitElement(p));
            }

            @Override
            public Boolean visit(Union union)
            {
                return union.getPatterns().stream().allMatch(p -> visitElement(p));
            }

            @Override
            public Boolean visit(Select select)
            {
                //NOTE: an aggregation without GROUP BY has a solution even if its pattern has none
                if(select.isInAggregateMode() && select.getGroupByConditions().isEmpty())
                    return false;

                return visitElement(select.getPattern());
            }

            @Override
            public Boolean visit(Graph nested)
            {
                //NOTE: a nested GRAPH clause is evaluated in another graph, unless it names the same one
                if(!getTerm(nested.getName()).equals(graphTerm))
                    return false;

                return visitElement(nested.getPattern());
            }

            @Override
            public Boolean visit(Optional optional)
            {
                return false;
            }

            @Override
            public Boolean visit(Minus minus)
            {
                return false;
            }

            @Override
            public Boolean visit(Filter filter)
            {
                return false;
            }

            @Override
            public Boolean visit(Bind bind)
            {
                return false;
            }

            @Override
            public Boolean visit(Values values)
            {
                return false;
            }

            @Override
            public Boolean visit(Service service)
            {
                return false;
            }

            @Override
            public Boolean visit(ProcedureCall procedureCall)
            {
                return false;
            }

            @Override
            public Boolean visit(MultiProcedureCall multiProcedureCall)
            {
                return false;
            }
        }.visitElement(graph.getPattern());
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
        List<List<RdfTerm>> lines = new ArrayList<>(values.getValuesLists().size());

        for(ValuesList list : values.getValuesLists())
        {
            List<RdfTerm> line = new ArrayList<>(list.getValues().size());

            for(Expression expression : list.getValues())
                line.add(getTerm((Node) expression));

            lines.add(line);
        }

        return translateValues(values.getVariables().stream().map(v -> getVariable(v)).toList(), lines);
    }


    @Override
    public SqlIntercode visit(Triple triple)
    {
        RdfTerm graph = getGraph();
        RdfTerm subject = getTerm(triple.getSubject());
        Verb predicate = triple.getPredicate();
        RdfTerm object = getTerm(triple.getObject());

        PathTranslateVisitor pathVisitor = new PathTranslateVisitor(request, this, mappings);
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


    /**
     * Translates the patterns of a group starting from {@code base}: joins them left to right, applies OPTIONAL as a
     * left join (with its filters as join conditions), MINUS, BIND, SERVICE and procedure calls in place, and the
     * group's FILTERs at the end.
     *
     * @param patterns the patterns
     * @param base the code the patterns are joined to
     * @return the resulting intermediate code
     */
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
                    LinkedList<Pattern> optionalPatterns = new LinkedList<>();

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


    /**
     * Left join of the group pattern with an OPTIONAL pattern, its filters becoming join conditions.
     *
     * @param translatedGroupPattern code of the patterns so far
     * @param translatedPattern code of the optional pattern
     * @param optionalFilters filters inside the OPTIONAL
     * @return left join of the group pattern with an OPTIONAL pattern, its filters becoming join conditions
     */
    private SqlIntercode translateLeftJoin(SqlIntercode translatedGroupPattern, SqlIntercode translatedPattern,
            LinkedList<Filter> optionalFilters)
    {
        List<SqlExpressionIntercode> conditions = new LinkedList<>();

        if(!optionalFilters.isEmpty())
        {
            VariableBindings bindings = SqlLeftJoin.getExpressionVariableBindings(request,
                    translatedGroupPattern.getVariableBindings(), translatedPattern.getVariableBindings());

            for(Filter filter : optionalFilters)
            {
                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request, bindings, this);
                SqlExpressionIntercode expression = SqlEffectiveBooleanValue
                        .create(visitor.visitElement(filter.getConstraint()));

                if(!expression.equals(trueValue))
                    conditions.add(expression);
            }
        }

        return SqlLeftJoin.leftJoin(request, translatedGroupPattern, translatedPattern, conditions);
    }


    /**
     * Applies a MINUS pattern to the group pattern.
     *
     * @param pattern the MINUS pattern
     * @param translatedGroupPattern code of the patterns so far
     * @return the resulting intermediate code
     */
    private SqlIntercode translateMinus(Minus pattern, SqlIntercode translatedGroupPattern)
    {
        SqlIntercode minusPattern = visitElement(pattern.getPattern());

        return SqlMinus.minus(request, translatedGroupPattern, minusPattern);
    }


    /**
     * Applies a BIND to the group pattern.
     *
     * @param bind the BIND pattern
     * @param translatedGroupPattern code of the patterns so far
     * @return the resulting intermediate code
     */
    private SqlIntercode translateBind(Bind bind, SqlIntercode translatedGroupPattern)
    {
        Variable var = getVariable(bind.getVariable());

        ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request,
                translatedGroupPattern.getVariableBindings(), this);
        SqlExpressionIntercode expression = visitor.visitElement(bind.getExpression());

        return SqlBind.bind(request, var, expression, translatedGroupPattern);
    }


    /**
     * Applies the FILTERs to the group pattern.
     *
     * @param filters the filters
     * @param groupPattern code of the group
     * @return the resulting intermediate code
     */
    private SqlIntercode translateFilters(List<Filter> filters, SqlIntercode groupPattern)
    {
        if(filters.size() == 0)
            return groupPattern;


        List<SqlExpressionIntercode> filterExpressions = new ArrayList<>(filters.size());

        for(Filter filter : filters)
        {
            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request,
                    groupPattern.getVariableBindings(), this);
            SqlExpressionIntercode expression = SqlEffectiveBooleanValue
                    .create(visitor.visitElement(filter.getConstraint()));

            filterExpressions.add(expression);
        }

        return SqlFilter.filter(request, filterExpressions, groupPattern);
    }


    /**
     * Translates inline data into a VALUES node, materialising each variable in the classes of the terms it takes.
     *
     * @param variableNames variables of the VALUES clause
     * @param lines the rows of terms
     * @return the resulting intermediate code
     */
    private SqlIntercode translateValues(List<Variable> variableNames, List<List<RdfTerm>> lines)
    {
        if(lines.size() == 0)
            return SqlNoSolution.get();


        Map<Variable, List<ResourceClass>> resourceClasses = new HashMap<>();
        LinkedHashMap<Column, List<Column>> data = new LinkedHashMap<>();
        Map<List<Column>, Column> revData = new HashMap<>();
        VariableBindings bindings = new VariableBindings();

        for(int i = 0; i < variableNames.size(); i++)
        {
            if(bindings.get(variableNames.get(i)) != null)
                continue; // ignore repeated occurrences of the variable

            LinkedHashSet<ResourceClass> resClasses = new LinkedHashSet<>();
            boolean canBeNull = false;

            List<ResourceClass> nodeResourceClasses = new ArrayList<>();

            for(List<RdfTerm> line : lines)
            {
                RdfTerm term = line.get(i);

                if(term != null)
                {
                    ResourceClass resClass = request.getResourceClass(term);

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

            VariableBinding binding = new VariableBinding(variableNames.get(i), canBeNull);

            for(ResourceClass resClass : resClasses)
            {
                List<List<Column>> classColumns = new ArrayList<>(resClass.getColumnCount());

                for(int j = 0; j < resClass.getColumnCount(); j++)
                    classColumns.add(new ArrayList<>(lines.size()));

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
                            classColumns.get(j).add(new NullColumn(resClass.getSqlTypes().get(j)));
                    }
                }


                List<Column> tableColumns = resClass.createColumns(request.getColumnMap(), variableNames.get(i));
                List<Column> mapping = new ArrayList<>(resClass.getColumnCount());

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

                binding.addMapping(resClass, mapping);
            }

            bindings.add(binding);
        }

        return SqlValues.create(bindings, resourceClasses, data, lines.size());
    }


    /**
     * Translates a procedure call evaluated laterally for every solution of {@code context}, filling in the default
     * values of omitted parameters.
     *
     * @param procedureCallBase the procedure call
     * @param context solutions the call is evaluated for
     * @return the resulting intermediate code
     */
    private SqlIntercode translateProcedureCall(ProcedureCallBase procedureCallBase, SqlIntercode context)
    {
        IriNode procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = configuration.getProcedures(getService())
                .get(procedureName.getValue());
        VariableBindings contextBindings = context.getVariableBindings();


        /* process parameters */

        ExpressionTranslateVisitor translator = new ExpressionTranslateVisitor(request, contextBindings, this);

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

        LinkedHashMap<ResultDefinition, Variable> resultNodes = new LinkedHashMap<>();

        Set<Variable> used = new HashSet<>(contextBindings.getVariables());
        LinkedHashMap<VariableNode, Node> conditions = new LinkedHashMap<>();

        if(procedureCallBase instanceof ProcedureCall)
        {
            /* single-result procedure call */

            ResultDefinition resultDefinition = procedureDefinition.getResult(null);
            Node result = ((ProcedureCall) procedureCallBase).getResult();

            if(!(result instanceof VariableOrBlankNode variable) || used.contains(getVariable(variable)))
            {
                VariableNode fakeResult = createVariableNode(variablePrefix);
                conditions.put(fakeResult, result);
                result = fakeResult;
            }

            used.add(getVariable((VariableOrBlankNode) result));

            resultNodes.put(resultDefinition, getVariable((VariableOrBlankNode) result));
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

                if(!(result instanceof VariableOrBlankNode variable) || used.contains(getVariable(variable)))
                {
                    VariableNode fakeResult = createVariableNode(variablePrefix);
                    conditions.put(fakeResult, result);
                    result = fakeResult;
                }

                used.add(getVariable((VariableOrBlankNode) result));

                resultNodes.put(resultDefinition, getVariable((VariableOrBlankNode) result));
            }
        }


        SqlIntercode intercode = SqlProcedureCall.create(request, procedureDefinition, parameterNodes, resultNodes,
                context);

        for(Entry<VariableNode, Node> entry : conditions.entrySet())
        {
            VariableNode fakeResult = entry.getKey();
            Node result = entry.getValue();

            VariableOrBlankNode var = null;

            if(result instanceof Literal literal)
            {
                var = createVariableNode(variablePrefix);
                intercode = SqlBind.bind(request, getVariable(var), SqlLiteral.create(request, literal), intercode);
            }
            else if(result instanceof IriNode iri)
            {
                var = createVariableNode(variablePrefix);
                intercode = SqlBind.bind(request, getVariable(var), SqlIri.create(request, getIri(iri)), intercode);
            }
            else if(result instanceof VariableOrBlankNode variable)
            {
                var = variable;
            }

            intercode = SqlMerge.create(request, getVariable(var), getVariable(fakeResult), intercode);
        }

        return intercode;
    }


    /**
     * Translates a SERVICE pattern: a service known to the configuration is translated in place with its own mappings,
     * any other becomes a {@link SqlServiceStub} evaluated later against the remote endpoint.
     *
     * @param service the SERVICE pattern
     * @param context solutions the call is evaluated for
     * @return the resulting intermediate code
     */
    private SqlIntercode translateService(Service service, SqlIntercode context)
    {
        RdfTerm name = getTerm(service.getName());

        if(configuration.getServices().contains(name))
        {
            serviceRestrictions.add((Iri) name);
            graphRestrictions.add(null);

            Pattern pattern = service.getPattern();
            List<Pattern> patterns = pattern instanceof GroupGraph group ? group.getPatterns() : List.of(pattern);

            //FIXME: passing context in this way may cause conflicts with the standard
            SqlIntercode result = translatePatternList(patterns, context);

            graphRestrictions.pop();
            serviceRestrictions.pop();

            return result;
        }

        String serviceCode = (new ServiceTranslateVisitor()).getResultCode(service.getPattern());

        Map<String, Variable> serviceVariables = service.getPattern().getVariablesInScope().stream()
                .collect(toMap(e -> e.getName(), e -> getVariable(e)));

        if(request.isServiceReorderEnabled())
        {
            SqlIntercode call = SqlServiceStub.create(request, name, serviceCode, serviceVariables,
                    SqlEmptySolution.get(), new StrBlankNodeInSegmentClass(--serviceId), service.isSilent());

            return SqlJoin.join(request, call, context);
        }
        else
        {
            return SqlServiceStub.create(request, name, serviceCode, serviceVariables, context,
                    new StrBlankNodeInSegmentClass(--serviceId), service.isSilent());
        }
    }


    /**
     * Translates the whole query; the optional offset, limit and ordering given by the caller (e.g. the endpoint) are
     * applied on top of those of the query.
     *
     * @param sparqlQuery the query to translate
     * @param offset the offset, or null
     * @param limit the upper bound
     * @param order variables to order the results by, on top of the query's own ORDER BY
     * @return the resulting select
     * @throws SQLException on database errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    public SqlSelect translate(Query sparqlQuery, BigInteger offset, BigInteger limit, List<Variable> order)
            throws SQLException, ServiceException
    {
        variableOccurrences = new HashMap<>();

        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(VariableNode variable)
            {
                List<Range> occurrences = variableOccurrences.get(variable.getName());

                if(occurrences == null)
                {
                    occurrences = new LinkedList<>();
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


    /**
     * Table providing the graph term of the mapping, or null if it is unknown.
     *
     * @param source the quad mapping
     * @return table providing the graph term of the mapping, or null if it is unknown
     */
    private static SourceTable getGraphTable(QuadMapping source)
    {
        return switch(source)
        {
            case SingleTableQuadMapping single -> single.getTable();
            case JoinTableQuadMapping join -> join.getTables().get(join.getGraphTableIdx());
            default -> null;
        };
    }


    /**
     * Condition restricting the graph term of the mapping to the given IRIs.
     *
     * @param source the quad mapping
     * @param graphMap the graph term mapping
     * @param iris IRIs the graph may take
     * @return condition restricting the graph term of the mapping to the given IRIs
     */
    private Conditions getGraphCondition(QuadMapping source, TermMapping graphMap, Set<Iri> iris)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();
        SourceTable table = getGraphTable(source);

        ResourceClass resClass = graphMap.getResourceClass(request);
        List<Column> cols = graphMap.getColumns(request);

        Conditions conditions = new Conditions(false);

        for(Iri iri : iris)
        {
            List<Column> values = resClass.toColumns(request.getStatement(), iri);

            Condition condition = new Condition();
            condition.addAreEqual(cols, values,
                    SqlTableAccess.needsNullSafeEquality(schema, table, resClass, cols, values));
            conditions = Conditions.or(conditions, new Conditions(condition));
        }

        return conditions;
    }


    /**
     * Selects the quad mappings of the query according to its FROM and FROM NAMED clauses (all mappings of the service
     * when there are none).
     *
     * @param datasets the dataset clauses
     */
    protected void setDatasets(List<DataSet> datasets)
    {
        if(datasets.isEmpty())
        {
            mappings = request.getConfiguration().getMappings(getService());
        }
        else
        {
            Set<Iri> defaults = datasets.stream().filter(d -> d.isDefault()).map(d -> getIri(d.getSourceSelector()))
                    .collect(toSet());

            Set<Iri> named = datasets.stream().filter(d -> !d.isDefault()).map(d -> getIri(d.getSourceSelector()))
                    .collect(toSet());


            mappings = new ArrayList<>();

            for(QuadMapping source : request.getConfiguration().getMappings(getService()))
            {
                TermMapping graphMap = source.getGraph();

                if(graphMap instanceof ConstantIriMapping constGraphMapping)
                {
                    if(defaults.contains(constGraphMapping.getIri()))
                        mappings.add(source.asDefaultGraphMapping());

                    if(named.contains(constGraphMapping.getIri()))
                        mappings.add(source);
                }
                else
                {
                    Set<Iri> validDefaults = defaults.stream().filter(i -> graphMap.match(request, i)).collect(toSet());

                    if(!validDefaults.isEmpty())
                        mappings.add(source.asDefaultGraphMapping(getGraphCondition(source, graphMap, validDefaults)));

                    Set<Iri> validNamed = named.stream().filter(i -> graphMap.match(request, i)).collect(toSet());

                    if(!validNamed.isEmpty())
                        mappings.add(source.asNamedGraphMapping(getGraphCondition(source, graphMap, validNamed)));
                }
            }
        }
    }


    /**
     * Fresh variable node with a name that cannot occur in a query.
     *
     * @param prefix name prefix of the variable
     * @return fresh variable node with a name that cannot occur in a query
     */
    protected VariableNode createVariableNode(String prefix)
    {
        return new VariableNode(prefix + variableId++);
    }


    /**
     * Fresh variable with a name that cannot occur in a query.
     *
     * @param prefix name prefix of the variable
     * @return fresh variable with a name that cannot occur in a query
     */
    protected Variable createVariable(String prefix)
    {
        return getVariable(new VariableNode(prefix + variableId++));
    }


    /**
     * Service whose mappings are in effect at the current position.
     *
     * @return service whose mappings are in effect at the current position
     */
    public final Iri getService()
    {
        return serviceRestrictions.peek();
    }


    /**
     * Graph term in effect at the current position; null for the default graph.
     *
     * @return graph term in effect at the current position; null for the default graph
     */
    public final RdfTerm getGraph()
    {
        return graphRestrictions.peek();
    }


    /**
     * User IRI classes of the configuration.
     *
     * @return user IRI classes of the configuration
     */
    public final List<UserIriClass> getIriClasses()
    {
        return iriClasses;
    }


    /**
     * Prologue of the query being translated.
     *
     * @return prologue of the query being translated
     */
    public Prologue getPrologue()
    {
        return prologue;
    }
}
