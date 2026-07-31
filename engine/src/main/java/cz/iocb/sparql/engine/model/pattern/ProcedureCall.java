package cz.iocb.sparql.engine.model.pattern;

import java.util.Collection;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.triple.Node;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Custom pattern used to represent call to a procedure with single return value.
 *
 * <p>
 * It is written as a triple where the subject represents the result of the procedure ({@link #getResult}), the
 * predicate is the name of the procedure ( {@link #getProcedure}) (which is one of the predefined names, see
 * {@link Parser#getProcedures}) and the object is a blank node property list containing the parameters of the procedure
 * ({@link #getParameters}).
 */
public class ProcedureCall extends ProcedureCallBase
{
    private final Node result;


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
