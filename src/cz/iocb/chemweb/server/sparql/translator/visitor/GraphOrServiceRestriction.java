package cz.iocb.chemweb.server.sparql.translator.visitor;

import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;



/**
 * Auxiliary class used for holding GRAPH or SERVICE patterns restrictions during the translation process.
 */
public class GraphOrServiceRestriction
{
    private RestrictionType restrictionType;
    private VarOrIri varOrIri;
    private boolean isSilent;

    /**
     * Constructs the restriction object.
     * 
     * @param restrictionType Type of restriction.
     * @param varOrIri Restricted variable or IRI.
     */
    public GraphOrServiceRestriction(RestrictionType restrictionType, VarOrIri varOrIri)
    {
        this.restrictionType = restrictionType;
        this.varOrIri = varOrIri;
        this.isSilent = false;
    }

    /**
     * Sets the SILENT modifier for the restriction object.
     * 
     * @param isSilent Is the SILENT modifier applied?
     * @return The object itself.
     */
    public GraphOrServiceRestriction setSilent(boolean isSilent)
    {
        this.isSilent = isSilent;
        return this;
    }

    /**
     * Checks whether the restriction type is of the given type.
     * 
     * @param restrictionType Restriction type to check.
     * @return true if the given restriction type matches.
     */
    public boolean isRestrictionType(RestrictionType restrictionType)
    {
        return this.restrictionType == restrictionType;
    }

    /**
     * Checks whether the SILENT modifier is applied.
     * 
     * @return true if the SILENT modifier is applied.
     */
    public boolean isSilent()
    {
        return this.isSilent;
    }

    /**
     * Get variable or IRI on which the restriction is applied.
     * 
     * @return Variable or IRI.
     */
    public VarOrIri getVarOrIri()
    {
        return varOrIri;
    }

    /**
     * Type of restriction.
     */
    public enum RestrictionType
    {
        /**
         * The restriction for the GRAPH pattern.
         */
        GRAPH_RESTRICTION,

        /**
         * The restriction for the SERVICE pattern.
         */
        SERVICE_RESTRICTION
    }
}
