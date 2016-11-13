package cz.iocb.chemweb.server.sparql.translator.visitor;

import java.util.LinkedHashSet;



/**
 * Auxiliary class used for holding a translation of subqueries. All the segments are being merged during the main
 * translation process.
 */
public class TranslatedSegment
{
    LinkedHashSet<String> subqueryVars = new LinkedHashSet<String>(); //FIXME:
    LinkedHashSet<String> possibleUnboundVars = new LinkedHashSet<String>(); //FIXME:
    String str = "";
    String sqlTableName = "";
    boolean isSparql = false;


    /**
     * Constructs the translated segment.
     *
     * @param str String of translated subquery.
     * @param isSparql Is this segment of type SPARQL?
     */
    public TranslatedSegment(String str, boolean isSparql)
    {
        this.str = str;
        this.isSparql = isSparql;
    }

    /**
     * Constructs the translated segment.
     *
     * @param str String of translated subquery.
     * @param isSparql Is this segment of type SPARQL?
     * @param subqueryVars Variables used (or projected) in this segment.
     */
    public TranslatedSegment(String str, boolean isSparql, LinkedHashSet<String> subqueryVars)
    {
        this(str, isSparql);
        setSubqueryVars(subqueryVars);
        possibleUnboundVars = new LinkedHashSet<>();
    }

    public TranslatedSegment(String str, boolean isSparql, LinkedHashSet<String> subqueryVars,
            LinkedHashSet<String> possibleUnboundVars)
    {
        this(str, isSparql);
        setSubqueryVars(subqueryVars);
        setPossibleUnboundVars(possibleUnboundVars);
    }


    /**
     * Constructs the translated segment.
     *
     * @param str String of translated subquery.
     * @param isSparql Is this segment of type SPARQL?
     * @param subqueryVars Variables used (or projected) in this segment.
     * @param sqlTableName SQL table name of this segment (if of type SQL).
     */
    public TranslatedSegment(String str, boolean isSparql, LinkedHashSet<String> subqueryVars,
            LinkedHashSet<String> possibleUnboundVars, String sqlTableName)
    {
        this(str, isSparql, subqueryVars, possibleUnboundVars);
        this.sqlTableName = sqlTableName;
    }

    public TranslatedSegment(String str, boolean isSparql, LinkedHashSet<String> subqueryVars, String sqlTableName)
    {
        this(str, isSparql, subqueryVars);
        this.sqlTableName = sqlTableName;
    }


    /**
     * Sets variables used (or projected) in this segment.
     *
     * @param subqueryVars List of variables.
     * @return This segment itself.
     */
    public final TranslatedSegment setSubqueryVars(LinkedHashSet<String> subqueryVars)
    {
        this.subqueryVars = new LinkedHashSet<>();
        this.subqueryVars.addAll(subqueryVars);
        return this;
    }

    /**
     * Sets possible unbound variables in this segment.
     *
     * @param possibleUnboundVars List of possible unbound variables.
     * @return This segment itself.
     */
    public final TranslatedSegment setPossibleUnboundVars(LinkedHashSet<String> possibleUnboundVars)
    {
        if(possibleUnboundVars != null && !possibleUnboundVars.isEmpty())
        {
            this.possibleUnboundVars = new LinkedHashSet<>();
            this.possibleUnboundVars.addAll(possibleUnboundVars);
        }
        return this;
    }
}
