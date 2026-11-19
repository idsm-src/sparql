package cz.iocb.sparql.engine.mapping.extension;

import java.util.List;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Definition of a SPARQL extension function implemented by an SQL function: the function IRI, the result and argument
 * classes, how many leading arguments are mandatory, whether the result can be NULL for non-NULL arguments, and whether
 * the function is deterministic. An argument class is the class in whose columns the SQL function takes the value of
 * the argument; which terms supply a value of it is decided as for a procedure parameter (see
 * {@link ParameterDefinition}), the string of a string literal of any kind being the value of the class of the string
 * literals. An argument supplying no value of its class is passed as NULL, so the SQL function has to be STRICT for
 * such an argument to make the call an error, as it has to; the call is not wrapped in a test of the arguments, which
 * would keep the database from using an index of an expression of the function. Every argument class and the result
 * class have a single column: the SQL function returns one value, and a value spread over several columns would make
 * the SQL of a computed argument be written, and evaluated, once for every column. A class of a single column takes the
 * place of each such class: a canonical class takes the value of a literal in every lexical form (see
 * {@link ParameterDefinition}), the scalar date and date-time classes keep the timezone offset along with the value,
 * the class of the string literals takes the string of a language-tagged literal, and the box takes any term whole.
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
     * Classes in whose columns the SQL function takes the values of the arguments, in order.
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
     * @param result class of the result, which has to have a single column, the value the SQL function returns
     * @param arguments classes of the arguments, each of a single column
     * @param requiredArgumentCount number of mandatory leading arguments
     * @param canBeNull whether the value may be null
     * @param isDeterministic whether the node is deterministic
     * @throws IllegalArgumentException if the result class or an argument class has more than one column
     */
    public FunctionDefinition(String functionName, Function sqlFunction, ResourceClass result,
            List<ResourceClass> arguments, int requiredArgumentCount, boolean canBeNull, boolean isDeterministic)
    {
        if(result.getColumnCount() != 1)
            throw new IllegalArgumentException(
                    "the result class of function " + functionName + " has to have a single column");

        for(ResourceClass argument : arguments)
            if(argument.getColumnCount() != 1)
                throw new IllegalArgumentException("the argument class " + argument.getResourceName() + " of function "
                        + functionName + " has more than one column; use a class of a single column instead: the "
                        + "canonical class of the datatype, the scalar date or date-time class, the class of the string "
                        + "literals or the box");

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
     * @param result class of the result, which has to have a single column, the value the SQL function returns
     * @param arguments classes of the arguments, each of a single column
     * @param canBeNull whether the value may be null
     * @param isDeterministic whether the node is deterministic
     * @throws IllegalArgumentException if the result class or an argument class has more than one column
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
     * Classes in whose columns the SQL function takes the values of the arguments, in order.
     *
     * @return classes in whose columns the SQL function takes the values of the arguments, in order
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
