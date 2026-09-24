package cz.iocb.sparql.engine.mapping.extension;

import java.util.List;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Definition of a SPARQL extension function implemented by an SQL function: the function IRI, the result and argument
 * classes, how many leading arguments are mandatory, whether the result can be NULL for non-NULL arguments, and whether
 * the function is deterministic.
 */
public class FunctionDefinition
{
    /**
     * IRI of the SPARQL function.
     */
    private final String functionName;

    /**
     * Implementing SQL function.
     */
    private final Function sqlFunction;

    /**
     * Class of the result.
     */
    private final ResourceClass result;

    /**
     * Classes of the arguments in order.
     */
    private final List<ResourceClass> arguments;

    /**
     * Number of mandatory leading arguments.
     */
    private final int requiredArgumentCount;

    /**
     * Whether the SQL function may return NULL for non-NULL arguments.
     */
    private final boolean canBeNull;

    /**
     * Whether the SQL function is deterministic.
     */
    private final boolean isDeterministic;


    /**
     * Creates the definition with optional trailing arguments.
     *
     * @param functionName IRI of the SPARQL function
     * @param sqlFunction the implementing SQL function
     * @param result class of the result
     * @param arguments classes of the arguments
     * @param requiredArgumentCount number of mandatory leading arguments
     * @param canBeNull whether the value may be null
     * @param isDeterministic whether the node is deterministic
     */
    public FunctionDefinition(String functionName, Function sqlFunction, ResourceClass result,
            List<ResourceClass> arguments, int requiredArgumentCount, boolean canBeNull, boolean isDeterministic)
    {
        this.functionName = functionName;
        this.sqlFunction = sqlFunction;
        this.result = result;
        this.arguments = arguments;
        this.requiredArgumentCount = requiredArgumentCount;
        this.canBeNull = canBeNull;
        this.isDeterministic = isDeterministic;
    }


    /**
     * Creates the definition with all arguments mandatory.
     *
     * @param functionName IRI of the SPARQL function
     * @param sqlFunction the implementing SQL function
     * @param result class of the result
     * @param arguments classes of the arguments
     * @param canBeNull whether the value may be null
     * @param isDeterministic whether the node is deterministic
     */
    public FunctionDefinition(String functionName, Function sqlFunction, ResourceClass result,
            List<ResourceClass> arguments, boolean canBeNull, boolean isDeterministic)
    {
        this(functionName, sqlFunction, result, arguments, arguments.size(), canBeNull, isDeterministic);
    }


    /**
     * IRI of the SPARQL function.
     *
     * @return IRI of the SPARQL function
     */
    public String getFunctionName()
    {
        return functionName;
    }


    /**
     * Implementing SQL function.
     *
     * @return implementing SQL function
     */
    public Function getSqlFunction()
    {
        return sqlFunction;
    }


    /**
     * Class of the result.
     *
     * @return class of the result
     */
    public ResourceClass getResultClass()
    {
        return result;
    }


    /**
     * Classes of the arguments in order.
     *
     * @return classes of the arguments in order
     */
    public List<ResourceClass> getArgumentClasses()
    {
        return arguments;
    }


    /**
     * True if the SQL function may return NULL even for non-NULL arguments.
     *
     * @return true if the SQL function may return NULL even for non-NULL arguments, false otherwise
     */
    public boolean canBeNull()
    {
        return canBeNull;
    }


    /**
     * True if the SQL function is deterministic, allowing the engine to reorder or deduplicate around it.
     *
     * @return true if the SQL function is deterministic, allowing the engine to reorder or deduplicate around it, false
     *         otherwise
     */
    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    /**
     * Number of leading arguments that must be supplied; the remaining ones are optional.
     *
     * @return number of leading arguments that must be supplied; the remaining ones are optional
     */
    public int getRequiredArgumentCount()
    {
        return requiredArgumentCount;
    }
}
