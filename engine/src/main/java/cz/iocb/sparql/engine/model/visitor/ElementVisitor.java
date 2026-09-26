package cz.iocb.sparql.engine.model.visitor;

import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.List;
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
import cz.iocb.sparql.engine.model.Select;
import cz.iocb.sparql.engine.model.SelectQuery;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.base.Element;
import cz.iocb.sparql.engine.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.model.expression.BracketedExpression;
import cz.iocb.sparql.engine.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.model.expression.ExistsExpression;
import cz.iocb.sparql.engine.model.expression.FunctionCallExpression;
import cz.iocb.sparql.engine.model.expression.InExpression;
import cz.iocb.sparql.engine.model.expression.LiteralNode;
import cz.iocb.sparql.engine.model.expression.UnaryExpression;
import cz.iocb.sparql.engine.model.pattern.Bind;
import cz.iocb.sparql.engine.model.pattern.Filter;
import cz.iocb.sparql.engine.model.pattern.Graph;
import cz.iocb.sparql.engine.model.pattern.GroupGraph;
import cz.iocb.sparql.engine.model.pattern.Minus;
import cz.iocb.sparql.engine.model.pattern.MultiProcedureCall;
import cz.iocb.sparql.engine.model.pattern.Optional;
import cz.iocb.sparql.engine.model.pattern.ProcedureCall;
import cz.iocb.sparql.engine.model.pattern.Service;
import cz.iocb.sparql.engine.model.pattern.Union;
import cz.iocb.sparql.engine.model.pattern.Values;
import cz.iocb.sparql.engine.model.triple.AlternativePath;
import cz.iocb.sparql.engine.model.triple.BlankNode;
import cz.iocb.sparql.engine.model.triple.BracketedPath;
import cz.iocb.sparql.engine.model.triple.InversePath;
import cz.iocb.sparql.engine.model.triple.NegatedPath;
import cz.iocb.sparql.engine.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.model.triple.SequencePath;
import cz.iocb.sparql.engine.model.triple.Triple;
import cz.iocb.sparql.engine.model.triple.TripleTermNode;



/**
 * Abstract visitor for {@link Element}s.
 *
 * <p>
 * Derived classes should override the methods that correspond to interesting types of elements.
 *
 * @param <T> The result of visiting the AST.
 */
public abstract class ElementVisitor<T>
{
    /**
     * Creates the visitor.
     */
    protected ElementVisitor()
    {
    }


    /**
     * Returns the value used for elements with no children that don't have overridden visit method.
     *
     * <p>
     * The default implementation returns null.
     *
     * @return the value used for elements with no children that don't have overridden visit method.
     *         <p>
     *         The default implementation returns null
     */
    protected T defaultResult()
    {
        return null;
    }


    /**
     * Combines the results of several children, see {@link #aggregateResult(List)}.
     *
     * @param results results of the children
     * @return the combined result
     */
    @SafeVarargs
    protected final T aggregateResult(T... results)
    {
        return aggregateResult(asList(results));
    }


    /**
     * Used to combine values from multiple children of an element that doesn't have overridden visit method.
     *
     * <p>
     * The default implementation is to return the last non-null item, or null.
     *
     * @param results results of the children
     * @return the combined result
     */
    protected T aggregateResult(List<T> results)
    {
        return results.stream().filter(x -> x != null).reduce((_, current) -> current).orElse(null);
    }


    /**
     * Visits a single element; returns null for a null element.
     *
     * @param element the element to visit
     * @return the result of the visit, or null for a null element
     */
    public T visitElement(Element element)
    {
        if(element == null)
            return null;

        return element.accept(this);
    }


    /**
     * Visits all elements and aggregates their results.
     *
     * @param elements the elements to visit
     * @return the aggregated result
     */
    public T visitElements(Collection<? extends Element> elements)
    {
        return aggregateResult(elements.stream().map(this::visitElement).toList());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param selectQuery the visited element
     * @return the result of the visit
     */
    public T visit(SelectQuery selectQuery)
    {
        return aggregateResult(visitElement(selectQuery.getPrologue()), visitElement(selectQuery.getSelect()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param askQuery the visited element
     * @return the result of the visit
     */
    public T visit(AskQuery askQuery)
    {
        return aggregateResult(visitElement(askQuery.getPrologue()), visitElement(askQuery.getSelect()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param describeQuery the visited element
     * @return the result of the visit
     */
    public T visit(DescribeQuery describeQuery)
    {
        return aggregateResult(visitElement(describeQuery.getPrologue()), visitElements(describeQuery.getResources()),
                visitElement(describeQuery.getSelect()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param constructQuery the visited element
     * @return the result of the visit
     */
    public T visit(ConstructQuery constructQuery)
    {
        return aggregateResult(visitElement(constructQuery.getPrologue()), visitElements(constructQuery.getTemplates()),
                visitElement(constructQuery.getSelect()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param prologue the visited element
     * @return the result of the visit
     */
    public T visit(Prologue prologue)
    {
        return aggregateResult(visitElements(prologue.getPrefixeDefinitions()));
    }


    /**
     * Visits a leaf element; returns the default result unless overridden.
     *
     * @param prefixedName the visited element
     * @return the result of the visit
     */
    public T visit(PrefixedName prefixedName)
    {
        return defaultResult();
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param prefix the visited element
     * @return the result of the visit
     */
    public T visit(PrefixDefinition prefix)
    {
        return visitElement(prefix.getIri());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param select the visited element
     * @return the result of the visit
     */
    public T visit(Select select)
    {
        return aggregateResult(visitElements(select.getProjections()), visitElements(select.getDataSets()),
                visitElement(select.getPattern()), visitElements(select.getGroupByConditions()),
                visitElements(select.getHavingConditions()), visitElements(select.getOrderByConditions()),
                visitElement(select.getValues()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param projection the visited element
     * @return the result of the visit
     */
    public T visit(Projection projection)
    {
        return aggregateResult(visitElement(projection.getExpression()), visitElement(projection.getVariable()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param dataSet the visited element
     * @return the result of the visit
     */
    public T visit(DataSet dataSet)
    {
        return visitElement(dataSet.getSourceSelector());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param groupCondition the visited element
     * @return the result of the visit
     */
    public T visit(GroupCondition groupCondition)
    {
        return aggregateResult(visitElement(groupCondition.getExpression()),
                visitElement(groupCondition.getVariable()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param orderCondition the visited element
     * @return the result of the visit
     */
    public T visit(OrderCondition orderCondition)
    {
        return visitElement(orderCondition.getExpression());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param groupGraph the visited element
     * @return the result of the visit
     */
    public T visit(GroupGraph groupGraph)
    {
        return visitElements(groupGraph.getPatterns());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param union the visited element
     * @return the result of the visit
     */
    public T visit(Union union)
    {
        return visitElements(union.getPatterns());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param optional the visited element
     * @return the result of the visit
     */
    public T visit(Optional optional)
    {
        return visitElement(optional.getPattern());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param minus the visited element
     * @return the result of the visit
     */
    public T visit(Minus minus)
    {
        return visitElement(minus.getPattern());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param filter the visited element
     * @return the result of the visit
     */
    public T visit(Filter filter)
    {
        return visitElement(filter.getConstraint());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param bind the visited element
     * @return the result of the visit
     */
    public T visit(Bind bind)
    {
        return aggregateResult(visitElement(bind.getExpression()), visitElement(bind.getVariable()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param graph the visited element
     * @return the result of the visit
     */
    public T visit(Graph graph)
    {
        return aggregateResult(visitElement(graph.getName()), visitElement(graph.getPattern()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param service the visited element
     * @return the result of the visit
     */
    public T visit(Service service)
    {
        return aggregateResult(visitElement(service.getName()), visitElement(service.getPattern()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param values the visited element
     * @return the result of the visit
     */
    public T visit(Values values)
    {
        return aggregateResult(visitElements(values.getVariables()), visitElements(values.getValuesLists()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param valuesList the visited element
     * @return the result of the visit
     */
    public T visit(Values.ValuesList valuesList)
    {
        return visitElements(valuesList.getValues());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param binaryExpression the visited element
     * @return the result of the visit
     */
    public T visit(BinaryExpression binaryExpression)
    {
        return aggregateResult(visitElement(binaryExpression.getLeft()), visitElement(binaryExpression.getRight()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param inExpression the visited element
     * @return the result of the visit
     */
    public T visit(InExpression inExpression)
    {
        return aggregateResult(visitElement(inExpression.getLeft()), visitElements(inExpression.getRight()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param unaryExpression the visited element
     * @return the result of the visit
     */
    public T visit(UnaryExpression unaryExpression)
    {
        return visitElement(unaryExpression.getOperand());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param bracketedExpression the visited element
     * @return the result of the visit
     */
    public T visit(BracketedExpression bracketedExpression)
    {
        return visitElement(bracketedExpression.getChild());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param builtInCallExpression the visited element
     * @return the result of the visit
     */
    public T visit(BuiltInCallExpression builtInCallExpression)
    {
        return visitElements(builtInCallExpression.getArguments());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param existsExpression the visited element
     * @return the result of the visit
     */
    public T visit(ExistsExpression existsExpression)
    {
        return visitElement(existsExpression.getPattern());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param functionCallExpression the visited element
     * @return the result of the visit
     */
    public T visit(FunctionCallExpression functionCallExpression)
    {
        return aggregateResult(visitElement(functionCallExpression.getFunction()),
                visitElements(functionCallExpression.getArguments()));
    }


    /**
     * Visits a leaf element; returns the default result unless overridden.
     *
     * @param iri the visited element
     * @return the result of the visit
     */
    public T visit(IriNode iri)
    {
        return defaultResult();
    }


    /**
     * Visits a leaf element; returns the default result unless overridden.
     *
     * @param node the visited element
     * @return the result of the visit
     */
    public T visit(LiteralNode node)
    {
        return defaultResult();
    }


    /**
     * Visits a leaf element; returns the default result unless overridden.
     *
     * @param variable the visited element
     * @return the result of the visit
     */
    public T visit(VariableNode variable)
    {
        return defaultResult();
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param triple the visited element
     * @return the result of the visit
     */
    public T visit(Triple triple)
    {
        return aggregateResult(visitElement(triple.getSubject()), visitElement(triple.getPredicate()),
                visitElement(triple.getObject()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param tripleTerm the visited element
     * @return the result of the visit
     */
    public T visit(TripleTermNode tripleTerm)
    {
        return aggregateResult(visitElement(tripleTerm.getSubject()), visitElement(tripleTerm.getPredicate()),
                visitElement(tripleTerm.getObject()));
    }


    /**
     * Visits a leaf element; returns the default result unless overridden.
     *
     * @param blankNode the visited element
     * @return the result of the visit
     */
    public T visit(BlankNode blankNode)
    {
        return defaultResult();
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param alternativePath the visited element
     * @return the result of the visit
     */
    public T visit(AlternativePath alternativePath)
    {
        return visitElements(alternativePath.getChildren());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param sequencePath the visited element
     * @return the result of the visit
     */
    public T visit(SequencePath sequencePath)
    {
        return visitElements(sequencePath.getChildren());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param inversePath the visited element
     * @return the result of the visit
     */
    public T visit(InversePath inversePath)
    {
        return visitElement(inversePath.getChild());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param repeatedPath the visited element
     * @return the result of the visit
     */
    public T visit(RepeatedPath repeatedPath)
    {
        return visitElement(repeatedPath.getChild());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param negatedPath the visited element
     * @return the result of the visit
     */
    public T visit(NegatedPath negatedPath)
    {
        return visitElement(negatedPath.getChild());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param bracketedPath the visited element
     * @return the result of the visit
     */
    public T visit(BracketedPath bracketedPath)
    {
        return visitElement(bracketedPath.getChild());
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param procedureCall the visited element
     * @return the result of the visit
     */
    public T visit(ProcedureCall procedureCall)
    {
        return aggregateResult(visitElement(procedureCall.getResult()), visitElement(procedureCall.getProcedure()),
                visitElements(procedureCall.getParameters()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param multiProcedureCall the visited element
     * @return the result of the visit
     */
    public T visit(MultiProcedureCall multiProcedureCall)
    {
        return aggregateResult(visitElements(multiProcedureCall.getResults()),
                visitElement(multiProcedureCall.getProcedure()), visitElements(multiProcedureCall.getParameters()));
    }


    /**
     * Visits the element by visiting its children and aggregating their results.
     *
     * @param parameter the visited element
     * @return the result of the visit
     */
    public T visit(ProcedureCall.Parameter parameter)
    {
        return aggregateResult(visitElement(parameter.getName()), visitElement(parameter.getValue()));
    }
}
