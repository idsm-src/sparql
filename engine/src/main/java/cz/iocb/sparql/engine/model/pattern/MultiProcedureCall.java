package cz.iocb.sparql.engine.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Custom pattern used to represent call to a procedure with multiple return values.
 *
 * <p>
 * It is written as a triple where the subject represents the results of the procedure ({@link #getResults}), the
 * predicate is the name of the procedure ({@link #getProcedure}), which is one of the procedures defined by the
 * configuration ({@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#getProcedures}), and the object is a
 * blank node property list containing the parameters of the procedure ({@link #getParameters}).
 */
public class MultiProcedureCall extends ProcedureCallBase
{
    /**
     * Named results of the procedure.
     */
    private final List<Parameter> results;

    /**
     * Node standing for the results in the text of the call.
     */
    private final Node resultNode;


    /**
     * Creates the call; variables among the results and parameters become in scope.
     *
     * @param results the named results
     * @param procedure IRI of the procedure
     * @param parameters the parameters
     * @param resultNode node standing for the results in the text of the call, or null
     * @param parameterNode node standing for the parameters in the text of the call, or null
     */
    public MultiProcedureCall(Collection<Parameter> results, IriNode procedure, Collection<Parameter> parameters,
            Node resultNode, Node parameterNode)
    {
        super(procedure, parameters, parameterNode);
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.resultNode = resultNode;

        for(Parameter result : results)
            if(result.getValue() instanceof VariableNode variable)
                variablesInScope.add(variable);

        for(Parameter parameter : parameters)
            if(parameter.getValue() instanceof VariableNode variable)
                variablesInScope.add(variable);
    }


    /**
     * Named results of the procedure.
     *
     * @return named results of the procedure
     */
    public List<Parameter> getResults()
    {
        return results;
    }


    /**
     * Node standing for the results in the text of the call: the subject of the triple naming the procedure, a blank
     * node or a variable that is the subject of the triples of the results. It is syntax only, so the call binds no
     * such variable, and no visitor visits the node as a child of the call.
     *
     * @return node standing for the results in the text of the call, or null if the call has none
     */
    public Node getResultNode()
    {
        return resultNode;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
