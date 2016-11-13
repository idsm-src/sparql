package cz.iocb.chemweb.server.sparql.translator.config;

import java.util.ArrayList;
import java.util.List;



/**
 * Auxiliary class for storing information about inner procedures, which can be used in a SPARQL query.
 */
public class ProcedureDefinition
{
    // SPARQL procedure name; e.g. <http://example.org/sim>
    private String sparqlProcedureName;

    // Procedure return value type IRI; e.g.
    // <http://www.w3.org/2001/XMLSchema#integer>
    private String retTypeIRI;

    // Mapped SQL procedure name; e.g. "sim" for sim(tresh, co)
    private String mappedProcedureName;

    // Parameters of the sparql procedure (+ optional default values); e.g.
    // <http://example.org/tresh> 5
    // (Corresponding to the mapped sql procedure parameters; number of params
    // should be the same)
    private List<ParameterDefinition> parameters;

    // Result columns (in the order they are added)
    private List<ResultDefinition> results;

    /**
     * Constructs procedure definition.
     * 
     * @param sparqlProcedureName SPARQL procedure name.
     * @param retTypeIRI Procedure return type IRI.
     * @param mappedProcedureName Mapped SQL procedure name.
     */
    public ProcedureDefinition(String sparqlProcedureName, String retTypeIRI, String mappedProcedureName)
    {
        this.sparqlProcedureName = sparqlProcedureName;
        this.retTypeIRI = retTypeIRI;
        this.mappedProcedureName = mappedProcedureName;
        this.parameters = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    /**
     * Constructs procedure definition.
     * 
     * @param sparqlProcedureName SPARQL procedure name.
     * @param retTypeIRI Procedure return type IRI.
     */
    public ProcedureDefinition(String sparqlProcedureName, String retTypeIRI)
    {
        this(sparqlProcedureName, retTypeIRI, null);
    }

    /**
     * Sets mapped SQL procedure name.
     * 
     * @param mappedProcedureName SQL procedure name.
     */
    public void setMappedProcName(String mappedProcedureName)
    {
        this.mappedProcedureName = mappedProcedureName;
    }

    /**
     * Adds parameter definition to the current procedure definition.
     * 
     * @param paramDefinition Parameter definition.
     */
    public void addParameter(ParameterDefinition paramDefinition)
    {
        this.parameters.add(paramDefinition);
    }

    /**
     * Adds result definition to the current procedure definition.
     * 
     * @param resultDefinition Result definition.
     */
    public void addResult(ResultDefinition resultDefinition)
    {
        this.results.add(resultDefinition);
    }

    /**
     * Gets SPARQL procedure name.
     * 
     * @return SPARQL procedure name.
     */
    public String getProcName()
    {
        return this.sparqlProcedureName;
    }

    /**
     * Gets return type IRI of the procedure.
     * 
     * @return Procedure return type IRI.
     */
    public String getRetTypeIRI()
    {
        return this.retTypeIRI;
    }

    /**
     * Gets mapped SQL procedure name.
     * 
     * @return Mapped SQL procedure name.
     */
    public String getMappedProcName()
    {
        return this.mappedProcedureName;
    }

    /**
     * Gets list of parameters definitions.
     * 
     * @return Collection of parameters definitions.
     */
    public List<ParameterDefinition> getParameters()
    {
        return this.parameters;
    }

    /**
     * Gets list of result definitions.
     * 
     * @return Collection of result definitions.
     */
    public List<ResultDefinition> getResults()
    {
        return this.results;
    }

    /**
     * Find a parameter definition by its mapped SQL procedure name.
     * 
     * @param mappedParamName Mapped SQL procedure name.
     * @return Parameter definition.
     */
    public ParameterDefinition getParameterByMappedParamName(String mappedParamName)
    {
        for(ParameterDefinition param : this.parameters)
        {
            if(param.getMappedParamName().equals(mappedParamName))
            {
                return param;
            }
        }
        return null;
    }
}
