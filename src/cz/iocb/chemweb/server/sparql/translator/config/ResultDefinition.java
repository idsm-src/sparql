package cz.iocb.chemweb.server.sparql.translator.config;

/**
 * Auxiliary class for storing information about result columns of inner procedures.
 */
public class ResultDefinition
{
    // IRI name of SPARQL parameter
    private String parameterName;

    // Type IRI of the result (e.g. <http://www.w3.org/2001/XMLSchema#integer>)
    private String typeIRI;

    // Can the result be a NULL value?
    private boolean canBeNull;

    /**
     * Constructs parameter definition object.
     * 
     * @param parameterName IRI name of SPARQL parameter.
     * @param canBeNull Can the result be a NULL value?
     */
    public ResultDefinition(String parameterName, String typeIRI, boolean canBeNull)
    {
        this.parameterName = parameterName;
        this.typeIRI = typeIRI;
        this.canBeNull = canBeNull;
    }

    /**
     * Gets a SPARQL name (IRI) of the current result.
     * 
     * @return Parameter name.
     */
    public String getParamName()
    {
        return this.parameterName;
    }

    /**
     * Gets a type IRI of the result.
     * 
     * @return Type IRI.
     */
    public String getTypeIRI()
    {
        return this.typeIRI;
    }

    /**
     * Can the result be a NULL value?
     * 
     * @return true if result can be a NULL value.
     */
    public boolean isCanBeNull()
    {
        return this.canBeNull;
    }
}
