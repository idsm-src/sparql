package cz.iocb.sparql.engine.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.VariableNode;
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
     * Creates the call; variables among the results and parameters become in scope.
     *
     * @param results the named results
     * @param procedure IRI of the procedure
     * @param parameters the parameters
     */
    public MultiProcedureCall(Collection<Parameter> results, IriNode procedure, Collection<Parameter> parameters)
    {
        super(procedure, parameters);
        this.results = Collections.unmodifiableList(new ArrayList<>(results));

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


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
