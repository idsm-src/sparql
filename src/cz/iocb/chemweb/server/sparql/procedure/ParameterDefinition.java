package cz.iocb.chemweb.server.sparql.procedure;

import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ParameterDefinition
{
    private final String parameterName;
    private final PatternResourceClass parameterClass;
    private final Node defaultValue;


    public ParameterDefinition(String parameterName, PatternResourceClass parameterClass, Node defaultValue)
    {
        this.parameterName = parameterName;
        this.parameterClass = parameterClass;
        this.defaultValue = defaultValue;
    }


    public final String getParamName()
    {
        return this.parameterName;
    }


    public final PatternResourceClass getParameterClass()
    {
        return parameterClass;
    }


    public final Node getDefaultValue()
    {
        return defaultValue;
    }
}
