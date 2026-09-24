package cz.iocb.sparql.engine.mapping.extension;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.model.triple.Node;



/**
 * Definition of a procedure parameter: the IRI used as its predicate in the call, its resource class and an optional
 * default value used when the call omits it.
 */
public class ParameterDefinition
{
    /**
     * IRI naming the parameter.
     */
    private final String parameterName;

    /**
     * Class of the parameter value.
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
     * @param parameterClass class of the parameter value
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
     * Class of the parameter value.
     *
     * @return class of the parameter value
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
