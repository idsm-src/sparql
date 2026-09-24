package cz.iocb.sparql.engine.model.pattern;

import java.util.Collection;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Custom pattern used to represent call to a procedure with single return value.
 *
 * <p>
 * It is written as a triple where the subject represents the result of the procedure ({@link #getResult}), the
 * predicate is the name of the procedure ({@link #getProcedure}), which is one of the procedures defined by the
 * configuration ({@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#getProcedures}), and the object is a
 * blank node property list containing the parameters of the procedure ({@link #getParameters}).
 */
public class ProcedureCall extends ProcedureCallBase
{
    /**
     * Node receiving the single result.
     */
    private final Node result;


    /**
     * Creates the call; a variable result and variables among the parameters become in scope.
     *
     * @param result node receiving the result
     * @param procedure IRI of the procedure
     * @param parameters the parameters
     */
    public ProcedureCall(Node result, IriNode procedure, Collection<Parameter> parameters)
    {
        super(procedure, parameters);
        this.result = result;

        if(result instanceof VariableNode variable)
            variablesInScope.add(variable);

        for(Parameter parameter : parameters)
            if(parameter.getValue() instanceof VariableNode variable)
                variablesInScope.add(variable);
    }


    /**
     * Node receiving the single result.
     *
     * @return node receiving the single result
     */
    public Node getResult()
    {
        return result;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
