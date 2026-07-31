package cz.iocb.sparql.engine.parser;

import static cz.iocb.sparql.engine.translator.TermGenerator.getIri;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.grammar.SparqlParser.AggregateContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BaseDeclContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BindContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.BlankNodeContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ConstructTemplateContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.DataBlockContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.DataBlockValueContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.DataBlockValuesContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.DatasetClauseContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ExistsFunctionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.FilterContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.GraphGraphPatternContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.GroupClauseContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.GroupConditionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.GroupGraphPatternContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.GroupGraphPatternSubContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.GroupGraphPatternSubListContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.GroupOrUnionGraphPatternContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.HavingClauseContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.InlineDataContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.InlineDataFullContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.InlineDataOneVarContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.IriContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.LimitOffsetClausesContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.MinusGraphPatternContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.NotExistsFunctionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.OptionalGraphPatternContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.OrderClauseContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.OrderConditionContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.PrefixDeclContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.PrefixedNameContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.QueryContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.SelectClauseContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.SelectVariableContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ServiceGraphPatternContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.SolutionModifierContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.SubSelectContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.TriplesBlockContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.TriplesSameSubjectContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.TriplesSameSubjectPathContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.ValuesClauseContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.VarOrIriContext;
import cz.iocb.sparql.engine.grammar.SparqlParser.WhereClauseContext;
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
import cz.iocb.sparql.engine.model.PrefixDefinition;
import cz.iocb.sparql.engine.model.PrefixedName;
import cz.iocb.sparql.engine.model.Projection;
import cz.iocb.sparql.engine.model.Prologue;
import cz.iocb.sparql.engine.model.Query;
import cz.iocb.sparql.engine.model.Select;
import cz.iocb.sparql.engine.model.SelectQuery;
import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.model.base.Range;
import cz.iocb.sparql.engine.model.expression.BracketedExpression;
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
import cz.iocb.sparql.engine.model.triple.BlankNodePropertyList;
import cz.iocb.sparql.engine.model.triple.ComplexNode;
import cz.iocb.sparql.engine.model.triple.ComplexTriple;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.triple.Property;
import cz.iocb.sparql.engine.model.triple.RdfCollection;
import cz.iocb.sparql.engine.model.triple.Triple;
import cz.iocb.sparql.engine.model.triple.Verb;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.rdf.Iri;



public class QueryVisitor extends BaseVisitor<Query>
{
    private final SparqlDatabaseConfiguration config;
    private final Stack<VarOrIri> graphs;
    private final Stack<VarOrIri> services;
    private final VariableScopes scopes;
    private final Set<String> usedBlankNodes;
    private final List<TranslateMessage> messages;
    private Prologue prologue;


    public QueryVisitor(SparqlDatabaseConfiguration config, List<TranslateMessage> messages)
    {
        this.config = config;
        this.graphs = new Stack<>();
        this.services = new Stack<>();
        this.usedBlankNodes = new HashSet<>();
        this.scopes = new VariableScopes();
        this.messages = messages;
    }


    public QueryVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, Set<String> usedBlankNodes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.graphs = graphs;
        this.services = services;
        this.usedBlankNodes = usedBlankNodes;
        this.scopes = scopes;
        this.messages = messages;
    }


    @Override
    public Query visitQuery(QueryContext ctx)
    {
        final Set<String> patternBlankNodes = new HashSet<>();

        new BaseVisitor<Void>()
        {
            @Override
            public Void visitBlankNode(BlankNodeContext ctx)
            {
                if(ctx.BLANK_NODE_LABEL() != null)
                {
                    String name = ctx.getText().replaceFirst("^:_", "");

                    if(usedBlankNodes.contains(name))
                        messages.add(
                                new TranslateMessage(MessageType.reuseOfBlankNode, Range.compute(ctx), ctx.getText()));

                    patternBlankNodes.add(name);
                }

                return null;
            }

            @Override
            public Void visitGroupGraphPattern(GroupGraphPatternContext ctx)
            {
                usedBlankNodes.addAll(patternBlankNodes);
                patternBlankNodes.clear();
                super.visitGroupGraphPattern(ctx);
                usedBlankNodes.addAll(patternBlankNodes);
                patternBlankNodes.clear();
                return null;
            }

            @Override
            public Void visitBind(BindContext ctx)
            {
                usedBlankNodes.addAll(patternBlankNodes);
                patternBlankNodes.clear();
                return super.visitBind(ctx);
            }

            @Override
            public Void visitInlineData(InlineDataContext ctx)
            {
                usedBlankNodes.addAll(patternBlankNodes);
                patternBlankNodes.clear();
                return super.visitInlineData(ctx);
            }

            @Override
            public Void visitConstructTemplate(ConstructTemplateContext ctx)
            {
                return null;
            }
        }.visit(ctx);


        if(ctx.prologue() != null)
        {
            PrologueVisitor visitor = new PrologueVisitor(config, messages);
            visitor.visit(ctx.prologue());
            prologue = visitor.getPrologue();
        }
        else
        {
            prologue = new Prologue(config.getPrefixes());
        }


        Query result = null;

        if(ctx.selectQuery() != null)
        {
            Select select = withRange(parseSelect(ctx.selectQuery().selectClause(), ctx.selectQuery().datasetClause(),
                    ctx.selectQuery().whereClause(), ctx.selectQuery().solutionModifier(), ctx.valuesClause(), false),
                    ctx);

            result = new SelectQuery(prologue, select);
        }
        else if(ctx.askQuery() != null)
        {
            GraphPattern pattern = new GraphPatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes,
                    messages).visit(ctx.askQuery().whereClause());
            Values values = parseValues(ctx.valuesClause());

            Select select = withRange(new Select(new LinkedList<>(), pattern, values, true), ctx);
            select.setReduced(true);

            if(ctx.askQuery().datasetClause() != null)
                select.getDataSets().addAll(ctx.askQuery().datasetClause().stream().map(this::parseDataSet).toList());

            select.getGroupByConditions().addAll(parseGroupClause(ctx.askQuery().groupClause()));
            select.getHavingConditions().addAll(parseHavingClause(ctx.askQuery().havingClause()));
            select.setIsInAggregateMode(isInAggregateMode(ctx.askQuery().groupClause(), ctx.askQuery().havingClause()));

            checkSelect(select);

            result = new AskQuery(prologue, select);
        }
        else if(ctx.describeQuery() != null)
        {
            NodeVisitor visitor = new NodeVisitor(config, prologue, scopes, messages);
            LinkedList<VarOrIri> resources = new LinkedList<>();

            for(VarOrIriContext item : ctx.describeQuery().describeClause().varOrIri())
                resources.add((VarOrIri) visitor.visit(item));


            GraphPattern pattern = ctx.describeQuery().whereClause() != null ?
                    new GraphPatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages)
                            .visit(ctx.describeQuery().whereClause()) :
                    new GroupGraph(new ArrayList<>(0));

            Values values = parseValues(ctx.valuesClause());


            if(ctx.describeQuery().describeClause().varOrIri().isEmpty())
            {
                if(!isInAggregateMode(ctx.describeQuery().solutionModifier()))
                    for(VariableNode variable : pattern.getVariablesInScope())
                        resources.add(new VariableNode(scopes.addToScope(variable.getName()), variable.getName()));
                else
                    messages.add(new TranslateMessage(MessageType.invalidProjection,
                            Range.compute(ctx.describeQuery().describeClause())));
            }

            LinkedList<Projection> projections = new LinkedList<>();

            for(VarOrIri resource : resources)
                if(resource instanceof VariableNode variable)
                    projections.add(new Projection(variable));


            Select select = withRange(new Select(projections, pattern, values, true), ctx);
            select.setReduced(true);

            if(ctx.describeQuery().datasetClause() != null)
                select.getDataSets()
                        .addAll(ctx.describeQuery().datasetClause().stream().map(this::parseDataSet).toList());

            SolutionModifierContext solutionModifierCtx = ctx.describeQuery().solutionModifier();

            select.getGroupByConditions().addAll(parseGroupClause(solutionModifierCtx.groupClause()));
            select.getHavingConditions().addAll(parseHavingClause(solutionModifierCtx.havingClause()));
            select.getOrderByConditions().addAll(parseOrderClause(solutionModifierCtx.orderClause()));
            select.setLimit(parseLimitClause(solutionModifierCtx.limitOffsetClauses()));
            select.setOffset(parseOffsetClause(solutionModifierCtx.limitOffsetClauses()));
            select.setIsInAggregateMode(isInAggregateMode(solutionModifierCtx));

            checkSelect(select);

            result = new DescribeQuery(prologue, resources, select);
        }
        else if(ctx.constructQuery() != null)
        {
            PropertiesVisitor propertiesVisitor = new PropertiesVisitor(config, prologue, scopes, messages);
            NodeVisitor nodeVisitor = new NodeVisitor(config, prologue, scopes, messages);
            TripleExpander expander = new TripleExpander(usedBlankNodes);

            if(ctx.constructQuery().constructTemplate().triplesTemplate() != null)
            {
                for(TriplesSameSubjectContext triplesCtx : ctx.constructQuery().constructTemplate().triplesTemplate()
                        .triplesSameSubject())
                {
                    if(triplesCtx.varOrTerm() != null)
                    {
                        ComplexNode node = nodeVisitor.visit(triplesCtx.varOrTerm());
                        Stream<Property> properties = propertiesVisitor.visit(triplesCtx.propertyListNotEmpty());
                        expander.visit(new ComplexTriple(node, properties.toList()));
                    }
                    else
                    {
                        ComplexNode node = nodeVisitor.visit(triplesCtx.triplesNode());
                        Stream<Property> properties = triplesCtx.propertyList().propertyListNotEmpty() != null ?
                                propertiesVisitor.visit(triplesCtx.propertyList()) : Stream.empty();
                        expander.visit(new ComplexTriple(node, properties.toList()));
                    }
                }
            }

            List<Pattern> templates = expander.getResults();


            LinkedList<Projection> projections = new LinkedList<>();

            BaseVisitor<Void> variableVisitor = new BaseVisitor<>()
            {
                Set<String> variables = new HashSet<>();

                @Override
                public Void visitVar(VarContext ctx)
                {
                    String variable = ctx.getText().substring(1);

                    if(!variables.contains(variable))
                    {
                        variables.add(variable);
                        projections.add(new Projection(new VariableNode(scopes.addToScope(variable), variable)));
                    }

                    return null;
                }
            };

            variableVisitor.visit(ctx.constructQuery().constructTemplate());


            GraphPattern pattern = ctx.constructQuery().whereClause() != null ?
                    new GraphPatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages)
                            .visit(ctx.constructQuery().whereClause()) :
                    withRange(new GroupGraph(templates), ctx.constructQuery().constructTemplate());

            Values values = parseValues(ctx.valuesClause());

            Select select = withRange(new Select(projections, pattern, values, true), ctx);
            select.setReduced(true);

            if(ctx.constructQuery().datasetClause() != null)
                select.getDataSets()
                        .addAll(ctx.constructQuery().datasetClause().stream().map(this::parseDataSet).toList());

            SolutionModifierContext solutionModifierCtx = ctx.constructQuery().solutionModifier();

            select.getGroupByConditions().addAll(parseGroupClause(solutionModifierCtx.groupClause()));
            select.getHavingConditions().addAll(parseHavingClause(solutionModifierCtx.havingClause()));
            select.getOrderByConditions().addAll(parseOrderClause(solutionModifierCtx.orderClause()));
            select.setLimit(parseLimitClause(solutionModifierCtx.limitOffsetClauses()));
            select.setOffset(parseOffsetClause(solutionModifierCtx.limitOffsetClauses()));
            select.setIsInAggregateMode(isInAggregateMode(solutionModifierCtx));

            checkSelect(select);

            result = new ConstructQuery(prologue, templates, select);
        }
        else if(ctx.updateCommand() != null)
        {
            messages.add(
                    new TranslateMessage(MessageType.unsupportedUpdateCommand, Range.compute(ctx.updateCommand())));
        }

        prologue = null;
        return result;
    }


    /**
     * @param ctx Can be null.
     */
    public Values parseValues(ValuesClauseContext ctx)
    {
        if(ctx == null)
            return null;

        DataBlockContext dataBlockCtx = ctx.dataBlock();
        if(dataBlockCtx == null)
            return null;

        return (Values) new PatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages)
                .visit(dataBlockCtx);
    }


    public boolean isInAggregateMode(ParserRuleContext... contexts)
    {
        BaseVisitor<Boolean> visitor = new BaseVisitor<>()
        {
            @Override
            public Boolean aggregateResult(Boolean aggregate, Boolean nextResult)
            {
                return (aggregate != null && aggregate) || (nextResult != null && nextResult);
            }

            @Override
            public Boolean visit(ParseTree tree)
            {
                if(tree == null)
                    return false;

                Boolean result = super.visit(tree);

                return result != null && result;
            }

            @Override
            public Boolean visitExistsFunction(ExistsFunctionContext ctx)
            {
                return false;
            }

            @Override
            public Boolean visitNotExistsFunction(NotExistsFunctionContext ctx)
            {
                return false;
            }

            @Override
            public Boolean visitAggregate(AggregateContext ctx)
            {
                return true;
            }

            @Override
            public Boolean visitGroupClause(GroupClauseContext ctx)
            {
                return true;
            }
        };


        for(ParserRuleContext ctx : contexts)
            if(visitor.visit(ctx))
                return true;

        return false;
    }


    public Select parseSelect(SelectClauseContext selectClauseCtx, List<DatasetClauseContext> dataSetClauseCtxs,
            WhereClauseContext whereClauseCtx, SolutionModifierContext solutionModifierCtx,
            ValuesClauseContext valuesClauseContext, boolean isSubSelect)
    {
        if(!selectClauseCtx.selectVariable().isEmpty())
        {
            selectClauseCtx.selectVariable().stream().filter(c -> c.var() != null)
                    .forEach(c -> scopes.addToScope(c.var().getText()));
            scopes.addScope(selectClauseCtx.selectVariable().stream().filter(c -> c.var() != null)
                    .map(c -> c.var().getText()).collect(toSet()));
        }

        try
        {
            GraphPattern pattern = new GraphPatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes,
                    messages).visit(whereClauseCtx);

            LinkedList<Projection> projections = new LinkedList<>();

            if(!selectClauseCtx.selectVariable().isEmpty())
            {
                for(Projection projection : selectClauseCtx.selectVariable().stream().map(this::parseProjection)
                        .toList())
                    if(projection != null)
                        projections.add(projection);
            }
            else if(!isInAggregateMode(selectClauseCtx, solutionModifierCtx))
            {
                Set<String> variables = new HashSet<>();
                Range range = Range.compute(selectClauseCtx.star, selectClauseCtx.star);

                BaseVisitor<Void> variableVisitor = new BaseVisitor<>()
                {
                    @Override
                    public Void visitFilter(FilterContext ctx)
                    {
                        return null;
                    }

                    @Override
                    public Void visitMinusGraphPattern(MinusGraphPatternContext ctx)
                    {
                        return null;
                    }

                    @Override
                    public Void visitBind(BindContext ctx)
                    {
                        visit(ctx.var());
                        return null;
                    }

                    @Override
                    public Void visitSubSelect(SubSelectContext ctx)
                    {
                        if(!ctx.selectClause().selectVariable().isEmpty())
                        {
                            for(SelectVariableContext var : ctx.selectClause().selectVariable())
                                if(var.var() != null)
                                    visit(var.var());
                        }
                        else if(!isInAggregateMode(ctx.selectClause(), ctx.solutionModifier()))
                        {
                            visit(ctx.whereClause());

                            if(ctx.solutionModifier().groupClause() != null)
                                for(GroupConditionContext cnd : ctx.solutionModifier().groupClause().groupCondition())
                                    if(cnd.var() != null)
                                        visit(cnd.var());
                        }

                        if(ctx.valuesClause() != null)
                            visit(ctx.valuesClause());

                        return null;
                    }

                    @Override
                    public Void visitVar(VarContext ctx)
                    {
                        String variable = ctx.getText().substring(1);

                        if(!variables.contains(variable))
                        {
                            variables.add(variable);

                            VariableNode selectVariable = new VariableNode(scopes.addToScope(variable), variable);
                            selectVariable.setRange(range);

                            projections.add(new Projection(selectVariable));
                        }

                        return null;
                    }
                };

                variableVisitor.visit(whereClauseCtx);

                if(valuesClauseContext != null)
                    variableVisitor.visit(valuesClauseContext);
            }
            else
            {
                messages.add(new TranslateMessage(MessageType.invalidProjection, Range.compute(selectClauseCtx)));
            }

            Values values = parseValues(valuesClauseContext);

            Select result = new Select(projections, pattern, values, isSubSelect);

            if(selectClauseCtx.DISTINCT() != null)
                result.setDistinct(true);
            if(selectClauseCtx.REDUCED() != null)
                result.setReduced(true);

            if(dataSetClauseCtxs != null)
                result.getDataSets().addAll(dataSetClauseCtxs.stream().map(this::parseDataSet).toList());

            result.getGroupByConditions().addAll(parseGroupClause(solutionModifierCtx.groupClause()));
            result.getHavingConditions().addAll(parseHavingClause(solutionModifierCtx.havingClause()));
            result.getOrderByConditions().addAll(parseOrderClause(solutionModifierCtx.orderClause()));
            result.setLimit(parseLimitClause(solutionModifierCtx.limitOffsetClauses()));
            result.setOffset(parseOffsetClause(solutionModifierCtx.limitOffsetClauses()));
            result.setIsInAggregateMode(isInAggregateMode(selectClauseCtx, solutionModifierCtx));

            checkSelect(result);

            return result;
        }
        finally
        {
            if(!selectClauseCtx.selectVariable().isEmpty())
                scopes.popScope();
        }
    }


    private void checkSelect(Select select)
    {
        Set<String> vars = new HashSet<>();

        for(Projection projection : select.getProjections())
        {
            if(vars.contains(projection.getVariable().getName()))
                messages.add(new TranslateMessage(MessageType.repeatOfProjectionVariable,
                        projection.getVariable().getRange(), projection.getVariable().getName()));

            vars.add(projection.getVariable().getName());
        }

        if(select.isInAggregateMode())
        {
            Set<String> groupVars = new HashSet<>();

            for(GroupCondition groupCondition : select.getGroupByConditions())
            {
                if(groupCondition.getVariable() != null)
                    groupVars.add(groupCondition.getVariable().getName());
                else if(groupCondition.getExpression() instanceof VariableNode variable)
                    groupVars.add(variable.getName());
            }

            for(Projection projection : select.getProjections())
            {
                if(projection.getExpression() != null)
                    checkExpressionForGroupedSolutions(projection.getExpression(), groupVars);
                else
                    checkExpressionForGroupedSolutions(projection.getVariable(), groupVars);
            }
        }

        for(GroupCondition groupBy : select.getGroupByConditions())
        {
            if(groupBy.getVariable() != null)
            {
                VariableNode variable = groupBy.getVariable();

                if(select.getPattern().getVariablesInScope().contains(variable))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeGroupBy,
                            groupBy.getVariable().getRange(), variable.getName()));
            }
        }


        Set<VariableNode> inScopeVariables = new HashSet<>(select.getPattern().getVariablesInScope());

        for(Projection projection : select.getProjections())
        {
            if(projection.getExpression() != null)
            {
                VariableNode variable = projection.getVariable();

                if(inScopeVariables.contains(variable))
                    messages.add(new TranslateMessage(MessageType.variableUsedBeforeProjection, variable.getRange(),
                            variable.getName()));

                inScopeVariables.add(variable);
            }
        }
    }


    private void checkExpressionForGroupedSolutions(Expression expresion, Set<String> groupByVars)
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
            public Void visit(VariableNode var)
            {
                String name = var.getName();

                if(!inAggregateFunction && !groupByVars.contains(name))
                    messages.add(
                            new TranslateMessage(MessageType.invalidVariableOutsideAggregate, var.getRange(), name));

                return null;
            }

            @Override
            public Void visit(ExistsExpression expr)
            {
                return null;
            }
        }.visitElement(expresion);
    }


    private Projection parseProjection(SelectVariableContext variableCtx)
    {
        if(variableCtx.var() == null)
            return null;

        VariableNode variable = withRange(
                new VariableNode(scopes.addToScope(variableCtx.var().getText()), variableCtx.var().getText()),
                variableCtx.var());

        if(variableCtx.expression() != null)
        {
            Expression expression = new ExpressionVisitor(config, prologue, graphs, services, scopes, usedBlankNodes,
                    messages, true).visit(variableCtx.expression());

            return new Projection(expression, variable);
        }

        return withRange(new Projection(variable), variableCtx);
    }


    private DataSet parseDataSet(DatasetClauseContext dataSetCtx)
    {
        IriNode iri = new IriVisitor(prologue, messages).visit(dataSetCtx.iri());
        boolean isDefault = dataSetCtx.NAMED() == null;

        return withRange(new DataSet(iri, isDefault), dataSetCtx);
    }


    private List<GroupCondition> parseGroupClause(GroupClauseContext ctx)
    {
        if(ctx == null)
            return new ArrayList<>();

        return ctx.groupCondition().stream().map(this::parseGroupCondition).toList();
    }


    private GroupCondition parseGroupCondition(GroupConditionContext ctx)
    {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, false);

        if(ctx.expression() != null)
        {
            Expression expression = expressionVisitor.visit(ctx.expression());

            if(ctx.var() == null)
            {
                return new GroupCondition(new BracketedExpression(expression));
            }

            VariableNode variable = withRange(
                    new VariableNode(scopes.addToScope(ctx.var().getText()), ctx.var().getText()), ctx.var());

            return new GroupCondition(expression, variable);
        }

        return withRange(new GroupCondition(expressionVisitor.visit(ctx)), ctx);
    }


    private List<Expression> parseHavingClause(HavingClauseContext ctx)
    {
        if(ctx == null)
            return new ArrayList<>();

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages, true);

        return ctx.havingCondition().stream().map(expressionVisitor::visit).toList();
    }


    private List<OrderCondition> parseOrderClause(OrderClauseContext ctx)
    {
        if(ctx == null)
            return new ArrayList<>();

        return ctx.orderCondition().stream().map(this::parseOrderCondition).toList();
    }


    private OrderCondition parseOrderCondition(OrderConditionContext ctx)
    {
        if(ctx.ASC() != null || ctx.DESC() != null)
        {
            OrderCondition.Direction direction = ctx.ASC() != null ? OrderCondition.Direction.Ascending :
                    OrderCondition.Direction.Descending;

            return new OrderCondition(direction,
                    new ExpressionVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages, true)
                            .visit(ctx.expression()));
        }

        return withRange(new OrderCondition(
                new ExpressionVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages, true)
                        .visit(ctx)),
                ctx);
    }


    private static BigInteger parseLimitClause(LimitOffsetClausesContext ctx)
    {
        if(ctx == null || ctx.limitClause() == null)
            return null;

        return new BigInteger(ctx.limitClause().INTEGER().getText());
    }


    private static BigInteger parseOffsetClause(LimitOffsetClausesContext ctx)
    {
        if(ctx == null || ctx.offsetClause() == null)
            return null;

        return new BigInteger(ctx.offsetClause().INTEGER().getText());
    }
}


class PrologueVisitor extends BaseVisitor<Void>
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public PrologueVisitor(SparqlDatabaseConfiguration config, List<TranslateMessage> messages)
    {
        this.messages = messages;
        this.prologue = new Prologue(config.getPrefixes());
    }


    public Prologue getPrologue()
    {
        return prologue;
    }


    @Override
    public Void visitBaseDecl(BaseDeclContext ctx)
    {
        String uri = ctx.IRIREF().getText();
        uri = uri.substring(1, uri.length() - 1);

        try
        {
            if(!(new URI(uri)).isAbsolute())
                messages.add(new TranslateMessage(MessageType.invalidBaseIri, Range.compute(ctx)));
        }
        catch(URISyntaxException e)
        {
            Range range = Range.compute(ctx.IRIREF().getSymbol(), ctx.IRIREF().getSymbol());
            messages.add(new TranslateMessage(MessageType.malformedIri, range));
        }

        prologue.setBase(uri);

        return null;
    }


    @Override
    public Void visitPrefixDecl(PrefixDeclContext ctx)
    {
        String name = ctx.PNAME_NS().getText();
        IriNode iri = new IriNode(new IriVisitor(prologue, messages).parseUri(ctx.IRIREF(), prologue));

        PrefixDefinition result = withRange(new PrefixDefinition(name, iri), ctx);
        prologue.addPrefixDefinition(result);

        return null;
    }
}


class GraphPatternVisitor extends BaseVisitor<GraphPattern>
{
    private final SparqlDatabaseConfiguration config;
    private final Prologue prologue;
    private final Stack<VarOrIri> graphs;
    private final Stack<VarOrIri> services;
    private final VariableScopes scopes;
    private final Set<String> usedBlankNodes;
    private final List<TranslateMessage> messages;


    public GraphPatternVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, Set<String> usedBlankNodes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.graphs = graphs;
        this.services = services;
        this.scopes = scopes;
        this.usedBlankNodes = usedBlankNodes;
        this.messages = messages;
    }


    @Override
    public GraphPattern visitGroupGraphPattern(GroupGraphPatternContext ctx)
    {
        if(ctx.subSelect() == null && ctx.groupGraphPatternSub() == null)
            return new GroupGraph(new ArrayList<>());

        // brackets are added to the range of group graph pattern, but not of sub select

        if(ctx.groupGraphPatternSub() == null)
            return visit(ctx.subSelect());

        GraphPattern graphPattern = visit(ctx.groupGraphPatternSub());
        graphPattern.setRange(Range.compute(ctx));
        return graphPattern;
    }


    @Override
    public GraphPattern visitGroupGraphPatternSub(GroupGraphPatternSubContext ctx)
    {
        List<Pattern> patterns = new GroupGraphPatternVisitor(config, prologue, graphs, services, scopes,
                usedBlankNodes, messages).visit(ctx).toList();


        Set<VariableNode> inScopeVariables = new HashSet<>();

        for(Pattern pattern : patterns)
        {
            if(pattern instanceof Bind bind && inScopeVariables.contains(bind.getVariable()))
                messages.add(new TranslateMessage(MessageType.variableUsedBeforeBind, bind.getVariable().getRange(),
                        bind.getVariable().getName()));

            inScopeVariables.addAll(pattern.getVariablesInScope());
        }


        return new GroupGraph(assembleProcedureCalls(patterns));
    }


    @Override
    public GraphPattern visitSubSelect(SubSelectContext ctx)
    {
        return withRange(
                new QueryVisitor(config, prologue, graphs, services, scopes, usedBlankNodes, messages).parseSelect(
                        ctx.selectClause(), null, ctx.whereClause(), ctx.solutionModifier(), ctx.valuesClause(), true),
                ctx);
    }


    private List<Pattern> assembleProcedureCalls(List<Pattern> patterns)
    {
        if(!services.stream().allMatch(s -> s instanceof IriNode n && config.getServices().contains(getIri(n))))
            return patterns;

        Iri service = services.isEmpty() ? config.getServiceIri() : getIri(((IriNode) services.peek()));


        Set<Pattern> callPatterns = new HashSet<>();
        Map<Node, List<Parameter>> parameterNodeMap = new HashMap<>();
        Map<Node, List<Parameter>> resultNodeMap = new HashMap<>();

        Map<Node, Set<Range>> parameterNodeOccurences = new HashMap<>();
        Map<Node, Set<Range>> resultNodeOccurences = new HashMap<>();


        for(Pattern pattern : patterns)
        {
            if(!(pattern instanceof Triple))
                continue;

            Triple triple = (Triple) pattern;
            Verb predicate = triple.getPredicate();

            if(predicate instanceof IriNode procedureName)
            {
                ProcedureDefinition definition = config.getProcedures(service).get(procedureName.getValue());

                if(definition != null)
                {
                    callPatterns.add(triple);

                    Node parameterNode = triple.getObject();

                    if(parameterNode instanceof VariableOrBlankNode)
                    {
                        parameterNodeMap.put(parameterNode, new ArrayList<>());

                        if(!parameterNodeOccurences.containsKey(parameterNode)
                                && !resultNodeOccurences.containsKey(parameterNode))
                            parameterNodeOccurences.put(parameterNode, new LinkedHashSet<>());
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
                            resultNodeMap.put(resultNode, new ArrayList<>());

                            if(!parameterNodeOccurences.containsKey(resultNode)
                                    && !resultNodeOccurences.containsKey(resultNode))
                                resultNodeOccurences.put(resultNode, new LinkedHashSet<>());
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
                    public Void visit(IriNode iri)
                    {
                        if(config.getProcedures(service).get(iri.getValue()) != null)
                            messages.add(new TranslateMessage(MessageType.invalidProcedureCallPropertyPathCombinaion,
                                    iri.getRange())); //TODO: could be supported in a future version

                        return null;
                    }
                }.visitElement(predicate);
            }
        }



        List<Pattern> resultPatterns = new ArrayList<>(patterns);

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

                if(triple.getPredicate() instanceof IriNode iri)
                    parameters.add(new Parameter(iri, triple.getObject()));
                else
                    messages.add(new TranslateMessage(MessageType.invalidProcedureParameterValue,
                            triple.getPredicate().getRange())); //TODO: could be supported in a future version
            }


            List<Parameter> results = resultNodeMap.get(triple.getSubject());

            if(results != null)
            {
                iterator.remove();
                resultNodeOccurences.get(triple.getSubject()).add(triple.getSubject().getRange());

                if(triple.getPredicate() instanceof IriNode iri)
                    results.add(new Parameter(iri, triple.getObject()));
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

                if(object instanceof BlankNode bnode)
                {
                    if(parameterNodeOccurences.containsKey(object))
                        messages.add(new TranslateMessage(MessageType.invalidParameterBlankNodeOccurence,
                                object.getRange(), bnode.getName()));

                    if(resultNodeOccurences.containsKey(object))
                        messages.add(new TranslateMessage(MessageType.invalidResultBlankNodeOccurence,
                                object.getRange(), bnode.getName()));
                }
            }
        }


        ListIterator<Pattern> listIterator = resultPatterns.listIterator();

        while(listIterator.hasNext())
        {
            Pattern pattern = listIterator.next();

            if(!callPatterns.contains(pattern))
                continue;

            Triple triple = (Triple) pattern;

            IriNode name = (IriNode) triple.getPredicate();
            ProcedureDefinition definition = config.getProcedures(service).get(name.getValue());

            List<Parameter> parameters = parameterNodeMap.get(triple.getObject());

            if(parameters == null)
                parameters = new ArrayList<>();


            if(definition.isSimple())
            {
                ProcedureCall call = new ProcedureCall(triple.getSubject(), name, parameters);
                checkProcedureCall(call, service);
                listIterator.set(call);
            }
            else
            {
                List<Parameter> results = resultNodeMap.get(triple.getSubject());

                if(results == null)
                    results = new ArrayList<>();

                MultiProcedureCall call = new MultiProcedureCall(results, name, parameters);
                checkProcedureCall(call, service);
                listIterator.set(call);
            }
        }


        return resultPatterns;
    }


    private void checkProcedureCall(ProcedureCallBase procedureCallBase, Iri service)
    {
        IriNode procedureName = procedureCallBase.getProcedure();
        ProcedureDefinition procedureDefinition = config.getProcedures(service).get(procedureName.getValue());


        /* check graph */

        if(!graphs.empty())
            messages.add(new TranslateMessage(MessageType.procedureCallInsideGraph,
                    procedureCallBase.getProcedure().getRange(), procedureName.toString(prologue)));


        /* check parameters */

        LinkedHashMap<ParameterDefinition, Boolean> parameterNodes = new LinkedHashMap<>();

        for(ParameterDefinition parameter : procedureDefinition.getParameters())
            parameterNodes.put(parameter, false);

        for(Parameter parameter : procedureCallBase.getParameters())
        {
            String parameterName = parameter.getName().getValue();
            ParameterDefinition parameterDefinition = procedureDefinition.getParameter(parameterName);

            if(parameterDefinition == null)
            {
                messages.add(new TranslateMessage(MessageType.invalidParameterPredicate, parameter.getName().getRange(),
                        parameter.getName().toString(prologue), procedureCallBase.getProcedure().toString(prologue)));
            }
            else if(parameterNodes.get(parameterDefinition))
            {
                messages.add(new TranslateMessage(MessageType.repeatOfParameterPredicate,
                        parameter.getName().getRange(), parameter.getName().toString(prologue)));
            }
            else
            {
                parameterNodes.put(parameterDefinition, true);
            }
        }


        for(Entry<ParameterDefinition, Boolean> entry : parameterNodes.entrySet())
        {
            if(!entry.getValue())
            {
                ParameterDefinition parameterDefinition = entry.getKey();

                if(parameterDefinition.getDefaultValue() == null)
                    messages.add(new TranslateMessage(MessageType.missingParameterPredicate,
                            procedureCallBase.getProcedure().getRange(),
                            new IriNode(parameterDefinition.getParamName()).toString(prologue),
                            procedureCallBase.getProcedure().toString(prologue)));
            }
        }


        /* check results */

        if(procedureCallBase instanceof MultiProcedureCall multiProcedureCall)
        {
            Set<ResultDefinition> resultNodes = new HashSet<>();

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
                else if(resultNodes.contains(resultDefinition))
                {
                    messages.add(new TranslateMessage(MessageType.repeatOfResultPredicate,
                            resultParameter.getName().getRange(), resultParameter.getName().toString(prologue)));
                }
                else
                {
                    resultNodes.add(resultDefinition);
                }
            }
        }
    }
}


class TripleExpander extends ComplexElementVisitor<Node>
{
    private final List<Pattern> results = new ArrayList<>();
    private final Set<String> usedBlankNodes;

    private final String blankNodePrefix = "blanknode";
    private int blankNodeId = 0;


    public TripleExpander(Set<String> usedBlankNodes)
    {
        this.usedBlankNodes = usedBlankNodes;
    }


    public List<Pattern> getResults()
    {
        return results;
    }


    @Override
    public Node visit(ComplexTriple triple)
    {
        Node subject = null;

        if(triple.getProperties().isEmpty())
            subject = visitElement(triple.getNode());


        for(Property property : triple.getProperties())
        {
            if(subject == null)
                subject = visitElement(triple.getNode());

            Verb verb = property.getVerb();

            for(ComplexNode objectNode : property.getObjects())
            {
                Node object = visitElement(objectNode);

                Triple resultTriple = new Triple(subject, verb, object);
                resultTriple.setRange(triple.getRange());
                results.add(resultTriple);
            }
        }

        return null;
    }


    @Override
    public Node visit(BlankNodePropertyList blankNodePropertyList)
    {
        BlankNode blankNode = getBlankNode();
        blankNode.setRange(blankNodePropertyList.getRange());

        // take advantage of triple processing
        ComplexTriple triple = new ComplexTriple(blankNode, blankNodePropertyList.getProperties());
        triple.setRange(blankNodePropertyList.getRange());
        visit(triple);

        return blankNode;
    }


    @Override
    public Node visit(RdfCollection rdfCollection)
    {
        if(rdfCollection.getNodes().isEmpty())
            return new IriNode(Rdf.NIL);

        BlankNode firstNode = getBlankNode();
        firstNode.setRange(rdfCollection.getRange());

        BlankNode currentNode = firstNode;

        for(int i = 0; i < rdfCollection.getNodes().size(); i++)
        {
            BlankNode nextNode = null;

            Triple firstTriple = new Triple(currentNode, new IriNode(Rdf.FIRST),
                    visitElement(rdfCollection.getNodes().get(i)));
            firstTriple.setRange(rdfCollection.getRange());
            results.add(firstTriple);

            Node restObject;
            if(i == rdfCollection.getNodes().size() - 1)
            {
                restObject = new IriNode(Rdf.NIL);
            }
            else
            {
                nextNode = getBlankNode();
                nextNode.setRange(rdfCollection.getRange());

                restObject = nextNode;
            }

            Triple restTriple = new Triple(currentNode, new IriNode(Rdf.REST), restObject);
            restTriple.setRange(rdfCollection.getRange());
            results.add(restTriple);

            currentNode = nextNode;
        }

        return firstNode;
    }


    @Override
    public Node visit(VariableNode variable)
    {
        return variable;
    }


    @Override
    public Node visit(IriNode iri)
    {
        return iri;
    }


    @Override
    public Node visit(BlankNode node)
    {
        return node;
    }


    @Override
    public Node visit(LiteralNode literal)
    {
        return literal;
    }


    private BlankNode getBlankNode()
    {
        String name = null;

        do
        {
            name = blankNodePrefix + blankNodeId++;
        }
        while(usedBlankNodes.contains(name));

        usedBlankNodes.add(name);
        return new BlankNode(name);
    }
}


class GroupGraphPatternVisitor extends BaseVisitor<Stream<Pattern>>
{
    private final SparqlDatabaseConfiguration config;
    private final Prologue prologue;
    private final Stack<VarOrIri> graphs;
    private final Stack<VarOrIri> services;
    private final VariableScopes scopes;
    private final Set<String> usedBlankNodes;
    private final List<TranslateMessage> messages;


    public GroupGraphPatternVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, Set<String> usedBlankNodes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.graphs = graphs;
        this.services = services;
        this.scopes = scopes;
        this.usedBlankNodes = usedBlankNodes;
        this.messages = messages;
    }


    public Stream<Pattern> visitIfNotNull(ParseTree ctx)
    {
        if(ctx == null)
            return Stream.empty();

        return visit(ctx);
    }


    @Override
    public Stream<Pattern> visitGroupGraphPatternSub(GroupGraphPatternSubContext ctx)
    {
        Stream<Pattern> triplesPatterns = visitIfNotNull(ctx.triplesBlock());

        Stream<Pattern> groupGraphPatterns = ctx.groupGraphPatternSubList().stream().flatMap(this::visit);

        return Stream.concat(triplesPatterns, groupGraphPatterns);
    }


    @Override
    public Stream<Pattern> visitTriplesBlock(TriplesBlockContext ctx)
    {
        PropertiesVisitor propertiesVisitor = new PropertiesVisitor(config, prologue, scopes, messages);
        NodeVisitor nodeVisitor = new NodeVisitor(config, prologue, scopes, messages);
        List<ComplexTriple> triples = new LinkedList<>();

        for(TriplesSameSubjectPathContext triplesCtx : ctx.triplesSameSubjectPath())
        {
            if(triplesCtx.varOrTerm() != null)
            {
                ComplexNode node = nodeVisitor.visit(triplesCtx.varOrTerm());
                Stream<Property> properties = propertiesVisitor.visit(triplesCtx.propertyListPathNotEmpty());
                triples.add(new ComplexTriple(node, properties.toList()));
            }
            else
            {
                ComplexNode node = nodeVisitor.visit(triplesCtx.triplesNodePath());
                Stream<Property> properties = propertiesVisitor.visit(triplesCtx.propertyListPath());
                triples.add(new ComplexTriple(node, properties.toList()));
            }
        }


        TripleExpander tripleExpander = new TripleExpander(usedBlankNodes);

        for(ComplexTriple triple : triples)
            tripleExpander.visit(triple);

        return tripleExpander.getResults().stream();
    }


    @Override
    public Stream<Pattern> visitGroupGraphPatternSubList(GroupGraphPatternSubListContext ctx)
    {
        PatternVisitor patternVisitor = new PatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes,
                messages);

        Pattern graphPattern = patternVisitor.visit(ctx.graphPatternNotTriples());

        Stream<Pattern> triplesPatterns = visitIfNotNull(ctx.triplesBlock());

        return Stream.concat(Stream.of(graphPattern), triplesPatterns);
    }
}


class PatternVisitor extends BaseVisitor<Pattern>
{
    private final SparqlDatabaseConfiguration config;
    private final Prologue prologue;
    private final Stack<VarOrIri> graphs;
    private final Stack<VarOrIri> services;
    private final VariableScopes scopes;
    private final List<TranslateMessage> messages;
    private final GraphPatternVisitor graphPatternVisitor;
    private final ExpressionVisitor expressionVisitor;


    public PatternVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> graphs,
            Stack<VarOrIri> services, VariableScopes scopes, Set<String> usedBlankNodes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.graphs = graphs;
        this.services = services;
        this.scopes = scopes;
        this.messages = messages;
        this.graphPatternVisitor = new GraphPatternVisitor(config, prologue, graphs, services, scopes, usedBlankNodes,
                messages);
        this.expressionVisitor = new ExpressionVisitor(config, prologue, graphs, services, scopes, usedBlankNodes,
                messages, false);
    }


    @Override
    public Pattern visitGroupOrUnionGraphPattern(GroupOrUnionGraphPatternContext ctx)
    {
        List<GraphPattern> patterns = ctx.groupGraphPattern().stream().map(graphPatternVisitor::visit).toList();

        if(patterns.size() == 1)
        {
            return patterns.get(0);
        }

        return new Union(patterns);
    }


    @Override
    public Optional visitOptionalGraphPattern(OptionalGraphPatternContext ctx)
    {
        return new Optional(graphPatternVisitor.visit(ctx.groupGraphPattern()));
    }


    @Override
    public Minus visitMinusGraphPattern(MinusGraphPatternContext ctx)
    {
        scopes.addScope();

        try
        {
            return new Minus(graphPatternVisitor.visit(ctx.groupGraphPattern()));
        }
        finally
        {
            scopes.popScope();
        }
    }


    @Override
    public Graph visitGraphGraphPattern(GraphGraphPatternContext ctx)
    {
        VarOrIri name = new NodeVisitor(config, prologue, scopes, messages).parseVarOrIri(ctx.varOrIri());

        graphs.add(name);
        GraphPattern pattern = graphPatternVisitor.visit(ctx.groupGraphPattern());
        graphs.pop();

        return new Graph(name, pattern);
    }


    @Override
    public Service visitServiceGraphPattern(ServiceGraphPatternContext ctx)
    {
        VarOrIri name = new NodeVisitor(config, prologue, scopes, messages).parseVarOrIri(ctx.varOrIri());

        services.add(name);
        GraphPattern pattern = graphPatternVisitor.visit(ctx.groupGraphPattern());
        services.pop();

        return new Service(name, pattern, ctx.SILENT() != null);
    }


    @Override
    public Filter visitFilter(FilterContext ctx)
    {
        Filter filter = new Filter(expressionVisitor.visit(ctx.constraint()));

        return filter;
    }


    @Override
    public Bind visitBind(BindContext ctx)
    {
        Bind bind = new Bind(expressionVisitor.visit(ctx.expression()),
                withRange(new VariableNode(scopes.addToScope(ctx.var().getText()), ctx.var().getText()), ctx.var()));

        return bind;
    }


    @Override
    public Values visitDataBlock(DataBlockContext ctx)
    {
        if(ctx.inlineDataOneVar() != null)
        {
            return visitInlineDataOneVar(ctx.inlineDataOneVar());
        }

        return visitInlineDataFull(ctx.inlineDataFull());
    }


    @Override
    public Values visitInlineDataOneVar(InlineDataOneVarContext ctx)
    {
        VariableNode variable = withRange(new VariableNode(scopes.addToScope(ctx.var().getText()), ctx.var().getText()),
                ctx.var());
        List<Values.ValuesList> valuesLists = ctx.dataBlockValue().stream()
                .map(value -> new Values.ValuesList(Collections.singleton(createVal(value)))).toList();

        return withRange(new Values(Collections.singleton(variable), valuesLists), ctx);
    }


    @Override
    public Values visitInlineDataFull(InlineDataFullContext ctx)
    {
        List<VariableNode> variables = ctx.var().stream()
                .map(var -> withRange(new VariableNode(scopes.addToScope(var.getText()), var.getText()), var)).toList();
        List<Values.ValuesList> valuesLists = new LinkedList<>();


        Set<VariableNode> valueVariables = new HashSet<>();

        for(VariableNode var : variables)
            if(valueVariables.contains(var))
                messages.add(new TranslateMessage(MessageType.repeatOfValuesVariable, var.getRange(), var.getName()));


        for(DataBlockValuesContext block : ctx.dataBlockValues())
        {
            List<Expression> values = block.dataBlockValue().stream().map(this::createVal)
                    .collect(toCollection(ArrayList::new));

            if(values.size() != variables.size())
            {
                messages.add(new TranslateMessage(MessageType.wrongNumberOfValues, Range.compute(block)));

                for(int i = 0; i < variables.size() - values.size(); i++)
                    values.add(null);
            }

            valuesLists.add(withRange(new ValuesList(values), block));
        }

        return withRange(new Values(variables, valuesLists), ctx);
    }


    Expression createVal(DataBlockValueContext value)
    {
        Expression ret = new LiteralVisitor(prologue, messages).visit(value);

        if(ret == null)
            ret = new IriVisitor(prologue, messages).visit(value);

        return ret;
    }
}


class IriVisitor extends BaseVisitor<IriNode>
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public IriVisitor(Prologue prologue, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.messages = messages;
    }


    @Override
    public IriNode visitIri(IriContext ctx)
    {
        if(ctx.IRIREF() == null && ctx.prefixedName() == null)
            return null;

        if(ctx.IRIREF() != null)
            return new IriNode(parseUri(ctx.IRIREF(), prologue));

        return visit(ctx.prefixedName());
    }


    public String parseUri(TerminalNode iriRef, Prologue prologue)
    {
        String uri = iriRef.getText();
        uri = uri.substring(1, uri.length() - 1);

        try
        {
            if(!(new URI(uri)).isAbsolute())
                uri = (new URI(prologue.getBase())).resolve(uri).toString();
        }
        catch(URISyntaxException e)
        {
            Range range = Range.compute(iriRef.getSymbol(), iriRef.getSymbol());
            messages.add(new TranslateMessage(MessageType.malformedIri, range));
        }

        return uri;
    }


    @Override
    public IriNode visitPrefixedName(PrefixedNameContext ctx)
    {
        PrefixedName prefixedName = parsePrefixedName(ctx);

        String prefix = prologue.getPrefixes().get(prefixedName.getPrefix());

        if(prefix == null)
        {
            messages.add(
                    new TranslateMessage(MessageType.unknownPrefix, prefixedName.getRange(), prefixedName.getPrefix()));
            return new IriNode(prefixedName.getPrefix() + ":" + prefixedName.getLocalName());
        }

        return new IriNode(prefix + prefixedName.getLocalName());
    }


    public static PrefixedName parsePrefixedName(PrefixedNameContext ctx)
    {
        String[] parts = ctx.getText().split(":", 2);

        parts[1] = parts[1].replace("\\", "");

        return withRange(new PrefixedName(parts[0], parts[1]), ctx);
    }
}
