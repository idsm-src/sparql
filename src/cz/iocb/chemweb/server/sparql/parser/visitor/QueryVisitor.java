package cz.iocb.chemweb.server.sparql.parser.visitor;

import static cz.iocb.chemweb.server.sparql.parser.StreamUtils.mapList;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.v4.runtime.tree.ParseTree;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BaseDeclContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.BindContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.CollectionContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.CollectionPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DataBlockContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DataBlockValueContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DatasetClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DefineDeclContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.DefineValueContext;
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
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.LimitClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.LimitOffsetClausesContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.MinusGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.NilContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.OffsetClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.OptionalGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.OrderClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.OrderConditionContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PrefixDeclContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.PrefixedNameContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.QueryContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SelectClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SelectQueryContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SelectVariableContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ServiceGraphPatternContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SolutionModifierContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.SubSelectContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.TriplesBlockContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.TriplesSameSubjectPathContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.ValuesClauseContext;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParser.WhereClauseContext;
import cz.iocb.chemweb.server.sparql.parser.ComplexElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Range;
import cz.iocb.chemweb.server.sparql.parser.Rdf;
import cz.iocb.chemweb.server.sparql.parser.error.ErrorType;
import cz.iocb.chemweb.server.sparql.parser.error.ParseException;
import cz.iocb.chemweb.server.sparql.parser.error.UncheckedParseException;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.Define;
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
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BracketedExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.BasicPattern;
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
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNodePropertyList;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexTriple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Property;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RdfCollection;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;
import cz.iocb.chemweb.server.sparql.procedure.ProcedureDefinition;



public class QueryVisitor extends BaseVisitor<Query>
{
    // This is not very nice, but it's better than passing these values
    // everywhere.
    // It needs to be improved if more than one query is to be parsed at the
    // same time on the same thread.
    private static ThreadLocal<Prologue> currentPrologue = new ThreadLocal<>();
    private static ThreadLocal<LinkedHashMap<String, ProcedureDefinition>> currentProcedures = new ThreadLocal<>();
    private static ThreadLocal<List<Prefix>> currentPredefinedPrefixes = new ThreadLocal<>();
    private static ThreadLocal<Consumer<ParseException>> currentExceptionConsumer = new ThreadLocal<>();

    /**
     * Returns prologue for the tree that's currently being processed on the current thread.
     */
    public static Prologue getPrologue()
    {
        return currentPrologue.get();
    }

    public static LinkedHashMap<String, ProcedureDefinition> getProcedures()
    {
        return currentProcedures.get();
    }

    public static void setProcedures(LinkedHashMap<String, ProcedureDefinition> procedures)
    {
        currentProcedures.set(procedures);
    }

    public static List<Prefix> getPredefinedPrefixes()
    {
        return currentPredefinedPrefixes.get();
    }

    public static void setPredefinedPrefixes(Collection<Prefix> predefinedPrefixes)
    {
        currentPredefinedPrefixes.set(new ArrayList<>(predefinedPrefixes));
    }

    public static Consumer<ParseException> getExceptionConsumer()
    {
        return currentExceptionConsumer.get();
    }

    public static void setExceptionConsumer(Consumer<ParseException> exceptionConsumer)
    {
        currentExceptionConsumer.set(exceptionConsumer);
    }

    class ContainsCollectionVisitor extends BaseVisitor<Boolean>
    {
        @Override
        protected Boolean defaultResult()
        {
            return false;
        }

        @Override
        protected Boolean aggregateResult(Boolean aggregate, Boolean nextResult)
        {
            return aggregate || nextResult;
        }

        @Override
        public Boolean visitCollection(CollectionContext ctx)
        {
            return true;
        }

        @Override
        public Boolean visitCollectionPath(CollectionPathContext ctx)
        {
            return true;
        }

        @Override
        public Boolean visitNil(NilContext ctx)
        {
            return true;
        }
    }

    @Override
    public Query visitQuery(QueryContext ctx)
    {
        Prologue prologue;

        if(ctx.prologue() != null)
        {
            PrologueVisitor visitor = new PrologueVisitor();
            visitor.visit(ctx.prologue());
            prologue = visitor.getPrologue();
        }
        else
            prologue = new Prologue();

        currentPrologue.set(prologue);

        if(ctx.selectQuery() != null)
        {
            SelectQuery result = visitSelectQuery(ctx.selectQuery());

            result.setPrologue(prologue);

            if(new ContainsCollectionVisitor().visit(ctx.selectQuery()))
            {
                IRI rdfIri = new IRI(Rdf.NS);

                // if we find PREFIX with the rdf IRI, we don't need to add a new one
                // if we find PREFIX with the rdf: prefix, adding a new one would cause a conflict this means that if
                // the existing rdf: PREFIX is wrong, expanded RDF collection triplets will use the full IRI
                if(!result.getPrologue().getAllPrefixes().stream()
                        .filter(prefix -> prefix.getName().equals("rdf") || prefix.getIri().equals(Rdf.NS)).findAny()
                        .isPresent())
                {
                    result.getPrologue().addPrefixDefinition(new PrefixDefinition("rdf", rdfIri));
                }
            }

            result.getSelect().setValues(parseValues(ctx.valuesClause()));

            currentPrologue.set(null);

            return result;
        }

        currentPrologue.set(null);

        return null;
    }

    /**
     * @param ctx Can be null.
     */
    public static Values parseValues(ValuesClauseContext ctx)
    {
        if(ctx == null)
            return null;

        DataBlockContext dataBlockCtx = ctx.dataBlock();
        if(dataBlockCtx == null)
            return null;

        return (Values) new PatternVisitor().visit(dataBlockCtx);
    }

    @Override
    public SelectQuery visitSelectQuery(SelectQueryContext ctx)
    {
        Select select = parseSelect(ctx.selectClause(), ctx.datasetClause(), ctx.whereClause(), ctx.solutionModifier());

        return new SelectQuery(select);
    }

    public static Select parseSelect(SelectClauseContext selectClauseCtx, List<DatasetClauseContext> dataSetClauseCtxs,
            WhereClauseContext whereClauseCtx, SolutionModifierContext solutionModifierCtx)
    {
        Select result = withRange(new Select(), selectClauseCtx);

        if(selectClauseCtx.DISTINCT() != null)
            result.setDistinct(true);
        if(selectClauseCtx.REDUCED() != null)
            result.setReduced(true);

        if(!selectClauseCtx.selectVariable().isEmpty())
        {
            result.getProjections().addAll(mapList(selectClauseCtx.selectVariable(), QueryVisitor::parseProjection));
        }

        if(dataSetClauseCtxs != null)
        {
            result.getDataSets().addAll(mapList(dataSetClauseCtxs, QueryVisitor::parseDataSet));
        }

        GraphPattern pattern = new GraphPatternVisitor().visit(whereClauseCtx);

        result.setPattern(pattern);

        parseSolutionModifier(solutionModifierCtx, result);

        return result;
    }

    private static Projection parseProjection(SelectVariableContext variableCtx)
    {
        Variable variable = withRange(new Variable(variableCtx.var().getText()), variableCtx.var());

        if(variableCtx.expression() != null)
        {
            Expression expression = new ExpressionVisitor().visit(variableCtx.expression());

            return new Projection(expression, variable);
        }

        return withRange(new Projection(variable), variableCtx);
    }

    private static DataSet parseDataSet(DatasetClauseContext dataSetCtx)
    {
        IRI iri = new IriVisitor().visit(dataSetCtx.iri());
        boolean isDefault = dataSetCtx.NAMED() == null;

        return withRange(new DataSet(iri, isDefault), dataSetCtx);
    }

    private static void parseSolutionModifier(SolutionModifierContext ctx, Select select)
    {
        parseGroupClause(ctx.groupClause(), select);
        parseHavingClause(ctx.havingClause(), select);
        parseOrderClause(ctx.orderClause(), select);
        parseLimitOffsetClauses(ctx.limitOffsetClauses(), select);
    }

    private static void parseGroupClause(GroupClauseContext ctx, Select select)
    {
        if(ctx == null)
            return;

        List<GroupCondition> conditions = mapList(ctx.groupCondition(), QueryVisitor::parseGroupCondition);

        select.getGroupByConditions().addAll(conditions);
    }

    private static GroupCondition parseGroupCondition(GroupConditionContext ctx)
    {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor();

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

    private static void parseHavingClause(HavingClauseContext ctx, Select select)
    {
        if(ctx == null)
            return;

        ExpressionVisitor expressionVisitor = new ExpressionVisitor();

        List<Expression> conditions = mapList(ctx.havingCondition(), expressionVisitor::visit);

        select.getHavingConditions().addAll(conditions);
    }

    private static void parseOrderClause(OrderClauseContext ctx, Select select)
    {
        if(ctx == null)
            return;

        List<OrderCondition> conditions = mapList(ctx.orderCondition(), QueryVisitor::parseOrderCondition);

        select.getOrderByConditions().addAll(conditions);
    }

    private static OrderCondition parseOrderCondition(OrderConditionContext ctx)
    {
        if(ctx.ASC() != null || ctx.DESC() != null)
        {
            OrderCondition.Direction direction = ctx.ASC() != null ? OrderCondition.Direction.Ascending
                    : OrderCondition.Direction.Descending;

            return new OrderCondition(direction, new ExpressionVisitor().visit(ctx.expression()));
        }

        return withRange(new OrderCondition(new ExpressionVisitor().visit(ctx)), ctx);
    }

    private static void parseLimitOffsetClauses(LimitOffsetClausesContext ctx, Select select)
    {
        if(ctx == null)
            return;

        parseLimitClause(ctx.limitClause(), select);
        parseOffsetClause(ctx.offsetClause(), select);
    }

    private static void parseLimitClause(LimitClauseContext ctx, Select select)
    {
        if(ctx == null)
            return;

        select.setLimit(new BigInteger(ctx.INTEGER().getText()));
    }

    private static void parseOffsetClause(OffsetClauseContext ctx, Select select)
    {
        if(ctx == null)
            return;

        select.setOffset(new BigInteger(ctx.INTEGER().getText()));
    }
}

class PrologueVisitor extends BaseVisitor<Void>
{
    private final Prologue prologue = new Prologue();

    public Prologue getPrologue()
    {
        return prologue;
    }

    @Override
    public Void visitDefineDecl(DefineDeclContext ctx)
    {
        PrefixedName key = IriVisitor.parsePrefixedName(ctx.prefixedName());

        List<Define.DefineValue> defineValues = mapList(ctx.defineValue(), this::parseDefineValue);

        Define result = withRange(new Define(key, defineValues), ctx);

        prologue.getDefines().add(result);

        return null;
    }

    private Define.DefineValue parseDefineValue(DefineValueContext ctx)
    {
        Define.DefineValue value;

        if(ctx.iri() != null)
        {
            IriContext iriCtx = ctx.iri();

            // can't use IriVisitor.visit for both, because prefixed name has to
            // use PrefixedName here, not IRI
            if(iriCtx.IRIREF() != null)
                value = new IriVisitor().visit(iriCtx);
            else
                value = IriVisitor.parsePrefixedName(iriCtx.prefixedName());
        }
        else if(ctx.string() != null)
            value = new Define.StringValue(ctx.string().getText());
        else
            value = new Define.IntegerValue(ctx.INTEGER().getText());

        return withRange(value, ctx);
    }

    @Override
    public Void visitBaseDecl(BaseDeclContext ctx)
    {
        String uri = ctx.IRIREF().getText();

        prologue.setBase(new IRI(uri));

        return null;
    }

    @Override
    public Void visitPrefixDecl(PrefixDeclContext ctx)
    {
        String name = ctx.PNAME_NS().getText();
        IRI iri = new IRI(IriVisitor.parseUri(ctx.IRIREF().getText(), prologue));

        PrefixDefinition result = withRange(new PrefixDefinition(name, iri), ctx);

        /*
        java.util.Optional<PrefixDefinition> existingPrefix = prologue.getPrefixes().stream()
                .filter(p -> p.getName().equals(result.getName())).findFirst();
        
        if(existingPrefix.isPresent())
        {
            // this should probably be a warning
            prologue.getPrefixes().remove(existingPrefix.get());
        }
        */

        prologue.addPrefixDefinition(result);

        return null;
    }
}

class GraphPatternVisitor extends BaseVisitor<GraphPattern>
{
    @Override
    public GraphPattern visitGroupGraphPattern(GroupGraphPatternContext ctx)
    {
        // brackets are added to the range of group graph pattern, but not of
        // sub select

        if(ctx.groupGraphPatternSub() == null)
            return visit(ctx.subSelect());

        GraphPattern graphPattern = visit(ctx.groupGraphPatternSub());
        graphPattern.setRange(Range.compute(ctx));
        return graphPattern;
    }

    public Stream<? extends Pattern> processPattern(Pattern pattern)
    {
        if(pattern instanceof ComplexTriple)
        {
            ComplexTriple triple = (ComplexTriple) pattern;

            TripleExpander tripleExpander = new TripleExpander();
            tripleExpander.visit(triple);
            return tripleExpander.getResults().stream();
        }

        return Stream.of(pattern);
    }

    /**
     * Quick (and dirty) fix to merge triplets representing a procedure call splitted by a remote endpoint.
     *
     * TODO: need to be addressed more generally
     */
    private Stream<Pattern> repairExpandedProcedureCalls(Stream<Pattern> stream)
    {
        List<Pattern> patterns = stream.collect(Collectors.toList());;
        LinkedList<Pattern> removed = new LinkedList<Pattern>();


        for(Pattern pattern : patterns)
        {
            if(removed.contains(pattern))
                continue;

            if(!(pattern instanceof ComplexTriple))
                continue;

            ComplexTriple triple = (ComplexTriple) pattern;

            List<Property> properties = triple.getProperties();

            if(properties.size() != 1)
                continue;

            Property property = properties.get(0);
            Verb verb = property.getVerb();

            if(!(verb instanceof IRI))
                continue;

            ProcedureDefinition callDefinition = QueryVisitor.getProcedures().get(((IRI) verb).getUri().toString());

            if(callDefinition == null)
                continue;

            List<ComplexNode> objects = property.getObjects();

            if(objects.size() != 1)
                continue;

            ComplexNode object = objects.get(0);

            if(!(object instanceof Variable) && !(object instanceof BlankNode))
                continue;


            /* procedure call object needs to be fixed */
            BlankNodePropertyList objectBlanknode = new BlankNodePropertyList();
            objectBlanknode.setRange(object.getRange());
            property.getObjects().set(0, objectBlanknode);

            for(Pattern subPattern : patterns)
            {
                if(subPattern == pattern)
                    continue;

                if(!(subPattern instanceof ComplexTriple))
                    continue;

                ComplexTriple subTriple = (ComplexTriple) subPattern;

                ComplexNode subSubject = subTriple.getNode();

                if(subSubject.equals(object))
                {
                    objectBlanknode.getProperties().addAll(subTriple.getProperties());
                    removed.add(subPattern);
                }
            }


            if(callDefinition.isSimple())
                continue;

            ComplexNode subject = triple.getNode();

            if(!(subject instanceof Variable) && !(subject instanceof BlankNode))
                continue;


            /* procedure call subject needs to be fixed */
            BlankNodePropertyList subjectBlanknode = new BlankNodePropertyList();
            subjectBlanknode.setRange(subject.getRange());
            triple.setNode(subjectBlanknode);

            for(Pattern subPattern : patterns)
            {
                if(subPattern == pattern)
                    continue;

                if(!(subPattern instanceof ComplexTriple))
                    continue;

                ComplexTriple subTriple = (ComplexTriple) subPattern;

                ComplexNode subSubject = subTriple.getNode();

                if(subSubject.equals(subject))
                {
                    subjectBlanknode.getProperties().addAll(subTriple.getProperties());
                    removed.add(subPattern);
                }
            }
        }

        patterns.removeAll(removed);

        return patterns.stream();
    }

    @Override
    public GraphPattern visitGroupGraphPatternSub(GroupGraphPatternSubContext ctx)
    {
        List<Pattern> patterns = repairExpandedProcedureCalls(new GroupGraphPatternVisitor().visit(ctx))
                .flatMap(this::processPattern).collect(Collectors.toList());

        return new GroupGraph(patterns);
    }

    @Override
    public GraphPattern visitSubSelect(SubSelectContext ctx)
    {
        Select select = QueryVisitor.parseSelect(ctx.selectClause(), null, ctx.whereClause(), ctx.solutionModifier());

        select.setValues(QueryVisitor.parseValues(ctx.valuesClause()));

        return select;
    }
}

class TripleExpander extends ComplexElementVisitor<Node>
{
    private static AtomicInteger variableId = new AtomicInteger();

    private final List<BasicPattern> results = new ArrayList<>();

    public List<BasicPattern> getResults()
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

        // NOTE: added by galgonek
        if(triple.getProperties().isEmpty())
            subject = visitElement(triple.getNode());

        for(Property property : triple.getProperties())
        {
            Verb verb = property.getVerb();
            ProcedureDefinition callDefinition = verb instanceof IRI
                    ? QueryVisitor.getProcedures().get(((IRI) verb).getUri().toString())
                    : null;

            if(callDefinition != null)
            {
                IRI procedureName = (IRI) verb;

                for(ComplexNode objectNode : property.getObjects())
                {
                    if(!(objectNode instanceof BlankNodePropertyList))
                        throw new UncheckedParseException(ErrorType.invalidProcedureCallObject, objectNode.getRange(),
                                procedureName.toString(QueryVisitor.getPrologue()));

                    BlankNodePropertyList blankNodePropertyList = (BlankNodePropertyList) objectNode;

                    List<ProcedureCall.Parameter> parameters = mapList(blankNodePropertyList.getProperties(),
                            this::parseParameter);

                    ComplexNode originalSubject = triple.getNode();

                    ProcedureCallBase result;

                    if(!callDefinition.isSimple() /* originalSubject instanceof BlankNodePropertyList */)
                    {
                        if(!(originalSubject instanceof BlankNodePropertyList))
                            throw new UncheckedParseException(ErrorType.invalidMultiProcedureCallSubject,
                                    originalSubject.getRange(), procedureName.toString(QueryVisitor.getPrologue()));

                        if(triple.getProperties().size() > 1)
                            throw new UncheckedParseException(ErrorType.invalidMultiProcedureCallPredicateCombinaion,
                                    triple.getProperties().get(1).getRange());

                        BlankNodePropertyList castedSubject = (BlankNodePropertyList) originalSubject;
                        List<ProcedureCall.Parameter> results = mapList(castedSubject.getProperties(),
                                this::parseResultParameter);

                        result = new MultiProcedureCall(results, procedureName, parameters);
                    }
                    else
                    {
                        if(!(originalSubject instanceof BlankNodePropertyList) && !(originalSubject instanceof Node))
                            throw new UncheckedParseException(ErrorType.invalidProcedureCallSubject,
                                    originalSubject.getRange(), procedureName.toString(QueryVisitor.getPrologue()));

                        if(subject == null)
                            subject = visitElement(triple.getNode());

                        result = new ProcedureCall(subject, procedureName, parameters);
                    }

                    result.setRange(triple.getRange());
                    results.add(result);
                }
            }
            else
            {
                new ElementVisitor<Void>()
                {
                    @Override
                    public Void visit(IRI iri)
                    {
                        if(QueryVisitor.getProcedures().get(iri.getUri().toString()) != null)
                            throw new UncheckedParseException(ErrorType.invalidProcedureCallPropertyPathCombinaion,
                                    iri.getRange());

                        return null;
                    }
                }.visitElement(verb);


                if(subject == null)
                    subject = visitElement(triple.getNode());

                for(ComplexNode objectNode : property.getObjects())
                {
                    Node object = visitElement(objectNode);

                    Triple resultTriple = new Triple(subject, verb, object);
                    resultTriple.setRange(triple.getRange());
                    results.add(resultTriple);
                }
            }
        }

        return null;
    }

    private ProcedureCall.Parameter parseParameter(Property property)
    {
        if(!(property.getVerb() instanceof IRI))
            throw new UncheckedParseException(ErrorType.invalidProcedureParameterValue, property.getVerb().getRange());

        IRI parameterName = (IRI) property.getVerb();

        if(property.getObjects().size() != 1)
            throw new UncheckedParseException(ErrorType.invalidProcedureParameterValueNumber, property.getRange());

        Node parameterValue = visitElement(property.getObjects().get(0));

        return new ProcedureCall.Parameter(parameterName, parameterValue);
    }

    private ProcedureCall.Parameter parseResultParameter(Property property)
    {
        if(!(property.getVerb() instanceof IRI))
            throw new UncheckedParseException(ErrorType.invalidProcedureResultValue, property.getVerb().getRange());

        IRI parameterName = (IRI) property.getVerb();

        if(property.getObjects().size() != 1)
            throw new UncheckedParseException(ErrorType.invalidProcedureResultValueNumber, property.getRange());

        Node parameterValue = visitElement(property.getObjects().get(0));

        return new ProcedureCall.Parameter(parameterName, parameterValue);
    }

    private static BlankNode getNewBlankNode()
    {
        int id = variableId.incrementAndGet();
        return new BlankNode(BlankNode.prefix + id);
    }

    @Override
    public Node visit(BlankNodePropertyList blankNodePropertyList)
    {
        BlankNode blankNode = getNewBlankNode();
        blankNode.setNodeOptions(blankNodePropertyList.getNodeOptions());
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

        BlankNode firstNode = getNewBlankNode();
        firstNode.setNodeOptions(rdfCollection.getNodeOptions());
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
                nextNode = getNewBlankNode();
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
}

class GroupGraphPatternVisitor extends BaseVisitor<Stream<Pattern>>
{
    @Override
    public Stream<Pattern> visit(ParseTree tree)
    {
        try
        {
            return super.visit(tree);
        }
        catch(UncheckedParseException e)
        {
            QueryVisitor.getExceptionConsumer().accept(new ParseException(e));

            return Stream.empty();
        }
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
        PatternVisitor patternVisitor = new PatternVisitor();

        return ctx.triplesSameSubjectPath().stream().map(patternVisitor::visit).filter(Objects::nonNull);
    }

    @Override
    public Stream<Pattern> visitGroupGraphPatternSubList(GroupGraphPatternSubListContext ctx)
    {
        PatternVisitor patternVisitor = new PatternVisitor();

        Pattern graphPattern = patternVisitor.visit(ctx.graphPatternNotTriples());

        Stream<Pattern> triplesPatterns = visitIfNotNull(ctx.triplesBlock());

        return Stream.concat(Stream.of(graphPattern), triplesPatterns);
    }
}

class PatternVisitor extends BaseVisitor<Pattern>
{
    GraphPatternVisitor graphPatternVisitor = new GraphPatternVisitor();
    ExpressionVisitor expressionVisitor = new ExpressionVisitor();

    @Override
    public Pattern visit(ParseTree tree)
    {
        try
        {
            return super.visit(tree);
        }
        catch(UncheckedParseException e)
        {
            QueryVisitor.getExceptionConsumer().accept(new ParseException(e));

            return null;
        }
    }

    @Override
    public ComplexTriple visitTriplesSameSubjectPath(TriplesSameSubjectPathContext ctx)
    {
        return TripleVisitor.parseTriple(ctx);
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
        return new Graph(new NodeVisitor().parseVarOrIri(ctx.varOrIRI()),
                graphPatternVisitor.visit(ctx.groupGraphPattern()));
    }

    @Override
    public Service visitServiceGraphPattern(ServiceGraphPatternContext ctx)
    {
        return new Service(new NodeVisitor().parseVarOrIri(ctx.varOrIRI()),
                graphPatternVisitor.visit(ctx.groupGraphPattern()), ctx.SILENT() != null);
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

        // withRange is called here, so that the thrown exception knows about
        // its position
        Values result = withRange(new Values(Collections.singleton(variable), valuesLists), ctx);
        result.checkCounts();
        return result;
    }

    @Override
    public Values visitInlineDataFull(InlineDataFullContext ctx)
    {
        List<Variable> variables = mapList(ctx.var(), var -> withRange(new Variable(var.getText()), var));
        List<Values.ValuesList> valuesLists = mapList(ctx.dataBlockValues(),
                values -> new Values.ValuesList(mapList(values.dataBlockValue(), this::createVal)));

        // withRange is called here, so that the thrown exception knows about
        // its position
        Values result = withRange(new Values(variables, valuesLists), ctx);
        result.checkCounts();
        return result;
    }

    Expression createVal(DataBlockValueContext value)
    {
        Expression ret = new LiteralVisitor().visit(value);

        if(ret == null)
            ret = new IriVisitor().visit(value);

        return ret;
    }
}

class IriVisitor extends BaseVisitor<IRI>
{
    @Override
    public IRI visitIri(IriContext ctx)
    {
        if(ctx.IRIREF() != null)
        {
            URI uri = parseUri(ctx.IRIREF().getText(), QueryVisitor.getPrologue());

            return new IRI(uri);
        }

        return visit(ctx.prefixedName());
    }

    public static URI parseUri(String uriString, Prologue prologue)
    {
        uriString = cz.iocb.chemweb.server.sparql.parser.model.IRI.removeBrackets(uriString);

        URI uri = java.net.URI.create(uriString);

        if(!uri.isAbsolute())
        {
            uri = java.net.URI.create(prologue.getBase().getUri().toString() + uriString);
        }

        return uri;
    }

    @Override
    public IRI visitPrefixedName(PrefixedNameContext ctx)
    {
        return new IRI(parsePrefixedName(ctx).getAbsoluteURI(QueryVisitor.getPrologue()));
    }

    public static PrefixedName parsePrefixedName(PrefixedNameContext ctx)
    {
        String[] parts = ctx.getText().split(":", 2);

        parts[1] = parts[1].replace("\\", "");

        return withRange(new PrefixedName(parts[0], parts[1]), ctx);
    }
}
