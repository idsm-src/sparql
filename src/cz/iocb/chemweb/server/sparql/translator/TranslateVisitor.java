package cz.iocb.chemweb.server.sparql.translator;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.SQLRuntimeException;
import cz.iocb.chemweb.server.sparql.engine.IriNode;
import cz.iocb.chemweb.server.sparql.engine.LanguageTaggedLiteral;
import cz.iocb.chemweb.server.sparql.engine.RdfNode;
import cz.iocb.chemweb.server.sparql.engine.ReferenceNode;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.engine.Result;
import cz.iocb.chemweb.server.sparql.engine.Result.ResultType;
import cz.iocb.chemweb.server.sparql.engine.SelectResult;
import cz.iocb.chemweb.server.sparql.engine.TypedLiteral;
import cz.iocb.chemweb.server.sparql.error.MessageType;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.BuiltinTypes;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.model.AskQuery;
import cz.iocb.chemweb.server.sparql.parser.model.ConstructQuery;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.DescribeQuery;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition.Direction;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Query;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
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
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values.ValuesList;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;
import cz.iocb.chemweb.server.sparql.translator.expression.ExpressionAggregationRewriteVisitor;
import cz.iocb.chemweb.server.sparql.translator.expression.ExpressionTranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.expression.LeftJoinVariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.expression.SimpleVariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlAggregation;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlEmptySolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlFilter;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlJoin;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlLeftJoin;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlMerge;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlMinus;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlNoSolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlProcedureCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlQuery;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlSelect;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlUnion;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlValues;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExists;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlIri;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class TranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final int serviceRedirectLimit = 3;
    private static final int serviceContextLimit = 1000;
    private static final int serviceResultLimit = 100000;
    private static final String variablePrefix = "@additionalvar";
    private int variableId = 0;
    private int serviceId = 0;

    private final Stack<VarOrIri> graphRestrictions = new Stack<>();
    private final List<TranslateMessage> messages;

    private final SparqlDatabaseConfiguration configuration;
    private final List<UserIriClass> iriClasses;
    private final DatabaseSchema schema;
    private final Request request;
    private final boolean evalSeriveces;

    private HashMap<String, List<Range>> variableOccurrences;
    private List<DataSet> datasets;
    private Prologue prologue;


    public TranslateVisitor(Request request, List<TranslateMessage> messages, boolean evalSeriveces)
    {
        this.configuration = request.getConfiguration();
        this.iriClasses = configuration.getIriClasses();
        this.schema = configuration.getDatabaseSchema();
        this.request = request;
        this.messages = messages;
        this.evalSeriveces = evalSeriveces;
    }


    @Override
    public SqlIntercode visit(SelectQuery selectQuery)
    {
        prologue = selectQuery.getPrologue();
        datasets = selectQuery.getSelect().getDataSets();

        SqlIntercode translatedSelect = visitElement(selectQuery.getSelect());
        return new SqlQuery(selectQuery.getSelect().getVariablesInScope(), translatedSelect);
    }


    @Override
    public SqlIntercode visit(AskQuery askQuery)
    {
        prologue = askQuery.getPrologue();
        datasets = askQuery.getSelect().getDataSets();

        Variable variable = new Variable("@ask");

        LinkedHashSet<String> variablesInScope = new LinkedHashSet<String>();
        variablesInScope.add(variable.getName());

        SqlIntercode translatedSelect = visitElement(askQuery.getSelect());
        VariableAccessor variableAccessor = new SimpleVariableAccessor(new UsedVariables());
        SqlExpressionIntercode expression = SqlExists.create(false, translatedSelect, variableAccessor);
        SqlIntercode bind = SqlBind.bind(variable.getSqlName(), expression, SqlEmptySolution.get());

        return new SqlQuery(variablesInScope, bind);
    }


    @Override
    public SqlIntercode visit(DescribeQuery describeQuery)
    {
        prologue = describeQuery.getPrologue();
        datasets = describeQuery.getSelect().getDataSets();

        Variable subject = new Variable("@subject");
        Variable predicate = new Variable("@predikate");
        Variable object = new Variable("@object");

        HashSet<String> restrictions = new LinkedHashSet<String>();
        restrictions.add(subject.getSqlName());
        restrictions.add(predicate.getSqlName());
        restrictions.add(object.getSqlName());

        PathTranslateVisitor visitor = new PathTranslateVisitor(request, this, datasets);
        SqlIntercode result = SqlNoSolution.get();
        SqlIntercode select = visitElement(describeQuery.getSelect());

        for(VarOrIri resource : describeQuery.getResources())
        {
            if(resource instanceof IRI)
            {
                IRI iri = (IRI) resource;
                SqlExpressionIntercode expression = SqlIri.create(iri, request);

                SqlIntercode subjectPattern = visitor.translate(null, iri, predicate, object);
                subjectPattern = SqlBind.bind(subject.getSqlName(), expression, subjectPattern);
                result = SqlUnion.union(result, subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, iri);
                objectPattern = SqlBind.bind(object.getSqlName(), expression, objectPattern);
                result = SqlUnion.union(result, objectPattern);
            }
            else
            {
                Variable variable = (Variable) resource;
                SqlExpressionIntercode expression = SqlVariable.create(variable.getSqlName(),
                        new SimpleVariableAccessor(select.getVariables()));

                SqlExpressionIntercode filter = SqlBuiltinCall.create("bound", false, Arrays.asList(expression));
                SqlIntercode source = SqlFilter.filter(Arrays.asList(filter), select);

                SqlIntercode subjectPattern = visitor.translate(null, variable, predicate, object);
                subjectPattern = SqlJoin.join(schema, source, subjectPattern);
                subjectPattern = SqlBind.bind(subject.getSqlName(), expression, subjectPattern);
                result = SqlUnion.union(result, subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, variable);
                objectPattern = SqlJoin.join(schema, source, objectPattern);
                objectPattern = SqlBind.bind(object.getSqlName(), expression, objectPattern);
                result = SqlUnion.union(result, objectPattern);
            }
        }

        return new SqlQuery(restrictions, result).optimize(request);
    }


    @Override
    public SqlIntercode visit(ConstructQuery constructQuery)
    {
        prologue = constructQuery.getPrologue();
        datasets = constructQuery.getSelect().getDataSets();

        SqlIntercode translatedSelect = visitElement(constructQuery.getSelect());
        return new SqlQuery(constructQuery.getSelect().getVariablesInScope(), translatedSelect);
    }


    @Override
    public SqlIntercode visit(Select select)
    {
        checkProjectionVariables(select);


        // translate the WHERE clause
        GraphPattern pattern = select.getPattern();

        if(select.getValues() != null)
        {
            List<Pattern> patterns = new LinkedList<Pattern>();

            for(Pattern subpattern : ((GroupGraph) pattern).getPatterns())
            {
                if(subpattern instanceof ProcedureCallBase)
                {
                    List<Variable> variables = new LinkedList<Variable>();
                    boolean[] mask = new boolean[select.getValues().getVariables().size()];

                    for(Parameter par : ((ProcedureCallBase) subpattern).getParameters())
                    {
                        if(par.getValue() instanceof Variable)
                        {
                            Variable variable = (Variable) par.getValue();
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

        if(select.getValues() != null)
            translatedWhereClause = SqlJoin.join(schema, translatedWhereClause, visitElement(select.getValues()));


        // translate the GROUP BY clause
        HashSet<String> groupByVariables = new HashSet<String>();

        for(GroupCondition groupBy : select.getGroupByConditions())
        {
            if(groupBy.getExpression() instanceof Variable && groupBy.getVariable() == null)
            {
                groupByVariables.add(((Variable) groupBy.getExpression()).getSqlName());
            }
            else
            {
                Variable variable = groupBy.getVariable();

                if(variable == null)
                    variable = new Variable(variablePrefix + variableId++);

                groupByVariables.add(variable.getSqlName());

                if(select.getPattern().getVariablesInScope().contains(variable.getName()))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind,
                            groupBy.getVariable().getRange(), variable.getName()));


                checkExpressionForUnGroupedSolutions(groupBy.getExpression());

                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                        new SimpleVariableAccessor(translatedWhereClause.getVariables()), this);

                SqlExpressionIntercode expression = visitor.visitElement(groupBy.getExpression());

                //TODO: optimize based on the expression value

                translatedWhereClause = SqlBind.bind(variable.getSqlName(), expression, translatedWhereClause);
            }
        }


        List<Projection> projections = select.getProjections();
        List<OrderCondition> orderByConditions = select.getOrderByConditions();

        if(select.isInAggregateMode())
        {
            ExpressionAggregationRewriteVisitor rewriter = new ExpressionAggregationRewriteVisitor();

            List<Filter> havingConditions = select.getHavingConditions().stream()
                    .map(e -> new Filter(rewriter.visitElement(e))).collect(Collectors.toList());


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


            for(BuiltInCallExpression expression : rewriter.getAggregations().values())
            {
                if(expression.getArguments().size() > 0)
                {
                    new ElementVisitor<Void>()
                    {
                        @Override
                        public Void visit(BuiltInCallExpression call)
                        {
                            if(call.isAggregateFunction())
                                messages.add(
                                        new TranslateMessage(MessageType.nestedAggregateFunction, call.getRange()));

                            return null;
                        }
                    }.visitElement(expression.getArguments().get(0));
                }
            }


            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                    new SimpleVariableAccessor(translatedWhereClause.getVariables()), this);

            LinkedHashMap<String, SqlExpressionIntercode> aggregations = new LinkedHashMap<>();

            for(Entry<Variable, BuiltInCallExpression> entry : rewriter.getAggregations().entrySet())
                aggregations.put(entry.getKey().getSqlName(), visitor.visitElement(entry.getValue()));

            SqlIntercode intercode = SqlAggregation.aggregate(groupByVariables, aggregations, translatedWhereClause,
                    null);

            translatedWhereClause = intercode;


            // translate having as filters
            translatedWhereClause = translateFilters(havingConditions, translatedWhereClause);
        }


        // translate projection expressions
        List<String> inScopeVariables = new LinkedList<String>(select.getPattern().getVariablesInScope());

        for(Projection projection : projections)
        {
            if(projection.getExpression() != null)
            {
                Bind bind = new Bind(projection.getExpression(), projection.getVariable());

                String variableName = projection.getVariable().getName();

                if(inScopeVariables.contains(variableName))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind, bind.getVariable().getRange(),
                            variableName));

                inScopeVariables.add(variableName);

                translatedWhereClause = translateBind(bind, translatedWhereClause);
            }
        }


        // translate order by expressions
        LinkedHashMap<String, Direction> orderByVariables = new LinkedHashMap<String, Direction>();

        for(OrderCondition condition : orderByConditions)
        {
            if(condition.getExpression() instanceof Variable)
            {
                String varName = ((Variable) condition.getExpression()).getSqlName();

                if(translatedWhereClause.getVariables().get(varName) != null)
                    orderByVariables.put(varName, condition.getDirection());
            }
            else
            {
                Variable variable = new Variable(variablePrefix + variableId++);
                String varName = variable.getSqlName();

                Bind bind = new Bind(condition.getExpression(), variable);
                translatedWhereClause = translateBind(bind, translatedWhereClause);

                if(translatedWhereClause.getVariables().get(varName) != null)
                    orderByVariables.put(varName, condition.getDirection());
            }
        }


        UsedVariables variables = new UsedVariables();

        for(Projection projection : select.getProjections())
        {
            String variableName = projection.getVariable().getSqlName();

            UsedVariable variable = translatedWhereClause.getVariables().get(variableName);

            if(variable != null)
                variables.add(variable);
        }


        if(select.isSubSelect() && graphRestrictions.size() > 0 && graphRestrictions.peek() instanceof Variable)
        {
            String graphVariableName = ((Variable) graphRestrictions.peek()).getSqlName();

            UsedVariable variable = translatedWhereClause.getVariables().get(graphVariableName);

            if(variable != null)
                variables.add(variable);
        }


        HashSet<String> distinctVariables = new HashSet<String>();

        if(select.isDistinct())
            for(Projection projection : select.getProjections())
                distinctVariables.add(projection.getVariable().getSqlName());

        SqlSelect sqlSelect = new SqlSelect(variables, translatedWhereClause, distinctVariables, orderByVariables);


        if(select.getLimit() != null)
            sqlSelect.setLimit(select.getLimit());

        if(select.getOffset() != null)
            sqlSelect.setOffset(select.getOffset());


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
            graphVariable = new Variable("@graph" + ((Variable) graphVariable).getName());

        graphRestrictions.push(graphVariable);

        SqlIntercode translatedPattern = visitElement(graph.getPattern());

        graphRestrictions.pop();

        if(rename)
            translatedPattern = SqlMerge.create(((Variable) graph.getName()).getSqlName(),
                    ((Variable) graphVariable).getSqlName(), translatedPattern, false);


        if(graph.getName() instanceof Variable)
        {
            String varName = ((Variable) graph.getName()).getSqlName();
            UsedVariable variable = translatedPattern.getVariables().get(varName);

            if(variable == null || variable.canBeNull())
            {
                if(translatedPattern.isDeterministic())
                {
                    //TODO: can be ignored, if the graph variable is not required outside the graph pattern

                    ArrayList<Pair<String, Set<ResourceClass>>> typedVariables = new ArrayList<>();
                    ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList = new ArrayList<>();

                    Set<ResourceClass> valueClasses = new HashSet<ResourceClass>();
                    typedVariables.add(new Pair<String, Set<ResourceClass>>(varName, valueClasses));

                    for(ConstantIriMapping g : configuration.getGraphs())
                    {
                        valueClasses.add(g.getIriClass());

                        ArrayList<Pair<Node, ResourceClass>> list = new ArrayList<Pair<Node, ResourceClass>>();
                        list.add(new Pair<Node, ResourceClass>(g.getValue(), g.getIriClass()));
                        typedValuesList.add(list);
                    }

                    translatedPattern = SqlJoin.join(schema, translatedPattern,
                            SqlValues.create(typedVariables, typedValuesList));
                }
                else if(variable == null)
                {
                    SqlIntercode innerPattern = translatedPattern;
                    translatedPattern = SqlNoSolution.get();

                    for(ConstantIriMapping g : configuration.getGraphs())
                        translatedPattern = SqlUnion.union(translatedPattern,
                                SqlBind.bind(varName, SqlIri.create((IRI) g.getValue(), request), innerPattern));
                }
                else
                {
                    SqlIntercode innerPattern = translatedPattern;
                    translatedPattern = SqlNoSolution.get();

                    for(ConstantIriMapping g : configuration.getGraphs())
                    {
                        ArrayList<Pair<String, Set<ResourceClass>>> typedVariables = new ArrayList<>();
                        ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList = new ArrayList<>();

                        Set<ResourceClass> valueClasses = new HashSet<ResourceClass>();
                        typedVariables.add(new Pair<String, Set<ResourceClass>>(varName, valueClasses));

                        valueClasses.add(g.getIriClass());

                        ArrayList<Pair<Node, ResourceClass>> list = new ArrayList<Pair<Node, ResourceClass>>();
                        list.add(new Pair<Node, ResourceClass>(g.getValue(), g.getIriClass()));
                        typedValuesList.add(list);

                        translatedPattern = SqlUnion.union(translatedPattern,
                                SqlJoin.join(schema, innerPattern, SqlValues.create(typedVariables, typedValuesList)));
                    }
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
        SqlIntercode translatedPattern = SqlNoSolution.get();

        for(GraphPattern pattern : union.getPatterns())
        {
            SqlIntercode translated = visitElement(pattern);
            translatedPattern = SqlUnion.union(translatedPattern, translated);
        }

        return translatedPattern;
    }


    @Override
    public SqlIntercode visit(Values values)
    {
        final int size = values.getVariables().size();

        ArrayList<String> variables = new ArrayList<String>();
        ArrayList<Pair<String, Set<ResourceClass>>> typedVariables = new ArrayList<>();
        ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList = new ArrayList<>();

        for(int j = 0; j < values.getValuesLists().size(); j++)
            typedValuesList.add(new ArrayList<Pair<Node, ResourceClass>>(size));


        for(int i = 0; i < values.getVariables().size(); i++)
        {
            Variable variable = values.getVariables().get(i);

            if(!variables.contains(variable.getName()))
                variables.add(variable.getName());
            else
                messages.add(new TranslateMessage(MessageType.repeatOfValuesVariable,
                        values.getVariables().get(i).getRange(), variable.getName()));


            Set<ResourceClass> valueClasses = new HashSet<ResourceClass>();
            typedVariables.add(new Pair<String, Set<ResourceClass>>(variable.getSqlName(), valueClasses));


            for(int j = 0; j < values.getValuesLists().size(); j++)
            {
                ValuesList valuesList = values.getValuesLists().get(j);
                Expression value = valuesList.getValues().get(i);
                ResourceClass valueType = null;

                if(value != null)
                {
                    if(value instanceof Literal)
                    {
                        Literal literal = (Literal) value;

                        if(literal.getLanguageTag() != null)
                        {
                            valueType = LangStringConstantTagClass.get(literal.getLanguageTag());
                        }
                        else
                        {
                            valueType = BuiltinClasses.unsupportedLiteral;

                            if(literal.getValue() != null)
                            {
                                IRI iri = literal.getTypeIri();

                                if(iri != null)
                                    for(LiteralClass literalClass : BuiltinClasses.getLiteralClasses())
                                        if(literalClass.getTypeIri().equals(iri))
                                            valueType = literalClass;

                                if(valueType == xsdDateTime)
                                    valueType = DateTimeConstantZoneClass
                                            .get(DateTimeConstantZoneClass.getZone(literal));

                                if(valueType == xsdDate)
                                    valueType = DateConstantZoneClass.get(DateConstantZoneClass.getZone(literal));
                            }
                        }
                    }
                    else if(value instanceof IRI)
                    {
                        valueType = BuiltinClasses.unsupportedIri;

                        for(UserIriClass resClass : iriClasses)
                        {
                            if(resClass.match((Node) value, request))
                            {
                                valueType = resClass;
                                break;
                            }
                        }
                    }
                    else
                    {
                        assert false;
                    }

                    valueClasses.add(valueType);
                }

                typedValuesList.get(j).add(new Pair<Node, ResourceClass>((Node) value, valueType));
            }
        }


        return SqlValues.create(typedVariables, typedValuesList);
    }


    @Override
    public SqlIntercode visit(Triple triple)
    {
        Node graph = null;
        Node subject = triple.getSubject();
        Verb predicate = triple.getPredicate();
        Node object = triple.getObject();


        if(!graphRestrictions.isEmpty())
            graph = graphRestrictions.peek();


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

    /**************************************************************************/

    private void checkProjectionVariables(Select select)
    {
        HashSet<String> vars = new HashSet<>();

        for(Projection projection : select.getProjections())
        {
            if(vars.contains(projection.getVariable().getName()))
                messages.add(new TranslateMessage(MessageType.repeatOfProjectionVariable,
                        projection.getVariable().getRange(), projection.getVariable().getName()));

            vars.add(projection.getVariable().getName());
        }

        if(!select.isInAggregateMode())
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

        for(Projection projection : select.getProjections())
        {
            if(projection.getExpression() != null)
                checkExpressionForGroupedSolutions(projection.getExpression(), groupVars);
            else
                checkExpressionForGroupedSolutions(projection.getVariable(), groupVars);
        }


        for(Projection projection : select.getProjections())
            if(projection.getExpression() != null)
                groupVars.add(projection.getVariable().getName());

        for(OrderCondition orderCondition : select.getOrderByConditions())
            checkExpressionForGroupedSolutions(orderCondition.getExpression(), groupVars);
    }


    private void checkExpressionForGroupedSolutions(Expression expresion, HashSet<String> groupByVars)
    {
        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(BuiltInCallExpression func)
            {
                if(!func.isAggregateFunction() || func.getArguments().isEmpty())
                    return null;

                Expression arg = func.getArguments().get(0);

                if(!(arg instanceof Variable))
                    return null;

                String name = ((Variable) arg).getName();

                if(groupByVars.contains(name))
                    messages.add(new TranslateMessage(MessageType.invalidVariableInAggregate, arg.getRange(), name));

                return null;
            }

            @Override
            public Void visit(Variable var)
            {
                if(!groupByVars.contains(var.getName()))
                    messages.add(new TranslateMessage(MessageType.invalidVariableOutsideAggregate, var.getRange(),
                            var.getName()));

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
                if(call.isAggregateFunction())
                    messages.add(new TranslateMessage(MessageType.invalidContextOfAggregate, call.getRange()));

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


    public List<Pattern> assembleProcedureCalls(List<Pattern> patterns)
    {
        HashSet<Pattern> callPatterns = new HashSet<Pattern>();
        HashMap<Node, List<Parameter>> parameterNodeMap = new HashMap<Node, List<Parameter>>();
        HashMap<Node, List<Parameter>> resultNodeMap = new HashMap<Node, List<Parameter>>();

        HashMap<Node, HashSet<Range>> parameterNodeOccurences = new HashMap<Node, HashSet<Range>>();
        HashMap<Node, HashSet<Range>> resultNodeOccurences = new HashMap<Node, HashSet<Range>>();


        for(Pattern pattern : patterns)
        {
            if(!(pattern instanceof Triple))
                continue;

            Triple triple = (Triple) pattern;
            Verb predicate = triple.getPredicate();

            if(predicate instanceof IRI)
            {
                IRI procedureName = (IRI) predicate;
                ProcedureDefinition definition = configuration.getProcedures().get(procedureName.getValue());

                if(definition != null)
                {
                    callPatterns.add(triple);

                    Node parameterNode = triple.getObject();

                    if(parameterNode instanceof VariableOrBlankNode)
                    {
                        parameterNodeMap.put(parameterNode, new ArrayList<Parameter>());

                        if(!parameterNodeOccurences.containsKey(parameterNode)
                                && !resultNodeOccurences.containsKey(parameterNode))
                            parameterNodeOccurences.put(parameterNode, new LinkedHashSet<Range>());
                        else
                            messages.add(new TranslateMessage(MessageType.reuseOfParameterNode,
                                    parameterNode.getRange(), procedureName.toString(prologue)));
                    }
                    else
                    {
                        messages.add(new TranslateMessage(MessageType.invalidProcedureCallObject,
                                parameterNode.getRange(), procedureName.toString(prologue)));
                    }


                    if(!definition.isSimple())
                    {
                        Node resultNode = triple.getSubject();

                        if(resultNode instanceof VariableOrBlankNode)
                        {
                            resultNodeMap.put(resultNode, new ArrayList<Parameter>());

                            if(!parameterNodeOccurences.containsKey(resultNode)
                                    && !resultNodeOccurences.containsKey(resultNode))
                                resultNodeOccurences.put(resultNode, new LinkedHashSet<Range>());
                            else
                                messages.add(new TranslateMessage(MessageType.reuseOfResultNode, resultNode.getRange(),
                                        procedureName.toString(prologue)));
                        }
                        else
                        {
                            messages.add(new TranslateMessage(MessageType.invalidMultiProcedureCallSubject,
                                    resultNode.getRange(), procedureName.toString(prologue)));
                        }
                    }
                }
            }
            else
            {
                new ElementVisitor<Void>()
                {
                    @Override
                    public Void visit(IRI iri)
                    {
                        //TODO: could be supported in a future version
                        if(configuration.getProcedures().get(iri.getValue()) != null)
                            messages.add(new TranslateMessage(MessageType.invalidProcedureCallPropertyPathCombinaion,
                                    iri.getRange()));

                        return null;
                    }
                }.visitElement(predicate);
            }
        }



        List<Pattern> resultPatterns = new ArrayList<Pattern>(patterns);

        Iterator<Pattern> iterator = resultPatterns.iterator();

        while(iterator.hasNext())
        {
            Pattern pattern = iterator.next();

            if(!(pattern instanceof Triple))
                continue;

            Triple triple = (Triple) pattern;

            if(callPatterns.contains(triple))
                continue;


            List<Parameter> parameters = parameterNodeMap.get(triple.getSubject());

            if(parameters != null)
            {
                iterator.remove();
                parameterNodeOccurences.get(triple.getSubject()).add(triple.getSubject().getRange());

                if(triple.getPredicate() instanceof IRI)
                    parameters.add(new Parameter((IRI) triple.getPredicate(), triple.getObject()));
                else
                    messages.add(new TranslateMessage(MessageType.invalidProcedureParameterValue,
                            triple.getPredicate().getRange())); //TODO: could be supported in a future version
            }


            List<Parameter> results = resultNodeMap.get(triple.getSubject());

            if(results != null)
            {
                iterator.remove();
                resultNodeOccurences.get(triple.getSubject()).add(triple.getSubject().getRange());

                if(triple.getPredicate() instanceof IRI)
                    results.add(new Parameter((IRI) triple.getPredicate(), triple.getObject()));
                else
                    messages.add(new TranslateMessage(MessageType.invalidProcedureResultValue,
                            triple.getPredicate().getRange())); //TODO: could be supported in a future version
            }
        }


        for(Pattern pattern : resultPatterns)
        {
            if(pattern instanceof Triple && !callPatterns.contains(pattern))
            {
                Node object = ((Triple) pattern).getObject();

                if(object instanceof BlankNode)
                {
                    if(parameterNodeOccurences.containsKey(object))
                        messages.add(new TranslateMessage(MessageType.invalidParameterBlankNodeOccurence,
                                object.getRange(), ((BlankNode) object).getName()));

                    if(resultNodeOccurences.containsKey(object))
                        messages.add(new TranslateMessage(MessageType.invalidResultBlankNodeOccurence,
                                object.getRange(), ((BlankNode) object).getName()));
                }
            }
        }


        for(Entry<Node, HashSet<Range>> entry : parameterNodeOccurences.entrySet())
        {
            if(entry.getKey() instanceof Variable)
            {
                String variable = ((Variable) entry.getKey()).getName();

                for(Range occurrence : variableOccurrences.get(variable))
                    if(occurrence != entry.getKey().getRange() && !entry.getValue().contains(occurrence))
                        messages.add(new TranslateMessage(MessageType.invalidParameterVariableOccurence, occurrence,
                                variable));
            }
        }


        for(Entry<Node, HashSet<Range>> entry : resultNodeOccurences.entrySet())
        {
            if(entry.getKey() instanceof Variable)
            {
                String variable = ((Variable) entry.getKey()).getName();

                for(Range occurrence : variableOccurrences.get(variable))
                    if(occurrence != entry.getKey().getRange() && !entry.getValue().contains(occurrence))
                        messages.add(
                                new TranslateMessage(MessageType.invalidResultVariableOccurence, occurrence, variable));
            }
        }


        ListIterator<Pattern> listIterator = resultPatterns.listIterator();

        while(listIterator.hasNext())
        {
            Pattern pattern = listIterator.next();

            if(!callPatterns.contains(pattern))
                continue;

            Triple triple = (Triple) pattern;

            IRI name = (IRI) triple.getPredicate();
            ProcedureDefinition definition = configuration.getProcedures().get(name.getValue());

            List<Parameter> parameters = parameterNodeMap.get(triple.getObject());

            if(parameters == null)
                parameters = new ArrayList<Parameter>();


            if(definition.isSimple())
            {
                ProcedureCall call = new ProcedureCall(triple.getSubject(), name, parameters);
                listIterator.set(call);
            }
            else
            {
                List<Parameter> results = resultNodeMap.get(triple.getSubject());

                if(results == null)
                    results = new ArrayList<Parameter>();

                MultiProcedureCall call = new MultiProcedureCall(results, name, parameters);
                listIterator.set(call);
            }
        }


        return resultPatterns;
    }


    private SqlIntercode translatePatternList(List<Pattern> patterns)
    {
        SqlIntercode translatedGroupPattern = SqlEmptySolution.get();

        LinkedList<Filter> filters = new LinkedList<>();
        LinkedList<String> inScopeVariables = new LinkedList<String>();

        for(Pattern pattern : assembleProcedureCalls(patterns))
        {
            if(pattern instanceof Optional)
            {
                GraphPattern optionalPattern = ((Optional) pattern).getPattern();

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
                inScopeVariables.addAll(pattern.getVariablesInScope());
            }
            else if(pattern instanceof Minus)
            {
                translatedGroupPattern = translateMinus((Minus) pattern, translatedGroupPattern);
            }
            else if(pattern instanceof Bind)
            {
                String variableName = ((Bind) pattern).getVariable().getName();

                if(inScopeVariables.contains(variableName))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind,
                            ((Bind) pattern).getVariable().getRange(), variableName));

                translatedGroupPattern = translateBind((Bind) pattern, translatedGroupPattern);
                inScopeVariables.add(variableName);
            }
            else if(pattern instanceof Filter)
            {
                filters.add((Filter) pattern);
            }
            else if(pattern instanceof ProcedureCallBase)
            {
                translatedGroupPattern = translateProcedureCall((ProcedureCallBase) pattern, translatedGroupPattern);
                inScopeVariables.addAll(pattern.getVariablesInScope());
            }
            else if(pattern instanceof Service)
            {
                translatedGroupPattern = translateService((Service) pattern, translatedGroupPattern);
                inScopeVariables.addAll(pattern.getVariablesInScope());
            }
            else
            {
                SqlIntercode translatedPattern = visitElement(pattern);

                translatedGroupPattern = SqlJoin.join(schema, translatedGroupPattern, translatedPattern);
                inScopeVariables.addAll(pattern.getVariablesInScope());
            }
        }

        return translateFilters(filters, translatedGroupPattern);
    }


    private SqlIntercode translateLeftJoin(SqlIntercode translatedGroupPattern, SqlIntercode translatedPattern,
            LinkedList<Filter> optionalFilters)
    {
        List<SqlExpressionIntercode> conditions = null;

        if(!optionalFilters.isEmpty())
        {
            VariableAccessor variableAccessor = new LeftJoinVariableAccessor(translatedGroupPattern.getVariables(),
                    translatedPattern.getVariables());
            conditions = translateLeftJoinFilters(optionalFilters, variableAccessor);

            if(conditions == null)
                return translatedGroupPattern;
        }

        if(conditions == null)
            conditions = new LinkedList<SqlExpressionIntercode>();

        SqlIntercode intercode = SqlLeftJoin.leftJoin(schema, translatedGroupPattern, translatedPattern, conditions,
                null);

        return intercode;
    }


    List<SqlExpressionIntercode> translateLeftJoinFilters(LinkedList<Filter> optionalFilters,
            VariableAccessor variableAccessor)
    {
        List<SqlExpressionIntercode> expressions = new LinkedList<SqlExpressionIntercode>();
        boolean isFalse = false;

        for(Filter filter : optionalFilters)
        {
            checkExpressionForUnGroupedSolutions(filter.getConstraint());

            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(variableAccessor, this);

            SqlExpressionIntercode expression = visitor.visitElement(filter.getConstraint());
            expression = SqlEffectiveBooleanValue.create(expression);

            if(expression == SqlNull.get() || expression == SqlEffectiveBooleanValue.falseValue)
                isFalse = true;
            else if(expression instanceof SqlBinaryComparison
                    && ((SqlBinaryComparison) expression).isAlwaysFalseOrNull())
                isFalse = true;
            else if(expression != SqlEffectiveBooleanValue.trueValue)
                expressions.add(expression);
        }

        if(isFalse)
            return null;

        return expressions;
    }


    private SqlIntercode translateMinus(Minus pattern, SqlIntercode translatedGroupPattern)
    {
        SqlIntercode minusPattern = visitElement(pattern.getPattern());

        return SqlMinus.minus(translatedGroupPattern, minusPattern, null);
    }


    private SqlIntercode translateBind(Bind bind, SqlIntercode translatedGroupPattern)
    {
        String variableName = bind.getVariable().getSqlName();

        checkExpressionForUnGroupedSolutions(bind.getExpression());

        ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                new SimpleVariableAccessor(translatedGroupPattern.getVariables()), this);

        SqlExpressionIntercode expression = visitor.visitElement(bind.getExpression());

        SqlIntercode intercode = SqlBind.bind(variableName, expression, translatedGroupPattern);
        return intercode;
    }


    private SqlIntercode translateFilters(List<Filter> filters, SqlIntercode groupPattern)
    {
        if(filters.size() == 0)
            return groupPattern;


        List<SqlExpressionIntercode> filterExpressions = new LinkedList<SqlExpressionIntercode>();

        for(Filter filter : filters)
        {
            checkExpressionForUnGroupedSolutions(filter.getConstraint());

            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                    new SimpleVariableAccessor(groupPattern.getVariables()), this);

            SqlExpressionIntercode expression = visitor.visitElement(filter.getConstraint());
            expression = SqlEffectiveBooleanValue.create(expression);

            filterExpressions.add(expression);
        }

        return SqlFilter.filter(filterExpressions, groupPattern);
    }


    private SqlIntercode translateProcedureCall(ProcedureCallBase procedureCallBase, SqlIntercode context)
    {
        IRI procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = configuration.getProcedures().get(procedureName.getValue());
        UsedVariables contextVariables = context.getVariables();


        /* check graph */

        if(!graphRestrictions.isEmpty())
        {
            messages.add(new TranslateMessage(MessageType.procedureCallInsideGraph,
                    procedureCallBase.getProcedure().getRange(), procedureName.toString(prologue)));
        }


        /* check paramaters */

        LinkedHashMap<ParameterDefinition, Node> parameterNodes = new LinkedHashMap<ParameterDefinition, Node>();

        for(ParameterDefinition parameter : procedureDefinition.getParameters())
            parameterNodes.put(parameter, null);


        ArrayList<String> variablesInScope = new ArrayList<String>();

        for(ProcedureCall.Parameter parameter : procedureCallBase.getParameters())
        {
            String parameterName = parameter.getName().getValue();
            ParameterDefinition parameterDefinition = procedureDefinition.getParameter(parameterName);

            if(parameterDefinition == null)
            {
                messages.add(new TranslateMessage(MessageType.invalidParameterPredicate, parameter.getName().getRange(),
                        parameter.getName().toString(prologue), procedureCallBase.getProcedure().toString(prologue)));
            }
            else if(parameterNodes.get(parameterDefinition) != null)
            {
                messages.add(new TranslateMessage(MessageType.repeatOfParameterPredicate,
                        parameter.getName().getRange(), parameter.getName().toString(prologue)));
            }
            else
            {
                Node value = parameter.getValue();

                if(value instanceof VariableOrBlankNode)
                {
                    String variableName = ((VariableOrBlankNode) value).getSqlName();
                    UsedVariable variable = contextVariables.get(variableName);

                    if(variable == null)
                    {
                        String parName = parameter.getName().toString(prologue);

                        if(value instanceof Variable)
                            messages.add(new TranslateMessage(MessageType.unboundedVariableParameterValue,
                                    value.getRange(), parName, ((Variable) value).getName()));
                        else if(value instanceof BlankNode)
                            messages.add(new TranslateMessage(MessageType.unboundedBlankNodeParameterValue,
                                    value.getRange(), parName));
                    }
                }

                parameterNodes.put(parameterDefinition, value);
            }


            if(parameter.getValue() instanceof Variable)
                variablesInScope.add(((Variable) parameter.getValue()).getName());
        }


        for(Entry<ParameterDefinition, Node> entry : parameterNodes.entrySet())
        {
            Node parameterValue = entry.getValue();

            if(parameterValue == null)
            {
                ParameterDefinition parameterDefinition = entry.getKey();

                if(parameterDefinition.getDefaultValue() != null)
                    entry.setValue(parameterDefinition.getDefaultValue());
                else
                    messages.add(new TranslateMessage(MessageType.missingParameterPredicate,
                            procedureCallBase.getProcedure().getRange(),
                            new IRI(parameterDefinition.getParamName()).toString(prologue),
                            procedureCallBase.getProcedure().toString(prologue)));
            }
        }


        /* check results */

        LinkedHashMap<ResultDefinition, Node> resultNodes = new LinkedHashMap<ResultDefinition, Node>();


        if(procedureCallBase instanceof ProcedureCall)
        {
            /* single-result procedure call */

            ResultDefinition resultDefinition = procedureDefinition.getResult(null);
            Node result = ((ProcedureCall) procedureCallBase).getResult();
            resultNodes.put(resultDefinition, result);

            if(result instanceof Variable)
                variablesInScope.add(((Variable) result).getName());
        }
        else
        {
            /* multi-result procedure call */

            MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;

            for(Parameter result : multiProcedureCall.getResults())
            {
                String parameterName = result.getName().getValue();
                ResultDefinition resultDefinition = procedureDefinition.getResult(parameterName);

                if(resultDefinition == null)
                {
                    messages.add(new TranslateMessage(MessageType.invalidResultPredicate, result.getName().getRange(),
                            result.getName().toString(prologue), procedureCallBase.getProcedure().toString(prologue)));

                }
                else if(resultNodes.get(resultDefinition) != null)
                {
                    messages.add(new TranslateMessage(MessageType.repeatOfResultPredicate, result.getName().getRange(),
                            result.getName().toString(prologue)));
                }
                else
                {
                    resultNodes.put(resultDefinition, result.getValue());
                }


                if(result.getValue() instanceof Variable)
                    variablesInScope.add(((Variable) result.getValue()).getName());
            }
        }

        return SqlProcedureCall.create(procedureDefinition, parameterNodes, resultNodes, context, request, null);
    }


    private SqlIntercode translateService(Service service, SqlIntercode context)
    {
        ServiceTranslateVisitor visitor = new ServiceTranslateVisitor();
        HashSet<String> serviceInScopeVars = visitor.visitElement(service.getPattern());
        String serviceCode = visitor.getResultCode();

        VarOrIri name = service.getName();

        if(context == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(name instanceof Variable && context.getVariables().get(((Variable) name).getSqlName()) == null)
            return SqlNoSolution.get();


        /* create variable lists */
        HashSet<String> contextVariables = new HashSet<String>(context.getVariables().getNames());

        List<String> mergedVariables = new LinkedList<String>();
        mergedVariables.addAll(contextVariables);
        serviceInScopeVars.stream().filter(v -> !contextVariables.contains(v)).forEach(v -> mergedVariables.add(v));

        List<String> sharedVariables = contextVariables.stream().filter(v -> serviceInScopeVars.contains(v))
                .collect(Collectors.toList());


        if(!evalSeriveces)
            return createEmptyServiceTranslatedSegment(serviceInScopeVars, context);


        ArrayList<RdfNode[]> rows = new ArrayList<RdfNode[]>();
        HashMap<String, Integer> varIndexes = null;

        try
        {
            /* evaluate context pattern */
            SqlSelect sqlSelect = new SqlSelect(context.getVariables(), context, new HashSet<String>(),
                    new LinkedHashMap<String, Direction>());
            sqlSelect.setLimit(BigInteger.valueOf(serviceContextLimit + 1));
            SqlQuery query = new SqlQuery(contextVariables, sqlSelect);

            String code = query.optimize(request).translate();

            try(Result result = new SelectResult(ResultType.SELECT, request.getStatement().executeQuery(code)))
            {
                varIndexes = result.getVariableIndexes();

                while(result.next())
                    rows.add(result.getRow());
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }


        if(rows.size() > serviceContextLimit)
        {
            messages.add(new TranslateMessage(MessageType.serviceContextLimitExceeded, service.getRange()));
            return createEmptyServiceTranslatedSegment(serviceInScopeVars, context);
        }


        /* create result lists */
        ArrayList<Pair<String, Set<ResourceClass>>> typedVariables = new ArrayList<>();

        for(String var : mergedVariables)
            typedVariables.add(new Pair<String, Set<ResourceClass>>(var, new HashSet<>()));

        ArrayList<ArrayList<Pair<Node, ResourceClass>>> results = new ArrayList<>();
        Pair<Node, ResourceClass> emptyResult = new Pair<Node, ResourceClass>(null, null);


        for(RdfNode[] row : rows)
        {
            String endpoint = null;

            if(name instanceof IRI)
                endpoint = ((IRI) name).getValue();
            else if(row[varIndexes.get(((Variable) name).getSqlName())] instanceof IriNode)
                endpoint = row[varIndexes.get(((Variable) name).getSqlName())].getValue();


            /* build default result */
            BlankNodeClass blankNodeClass = new UserStrBlankNodeClass(--serviceId);
            ArrayList<Pair<Node, ResourceClass>> defaulResult = new ArrayList<>(mergedVariables.size());
            ArrayList<ArrayList<Pair<Node, ResourceClass>>> rowResults = new ArrayList<>();

            ArrayList<Pair<String, Set<ResourceClass>>> rowTypedVariables = new ArrayList<>();

            for(String var : mergedVariables)
                rowTypedVariables.add(new Pair<String, Set<ResourceClass>>(var, new HashSet<>()));


            for(int i = 0; i < mergedVariables.size(); i++)
            {
                Integer idx = varIndexes.get(mergedVariables.get(i));
                Node node = null;

                if(idx != null)
                {
                    RdfNode term = row[varIndexes.get(mergedVariables.get(i))];

                    if(term instanceof IriNode)
                        node = new IRI(term.getValue());
                    else if(term instanceof LanguageTaggedLiteral)
                        node = new Literal(term.getValue(), ((LanguageTaggedLiteral) term).getLanguage());
                    else if(term instanceof TypedLiteral)
                        node = new Literal(term.getValue(), new IRI(((TypedLiteral) term).getDatatype().getValue()));
                    else if(term instanceof ReferenceNode)
                        node = new BlankNode(term.getValue());
                }

                if(node != null)
                {
                    ResourceClass resourceClass = getResourceClass(node, blankNodeClass);
                    typedVariables.get(i).getValue().add(resourceClass);
                    defaulResult.add(new Pair<>(node, resourceClass));
                }
                else
                {
                    defaulResult.add(emptyResult);
                }
            }


            /* build service query */
            StringBuilder sparqlQueryBuilder = new StringBuilder();
            sparqlQueryBuilder.append("select * where ");
            sparqlQueryBuilder.append(serviceCode);

            if(!sharedVariables.isEmpty())
            {
                sparqlQueryBuilder.append("values (");

                for(String var : sharedVariables)
                    sparqlQueryBuilder.append(" ?").append(var);

                sparqlQueryBuilder.append(") {(");

                for(String variable : sharedVariables)
                {
                    RdfNode term = row[varIndexes.get(variable)];

                    if(term != null && (term instanceof IriNode || term.isLiteral()))
                        sparqlQueryBuilder.append(term).append(" ");
                    else
                        sparqlQueryBuilder.append("undef ");
                }

                sparqlQueryBuilder.append(")}");
            }


            boolean[] limitExceeded = { false };

            try
            {
                /* process service query */
                HttpURLConnection connection = null;
                String url = endpoint;

                for(int i = 0; i <= serviceRedirectLimit && url != null; i++)
                {
                    connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                    connection.setRequestProperty("accept", "application/sparql-results+xml");
                    connection.setDoOutput(true);

                    try(OutputStream out = connection.getOutputStream())
                    {
                        out.write(("query=" + URLEncoder.encode(sparqlQueryBuilder.toString(), "UTF-8")).getBytes());
                    }

                    url = connection.getHeaderField("Location");
                }

                if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    throw new IOException(connection.getResponseMessage());


                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();

                DefaultHandler handler = new DefaultHandler()
                {
                    ArrayList<Pair<Node, ResourceClass>> result;
                    int varIndex;
                    StringBuilder data;
                    String datatype;
                    String lang;

                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes)
                            throws SAXException
                    {
                        if(qName.equalsIgnoreCase("result"))
                        {
                            result = new ArrayList<Pair<Node, ResourceClass>>(mergedVariables.size());
                            result.addAll(defaulResult);
                            rowResults.add(result);

                            if(rowResults.size() + results.size() > serviceResultLimit)
                            {
                                limitExceeded[0] = true;
                                throw new SAXException();
                            }
                        }
                        else if(qName.equalsIgnoreCase("binding"))
                        {
                            varIndex = mergedVariables.indexOf(attributes.getValue("name"));
                        }
                        else if(qName.equalsIgnoreCase("literal"))
                        {
                            lang = attributes.getValue("xml:lang");
                            datatype = attributes.getValue("datatype");
                            data = new StringBuilder();
                        }
                        else if(qName.equalsIgnoreCase("uri") || qName.equalsIgnoreCase("bnode"))
                        {
                            data = new StringBuilder();
                        }
                    }

                    @Override
                    public void endElement(String uri, String localName, String qName) throws SAXException
                    {
                        if(varIndex == -1)
                            return;

                        Node node = null;

                        if(qName.equalsIgnoreCase("uri"))
                            node = new IRI(data.toString());
                        else if(qName.equalsIgnoreCase("bnode"))
                            node = new BlankNode(data.toString());
                        else if(!qName.equalsIgnoreCase("literal"))
                            return;
                        else if(lang != null)
                            node = new Literal(data.toString(), lang);
                        else if(datatype != null)
                            node = new Literal(data.toString(), new IRI(datatype));
                        else
                            node = new Literal(data.toString(), BuiltinTypes.xsdStringIri);

                        if(result.get(varIndex).getKey() != null && !result.get(varIndex).getKey().equals(node))
                            throw new SAXException("unexpected value");

                        ResourceClass resourceClass = getResourceClass(node, blankNodeClass);
                        rowTypedVariables.get(varIndex).getValue().add(resourceClass);
                        result.set(varIndex, new Pair<Node, ResourceClass>(node, resourceClass));
                    }

                    @Override
                    public void characters(char ch[], int start, int length)
                    {
                        if(data != null)
                            data.append(new String(ch, start, length));
                    }
                };

                try(InputStream input = connection.getInputStream())
                {
                    saxParser.parse(input, handler);
                }


                /* merge results */
                for(int i = 0; i < rowTypedVariables.size(); i++)
                    typedVariables.get(i).getValue().addAll(rowTypedVariables.get(i).getValue());

                results.addAll(rowResults);
            }
            catch(ParserConfigurationException | SAXException | IOException e)
            {
                if(limitExceeded[0])
                {
                    messages.add(new TranslateMessage(MessageType.serviceResultLimitExceeded, service.getRange()));
                    return createEmptyServiceTranslatedSegment(serviceInScopeVars, context);
                }

                e.printStackTrace();

                if(service.isSilent())
                {
                    results.add(defaulResult);
                }
                else
                {
                    messages.add(new TranslateMessage(MessageType.badServiceEndpoint, service.getRange(), endpoint));
                    return createEmptyServiceTranslatedSegment(serviceInScopeVars, context);
                }
            }
        }


        return SqlValues.create(typedVariables, results);
    }


    private SqlIntercode createEmptyServiceTranslatedSegment(HashSet<String> serviceInScopeVars, SqlIntercode context)
    {
        ArrayList<Pair<String, Set<ResourceClass>>> typedVariables = new ArrayList<>();
        ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList = new ArrayList<>();

        Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
        resourceClasses.addAll(BuiltinClasses.getClasses());

        for(String var : serviceInScopeVars)
        {
            typedVariables.add(new Pair<String, Set<ResourceClass>>(var, resourceClasses));
            typedValuesList.add(null);
        }

        return SqlJoin.join(schema, context, SqlValues.create(typedVariables, new ArrayList<>()));
    }


    private ResourceClass getResourceClass(Node node, ResourceClass blankNodeClass)
    {
        if(node instanceof Literal)
        {
            Literal literal = (Literal) node;

            if(literal.getLanguageTag() != null)
                LangStringConstantTagClass.get(literal.getLanguageTag());

            ResourceClass resourceClass = BuiltinClasses.unsupportedLiteral;

            for(LiteralClass literalClass : BuiltinClasses.getLiteralClasses())
                if(literalClass.getTypeIri().equals(literal.getTypeIri()))
                    resourceClass = literalClass;

            if(resourceClass == xsdDateTime)
                resourceClass = DateTimeConstantZoneClass.get(DateTimeConstantZoneClass.getZone(literal));

            if(resourceClass == xsdDate)
                resourceClass = DateConstantZoneClass.get(DateConstantZoneClass.getZone(literal));

            return resourceClass;
        }
        else if(node instanceof IRI)
        {
            ResourceClass resourceClass = BuiltinClasses.unsupportedIri;

            for(UserIriClass resClass : iriClasses)
            {
                if(resClass.match(node, request))
                {
                    resourceClass = resClass;
                    break;
                }
            }

            return resourceClass;
        }
        else if(node instanceof BlankNode)
        {
            return blankNodeClass;
        }

        return null;
    }


    public SqlQuery translate(Query sparqlQuery) throws SQLException
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

                occurrences.add(variable.getRange());

                return defaultResult();
            }
        }.visitElement(sparqlQuery);


        try
        {
            SqlQuery imcode = (SqlQuery) visitElement(sparqlQuery);
            return (SqlQuery) imcode.optimize(request);
        }
        catch(SQLRuntimeException e)
        {
            throw(SQLException) e.getCause();
        }
    }


    public final List<UserIriClass> getIriClasses()
    {
        return iriClasses;
    }


    public Request getRequest()
    {
        return request;
    }


    public Prologue getPrologue()
    {
        return prologue;
    }


    public List<TranslateMessage> getMessages()
    {
        return messages;
    }
}
