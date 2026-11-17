package cz.iocb.sparql.engine.mapping.extension;

import java.util.Collection;
import java.util.LinkedHashMap;
import cz.iocb.sparql.engine.database.Function;



/**
 * Definition of a procedure, a set-returning SQL function callable from a graph pattern (see
 * {@link cz.iocb.sparql.engine.model.pattern.ProcedureCall}): the procedure IRI, the SQL function, and its named
 * parameters and results in declaration order. A specialiser may adapt the calls to the classes the query can use the
 * results in (see {@link #setSpecializer}). A procedure is deterministic unless declared otherwise (see
 * {@link #setDeterministic}).
 */
public class ProcedureDefinition
{
    /**
     * IRI of the procedure.
     */
    private final String procedureName;

    /**
     * Implementing set-returning SQL function.
     */
    private final Function sqlProcedure;

    /**
     * Parameters by IRI, in declaration order.
     */
    private final LinkedHashMap<String, ParameterDefinition> parameters;

    /**
     * Results by IRI (null for the single unnamed result), in declaration order.
     */
    private final LinkedHashMap<String, ResultDefinition> results;

    /**
     * Specialiser of the calls to the classes the query can use the results in (see {@link #setSpecializer}), or null.
     */
    private ProcedureSpecializer specializer = null;

    /**
     * Whether the procedure gives the same results whenever it is called with the same arguments (see
     * {@link #setDeterministic}).
     */
    private boolean deterministic = true;


    /**
     * Creates the definition without parameters and results.
     *
     * @param procedureName IRI of the procedure
     * @param sqlProcedure the implementing SQL function
     */
    public ProcedureDefinition(String procedureName, Function sqlProcedure)
    {
        this.procedureName = procedureName;
        this.sqlProcedure = sqlProcedure;
        this.parameters = new LinkedHashMap<>();
        this.results = new LinkedHashMap<>();
    }


    /**
     * Adds a parameter.
     *
     * @param parameter the parameter
     */
    public void addParameter(ParameterDefinition parameter)
    {
        parameters.put(parameter.getParamName(), parameter);
    }


    /**
     * Adds a result.
     *
     * @param result the result definition
     */
    public void addResult(ResultDefinition result)
    {
        results.put(result.getResultName(), result);
    }


    /**
     * Parameter of the given IRI, or null.
     *
     * @param parameterName IRI naming the parameter
     * @return parameter of the given IRI, or null
     */
    public ParameterDefinition getParameter(String parameterName)
    {
        return parameters.get(parameterName);
    }


    /**
     * Sets the specialiser of the calls: given, for every result a call delivers, the classes of the result the query
     * can use a value of, it may have the call use another SQL function or other arguments, and tells the classes the
     * function then binds the results in (see {@link ProcedureSpecialization}). The engine binds a result to the
     * classes the query can use in any case, so a function need not be told; a specialiser lets it skip the work for
     * the other classes.
     *
     * @param specializer the specialiser, or null to call the procedure as declared
     */
    public void setSpecializer(ProcedureSpecializer specializer)
    {
        this.specializer = specializer;
    }


    /**
     * Specialiser of the calls (see {@link #setSpecializer}), or null.
     *
     * @return specialiser of the calls, or null
     */
    public ProcedureSpecializer getSpecializer()
    {
        return specializer;
    }


    /**
     * Declares whether the procedure gives the same results whenever it is called with the same arguments, which it
     * does by default. The engine evaluates a call of a procedure that does not once for every solution it is called
     * for, a call taking no values of a context once at every place of the query it occurs at, and neither shares nor
     * deduplicates its calls, whereas it may evaluate the calls of a deterministic procedure once for equal arguments.
     * The declaration is not read from the volatility of the SQL function, as a function of a procedural language is
     * volatile unless declared otherwise.
     *
     * @param deterministic whether the procedure gives the same results whenever it is called with the same arguments
     */
    public void setDeterministic(boolean deterministic)
    {
        this.deterministic = deterministic;
    }


    /**
     * True if the procedure gives the same results whenever it is called with the same arguments (see
     * {@link #setDeterministic}).
     *
     * @return true if the procedure gives the same results whenever it is called with the same arguments, false
     *         otherwise
     */
    public boolean isDeterministic()
    {
        return deterministic;
    }


    /**
     * Result of the given IRI (null for the unnamed result), or null if there is none.
     *
     * @param resultName IRI naming the result
     * @return result of the given IRI (null for the unnamed result), or null if there is none
     */
    public ResultDefinition getResult(String resultName)
    {
        return results.get(resultName);
    }


    /**
     * Parameters in declaration order.
     *
     * @return parameters in declaration order
     */
    public Collection<ParameterDefinition> getParameters()
    {
        return parameters.values();
    }


    /**
     * Results in declaration order.
     *
     * @return results in declaration order
     */
    public Collection<ResultDefinition> getResults()
    {
        return results.values();
    }


    /**
     * True if the procedure has a single unnamed result, so it is called with a plain result node instead of a result
     * blank node pattern.
     *
     * @return true if the procedure has a single unnamed result, so it is called with a plain result node instead of a
     *         result blank node pattern, false otherwise
     */
    public boolean isSimple()
    {
        return results.size() == 1 && results.get(null) != null;
    }


    /**
     * IRI of the procedure.
     *
     * @return IRI of the procedure
     */
    public final String getProcedureName()
    {
        return procedureName;
    }


    /**
     * Implementing set-returning SQL function.
     *
     * @return implementing set-returning SQL function
     */
    public final Function getSqlProcedure()
    {
        return sqlProcedure;
    }
}
