package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import java.util.Collection;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



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
    private Node result;

    public ProcedureCall()
    {
    }

    public ProcedureCall(Node result, IRI procedure, Collection<Parameter> parameters)
    {
        super(procedure, parameters);
        this.result = result;
    }

    public Node getResult()
    {
        return result;
    }

    public void setResult(Node result)
    {
        this.result = result;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
