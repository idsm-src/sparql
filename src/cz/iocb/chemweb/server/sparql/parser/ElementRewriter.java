package cz.iocb.chemweb.server.sparql.parser;

import static cz.iocb.chemweb.server.sparql.parser.StreamUtils.mapList;
import java.util.Collection;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.Define;
import cz.iocb.chemweb.server.sparql.parser.model.GroupCondition;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.OrderCondition;
import cz.iocb.chemweb.server.sparql.parser.model.PrefixDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.PrefixedName;
import cz.iocb.chemweb.server.sparql.parser.model.Projection;
import cz.iocb.chemweb.server.sparql.parser.model.Prologue;
import cz.iocb.chemweb.server.sparql.parser.model.Query;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BracketedExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BuiltInCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.ExistsExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.FunctionCallExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.InExpression;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.expression.UnaryExpression;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Bind;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Filter;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Graph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GraphPattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GroupGraph;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Minus;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Optional;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Pattern;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.ProcedureCall;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Service;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Union;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Values;
import cz.iocb.chemweb.server.sparql.parser.model.triple.AlternativePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BracketedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.InversePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NegatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Path;
import cz.iocb.chemweb.server.sparql.parser.model.triple.RepeatedPath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.SequencePath;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;



/**
 * Base class for visitors that want to modify some types of elements in an AST.
 */
public class ElementRewriter extends ElementVisitor<Element>
{
    @Override
    public Element visitElement(Element element)
    {
        Element result = super.visitElement(element);

        if(result != null && result.getRange() == null)
            result.setRange(element.getRange());

        return result;
    }

    @Override
    public Query visit(SelectQuery selectQuery)
    {
        return new SelectQuery(visit(selectQuery.getPrologue()), visit(selectQuery.getSelect()));
    }

    @Override
    public Prologue visit(Prologue prologue)
    {
        if(prologue == null)
            return null;

        List<Define> defines = mapList(prologue.getDefines(), this::visit);
        List<PrefixDefinition> prefixes = mapList(prologue.getPrefixes(), this::visit);

        return new Prologue(defines, visit(prologue.getBase()), prefixes);
    }

    @Override
    public Define visit(Define define)
    {
        List<Define.DefineValue> values = mapList(define.getValues(), this::visit);

        return new Define(visit(define.getKey()), values);
    }

    private Define.DefineValue visit(Define.DefineValue value)
    {
        return (Define.DefineValue) visitElement(value);
    }

    @Override
    public Define.DefineValue visit(Define.StringValue stringValue)
    {
        return stringValue;
    }

    @Override
    public Define.DefineValue visit(Define.IntegerValue integerValue)
    {
        return integerValue;
    }

    @Override
    public PrefixDefinition visit(PrefixDefinition prefix)
    {
        return new PrefixDefinition(prefix.getName(), visit(prefix.getIri()));
    }

    @Override
    public Select visit(Select select)
    {
        List<Projection> projections = mapList(select.getProjections(), this::visit);
        List<DataSet> dataSets = mapList(select.getDataSets(), this::visit);
        GraphPattern pattern = (GraphPattern) visitElement(select.getPattern());
        List<GroupCondition> groupConditions = mapList(select.getGroupByConditions(), this::visit);
        List<Expression> havingConditions = mapList(select.getHavingConditions(), this::visit);
        List<OrderCondition> orderConditions = mapList(select.getOrderByConditions(), this::visit);
        Values values = visit(select.getValues());

        Select result = new Select();

        result.getProjections().addAll(projections);
        result.getDataSets().addAll(dataSets);
        result.setPattern(pattern);
        result.getGroupByConditions().addAll(groupConditions);
        result.getHavingConditions().addAll(havingConditions);
        result.getOrderByConditions().addAll(orderConditions);
        result.setValues(values);

        return result;
    }

    @Override
    public Projection visit(Projection projection)
    {
        return new Projection(visit(projection.getExpression()), visit(projection.getVariable()));
    }

    @Override
    public DataSet visit(DataSet dataSet)
    {
        return new DataSet(visit(dataSet.getSourceSelector()), dataSet.isDefault());
    }

    @Override
    public GroupCondition visit(GroupCondition groupCondition)
    {
        return new GroupCondition(visit(groupCondition.getExpression()), visit(groupCondition.getVariable()));
    }

    @Override
    public OrderCondition visit(OrderCondition orderCondition)
    {
        return new OrderCondition(orderCondition.getDirection(), visit(orderCondition.getExpression()));
    }

    private GraphPattern visit(GraphPattern graphPattern)
    {
        return (GraphPattern) visitElement(graphPattern);
    }

    private Pattern visit(Pattern pattern)
    {
        return (Pattern) visitElement(pattern);
    }

    @Override
    public GraphPattern visit(GroupGraph groupGraph)
    {
        return new GroupGraph(mapList(groupGraph.getPatterns(), this::visit));
    }

    @Override
    public Pattern visit(Union union)
    {
        return new Union(mapList(union.getPatterns(), this::visit));
    }

    @Override
    public Pattern visit(Optional optional)
    {
        return new Optional(visit(optional.getPattern()));
    }

    @Override
    public Pattern visit(Minus minus)
    {
        return new Minus(minus.getPattern());
    }

    @Override
    public Pattern visit(Filter filter)
    {
        return new Filter(visit(filter.getConstraint()));
    }

    @Override
    public Pattern visit(Bind bind)
    {
        return new Bind(visit(bind.getExpression()), visit(bind.getVariable()));
    }

    @Override
    public Pattern visit(Graph graph)
    {
        return new Graph(visit(graph.getName()), visit(graph.getPattern()));
    }

    @Override
    public Pattern visit(Service service)
    {
        return new Service(visit(service.getName()), visit(service.getPattern()), service.isSilent());
    }

    @Override
    public Values visit(Values values)
    {
        if(values == null)
            return null;

        List<Variable> variables = mapList(values.getVariables(), this::visit);
        List<Values.ValuesList> valuesLists = mapList(values.getValuesLists(), this::visit);

        return new Values(variables, valuesLists);
    }

    @Override
    public Values.ValuesList visit(Values.ValuesList valuesList)
    {
        return new Values.ValuesList(mapList(valuesList.getValues(), this::visit));
    }

    private List<Expression> visitExpressions(Collection<Expression> expressions)
    {
        return mapList(expressions, this::visit);
    }

    private Expression visit(Expression expression)
    {
        return (Expression) visitElement(expression);
    }

    @Override
    public Expression visit(BinaryExpression binaryExpression)
    {
        return new BinaryExpression(binaryExpression.getOperator(), visit(binaryExpression.getLeft()),
                visit(binaryExpression.getRight()));
    }

    @Override
    public Expression visit(InExpression inExpression)
    {
        return new InExpression(visit(inExpression.getLeft()), visitExpressions(inExpression.getRight()),
                inExpression.isNegated());
    }

    @Override
    public Expression visit(UnaryExpression unaryExpression)
    {
        return new UnaryExpression(unaryExpression.getOperator(), visit(unaryExpression.getOperand()));
    }

    @Override
    public Expression visit(BracketedExpression bracketedExpression)
    {
        return new BracketedExpression(visit(bracketedExpression.getChild()));
    }

    @Override
    public Expression visit(BuiltInCallExpression builtInCallExpression)
    {
        BuiltInCallExpression result = new BuiltInCallExpression(builtInCallExpression.getFunctionName(),
                visitExpressions(builtInCallExpression.getArguments()));
        result.setDistinct(builtInCallExpression.isDistinct());
        return result;
    }

    @Override
    public Expression visit(ExistsExpression existsExpression)
    {
        return new ExistsExpression(visit(existsExpression.getPattern()), existsExpression.isNegated());
    }

    @Override
    public Expression visit(FunctionCallExpression functionCallExpression)
    {
        return new FunctionCallExpression(visit(functionCallExpression.getFunction()),
                visitExpressions(functionCallExpression.getArguments()));
    }

    @Override
    public IRI visit(IRI iri)
    {
        return iri;
    }

    @Override
    public PrefixedName visit(PrefixedName prefixedName)
    {
        return prefixedName;
    }

    @Override
    public Literal visit(Literal literal)
    {
        if(literal.getTypeIri().getUri().equals(Xsd.STRING_URI))
        {
            return new Literal(literal.getStringValue(), literal.getLanguageTag());
        }
        else
        {
            return new Literal(literal.getStringValue(), visit(literal.getTypeIri()));
        }
    }

    private VarOrIri visit(VarOrIri varOrIri)
    {
        return (VarOrIri) visitElement(varOrIri);
    }

    @Override
    public Variable visit(Variable variable)
    {
        return variable;
    }

    @Override
    public Pattern visit(Triple triple)
    {
        return new Triple(visit(triple.getSubject()), visit(triple.getPredicate()), visit(triple.getObject()));
    }

    private Node visit(Node node)
    {
        return (Node) visitElement(node);
    }

    @Override
    public Node visit(BlankNode blankNode)
    {
        return new BlankNode(blankNode.getName());
    }

    private Verb visit(Verb verb)
    {
        return (Verb) visitElement(verb);
    }

    private List<Path> visitPaths(Collection<Path> paths)
    {
        return mapList(paths, this::visit);
    }

    private Path visit(Path path)
    {
        return (Path) visitElement(path);
    }

    @Override
    public Path visit(AlternativePath alternativePath)
    {
        return new AlternativePath(visitPaths(alternativePath.getChildren()));
    }

    @Override
    public Path visit(SequencePath sequencePath)
    {
        return new SequencePath(visitPaths(sequencePath.getChildren()));
    }

    @Override
    public Path visit(InversePath inversePath)
    {
        return new InversePath(visit(inversePath.getChild()));
    }

    @Override
    public Path visit(RepeatedPath repeatedPath)
    {
        return new RepeatedPath(repeatedPath.getKind(), visit(repeatedPath.getChild()));
    }

    @Override
    public Path visit(NegatedPath negatedPath)
    {
        return new NegatedPath(visit(negatedPath.getChild()));
    }

    @Override
    public Path visit(BracketedPath bracketedPath)
    {
        return new BracketedPath(visit(bracketedPath.getChild()));
    }

    @Override
    public Element visit(ProcedureCall procedureCall)
    {
        List<ProcedureCall.Parameter> parameters = mapList(procedureCall.getParameters(), this::visit);

        return new ProcedureCall(visit(procedureCall.getResult()), visit(procedureCall.getProcedure()), parameters);
    }

    @Override
    public ProcedureCall.Parameter visit(ProcedureCall.Parameter parameter)
    {
        return new ProcedureCall.Parameter(visit(parameter.getName()), visit(parameter.getValue()));
    }
}
