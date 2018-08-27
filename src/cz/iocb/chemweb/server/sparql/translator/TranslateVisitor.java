package cz.iocb.chemweb.server.sparql.translator;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.ConstantMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.ParametrisedMapping;
import cz.iocb.chemweb.server.sparql.mapping.QuadMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DatePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringPatternClassWithConstantTag;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
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
import cz.iocb.chemweb.server.sparql.procedure.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.procedure.ResultDefinition;
import cz.iocb.chemweb.server.sparql.translator.GraphOrServiceRestriction.RestrictionType;
import cz.iocb.chemweb.server.sparql.translator.error.ErrorType;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateException;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.translator.expression.ExpressionTranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.expression.LeftJoinVariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.expression.SimpleVariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;
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
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlTableAccess;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlUnion;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlValues;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;



public class TranslateVisitor extends ElementVisitor<TranslatedSegment>
{
    private final Stack<List<DataSet>> selectDatasets = new Stack<>();
    private final Stack<GraphOrServiceRestriction> graphOrServiceRestrictions = new Stack<>();

    private final List<TranslateException> exceptions = new LinkedList<TranslateException>();
    private final List<TranslateException> warnings = new LinkedList<TranslateException>();

    private final SparqlDatabaseConfiguration configuration;
    private final LinkedHashMap<String, UserIriClass> iriClasses;
    private final List<QuadMapping> mappings;
    private final DatabaseSchema schema;
    private final LinkedHashMap<String, ProcedureDefinition> procedures;

    private Prologue prologue;


    public TranslateVisitor(SparqlDatabaseConfiguration configuration)
    {
        this.configuration = configuration;
        this.iriClasses = configuration.getIriClasses();
        this.mappings = configuration.getMappings();
        this.schema = configuration.getSchema();
        this.procedures = configuration.getProcedures();
    }


    @Override
    public TranslatedSegment visit(SelectQuery selectQuery)
    {
        Prologue original = prologue;
        prologue = selectQuery.getPrologue();

        TranslatedSegment translatedSelect = visitElement(selectQuery.getSelect());
        SqlQuery intercode = new SqlQuery(translatedSelect.getVariablesInScope(), translatedSelect.getIntercode());

        prologue = original;

        return new TranslatedSegment(translatedSelect.getVariablesInScope(), intercode);
    }


    @Override
    public TranslatedSegment visit(Select select)
    {
        /*
         * Quick (and dirty) fix to accept procedure calls with VALUES statements submitted by endpoints
         *
         * FIXME: This is not valid in general!
         */
        if(select.getValues() != null)
            ((GroupGraph) select.getPattern()).getPatterns().add(0, select.getValues());


        checkProjectionVariables(select);

        // register information about datasets used in this SELECT (sub)query
        selectDatasets.push(select.getDataSets()); // TODO: datasets are not supported

        // translate the WHERE clause
        TranslatedSegment translatedWhereClause = visitElement(select.getPattern());

        if(!select.getGroupByConditions().isEmpty())
            translatedWhereClause = translateBindInGroupBy(select.getGroupByConditions(), translatedWhereClause);



        UsedVariables variables = new UsedVariables();
        ArrayList<String> variablesInScope = null;

        if(select.getProjections().isEmpty())
        {
            //TODO: check whether star can be used

            for(String variableName : translatedWhereClause.getVariablesInScope())
            {
                UsedVariable variable = translatedWhereClause.getIntercode().getVariables().get(variableName);

                if(variable != null)
                    variables.add(variable);
            }

            variablesInScope = translatedWhereClause.getVariablesInScope();
        }
        else
        {
            variablesInScope = new ArrayList<String>();

            for(Projection projection : select.getProjections())
            {
                String variableName = projection.getVariable().getName();

                if(projection.getExpression() != null)
                {
                    //TODO: expression translation is not implemented yet
                }
                else
                {
                    UsedVariable variable = translatedWhereClause.getIntercode().getVariables().get(variableName);

                    if(variable != null)
                        variables.add(variable);

                    variablesInScope.add(variableName);
                }
            }
        }


        SqlSelect sqlSelect = new SqlSelect(variablesInScope, variables, translatedWhereClause.getIntercode());


        if(select.isDistinct())
            sqlSelect.setDistinct(true);

        if(select.getLimit() != null)
            sqlSelect.setLimit(select.getLimit());

        if(select.getOffset() != null)
            sqlSelect.setOffset(select.getOffset());


        // clear information about datasets of this SELECT (sub)query
        selectDatasets.pop();

        return new TranslatedSegment(variablesInScope, sqlSelect);
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
        TranslatedSegment translatedPattern = null;

        for(GraphPattern pattern : union.getPatterns())
        {
            TranslatedSegment translated = visitElement(pattern);

            if(translatedPattern == null)
            {
                translatedPattern = translated;
            }
            else
            {
                SqlIntercode intercode = SqlUnion.union(translatedPattern.getIntercode(), translated.getIntercode());
                ArrayList<String> variablesInScope = TranslatedSegment
                        .mergeVariableLists(translatedPattern.getVariablesInScope(), translated.getVariablesInScope());

                translatedPattern = new TranslatedSegment(variablesInScope, intercode);
            }
        }

        assert translatedPattern != null;
        return translatedPattern;
    }


    @Override
    public TranslatedSegment visit(Values values)
    {
        final int size = values.getVariables().size();

        ArrayList<String> variablesInScope = new ArrayList<String>();
        UsedVariables usedVariables = new UsedVariables();


        ArrayList<Pair<String, ArrayList<PatternResourceClass>>> typedVariables = new ArrayList<>();
        ArrayList<ArrayList<Pair<Node, PatternResourceClass>>> typedValuesList = new ArrayList<>();

        for(int j = 0; j < values.getValuesLists().size(); j++)
            typedValuesList.add(new ArrayList<Pair<Node, PatternResourceClass>>(size));


        for(int i = 0; i < values.getVariables().size(); i++)
        {
            String variable = values.getVariables().get(i).getName();

            if(!variablesInScope.contains(variable))
                variablesInScope.add(variable);
            else
                exceptions.add(new TranslateException(ErrorType.repeatOfValuesVariable,
                        values.getVariables().get(i).getRange(), variable));


            ArrayList<PatternResourceClass> valueClasses = new ArrayList<PatternResourceClass>();
            typedVariables.add(new Pair<String, ArrayList<PatternResourceClass>>(variable, valueClasses));


            int valueCount = 0;

            for(int j = 0; j < values.getValuesLists().size(); j++)
            {
                ValuesList valuesList = values.getValuesLists().get(j);
                Expression value = valuesList.getValues().get(i);
                PatternResourceClass valueType = null;

                if(value != null)
                {
                    valueCount++;

                    if(value instanceof Literal)
                    {
                        Literal literal = (Literal) value;

                        if(literal.getLanguageTag() != null)
                        {
                            valueType = LangStringPatternClassWithConstantTag.get(literal.getLanguageTag());
                        }
                        else
                        {
                            valueType = BuiltinClasses.unsupportedLiteral;

                            if(literal.getValue() != null)
                            {
                                IRI iri = literal.getTypeIri();

                                if(iri != null)
                                    for(PatternLiteralClass literalClass : BuiltinClasses.getPatternLiteralClasses())
                                        if(literalClass.getTypeIri().equals(iri))
                                            valueType = literalClass;

                                if(valueType == xsdDateTime)
                                    valueType = DateTimePatternClassWithConstantZone
                                            .get(((OffsetDateTime) literal.getValue()).getOffset().getTotalSeconds());

                                if(valueType == xsdDate)
                                    valueType = DatePatternClassWithConstantZone
                                            .get(DatePatternClassWithConstantZone.getZone(literal));
                            }
                        }
                    }
                    else if(value instanceof IRI)
                    {
                        valueType = BuiltinClasses.unsupportedIri;

                        for(PatternResourceClass resClass : iriClasses.values())
                            if(resClass instanceof IriClass)
                                if(resClass.match((Node) value))
                                    valueType = resClass;
                    }
                    else
                    {
                        assert false;
                    }

                    if(!valueClasses.contains(valueType))
                        valueClasses.add(valueType);
                }

                typedValuesList.get(j).add(new Pair<Node, PatternResourceClass>((Node) value, valueType));
            }


            if(valueCount > 0)
            {
                boolean canBeNull = valueCount < values.getValuesLists().size();
                UsedVariable usedVariable = new UsedVariable(variable, canBeNull);
                usedVariable.addClasses(valueClasses);

                usedVariables.add(usedVariable);
            }
        }

        return new TranslatedSegment(variablesInScope, new SqlValues(usedVariables, typedVariables, typedValuesList));
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


        ArrayList<String> variablesInScope = new ArrayList<String>();

        if(graph instanceof Variable)
            variablesInScope.add(((Variable) graph).getName());

        if(subject instanceof Variable)
            variablesInScope.add(((Variable) subject).getName());

        if(predicate instanceof Variable)
            variablesInScope.add(((Variable) predicate).getName());

        if(object instanceof Variable)
            variablesInScope.add(((Variable) object).getName());


        if(translatedPattern == null)
            return new TranslatedSegment(variablesInScope, new SqlNoSolution());


        return new TranslatedSegment(variablesInScope, translatedPattern);
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
        TranslatedSegment translatedGroupPattern = new TranslatedSegment(new ArrayList<String>(),
                new SqlEmptySolution());

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

                if(translatedPattern == null)
                    System.err.println(pattern.getClass().getCanonicalName());

                SqlIntercode intercode = SqlJoin.join(schema, translatedGroupPattern.getIntercode(),
                        translatedPattern.getIntercode());
                ArrayList<String> variablesInScope = TranslatedSegment.mergeVariableLists(
                        translatedGroupPattern.getVariablesInScope(), translatedPattern.getVariablesInScope());

                translatedGroupPattern = new TranslatedSegment(variablesInScope, intercode);
            }
        }

        return translateFilters(filters, translatedGroupPattern);
    }


    private void processNodeMapping(SqlTableAccess translated, Node node, NodeMapping mapping)
    {
        if(!(node instanceof VariableOrBlankNode))
            return;

        translated.addVariableClass(((VariableOrBlankNode) node).getName(), mapping.getResourceClass());
        translated.addMapping(((VariableOrBlankNode) node).getName(), mapping);
    }


    private void processNodeCondition(SqlTableAccess translated, Node node, NodeMapping mapping)
    {
        if(node instanceof VariableOrBlankNode && mapping instanceof ParametrisedMapping)
            translated.addNotNullCondition((ParametrisedMapping) mapping);

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

        return false;
    }


    private TranslatedSegment translateLeftJoin(TranslatedSegment translatedGroupPattern,
            TranslatedSegment translatedPattern, LinkedList<Filter> optionalFilters)
    {
        ArrayList<String> variablesInScope = TranslatedSegment.mergeVariableLists(
                translatedGroupPattern.getVariablesInScope(), translatedPattern.getVariablesInScope());

        List<SqlExpressionIntercode> conditions = null;

        if(!optionalFilters.isEmpty())
        {
            VariableAccessor variableAccessor = new LeftJoinVariableAccessor(
                    translatedGroupPattern.getIntercode().getVariables(),
                    translatedPattern.getIntercode().getVariables());
            conditions = translateLeftJoinFilters(optionalFilters, variableAccessor);

            if(conditions == null)
                return new TranslatedSegment(variablesInScope, translatedGroupPattern.getIntercode());
        }

        SqlIntercode intercode = SqlLeftJoin.leftJoin(schema, translatedGroupPattern.getIntercode(),
                translatedPattern.getIntercode(), conditions);

        return new TranslatedSegment(variablesInScope, intercode);
    }


    List<SqlExpressionIntercode> translateLeftJoinFilters(LinkedList<Filter> optionalFilters,
            VariableAccessor variableAccessor)
    {
        List<SqlExpressionIntercode> expressions = new LinkedList<SqlExpressionIntercode>();
        boolean isFalse = false;

        for(Filter filter : optionalFilters)
        {
            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(variableAccessor, configuration,
                    prologue, exceptions, warnings);

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


    private TranslatedSegment translateMinus(Minus pattern, TranslatedSegment translatedGroupPattern)
    {
        TranslatedSegment minusPattern = visitElement(pattern.getPattern());

        SqlIntercode intercode = SqlMinus.minus(translatedGroupPattern.getIntercode(), minusPattern.getIntercode());
        return new TranslatedSegment(translatedGroupPattern.getVariablesInScope(), intercode);
    }


    private TranslatedSegment translateBind(Bind bind, TranslatedSegment translatedGroupPattern)
    {
        String variableName = bind.getVariable().getName();

        if(translatedGroupPattern.getVariablesInScope().contains(variableName))
            exceptions.add(new TranslateException(ErrorType.variableUsedBeforeBind, bind.getVariable().getRange(),
                    variableName));

        ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                new SimpleVariableAccessor(translatedGroupPattern.getIntercode().getVariables()), configuration,
                prologue, exceptions, warnings);

        SqlExpressionIntercode expression = visitor.visitElement(bind.getExpression());

        ArrayList<String> variables = new ArrayList<String>(translatedGroupPattern.getVariablesInScope().size() + 1);
        variables.addAll(translatedGroupPattern.getVariablesInScope());
        variables.add(variableName);

        SqlIntercode intercode = SqlBind.bind(variableName, expression, translatedGroupPattern.getIntercode());
        return new TranslatedSegment(variables, intercode);
    }


    private SqlIntercode translateFilters(List<SqlExpressionIntercode> filterExpressions, SqlIntercode child)
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

                union = SqlUnion.union(union, translateFilters(optimizedExpressions, subChild));
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


    private TranslatedSegment translateFilters(LinkedList<Filter> filters, TranslatedSegment groupPattern)
    {
        if(filters.size() == 0)
            return groupPattern;


        List<SqlExpressionIntercode> filterExpressions = new LinkedList<SqlExpressionIntercode>();

        for(Filter filter : filters)
        {
            ExpressionTranslateVisitor visitor = new ExpressionTranslateVisitor(
                    new SimpleVariableAccessor(groupPattern.getIntercode().getVariables()), configuration, prologue,
                    exceptions, warnings);

            SqlExpressionIntercode expression = visitor.visitElement(filter.getConstraint());
            expression = SqlEffectiveBooleanValue.create(expression);

            filterExpressions.add(expression);
        }

        return new TranslatedSegment(groupPattern.getVariablesInScope(),
                translateFilters(filterExpressions, groupPattern.getIntercode()));
    }


    private TranslatedSegment translateProcedureCall(ProcedureCallBase procedureCallBase, TranslatedSegment context)
    {
        IRI procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = procedures.get(procedureName.getUri().toString());
        UsedVariables contextVariables = context.getIntercode().getVariables();


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

        LinkedHashMap<ParameterDefinition, Node> parameterNodes = new LinkedHashMap<ParameterDefinition, Node>();

        for(ParameterDefinition parameter : procedureDefinition.getParameters())
            parameterNodes.put(parameter, null);


        ArrayList<String> variablesInScope = new ArrayList<String>();

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
                    exceptions.add(new TranslateException(ErrorType.missingParameterPredicate,
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
                    resultNodes.put(resultDefinition, result.getValue());
                }


                if(result.getValue() instanceof Variable)
                    variablesInScope.add(((Variable) result.getValue()).getName());
            }
        }

        SqlIntercode intercode = SqlProcedureCall.create(procedureDefinition, parameterNodes, resultNodes,
                context.getIntercode());

        return new TranslatedSegment(variablesInScope, intercode);
    }


    public String translate(SelectQuery sparqlQuery) throws TranslateExceptions
    {
        TranslatedSegment segment = visitElement(sparqlQuery);

        if(!exceptions.isEmpty())
            throw new TranslateExceptions(exceptions);

        return segment.getIntercode().translate();
    }


    public TranslateResult tryTranslate(SelectQuery sparqlQuery)
    {
        visitElement(sparqlQuery);
        return new TranslateResult(null, exceptions, warnings);
    }


    public final LinkedHashMap<String, UserIriClass> getIriClasses()
    {
        return iriClasses;
    }
}
