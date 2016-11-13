package cz.iocb.chemweb.server.sparql.translator.visitor;

/**
 * Auxiliary class for holding the information of a parameter call during the translation of inner procedure calls.
 */
public class ParameterCall
{
    private String parName = null;
    private String var = null;
    private String typeIRI = null;
    private String literalVal = null;
    private String vectorVar = null;

    /**
     * Constructs the parameter call objects.
     *
     * @param parName Parameter name.
     * @param var Variable used for this parameter.
     * @param typeIRI Type IRI for this parameter.
     * @param literalVal Used literal value for this parameter.
     */
    public ParameterCall(String parName, String var, String typeIRI, String literalVal)
    {
        this.parName = parName;
        this.var = var;
        this.typeIRI = typeIRI;
        this.literalVal = literalVal;
    }

    /**
     * Sets parameter name.
     *
     * @param parName Parameter name.
     */
    public void setParName(String parName)
    {
        this.parName = parName;
    }

    /**
     * Gets parameter name.
     *
     * @return Parameter name.
     */
    public String getParName()
    {
        return this.parName;
    }

    /**
     * Sets variable name.
     *
     * @param var Variable name.
     */
    public void setVar(String var)
    {
        this.var = var;
    }

    /**
     * Gets variable name.
     *
     * @return Variable name.
     */
    public String getVar()
    {
        return this.var;
    }

    /**
     * Sets parameter type IRI.
     *
     * @param typeIRI Type IRI.
     */
    public void setTypeIRI(String typeIRI)
    {
        this.typeIRI = typeIRI;
    }

    /**
     * Gets parameter type IRI.
     *
     * @return Type IRI.
     */
    public String getTypeIRI()
    {
        return this.typeIRI;
    }

    /**
     * Sets literal value used.
     *
     * @param literalVal Literal value.
     */
    public void setLiteralVal(String literalVal)
    {
        this.literalVal = literalVal;
    }

    /**
     * Gets literal value used.
     *
     * @return Literal value.
     */
    public String getLiteralVal()
    {
        return this.literalVal;
    }

    /**
     * Sets vectorized variable name.
     *
     * @param vectorVar Vectorized variable name.
     */
    public void setVectorVar(String vectorVar)
    {
        this.vectorVar = vectorVar;
    }

    /**
     * Gets vectorized variable name.
     *
     * @return Vectorized variable name.
     */
    public String getVectorVar()
    {
        return this.vectorVar;
    }
}
