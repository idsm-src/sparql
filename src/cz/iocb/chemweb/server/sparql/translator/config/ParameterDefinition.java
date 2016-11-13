package cz.iocb.chemweb.server.sparql.translator.config;

/**
 * Auxiliary class for storing information about parameteres used in the inner procedures.
 */
public class ParameterDefinition
{
    // IRI name of SPARQL parameter
    private String parameterName;

    // Type IRI of the parameter (e.g.
    // <http://www.w3.org/2001/XMLSchema#integer>)
    private String typeIRI;

    // Name of the parameter of the SQL mapped procedure
    private final String mappedParameterName;

    // Default value (optional)
    private String defaultValue;

    /**
     * Constructs parameter definition object.
     *
     * @param mappedParameterName SQL mapped procedure name.
     */
    public ParameterDefinition(String mappedParameterName)
    {
        this(null, null, "", mappedParameterName);
    }

    /**
     * Constructs parameter definition object.
     *
     * @param parameterName IRI name of SPARQL parameter.
     * @param typeIRI Type IRI for the parameter.
     * @param defaultValue Default value for the parameter (optional).
     * @param mappedParameterName SQL mapped procedure name.
     */
    public ParameterDefinition(String parameterName, String typeIRI, String defaultValue, String mappedParameterName)
    {
        this.parameterName = parameterName;
        this.typeIRI = typeIRI;
        this.defaultValue = defaultValue;
        this.mappedParameterName = mappedParameterName;
    }

    /**
     * Sets the SPARQL name (IRI) of the current parameter.
     *
     * @param parameterName Parameter name.
     */
    public void setParamName(String parameterName)
    {
        this.parameterName = parameterName;
    }

    /**
     * Sets the type IRI of the current parameter.
     *
     * @param typeIRI Type IRI.
     */
    public void setTypeIRI(String typeIRI)
    {
        this.typeIRI = typeIRI;
    }

    /**
     * Sets the default value of the current parameter.
     *
     * @param defaultValue Default value.
     */
    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets a SPARQL name (IRI) of the current parameter.
     *
     * @return Parameter name.
     */
    public String getParamName()
    {
        return this.parameterName;
    }

    /**
     * Gets a type IRI of the current parameter.
     *
     * @return Type IRI.
     */
    public String getTypeIRI()
    {
        return this.typeIRI;
    }

    /**
     * Gets a default value of the current parameter.
     *
     * @return Default value.
     */
    public String getDefaultValue()
    {
        return this.defaultValue;
    }

    /**
     * Gets a parameter name of the SQL mapped procedure.
     *
     * @return SQL parameter name.
     */
    public String getMappedParamName()
    {
        return this.mappedParameterName;
    }
}
