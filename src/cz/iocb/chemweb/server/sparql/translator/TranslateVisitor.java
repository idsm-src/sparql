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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.db.IriNode;
import cz.iocb.chemweb.server.db.LanguageTaggedLiteral;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.ReferenceNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.SQLRuntimeException;
import cz.iocb.chemweb.server.db.TypedLiteral;
import cz.iocb.chemweb.server.db.postgresql.PostgresDatabase;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.error.MessageType;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.BuiltinTypes;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition.Direction;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
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
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlMinus;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlNoSolution;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlProcedureCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlQuery;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlSelect;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlUnion;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlValues;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;



public class TranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final int serviceContextLimit = 200;
    private static final int serviceResultLimit = 10000;

    private final Stack<VarOrIri> graphRestrictions = new Stack<>();
    private final List<TranslateMessage> messages;

    private final SparqlDatabaseConfiguration configuration;
    private final LinkedHashMap<String, UserIriClass> iriClasses;
    private final DatabaseSchema schema;
    private final LinkedHashMap<String, ProcedureDefinition> procedures;
    private final SSLContext sslContext;
    private final boolean evalSeriveces;

    private List<DataSet> datasets;
    private Prologue prologue;


    public TranslateVisitor(SparqlDatabaseConfiguration configuration, SSLContext sslContext,
            List<TranslateMessage> messages, boolean evalSeriveces)
    {
        this.configuration = configuration;
        this.iriClasses = configuration.getIriClasses();
        this.schema = configuration.getSchema();
        this.procedures = configuration.getProcedures();
        this.sslContext = sslContext;
        this.evalSeriveces = evalSeriveces;
        this.messages = messages;
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
    public SqlIntercode visit(Select select)
    {
        checkProjectionVariables(select);


        // translate the WHERE clause
        GraphPattern pattern = select.getPattern();

        if(select.getValues() != null)
        {
            /*
             * Quick (and dirty) fix to accept procedure calls with VALUES statements submitted by endpoints
             *
             * FIXME: This is not valid in general!
             */

            List<Pattern> patterns = new LinkedList<Pattern>();
            patterns.add(select.getValues());

            if(pattern instanceof GroupGraph)
                patterns.addAll(((GroupGraph) pattern).getPatterns());
            else
                patterns.add(pattern);

            pattern = new GroupGraph(patterns);
        }

        SqlIntercode translatedWhereClause = visitElement(pattern);


        // translate the GROUP BY clause
        List<Variable> groupByVariables = new LinkedList<Variable>();

        for(GroupCondition groupBy : select.getGroupByConditions())
        {
            if(groupBy.getExpression() instanceof Variable && groupBy.getVariable() == null)
            {
                groupByVariables.add((Variable) groupBy.getExpression());
            }
            else
            {
                Variable variable = groupBy.getVariable();

                if(variable == null)
                    variable = Variable.getNewVariable();

                groupByVariables.add(variable);
                String name = variable.getName();

                if(select.getPattern().getVariablesInScope().contains(name))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind,
                            groupBy.getVariable().getRange(), name));


                checkExpressionForUnGroupedSolutions(groupBy.getExpression());

                ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                        new SimpleVariableAccessor(translatedWhereClause.getVariables()), this);

                SqlExpressionIntercode expression = visitor.visitElement(groupBy.getExpression());

                //TODO: optimize based on the expression value

                translatedWhereClause = SqlBind.bind(name, expression, translatedWhereClause);
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

            LinkedHashMap<Variable, SqlExpressionIntercode> aggregations = new LinkedHashMap<>();

            for(Entry<Variable, BuiltInCallExpression> entry : rewriter.getAggregations().entrySet())
                aggregations.put(entry.getKey(), visitor.visitElement(entry.getValue()));

            SqlIntercode intercode = SqlAggregation.aggregate(groupByVariables, aggregations, translatedWhereClause);

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

                inScopeVariables.add(projection.getVariable().getName());

                translatedWhereClause = translateBind(bind, translatedWhereClause);
            }
        }


        // translate order by expressions
        LinkedHashMap<String, Direction> orderByVariables = new LinkedHashMap<String, Direction>();

        for(OrderCondition condition : orderByConditions)
        {
            if(condition.getExpression() instanceof Variable)
            {
                String varName = ((Variable) condition.getExpression()).getName();

                if(translatedWhereClause.getVariables().get(varName) != null)
                    orderByVariables.put(varName, condition.getDirection());
            }
            else
            {
                Variable variable = Variable.getNewVariable();
                orderByVariables.put(variable.getName(), condition.getDirection());

                Bind bind = new Bind(condition.getExpression(), variable);
                translatedWhereClause = translateBind(bind, translatedWhereClause);
            }
        }


        UsedVariables variables = new UsedVariables();
        LinkedHashSet<String> variablesInScope = new LinkedHashSet<String>();

        for(Projection projection : select.getProjections())
        {
            String variableName = projection.getVariable().getName();

            UsedVariable variable = translatedWhereClause.getVariables().get(variableName);

            if(variable != null)
                variables.add(variable);

            variablesInScope.add(variableName);
        }


        SqlSelect sqlSelect = new SqlSelect(variablesInScope, variables, translatedWhereClause, orderByVariables);


        if(select.isDistinct())
            sqlSelect.setDistinct(true);

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
        graphRestrictions.push(graph.getName());

        SqlIntercode translatedPattern = visitElement(graph.getPattern());

        graphRestrictions.pop();
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
        SqlIntercode translatedPattern = new SqlNoSolution();

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

        ArrayList<String> variablesInScope = new ArrayList<String>();
        UsedVariables usedVariables = new UsedVariables();


        ArrayList<Pair<String, Set<ResourceClass>>> typedVariables = new ArrayList<>();
        ArrayList<ArrayList<Pair<Node, ResourceClass>>> typedValuesList = new ArrayList<>();

        for(int j = 0; j < values.getValuesLists().size(); j++)
            typedValuesList.add(new ArrayList<Pair<Node, ResourceClass>>(size));


        for(int i = 0; i < values.getVariables().size(); i++)
        {
            String variable = values.getVariables().get(i).getName();

            if(!variablesInScope.contains(variable))
                variablesInScope.add(variable);
            else
                messages.add(new TranslateMessage(MessageType.repeatOfValuesVariable,
                        values.getVariables().get(i).getRange(), variable));


            Set<ResourceClass> valueClasses = new HashSet<ResourceClass>();
            typedVariables.add(new Pair<String, Set<ResourceClass>>(variable, valueClasses));


            int valueCount = 0;

            for(int j = 0; j < values.getValuesLists().size(); j++)
            {
                ValuesList valuesList = values.getValuesLists().get(j);
                Expression value = valuesList.getValues().get(i);
                ResourceClass valueType = null;

                if(value != null)
                {
                    valueCount++;

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

                        for(ResourceClass resClass : iriClasses.values())
                            if(resClass instanceof IriClass)
                                if(resClass.match((Node) value))
                                    valueType = resClass;
                    }
                    else
                    {
                        assert false;
                    }

                    valueClasses.add(valueType);
                }

                typedValuesList.get(j).add(new Pair<Node, ResourceClass>((Node) value, valueType));
            }


            if(valueCount > 0)
            {
                boolean canBeNull = valueCount < values.getValuesLists().size();
                UsedVariable usedVariable = new UsedVariable(variable, valueClasses, canBeNull);

                usedVariables.add(usedVariable);
            }
        }

        return new SqlValues(usedVariables, typedVariables, typedValuesList);
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


        PathTranslateVisitor pathVisitor = new PathTranslateVisitor(this, datasets);
        SqlIntercode translatedPattern = pathVisitor.visitElement(predicate, graph, subject, object);


        ArrayList<String> variablesInScope = new ArrayList<String>();

        if(graph instanceof Variable)
            variablesInScope.add(((Variable) graph).getName());

        if(subject instanceof Variable)
            variablesInScope.add(((Variable) subject).getName());

        if(predicate instanceof Variable)
            variablesInScope.add(((Variable) predicate).getName());

        if(object instanceof Variable)
            variablesInScope.add(((Variable) object).getName());


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
                        projection.getVariable().getRange(), projection.getVariable().toString()));

            vars.add(projection.getVariable().getName());
        }

        if(!select.isInAggregateMode())
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

        for(Projection projection : select.getProjections())
        {
            if(projection.getExpression() != null)
                checkExpressionForGroupedSolutions(projection.getExpression(), groupVars);
            else
                checkExpressionForGroupedSolutions(projection.getVariable(), groupVars);
        }

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

                if(!(arg instanceof VariableOrBlankNode))
                    return null;

                String name = ((VariableOrBlankNode) arg).getName();

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


    private SqlIntercode translatePatternList(List<Pattern> patterns)
    {
        SqlIntercode translatedGroupPattern = new SqlEmptySolution();

        LinkedList<Filter> filters = new LinkedList<>();
        LinkedList<String> inScopeVariables = new LinkedList<String>();

        for(Pattern pattern : patterns)
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

        SqlIntercode intercode = SqlLeftJoin.leftJoin(schema, translatedGroupPattern, translatedPattern, conditions);

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

        return SqlMinus.minus(translatedGroupPattern, minusPattern);
    }


    private SqlIntercode translateBind(Bind bind, SqlIntercode translatedGroupPattern)
    {
        String variableName = bind.getVariable().getName();

        checkExpressionForUnGroupedSolutions(bind.getExpression());

        ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                new SimpleVariableAccessor(translatedGroupPattern.getVariables()), this);

        SqlExpressionIntercode expression = visitor.visitElement(bind.getExpression());

        SqlIntercode intercode = SqlBind.bind(variableName, expression, translatedGroupPattern);
        return intercode;
    }


    private SqlIntercode translateSqlFilters(List<SqlExpressionIntercode> filterExpressions, SqlIntercode child)
    {
        if(child instanceof SqlNoSolution)
            return new SqlNoSolution();


        if(child instanceof SqlUnion)
        {
            SqlIntercode union = new SqlNoSolution();

            for(SqlIntercode subChild : ((SqlUnion) child).getChilds())
            {
                VariableAccessor accessor = new SimpleVariableAccessor(subChild.getVariables());

                List<SqlExpressionIntercode> optimizedExpressions = filterExpressions.stream()
                        .map(f -> SqlEffectiveBooleanValue.create(f.optimize(accessor))).collect(Collectors.toList());

                union = SqlUnion.union(union, translateSqlFilters(optimizedExpressions, subChild));
            }

            return union;
        }


        List<SqlExpressionIntercode> validExpressions = new LinkedList<SqlExpressionIntercode>();
        boolean isFalse = false;

        for(SqlExpressionIntercode expression : filterExpressions)
        {
            if(expression == SqlNull.get() || expression == SqlEffectiveBooleanValue.falseValue)
                isFalse = true;
            else if(expression instanceof SqlBinaryComparison
                    && ((SqlBinaryComparison) expression).isAlwaysFalseOrNull())
                isFalse = true;
            else if(expression != SqlEffectiveBooleanValue.trueValue)
                validExpressions.add(expression);
        }

        if(isFalse)
            return new SqlNoSolution();

        if(validExpressions.isEmpty())
            return child;

        return new SqlFilter(child, validExpressions);
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

        return translateSqlFilters(filterExpressions, groupPattern);
    }


    private SqlIntercode translateProcedureCall(ProcedureCallBase procedureCallBase, SqlIntercode context)
    {
        IRI procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = procedures.get(procedureName.getValue());
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
                    String variableName = ((VariableOrBlankNode) value).getName();
                    UsedVariable variable = contextVariables.get(variableName);

                    if(variable == null)
                    {
                        if(value instanceof Variable)
                            messages.add(new TranslateMessage(MessageType.unboundedVariableParameterValue,
                                    value.getRange(), parameter.getName().toString(prologue), variableName));
                        else if(value instanceof BlankNode)
                            messages.add(new TranslateMessage(MessageType.unboundedBlankNodeParameterValue,
                                    value.getRange(), parameter.getName().toString(prologue)));
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

        return SqlProcedureCall.create(procedureDefinition, parameterNodes, resultNodes, context);
    }


    private SqlIntercode translateService(Service service, SqlIntercode context)
    {
        ServiceTranslateVisitor visitor = new ServiceTranslateVisitor();
        List<String> serviceInScopeVars = visitor.visitElement(service.getPattern());
        String serviceCode = visitor.getResultCode();

        VarOrIri name = service.getName();

        if(context instanceof SqlNoSolution)
            return new SqlNoSolution();

        if(name instanceof Variable && context.getVariables().get(((Variable) name).getName()) == null)
            return new SqlNoSolution();


        /* create variable lists */
        List<String> contextVariables = context.getVariables().getValues().stream().map(v -> v.getName())
                .collect(Collectors.toList());

        List<String> mergedVariables = new LinkedList<String>();
        mergedVariables.addAll(contextVariables);
        serviceInScopeVars.stream().filter(v -> !contextVariables.contains(v)).forEach(v -> mergedVariables.add(v));

        List<String> sharedVariables = contextVariables.stream().filter(v -> serviceInScopeVars.contains(v))
                .collect(Collectors.toList());


        if(!evalSeriveces)
            return createEmptyServiceTranslatedSegment(serviceInScopeVars, context);


        Result result = null;

        try
        {
            /* evaluate context pattern */
            SqlSelect sqlSelect = new SqlSelect(contextVariables, context.getVariables(), context,
                    new LinkedHashMap<String, Direction>());
            sqlSelect.setLimit(BigInteger.valueOf(serviceContextLimit + 1));

            PostgresDatabase db = new PostgresDatabase(configuration.getConnectionPool());
            result = db.query(sqlSelect.translate());
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }

        if(result.size() > serviceContextLimit)
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


        for(Row row : result)
        {
            String endpoint = null;

            if(name instanceof IRI)
                endpoint = ((IRI) name).getValue();
            else if(row.get(((Variable) name).getName()) instanceof IriNode)
                endpoint = row.get(((Variable) name).getName()).getValue();


            /* build default result */
            ArrayList<Pair<Node, ResourceClass>> defaulResult = new ArrayList<>(mergedVariables.size());
            ArrayList<ArrayList<Pair<Node, ResourceClass>>> rowResults = new ArrayList<>();

            ArrayList<Pair<String, Set<ResourceClass>>> rowTypedVariables = new ArrayList<>();

            for(String var : mergedVariables)
                rowTypedVariables.add(new Pair<String, Set<ResourceClass>>(var, new HashSet<>()));


            for(int i = 0; i < mergedVariables.size(); i++)
            {
                RdfNode term = row.get(mergedVariables.get(i));
                Node node = null;

                if(term instanceof IriNode)
                    node = new IRI(term.getValue());
                else if(term instanceof LanguageTaggedLiteral)
                    node = new Literal(term.getValue(), ((LanguageTaggedLiteral) term).getLanguage());
                else if(term instanceof TypedLiteral)
                    node = new Literal(term.getValue(), new IRI(((TypedLiteral) term).getDatatype().toString()));
                else if(term instanceof ReferenceNode)
                    node = new BlankNode(term.getValue());

                if(node != null)
                {
                    ResourceClass resourceClass = getResourceClass(node);
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
                    RdfNode term = row.get(variable);

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
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();

                if(connection instanceof HttpsURLConnection && sslContext != null)
                    ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());

                connection.setRequestMethod("POST");
                connection.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                connection.setRequestProperty("accept", "application/sparql-results+xml");
                connection.setDoOutput(true);

                try(OutputStream out = connection.getOutputStream())
                {
                    out.write(("query=" + URLEncoder.encode(sparqlQueryBuilder.toString(), "UTF-8")).getBytes());
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

                        ResourceClass resourceClass = getResourceClass(node);
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


        UsedVariables usedVariables = new UsedVariables();

        for(int i = 0; i < mergedVariables.size(); i++)
        {
            final int fi = i;
            Set<ResourceClass> resourceClasses = typedVariables.get(i).getValue();
            boolean canBeNull = results.size() > results.stream().filter(l -> l.get(fi).getValue() != null).count();
            usedVariables.add(new UsedVariable(mergedVariables.get(i), resourceClasses, canBeNull));
        }

        if(results.isEmpty())
            return new SqlNoSolution();

        return new SqlValues(usedVariables, typedVariables, results);
    }


    private SqlIntercode createEmptyServiceTranslatedSegment(List<String> serviceInScopeVars, SqlIntercode context)
    {
        List<String> mergedVariables = context.getVariables().getValues().stream().map(v -> v.getName())
                .collect(Collectors.toList());
        serviceInScopeVars.stream().filter(v -> !mergedVariables.contains(v)).forEach(v -> mergedVariables.add(v));


        Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
        resourceClasses.addAll(BuiltinClasses.getClasses());

        UsedVariables usedVariables = new UsedVariables();

        context.getVariables().getValues().stream().filter(v -> !v.canBeNull()).forEach(v -> usedVariables.add(v));

        for(String var : mergedVariables)
            if(usedVariables.get(var) == null)
                usedVariables.add(new UsedVariable(var, resourceClasses, true));


        ArrayList<Pair<String, Set<ResourceClass>>> typedVariables = new ArrayList<>();

        for(String var : mergedVariables)
            typedVariables.add(new Pair<String, Set<ResourceClass>>(var, usedVariables.get(var).getClasses()));

        return new SqlValues(usedVariables, typedVariables, new ArrayList<>());
    }


    private ResourceClass getResourceClass(Node node)
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

            for(ResourceClass resClass : iriClasses.values())
                if(resClass instanceof IriClass)
                    if(resClass.match(node))
                        resourceClass = resClass;

            return resourceClass;
        }
        else if(node instanceof BlankNode)
        {
            return BuiltinClasses.strBlankNode;
        }

        return null;
    }


    public String translate(SelectQuery sparqlQuery) throws SQLException
    {
        try
        {
            SqlIntercode imcode = visitElement(sparqlQuery);

            return imcode.translate();
        }
        catch(SQLRuntimeException e)
        {
            throw(SQLException) e.getCause();
        }
    }


    public final LinkedHashMap<String, UserIriClass> getIriClasses()
    {
        return iriClasses;
    }


    public SparqlDatabaseConfiguration getConfiguration()
    {
        return configuration;
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
