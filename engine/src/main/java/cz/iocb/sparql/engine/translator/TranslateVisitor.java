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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.LateralTable;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.imcode.Scope;
import cz.iocb.sparql.engine.imcode.SqlAggregation;
import cz.iocb.sparql.engine.imcode.SqlBind;
import cz.iocb.sparql.engine.imcode.SqlConstruct;
import cz.iocb.sparql.engine.imcode.SqlConstruct.BlankNodeTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.IriTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.LiteralTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.RdfTermTemplate;
import cz.iocb.sparql.engine.imcode.SqlConstruct.Template;
import cz.iocb.sparql.engine.imcode.SqlConstruct.TripleTermTemplate;
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
import cz.iocb.sparql.engine.imcode.SqlServiceStub.Enclosure;
import cz.iocb.sparql.engine.imcode.SqlShared;
import cz.iocb.sparql.engine.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.imcode.SqlUnion;
import cz.iocb.sparql.engine.imcode.SqlValues;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.sparql.engine.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.imcode.expression.SqlLateralVariable;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlOuterVariable;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
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
import cz.iocb.sparql.engine.model.base.Element;
import cz.iocb.sparql.engine.model.base.Range;
import cz.iocb.sparql.engine.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.model.expression.ExistsExpression;
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
import cz.iocb.sparql.engine.model.triple.TripleTermNode;
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
     * Lateral values of the graphs of the sub-selects being evaluated per graph, by the variable standing for the graph
     * inside them (see {@link #visit(Select)}).
     */
    private final Map<Variable, SqlExpressionIntercode> lateralGraphs = new HashMap<>();

    /**
     * Lateral joins of the evaluations per graph being translated whose pattern holds a SERVICE call, outermost first:
     * the context of a call may refer to the graphs of their current solutions (see {@link #translatePerGraph}).
     */
    private final Stack<Enclosure> enclosures = new Stack<>();

    /**
     * Code enumerating the named graphs of the mappings in effect, by the variable bound to them (see
     * {@link #getGraphEnumeration}).
     */
    private Map<Variable, SqlIntercode> graphEnumerations = new HashMap<>();

    /**
     * The named graphs of the mappings in effect shared by the statement, by the variable bound to them (see
     * {@link #getNamedGraphs}).
     */
    private Map<Variable, SqlIntercode> namedGraphs = new HashMap<>();

    /**
     * Scope of the pattern being translated: that of the statement, and inside an EXISTS pattern the scope of that
     * pattern, entered with the current solution the pattern is evaluated for (see {@link #translateExists}).
     */
    private Scope scope = Scope.TOP;

    /**
     * True while the clauses above the WHERE clause of a sub-select inside an EXISTS pattern are being translated:
     * their expressions see only the solutions the WHERE clause delivers, which hold the values of the current solution
     * the clauses refer to (see {@link #restrictToCurrentSolution}), not the current solution itself.
     */
    private boolean aboveWhereClause = false;

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
     * Quad mappings in effect at the current position: the mappings of the queried service the dataset clauses of the
     * query select, and inside a SERVICE pattern naming a service of the configuration all mappings of that service.
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
        SqlExpressionIntercode expression;

        // the select is the pattern of an existence test, so its nodes lie one level deeper than the node holding it
        Scope enclosingScope = scope;
        scope = enclosingScope.enterPattern(null);

        try
        {
            SqlIntercode translatedSelect = visitElement(askQuery.getSelect());
            expression = SqlExists.create(request, enclosingScope, false, translatedSelect, new VariableBindings());
        }
        finally
        {
            scope = enclosingScope;
        }

        SqlIntercode bind = SqlBind.bind(request, variable, expression, SqlEmptySolution.get());

        return SqlSelect.createTopLevel(request, List.of(variable), bind);
    }


    @Override
    public SqlIntercode visit(DescribeQuery describeQuery)
    {
        ClassRelations relations = request.getConfiguration();

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
                SqlExpressionIntercode expression = SqlVariable.create(relations, select.getVariable(variable));

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
        ClassRelations relations = request.getConfiguration();

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

        // the subject of a produced triple has to be an IRI or a blank node, its predicate an IRI
        SqlIntercode filter = SqlFilter.filter(request, List.of(
                SqlBuiltinCall.create(request, "isiri", false,
                        List.of(SqlVariable.create(relations, construct.getVariable(PREDICATE.getVariable())))),
                SqlUnaryLogical.create(relations,
                        SqlBuiltinCall.create(request, "isliteral", false,
                                List.of(SqlVariable.create(relations, construct.getVariable(SUBJECT.getVariable()))))),
                SqlUnaryLogical.create(relations,
                        SqlBuiltinCall.create(request, "istriple", false,
                                List.of(SqlVariable.create(relations, construct.getVariable(SUBJECT.getVariable())))))),
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
            case TripleTermNode triple -> new TripleTermTemplate(new Template(createTemplate(triple.getSubject()),
                    createTemplate(triple.getPredicate()), createTemplate(triple.getObject())));
            default -> throw new IllegalArgumentException();
        };
    }


    @Override
    public SqlIntercode visit(Select select)
    {
        /* NOTE: A sub-select inside GRAPH ?g is evaluated separately in every named graph: its DISTINCT, ORDER BY,
         * OFFSET, LIMIT and aggregation apply per graph, and an aggregation without GROUP BY has a solution even in a
         * graph without matching triples. Unless its enclosing pattern is already evaluated that way, the sub-select
         * is evaluated per graph by itself; the result binds the graph variable of the enclosing clause.
         */
        if(select.isSubSelect() && getGraph() instanceof Variable graph && !lateralGraphs.containsKey(graph))
            return translatePerGraph(graph, select, () -> translateSelect(select));

        return translateSelect(select);
    }


    /**
     * Translates the select; in an evaluation per graph, the WHERE clause of a sub-select is restricted to the graph of
     * the current solution. Inside an EXISTS pattern, the WHERE clause of a sub-select is restricted to the solutions
     * compatible with the current solution the pattern is evaluated for (see {@link #restrictToCurrentSolution}),
     * because the projection, the grouping, DISTINCT and the slice above it work with complete solutions; the clauses
     * above it then see the values of the current solution only through those solutions (see
     * {@link #aboveWhereClause}).
     *
     * @param select the select
     * @return the resulting intermediate code
     */
    private SqlIntercode translateSelect(Select select)
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


        SqlIntercode translatedWhereClause = restrictToCurrentSolution(visitElement(pattern), null,
                getClauseVariables(select), false);

        translatedWhereClause = restrictToCurrentGraph(translatedWhereClause);

        boolean enclosingAboveWhereClause = aboveWhereClause;
        aboveWhereClause = select.isSubSelect() && scope.getCurrentSolution() != null;

        try
        {
            return translateSelectClauses(select, translatedWhereClause);
        }
        finally
        {
            aboveWhereClause = enclosingAboveWhereClause;
        }
    }


    /**
     * Translates the clauses of the select above its WHERE clause: the grouping, the aggregation with HAVING, the
     * trailing VALUES, the projection, the ordering and the slice.
     *
     * @param select the select
     * @param translatedWhereClause code of the WHERE clause
     * @return the resulting intermediate code
     */
    private SqlIntercode translateSelectClauses(Select select, SqlIntercode translatedWhereClause)
    {
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
                getGroupStart(groupGraph.getPatterns()));

        return translatedGroupGraphPattern;
    }


    @Override
    public SqlIntercode visit(Graph graph)
    {
        RdfTerm graphTerm = getTerm(graph.getName());

        /* NOTE: When not every solution of the pattern comes from a triple of the graph, the pattern does not bind the
         * graph variable by itself, and a MINUS, OPTIONAL or EXISTS in it whose triples do would be evaluated over all
         * graphs at once. Such a pattern is evaluated separately in every named graph. In the other patterns, a group
         * whose MINUS, OPTIONAL or EXISTS comes before the triples that bind the graph starts from the named graphs
         * (see getGroupStart).
         */
        if(graph.getName() instanceof VariableNode graphNode && !isGraphWitnessed(graph.getPattern(), graphTerm))
            return translatePerGraph(getVariable(graphNode), graph.getPattern(),
                    () -> restrictToCurrentGraph(visitElement(graph.getPattern())));

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

            //NOTE: a witnessed pattern binds the variable in every solution; otherwise it is evaluated per graph
            if(binding == null || binding.canBeNull())
                return translatePerGraph(var, graph.getPattern(),
                        () -> restrictToCurrentGraph(visitElement(graph.getPattern())));
        }
        else if(!isGraphWitnessed(graph.getPattern(), graphTerm))
        {
            //NOTE: the pattern has solutions even if the graph does not exist, so the existence has to be checked
            SqlExpressionIntercode exists = SqlExists.create(request, scope, false, quads, new VariableBindings());

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
     * Code enumerating the named graphs as solutions of the variable: the distinct graphs of all quads.
     *
     * @param variable the variable bound to the graphs
     * @return code enumerating the named graphs as solutions of the variable
     */
    private SqlIntercode translateGraphs(Variable variable)
    {
        return SqlDistinct.create(request, translateGraphQuads(variable), Set.of(variable));
    }


    /**
     * Solutions holding in every graph whatever its triples (the zero-length path from a constant) as solutions in the
     * given graph: in a variable graph, they are paired with every named graph, so that they bind the graph like the
     * solutions of a triple, the named graphs being shared by the statement (see {@link #getNamedGraphs}); in a
     * constant graph, they hold only when the graph exists; in the default graph, they are returned unchanged. Inside
     * an evaluation per graph (see {@link #translatePerGraph}), the solutions of an EXISTS pattern take the lateral
     * value of the graph the pattern is evaluated in instead of being paired with every named graph, so that a walk
     * starting from them stays in that graph.
     *
     * @param graph the graph term, or null for the default graph
     * @param solutions the solutions
     * @return the solutions in the given graph
     */
    SqlIntercode translateInGraph(RdfTerm graph, SqlIntercode solutions)
    {
        // a pattern evaluated for a current solution is the pattern of an EXISTS
        if(graph instanceof Variable variable && lateralGraphs.containsKey(variable)
                && scope.getCurrentSolution() != null)
            return SqlBind.bind(request, variable, lateralGraphs.get(variable), solutions);

        if(graph instanceof Variable variable)
            return SqlJoin.join(request, getNamedGraphs(variable), solutions);

        if(graph == null)
            return solutions;

        SqlExpressionIntercode exists = SqlExists.create(request, scope, false, translateGraphQuads(graph),
                new VariableBindings());

        return SqlFilter.filter(request, List.of(exists), solutions);
    }


    /**
     * Evaluates the pattern given by the translation separately in every named graph bound to the variable: the graphs
     * are enumerated, the pattern is translated with a fresh variable standing for the graph of its triples and joined
     * laterally, evaluated for the graph of the current solution. The lateral value of that graph is remembered in
     * {@link #lateralGraphs}, and {@link #restrictToCurrentGraph} merges the fresh variable with it wherever the graph
     * matters: the pattern as a whole, the WHERE clause of a sub-select, the right side of a MINUS or an OPTIONAL and
     * the pattern of an EXISTS. The result binds the variable to the graph. The pattern reads the lateral value as the
     * enumeration provides it at the moment (see {@link SqlLateralJoin}): the optimisation splits the enumeration into
     * branches, those of the graphs given by constants and those of the graphs of a class, and the pattern of a branch
     * keeps only the table accesses that can hold triples of its graphs. When the pattern holds a SERVICE call, the
     * join is remembered in {@link #enclosures} too, because the context of the call may refer to the lateral value and
     * is evaluated by a statement of its own: the call evaluates it for all graphs, joined laterally with the graphs
     * bound to a fresh variable again, and keeps the received solutions of the current graph (see {@link Enclosure}).
     * Such a pattern keeps the lateral value as the whole enumeration provides it, so that the copies of the call in
     * the branches stay the same and share that one evaluation.
     *
     * @param variable the graph variable
     * @param pattern the pattern, searched for SERVICE calls
     * @param translation translation of the pattern, run with the fresh variable as the graph in effect
     * @return the resulting intermediate code
     */
    private SqlIntercode translatePerGraph(Variable variable, Element pattern, Supplier<SqlIntercode> translation)
    {
        SqlIntercode graphs = getGraphEnumeration(variable);
        VariableBinding graphBinding = graphs.getVariableBindings().get(variable);

        if(graphBinding == null)
            return SqlNoSolution.get();

        LateralTable lateralTable = request.createLateralTable();
        Restrictions requirements = new Restrictions(List.of(variable));
        VariableBindings lateral = SqlLateralJoin.getLateralVariableBindings(request.getConfiguration(), lateralTable,
                graphs, requirements);

        Variable innerGraph = createVariable("@graph");

        lateralGraphs.put(innerGraph, SqlLateralVariable.create(lateralTable, lateral.get(variable)));
        graphRestrictions.push(innerGraph);

        boolean service = holds(pattern, Service.class);

        if(service)
        {
            // the graphs bound to a variable that no pattern mentions, with the columns the pattern refers to
            Variable current = createVariable("@lateral");
            VariableBinding exposed = graphs.getVariableBindings().restrict(request.getConfiguration(), requirements)
                    .get(variable);

            enclosures.push(new Enclosure(SqlMerge.create(request, current, variable, graphs), lateralTable,
                    new VariableBinding(current, exposed.getMappings(), exposed.canBeNull())));
        }

        SqlIntercode translated = translation.get();

        if(service)
            enclosures.pop();

        graphRestrictions.pop();
        lateralGraphs.remove(innerGraph);

        // the copies of a SERVICE call in the branches of the graphs share one evaluation of the call for all graphs
        return SqlLateralJoin.lateralJoin(request, graphs, translated, lateralTable, requirements, !service);
    }


    /**
     * In an evaluation per graph (see {@link #translatePerGraph}), keeps the solutions of the pattern that hold in the
     * graph of the current solution: those whose variable of the graph in effect holds the same term, and those binding
     * no graph, which hold in every graph. The variable is merged with the lateral value of the graph the way a
     * variable of an EXISTS pattern is merged with its value in the current solution (see
     * {@link #restrictToCurrentSolution}): the value is bound to a fresh variable merged into the variable, so the
     * table accesses of the pattern take it into their conditions and the database looks the triples of the graph up
     * instead of reading the triples of every graph for each of them. Outside such an evaluation, the pattern is
     * returned unchanged.
     *
     * @param pattern the pattern
     * @return solutions of the pattern that hold in the graph of the current solution
     */
    private SqlIntercode restrictToCurrentGraph(SqlIntercode pattern)
    {
        if(!(getGraph() instanceof Variable graph) || !lateralGraphs.containsKey(graph))
            return pattern;

        if(pattern.getVariableBindings().get(graph) == null)
            return pattern;

        Variable fresh = createVariable(variablePrefix);

        return SqlMerge.create(request, graph, fresh, SqlBind.bind(request, fresh, lateralGraphs.get(graph), pattern));
    }


    /**
     * Translates an EXISTS expression evaluated for the solutions of the given bindings. Following SPARQL 1.2, the
     * pattern is evaluated for the current solution: every solution of every group graph pattern inside it extends the
     * current solution. No node of the pattern stands for the current solution; its bindings are kept by the scope the
     * pattern is translated in (see {@link Scope#enterPattern}), and the pattern is made to agree with it only where
     * the algebra needs it. The test itself compares the solutions of the pattern with the current solution (see
     * {@link SqlExists}), except for the variables the pattern may leave unbound, which are merged with their current
     * values inside the pattern (see {@link #restrictToCurrentSolution}). The right side of an OPTIONAL or a MINUS and
     * the WHERE clause of a sub-select are restricted to the solutions compatible with the current solution, and the
     * sides of a MINUS share its variables (see {@link #getCurrentSolutionBound}). A variable an expression of the
     * pattern finds unbound takes its value in the current solution (see {@link #getOuterVariable}), and a walk of a
     * property path from a node of the current solution starts from that node (see {@link #getWalkStart}). The current
     * solution holds the variables mentioned in the pattern and the graph variable in effect, because the triples of
     * the pattern bind it; when the pattern contains a MINUS, it holds all variables, because the sides of the MINUS
     * share the variables of the current solution. The solutions of a node of a group lying in another EXISTS pattern
     * extend the current solution of that pattern, so the current solution of the nested pattern is joined with it. In
     * an evaluation per graph, the pattern is restricted to the graph of the current solution.
     *
     * @param exists the EXISTS expression
     * @param bindings bindings of the solutions the expression is evaluated for
     * @return the resulting expression
     */
    SqlExpressionIntercode translateExists(ExistsExpression exists, VariableBindings bindings)
    {
        GraphPattern pattern = exists.getPattern();

        Set<Variable> mentioned = new HashSet<>();
        boolean[] hasMinus = new boolean[1];

        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(VariableNode variable)
            {
                mentioned.add(getVariable(variable));
                return null;
            }

            @Override
            public Void visit(Minus minus)
            {
                hasMinus[0] = true;
                return super.visit(minus);
            }
        }.visitElement(pattern);

        if(getGraph() instanceof Variable graph)
            mentioned.add(graph);

        // the clauses above the WHERE clause of a sub-select see its solutions only, which extend no current solution
        boolean enclosingAboveWhereClause = aboveWhereClause;
        boolean chained = !enclosingAboveWhereClause;
        VariableBindings enclosing = chained ? scope.getCurrentSolution() : null;

        Set<Variable> variables = mentioned;

        if(hasMinus[0])
        {
            variables = new HashSet<>(bindings.getVariables());

            if(enclosing != null)
                variables.addAll(enclosing.getVariables());
        }

        VariableBindings solution = SqlExists.getCurrentSolution(request, scope, bindings, variables, chained);

        // the nodes of the pattern lie one level deeper than the node holding the expression
        Scope enclosingScope = scope;
        scope = enclosingScope.enterPattern(solution);
        aboveWhereClause = false;

        try
        {
            SqlIntercode translated = restrictToCurrentGraph(visitElement(pattern));

            // the test compares a variable the pattern binds always by plain equality, whereas one it may leave unbound
            // would be compared by a disjunction the database can neither hash nor look up by an index
            translated = restrictToCurrentSolution(translated, null, Set.of(), true);

            return SqlExists.create(request, enclosingScope, exists.isNegated(), translated, bindings, variables,
                    chained, solution);
        }
        finally
        {
            aboveWhereClause = enclosingAboveWhereClause;
            scope = enclosingScope;
        }
    }


    /**
     * Value of the variable in the current solution of the innermost EXISTS pattern being translated, for an expression
     * of the pattern that finds the variable unbound; NULL outside such a pattern, when the current solution does not
     * bind the variable, and for the clauses above the WHERE clause of a sub-select (see {@link #aboveWhereClause}).
     *
     * @param variable the variable
     * @return value of the variable in the current solution of the innermost EXISTS pattern being translated
     */
    SqlExpressionIntercode getOuterVariable(Variable variable)
    {
        VariableBindings solution = scope.getCurrentSolution();

        if(solution == null || aboveWhereClause)
            return SqlNull.get();

        return SqlOuterVariable.create(variable, solution);
    }


    /**
     * Keeps the solutions of a pattern inside an EXISTS pattern that are compatible with the current solution the
     * EXISTS pattern is evaluated for, where no join with such solutions does it: the right side of an OPTIONAL or a
     * MINUS, the WHERE clause of a sub-select, and the EXISTS pattern as a whole for the variables it may leave
     * unbound. A variable of the current solution the pattern binds is merged with its current value (the value is
     * bound to a fresh variable merged into the variable, see {@link SqlMerge}, which is distributed over the branches
     * of a union; the variable takes the value where the pattern leaves it unbound), unless the left side the pattern
     * is joined with binds the variable always: the join then makes the pattern agree with the left side, which is
     * compared with the current solution in its turn. The completed variables are bound to their current values even
     * where the pattern does not bind them, so that the solutions are complete for the clauses of a sub-select that
     * read them. Outside an EXISTS pattern, the pattern is returned unchanged.
     *
     * @param pattern the pattern
     * @param left bindings of the left side the pattern is joined with, or null
     * @param completed variables bound to their current values also where the pattern does not bind them
     * @param nullableOnly whether only the variables the pattern may leave unbound are merged
     * @return solutions of the pattern compatible with the current solution
     */
    private SqlIntercode restrictToCurrentSolution(SqlIntercode pattern, VariableBindings left, Set<Variable> completed,
            boolean nullableOnly)
    {
        VariableBindings solution = scope.getCurrentSolution();

        if(solution == null)
            return pattern;

        for(VariableBinding binding : solution.getValues())
        {
            Variable variable = binding.getVariable();
            VariableBinding own = pattern.getVariableBindings().get(variable);

            if(own == null && !completed.contains(variable))
                continue;

            if(nullableOnly && (own == null || !own.canBeNull()))
                continue;

            if(own != null && left != null && left.get(variable) != null && !left.get(variable).canBeNull())
                continue;

            SqlExpressionIntercode value = SqlOuterVariable.create(variable, solution);

            if(own == null)
            {
                pattern = SqlBind.bind(request, variable, value, pattern);
            }
            else
            {
                Variable fresh = createVariable(variablePrefix);

                pattern = SqlMerge.create(request, variable, fresh, SqlBind.bind(request, fresh, value, pattern));
            }
        }

        return pattern;
    }


    /**
     * The variables the clauses above the WHERE clause of a sub-select inside an EXISTS pattern refer to: those of the
     * projection and of the GROUP BY, HAVING and ORDER BY clauses, also inside their expressions. The WHERE clause
     * binds them to their values in the current solution where its pattern does not bind them (see
     * {@link #restrictToCurrentSolution}), because the clauses read its solutions only; an aggregate over a value of
     * the current solution alone would even belong to the query of the node holding the EXISTS expression. Outside an
     * EXISTS pattern, there are none.
     *
     * @param select the select
     * @return the variables the clauses above the WHERE clause of a sub-select inside an EXISTS pattern refer to
     */
    private Set<Variable> getClauseVariables(Select select)
    {
        Set<Variable> variables = new HashSet<>();

        if(!select.isSubSelect() || scope.getCurrentSolution() == null)
            return variables;

        ElementVisitor<Void> visitor = new ElementVisitor<>()
        {
            @Override
            public Void visit(VariableNode variable)
            {
                variables.add(getVariable(variable));
                return null;
            }
        };

        for(Projection projection : select.getProjections())
        {
            variables.add(getVariable(projection.getVariable()));
            visitor.visitElement(projection.getExpression());
        }

        for(GroupCondition condition : select.getGroupByConditions())
            visitor.visitElement(condition.getExpression());

        for(Expression condition : select.getHavingConditions())
            visitor.visitElement(condition);

        for(OrderCondition condition : select.getOrderByConditions())
            visitor.visitElement(condition.getExpression());

        return variables;
    }


    /**
     * Condition that the current solution of the innermost EXISTS pattern being translated binds a variable, an
     * expression over the values of that solution only; null outside such a pattern and when the current solution has
     * no variable. Every solution of both sides of a MINUS inside the pattern extends the current solution, so the
     * sides share its variables (see {@link SqlMinus}).
     *
     * @return condition that the current solution binds a variable, or null
     */
    private SqlExpressionIntercode getCurrentSolutionBound()
    {
        VariableBindings solution = scope.getCurrentSolution();

        if(solution == null)
            return null;

        ClassRelations relations = request.getConfiguration();
        SqlExpressionIntercode result = null;

        for(Variable variable : solution.getVariables())
        {
            SqlExpressionIntercode bound = SqlBuiltinCall.create(request, "bound", false,
                    List.of(SqlOuterVariable.create(variable, solution)));

            result = result == null ? bound :
                    SqlBinaryLogical.create(relations, LogicalOperator.OR,
                            SqlEffectiveBooleanValue.create(relations, result),
                            SqlEffectiveBooleanValue.create(relations, bound));
        }

        return result;
    }


    /**
     * The values a walk of a property path between two variables starts from inside an EXISTS pattern, when the current
     * solution of the pattern binds the given start variable always and does not bind the end variable: the value of
     * the start variable in the current solution and, when the current solution binds the graph variable of the walk
     * always too, its value, by the variables (see {@link PathTranslateVisitor}). The walk then starts from the node
     * the current solution gives, instead of being evaluated between all nodes and compared with the current solution
     * afterwards, unless siblings of the walk restrict its other end more. A walk between two nodes of the current
     * solution is evaluated by itself and compared, which the database does once for all current solutions.
     *
     * @param begin the variable the walk would start from
     * @param end the variable at the other end of the walk
     * @param graph the graph variable of the walk, or null
     * @return the values the walk starts from, by the variables; empty when the walk does not start from the current
     *         solution
     */
    Map<Variable, SqlExpressionIntercode> getWalkStart(Variable begin, Variable end, Variable graph)
    {
        VariableBindings solution = scope.getCurrentSolution();

        if(solution == null || solution.get(begin) == null || solution.get(begin).canBeNull()
                || solution.get(end) != null)
            return Map.of();

        Map<Variable, SqlExpressionIntercode> start = new LinkedHashMap<>();
        start.put(begin, SqlOuterVariable.create(begin, solution));

        if(graph != null && solution.get(graph) != null && !solution.get(graph).canBeNull())
            start.put(graph, SqlOuterVariable.create(graph, solution));

        return start;
    }


    /**
     * The solution a group starts from: the empty solution, or the named graphs inside GRAPH ?g evaluated over all
     * graphs at once (see {@link #visit(Graph)}) when a member of the group would see the graph unbound: an OPTIONAL, a
     * MINUS or a BIND holding an EXISTS before the first member witnessing the graph (see {@link #isGraphWitnessed}),
     * or a FILTER holding an EXISTS in a group witnessing it nowhere. Started from the named graphs, the group is
     * evaluated in every graph by a join on the graph variable. Inside an EXISTS pattern whose current solution binds
     * the graph always, the group lies in that graph by itself: its members are made to agree with the current solution
     * where they need the graph (see {@link #translateExists}). The named graphs are not shared here (see
     * {@link #getNamedGraphs}): the join splits them by the classes of the graph variable and compares each class by
     * plain equality.
     *
     * @param patterns the members of the group
     * @return the solution the group starts from
     */
    private SqlIntercode getGroupStart(List<Pattern> patterns)
    {
        if(!(getGraph() instanceof Variable graph) || lateralGraphs.containsKey(graph))
            return SqlEmptySolution.get();

        VariableBindings solution = scope.getCurrentSolution();
        VariableBinding currentGraph = solution != null ? solution.get(graph) : null;

        if(currentGraph != null && !currentGraph.canBeNull())
            return SqlEmptySolution.get();

        boolean witnessed = false;
        boolean filtersExists = false;
        boolean needed = false;

        for(Pattern member : patterns)
        {
            if(member instanceof Optional || member instanceof Minus)
                needed |= !witnessed;
            else if(member instanceof Bind bind)
                needed |= !witnessed && holds(bind.getExpression(), ExistsExpression.class);
            else if(member instanceof Filter filter)
                filtersExists |= holds(filter, ExistsExpression.class);
            else
                witnessed |= isGraphWitnessed(member, graph);
        }

        if(!needed && (witnessed || !filtersExists))
            return SqlEmptySolution.get();

        return getGraphEnumeration(graph);
    }


    /**
     * Code enumerating the named graphs as solutions of the variable (see {@link #translateGraphs}), built once per
     * variable, because enumerating the graphs of every quad mapping is costly to translate.
     *
     * @param variable the variable bound to the graphs
     * @return code enumerating the named graphs as solutions of the variable
     */
    private SqlIntercode getGraphEnumeration(Variable variable)
    {
        return graphEnumerations.computeIfAbsent(variable, v -> translateGraphs(v));
    }


    /**
     * The named graphs as solutions of the variable, optimised for it and shared by the statement (see
     * {@link SqlShared#shareOptimized}), also inside the SQL of an expression, built once per variable.
     *
     * @param variable the variable bound to the graphs
     * @return the named graphs as solutions of the variable
     */
    private SqlIntercode getNamedGraphs(Variable variable)
    {
        return namedGraphs.computeIfAbsent(variable, v -> SqlShared.shareOptimized(request, getGraphEnumeration(v), v));
    }


    /**
     * True if every solution of the pattern of a GRAPH clause (or of a member of it) necessarily comes from a triple
     * evaluated in the graph, which then binds a graph variable and is known to exist; false for patterns like
     * {@code {}}, VALUES or BIND alone, which are evaluated per graph (a variable graph) or have to check that the
     * graph exists (a constant one). A zero-length path from a constant is paired with the named graphs (see
     * {@link #translateInGraph}), so it witnesses the graph as well.
     *
     * @param pattern the pattern
     * @param graphTerm the graph term
     * @return true if every solution of the pattern necessarily comes from a triple evaluated in the graph, which then
     *         binds a graph variable and is known to exist; false for patterns like {@code {}}, VALUES or BIND alone,
     *         which are evaluated per graph (a variable graph) or have to check that the graph exists (a constant one)
     */
    private boolean isGraphWitnessed(Element pattern, RdfTerm graphTerm)
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
        }.visitElement(pattern);
    }


    /**
     * True if the element holds an element of the given kind.
     *
     * @param element the element
     * @param kind the kind of the elements
     * @return true if the element holds an element of the given kind, false otherwise
     */
    private static boolean holds(Element element, Class<? extends Element> kind)
    {
        Boolean holds = new ElementVisitor<Boolean>()
        {
            @Override
            public Boolean visitElement(Element child)
            {
                return kind.isInstance(child) ? Boolean.TRUE : super.visitElement(child);
            }

            @Override
            protected Boolean aggregateResult(List<Boolean> results)
            {
                return results.stream().anyMatch(e -> e != null && e);
            }
        }.visitElement(element);

        return holds != null && holds;
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

        return SqlValues.create(request, values.getVariables().stream().map(v -> getVariable(v)).toList(), lines);
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
     * group's FILTERs at the end. A filter is applied at the end also when a remote SERVICE call of the group could be
     * asked about fewer solutions with it: which of its conditions may restrict the context of a call is decided when
     * the intermediate code is optimised (see {@link SqlIntercode#sink}), where the bindings tell whether a condition
     * holds for a solution of the context exactly when it holds for the solutions the call makes of it.
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

                    translatedPattern = translatePatternList(optionalPatterns, getGroupStart(optionalPatterns));
                }
                else
                {
                    translatedPattern = visitElement(optionalPattern);
                }

                // inside an EXISTS pattern, the optional solutions extend the current solution too
                translatedPattern = restrictToCurrentSolution(translatedPattern,
                        translatedGroupPattern.getVariableBindings(), Set.of(), false);

                translatedGroupPattern = translateLeftJoin(translatedGroupPattern,
                        restrictToCurrentGraph(translatedPattern), optionalFilters);
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
            VariableBindings bindings = SqlLeftJoin.getExpressionVariableBindings(request, scope,
                    translatedGroupPattern.getVariableBindings(), translatedPattern.getVariableBindings());

            for(Filter filter : optionalFilters)
            {
                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(request, bindings, this);
                SqlExpressionIntercode expression = SqlEffectiveBooleanValue.create(request.getConfiguration(),
                        visitor.visitElement(filter.getConstraint()));

                if(!expression.equals(trueValue))
                    conditions.add(expression);
            }
        }

        return SqlLeftJoin.leftJoin(request, scope, translatedGroupPattern, translatedPattern, conditions);
    }


    /**
     * Applies a MINUS pattern to the group pattern. Inside an EXISTS pattern, the solutions of both sides extend the
     * current solution the pattern is evaluated for: the right side is restricted to the solutions compatible with it
     * (see {@link #restrictToCurrentSolution}), and the sides share its variables (see
     * {@link #getCurrentSolutionBound}).
     *
     * @param pattern the MINUS pattern
     * @param translatedGroupPattern code of the patterns so far
     * @return the resulting intermediate code
     */
    private SqlIntercode translateMinus(Minus pattern, SqlIntercode translatedGroupPattern)
    {
        SqlIntercode minusPattern = restrictToCurrentGraph(restrictToCurrentSolution(visitElement(pattern.getPattern()),
                translatedGroupPattern.getVariableBindings(), Set.of(), false));

        /* NOTE: Inside GRAPH ?g, both sides are evaluated in the same graph, which the triples of both bind to the
         * graph variable, but the variable is no variable of their solutions and so does not count as shared.
         */
        Variable graph = getGraph() instanceof Variable variable ? variable : null;

        return SqlMinus.minus(request, translatedGroupPattern, minusPattern, graph, getCurrentSolutionBound());
    }


    /**
     * Applies a BIND to the group pattern. A variable the solutions bind already is not rebound: the value is bound to
     * a fresh variable merged into it, so it has to agree with the bound value where both are bound. The syntax allows
     * it only for a variable of the current solution of an EXISTS pattern, which the WHERE clause of a sub-select binds
     * for the projection assigning it (see {@link #getClauseVariables}); elsewhere in the pattern, the test compares
     * the assigned value with the current solution (see {@link #translateExists}).
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

        if(translatedGroupPattern.getVariableBindings().get(var) != null)
        {
            Variable value = createVariable(variablePrefix);

            return SqlMerge.create(request, var, value,
                    SqlBind.bind(request, value, expression, translatedGroupPattern));
        }

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
            SqlExpressionIntercode expression = SqlEffectiveBooleanValue.create(request.getConfiguration(),
                    visitor.visitElement(filter.getConstraint()));

            filterExpressions.add(expression);
        }

        return SqlFilter.filter(request, filterExpressions, groupPattern);
    }


    /**
     * Translates a procedure call evaluated laterally for every solution of {@code context}, filling in the default
     * values of omitted parameters; a call that takes no parameter value from {@code context} is evaluated once and
     * joined with it, because the database would otherwise run the function again for every solution. Inside an EXISTS
     * pattern, a parameter the context does not bind takes its value in the current solution the pattern is evaluated
     * for (see {@link #getOuterVariable}).
     *
     * @param procedureCallBase the procedure call
     * @param context solutions the call is evaluated for
     * @return the resulting intermediate code
     */
    private SqlIntercode translateProcedureCall(ProcedureCallBase procedureCallBase, SqlIntercode context)
    {
        if(!(context instanceof SqlEmptySolution) && !usesContext(procedureCallBase, context))
            return SqlJoin.join(request, context, translateProcedureCall(procedureCallBase, SqlEmptySolution.get()));

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
     * Tells whether a parameter of the procedure call takes its value from a variable of the context, or from a
     * variable of the current solution of the EXISTS pattern being translated, which every solution of the context
     * extends.
     *
     * @param procedureCall the procedure call
     * @param context solutions the call would be evaluated for
     * @return whether the value of a parameter is a variable that the context or the current solution binds
     */
    private boolean usesContext(ProcedureCallBase procedureCall, SqlIntercode context)
    {
        Set<Variable> variables = new HashSet<>(context.getVariableBindings().getVariables());

        if(scope.getCurrentSolution() != null)
            variables.addAll(scope.getCurrentSolution().getVariables());

        return procedureCall.getParameters().stream()
                .anyMatch(p -> p.getValue() instanceof VariableOrBlankNode v && variables.contains(getVariable(v)));
    }


    /**
     * Translates a SERVICE pattern: a service known to the configuration is translated in place, with its own
     * procedures and functions and over all its own mappings, which the dataset clauses of the query do not restrict,
     * as they would not restrict a call of the endpoint of that service; any other service becomes a
     * {@link SqlServiceStub} evaluated later against the remote endpoint, which knows the lateral joins of the
     * evaluations per graph it lies in (see {@link #enclosures}).
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
            // the mappings of the service come into effect, and with them the named graphs they enumerate
            List<QuadMapping> enclosingMappings = mappings;
            Map<Variable, SqlIntercode> enclosingGraphEnumerations = graphEnumerations;
            Map<Variable, SqlIntercode> enclosingNamedGraphs = namedGraphs;

            mappings = configuration.getMappings((Iri) name);
            graphEnumerations = new HashMap<>();
            namedGraphs = new HashMap<>();

            serviceRestrictions.add((Iri) name);
            graphRestrictions.add(null);

            Pattern pattern = service.getPattern();
            List<Pattern> patterns = pattern instanceof GroupGraph group ? group.getPatterns() : List.of(pattern);

            //FIXME: passing context in this way may cause conflicts with the standard
            SqlIntercode result = translatePatternList(patterns, context);

            graphRestrictions.pop();
            serviceRestrictions.pop();

            mappings = enclosingMappings;
            graphEnumerations = enclosingGraphEnumerations;
            namedGraphs = enclosingNamedGraphs;

            return result;
        }

        String serviceCode = (new ServiceTranslateVisitor()).getResultCode(service.getPattern());

        Map<String, Variable> serviceVariables = service.getPattern().getVariablesInScope().stream()
                .collect(toMap(e -> e.getName(), e -> getVariable(e)));

        /* NOTE: The members of an EXISTS pattern may refer to the current solution the pattern is evaluated for, which
         * is known only at their place, so a call cannot be evaluated for a context holding them; the call is evaluated
         * by itself and joined afterwards. With the reordering of calls, the join evaluates the call for the solutions
         * of its other members, which may refer to the graph of an evaluation per graph too, and so it does for a call
         * whose endpoint is named by a variable that the context does not bind, because only another member of the join
         * can name the endpoint then (see SqlServiceStub.isEvaluatedByJoin).
         */
        if(request.isServiceReorderEnabled() || scope.getCurrentSolution() != null
                || name instanceof Variable variable && context.getVariable(variable) == null)
        {
            SqlIntercode call = SqlServiceStub.create(request, name, serviceCode, serviceVariables,
                    SqlEmptySolution.get(), new StrBlankNodeInSegmentClass(--serviceId), service.isSilent(),
                    List.copyOf(enclosures));

            return SqlJoin.join(request, call, context);
        }
        else
        {
            return SqlServiceStub.create(request, name, serviceCode, serviceVariables, context,
                    new StrBlankNodeInSegmentClass(--serviceId), service.isSilent(), List.copyOf(enclosures));
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
                imcode = imcode.addExternalLimits(request.getConfiguration(), offset, limit, order);

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
            List<Column> values = resClass.toColumns(request, iri);

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
                else if(graphMap != null)
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
