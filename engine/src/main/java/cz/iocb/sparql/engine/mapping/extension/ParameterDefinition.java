package cz.iocb.sparql.engine.mapping.extension;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.model.triple.Node;



/**
 * Definition of a procedure parameter: the IRI used as its predicate in the call, the class in whose columns the SQL
 * function takes the value, and an optional default value used when the call omits it. Which terms supply a value of
 * the class is decided as SPARQL decides it for its own functions, not by the deployment: a literal of the datatype of
 * a canonical literal class supplies its value in every valid lexical form, a numeric class takes the value of every
 * numeric XPath substitutes or promotes to its datatype, e.g. an xsd:integer or an xsd:decimal for an xsd:double, but
 * not an xsd:decimal for an xsd:integer (see
 * {@link cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode#getAcceptedClass}), and a term supplying no
 * value of the class gives the call no solution. A function working on the terms themselves declares a base class,
 * which keeps the lexical form, or the box.
 */
public class ParameterDefinition
{
    /**
     * IRI naming the parameter.
     */
    private final String parameterName;

    /**
     * Class in whose columns the SQL function takes the value.
     */
    private final ResourceClass parameterClass;

    /**
     * Default value, or null when the parameter is mandatory.
     */
    private final Node defaultValue;


    /**
     * Creates the definition.
     *
     * @param parameterName IRI naming the parameter
     * @param parameterClass class in whose columns the SQL function takes the value
     * @param defaultValue default value, or null when mandatory
     */
    public ParameterDefinition(String parameterName, ResourceClass parameterClass, Node defaultValue)
    {
        this.parameterName = parameterName;
        this.parameterClass = parameterClass;
        this.defaultValue = defaultValue;
    }


    /**
     * IRI naming the parameter.
     *
     * @return IRI naming the parameter
     */
    public final String getParamName()
    {
        return this.parameterName;
    }


    /**
     * Class in whose columns the SQL function takes the value.
     *
     * @return class in whose columns the SQL function takes the value
     */
    public final ResourceClass getParameterClass()
    {
        return parameterClass;
    }


    /**
     * Default value, or null when the parameter is mandatory.
     *
     * @return default value, or null when the parameter is mandatory
     */
    public final Node getDefaultValue()
    {
        return defaultValue;
    }
}
