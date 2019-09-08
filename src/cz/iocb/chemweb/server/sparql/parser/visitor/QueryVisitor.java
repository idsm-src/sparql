package cz.iocb.chemweb.server.sparql.parser.visitor;

import static cz.iocb.chemweb.server.sparql.parser.StreamUtils.mapList;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.error.MessageType;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.AggregateContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BaseDeclContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BindContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BlankNodeContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DataBlockContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DataBlockValueContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DataBlockValuesContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DatasetClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ExistsFunctionContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.FilterContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.GraphGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.GroupClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.GroupConditionContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.GroupGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.GroupGraphPatternSubContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.GroupGraphPatternSubListContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.GroupOrUnionGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.HavingClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.InlineDataFullContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.InlineDataOneVarContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.IriContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.LimitOffsetClausesContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.MinusGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NotExistsFunctionContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.OptionalGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.OrderClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.OrderConditionContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PrefixDeclContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PrefixedNameContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.QueryContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SelectClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SelectVariableContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ServiceGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SolutionModifierContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SubSelectContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.TriplesBlockContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.TriplesSameSubjectContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.TriplesSameSubjectPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ValuesClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.VarContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.WhereClauseContext;
import cz.iocb.chemweb.server.sparql.mapping.procedure.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.parser.ComplexElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.Rdf;
import cz.iocb.chemweb.server.sparql.parser.model.AskQuery;
import cz.iocb.chemweb.server.sparql.parser.model.ConstructQuery;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.Prefix;
import cz.iocb.chemweb.server.sparql.parser.model.PrefixDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.PrefixedName;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Query;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BracketedExpression;
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
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCallBase.Parameter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values.ValuesList;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNodePropertyList;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexTriple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Property;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RdfCollection;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;



public class QueryVisitor extends BaseVisitor<Query>
{
    private final SparqlDatabaseConfiguration config;
    private final Stack<VarOrIri> services;
    private final HashSet<String> usedBlankNodes;
    private final HashSet<Node> usedParameterNodes;
    private final HashSet<Node> usedResultNodes;
    private final List<TranslateMessage> messages;
    private Prologue prologue;


    public QueryVisitor(SparqlDatabaseConfiguration config, List<TranslateMessage> messages)
    {
        this.config = config;
        this.services = new Stack<VarOrIri>();
        this.usedBlankNodes = new HashSet<String>();
        this.usedParameterNodes = new HashSet<Node>();
        this.usedResultNodes = new HashSet<Node>();
        this.messages = messages;
    }


    public QueryVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> services,
            HashSet<String> usedBlankNodes, HashSet<Node> usedParameterNodes, HashSet<Node> usedResultNodes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.services = services;
        this.usedBlankNodes = usedBlankNodes;
        this.usedParameterNodes = usedParameterNodes;
        this.usedResultNodes = usedResultNodes;
        this.messages = messages;
    }


    @Override
    public Query visitQuery(QueryContext ctx)
    {
        new BaseVisitor<Void>()
        {
            @Override
            public Void visitBlankNode(BlankNodeContext ctx)
            {
                usedBlankNodes.add(ctx.getText().replaceFirst("^:_", ""));
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
            GraphPattern pattern = new GraphPatternVisitor(config, prologue, services, usedBlankNodes,
                    usedParameterNodes, usedResultNodes, messages).visit(ctx.askQuery().whereClause());
            Values values = parseValues(ctx.valuesClause());

            Select select = withRange(new Select(new LinkedList<Projection>(), pattern, values, true), ctx);
            select.setReduced(true);

            if(ctx.askQuery().datasetClause() != null)
                select.getDataSets().addAll(mapList(ctx.askQuery().datasetClause(), this::parseDataSet));

            select.getGroupByConditions().addAll(parseGroupClause(ctx.askQuery().groupClause()));
            select.getHavingConditions().addAll(parseHavingClause(ctx.askQuery().havingClause()));
            select.setIsInAggregateMode(isInAggregateMode(ctx.askQuery().groupClause(), ctx.askQuery().havingClause()));

            result = new AskQuery(prologue, select);
        }
        else if(ctx.constructQuery() != null)
        {
            PropertiesVisitor propertiesVisitor = new PropertiesVisitor(prologue, messages);
            NodeVisitor nodeVisitor = new NodeVisitor(prologue, messages);
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
                        expander.visit(new ComplexTriple(node, properties.collect(Collectors.toList())));
                    }
                    else
                    {
                        ComplexNode node = nodeVisitor.visit(triplesCtx.triplesNode());
                        Stream<Property> properties = propertiesVisitor.visit(triplesCtx.propertyList());
                        expander.visit(new ComplexTriple(node, properties.collect(Collectors.toList())));
                    }
                }
            }

            List<Pattern> templates = expander.getResults();


            LinkedList<Projection> projections = new LinkedList<Projection>();

            BaseVisitor<Void> variableVisitor = new BaseVisitor<Void>()
            {
                HashSet<String> variables = new HashSet<String>();

                @Override
                public Void visitVar(VarContext ctx)
                {
                    String variable = ctx.getText().substring(1);

                    if(!variables.contains(variable))
                    {
                        variables.add(variable);
                        projections.add(new Projection(new Variable(variable)));
                    }

                    return null;
                }
            };

            variableVisitor.visit(ctx.constructQuery().constructTemplate());


            GraphPattern pattern = ctx.constructQuery().whereClause() != null ?
                    new GraphPatternVisitor(config, prologue, services, usedBlankNodes, usedParameterNodes,
                            usedResultNodes, messages).visit(ctx.constructQuery().whereClause()) :
                    withRange(
                            new GroupGraph(new GroupGraphPatternVisitor(config, prologue, services, usedBlankNodes,
                                    usedParameterNodes, usedResultNodes, messages).assembleProcedureCalls(templates)),
                            ctx.constructQuery().constructTemplate());

            Values values = parseValues(ctx.valuesClause());

            Select select = withRange(new Select(projections, pattern, values, true), ctx);
            select.setReduced(true);

            if(ctx.constructQuery().datasetClause() != null)
                select.getDataSets().addAll(mapList(ctx.constructQuery().datasetClause(), this::parseDataSet));

            SolutionModifierContext solutionModifierCtx = ctx.constructQuery().solutionModifier();

            select.getGroupByConditions().addAll(parseGroupClause(solutionModifierCtx.groupClause()));
            select.getHavingConditions().addAll(parseHavingClause(solutionModifierCtx.havingClause()));
            select.getOrderByConditions().addAll(parseOrderClause(solutionModifierCtx.orderClause()));
            select.setLimit(parseLimitClause(solutionModifierCtx.limitOffsetClauses()));
            select.setOffset(parseOffsetClause(solutionModifierCtx.limitOffsetClauses()));
            select.setIsInAggregateMode(isInAggregateMode(solutionModifierCtx));

            result = new ConstructQuery(prologue, templates, select);
        }


        //TODO: the check could be less strict in a future version
        new ElementVisitor<Void>()
        {
            @Override
            public Void visit(ConstructQuery constructQuery)
            {
                visitElement(constructQuery.getPrologue());
                visitElement(constructQuery.getSelect());
                return defaultResult();
            }

            @Override
            public Void visit(Variable variable)
            {
                if(usedParameterNodes.contains(variable))
                    messages.add(new TranslateMessage(MessageType.invalidUseOfParameterNode, variable.getRange()));

                if(usedResultNodes.contains(variable))
                    messages.add(new TranslateMessage(MessageType.invalidUseOfResultNode, variable.getRange()));

                return defaultResult();
            }

            @Override
            public Void visit(BlankNode blankNode)
            {
                if(usedParameterNodes.contains(blankNode))
                    messages.add(new TranslateMessage(MessageType.invalidUseOfParameterNode, blankNode.getRange()));

                if(usedResultNodes.contains(blankNode))
                    messages.add(new TranslateMessage(MessageType.invalidUseOfResultNode, blankNode.getRange()));

                return defaultResult();
            }
        }.visitElement(result);


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

        return (Values) new PatternVisitor(config, prologue, services, usedBlankNodes, usedParameterNodes,
                usedResultNodes, messages).visit(dataBlockCtx);
    }


    public boolean isInAggregateMode(ParserRuleContext... contexts)
    {
        BaseVisitor<Boolean> visitor = new BaseVisitor<Boolean>()
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
        LinkedList<Projection> projections = new LinkedList<Projection>();

        if(!selectClauseCtx.selectVariable().isEmpty())
        {
            projections.addAll(mapList(selectClauseCtx.selectVariable(), this::parseProjection));
        }
        else if(!isInAggregateMode(selectClauseCtx, solutionModifierCtx))
        {
            HashSet<String> variables = new HashSet<String>();

            BaseVisitor<Void> variableVisitor = new BaseVisitor<Void>()
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
                        projections.add(new Projection(new Variable(variable)));
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


        GraphPattern pattern = new GraphPatternVisitor(config, prologue, services, usedBlankNodes, usedParameterNodes,
                usedResultNodes, messages).visit(whereClauseCtx);
        Values values = parseValues(valuesClauseContext);

        Select result = new Select(projections, pattern, values, isSubSelect);


        if(selectClauseCtx.DISTINCT() != null)
            result.setDistinct(true);
        if(selectClauseCtx.REDUCED() != null)
            result.setReduced(true);

        if(dataSetClauseCtxs != null)
            result.getDataSets().addAll(mapList(dataSetClauseCtxs, this::parseDataSet));

        result.getGroupByConditions().addAll(parseGroupClause(solutionModifierCtx.groupClause()));
        result.getHavingConditions().addAll(parseHavingClause(solutionModifierCtx.havingClause()));
        result.getOrderByConditions().addAll(parseOrderClause(solutionModifierCtx.orderClause()));
        result.setLimit(parseLimitClause(solutionModifierCtx.limitOffsetClauses()));
        result.setOffset(parseOffsetClause(solutionModifierCtx.limitOffsetClauses()));
        result.setIsInAggregateMode(isInAggregateMode(selectClauseCtx, solutionModifierCtx));

        return result;
    }


    private Projection parseProjection(SelectVariableContext variableCtx)
    {
        Variable variable = withRange(new Variable(variableCtx.var().getText()), variableCtx.var());

        if(variableCtx.expression() != null)
        {
            Expression expression = new ExpressionVisitor(config, prologue, services, usedBlankNodes,
                    usedParameterNodes, usedResultNodes, messages).visit(variableCtx.expression());

            return new Projection(expression, variable);
        }

        return withRange(new Projection(variable), variableCtx);
    }


    private DataSet parseDataSet(DatasetClauseContext dataSetCtx)
    {
        IRI iri = new IriVisitor(prologue, messages).visit(dataSetCtx.iri());
        boolean isDefault = dataSetCtx.NAMED() == null;

        return withRange(new DataSet(iri, isDefault), dataSetCtx);
    }


    private List<GroupCondition> parseGroupClause(GroupClauseContext ctx)
    {
        if(ctx == null)
            return new ArrayList<GroupCondition>();

        return mapList(ctx.groupCondition(), this::parseGroupCondition);
    }


    private GroupCondition parseGroupCondition(GroupConditionContext ctx)
    {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(config, prologue, services, usedBlankNodes,
                usedParameterNodes, usedResultNodes, messages);

        if(ctx.expression() != null)
        {
            Expression expression = expressionVisitor.visit(ctx.expression());

            if(ctx.var() == null)
            {
                return new GroupCondition(new BracketedExpression(expression));
            }

            Variable variable = withRange(new Variable(ctx.var().getText()), ctx.var());

            return new GroupCondition(expression, variable);
        }

        return withRange(new GroupCondition(expressionVisitor.visit(ctx)), ctx);
    }


    private List<Expression> parseHavingClause(HavingClauseContext ctx)
    {
        if(ctx == null)
            return new ArrayList<Expression>();

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(config, prologue, services, usedBlankNodes,
                usedParameterNodes, usedResultNodes, messages);

        return mapList(ctx.havingCondition(), expressionVisitor::visit);
    }


    private List<OrderCondition> parseOrderClause(OrderClauseContext ctx)
    {
        if(ctx == null)
            return new ArrayList<OrderCondition>();

        return mapList(ctx.orderCondition(), this::parseOrderCondition);
    }


    private OrderCondition parseOrderCondition(OrderConditionContext ctx)
    {
        if(ctx.ASC() != null || ctx.DESC() != null)
        {
            OrderCondition.Direction direction = ctx.ASC() != null ? OrderCondition.Direction.Ascending :
                    OrderCondition.Direction.Descending;

            return new OrderCondition(direction, new ExpressionVisitor(config, prologue, services, usedBlankNodes,
                    usedParameterNodes, usedResultNodes, messages).visit(ctx.expression()));
        }

        return withRange(new OrderCondition(new ExpressionVisitor(config, prologue, services, usedBlankNodes,
                usedParameterNodes, usedResultNodes, messages).visit(ctx)), ctx);
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

        prologue.setBase(new IRI(uri));

        return null;
    }


    @Override
    public Void visitPrefixDecl(PrefixDeclContext ctx)
    {
        String name = ctx.PNAME_NS().getText();
        IRI iri = new IRI(new IriVisitor(prologue, messages).parseUri(ctx.IRIREF(), prologue));

        PrefixDefinition result = withRange(new PrefixDefinition(name, iri), ctx);
        prologue.addPrefixDefinition(result);

        return null;
    }
}


class GraphPatternVisitor extends BaseVisitor<GraphPattern>
{
    private final SparqlDatabaseConfiguration config;
    private final Prologue prologue;
    private final Stack<VarOrIri> services;
    private final HashSet<String> usedBlankNodes;
    private final HashSet<Node> usedParameterNodes;
    private final HashSet<Node> usedResultNodes;
    private final List<TranslateMessage> messages;


    public GraphPatternVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> services,
            HashSet<String> usedBlankNodes, HashSet<Node> usedParameterNodes, HashSet<Node> usedResultNodes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.services = services;
        this.usedBlankNodes = usedBlankNodes;
        this.usedParameterNodes = usedParameterNodes;
        this.usedResultNodes = usedResultNodes;
        this.messages = messages;
    }


    @Override
    public GraphPattern visitGroupGraphPattern(GroupGraphPatternContext ctx)
    {
        if(ctx.subSelect() == null && ctx.groupGraphPatternSub() == null)
            return new GroupGraph(new ArrayList<Pattern>());

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
        List<Pattern> patterns = new GroupGraphPatternVisitor(config, prologue, services, usedBlankNodes,
                usedParameterNodes, usedResultNodes, messages).visit(ctx).collect(Collectors.toList());

        return new GroupGraph(patterns);
    }


    @Override
    public GraphPattern visitSubSelect(SubSelectContext ctx)
    {
        return withRange(new QueryVisitor(config, prologue, services, usedBlankNodes, usedParameterNodes,
                usedResultNodes, messages).parseSelect(ctx.selectClause(), null, ctx.whereClause(),
                        ctx.solutionModifier(), ctx.valuesClause(), true),
                ctx);
    }
}


class TripleExpander extends ComplexElementVisitor<Node>
{
    private final List<Pattern> results = new ArrayList<Pattern>();
    private final HashSet<String> usedBlankNodes;

    private final String blankNodePrefix = "blanknode";
    private int blankNodeId = 0;


    public TripleExpander(HashSet<String> usedBlankNodes)
    {
        this.usedBlankNodes = usedBlankNodes;
    }


    public List<Pattern> getResults()
    {
        return results;
    }


    @Override
    public Node visitElement(Element element)
    {
        Node result = super.visitElement(element);

        if(result == null && element instanceof Node)
            return (Node) element;

        return result;
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
            return new IRI(Rdf.NIL);

        BlankNode firstNode = getBlankNode();
        firstNode.setRange(rdfCollection.getRange());

        BlankNode currentNode = firstNode;

        for(int i = 0; i < rdfCollection.getNodes().size(); i++)
        {
            BlankNode nextNode = null;

            Triple firstTriple = new Triple(currentNode, new IRI(Rdf.FIRST),
                    visitElement(rdfCollection.getNodes().get(i)));
            firstTriple.setRange(rdfCollection.getRange());
            results.add(firstTriple);

            Node restObject;
            if(i == rdfCollection.getNodes().size() - 1)
            {
                restObject = new IRI(Rdf.NIL);
            }
            else
            {
                nextNode = getBlankNode();
                nextNode.setRange(rdfCollection.getRange());

                restObject = nextNode;
            }

            Triple restTriple = new Triple(currentNode, new IRI(Rdf.REST), restObject);
            restTriple.setRange(rdfCollection.getRange());
            results.add(restTriple);

            currentNode = nextNode;
        }

        return firstNode;
    }


    @Override
    public Node visit(Literal literal)
    {
        return null;
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
    private final Stack<VarOrIri> services;
    private final HashSet<String> usedBlankNodes;
    private final HashSet<Node> usedParameterNodes;
    private final HashSet<Node> usedResultNodes;
    private final List<TranslateMessage> messages;


    public GroupGraphPatternVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> services,
            HashSet<String> usedBlankNodes, HashSet<Node> usedParameterNodes, HashSet<Node> usedResultNodes,
            List<TranslateMessage> messages)
    {
        this.config = config;
        this.prologue = prologue;
        this.services = services;
        this.usedBlankNodes = usedBlankNodes;
        this.usedParameterNodes = usedParameterNodes;
        this.usedResultNodes = usedResultNodes;
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
        PropertiesVisitor propertiesVisitor = new PropertiesVisitor(prologue, messages);
        NodeVisitor nodeVisitor = new NodeVisitor(prologue, messages);
        List<ComplexTriple> triples = new LinkedList<ComplexTriple>();

        for(TriplesSameSubjectPathContext triplesCtx : ctx.triplesSameSubjectPath())
        {
            if(triplesCtx.varOrTerm() != null)
            {
                ComplexNode node = nodeVisitor.visit(triplesCtx.varOrTerm());
                Stream<Property> properties = propertiesVisitor.visit(triplesCtx.propertyListPathNotEmpty());
                triples.add(new ComplexTriple(node, properties.collect(Collectors.toList())));
            }
            else
            {
                ComplexNode node = nodeVisitor.visit(triplesCtx.triplesNodePath());
                Stream<Property> properties = propertiesVisitor.visit(triplesCtx.propertyListPath());
                triples.add(new ComplexTriple(node, properties.collect(Collectors.toList())));
            }
        }


        TripleExpander tripleExpander = new TripleExpander(usedBlankNodes);

        for(ComplexTriple triple : triples)
            tripleExpander.visit(triple);

        return assembleProcedureCalls(tripleExpander.getResults()).stream();
    }


    @Override
    public Stream<Pattern> visitGroupGraphPatternSubList(GroupGraphPatternSubListContext ctx)
    {
        PatternVisitor patternVisitor = new PatternVisitor(config, prologue, services, usedBlankNodes,
                usedParameterNodes, usedResultNodes, messages);

        Pattern graphPattern = patternVisitor.visit(ctx.graphPatternNotTriples());

        Stream<Pattern> triplesPatterns = visitIfNotNull(ctx.triplesBlock());

        return Stream.concat(Stream.of(graphPattern), triplesPatterns);
    }



    public List<Pattern> assembleProcedureCalls(List<Pattern> patterns)
    {
        if(!services.empty())
            return patterns;


        HashSet<Pattern> callPatterns = new HashSet<Pattern>();
        HashMap<Node, List<Parameter>> parameterNodeMap = new HashMap<Node, List<Parameter>>();
        HashMap<Node, List<Parameter>> resultNodeMap = new HashMap<Node, List<Parameter>>();


        for(Pattern pattern : patterns)
        {
            Triple triple = (Triple) pattern;
            Verb predicate = triple.getPredicate();

            if(predicate instanceof IRI)
            {
                IRI procedureName = (IRI) predicate;
                ProcedureDefinition definition = config.getProcedures().get(procedureName.getValue());

                if(definition != null)
                {
                    callPatterns.add(triple);

                    Node parameterNode = triple.getObject();

                    if(parameterNode instanceof VariableOrBlankNode)
                    {
                        parameterNodeMap.put(parameterNode, new ArrayList<Parameter>());

                        if(!usedParameterNodes.contains(parameterNode) && !usedResultNodes.contains(parameterNode))
                            usedParameterNodes.add(parameterNode);
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

                            if(!usedParameterNodes.contains(resultNode) && !usedResultNodes.contains(resultNode))
                                usedResultNodes.add(resultNode);
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
                        if(config.getProcedures().get(iri.getValue()) != null)
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
            Triple triple = (Triple) iterator.next();

            if(callPatterns.contains(triple))
                continue;


            List<Parameter> parameters = parameterNodeMap.get(triple.getSubject());

            if(parameters != null)
            {
                iterator.remove();

                if(triple.getPredicate() instanceof IRI)
                {
                    IRI name = (IRI) triple.getPredicate();
                    parameters.add(new Parameter(name, triple.getObject()));
                }
                else
                {
                    //TODO: could be supported in a future version
                    messages.add(new TranslateMessage(MessageType.invalidProcedureParameterValue,
                            triple.getPredicate().getRange()));
                }
            }


            List<Parameter> results = resultNodeMap.get(triple.getSubject());

            if(results != null)
            {
                iterator.remove();

                if(triple.getPredicate() instanceof IRI)
                {
                    IRI name = (IRI) triple.getPredicate();
                    results.add(new Parameter(name, triple.getObject()));
                }
                else
                {
                    //TODO: could be supported in a future version
                    messages.add(new TranslateMessage(MessageType.invalidProcedureResultValue,
                            triple.getPredicate().getRange()));
                }
            }
        }


        ListIterator<Pattern> listIterator = resultPatterns.listIterator();

        while(listIterator.hasNext())
        {
            Triple triple = (Triple) listIterator.next();

            if(!callPatterns.contains(triple))
                continue;

            IRI name = (IRI) triple.getPredicate();
            ProcedureDefinition definition = config.getProcedures().get(name.getValue());

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
}


class PatternVisitor extends BaseVisitor<Pattern>
{
    private final Prologue prologue;
    private final Stack<VarOrIri> services;
    private final List<TranslateMessage> messages;
    private final GraphPatternVisitor graphPatternVisitor;
    private final ExpressionVisitor expressionVisitor;


    public PatternVisitor(SparqlDatabaseConfiguration config, Prologue prologue, Stack<VarOrIri> services,
            HashSet<String> usedBlankNodes, HashSet<Node> usedParameterNodes, HashSet<Node> usedResultNodes,
            List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.services = services;
        this.messages = messages;
        this.graphPatternVisitor = new GraphPatternVisitor(config, prologue, services, usedBlankNodes,
                usedParameterNodes, usedResultNodes, messages);
        this.expressionVisitor = new ExpressionVisitor(config, prologue, services, usedBlankNodes, usedParameterNodes,
                usedResultNodes, messages);
    }


    @Override
    public Pattern visitGroupOrUnionGraphPattern(GroupOrUnionGraphPatternContext ctx)
    {
        List<GraphPattern> patterns = mapList(ctx.groupGraphPattern(), graphPatternVisitor::visit);

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
        return new Minus(graphPatternVisitor.visit(ctx.groupGraphPattern()));
    }


    @Override
    public Graph visitGraphGraphPattern(GraphGraphPatternContext ctx)
    {
        return new Graph(new NodeVisitor(prologue, messages).parseVarOrIri(ctx.varOrIRI()),
                graphPatternVisitor.visit(ctx.groupGraphPattern()));
    }


    @Override
    public Service visitServiceGraphPattern(ServiceGraphPatternContext ctx)
    {
        VarOrIri name = new NodeVisitor(prologue, messages).parseVarOrIri(ctx.varOrIRI());

        services.add(name);
        GraphPattern pattern = graphPatternVisitor.visit(ctx.groupGraphPattern());
        services.pop();

        return new Service(name, pattern, ctx.SILENT() != null);
    }


    @Override
    public Filter visitFilter(FilterContext ctx)
    {
        return new Filter(expressionVisitor.visit(ctx.constraint()));
    }


    @Override
    public Bind visitBind(BindContext ctx)
    {
        return new Bind(expressionVisitor.visit(ctx.expression()),
                withRange(new Variable(ctx.var().getText()), ctx.var()));
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
        Variable variable = withRange(new Variable(ctx.var().getText()), ctx.var());
        List<Values.ValuesList> valuesLists = mapList(ctx.dataBlockValue(),
                value -> new Values.ValuesList(Collections.singleton(createVal(value))));

        return withRange(new Values(Collections.singleton(variable), valuesLists), ctx);
    }


    @Override
    public Values visitInlineDataFull(InlineDataFullContext ctx)
    {
        List<Variable> variables = mapList(ctx.var(), var -> withRange(new Variable(var.getText()), var));
        List<Values.ValuesList> valuesLists = new LinkedList<Values.ValuesList>();


        for(DataBlockValuesContext block : ctx.dataBlockValues())
        {
            List<Expression> values = mapList(block.dataBlockValue(), this::createVal);

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


class IriVisitor extends BaseVisitor<IRI>
{
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public IriVisitor(Prologue prologue, List<TranslateMessage> messages)
    {
        this.prologue = prologue;
        this.messages = messages;
    }


    @Override
    public IRI visitIri(IriContext ctx)
    {
        if(ctx.IRIREF() == null && ctx.prefixedName() == null)
            return null;

        if(ctx.IRIREF() != null)
            return new IRI(parseUri(ctx.IRIREF(), prologue));

        return visit(ctx.prefixedName());
    }


    public String parseUri(TerminalNode iriRef, Prologue prologue)
    {
        String uri = iriRef.getText();
        uri = uri.substring(1, uri.length() - 1);

        try
        {
            if(!(new URI(uri)).isAbsolute())
                uri = (new URI(prologue.getBase().getValue())).resolve(uri).toString();
        }
        catch(URISyntaxException e)
        {
            Range range = Range.compute(iriRef.getSymbol(), iriRef.getSymbol());
            messages.add(new TranslateMessage(MessageType.malformedIri, range));
        }

        return uri;
    }


    @Override
    public IRI visitPrefixedName(PrefixedNameContext ctx)
    {
        PrefixedName prefixedName = parsePrefixedName(ctx);

        java.util.Optional<Prefix> prefix = prologue.getPrefixes().stream()
                .filter(p -> p.getName().equals(prefixedName.getPrefix())).findFirst();

        if(!prefix.isPresent())
        {
            messages.add(
                    new TranslateMessage(MessageType.unknownPrefix, prefixedName.getRange(), prefixedName.getPrefix()));
            return new IRI(prefixedName.getPrefix() + ":" + prefixedName.getLocalName());
        }

        return new IRI(prefix.get().getIri() + prefixedName.getLocalName());
    }


    public static PrefixedName parsePrefixedName(PrefixedNameContext ctx)
    {
        String[] parts = ctx.getText().split(":", 2);

        parts[1] = parts[1].replace("\\", "");

        return withRange(new PrefixedName(parts[0], parts[1]), ctx);
    }
}
