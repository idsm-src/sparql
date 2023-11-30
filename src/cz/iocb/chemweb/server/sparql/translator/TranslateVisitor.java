package cz.iocb.chemweb.server.sparql.translator;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdStringType;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
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
import cz.iocb.chemweb.server.sparql.mapping.BlankNodeLiteral;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ResultDefinition;
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
import cz.iocb.chemweb.server.sparql.parser.model.expression.ExistsExpression;
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
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExists;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlIri;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlLiteral;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class TranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final int serviceRedirectLimit = 3;
    private static final int serviceContextLimit = 1000;
    private static final int serviceResultLimit = 100000;
    private static final String variablePrefix = "@additionalvar";

    private int variableId = 0;
    private int serviceId = 0;

    private final Stack<IRI> serviceRestrictions = new Stack<>();
    private final Stack<VarOrIri> graphRestrictions = new Stack<>();
    private final List<TranslateMessage> messages;

    private final SparqlDatabaseConfiguration configuration;
    private final List<UserIriClass> iriClasses;
    private final boolean evalSeriveces;

    private HashMap<String, List<Range>> variableOccurrences;
    private List<DataSet> datasets;
    private Prologue prologue;


    public TranslateVisitor(List<TranslateMessage> messages, boolean evalSeriveces)
    {
        Request request = Request.currentRequest();

        this.configuration = request.getConfiguration();
        this.iriClasses = configuration.getIriClasses();
        this.messages = messages;
        this.evalSeriveces = evalSeriveces;

        serviceRestrictions.add(configuration.getServiceIri());
        graphRestrictions.add(null);
    }


    @Override
    public SqlIntercode visit(SelectQuery selectQuery)
    {
        prologue = selectQuery.getPrologue();
        datasets = selectQuery.getSelect().getDataSets();

        Select select = selectQuery.getSelect();
        SqlIntercode translatedSelect = visitElement(select);
        List<String> variables = select.getVariablesInScope().stream().map(v -> v.getSqlName()).collect(toList());

        return new SqlQuery(variables, translatedSelect);
    }


    @Override
    public SqlIntercode visit(AskQuery askQuery)
    {
        prologue = askQuery.getPrologue();
        datasets = askQuery.getSelect().getDataSets();

        Variable variable = new Variable(null, "@ask");

        LinkedHashSet<String> variablesInScope = new LinkedHashSet<String>();
        variablesInScope.add(variable.getName());

        SqlIntercode translatedSelect = visitElement(askQuery.getSelect());
        SqlExpressionIntercode expression = SqlExists.create(false, translatedSelect, new UsedVariables());
        SqlIntercode bind = SqlBind.bind(variable.getSqlName(), expression, SqlEmptySolution.get());

        return new SqlQuery(variablesInScope, bind);
    }


    @Override
    public SqlIntercode visit(DescribeQuery describeQuery)
    {
        prologue = describeQuery.getPrologue();
        datasets = describeQuery.getSelect().getDataSets();

        Variable subject = new Variable(null, "@subject");
        Variable predicate = new Variable(null, "@predikate");
        Variable object = new Variable(null, "@object");

        Set<String> restrictions = new LinkedHashSet<String>();
        restrictions.add(subject.getSqlName());
        restrictions.add(predicate.getSqlName());
        restrictions.add(object.getSqlName());

        PathTranslateVisitor visitor = new PathTranslateVisitor(this, datasets);
        SqlIntercode select = visitElement(describeQuery.getSelect());
        List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

        for(VarOrIri resource : describeQuery.getResources())
        {
            if(resource instanceof IRI)
            {
                IRI iri = (IRI) resource;
                SqlExpressionIntercode expression = SqlIri.create(iri);

                SqlIntercode subjectPattern = visitor.translate(null, iri, predicate, object);
                subjectPattern = SqlBind.bind(subject.getSqlName(), expression, subjectPattern);
                unionList.add(subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, iri);
                objectPattern = SqlBind.bind(object.getSqlName(), expression, objectPattern);
                unionList.add(objectPattern);
            }
            else
            {
                Variable variable = (Variable) resource;
                SqlExpressionIntercode expression = SqlVariable.create(variable.getSqlName(), select.getVariables());

                SqlExpressionIntercode filter = SqlBuiltinCall.create("bound", false, asList(expression));
                SqlIntercode source = SqlFilter.filter(asList(filter), select);

                SqlIntercode subjectPattern = visitor.translate(null, variable, predicate, object);
                subjectPattern = SqlJoin.join(source, subjectPattern);
                subjectPattern = SqlBind.bind(subject.getSqlName(), expression, subjectPattern);
                unionList.add(subjectPattern);

                SqlIntercode objectPattern = visitor.translate(null, subject, predicate, variable);
                objectPattern = SqlJoin.join(source, objectPattern);
                objectPattern = SqlBind.bind(object.getSqlName(), expression, objectPattern);
                unionList.add(objectPattern);
            }
        }

        return new SqlQuery(restrictions, SqlUnion.union(unionList)).optimize();
    }


    @Override
    public SqlIntercode visit(ConstructQuery constructQuery)
    {
        prologue = constructQuery.getPrologue();
        datasets = constructQuery.getSelect().getDataSets();

        Select select = constructQuery.getSelect();
        SqlIntercode translatedSelect = visitElement(select);
        List<String> variables = select.getVariablesInScope().stream().map(v -> v.getSqlName()).collect(toList());

        return new SqlQuery(variables, translatedSelect);
    }


    @Override
    public SqlIntercode visit(Select select)
    {
        checkProjectionVariables(select);


        // translate the WHERE clause
        GraphPattern pattern = select.getPattern();

        if(select.getValues() != null && !select.isInAggregateMode())
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
                    variable = createVariable(variablePrefix);

                groupByVariables.add(variable.getSqlName());

                if(select.getPattern().getVariablesInScope().contains(variable))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind,
                            groupBy.getVariable().getRange(), variable.getName()));


                checkExpressionForUnGroupedSolutions(groupBy.getExpression());

                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                        translatedWhereClause.getVariables(), this);
                SqlExpressionIntercode expression = visitor.visitElement(groupBy.getExpression());

                //TODO: optimize based on the expression value

                translatedWhereClause = SqlBind.bind(variable.getSqlName(), expression, translatedWhereClause);
            }
        }


        List<Projection> projections = select.getProjections();
        List<OrderCondition> orderByConditions = select.getOrderByConditions();

        if(select.isInAggregateMode())
        {
            ExpressionAggregationRewriteVisitor rewriter = new ExpressionAggregationRewriteVisitor(this);

            List<Filter> havingConditions = select.getHavingConditions().stream()
                    .map(e -> new Filter(rewriter.visitElement(e))).collect(toList());


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


            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(translatedWhereClause.getVariables(),
                    this);

            LinkedHashMap<String, SqlExpressionIntercode> aggregations = new LinkedHashMap<>();

            for(Entry<Variable, BuiltInCallExpression> entry : rewriter.getAggregations().entrySet())
                aggregations.put(entry.getKey().getSqlName(), visitor.visitElement(entry.getValue()));

            SqlIntercode intercode = SqlAggregation.aggregate(groupByVariables, aggregations, translatedWhereClause);

            translatedWhereClause = intercode;


            // translate having as filters
            translatedWhereClause = translateFilters(havingConditions, translatedWhereClause);
        }


        // translate final values clause
        if(select.getValues() != null)
            translatedWhereClause = SqlJoin.join(translatedWhereClause, visitElement(select.getValues()));


        // translate projection expressions
        List<Variable> inScopeVariables = new LinkedList<Variable>(select.getPattern().getVariablesInScope());

        for(Projection projection : projections)
        {
            if(projection.getExpression() != null)
            {
                Bind bind = new Bind(projection.getExpression(), projection.getVariable());

                Variable variable = projection.getVariable();

                if(inScopeVariables.contains(variable))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind, bind.getVariable().getRange(),
                            variable.getName()));

                inScopeVariables.add(variable);

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
                Variable variable = createVariable(variablePrefix);
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


        if(select.isSubSelect() && getGraph() instanceof Variable)
        {
            String graphVariableName = ((Variable) getGraph()).getSqlName();

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
        SqlIntercode translatedGroupGraphPattern = translatePatternList(groupGraph.getPatterns(),
                SqlEmptySolution.get());

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
            graphVariable = createVariable("@graph");

        graphRestrictions.push(graphVariable);

        SqlIntercode translatedPattern = visitElement(graph.getPattern());

        graphRestrictions.pop();

        if(rename)
            translatedPattern = SqlMerge.create(((Variable) graph.getName()).getSqlName(),
                    ((Variable) graphVariable).getSqlName(), translatedPattern);


        if(graph.getName() instanceof Variable)
        {
            String varName = ((Variable) graph.getName()).getSqlName();
            UsedVariable variable = translatedPattern.getVariables().get(varName);

            if(variable == null || variable.canBeNull())
            {
                if(translatedPattern.isDeterministic())
                {
                    //TODO: can be ignored, if the graph variable is not required outside the graph pattern

                    List<List<Node>> values = new ArrayList<List<Node>>();

                    for(ConstantIriMapping g : configuration.getGraphs(getService()))
                        values.add(asList(g.getValue()));

                    translatedPattern = SqlJoin.join(translatedPattern, SqlValues.create(asList(varName), values));
                }
                else if(variable == null)
                {
                    List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

                    for(ConstantIriMapping g : configuration.getGraphs(getService()))
                        unionList.add(SqlBind.bind(varName, SqlIri.create((IRI) g.getValue()), translatedPattern));

                    translatedPattern = SqlUnion.union(unionList);
                }
                else
                {
                    List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

                    for(ConstantIriMapping g : configuration.getGraphs(getService()))
                        unionList.add(SqlJoin.join(translatedPattern,
                                SqlValues.create(asList(varName), asList(asList(g.getValue())))));

                    translatedPattern = SqlUnion.union(unionList);
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
        return SqlUnion.union(union.getPatterns().stream().map(p -> visitElement(p)).collect(toList()));
    }


    @Override
    public SqlIntercode visit(Values values)
    {
        ArrayList<String> variables = new ArrayList<String>();

        for(int i = 0; i < values.getVariables().size(); i++)
        {
            Variable variable = values.getVariables().get(i);

            if(variables.contains(variable.getName()))
                messages.add(new TranslateMessage(MessageType.repeatOfValuesVariable,
                        values.getVariables().get(i).getRange(), variable.getName()));

            variables.add(variable.getName());
        }

        List<List<Node>> lines = new ArrayList<List<Node>>(values.getValuesLists().size());

        for(ValuesList list : values.getValuesLists())
        {
            List<Node> line = new ArrayList<Node>(list.getValues().size());

            for(Expression expression : list.getValues())
                line.add((Node) expression);

            lines.add(line);
        }

        return SqlValues.create(values.getVariables().stream().map(v -> v.getSqlName()).collect(toList()), lines);
    }


    @Override
    public SqlIntercode visit(Triple triple)
    {
        Node graph = getGraph();
        Node subject = triple.getSubject();
        Verb predicate = triple.getPredicate();
        Node object = triple.getObject();

        PathTranslateVisitor pathVisitor = new PathTranslateVisitor(this, datasets);
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
            private boolean inAggregateFunction = false;

            @Override
            public Void visit(BuiltInCallExpression func)
            {
                boolean state = inAggregateFunction;
                inAggregateFunction |= func.isAggregateFunction();
                super.visit(func);
                inAggregateFunction = state;

                return null;
            }

            @Override
            public Void visit(Variable var)
            {
                String name = var.getName();

                if(!inAggregateFunction && !groupByVars.contains(name))
                    messages.add(
                            new TranslateMessage(MessageType.invalidVariableOutsideAggregate, var.getRange(), name));

                if(inAggregateFunction && groupByVars.contains(name))
                    messages.add(new TranslateMessage(MessageType.invalidVariableInAggregate, var.getRange(), name));

                return null;
            }

            @Override
            public Void visit(ExistsExpression expr)
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
                ProcedureDefinition definition = configuration.getProcedures(getService())
                        .get(procedureName.getValue());

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
                        if(configuration.getProcedures(getService()).get(iri.getValue()) != null)
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
            ProcedureDefinition definition = configuration.getProcedures(getService()).get(name.getValue());

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


    private SqlIntercode translatePatternList(List<Pattern> patterns, SqlIntercode base)
    {
        SqlIntercode translatedGroupPattern = base;

        LinkedList<Filter> filters = new LinkedList<>();
        LinkedList<Variable> inScopeVariables = new LinkedList<Variable>();

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

                    translatedPattern = translatePatternList(optionalPatterns, SqlEmptySolution.get());
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
                Variable variable = ((Bind) pattern).getVariable();

                if(inScopeVariables.contains(variable))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind,
                            ((Bind) pattern).getVariable().getRange(), variable.getName()));

                translatedGroupPattern = translateBind((Bind) pattern, translatedGroupPattern);
                inScopeVariables.add(variable);
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
                translatedGroupPattern = translateFilters(filters, translatedGroupPattern);
                filters.clear();

                translatedGroupPattern = translateService((Service) pattern, translatedGroupPattern);
                inScopeVariables.addAll(pattern.getVariablesInScope());
            }
            else
            {
                SqlIntercode translatedPattern = visitElement(pattern);

                translatedGroupPattern = SqlJoin.join(translatedGroupPattern, translatedPattern);
                inScopeVariables.addAll(pattern.getVariablesInScope());
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
            UsedVariables variables = SqlLeftJoin.getExpressionVariables(translatedGroupPattern.getVariables(),
                    translatedPattern.getVariables());

            for(Filter filter : optionalFilters)
            {
                checkExpressionForUnGroupedSolutions(filter.getConstraint());

                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(variables, this);
                SqlExpressionIntercode expression = SqlEffectiveBooleanValue
                        .create(visitor.visitElement(filter.getConstraint()));

                if(expression != SqlEffectiveBooleanValue.trueValue)
                    conditions.add(expression);
            }
        }

        return SqlLeftJoin.leftJoin(translatedGroupPattern, translatedPattern, conditions);
    }


    private SqlIntercode translateMinus(Minus pattern, SqlIntercode translatedGroupPattern)
    {
        SqlIntercode minusPattern = visitElement(pattern.getPattern());

        return SqlMinus.minus(translatedGroupPattern, minusPattern);
    }


    private SqlIntercode translateBind(Bind bind, SqlIntercode translatedGroupPattern)
    {
        String variableName = bind.getVariable().getSqlName();

        checkExpressionForUnGroupedSolutions(bind.getExpression());

        ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(translatedGroupPattern.getVariables(),
                this);
        SqlExpressionIntercode expression = visitor.visitElement(bind.getExpression());

        return SqlBind.bind(variableName, expression, translatedGroupPattern);
    }


    private SqlIntercode translateFilters(List<Filter> filters, SqlIntercode groupPattern)
    {
        if(filters.size() == 0)
            return groupPattern;


        List<SqlExpressionIntercode> filterExpressions = new ArrayList<SqlExpressionIntercode>(filters.size());

        for(Filter filter : filters)
        {
            checkExpressionForUnGroupedSolutions(filter.getConstraint());

            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(groupPattern.getVariables(), this);
            SqlExpressionIntercode expression = SqlEffectiveBooleanValue
                    .create(visitor.visitElement(filter.getConstraint()));

            filterExpressions.add(expression);
        }

        return SqlFilter.filter(filterExpressions, groupPattern);
    }


    private SqlIntercode translateProcedureCall(ProcedureCallBase procedureCallBase, SqlIntercode context)
    {
        IRI procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = configuration.getProcedures(getService())
                .get(procedureName.getValue());
        UsedVariables contextVariables = context.getVariables();


        /* check graph */

        if(getGraph() != null)
        {
            messages.add(new TranslateMessage(MessageType.procedureCallInsideGraph,
                    procedureCallBase.getProcedure().getRange(), procedureName.toString(prologue)));
        }


        /* check parameters */

        LinkedHashMap<ParameterDefinition, Node> parameterNodes = new LinkedHashMap<ParameterDefinition, Node>();

        for(ParameterDefinition parameter : procedureDefinition.getParameters())
            parameterNodes.put(parameter, null);

        for(Parameter parameter : procedureCallBase.getParameters())
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

        LinkedHashMap<ResultDefinition, VariableOrBlankNode> resultNodes = new LinkedHashMap<ResultDefinition, VariableOrBlankNode>();

        Set<String> used = new HashSet<String>(contextVariables.getNames());
        LinkedHashMap<Variable, Node> conditions = new LinkedHashMap<Variable, Node>();

        if(procedureCallBase instanceof ProcedureCall)
        {
            /* single-result procedure call */

            ResultDefinition resultDefinition = procedureDefinition.getResult(null);
            Node result = ((ProcedureCall) procedureCallBase).getResult();

            if(!(result instanceof VariableOrBlankNode) || used.contains(((VariableOrBlankNode) result).getSqlName()))
            {
                Variable fakeResult = createVariable(variablePrefix);
                conditions.put(fakeResult, result);
                result = fakeResult;
            }

            used.add(((VariableOrBlankNode) result).getSqlName());

            resultNodes.put(resultDefinition, (VariableOrBlankNode) result);
        }
        else
        {
            /* multi-result procedure call */

            MultiProcedureCall multiProcedureCall = (MultiProcedureCall) procedureCallBase;

            for(Parameter resultParameter : multiProcedureCall.getResults())
            {
                String parameterName = resultParameter.getName().getValue();
                ResultDefinition resultDefinition = procedureDefinition.getResult(parameterName);

                if(resultDefinition == null)
                {
                    messages.add(new TranslateMessage(MessageType.invalidResultPredicate,
                            resultParameter.getName().getRange(), resultParameter.getName().toString(prologue),
                            procedureCallBase.getProcedure().toString(prologue)));

                }
                else if(resultNodes.get(resultDefinition) != null)
                {
                    messages.add(new TranslateMessage(MessageType.repeatOfResultPredicate,
                            resultParameter.getName().getRange(), resultParameter.getName().toString(prologue)));
                }
                else
                {
                    Node result = resultParameter.getValue();

                    if(!(result instanceof VariableOrBlankNode)
                            || used.contains(((VariableOrBlankNode) result).getSqlName()))
                    {
                        Variable fakeResult = createVariable(variablePrefix);
                        conditions.put(fakeResult, result);
                        result = fakeResult;
                    }

                    used.add(((VariableOrBlankNode) result).getSqlName());

                    resultNodes.put(resultDefinition, (VariableOrBlankNode) result);
                }
            }
        }


        SqlIntercode intercode = SqlProcedureCall.create(procedureDefinition, parameterNodes, resultNodes, context);

        for(Entry<Variable, Node> entry : conditions.entrySet())
        {
            Variable fakeResult = entry.getKey();
            Node result = entry.getValue();

            VariableOrBlankNode var = null;

            if(result instanceof Literal)
            {
                var = createVariable(variablePrefix);
                intercode = SqlBind.bind(var.getSqlName(), SqlLiteral.create((Literal) result), intercode);
            }
            else if(result instanceof IRI)
            {
                var = createVariable(variablePrefix);
                intercode = SqlBind.bind(var.getSqlName(), SqlIri.create((IRI) result), intercode);
            }
            else //if(result instanceof VariableOrBlankNode)
            {
                var = (VariableOrBlankNode) result;
            }

            intercode = SqlMerge.create(var.getSqlName(), fakeResult.getSqlName(), intercode);
        }

        return intercode;
    }


    private SqlIntercode translateService(Service service, SqlIntercode context)
    {
        VarOrIri name = service.getName();

        if(context == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(name instanceof Variable && context.getVariables().get(((Variable) name).getSqlName()) == null)
            return SqlNoSolution.get();


        if(configuration.getServices().contains(name))
        {
            serviceRestrictions.add((IRI) name);
            graphRestrictions.add(null);

            SqlIntercode result = translatePatternList(((GroupGraph) service.getPattern()).getPatterns(), context);

            graphRestrictions.pop();
            serviceRestrictions.pop();

            return result;
        }


        ServiceTranslateVisitor visitor = new ServiceTranslateVisitor();
        String serviceCode = visitor.getResultCode(service.getPattern());

        /* create variable lists */

        LinkedHashSet<Variable> serviceInScopeVars = service.getPattern().getVariablesInScope();

        List<String> mergedVariables = new LinkedList<String>();
        List<String> sharedVariables = new LinkedList<String>();

        HashMap<String, Integer> serviceVariables = new HashMap<String, Integer>();
        HashSet<String> contextVariables = new HashSet<String>(context.getVariables().getNames());

        for(Variable var : serviceInScopeVars)
        {
            mergedVariables.add(var.getSqlName());
            serviceVariables.put(var.getName(), serviceVariables.size());

            if(contextVariables.contains(var.getSqlName()))
                sharedVariables.add(var.getSqlName());
        }

        contextVariables.stream().filter(v -> !mergedVariables.contains(v)).forEach(v -> mergedVariables.add(v));


        if(!evalSeriveces)
            return context;


        ArrayList<RdfNode[]> rows = new ArrayList<RdfNode[]>();
        HashMap<String, Integer> varIndexes = null;

        if(context == SqlEmptySolution.get())
        {
            rows.add(new RdfNode[0]);
            varIndexes = new HashMap<String, Integer>();
        }
        else
        {
            try
            {
                /* evaluate context pattern */

                SqlSelect sqlSelect = new SqlSelect(context.getVariables(), context, new HashSet<String>(),
                        new LinkedHashMap<String, Direction>());
                sqlSelect.setLimit(BigInteger.valueOf(serviceContextLimit + 1));
                SqlQuery query = new SqlQuery(contextVariables, sqlSelect);

                String code = query.optimize().translate();

                Request request = Request.currentRequest();

                try(Result result = new SelectResult(ResultType.SELECT, request.getStatement().executeQuery(code),
                        request.getBegin(), request.getTimeout()))
                {
                    varIndexes = result.getVariableIndexes();

                    while(result.next())
                    {
                        rows.add(result.getRow());

                        if(rows.size() > serviceContextLimit)
                            break;
                    }
                }
            }
            catch(SQLException e)
            {
                throw new SQLRuntimeException(e);
            }

            if(rows.size() > serviceContextLimit)
            {
                messages.add(new TranslateMessage(MessageType.serviceContextLimitExceeded, service.getRange()));
                return context;
            }
        }


        /* create result lists */

        ArrayList<List<Node>> results = new ArrayList<List<Node>>();

        BlankNodeClass blankNodeClass = new UserStrBlankNodeClass(--serviceId);
        int call = 0;

        for(RdfNode[] row : rows)
        {
            String endpoint = null;

            if(name instanceof IRI)
                endpoint = ((IRI) name).getValue();
            else if(row[varIndexes.get(((Variable) name).getSqlName())] instanceof IriNode)
                endpoint = row[varIndexes.get(((Variable) name).getSqlName())].getValue();
            else
                continue;


            /* build default result */

            ArrayList<Node> defaulResult = new ArrayList<Node>(mergedVariables.size());
            ArrayList<ArrayList<Node>> rowResults = new ArrayList<ArrayList<Node>>();

            for(int i = 0; i < mergedVariables.size(); i++)
            {
                String varName = mergedVariables.get(i);
                Integer idx = varIndexes.get(varName);
                Node node = null;

                if(idx != null)
                {
                    RdfNode term = row[varIndexes.get(varName)];

                    if(term instanceof IriNode)
                        node = new IRI(term.getValue());
                    else if(term instanceof LanguageTaggedLiteral)
                        node = new Literal(term.getValue(), ((LanguageTaggedLiteral) term).getLanguage());
                    else if(term instanceof TypedLiteral)
                        node = new Literal(term.getValue(), new IRI(((TypedLiteral) term).getDatatype().getValue()));
                    else if(term instanceof ReferenceNode)
                        node = new BlankNodeLiteral(term.getValue(), context.getVariables().get(varName).getClasses());
                }

                defaulResult.add(node);
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
                final int bnprefix = call++;

                DefaultHandler handler = new DefaultHandler()
                {
                    ArrayList<Node> result;
                    Integer varIndex;
                    StringBuilder data;
                    String datatype;
                    String lang;

                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes)
                            throws SAXException
                    {
                        if(qName.equalsIgnoreCase("result"))
                        {
                            result = new ArrayList<Node>(mergedVariables.size());
                            result.addAll(defaulResult);

                            if(rowResults.size() + results.size() >= serviceResultLimit)
                            {
                                limitExceeded[0] = true;
                                throw new SAXException();
                            }
                        }
                        else if(qName.equalsIgnoreCase("binding"))
                        {
                            String name = attributes.getValue("name");
                            varIndex = serviceVariables.get(name);
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
                        if(qName.equalsIgnoreCase("result") && result != null)
                            rowResults.add(result);

                        if(varIndex == null)
                            return;

                        Node node = null;

                        if(qName.equalsIgnoreCase("uri"))
                            node = new IRI(data.toString());
                        else if(qName.equalsIgnoreCase("bnode"))
                            node = new BlankNodeLiteral(bnprefix + "r" + data.toString(), blankNodeClass);
                        else if(!qName.equalsIgnoreCase("literal"))
                            return;
                        else if(lang != null)
                            node = new Literal(data.toString(), lang);
                        else if(datatype != null)
                            node = new Literal(data.toString(), new IRI(datatype));
                        else
                            node = new Literal(data.toString(), xsdStringType);


                        if(defaulResult.get(varIndex) instanceof BlankNodeLiteral)
                            result = null;

                        if(result != null)
                        {
                            if(result.get(varIndex) != null && !result.get(varIndex).equals(node))
                                throw new SAXException("unexpected value");

                            result.set(varIndex, node);
                        }
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

                results.addAll(rowResults);
            }
            catch(ParserConfigurationException | SAXException | IOException e)
            {
                if(limitExceeded[0])
                {
                    messages.add(new TranslateMessage(MessageType.serviceResultLimitExceeded, service.getRange()));
                    return context;
                }

                e.printStackTrace();

                if(service.isSilent())
                {
                    results.add(defaulResult);
                }
                else
                {
                    messages.add(new TranslateMessage(MessageType.badServiceEndpoint, service.getRange(), endpoint));
                    return context;
                }
            }
        }

        return SqlValues.create(mergedVariables, results);
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

                // implicit projection variables are added with null range by parser,
                // they are ignored here due to engines that replace blank nodes by variables
                if(variable.getRange() != null)
                    occurrences.add(variable.getRange());

                return defaultResult();
            }
        }.visitElement(sparqlQuery);


        try
        {
            SqlQuery imcode = (SqlQuery) visitElement(sparqlQuery);

            if(imcode == null)
                return null;

            return (SqlQuery) imcode.optimize();
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


    public List<TranslateMessage> getMessages()
    {
        return messages;
    }
}
