package cz.iocb.sparql.engine.model.expression;

import java.util.Collection;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * <p>
 * Represents a built-in function ({@link #getFunctionName}), called with a (possibly empty) list of arguments
 * ({@link #getArguments}).
 *
 * <p>
 * Corresponds to rule [121] BuiltInCall, except for the {@code EXISTS} and {@code NOT EXISTS} cases.
 *
 * <p>
 * Note: {@code COUNT} with empty arguments represents {@code COUNT(*)}; second parameter of {@code GROUP_CONCAT}
 * represents {@code SEPARATOR} (if present).
 */
public class BuiltInCallExpression extends CallExpression
{
    /**
     * Name of the built-in function as written in the query.
     */
    private String functionName;


    /**
     * Creates a call without arguments.
     *
     * @param functionName name of the built-in function
     */
    public BuiltInCallExpression(String functionName)
    {
        setFunctionName(functionName);
    }


    /**
     * Creates a call with the given arguments.
     *
     * @param functionName name of the built-in function
     * @param arguments the arguments
     */
    public BuiltInCallExpression(String functionName, Collection<Expression> arguments)
    {
        super(arguments);
        setFunctionName(functionName);
    }


    /**
     * Name of the built-in function as written in the query (case preserved).
     *
     * @return name of the built-in function as written in the query (case preserved)
     */
    public String getFunctionName()
    {
        return functionName;
    }


    /**
     * Sets the name of the built-in function.
     *
     * @param functionName name of the built-in function
     */
    public void setFunctionName(String functionName)
    {
        this.functionName = functionName;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    /**
     * True if the function is one of the SPARQL 1.1 aggregates.
     *
     * @return true if the function is one of the SPARQL 1.1 aggregates, false otherwise
     */
    public boolean isAggregateFunction()
    {
        switch(functionName.toLowerCase())
        {
            // aggregate functions according to SPARQL 1.1
            case "count":
            case "sum":
            case "min":
            case "max":
            case "avg":
            case "group_concat":
            case "sample":
                return true;
        }

        return false;
    }
}
